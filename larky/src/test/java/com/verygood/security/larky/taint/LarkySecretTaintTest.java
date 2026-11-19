package com.verygood.security.larky.taint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.verygood.security.larky.jsr223.LarkyCompiledScript;
import com.verygood.security.larky.jsr223.LarkyEvaluationScriptException;
import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import net.starlark.java.eval.SecretTaint;
import net.starlark.java.eval.TaintMode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** Secret taint through real Larky modules, and at the print sink. */
class LarkySecretTaintTest {

  private static final String FAKE_PAN = "4111111111111111";

  private static final String PRELUDE =
      "load('@vgs//vault', vault='vault')\n"
          + "load('@stdlib//json', 'json')\n"
          + "load('@stdlib//base64', 'base64')\n"
          + "load('@stdlib//binascii', 'binascii')\n"
          + "load('@stdlib//hashlib', 'hashlib')\n"
          + "load('@vendor//Crypto/Hash/HMAC', HMAC='HMAC')\n"
          + "load('@vendor//Crypto/Hash/SHA256', SHA256='SHA256')\n"
          + "load('@vendor//Crypto/Cipher/AES', AES='AES')\n"
          + "load('@vendor//Crypto/Util/Padding', pad='pad', unpad='unpad')\n"
          + "load('@vgs//nts', 'nts')\n"
          + "pan = vault.reveal(vault.redact('" + FAKE_PAN + "'))\n"
          + "pan_bytes = bytes(pan, encoding='utf-8')\n"
          + "aes_key = b'0123456789abcdef'\n"
          + "aes_iv = b'fedcba9876543210'\n"
          + "def aes_round_trip():\n"
          + "    ct = AES.new(aes_key, AES.MODE_CBC, aes_iv).encrypt(pad(pan_bytes, AES.block_size))\n"
          + "    return unpad(AES.new(aes_key, AES.MODE_CBC, aes_iv).decrypt(ct), AES.block_size)\n"
          + "def hmac_hex():\n"
          + "    h = HMAC.new(b'fake-key', digestmod=SHA256)\n"
          + "    h.update(pan_bytes)\n"
          + "    return h.hexdigest()\n"
          + "def sha_hex():\n"
          + "    h = hashlib.sha256()\n"
          + "    h.update(pan_bytes)\n"
          + "    return h.hexdigest()\n";

  private static final class Result {
    SecretTaint taint;
    ParsedStarFile file;
    String output;
    final List<String> hits = new ArrayList<>();
  }

  private static Result run(TaintMode mode, String body) throws Exception {
    Result result = new Result();
    result.taint =
        new SecretTaint(mode, (sink, reason) -> result.hits.add(sink + ":" + reason));
    Bindings bindings = new SimpleBindings();
    bindings.put(SecretTaint.BINDING_NAME, result.taint);
    ScriptContext context = new SimpleScriptContext();
    context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
    StringWriter out = new StringWriter();
    context.setWriter(out);
    LarkyCompiledScript script = (LarkyCompiledScript) new LarkyScriptEngine().compile(PRELUDE + body);
    result.file = (ParsedStarFile) script.eval(context);
    result.output = out.toString();
    return result;
  }

  private static Object global(Result r, String name) {
    return r.file.getGlobalEnvironmentVariable(name, Object.class);
  }

  private static final String[][] DERIVATIONS = {
    {"json.dumps", "json.dumps({'pan': pan})"},
    {"base64", "base64.b64encode(pan_bytes)"},
    {"binascii", "binascii.hexlify(pan_bytes)"},
    {"hashlib", "hashlib.sha256(pan_bytes).hexdigest()"},
    {"hashlib update", "sha_hex()"},
    {"hmac", "HMAC.new(b'fake-key', pan_bytes, SHA256).hexdigest()"},
    {"hmac update", "hmac_hex()"},
    {"aes", "aes_round_trip()"},
    {"percent", "'%s' % pan"},
    {"format", "'{}'.format(pan)"},
    {"reversed", "''.join(reversed(pan.elems()))"},
    {"spelled", "'-'.join([c for c in pan.elems()])"},
  };

  @TestFactory
  Stream<DynamicTest> derivedValuesAreTainted() {
    return Stream.of(DERIVATIONS)
        .map(
            d ->
                DynamicTest.dynamicTest(
                    d[0],
                    () -> {
                      Result r = run(TaintMode.ENFORCE, "r = " + d[1] + "\nprint(r)\n");
                      assertTrue(r.taint.isTainted(global(r, "r")), d[0]);
                      assertFalse(r.taint.isSaturated(), d[0]);
                      assertFalse(r.output.contains(FAKE_PAN), d[0]);
                      assertTrue(r.output.contains(SecretTaint.REDACTED), d[0]);
                    }));
  }

