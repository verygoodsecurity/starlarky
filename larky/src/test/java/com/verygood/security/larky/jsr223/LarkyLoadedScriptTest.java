package com.verygood.security.larky.jsr223;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.verygood.security.larky.parser.LarkyEvaluator;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;
import org.junit.Test;

public class LarkyLoadedScriptTest {

  /** A service a call is handled with, given per call. */
  @StarlarkBuiltin(name = "TestVault", documented = false)
  public static class TestVault implements StarlarkValue {
    private final String prefix;

    TestVault(String prefix) {
      this.prefix = prefix;
    }

    @StarlarkMethod(name = "reveal", documented = false, parameters = {@Param(name = "value")})
    public String reveal(String value) {
      return prefix + value;
    }
  }

  private static LarkyCompiledScript compile(String... lines) throws ScriptException {
    return (LarkyCompiledScript) new LarkyScriptEngine().compile(String.join("\n", lines) + "\n");
  }

  private static ScriptContext context(StringWriter out, Map<String, Object> bindings) {
    final ScriptContext context = new SimpleScriptContext();
    context.setWriter(out);
    final Bindings engine = new SimpleBindings();
    engine.putAll(bindings);
    context.setBindings(engine, ScriptContext.ENGINE_SCOPE);
    return context;
  }

  /** A loaded script gets nothing of the host's: a name it reads from the host must be declared per call. */
  @Test
  public void aScriptThatReadsAHostNameNotDeclaredPerCallIsNotLoaded() throws Exception {
    final LarkyCompiledScript script = compile(
        "def process():",
        "    return request_pan");

    final LarkyScriptNotLoadableException e = assertThrows(LarkyScriptNotLoadableException.class,
        () -> script.load(context(new StringWriter(), Map.of("request_pan", "4111-LOADTIME")), Set.of()));
    assertTrue(e.getMessage(), e.getMessage().contains("request_pan"));
  }

  /**
   * A value bound at load never reaches a call, so data of one request cannot reach another: each call sees only its
   * own per-call value, and a call without one has none.
   */
  @Test
  public void aLoadTimeBindingNeverReachesACall() throws Exception {
    final LarkyLoadedScript loaded = compile(
        "def process():",
        "    return request_pan.upper()")
        .load(context(new StringWriter(), Map.of("request_pan", "4111-LOADTIME")), Set.of("request_pan"));

    assertEquals("A", loaded.invoke("process", Map.of("request_pan", "a")));
    assertEquals("B", loaded.invoke("process", Map.of("request_pan", "b")));
    final Exception none = assertThrows(Exception.class, () -> loaded.invoke("process", Map.of()));
    assertFalse(none.getMessage(), none.getMessage().contains("4111-LOADTIME"));
  }

  @Test
  public void callsAFunctionManyTimesAndRunsTheTopLevelOnce() throws Exception {
    final StringWriter out = new StringWriter();
    final LarkyLoadedScript loaded = compile(
        "print('top level')",
        "GREETING = 'hello '",
        "def process(input, ctx):",
        "    return GREETING + input + ctx['who']")
        .load(context(out, Map.of()), Set.of());

    for (int i = 0; i < 3; i++) {
      assertEquals("hello a b", loaded.invokeFunction("process", "a ", Map.of("who", "b")));
    }
    assertEquals("top level\n", out.toString());
  }

  @Test
  public void loadWritesNothingBackToTheBindings() throws Exception {
    final ScriptContext context = context(new StringWriter(), Map.of("given", "x"));

    compile("defined = 1").load(context, Set.of());

    assertEquals(Set.of("given"), context.getBindings(ScriptContext.ENGINE_SCOPE).keySet());
  }

  @Test
  public void aPerCallNameReachesTheValueOfItsCall() throws Exception {
    final LarkyLoadedScript loaded = compile(
        "def process(input, ctx):",
        "    return vault.reveal(input)")
        .load(context(new StringWriter(), Map.of()), Set.of("vault"));

    assertEquals("a:x", loaded.invoke("process", Map.of("vault", new TestVault("a:")), "x", Map.of()));
    assertEquals("b:x", loaded.invoke("process", Map.of("vault", new TestVault("b:")), "x", Map.of()));
    assertEquals(Set.of("vault"), loaded.perCallNames());
  }

