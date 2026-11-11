package com.verygood.security.larky.jsr223;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
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
}
