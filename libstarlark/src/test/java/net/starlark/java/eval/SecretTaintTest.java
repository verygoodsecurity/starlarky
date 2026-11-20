package net.starlark.java.eval;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.starlark.java.annot.Declassifies;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.SecretSource;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.ext.ByteStringModuleApi;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of secret taint at the interpreter's choke points. */
@RunWith(JUnit4.class)
public final class SecretTaintTest {

  private static final String PAN = "4111111111111111";
  private static final String REVEAL = "s = secrets.reveal('tok_fake')\n";
  // As Larky runs scripts: top-level statements and rebinding are allowed.
  private static final FileOptions OPTIONS =
      FileOptions.DEFAULT.toBuilder().allowToplevelRebinding(true).loadBindsGlobally(true).build();

  /** Stands in for a vault. */
  @StarlarkBuiltin(name = "secrets", documented = false)
  public static final class Secrets implements StarlarkValue {
    @SecretSource
    @StarlarkMethod(name = "reveal", parameters = {@Param(name = "alias")})
    public Object reveal(Object alias) throws EvalException {
      if (alias instanceof StarlarkList<?> aliases) {
        Object[] out = new Object[aliases.size()];
        java.util.Arrays.fill(out, new String(PAN));
        return StarlarkList.immutableOf(out);
      }
      return new String(PAN);
    }

