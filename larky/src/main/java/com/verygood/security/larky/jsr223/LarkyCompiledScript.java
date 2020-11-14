package com.verygood.security.larky.jsr223;

import com.verygood.security.larky.parser.StarFile;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;


public class LarkyCompiledScript extends CompiledScript {

  private final LarkyScriptEngine engine;
  private final String content;

  /**
    * Construct a {@link LarkyCompiledScript}.
    * @param engine the {@link LarkyScriptEngine} that compiled this script
    * @param content Contents of a StarFile
   */
   LarkyCompiledScript(LarkyScriptEngine engine, String content) {
     this.engine = engine;
     this.content = content;
   }

   /**
    * Returns the compiled script content
    *
    * @return the {@link StarFile}'s content as String.
    */
   public String getScript() {
       return content;
   }


   @Override
   public ScriptEngine getEngine() {
       return engine;
   }

   // TODO(mahmoudimus): have this return a net.starlark.java.syntax.Program?
   @Override
   public Object eval(ScriptContext context) throws ScriptException {
     return getEngine().eval(this.getScript(), context);
   }
}
