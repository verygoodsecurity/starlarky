package com.verygood.security.larky.jsr223;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class LarkyCompiledScript extends CompiledScript {
  private final LarkyScriptEngine engine;
  private final Class<?> compiledClass;
  private final Object compiledInstance;

   /**
    * Construct a {@link LarkyCompiledScript}.
    *
    * @param engine the {@link LarkyScriptEngine} that compiled this script
    * @param compiledClass the compiled {@link Class}
    * @param compiledInstance the instance of the compiled {@link Class}
    */
   LarkyCompiledScript(LarkyScriptEngine engine, Class<?> compiledClass, Object compiledInstance) {
       this.engine = engine;
       this.compiledClass = compiledClass;
       this.compiledInstance = compiledInstance;
   }

   /**
    * Returns the compiled {@link Class}.
    *
    * @return the compiled {@link Class}.
    */
   public Class<?> getCompiledClass() {
       return compiledClass;
   }

   /**
    * Returns the instance of the compiled {@link Class}.
    *
    * @return the instance of the compiled {@link Class} or {@code null}
    */
   public Object getCompiledInstance() {
       return compiledInstance;
   }

   @Override
   public ScriptEngine getEngine() {
       return engine;
   }

   @Override
   public Object eval(ScriptContext context) throws ScriptException {
       Bindings globalBindings = context.getBindings(ScriptContext.GLOBAL_SCOPE);
       Bindings engineBindings = context.getBindings(ScriptContext.ENGINE_SCOPE);

       pushVariables(globalBindings, engineBindings);
       Object result = eval(); //TODO(mahmoud): FIX THIS
       //Object result = executionStrategy.execute(compiledInstance);
       pullVariables(globalBindings, engineBindings);

       return result;
   }

   private void pushVariables(Bindings globalBindings, Bindings engineBindings) throws ScriptException {
       Map<String, Object> mergedBindings = mergeBindings(globalBindings, engineBindings);

       for (Map.Entry<String, Object> entry : mergedBindings.entrySet()) {
           String name = entry.getKey();
           Object value = entry.getValue();

           try {
               Field field = compiledClass.getField(name);
               field.set(compiledInstance, value);
           } catch (NoSuchFieldException | IllegalAccessException e) {
               throw new ScriptException(e);
           }
       }
   }

   private void pullVariables(Bindings globalBindings, Bindings engineBindings) throws ScriptException {
       for (Field field : compiledClass.getFields()) {
           try {
               String name = field.getName();
               Object value = field.get(compiledInstance);
               setBindingsValue(globalBindings, engineBindings, name, value);
           } catch (IllegalAccessException e) {
               throw new ScriptException(e);
           }
       }
   }

   private void setBindingsValue(Bindings globalBindings, Bindings engineBindings, String name, Object value) {
       if (!engineBindings.containsKey(name) && globalBindings.containsKey(name)) {
           globalBindings.put(name, value);
       } else {
           engineBindings.put(name, value);
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