    @SecretSource
    @StarlarkMethod(name = "reveal_bytes", useStarlarkThread = true)
    public StarlarkBytes revealBytes(StarlarkThread thread) throws EvalException {
      return StarlarkBytes.of(thread.mutability(), PAN.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Declassifies
    @StarlarkMethod(name = "redact", parameters = {@Param(name = "value")})
    public String redact(String value) {
      return "tok_fake_alias";
    }

    @StarlarkMethod(name = "new_digest")
    public Digest newDigest() {
      return new Digest();
    }

    @StarlarkMethod(name = "new_box")
    public Box newBox() {
      return new Box();
    }

    @StarlarkMethod(name = "plain")
    public String plain() {
      return new String("nothing secret");
    }

    @StarlarkMethod(name = "unavailable", parameters = {@Param(name = "value", named = true)})
    public Object unavailable(Object value) throws EvalException {
      throw Starlark.errorf("diagnostic unavailable");
    }

  }

  /** A mutable object with fields, like {@code larky.mutablestruct}. */
  @StarlarkBuiltin(name = "box", documented = false)
  public static final class Box implements Structure {
    private final Map<String, Object> fields = new LinkedHashMap<>();

    @Override
    public Object getValue(String name) {
      return fields.get(name);
    }

    @Override
    public com.google.common.collect.ImmutableCollection<String> getFieldNames() {
      return com.google.common.collect.ImmutableList.copyOf(fields.keySet());
    }

    @Override
    public String getErrorMessageForUnknownField(String field) {
      return null;
    }

    @Override
    public void setField(String field, Object value) {
      fields.put(field, value);
    }

    @Override
    public void repr(Printer printer, StarlarkSemantics semantics) {
      printer.append("box(").append(fields.values().toString()).append(")");
    }
  }

  /** Stands in for a hash object: mutable state that a secret can flow into. */
  @StarlarkBuiltin(name = "digest", documented = false)
  public static final class Digest implements StarlarkValue {
    private final StringBuilder state = new StringBuilder();

    @StarlarkMethod(name = "update", parameters = {@Param(name = "data")})
    public void update(String data) {
      state.append(data);
    }

    @StarlarkMethod(name = "hexdigest")
    public String hexdigest() {
      return Integer.toHexString(state.toString().hashCode());
    }
  }

  private static final class Run {
    final SecretTaint taint;
    final Module module;
    final List<String> printed = new ArrayList<>();
    final List<String> hits = new ArrayList<>();
    StarlarkThread thread;

    Run(TaintMode mode) {
      this(mode, SecretTaint.DEFAULT_TAG_LIMIT);
    }

    Run(TaintMode mode, int limit) {
      taint = new SecretTaint(mode, (sink, reason) -> hits.add(sink + ":" + reason), limit);
      module =
          Module.withPredeclared(StarlarkSemantics.DEFAULT, ImmutableMap.of("secrets", new Secrets()));
    }

    Run exec(String src) throws Exception {
      try (Mutability mu = Mutability.create("test")) {
        thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
        thread.setThreadLocal(SecretTaint.class, taint);
        thread.setPrintHandler((t, msg) -> printed.add(msg));
        Starlark.execFile(ParserInput.fromString(src, "t.star"), OPTIONS, module, thread);
      }
      return this;
    }

    Object global(String name) {
      return module.getGlobal(name);
    }

    boolean tainted(String name) {
      return taint.isTainted(global(name));
    }
  }

  private static Run enforce(String src) throws Exception {
    return new Run(TaintMode.ENFORCE).exec(src);
  }

  private static void assertResultTainted(String expr) throws Exception {
    assertCarried(expr, enforce(REVEAL + "r = " + expr + "\n"));
  }

  // A taggable result must carry the tag; only an untaggable one may fall back to the sticky rule.
  private static void assertCarried(String expr, Run run) {
    if (SecretTaint.isTaggable(run.global("r"))) {
      assertWithMessage(expr).that(run.tainted("r")).isTrue();
    } else {
      assertWithMessage(expr).that(run.taint.isSecretDependent()).isTrue();
    }
  }

  /** Every str method: expression over the secret s. */
  private static final Map<String, String> STR_METHODS = new LinkedHashMap<>();

  static {
    Map<String, String> m = STR_METHODS;
    m.put("capitalize", "s.capitalize()");
    m.put("count", "s.count('1')");
    m.put("elems", "s.elems()");
    m.put("endswith", "s.endswith('1')");
    m.put("find", "s.find('1')");
    m.put("format", "'{}'.format(s)");
    m.put("index", "s.index('1')");
    m.put("isalnum", "s.isalnum()");
    m.put("isalpha", "s.isalpha()");
    m.put("isdigit", "s.isdigit()");
    m.put("islower", "s.islower()");
    m.put("isspace", "s.isspace()");
    m.put("istitle", "s.istitle()");
    m.put("isupper", "s.isupper()");
    m.put("join", "'-'.join([s, 'x'])");
    m.put("lower", "s.lower()");
    m.put("lstrip", "s.lstrip('4')");
    m.put("partition", "s.partition('1')");
    m.put("removeprefix", "s.removeprefix('4')");
    m.put("removesuffix", "s.removesuffix('1')");
    m.put("replace", "s.replace('1', 'x')");
    m.put("rfind", "s.rfind('1')");
    m.put("rindex", "s.rindex('1')");
    m.put("rpartition", "s.rpartition('1')");
    m.put("rsplit", "s.rsplit('1')");
    m.put("rstrip", "s.rstrip('1')");
    m.put("split", "s.split('1')");
    m.put("splitlines", "s.splitlines()");
    m.put("startswith", "s.startswith('4')");
    m.put("strip", "s.strip('4')");
    m.put("title", "s.title()");
    m.put("upper", "s.upper()");
  }

  /** Every bytes method: expression over the secret b. */
  private static final Map<String, String> BYTES_METHODS = new LinkedHashMap<>();

  static {
    Map<String, String> m = BYTES_METHODS;
    for (String name :
        new String[] {
          "capitalize", "hex", "isalnum", "isalpha", "isascii", "isdigit", "islower", "isspace",
          "istitle", "isupper", "lower", "lstrip", "rstrip", "splitlines", "strip", "swapcase",
          "title", "upper", "decode",
        }) {
      m.put(name, "b." + name + "()");
    }
    m.put("center", "b.center(30)");
    m.put("count", "b.count(b'1')");
    m.put("endswith", "b.endswith(b'1')");
    m.put("expandtabs", "b.expandtabs()");
    m.put("find", "b.find(b'1')");
    m.put("index", "b.index(b'1')");
    m.put("join", "b.join([b'x', b'y'])");
    m.put("ljust", "b.ljust(30)");
    m.put("partition", "b.partition(b'1')");
    m.put("removeprefix", "b.removeprefix(b'4')");
    m.put("removesuffix", "b.removesuffix(b'1')");
    m.put("replace", "b.replace(b'1', b'x')");
    m.put("rfind", "b.rfind(b'1')");
    m.put("rindex", "b.rindex(b'1')");
    m.put("rjust", "b.rjust(30)");
    m.put("rpartition", "b.rpartition(b'1')");
    m.put("rsplit", "b.rsplit(b'1')");
    m.put("split", "b.split(b'1')");
    m.put("startswith", "b.startswith(b'4')");
    m.put("translate", "b.translate(bytes(range(256)))");
    m.put("zfill", "b.zfill(30)");
  }

  @Test
  public void strMethodTableCoversEveryMethod() {
    Set<String> declared = new TreeSet<>();
    Starlark.getMethodAnnotations(StringModule.class).values().forEach(a -> declared.add(a.name()));
    assertThat(new TreeSet<>(STR_METHODS.keySet())).isEqualTo(declared);
  }

  @Test
  public void bytesMethodTableCoversEveryMethod() {
    Set<String> declared = new TreeSet<>();
    Starlark.getMethodAnnotations(ByteStringModuleApi.class)
        .values()
        .forEach(a -> declared.add(a.name()));
    assertThat(new TreeSet<>(BYTES_METHODS.keySet())).isEqualTo(declared);
  }

  @Test
  public void everyStrMethodPropagates() throws Exception {
    for (Map.Entry<String, String> e : STR_METHODS.entrySet()) {
      assertResultTainted(e.getValue());
    }
  }

  @Test
  public void everyBytesMethodPropagates() throws Exception {
    for (Map.Entry<String, String> e : BYTES_METHODS.entrySet()) {
      Run run;
      try {
        run = enforce("b = secrets.reveal_bytes()\nr = " + e.getValue() + "\n");
      } catch (Exception ex) {
        throw new AssertionError(e.getValue(), ex);
      }
      assertCarried(e.getValue(), run);
    }
  }

  @Test
  public void operatorsPropagate() throws Exception {
    for (String expr :
        new String[] {
          "s + 'x'", "'x' + s", "s * 2", "2 * s", "'%s' % s", "'%s %s' % (s, 'x')", "'{}'.format(s)",
          "[s] + []", "(s,) + ()", "[s] * 2", "{'k': s}", "int(s) + 1", "1 + int(s)", "int(s) * 3",
          "int(s) // 2", "int(s) % 7", "int(s) - 1", "int(s) << 2", "int(s) >> 2", "int(s) | 1",
          "int(s) & 255", "int(s) ^ 1", "-int(s)", "~int(s)", "int(s) / 2.0", "s[0]", "s[:6]",
          "s[-4:]", "s[::-1]", "str(s)", "repr(s)", "bytes(s)", "list(s.elems())",
          "sorted(s.elems())", "reversed(s.elems())", "tuple(s.elems())", "set(s.elems())",
          "dict(a=s)", "zip(s.elems())", "enumerate(s.elems())", "abs(int(s))",
          "max(s.elems())", "min(s.elems())",
        }) {
      assertResultTainted(expr);
    }
  }

  @Test
  public void comparisonsAndMembershipMakeTheRunSecretDependent() throws Exception {
    for (String expr :
        new String[] {
          "s == 'x'", "s != 'x'", "s < 'x'", "s <= 'x'", "s > 'x'", "s >= 'x'", "'4' in s",
          "'4' not in s", "s in ['x']", "s not in ['x']",
        }) {
      Run run = enforce(REVEAL + "r = " + expr + "\nprint('visa')\n");
      assertThat(run.taint.isSecretDependent()).isTrue();
      assertThat(run.printed).containsExactly(SecretTaint.REDACTED);
    }
  }

  @Test
  public void truthTestsMakeTheRunSecretDependent() throws Exception {
    for (String src :
        new String[] {
          "def f():\n  if s:\n    pass\nf()\n",
          "r = s and 1\n",
          "r = s or 1\n",
          "r = not s\n",
          "r = 1 if s else 2\n",
          "r = [1 for _ in [1] if s]\n",
        }) {
      Run run = enforce(REVEAL + src + "print('visa')\n");
      assertThat(run.taint.isSecretDependent()).isTrue();
      assertThat(run.printed).containsExactly(SecretTaint.REDACTED);
    }
  }

  @Test
  public void untaggableBuiltinResultsMakeTheRunSecretDependent() throws Exception {
    for (String expr :
        new String[] {"s.startswith('4')", "len(s)", "int(s[0])", "s.find('zz')", "bool(s)"}) {
      Run run = enforce(REVEAL + "r = " + expr + "\nprint('visa')\n");
      assertThat(run.taint.isSecretDependent()).isTrue();
      assertThat(run.printed).containsExactly(SecretTaint.REDACTED);
    }
  }

  @Test
  public void outputBeforeTheTriggerIsUnaffected() throws Exception {
    Run run =
        enforce("print('before')\n" + REVEAL + "print('after reveal')\nr = len(s)\nprint('after')\n");
    assertThat(run.printed).containsExactly("before", "after reveal", SecretTaint.REDACTED).inOrder();
  }

  @Test
  public void runWithoutRevealNeverActivates() throws Exception {
    Run run =
        enforce("s = 'not secret'\nr = len(s)\nt = 1 if s == 'x' else 2\nprint(s)\n");
    assertThat(run.thread.getSecretTaint()).isNull();
    assertThat(run.taint.isActive()).isFalse();
    assertThat(run.taint.isSecretDependent()).isFalse();
    assertThat(run.printed).containsExactly("not secret");
    assertThat(run.hits).isEmpty();
  }

  @Test
  public void offModeLeavesEverythingAlone() throws Exception {
    Run run = new Run(TaintMode.OFF).exec(REVEAL + "print(s)\nr = len(s)\nprint('visa')\n");
    assertThat(run.thread.getSecretTaint()).isNull();
    assertThat(run.printed).containsExactly(PAN, "visa").inOrder();
    assertThat(run.hits).isEmpty();
  }

  @Test
  public void memoizedCharacterIsCopiedNotTagged() throws Exception {
    Run run = enforce(REVEAL + "c = s[0]\nd = '4'\ne = s[0:1]\nf = s.elems()[0]\n");
    assertThat(run.tainted("c")).isTrue();
    assertThat(run.tainted("e")).isTrue();
    assertThat(run.tainted("f")).isTrue();
    assertThat(run.tainted("d")).isFalse();
    assertThat(run.global("c")).isEqualTo("4");
    assertThat(run.global("c")).isNotSameInstanceAs(run.global("d"));
  }

  @Test
  public void unrelatedLiteralIsNotBlocked() throws Exception {
    Run run = enforce(REVEAL + "c = s[0]\nprint('4')\nprint(c)\n");
    assertThat(run.printed).containsExactly("4", SecretTaint.REDACTED).inOrder();
  }

  @Test
  public void emptyStringsCarryNothing() throws Exception {
    Run run = enforce(REVEAL + "r = s[100:]\nprint(r)\n");
    assertThat(run.tainted("r")).isFalse();
    assertThat(run.taint.isSecretDependent()).isFalse();
    assertThat(run.printed).containsExactly("");
  }

  @Test
  public void containersKeepTheirElementsTainted() throws Exception {
    Run run =
        enforce(
            REVEAL
                + "l = []\nl.append(s)\njoined = ','.join(l)\n"
                + "d = {'a': s}\ndv = d['a']\n"
                + "t = (s, 'x')\nt0 = t[0]\n"
                + "nested = {'k': [(s,)]}\nn = nested['k'][0][0]\n"
                + "print(l)\nprint(d)\nprint(t)\nprint(nested)\nprint(joined)\n");
    assertThat(run.tainted("joined")).isTrue();
    assertThat(run.tainted("dv")).isTrue();
    assertThat(run.tainted("t0")).isTrue();
    assertThat(run.tainted("n")).isTrue();
    assertThat(run.printed).hasSize(5);
    assertThat(run.printed).containsNoneOf(PAN, "[\"" + PAN + "\"]");
    assertThat(run.printed.stream().allMatch(l -> l.contains(SecretTaint.REDACTED))).isTrue();
  }

  @Test
  public void appendDoesNotMakeTheRunSecretDependent() throws Exception {
    Run run = enforce(REVEAL + "l = []\nl.append(s)\nprint('ok')\n");
    assertThat(run.taint.isSecretDependent()).isFalse();
    assertThat(run.printed).containsExactly("ok");
  }

  @Test
  public void searchPastTheDepthLimitCountsAsTainted() throws Exception {
    Run run =
        enforce(
            REVEAL
                + "def nest():\n  deep = 'x'\n  for _ in range(20):\n    deep = [deep]\n  return deep\n"
                + "print(nest())\n");
    assertThat(run.printed).containsExactly(SecretTaint.REDACTED);
  }

  @Test
  public void mutableReceiverCarriesTheSecret() throws Exception {
    Run run =
        enforce(REVEAL + "h = secrets.new_digest()\nh.update(s)\nr = h.hexdigest()\nprint(r)\n");
    assertThat(run.tainted("r")).isTrue();
    assertThat(run.taint.isSecretDependent()).isFalse();
    assertThat(run.printed).containsExactly(SecretTaint.REDACTED);
  }

  @Test
  public void digestWithoutASecretStaysClean() throws Exception {
    Run run =
        enforce(REVEAL + "h = secrets.new_digest()\nh.update('public')\nr = h.hexdigest()\nprint(r)\n");
    assertThat(run.tainted("r")).isFalse();
    assertThat(run.printed).hasSize(1);
    assertThat(run.printed.get(0)).isNotEqualTo(SecretTaint.REDACTED);
  }

  @Test
  public void redactDeclassifies() throws Exception {
    Run run = enforce(REVEAL + "a = secrets.redact(s)\nprint(a)\n");
    assertThat(run.tainted("a")).isFalse();
    assertThat(run.taint.isSecretDependent()).isFalse();
    assertThat(run.printed).containsExactly("tok_fake_alias");
  }

  @Test
  public void listRevealTagsEachElement() throws Exception {
    Run run = enforce("l = secrets.reveal(['a', 'b'])\nr = l[1]\nprint(r)\n");
    assertThat(run.tainted("l")).isTrue();
    assertThat(run.tainted("r")).isTrue();
    assertThat(run.printed).containsExactly(SecretTaint.REDACTED);
  }

  @Test
  public void iteratingBytesIsSecretDependent() throws Exception {
    Run run =
        enforce(
            "b = secrets.reveal_bytes()\ndef f():\n  for x in b:\n    print(x)\n    break\nf()\n");
    assertThat(run.taint.isSecretDependent()).isTrue();
    assertThat(run.printed).containsExactly(SecretTaint.REDACTED);
  }

  @Test
  public void iteratingElemsYieldsTaggedCopies() throws Exception {
    Run run =
        enforce(
            REVEAL
                + "def f():\n  out = []\n  for c in s.elems():\n    out.append(c)\n  return out\n"
                + "r = f()[0]\n");
    assertThat(run.tainted("r")).isTrue();
    assertThat(StringModule.isMemoized(run.global("r"))).isFalse();
  }

  @Test
  public void fieldWriteTaintsTheObject() throws Exception {
    // Reversing defeats the value scan, so only the tag can catch this.
    Run run = enforce(REVEAL + "b = secrets.new_box()\nb.v = s[::-1]\nprint(b)\n");
    assertThat(run.tainted("b")).isTrue();
    assertThat(run.printed).containsExactly(SecretTaint.REDACTED);
  }

  @Test
  public void fieldReadFromATaintedObjectIsTainted() throws Exception {
    Run run = enforce(REVEAL + "b = secrets.new_box()\nb.v = s\nb.w = 'public'\nr = b.w\n");
    assertThat(run.tainted("r")).isTrue();
  }

  @Test
  public void defBodiesAreCoveredByTheirSites() throws Exception {
    Run run =
        enforce(
            REVEAL
                + "def rev(x):\n  out = ''\n  for c in x.elems():\n    out = c + out\n  return out\n"
                + "r = rev(s)\nprint(r)\n");
    assertThat(run.tainted("r")).isTrue();
    assertThat(run.printed).containsExactly(SecretTaint.REDACTED);
  }

  @Test
  public void observeLeavesOutputUnchangedAndCounts() throws Exception {
    Run run = new Run(TaintMode.OBSERVE).exec(REVEAL + "print(s)\n");
    assertThat(run.printed).containsExactly(PAN);
    assertThat(run.hits).containsExactly("PRINT:TAGGED");
  }

  @Test
  public void enforceBlocksWithoutThrowing() throws Exception {
    Run run = enforce(REVEAL + "print('got', s)\nr = 'still running'\n");
    assertThat(run.printed).containsExactly("got " + SecretTaint.REDACTED);
    assertThat(run.global("r")).isEqualTo("still running");
    assertThat(run.hits).containsExactly("PRINT:TAGGED");
  }

  @Test
  public void valueScanCatchesAValueThatLostItsTag() throws Exception {
    // The literal has the secret's text but none of its identity.
    String lost = "x = secrets.plain() + '" + PAN + "'\nprint(x)\n";
    Run run = enforce(REVEAL + lost);
    assertThat(run.printed).containsExactly("nothing secret" + SecretTaint.REDACTED);
    assertThat(run.hits).containsExactly("PRINT:VALUE_MATCH");
  }

  @Test
  public void valueScanCatchesEncodings() {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    taint.onSource(new String(PAN));
    assertThat(taint.containsSecretValue("a " + PAN + " b")).isTrue();
    assertThat(taint.containsSecretValue("NDExMTExMTExMTExMTExMQ")).isTrue();
    assertThat(taint.containsSecretValue("34313131313131313131313131313131")).isTrue();
    assertThat(taint.containsSecretValue("34313131313131313131313131313131".toUpperCase())).isTrue();
    assertThat(taint.containsSecretValue("nothing here")).isFalse();
  }

  @Test
  public void valueScanCatchesUrlEncoding() {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    taint.onSource(new String("a b&c=fake"));
    assertThat(taint.containsSecretValue("q=a+b%26c%3Dfake")).isTrue();
  }

  @Test
  public void valueScanUsesOriginalBytesForBinaryEncodings() {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    taint.onSource(StarlarkBytes.of(Mutability.IMMUTABLE, (byte) 0xff, (byte) 0xfe, (byte) 0, (byte) 1));
    assertThat(taint.containsSecretValue("//4AAQ==")).isTrue();
    assertThat(taint.containsSecretValue("__4AAQ==")).isTrue();
    assertThat(taint.containsSecretValue("FFFE0001")).isTrue();
    assertThat(taint.containsSecretValue("\uFFFD\uFFFD\0\1")).isFalse();
  }

  @Test
  public void shortPlaintextsAreNotScanned() {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    taint.onSource(new String("42"));
    assertThat(taint.containsSecretValue("the answer is 42")).isFalse();
  }

  @Test
  public void repeatedRevealsDoNotFillTheFormInventory() {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    for (int i = 0; i < 2000; i++) {
      taint.onSource(new String("separate instance"));
    }
    assertThat(taint.isSaturated()).isFalse();
    assertThat(taint.mask("diagnostic: separate instance")).isEqualTo("diagnostic: " + SecretTaint.REDACTED);
  }

  @Test
  public void distinctRevealsRespectTheFormCountBudget() {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    for (int i = 0; i < 2000 && !taint.isSaturated(); i++) {
      taint.onSource("registered value " + i);
    }
    assertThat(taint.isSaturated()).isTrue();
  }

  @Test
  public void longRevealsRespectTheTotalFormLengthBudget() {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    String payload = "a".repeat(4096);
    for (int i = 0; i < 30 && !taint.isSaturated(); i++) {
      taint.onSource("value " + i + ":" + payload);
    }
    assertThat(taint.isSaturated()).isTrue();
  }

  @Test
  public void oversizedSourceStopsBeforeEncoding() {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    taint.onSource("text".repeat(5000));
    assertThat(taint.isSaturated()).isTrue();
  }

  @Test
  public void textScanWorkLimitReportsSaturationInBothModes() {
    String diagnostic = "ordinary diagnostic".repeat(4096);
    for (TaintMode mode : List.of(TaintMode.OBSERVE, TaintMode.ENFORCE)) {
      List<String> hits = new ArrayList<>();
      SecretTaint taint = new SecretTaint(mode, (s, r) -> hits.add(s + ":" + r));
      for (int i = 0; i < 10; i++) {
        taint.onSource("registered value " + i + ":" + "a".repeat(64));
      }
      String actual = taint.sanitizeError(diagnostic);
      assertThat(taint.isSaturated()).isTrue();
      assertThat(hits).containsExactly("ERROR_TEXT:SATURATED");
      assertThat(actual).isEqualTo(mode == TaintMode.ENFORCE ? SecretTaint.REDACTED : diagnostic);
    }
  }

  @Test
  public void matchingSpansRespectTheOutputBudget() {
    String diagnostic = "amber ".repeat(10_001);
    for (TaintMode mode : List.of(TaintMode.OBSERVE, TaintMode.ENFORCE)) {
      List<String> hits = new ArrayList<>();
      SecretTaint taint = new SecretTaint(mode, (s, r) -> hits.add(s + ":" + r));
      taint.onSource(new String("amber"));
      String actual = taint.sanitizeError(diagnostic);
      assertThat(taint.isSaturated()).isTrue();
      assertThat(hits).containsExactly("ERROR_TEXT:SATURATED");
      assertThat(actual).isEqualTo(mode == TaintMode.ENFORCE ? SecretTaint.REDACTED : diagnostic);
    }
  }

  @Test
  public void containerInspectionBudgetsIncludeScalarsWithoutTags() {
    List<String> hits = new ArrayList<>();
    SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, (s, r) -> hits.add(s + ":" + r));
    Tuple values = Tuple.copyOf(java.util.Collections.nCopies(10_001, true));
    assertThat(taint.checkOutput(SecretTaint.Sink.PRINT, values))
        .isEqualTo(SecretTaint.Reason.SATURATED);
    assertThat(hits).containsExactly("PRINT:SATURATED");
  }

