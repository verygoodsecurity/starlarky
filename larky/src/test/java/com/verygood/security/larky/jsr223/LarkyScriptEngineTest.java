package com.verygood.security.larky.jsr223;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.verygood.security.larky.parser.ParsedStarFile;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.StarlarkFloat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

public class LarkyScriptEngineTest {

  private ScriptEngine instance;

  @Before
  public void setUp() {
    synchronized (this) {
      ScriptEngineManager manager = new ScriptEngineManager();
      instance = manager.getEngineByName("Larky");
    }

  }

  @After
  public void tearDown() {
    instance.getBindings(ScriptContext.ENGINE_SCOPE).clear();
    instance = null;
  }

  @Test
  public void testCompile_String() throws ScriptException, EvalException {
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
    CompiledScript cs = ((Compilable) instance).compile(script);
    ParsedStarFile evaluatedResult = (ParsedStarFile) cs.eval();
    Sequence<StarlarkFloat> output = Sequence.cast(
      evaluatedResult.getGlobalEnvironmentVariable("output", Sequence.class),
      StarlarkFloat.class,
      "Error casting output to expected type of Sequence<StarlarkFloat>.");
    List<Double> result = output.stream().map(StarlarkFloat::toDouble).collect(Collectors.toList());
    assertEquals(3.570796327, result.get(0), 0.000001);
    assertEquals(7.141592654, result.get(1), 0.000001);
  }

  @Test
  public void testCompile() {
    assertNotNull(instance);
  }

  @Test
  public void eval() {
    assertNotNull(instance);

  }

  @Test
  public void testEval() {
    assertNotNull(instance);
  }

  @Test
  public void testEval1() {
    assertNotNull(instance);
  }

  @Test
  public void testEval2() {
    assertNotNull(instance);
  }

  @Test
  public void testEval3() {
    assertNotNull(instance);
  }

  @Test
  public void testEval4() {
    assertNotNull(instance);
  }

  @Test
  public void testPut() {
    String key = "";
    Object value = null;
    assertNotNull(instance);

    try {
      instance.put(key, value);
    } catch (IllegalArgumentException e) {
      String expResult = "key can not be empty";
      assertEquals(expResult, e.getMessage());
    }
  }

  @Test
  public void testGet() {
    assertNotNull(instance);
    instance.put("abc", "aabc");
    instance.put("_abc", "abbc");
    instance.put("abc_", "abcc");

    String key = "abc";
    Object expResult = "aabc";
    Object result = instance.get(key);
    assertEquals(expResult, result);

    List<String> list = new ArrayList<>();
    list.add("aabc");
    instance.put(key, list);

    Map<String, String> map = new HashMap<>();
    map.put("Larky", "IsPythonLike");
    instance.put("abc_", map);

    result = instance.get(key);
    assertEquals(expResult, ((List<String>) result).get(0));

    key = "abc_";
    expResult = "IsPythonLike";
    result = instance.get(key);
    assertEquals(expResult, ((Map<String, String>) result).get("Larky"));

  }

  @Test
  public void testGetBindings() throws ScriptException {
    assertNotNull(instance);
    instance.eval("load(\"@stdlib/math\", \"math\")");
    instance.eval("p = 9.0");
    instance.eval("q = math.sqrt(p)");
    Double expResult = 9.0;
    int scope = ScriptContext.ENGINE_SCOPE;
    Bindings result = instance.getBindings(scope);
    assertEquals(expResult, Double.parseDouble(result.get("p").toString()), 0.01);
    expResult = 3.0;
    assertEquals(expResult, Double.parseDouble(result.get("q").toString()), 0.01);

    scope = ScriptContext.GLOBAL_SCOPE;
    result = instance.getBindings(scope);
    assertTrue(result instanceof SimpleBindings);
    assertEquals(0, result.size());

  }

  @Test
  public void setBindings() throws ScriptException {
    assertNotNull(instance);

    String script =
      "def message():\n" +
        "    return \"message: {}\".format(amessage)\n" +
        "\n" +
        "output = message()\n";
    Bindings bindings = new SimpleBindings();
    bindings.put("amessage", "What's up?");
    int scope = ScriptContext.ENGINE_SCOPE;
    Object expResult = "message: What's up?";
    instance.setBindings(bindings, scope);
    Object result = instance.eval(script);
    assertEquals(expResult, ((ParsedStarFile) result).getGlobalEnvironmentVariable("output", String.class));

  }

  @Test
  public void createBindings() {
    assertNotNull(instance);
    Bindings bindings = instance.getBindings(ScriptContext.ENGINE_SCOPE);
    Bindings result = instance.createBindings();
    assertNotSame(bindings, result);
  }

  @Test
  public void testGetContext() {
    assertNotNull(instance);
    ScriptContext result = instance.getContext();
    assertNotNull(result);
  }

  @Test
  public void testSetContext() {
    assertNotNull(instance);

    ScriptContext ctx = new SimpleScriptContext();
    StringWriter sw = new StringWriter();
    sw.write("Have a great summer!");
    ctx.setWriter(sw);
    instance.setContext(ctx);
    ScriptContext result = instance.getContext();
    Writer w = result.getWriter();
    Object expResult = "Have a great summer!";
    assertSame(sw, result.getWriter());
    assertEquals(expResult, (result.getWriter()).toString());
  }

  @Test
  public void testGetFactory() {
    assertNotNull(instance);

    ScriptEngineFactory result = instance.getFactory();
    assertTrue(result instanceof LarkyScriptEngineFactory);
    String expResult = "Larky ScriptEngine";
    String ret = result.getEngineName();
    assertEquals(expResult, ret);
  }
}