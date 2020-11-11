package com.verygood.security.larky.jsr223;

import com.google.common.collect.ImmutableSet;

import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.StreamWriterConsole;
import com.verygood.security.larky.nativelib.LarkyGlobals;
import com.verygood.security.larky.parser.InMemMapBackedStarFile;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.ParsedStarFile;
import com.verygood.security.larky.parser.StarFile;

import net.starlark.java.eval.Starlark;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;


public class LarkyCompiledScript extends CompiledScript {
  private static final String DEFAULT_SCRIPT_NAME = "larky.star";
  private final LarkyScriptEngine engine;
  private final StarFile script;

  /**
    * Construct a {@link LarkyCompiledScript}.
    *  @param engine the {@link LarkyScriptEngine} that compiled this script
    * @param content Contents of a StarFile
   */
   LarkyCompiledScript(LarkyScriptEngine engine, String content) {
       this.engine = engine;
       //StarlarkFile.parse(ParserInput.fromString(content, ""));
       this.script = InMemMapBackedStarFile.createStarFile(DEFAULT_SCRIPT_NAME, content);
   }

   /**
    * Returns content of starfile
    *
    * @return the {@link StarFile}.
    */
   public StarFile getScript() {
       return script;
   }


   @Override
   public ScriptEngine getEngine() {
       return engine;
   }

   @Override
   public Object eval(ScriptContext context) throws ScriptException {
     Bindings globalBindings = context.getBindings(ScriptContext.GLOBAL_SCOPE);
     Bindings engineBindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
     Map<String, Object> mergedBindings = mergeBindings(globalBindings, engineBindings);
     //TODO(mahmoudimus): Put this in LarkyScriptEngine?
     Map<String, Object> globalStarlarkValues = mergedBindings
         .entrySet()
         .stream()
         .collect(Collectors.toMap(
             Map.Entry::getKey,
             entry -> Starlark.fromJava(entry.getValue(), null), (a, b) -> b));

     LarkyScript interpreter = new LarkyScript(
         ImmutableSet.of(
             LarkyGlobals.class
         ),
         LarkyScript.StarlarkMode.STRICT,
         globalStarlarkValues);

     ModuleSupplier.ModuleSet moduleSet = new ModuleSupplier().create();
     Writer writer = context.getWriter();

     ParsedStarFile result = null;
     try {
       result = interpreter.evaluate(
           script,
           moduleSet,
           new StreamWriterConsole(writer)
       );
     } catch (IOException e) {
       throw new ScriptException(e);
     }
     setBindingsValue(globalBindings, engineBindings, result.getGlobals());
     return result;
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


   private Map<String, Object> mergeBindings(Bindings... bindingsToMerge) {
       Map<String, Object> variables = new HashMap<>();

       for (Bindings bindings : bindingsToMerge) {
           if (bindings != null) {
               for (Map.Entry<String, Object> globalEntry : bindings.entrySet()) {
                   variables.put(globalEntry.getKey(), globalEntry.getValue());
               }
           }
       }

       return variables;
   }
}