  @Test
  public void registersSourceLeafAtTheTraversalBoundary() {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    Object result = new String("synthetic source field");
    for (int i = 0; i < 8; i++) {
      result = Tuple.of(result);
    }
    taint.onSource(result);
    assertThat(taint.isSaturated()).isFalse();
    assertThat(taint.sanitizeError("field: synthetic source field"))
        .isEqualTo("field: " + SecretTaint.REDACTED);
  }

  @Test
  public void sourceSubtreeBeyondTraversalBoundarySaturates() {
    List<String> hits = new ArrayList<>();
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, (s, r) -> hits.add(s + ":" + r));
    Object result = new String("synthetic source field");
    for (int i = 0; i < 9; i++) {
      result = Tuple.of(result);
    }
    taint.onSource(result);
    assertThat(taint.sanitizeError("ordinary diagnostic")).isEqualTo(SecretTaint.REDACTED);
    assertThat(hits).containsExactly("ERROR_TEXT:SATURATED");
  }

  @Test
  public void tagTraversalAllowsEmptySubtreeAtTheBoundary() {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    Object result = Tuple.empty();
    for (int i = 0; i < 8; i++) {
      result = Tuple.of(result);
    }
    taint.tag(result);
    assertThat(taint.isSaturated()).isFalse();
  }