  @Test
  public void concurrentCallsEachSeeTheirOwnPerCallValues() throws Exception {
    final LarkyLoadedScript loaded = compile(
        "def process(input, ctx):",
        "    return vault.reveal(input)")
        .load(context(new StringWriter(), Map.of()), Set.of("vault"));
    final ExecutorService pool = Executors.newFixedThreadPool(8);
    try {
      final List<Future<Object>> results = new ArrayList<>();
      for (int i = 0; i < 400; i++) {
        final String prefix = i + ":";
        results.add(pool.submit(() -> loaded.invoke("process", Map.of("vault", new TestVault(prefix)), "v", Map.of())));
      }
      for (int i = 0; i < results.size(); i++) {
        assertEquals(i + ":v", results.get(i).get());
      }
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  public void aCallInsideACallKeepsTheOuterCallsValues() throws Exception {
    final LarkyLoadedScript[] loaded = new LarkyLoadedScript[1];
    // a vault that calls the script again, with another vault, before it answers
    final TestVault reentrant = new TestVault("outer:") {
      @Override
      public String reveal(String value) {
        try {
          return loaded[0].invoke("inner", Map.of("vault", new TestVault("inner:")), value) + "|" + super.reveal(value);
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
      }
    };
    loaded[0] = compile(
        "def inner(value):",
        "    return vault.reveal(value)",
        "def outer(value):",
        "    first = vault.reveal(value)",
        "    return first + '|' + vault.reveal('after')")
        .load(context(new StringWriter(), Map.of()), Set.of("vault"));

    assertEquals("inner:x|outer:x|inner:after|outer:after",
        loaded[0].invoke("outer", Map.of("vault", reentrant), "x"));
  }

  @Test
  public void aPerCallNameHasNoValueAtTheTopLevel() throws Exception {
    final LarkyCompiledScript script = compile("revealed = vault.reveal('x')");

    final ScriptException e = assertThrows(ScriptException.class,
        () -> script.load(context(new StringWriter(), Map.of()), Set.of("vault")));
    assertTrue(e.getMessage(), e.getMessage().contains("vault has a value only while a function of the script is called"));
  }

  @Test
  public void aPerCallNameLeftOutOfACallHasNoValueInIt() throws Exception {
    final LarkyLoadedScript loaded = compile(
        "def process(input):",
        "    return vault.reveal(input)")
        .load(context(new StringWriter(), Map.of()), Set.of("vault"));

    final ScriptException e = assertThrows(ScriptException.class, () -> loaded.invokeFunction("process", "x"));
    assertTrue(e.getMessage(), e.getMessage().contains("vault has a value only while"));
  }

  @Test
  public void refusesAScriptWhoseTopLevelLeavesAMutableValue() throws Exception {
    final LarkyCompiledScript script = compile(
        "load('@stdlib//larky', 'larky')",
        "kept = larky.mutablestruct(a = 1)",
        "def process(input):",
        "    return input");

    final LarkyScriptNotLoadableException e = assertThrows(LarkyScriptNotLoadableException.class,
        () -> script.load(context(new StringWriter(), Map.of()), Set.of()));
    assertTrue(e.getMessage(), e.getMessage().contains("kept"));
  }

  @Test
  public void frozenListsAndDictsAreImmutable() throws Exception {
    final LarkyLoadedScript loaded = compile(
        "NAMES = ['a', 'b']",
        "BY_NAME = {'a': 1}",
        "def process(input):",
        "    return BY_NAME.get(input, len(NAMES))")
        .load(context(new StringWriter(), Map.of()), Set.of());

    assertEquals(StarlarkInt.of(1), loaded.invokeFunction("process", "a"));
    final ScriptException e = assertThrows(ScriptException.class, () -> compile(
        "NAMES = ['a']",
        "def process(input):",
        "    NAMES.append(input)")
        .load(context(new StringWriter(), Map.of()), Set.of())
        .invokeFunction("process", "b"));
    assertTrue(e.getMessage(), e.getMessage().contains("frozen"));
  }

  @Test
  public void refusesAScriptThatDefinesAPerCallName() throws Exception {
    final LarkyCompiledScript script = compile("vault = 1");

    assertThrows(LarkyScriptNotLoadableException.class,
        () -> script.load(context(new StringWriter(), Map.of()), Set.of("vault")));
  }

  @Test
  public void aFailingTopLevelIsReportedAsEvalReportsIt() throws Exception {
    final ScriptException loading = assertThrows(ScriptException.class,
        () -> compile("fail('boom')").load(context(new StringWriter(), Map.of()), Set.of()));
    final ScriptException evaluating = assertThrows(ScriptException.class,
        () -> compile("fail('boom')").eval(context(new StringWriter(), Map.of())));

    assertEquals(evaluating.getMessage(), loading.getMessage());
  }

  @Test
  public void aFailingCallIsReportedWithWhereItFailed() throws Exception {
    final LarkyLoadedScript loaded = compile(
        "def process(input):",
        "    fail('bad ' + input)")
        .load(context(new StringWriter(), Map.of()), Set.of());

    final LarkyEvaluationScriptException e = assertThrows(LarkyEvaluationScriptException.class,
        () -> loaded.invokeFunction("process", "x"));
    assertTrue(e.getMessage(), e.getMessage().contains("bad x"));
    assertTrue(e.getMessage(), e.getMessage().contains("line number 2"));
  }

  @Test
  public void invokeRefusesANameThatIsNotPerCall() throws Exception {
    final LarkyLoadedScript loaded = compile("def process(input):\n    return input")
        .load(context(new StringWriter(), Map.of()), Set.of());

    assertThrows(IllegalArgumentException.class, () -> loaded.invoke("process", Map.of("vault", "v"), "x"));
  }

  @Test
  public void callingAFunctionTheScriptDoesNotDefineThrowsNoSuchMethod() throws Exception {
    final LarkyLoadedScript loaded = compile("NOT_A_FUNCTION = 1")
        .load(context(new StringWriter(), Map.of()), Set.of());

    assertFalse(loaded.defines("NOT_A_FUNCTION"));
    assertThrows(NoSuchMethodException.class, () -> loaded.invokeFunction("NOT_A_FUNCTION"));
    assertThrows(NoSuchMethodException.class, () -> loaded.invokeFunction("missing"));
  }

  @Test
  public void theLoadBindingsStepLimitAppliesToEachCall() throws Exception {
    final LarkyLoadedScript loaded = compile(
        "def spin(n):",
        "    total = 0",
        "    for i in range(n):",
        "        total += i",
        "    return total")
        .load(context(new StringWriter(), Map.of(LarkyEvaluator.STEP_LIMIT, 1000)), Set.of());

    assertEquals(StarlarkInt.of(45), loaded.invokeFunction("spin", 10));
    assertThrows(ScriptException.class, () -> loaded.invokeFunction("spin", 100_000));
  }

  @Test
  public void aCallExpiresAtTheDateItIsGiven() throws Exception {
    final LarkyLoadedScript loaded = compile(
        "def spin(n):",
        "    total = 0",
        "    for i in range(n):",
        "        total += i",
        "    return total")
        .load(context(new StringWriter(), Map.of()), Set.of());

    assertEquals(StarlarkInt.of(45),
        loaded.invoke("spin", Map.of(LarkyEvaluator.EXPIRATION_MS, System.currentTimeMillis() + 60_000), 10));
    assertThrows(ScriptException.class,
        () -> loaded.invoke("spin", Map.of(LarkyEvaluator.EXPIRATION_MS, 1L), 10_000_000));
  }

  /** What a host calls a script through. */
  public interface Greeter {
    String greet(String name);

    int twice(int n);

    default String greetTwice(String name) {
      return greet(name) + greet(name);
    }
  }

  /** A host interface the script does not implement in full. */
  public interface Partial {
    String greet(String name);

    String missing();
  }

  @Test
  public void getInterfaceCallsTheFunctionsOfTheSameName() throws Exception {
    final LarkyLoadedScript loaded = compile(
        "def greet(name):",
        "    return 'hi ' + name",
        "def twice(n):",
        "    return n * 2")
        .load(context(new StringWriter(), Map.of()), Set.of());

    final Greeter greeter = loaded.getInterface(Greeter.class);
    assertEquals("hi a", greeter.greet("a"));
    assertEquals(42, greeter.twice(21));
    assertEquals("hi bhi b", greeter.greetTwice("b"));
    assertNull(loaded.getInterface(Partial.class));
  }

  @Test
  public void invokeMethodCallsAMethodOfAValue() throws Exception {
    final LarkyLoadedScript loaded = compile("NAME = 'larky'")
        .load(context(new StringWriter(), Map.of()), Set.of());

    assertEquals("LARKY", loaded.invokeMethod("larky", "upper"));
    assertThrows(NoSuchMethodException.class, () -> loaded.invokeMethod("larky", "missing"));
  }

  @Test
  public void predeclaredNamesAreTheNamesTheScriptReadsFromItsBindings() throws Exception {
    final LarkyCompiledScript script = compile(
        "load('@stdlib//json', 'json')",
        "prefix = header",
        "def process(value):",
        "    return json.dumps([prefix, value, ctx, len(value)])");

    final Set<String> names = script.predeclaredNames();

    assertTrue(names.toString(), names.containsAll(Set.of("header", "ctx")));
    assertFalse(names.toString(), names.contains("prefix"));
    assertFalse(names.toString(), names.contains("value"));
    assertFalse(names.toString(), names.contains("json"));
    assertFalse(names.toString(), names.contains("len"));
  }
}
