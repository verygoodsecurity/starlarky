package com.verygood.security.larky.jsr223;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Throwables;
import java.io.StringWriter;

import com.verygood.security.larky.parser.ParsedStarFile;

import org.junit.Test;

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
    ParsedStarFile result = (ParsedStarFile) instance.eval();
    assertEquals(expResult, result.getGlobalEnvironmentVariable("output", String.class));
  }

  @Test
  public void testEval_withUncheckedException() {
    LarkyScriptEngineFactory factory = new LarkyScriptEngineFactory();
    LarkyScriptEngine engine = (LarkyScriptEngine) factory.getScriptEngine();
    String script = String.join("\n",
      "" +
        "def process(input, ctx):",
      "    foo = 'bar'",
      "    # expecting error below!",
      "    input.body = foo",
      "    return input",
      "",
      "output = process(None, {})"
    );

    LarkyCompiledScript instance = (LarkyCompiledScript) engine.compile(script);

    ParsedStarFile result = null;
    LarkyEvaluationScriptException scriptException = null;
    try {
      result = (ParsedStarFile) instance.eval();
      fail("should not have gotten here");
    } catch (LarkyEvaluationScriptException e) {
      scriptException = e;
    } catch (Exception e) {
      fail("Unexpected exception thrown!");
    }
    assertNotNull(scriptException);
    assertTrue(scriptException.getMessage().contains("cannot set .body field of NoneType value"));
    assertTrue(
      Throwables.getStackTraceAsString(scriptException)
        .contains(
          /*substring*/
          "Traceback (most recent call last):\n" +
            "\tFile \"larky.star\", line 7, column 17, in <toplevel>\n" +
            "\tFile \"larky.star\", line 4, column 10, in process\n" +
            "Error: cannot set .body field of NoneType value")
    );
  }

  @Test
  public void testEval_withCheckedException() {

    class OperationException extends Exception {

      public OperationException(Exception e) {
        super(e);
      }
    }

    LarkyScriptEngineFactory factory = new LarkyScriptEngineFactory();
    LarkyScriptEngine engine = (LarkyScriptEngine) factory.getScriptEngine();
    String script = String.join("\n",
      "" +
        "def process(input, ctx):",
      "    fail('boom')",
      "    return input",
      "",
      "output = process(None, {})"
    );

    LarkyCompiledScript instance = (LarkyCompiledScript) engine.compile(script);

    OperationException operationException = null;
    try {
      try {
        instance.eval();
      } catch (ScriptException e) {
        throw new OperationException(e);
      }
    } catch (OperationException e) {
      operationException = e;
    }
    assertNotNull(operationException);
    assertTrue(
      operationException.getMessage()
        .contains(
          /*substring*/
          "Traceback (most recent call last):\n" +
            "\tFile \"larky.star\", line 5, column 17, in <toplevel>\n" +
            "\tFile \"larky.star\", line 2, column 9, in process\n" +
            "Error in fail: boom")
    );
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
    ParsedStarFile result = (ParsedStarFile) instance.eval(bindings);
    assertEquals(expResult, result.getGlobalEnvironmentVariable("output", String.class));
  }

}