  @Test
  public void aliasBatchInspectionSpendsTheRunBudgetBeforeActivation() {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    StarlarkList<String> aliases =
        StarlarkList.immutableCopyOf(java.util.Collections.nCopies(4000, "unregistered alias"));
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      thread.setThreadLocal(SecretTaint.class, taint);
      for (int i = 0; i < 125; i++) {
        Object snapshot = Taint.snapshotSourceInput(thread, aliases);
        assertThat(taint.onSource(aliases, snapshot)).isSameInstanceAs(aliases);
      }
      assertThat(taint.isActive()).isFalse();
      assertThat(Taint.snapshotSourceInput(thread, aliases)).isNull();
      assertThat(taint.isActive()).isTrue();
      assertThat(thread.getSecretTaint()).isSameInstanceAs(taint);
      assertThat(taint.sanitizeError("ordinary diagnostic")).isEqualTo(SecretTaint.REDACTED);
    }
  }

  @Test
  public void errorTextUsesRunWideTaintReasons() {
    List<String> hits = new ArrayList<>();
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, (s, r) -> hits.add(s + ":" + r));
    taint.onSource(new String(PAN));
    taint.markSecretDependent();
    assertThat(taint.sanitizeError("failed: bad state")).isEqualTo(SecretTaint.REDACTED);
    taint.saturate();
    assertThat(taint.sanitizeError("failed: bad state")).isEqualTo(SecretTaint.REDACTED);
    assertThat(hits).containsExactly("ERROR_TEXT:SECRET_DEPENDENT", "ERROR_TEXT:SATURATED").inOrder();
  }

  @Test
  public void observeReportsSaturatedErrorsWithoutChangingText() {
    List<String> hits = new ArrayList<>();
    SecretTaint taint = new SecretTaint(TaintMode.OBSERVE, (s, r) -> hits.add(s + ":" + r));
    taint.saturate();
    assertThat(taint.sanitizeError("failed: bad state")).isEqualTo("failed: bad state");
    assertThat(hits).containsExactly("ERROR_TEXT:SATURATED");
  }

  @Test
  public void failKeepsRawTextAndMasksPreciseDiagnosticTextOnRequest() {
    Run run = new Run(TaintMode.ENFORCE);
    EvalException failure =
        assertThrows(EvalException.class, () -> run.exec(REVEAL + "fail('bad card: ' + s)\n"));
    assertThat(failure.getMessage()).isEqualTo("bad card: " + PAN);
    assertThat(failure.hasTaintedDiagnostic()).isTrue();
    assertThat(new EvalException(failure).hasTaintedDiagnostic()).isTrue();
    assertThat(new EvalException("ordinary diagnostic", failure).hasTaintedDiagnostic()).isFalse();
    assertThat(run.hits).isEmpty();
    assertThat(run.taint.sanitizeError(failure.getMessage()))
        .isEqualTo("bad card: " + SecretTaint.REDACTED);
    assertThat(run.taint.sanitizeError("runtime error: " + failure.getMessage()))
        .isEqualTo(SecretTaint.REDACTED);
    assertThat(run.hits).containsExactly("ERROR_TEXT:TAGGED", "ERROR_TEXT:TAGGED");
  }

  @Test
  public void builtinErrorsWithTaintedInputsHaveConservativeDiagnostics() {
    for (String argument : List.of("s", "value=s")) {
      Run run = new Run(TaintMode.ENFORCE);
      EvalException failure = assertThrows(
          EvalException.class, () -> run.exec(REVEAL + "secrets.unavailable(" + argument + ")\n"));
      assertThat(failure.getMessage()).isEqualTo("diagnostic unavailable");
      assertThat(failure.hasTaintedDiagnostic()).isTrue();
      assertThat(run.taint.sanitizeError(failure.getMessage())).isEqualTo(SecretTaint.REDACTED);
      assertThat(run.hits).containsExactly("ERROR_TEXT:TAGGED");
    }
  }

  @Test
  public void observedBuiltinErrorsPreserveOriginalText() {
    Run run = new Run(TaintMode.OBSERVE);
    EvalException failure =
        assertThrows(EvalException.class, () -> run.exec(REVEAL + "secrets.unavailable(s)\n"));
    assertThat(run.taint.sanitizeError(failure.getMessage())).isEqualTo("diagnostic unavailable");
    assertThat(run.hits).containsExactly("ERROR_TEXT:TAGGED");
  }

  @Test
  public void unrelatedBuiltinErrorsKeepTheirText() {
    Run run = new Run(TaintMode.ENFORCE);
    EvalException failure = assertThrows(
        EvalException.class, () -> run.exec(REVEAL + "secrets.unavailable('public')\n"));
    assertThat(run.taint.sanitizeError(failure.getMessage())).isEqualTo("diagnostic unavailable");
    assertThat(run.hits).isEmpty();
    assertThat(failure.hasTaintedDiagnostic()).isFalse();
  }

  @Test
  public void dictionaryErrorsCarryProvenanceFromTheirKeys() {
    for (String line : List.of("r = {s: 1, s: 2}", "r = {}[s]")) {
      Run run = new Run(TaintMode.ENFORCE);
      EvalException failure =
          assertThrows(EvalException.class, () -> run.exec(REVEAL + line + "\n"));
      assertThat(failure.hasTaintedDiagnostic()).isTrue();
      assertThat(run.taint.sanitizeError(failure.getMessage())).isEqualTo(SecretTaint.REDACTED);
      assertThat(run.hits).containsExactly("ERROR_TEXT:TAGGED");
    }
  }

  @Test
  public void untaintedLookupAfterAnEarlierErrorHasNoDiagnosticTag() {
    Run run = new Run(TaintMode.ENFORCE);
    assertThrows(EvalException.class, () -> run.exec(REVEAL + "r = {}[s]\n"));
    EvalException failure =
        assertThrows(EvalException.class, () -> run.exec("r = {}['public']\n"));
    assertThat(failure.hasTaintedDiagnostic()).isFalse();
  }

  @Test
  public void maskIsAFixedStringWhateverTheSecretLength() {
    assertThat(SecretTaint.REDACTED).isEqualTo("***REDACTED***");
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    taint.onSource(new String("abc"));
    taint.onSource(new String("a-much-longer-secret-value"));
    assertThat(taint.mask("x abc y a-much-longer-secret-value z"))
        .isEqualTo("x ***REDACTED*** y ***REDACTED*** z");
  }

  @Test
  public void maskCoversEveryEncodedFormAndEveryOccurrence() {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    taint.onSource(new String("a b&c=fake"));
    String b64 = java.util.Base64.getEncoder().withoutPadding()
        .encodeToString("a b&c=fake".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    String hex = "61206226633d66616b65";
    assertThat(taint.mask("raw=a b&c=fake url=a+b%26c%3Dfake b64=" + b64 + " hex=" + hex.toUpperCase()
            + " again=a b&c=fake"))
        .isEqualTo(
            "raw=***REDACTED*** url=***REDACTED*** b64=***REDACTED*** hex=***REDACTED***"
                + " again=***REDACTED***");
  }

  @Test
  public void maskHandlesOverlappingFormsAndNeverRematchesTheMask() {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    taint.onSource(new String("abcdef"));
    taint.onSource(new String("defxyz"));
    // The two plaintexts overlap in "def": one mask covers the union, no tail is left.
    assertThat(taint.mask("<abcdefxyz>")).isEqualTo("<***REDACTED***>");
    // A secret that occurs inside the mask text cannot be matched again.
    SecretTaint inMask = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    inMask.onSource(new String("RED"));
    inMask.onSource(new String("abcRED"));
    assertThat(inMask.mask("abcRED and RED")).isEqualTo("***REDACTED*** and ***REDACTED***");
  }

  @Test
  public void maskLeavesTextWithoutAFormUnchanged() {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    taint.onSource(new String(PAN));
    String text = "nothing here";
    assertThat(taint.mask(text)).isSameInstanceAs(text);
  }

  @Test
  public void printMasksTheSecretInPlace() throws Exception {
    Run run = enforce(REVEAL + "print('bad card: ' + s)\n");
    assertThat(run.printed).containsExactly("bad card: ***REDACTED***");
    assertThat(run.hits).containsExactly("PRINT:TAGGED");
  }

  @Test
  public void printMasksOnlyTheTaggedArgument() throws Exception {
    Run run = enforce(REVEAL + "print('card', s, 'ok')\n");
    assertThat(run.printed).containsExactly("card ***REDACTED*** ok");
  }

  @Test
  public void printRechecksTaintActivatedDuringRendering() throws Exception {
    Run run = new Run(TaintMode.ENFORCE).exec("");
    new MethodLibrary().print(" ", Tuple.of(activatingDiagnostic("rendered")), run.thread);
    assertThat(run.printed).containsExactly(SecretTaint.REDACTED);
    assertThat(run.hits).containsExactly("PRINT:TAGGED");
  }

  @Test
  public void failRecordsTaintActivatedDuringRenderingWithoutChangingRawText() throws Exception {
    Run run = new Run(TaintMode.ENFORCE).exec("");
    EvalException failure =
        assertThrows(EvalException.class,
            () -> new MethodLibrary().fail(
                activatingDiagnostic("rendered"), Starlark.NONE, " ", false,
                Tuple.empty(), run.thread));
    assertThat(failure.getMessage()).isEqualTo("rendered");
    assertThat(failure.hasTaintedDiagnostic()).isTrue();
    assertThat(run.taint.sanitizeError(failure.getMessage())).isEqualTo(SecretTaint.REDACTED);
  }

  private static StarlarkValue activatingDiagnostic(String message) {
    return new StarlarkValue() {
      @Override
      public void debugPrint(Printer printer, StarlarkThread thread) {
        SecretTaint state = thread.getThreadLocal(SecretTaint.class);
        state.markActive();
        thread.setSecretTaint(state);
        printer.append(message);
      }
    };
  }

  @Test
  public void printKeepsThePublicPortionOfAnAttributedRepresentation() throws Exception {
    Run run = enforce(REVEAL);
    String value = run.taint.tagConcatenation("detail: ", (String) run.global("s"), "detail: " + PAN);
    new MethodLibrary().print(" ", Tuple.of(renderedDiagnostic(value, run.thread)), run.thread);
    assertThat(run.printed).containsExactly("detail: " + SecretTaint.REDACTED);
    assertThat(run.hits).containsExactly("PRINT:TAGGED");
  }

  @Test
  public void foreignRepresentationIsOpaqueOnlyWhenTrackingIsEnabled() throws Exception {
    Run off = new Run(TaintMode.OFF).exec("");
    Run enforce = new Run(TaintMode.ENFORCE).exec("");
    StarlarkValue diagnostic = renderedDiagnostic("rendered", off.thread);
    new MethodLibrary().print(" ", Tuple.of(diagnostic), enforce.thread);
    assertThat(enforce.printed).containsExactly(SecretTaint.REDACTED);
    assertThat(enforce.hits).containsExactly("PRINT:TAGGED");

    StarlarkValue foreignToOff = renderedDiagnostic("rendered", enforce.thread);
    new MethodLibrary().print(" ", Tuple.of(foreignToOff), off.thread);
    assertThat(off.printed).containsExactly("rendered");
    assertThat(off.hits).isEmpty();
  }

  private static StarlarkValue renderedDiagnostic(String message, StarlarkThread producer) {
    return new StarlarkValue() {
      @Override
      public void debugPrint(Printer printer, StarlarkThread thread) {
        printer.appendRenderedValue(message, producer);
      }
    };
  }

  @Test
  public void printMasksAnOpaqueTaggedArgumentWholeEvenWithAKnownForm() throws Exception {
    Run run = enforce(REVEAL);
    Object opaque = run.taint.tag(new String("synthetic " + PAN));
    new MethodLibrary().print(" ", Tuple.of("detail:", opaque), run.thread);
    assertThat(run.printed).containsExactly("detail: " + SecretTaint.REDACTED);
  }

  @Test
  public void printDoesNotScanTextItJustMasked() throws Exception {
    Run run = enforce(REVEAL);
    Object shortSecret = run.taint.onSource(new String("RED"));
    new MethodLibrary().print(" ", Tuple.of("color:", shortSecret), run.thread);
    assertThat(run.printed).containsExactly("color: " + SecretTaint.REDACTED);
  }

  @Test
  public void printPreservesPublicConcatenationSpansBetweenTaggedStrings() throws Exception {
    Run run = enforce(REVEAL + "print('first=' + s + ', second=' + s + '.')\n");
    assertThat(run.printed).containsExactly("first=***REDACTED***, second=***REDACTED***.");
  }

  @Test
  public void printMasksATaggedValueInsideAContainerInPlace() throws Exception {
    Run run = enforce(REVEAL + "print('cards', [s, 'x'])\n");
    assertThat(run.printed).containsExactly("cards [\"***REDACTED***\", \"x\"]");
  }

  @Test
  public void printPreservesKnownSpansInsideAQuotedString() throws Exception {
    Run run = enforce(REVEAL + "print(['first=' + s + '.'])\n");
    assertThat(run.printed).containsExactly("[\"first=***REDACTED***.\"]");
  }

  @Test
  public void aTransformedTaggedArgumentIsMaskedWhole() throws Exception {
    Run run = enforce(REVEAL + "print('card', s[::-1], 'ok')\n");
    assertThat(run.printed).containsExactly("card ***REDACTED*** ok");
    assertThat(run.printed.get(0)).doesNotContain(PAN);
  }

  @Test
  public void aSecretDependentBranchRedactsTheWholeLine() throws Exception {
    Run run = enforce(REVEAL + "def f():\n    if s.startswith('4'):\n        print('visa', 'line')\nf()\n");
    assertThat(run.printed).containsExactly(SecretTaint.REDACTED);
    assertThat(run.hits).containsExactly("PRINT:SECRET_DEPENDENT");
  }

  @Test
  public void sanitizeMasksValueMatchesAndRedactsWholeForOtherReasons() {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    taint.onSource(new String(PAN));
    assertThat(taint.sanitize(SecretTaint.Sink.PRINT, "pan " + PAN + "!", null))
        .isEqualTo("pan ***REDACTED***!");
    assertThat(
            taint.sanitize(SecretTaint.Sink.PRINT, "pan " + PAN, SecretTaint.Reason.SECRET_DEPENDENT))
        .isEqualTo(SecretTaint.REDACTED);
    assertThat(taint.sanitize(SecretTaint.Sink.PRINT, "pan " + PAN, SecretTaint.Reason.SATURATED))
        .isEqualTo(SecretTaint.REDACTED);
    assertThat(taint.sanitize(SecretTaint.Sink.PRINT, "no form", SecretTaint.Reason.TAGGED))
        .isEqualTo(SecretTaint.REDACTED);
    assertThat(taint.sanitize(SecretTaint.Sink.PRINT, "pan " + PAN, SecretTaint.Reason.TAGGED))
        .isEqualTo(SecretTaint.REDACTED);
  }

  @Test
  public void sanitizeErrorMasksTheSecretAndKeepsTheRest() {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, TaintObserver.NOOP);
    taint.onSource(new String(PAN));
    assertThat(taint.sanitizeError("failed on " + PAN + " twice " + PAN))
        .isEqualTo("failed on ***REDACTED*** twice ***REDACTED***");
  }

  @Test
  public void observeModeLeavesTextUnchangedButReports() throws Exception {
    Run run = new Run(TaintMode.OBSERVE).exec(REVEAL + "print('bad card: ' + s)\n");
    assertThat(run.printed).containsExactly("bad card: " + PAN);
    assertThat(run.hits).containsExactly("PRINT:TAGGED");
    SecretTaint observe = new SecretTaint(TaintMode.OBSERVE, TaintObserver.NOOP);
    observe.onSource(new String(PAN));
    assertThat(observe.sanitizeError("x " + PAN)).isEqualTo("x " + PAN);
  }

  @Test
  public void withoutARevealTextIsUnchanged() throws Exception {
    Run run = enforce("print('bad card: ' + '" + PAN + "')\n");
    assertThat(run.printed).containsExactly("bad card: " + PAN);
    assertThat(run.hits).isEmpty();
  }

  @Test
  public void tagLimitSaturatesTheRun() throws Exception {
    Run run = new Run(TaintMode.ENFORCE, 2).exec(REVEAL + "a = s + 'x'\nb = s + 'y'\nprint('z')\n");
    assertThat(run.taint.isSaturated()).isTrue();
    assertThat(run.printed).containsExactly(SecretTaint.REDACTED);
    assertThat(run.hits).containsExactly("PRINT:SATURATED");
  }

  @Test
  public void observerFailureSaturatesAndDoesNotEscape() {
    SecretTaint taint =
        new SecretTaint(
            TaintMode.ENFORCE,
            (sink, reason) -> {
              throw new IllegalStateException("observer down");
            });
    Object tagged = taint.onSource(new String(PAN));
    assertThat(taint.checkOutput(SecretTaint.Sink.PRINT, tagged))
        .isEqualTo(SecretTaint.Reason.TAGGED);
    assertThat(taint.isSaturated()).isTrue();
  }

  @Test
  public void scriptErrorsArePreserved() {
    Run run = new Run(TaintMode.ENFORCE);
    EvalException e = assertThrows(EvalException.class, () -> run.exec(REVEAL + "fail('nope')\n"));
    assertThat(e).hasMessageThat().contains("nope");
  }

  @Test
  public void concurrentRunsDoNotShareTags() throws Exception {
    int threads = 8;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Run>> futures = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      boolean reveals = i % 2 == 0;
      futures.add(
          pool.submit(
              () -> {
                start.await();
                return enforce((reveals ? REVEAL + "x = s\n" : "x = 'public'\n") + "print(x)\n");
              }));
    }
    start.countDown();
    pool.shutdown();
    for (int i = 0; i < threads; i++) {
      Run run = futures.get(i).get();
      if (i % 2 == 0) {
        assertThat(run.printed).containsExactly(SecretTaint.REDACTED);
      } else {
        assertThat(run.printed).containsExactly("public");
        assertThat(run.taint.isActive()).isFalse();
      }
    }
  }

  @Test
  public void aSharedStringLiteralTaggedInOneRunIsCleanInAnother() throws Exception {
    Run first = enforce(REVEAL + "r = 'shared literal'.replace(s, 'x')\n");
    Run second = enforce("print('shared literal')\n");
    assertThat(first.taint.isActive()).isTrue();
    assertThat(second.printed).containsExactly("shared literal");
  }
}
