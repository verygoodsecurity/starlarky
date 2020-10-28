package com.verygood.security.larky;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * A set of modules and options for evaluating a Skylark config file.
 */
public class ModuleSet {

  private final ImmutableMap<String, Object> modules;

  ModuleSet(ImmutableMap<String, Object> modules) {
    this.modules = Preconditions.checkNotNull(modules);
  }

   /**
   * Non-static modules.
   */
  public ImmutableMap<String, Object> getModules() {
    return modules;
  }

}
