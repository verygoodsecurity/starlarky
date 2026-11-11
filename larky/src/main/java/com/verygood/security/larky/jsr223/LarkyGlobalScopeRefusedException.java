package com.verygood.security.larky.jsr223;

import java.util.Collection;
import java.util.TreeSet;
import javax.script.ScriptException;

/**
 * Thrown by an engine built with {@link LarkyScriptEngine.GlobalScope#REFUSE} for a context whose {@code GLOBAL_SCOPE}
 * has any binding.
 */
public class LarkyGlobalScopeRefusedException extends ScriptException {

  public LarkyGlobalScopeRefusedException(Collection<String> names) {
    super("this engine refuses a context with global-scope bindings " + new TreeSet<>(names)
        + ": a global scope is shared by every engine of a ScriptEngineManager, so put a run's values in its engine"
        + " scope (eval) or give them per call (LarkyLoadedScript.invoke)");
  }
}
