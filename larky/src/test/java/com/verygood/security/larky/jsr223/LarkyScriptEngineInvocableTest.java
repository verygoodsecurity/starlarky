package com.verygood.security.larky.jsr223;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.script.ScriptContext;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import net.starlark.java.eval.StarlarkInt;
import org.junit.Test;

public class LarkyScriptEngineInvocableTest {

  /** What a host calls a script through. */
  public interface Adder {
    int add(int a, int b);
  }

  /** What a host reads a script's results through, as each Java type. */
  public interface Results {
    long count();

    double ratio();

    Integer maybe();

    void nothing();

    String name();
  }

  /** One primitive result. */
  public interface Count {
    long count();
  }

  /** A method of a string. */
  public interface Upper {
    String upper();
  }

  /** Numbers as each Java numeric type. */
  public interface Numbers {
    short small();

    byte tiny();

    double widened();

    float single();

    float fromFloat();
  }

  /** An int result that does not fit the Java type. */
  public interface TooBig {
    int big();
  }

  /** Redeclares Object's equals, as java.util.Comparator does; the script need not define it. */
  public interface Compare {
    int compare(int a, int b);

    @Override
    boolean equals(Object other);
  }

  /** A host interface the script does not implement. */
  public interface Missing {
    void missing();
  }

  private static ScriptEngine evaluated(String script) throws ScriptException {
    final ScriptEngine engine = new LarkyScriptEngineFactory().getScriptEngine();
    engine.eval(script);
    return engine;
  }

  @Test
  public void theEngineIsInvocable() {
    final ScriptEngine engine = new ScriptEngineManager().getEngineByName("larky");
    assertTrue(String.valueOf(engine), engine instanceof Invocable);
  }

  @Test
  public void invokeFunctionCallsAFunctionAnEvalDefined() throws Exception {
    final Invocable engine = (Invocable) evaluated("def add(a, b):\n    return a + b\n");

    assertEquals(StarlarkInt.of(5), engine.invokeFunction("add", 2, 3));
    assertEquals("ab", engine.invokeFunction("add", "a", "b"));
  }

  @Test
  public void invokeFunctionPrintsToTheContextsWriter() throws Exception {
    final ScriptEngine engine = evaluated("def hello(name):\n    print('hello ' + name)\n");
    final StringWriter out = new StringWriter();
    engine.getContext().setWriter(out);

    ((Invocable) engine).invokeFunction("hello", "larky");

    assertEquals("hello larky\n", out.toString());
  }

  @Test
  public void invokeFunctionPrintsSourceLocationWhenEnabled() throws Exception {
    LarkyScriptEngine engine = LarkyScriptEngine.builder().verbose(true).build();
    StringWriter writer = new StringWriter();
    engine.getContext().setWriter(writer);
    engine.eval("def hello():\n    print('hello')\n");

    engine.invokeFunction("hello");

    assertEquals("larky.star:2:10: hello" + System.lineSeparator(), writer.toString());
  }

  @Test
  public void aFunctionNoEvalDefinedIsNoSuchMethod() throws Exception {
    final Invocable engine = (Invocable) evaluated("value = 1\n");

    assertThrows(NoSuchMethodException.class, () -> engine.invokeFunction("missing"));
    assertThrows(NoSuchMethodException.class, () -> engine.invokeFunction("value"));
  }

  @Test
  public void aFailingFunctionIsAScriptException() throws Exception {
    final Invocable engine = (Invocable) evaluated("def boom():\n    fail('boom')\n");

    final ScriptException e = assertThrows(ScriptException.class, () -> engine.invokeFunction("boom"));
    assertTrue(e.getMessage(), e.getMessage().contains("boom"));
  }

  @Test
  public void invokeMethodCallsAMethodOfAValue() throws Exception {
    final Invocable engine = (Invocable) evaluated("value = 1\n");

    assertEquals("LARKY", engine.invokeMethod("larky", "upper"));
    assertThrows(NoSuchMethodException.class, () -> engine.invokeMethod("larky", "missing"));
  }

  @Test
  public void getInterfaceCallsTheFunctionsOfTheSameName() throws Exception {
    final Invocable engine = (Invocable) evaluated("def add(a, b):\n    return a + b\n");

    assertEquals(5, engine.getInterface(Adder.class).add(2, 3));
    assertNull(engine.getInterface(Missing.class));
    assertThrows(IllegalArgumentException.class, () -> engine.getInterface(String.class));
  }

  @Test
  public void getInterfaceConvertsStarlarkResultsToTheReturnTypes() throws Exception {
    final Invocable engine = (Invocable) evaluated(
        "def count():\n    return 3\ndef ratio():\n    return 1.5\ndef maybe():\n    return None\n"
            + "def nothing():\n    return 1\ndef name():\n    return 'larky'\n");

    final Results results = engine.getInterface(Results.class);
    assertEquals(3L, results.count());
    assertEquals(1.5, results.ratio(), 0.0);
    assertNull(results.maybe());
    results.nothing();
    assertEquals("larky", results.name());
  }

