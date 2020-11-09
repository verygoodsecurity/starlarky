package com.verygood.security.larky.jsr223;

import static org.junit.Assert.assertEquals;

import net.starlark.java.eval.Module;

import org.junit.Test;

import javax.script.ScriptException;

public class LarkyCompiledScriptTest {

  @Test
  public void getEngine() {
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
}