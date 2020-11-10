package com.verygood.security.larky.jsr223;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.StarlarkFloat;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class LarkyScriptEngineTest {

  @Test
  public void testCompile_String() throws ScriptException, EvalException {
    ScriptEngine instance;
    synchronized (this) {
       ScriptEngineManager manager = new ScriptEngineManager();
       instance = manager.getEngineByName("Larky");
    }
    assertNotNull(instance);
    String script = String.join("\n",
        "",
        String.format("load(\"@stdlib/%1$s\", \"%1$s\")", "math"),
        "",
        "def norman_window(x, y):",
        "    return get_area(x, y), get_perimeter(x, y)",
        "",
        "def get_area(x, y):",
        "    return x * y + math.pi / 8.0 * math.pow(x, 2.0)",
        "",
        "def get_perimeter(x, y):",
        "    return x + 2.0 * y + math.pi / 2.0 * x",
        "",
        "output = norman_window(2, 1)"
    );
    CompiledScript cs = ((Compilable)instance).compile(script);
    Module evaluatedResult = (Module) cs.eval();
    Sequence<StarlarkFloat> output = Sequence.cast(
        evaluatedResult.getGlobal("output"),
        StarlarkFloat.class,
        "Error casting output to expected type of Sequence<StarlarkFloat>.");
    List<Double> result = output.stream().map(StarlarkFloat::toDouble).collect(Collectors.toList());
    assertEquals(3.570796327, result.get(0), 0.000001);
    assertEquals(7.141592654, result.get(1), 0.000001);
    instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
  }

  @Test
  public void testCompile() {
  }

  @Test
  public void eval() {
  }

  @Test
  public void testEval() {
  }

  @Test
  public void testEval1() {
  }

  @Test
  public void testEval2() {
  }

  @Test
  public void testEval3() {
  }

  @Test
  public void testEval4() {
  }

  @Test
  public void put() {
  }

  @Test
  public void get() {
  }

  @Test
  public void getBindings() {
  }

  @Test
  public void setBindings() {
  }

  @Test
  public void createBindings() {
  }

  @Test
  public void getContext() {
  }

  @Test
  public void setContext() {
  }

  @Test
  public void getFactory() {
  }
}