  @Test
  public void getInterfaceOfAValueCallsItsMethods() throws Exception {
    final Invocable engine = (Invocable) evaluated("value = 1\n");

    final Upper upper = engine.getInterface("larky", Upper.class);
    assertEquals("LARKY", upper.upper());
    assertNull(engine.getInterface("larky", Missing.class));
  }

  @Test
  public void getInterfaceConvertsNumbersToEveryNumericType() throws Exception {
    final Invocable engine = (Invocable) evaluated(
        "def small():\n    return 300\ndef tiny():\n    return -5\ndef widened():\n    return 7\n"
            + "def single():\n    return 2\ndef fromFloat():\n    return 0.5\ndef big():\n    return 1 << 40\n");

    final Numbers numbers = engine.getInterface(Numbers.class);
    assertEquals((short) 300, numbers.small());
    assertEquals((byte) -5, numbers.tiny());
    assertEquals(7.0, numbers.widened(), 0.0);
    assertEquals(2.0f, numbers.single(), 0.0f);
    assertEquals(0.5f, numbers.fromFloat(), 0.0f);
    // an int that does not fit is an error, not a truncated value
    final TooBig tooBig = engine.getInterface(TooBig.class);
    assertThrows(ArithmeticException.class, () -> {
      tooBig.big();
    });
  }

  @Test
  public void anInterfaceRedeclaringObjectsMethodsIsStillImplemented() throws Exception {
    final Invocable engine = (Invocable) evaluated("def compare(a, b):\n    return a - b\n");

    final Compare compare = engine.getInterface(Compare.class);
    assertEquals(-1, compare.compare(1, 2));
    assertTrue(compare.equals(compare));
  }

  @Test
  public void getInterfaceOfNullIsAnIllegalArgument() throws Exception {
    final Invocable engine = (Invocable) evaluated("value = 1\n");

    assertThrows(IllegalArgumentException.class, () -> {
      engine.getInterface(null, Upper.class);
    });
  }

  @Test
  public void aNoneForAPrimitiveReturnTypeIsAClassCastException() throws Exception {
    final Invocable engine = (Invocable) evaluated("def count():\n    return None\n");

    final Count count = engine.getInterface(Count.class);
    assertThrows(ClassCastException.class, () -> {
      count.count();
    });
  }

  @Test
  public void theStepLimitOfTheBindingsApplies() throws Exception {
    final ScriptEngine engine = evaluated("def spin():\n    for i in range(100000):\n        pass\n"
        + "def quick():\n    return 1\n");
    engine.getBindings(ScriptContext.ENGINE_SCOPE).put("LARKY_EVAL_STEP_LIMIT", 1000L);

    assertEquals(StarlarkInt.of(1), ((Invocable) engine).invokeFunction("quick"));
    final ScriptException e = assertThrows(ScriptException.class, () -> ((Invocable) engine).invokeFunction("spin"));
    assertTrue(e.getMessage(), e.getMessage().toLowerCase().contains("step"));

    engine.getBindings(ScriptContext.ENGINE_SCOPE).remove("LARKY_EVAL_STEP_LIMIT");
    ((Invocable) engine).invokeFunction("spin");
  }

  @Test
  public void theExpirationOfTheBindingsApplies() throws Exception {
    final ScriptEngine engine = evaluated("def spin():\n    for i in range(10000000):\n        pass\n");
    engine.getBindings(ScriptContext.ENGINE_SCOPE).put("LARKY_EVAL_EXPIRATION_MS", System.currentTimeMillis() - 1000);

    final ScriptException e = assertThrows(ScriptException.class, () -> ((Invocable) engine).invokeFunction("spin"));
    assertTrue(e.getMessage(), e.getMessage().toLowerCase().contains("expiration"));
  }

  @Test
  public void severalThreadsCallAtTheSameTime() throws Exception {
    final Invocable engine = (Invocable) evaluated("def add(a, b):\n    return a + b\n");
    final ExecutorService pool = Executors.newFixedThreadPool(8);
    try {
      final List<Future<Object>> futures = new ArrayList<>();
      for (int i = 0; i < 64; i++) {
        final int n = i;
        final Callable<Object> call = () -> engine.invokeFunction("add", n, 1000);
        futures.add(pool.submit(call));
      }
      for (int i = 0; i < 64; i++) {
        assertEquals(StarlarkInt.of(i + 1000), futures.get(i).get());
      }
    } finally {
      pool.shutdownNow();
    }
  }
}
