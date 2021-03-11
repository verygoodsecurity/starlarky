package com.verygood.security.larky.jsr223;

import com.google.common.io.CharStreams;

import com.verygood.security.larky.parser.DefaultLarkyInterpreter;
import com.verygood.security.larky.parser.InMemMapBackedStarFile;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.ParsedStarFile;
import com.verygood.security.larky.parser.StarFile;

import net.starlark.java.eval.EvalException;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;


public class LarkyCompiledScript extends CompiledScript {

  private static final LarkyScript.StarlarkMode LARKY_MODE = LarkyScript.StarlarkMode.STRICT;

  private static final String DEFAULT_SCRIPT_NAME = "larky.star";
  private final LarkyScriptEngine engine;

  /**
   * Construct a {@link LarkyCompiledScript}.
   *
   * @param engine the {@link LarkyScriptEngine} that compiled this script
   */
  LarkyCompiledScript(LarkyScriptEngine engine) {
    this.engine = engine;
  }


  @Override
  public ScriptEngine getEngine() {
    return engine;
  }

  @Override
  public Object eval(ScriptContext context) throws ScriptException {
    try(Reader reader = context.getReader()) {
      final StarFile script = InMemMapBackedStarFile.createStarFile(DEFAULT_SCRIPT_NAME, CharStreams.toString(reader));
      Bindings globalBindings = context.getBindings(ScriptContext.GLOBAL_SCOPE);
      Bindings engineBindings = context.getBindings(ScriptContext.ENGINE_SCOPE);

      final DefaultLarkyInterpreter larkyInterpreter = new DefaultLarkyInterpreter(LARKY_MODE, globalBindings, engineBindings);

      ParsedStarFile result = larkyInterpreter.evaluate(script, context.getWriter());
      setBindingsValue(globalBindings, engineBindings, result.getGlobals());
      return result;
    } catch (IOException | EvalException e) {
      throw new ScriptException(e);
    }
  }


  private void setBindingsValue(Bindings globalBindings, Bindings engineBindings, Map<String, Object> moduleGlobals) {
    for (Map.Entry<String, Object> entry : moduleGlobals.entrySet()) {
      String name = entry.getKey();
      Object value = entry.getValue();
      if (globalBindings != null && globalBindings.containsKey(name)) {
        globalBindings.put(name, value);
      }
      // by default, if defined values are not globals, they belong in engine binding scope
      // to allow for multiple evals() of an instance
      // TODO(mahmoudimus): is this threadsafe?
      else if (engineBindings != null) {
        engineBindings.put(name, value);
      }
    }
  }
}
