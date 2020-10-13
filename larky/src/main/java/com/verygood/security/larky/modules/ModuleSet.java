package com.verygood.security.larky.modules;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * A set of modules and options for evaluating a Skylark config file.
 */
public class ModuleSet {

  // TODO(malcon): Remove this once all modules are @StarlarkMethod
  private final ImmutableSet<Class<?>> staticModules;
  private final ImmutableMap<String, Object> modules;

  ModuleSet(
      ImmutableSet<Class<?>> staticModules,
      ImmutableMap<String, Object> modules) {
    this.staticModules = Preconditions.checkNotNull(staticModules);
    this.modules = Preconditions.checkNotNull(modules);
  }

  /**
   * Static modules. Will be deleted.
   * TODO(malcon): Delete
   */
  public ImmutableSet<Class<?>> getStaticModules() {
    return staticModules;
  }

  /**
   * Non-static Copybara modules.
   */
  public ImmutableMap<String, Object> getModules() {
    return modules;
  }

  public static ModuleSet getInstance(ImmutableSet<Class<?>> staticModules,
                                      ImmutableMap<String, Object> modules) {
    return new ModuleSet(staticModules, modules);
  }
}
