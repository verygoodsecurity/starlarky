package com.verygood.security.larky.jsr223;

import com.google.common.collect.ImmutableSet;

import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.LogConsole;
import com.verygood.security.larky.nativelib.LarkyGlobals;
import com.verygood.security.larky.parser.InMemMapBackedStarFile;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.StarFile;

import net.starlark.java.eval.Module;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
     //pushVariables(globalBindings, engineBindings);
     //TODO(mahmoudimus): Put this in LarkyScriptEngine?
     LarkyScript interpreter = new LarkyScript(
         ImmutableSet.of(
             LarkyGlobals.class
         ),
         LarkyScript.StarlarkMode.STRICT);

     ModuleSupplier.ModuleSet moduleSet = new ModuleSupplier(mergedBindings).create();
     LogConsole console = LogConsole.writeOnlyConsole(System.out, false);

     Module result = null;
     try {
       result = interpreter.executeSkylark(
           script,
           moduleSet,
           console
       );
     } catch (IOException|InterruptedException e) {
       throw new ScriptException(e);
     }
     //pullVariables(globalBindings, engineBindings);
     return result;
   }

//   private void pushVariables(Bindings globalBindings, Bindings engineBindings) throws ScriptException {
//     Map<String, Object> mergedBindings = mergeBindings(globalBindings, engineBindings);
//
//       for (Map.Entry<String, Object> entry : mergedBindings.entrySet()) {
//           String name = entry.getKey();
//           Object value = entry.getValue();
//
//           try {
//               Field field = compiledClass.getField(name);
//               field.set(compiledInstance, value);
//           } catch (NoSuchFieldException | IllegalAccessException e) {
//               throw new ScriptException(e);
//           }
//       }
//   }
//
//   private void pullVariables(Bindings globalBindings, Bindings engineBindings) throws ScriptException {
//       for (Field field : compiledClass.getFields()) {
//           try {
//               String name = field.getName();
//               Object value = field.get(compiledInstance);
//               setBindingsValue(globalBindings, engineBindings, name, value);
//           } catch (IllegalAccessException e) {
//               throw new ScriptException(e);
//           }
//       }
//   }
//
//   private void setBindingsValue(Bindings globalBindings, Bindings engineBindings, String name, Object value) {
//       if (!engineBindings.containsKey(name) && globalBindings.containsKey(name)) {
//           globalBindings.put(name, value);
//       } else {
//           engineBindings.put(name, value);
//       }
//   }

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