  @Test
  void searchingArgumentsDoesNotConsumeAnIterator() throws Exception {
    Result r =
        run(
            TaintMode.ENFORCE,
            "def main():\n"
                + "    it = iter(['a', 'b', 'c'])\n"
                + "    return [next(it), next(it), next(it)]\n"
                + "r = str(main())\n");
    assertEquals("[\"a\", \"b\", \"c\"]", global(r, "r"));
    assertFalse(r.taint.isSaturated());
  }

  @Test
  void aTaintedIteratorsJavaNextMethodCarriesTheSecret() throws Exception {
    Result r = run(TaintMode.ENFORCE, "c = iter(pan).__next__()\nprint(c)\n");
    assertTrue(r.taint.isTainted(global(r, "c")));
    assertEquals(SecretTaint.REDACTED, r.output.strip());
  }

  @Test
  void aValueRevealedWhileLoadingAModuleStaysTaintedOnTheMainThread() throws Exception {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, (sink, reason) -> {});
    LarkyScriptEngine engine = new LarkyScriptEngine();
    SimpleScriptContext context = new SimpleScriptContext();
    context.setAttribute(
        LarkyScriptEngine.MODULES,
        Map.of(
            "helper.star",
            "load('@vgs//vault', vault='vault')\n"
                + "SECRET = vault.reveal(vault.redact('" + FAKE_PAN + "'))\n"),
        ScriptContext.ENGINE_SCOPE);
    context.setAttribute(LarkyScriptEngine.SCRIPT_NAME, "host.star", ScriptContext.ENGINE_SCOPE);
    StringWriter out = new StringWriter();
    context.setWriter(out);
    engine.setContext(context);
    Bindings bindings = new SimpleBindings();
    bindings.put(SecretTaint.BINDING_NAME, taint);
    engine.eval("load('helper', 'SECRET')\nprint(SECRET)\n", bindings);
    assertEquals(SecretTaint.REDACTED, out.toString().strip());
    assertFalse(out.toString().contains(FAKE_PAN));
  }

  @Test
  void hashingUnrelatedDataIsNotTainted() throws Exception {
    Result r = run(TaintMode.ENFORCE, "r = hashlib.sha256(b'public').hexdigest()\nprint(r)\n");
    assertFalse(r.taint.isTainted(global(r, "r")));
    assertFalse(r.taint.isSecretDependent());
    assertEquals(global(r, "r"), r.output.strip());
  }

  @Test
  void aModuleIsNotTaintedByItsArguments() throws Exception {
    Result r =
        run(
            TaintMode.ENFORCE,
            "a = hashlib.sha256(pan_bytes).hexdigest()\n"
                + "r = hashlib.sha256(b'public').hexdigest()\n");
    assertTrue(r.taint.isTainted(global(r, "a")));
    assertFalse(r.taint.isTainted(global(r, "r")));
  }

  @TestFactory
  Stream<DynamicTest> scriptResultIsTheSameInEveryMode() {
    String body = "r = base64.b64encode(pan_bytes).decode('utf-8')\nprint(r)\n";
    return Stream.of(TaintMode.OBSERVE, TaintMode.ENFORCE)
        .map(
            mode ->
                DynamicTest.dynamicTest(
                    mode.name(),
                    () -> assertEquals(global(run(TaintMode.OFF, body), "r"), global(run(mode, body), "r"))));
  }

  @Test
  void observeKeepsOutputAndCounts() throws Exception {
    Result r = run(TaintMode.OBSERVE, "print(pan)\n");
    assertTrue(r.output.contains(FAKE_PAN));
    assertEquals(List.of("PRINT:TAGGED"), r.hits);
  }

  @Test
  void enforceRedactsPrint() throws Exception {
    Result r = run(TaintMode.ENFORCE, "print(pan)\nr = 'done'\n");
    assertFalse(r.output.contains(FAKE_PAN));
    assertTrue(r.output.contains(SecretTaint.REDACTED));
    assertEquals("done", global(r, "r"));
  }

  @Test
  void branchingOnASecretRedactsLaterPrints() throws Exception {
    Result r =
        run(
            TaintMode.ENFORCE,
            "def main():\n"
                + "    if pan.startswith('4'):\n"
                + "        print('visa')\n"
                + "main()\n");
    assertEquals(SecretTaint.REDACTED, r.output.strip());
  }

  @Test
  void aLaunderedValueIsCaughtByTheValueScan() throws Exception {
    Result r =
        run(
            TaintMode.ENFORCE,
            "print('k=" + FAKE_PAN.substring(0, 8) + "')\n"
                + "print('k=' + base64.b64encode(b'" + FAKE_PAN + "').decode('utf-8'))\n");
    assertEquals(
        "k=" + FAKE_PAN.substring(0, 8) + "\nk=" + SecretTaint.REDACTED, r.output.strip());
    assertEquals(List.of("PRINT:VALUE_MATCH"), r.hits);
  }

  @Test
  void runThatNeverRevealsIsUntouchedEvenWhenEnforcing() throws Exception {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, (s, why) -> {});
    Bindings bindings = new SimpleBindings();
    bindings.put(SecretTaint.BINDING_NAME, taint);
    ScriptContext context = new SimpleScriptContext();
    context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
    StringWriter out = new StringWriter();
    context.setWriter(out);
    LarkyCompiledScript script =
        (LarkyCompiledScript) new LarkyScriptEngine().compile("print('fine')\nx = len('abc')\n");
    script.eval(context);
    assertFalse(taint.isActive());
    assertEquals("fine", out.toString().strip());
  }

  @Test
  void ntsBehavesIdenticallyWhenEnforcing() throws Exception {
    String body =
        "def main():\n"
            + "    return nts.supports_dcvv({'pan': vault.redact('" + FAKE_PAN + "')}, '$.pan')\n"
            + "r = main()\n";
    Result off = run(TaintMode.OFF, body);
    Result enforced = run(TaintMode.ENFORCE, body);
    assertEquals(global(off, "r"), global(enforced, "r"));
    assertEquals(Boolean.TRUE, global(enforced, "r"));
    assertTrue(enforced.taint.isSecretDependent());
    assertEquals(List.of(), enforced.hits);
  }

  @Test
  void aWrappedTaggedErrorRequiresWholeMessageMasking() throws Exception {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, (s, why) -> {});
    Bindings bindings = new SimpleBindings();
    bindings.put(SecretTaint.BINDING_NAME, taint);
    ScriptContext context = new SimpleScriptContext();
    context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
    context.setWriter(new StringWriter());
    LarkyCompiledScript script =
        (LarkyCompiledScript)
            new LarkyScriptEngine()
                .compile(
                    "load('@vgs//vault', vault='vault')\n"
                        + "fail('bad card ' + vault.reveal(vault.redact('" + FAKE_PAN + "')))\n");
    LarkyEvaluationScriptException e =
        assertThrows(LarkyEvaluationScriptException.class, () -> script.eval(context));
    assertTrue(e.getMessage().contains(FAKE_PAN));
    assertEquals(SecretTaint.REDACTED, taint.sanitizeError(e.toString()));
  }

  @Test
  void aTaggedValueInALineIsMaskedInPlace() throws Exception {
    Result r = run(TaintMode.ENFORCE, "print('bad card: ' + pan)\n");
    assertEquals("bad card: ***REDACTED***", r.output.strip());
  }

  @Test
  void encodedFormsInAnUntaggedLineAreMasked() throws Exception {
    Result r =
        run(
            TaintMode.ENFORCE,
            "print('b64=' + 'NDExMTExMTExMTExMTExMQ' + ' hex=' + '34313131313131313131313131313131'.upper()"
                + " + ' url=' + '" + FAKE_PAN + "')\n");
    assertEquals("b64=***REDACTED*** hex=***REDACTED*** url=***REDACTED***", r.output.strip());
  }

  @Test
  void aTransformedTaggedArgumentIsMaskedWhole() throws Exception {
    Result r = run(TaintMode.ENFORCE, "print('card', ''.join(reversed(pan.elems())), 'ok')\n");
    assertEquals("card ***REDACTED*** ok", r.output.strip());
  }

  @Test
  void aSecretDependentBranchRedactsTheWholeLine() throws Exception {
    Result r =
        run(
            TaintMode.ENFORCE,
            "def main():\n"
                + "    if pan.startswith('4'):\n"
                + "        print('visa', 'line')\n"
                + "main()\n");
    assertEquals(SecretTaint.REDACTED, r.output.strip());
  }

  @Test
  void observeLeavesTextUnchangedButReports() throws Exception {
    Result r = run(TaintMode.OBSERVE, "print('bad card: ' + pan)\n");
    assertEquals("bad card: " + FAKE_PAN, r.output.strip());
    assertEquals(List.of("PRINT:TAGGED"), r.hits);
  }

  @Test
  void withoutARevealTextIsUnchangedWhenEnforcing() throws Exception {
    SecretTaint taint = new SecretTaint(TaintMode.ENFORCE, (s, why) -> {});
    Bindings bindings = new SimpleBindings();
    bindings.put(SecretTaint.BINDING_NAME, taint);
    ScriptContext context = new SimpleScriptContext();
    context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
    StringWriter out = new StringWriter();
    context.setWriter(out);
    ((LarkyCompiledScript)
            new LarkyScriptEngine().compile("print('bad card: " + FAKE_PAN + "')\n"))
        .eval(context);
    assertEquals("bad card: " + FAKE_PAN, out.toString().strip());
  }
}
