package com.verygood.security.larky.jsr223;

import static org.junit.Assert.assertEquals;

import net.starlark.java.eval.Module;

import org.junit.Test;

import java.io.StringWriter;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

public class LarkyCompiledScriptTest {

  @Test
  public void testGetEngine() throws ScriptException {
    LarkyScriptEngineFactory factory = new LarkyScriptEngineFactory();
    LarkyScriptEngine engine = (LarkyScriptEngine) factory.getScriptEngine();
    String script = "print(\"Hello World!!!\")";
    LarkyCompiledScript instance = (LarkyCompiledScript) engine.compile(script);

    Object expResult = "Larky ScriptEngine";
    Object result = instance.getEngine().getFactory().getEngineName();
    assertEquals(expResult, result);
  }

  @Test
  public void testEval() throws ScriptException {
    LarkyScriptEngineFactory factory = new LarkyScriptEngineFactory();
    LarkyScriptEngine engine = (LarkyScriptEngine) factory.getScriptEngine();
    String script = String.join("\n",
        "" +
        "def main():",
        "    return 'Hello World'",
        "",
        "output = main()"
    );

    LarkyCompiledScript instance = (LarkyCompiledScript) engine.compile(script);
    String expResult = "Hello World";
    Module result = (Module) instance.eval();
    assertEquals(expResult, result.getGlobal("output"));
  }

  @Test
  public void testEval_context() throws Exception {
    LarkyScriptEngineFactory factory = new LarkyScriptEngineFactory();
    LarkyScriptEngine engine = (LarkyScriptEngine) factory.getScriptEngine();
    ScriptContext context = new SimpleScriptContext();
    StringWriter writer = new StringWriter();
    StringWriter errorWriter = new StringWriter();
    context.setWriter(writer);
    context.setErrorWriter(errorWriter);

    context.setAttribute("message", "Hello World!!!!!", ScriptContext.ENGINE_SCOPE);
    engine.setContext(context);
    String script = "print(message)";
    LarkyCompiledScript instance = (LarkyCompiledScript) engine.compile(script);
    Object expResult = "Hello World!!!!!";
    instance.eval(context);
    Object result = writer.toString().trim();
    assertEquals(expResult, result);
    writer.close();
    errorWriter.close();
  }

  @Test
  public void testEval_bindings() throws Exception {
    LarkyScriptEngineFactory factory = new LarkyScriptEngineFactory();
    LarkyScriptEngine engine = (LarkyScriptEngine) factory.getScriptEngine();
    Bindings bindings = new SimpleBindings();
    bindings.put("message", "Helloooo Woooorld!");
    engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
    String script = String.join("\n",
        "" +
        "def main():",
        "    return 'I heard, {}'.format(message)",
        "",
        "output = main()"
    );
    LarkyCompiledScript instance = (LarkyCompiledScript) engine.compile(script);
    Object expResult = "I heard, Helloooo Woooorld!";
    Module result = (Module) instance.eval(bindings);
    assertEquals(expResult, result.getGlobal("output"));
  }

}