package com.verygood.security.larky.jsr223;

import javax.script.ScriptException;

/**
 * Thrown by {@link LarkyCompiledScript#load} for a script that cannot be loaded once: its top level leaves a mutable
 * value, or defines a name that is declared per call.
 */
public class LarkyScriptNotLoadableException extends ScriptException {

  public LarkyScriptNotLoadableException(String message) {
    super(message);
  }
}
