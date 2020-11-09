package com.verygood.security.larky.jsr223;

import static org.junit.Assert.assertEquals;

import net.starlark.java.eval.Module;

import org.junit.Test;

import java.util.List;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class LarkyScriptEngineTest {

  @Test
  public void testCompile_String() throws ScriptException {
    ScriptEngine instance;
    synchronized (this) {
       ScriptEngineManager manager = new ScriptEngineManager();
       instance = manager.getEngineByName("Larky");
    }
    assert instance != null;
//    String script = String.join("\n",
//        "def ")
    String script =
       "def norman_window(x, y)\n" +
          "return get_area(x, y), get_perimeter(x, y)\n" +
       "end\n" +
       "def get_area(x, y)\n" +
         "x * y + Math::PI / 8.0 * x ** 2.0\n" +
       "end\n" +
       "def get_perimeter(x, y)\n" +
         "x + 2.0 * y + Math::PI / 2.0 * x\n" +
       "end\n" +
       "output = norman_window(2, 1)";
    CompiledScript cs = ((Compilable)instance).compile(script);
    Module evaluatedResult = (Module) cs.eval();
    @SuppressWarnings("unchecked") List<Double> result = (List<Double>) evaluatedResult.getGlobal("output");
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