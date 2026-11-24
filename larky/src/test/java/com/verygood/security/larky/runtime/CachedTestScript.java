package com.verygood.security.larky.runtime;

import com.verygood.security.larky.ModuleSupplier.ModuleSet;
import com.verygood.security.larky.console.Console;
import com.verygood.security.larky.parser.EvaluationSettings;
import com.verygood.security.larky.parser.LarkyScript;
import java.util.Map;
import java.util.Set;

/** Lets tests run the public script entry points with a cache of their own. */
final class CachedTestScript extends LarkyScript {
  private ModuleCache cache;

  CachedTestScript(StarlarkMode mode) {
    super(mode);
  }

  CachedTestScript(Set<Class<?>> builtins, StarlarkMode mode) {
    super(builtins, mode);
  }

  CachedTestScript(Set<Class<?>> builtins, StarlarkMode mode, Map<String, Object> globals) {
    super(builtins, mode, globals);
  }

  CachedTestScript(
      Set<Class<?>> builtins, StarlarkMode mode, Map<String, Object> globals,
      EvaluationSettings settings) {
    super(builtins, mode, globals, settings);
  }

  CachedTestScript withModuleCache(ModuleCache cache) {
    this.cache = cache;
    return this;
  }

  @Override
  protected LarkyEvaluator newEvaluator(ModuleSet modules, Console console) {
    return new LarkyEvaluator(this, modules, console, cache);
  }

  static LarkyEvaluator evaluator(CachedTestScript script, Console console) {
    return script.newEvaluator(script.getModuleSet(), console);
  }
}
