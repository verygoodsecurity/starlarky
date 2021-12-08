/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.larky;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.function.Function;

import com.verygood.security.larky.modules.BinasciiModule;
import com.verygood.security.larky.modules.C99MathModule;
import com.verygood.security.larky.modules.CerebroModule;
import com.verygood.security.larky.modules.CodecsModule;
import com.verygood.security.larky.modules.CollectionsModule;
import com.verygood.security.larky.modules.CryptoModule;
import com.verygood.security.larky.modules.JsonModule;
import com.verygood.security.larky.modules.OpenSSLModule;
import com.verygood.security.larky.modules.ProtoBufModule;
import com.verygood.security.larky.modules.RegexModule;
import com.verygood.security.larky.modules.ResultModule;
import com.verygood.security.larky.modules.StructModule;
import com.verygood.security.larky.modules.SysModule;
import com.verygood.security.larky.modules.VaultModule;
import com.verygood.security.larky.modules.X509Module;
import com.verygood.security.larky.modules.XMLModule;
import com.verygood.security.larky.modules.ZLibModule;
import com.verygood.security.larky.modules.globals.LarkyGlobals;
import com.verygood.security.larky.modules.globals.PythonBuiltins;
import com.verygood.security.larky.modules.testing.AssertionsModule;
import com.verygood.security.larky.modules.testing.UnittestModule;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.StarlarkValue;

/**
 * A supplier of modules for Larky
 */
public class ModuleSupplier {

  public static final ImmutableSet<Class<?>> CORE_MODULES = ImmutableSet.of(
    LarkyGlobals.class,
    PythonBuiltins.class
  );

  public static final ImmutableSet<StarlarkValue> STD_MODULES = ImmutableSet.of(
    BinasciiModule.INSTANCE,
    C99MathModule.INSTANCE,
    CodecsModule.INSTANCE,
    CollectionsModule.INSTANCE,
    CryptoModule.INSTANCE,
    JsonModule.INSTANCE,
    OpenSSLModule.INSTANCE,
    ProtoBufModule.INSTANCE,
    RegexModule.INSTANCE,
    ResultModule.INSTANCE,
    StructModule.INSTANCE,
    SysModule.INSTANCE,
    X509Module.INSTANCE,
    XMLModule.INSTANCE,
    ZLibModule.INSTANCE
  );

  public static final ImmutableSet<StarlarkValue> VGS_MODULES = ImmutableSet.of(
    VaultModule.INSTANCE,
    CerebroModule.INSTANCE
  );

  public static final ImmutableSet<StarlarkValue> TEST_MODULES = ImmutableSet.of(
    UnittestModule.INSTANCE,
    AssertionsModule.INSTANCE
  );

  private final Map<String, Object> environment;

  public ModuleSupplier() {
    this(ImmutableMap.<String, Object>builder().build());
  }

  public ModuleSupplier(Map<String, Object> environment) {
    this.environment = Preconditions.checkNotNull(environment);
  }

  public ModuleSupplier(ImmutableSet<StarlarkValue> moduleSet) {
    this.environment = moduleSetAsMap(Preconditions.checkNotNull(moduleSet));
  }

  /**
   * Get all available modules
   */
  public ImmutableSet<StarlarkValue> getModules() {
    return getModules(false);
  }

  public ImmutableSet<StarlarkValue> getModules(boolean withTest) {
    ImmutableSet.Builder<StarlarkValue> modules = ImmutableSet.<StarlarkValue>builder()
                                                    .addAll(STD_MODULES)
                                                    .addAll(VGS_MODULES);

    return withTest
             ? modules.addAll(getTestModules()).build()
             : modules.build();
  }

  public ImmutableSet<StarlarkValue> getTestModules() {
    return TEST_MODULES;
  }

  public Map<String, Object> getEnvironment() {
    return environment;
  }

  public final ModuleSet create() {
    return new ModuleSet(ImmutableMap.<String, Object>builder()
                           .putAll(modulesToVariableMap())
                           .putAll(getEnvironment())
                           .build()); // should allow overrides ;
  }

  private ImmutableMap<String, Object> moduleSetAsMap(ImmutableSet<StarlarkValue> moduleSet) {
    return moduleSet
             .stream()
             .collect(
               ImmutableMap.toImmutableMap(
                 this::findClosestStarlarkBuiltinName,
                 Function.identity()));
  }

  public ImmutableMap<String, Object> modulesToVariableMap() {
    return moduleSetAsMap(getModules(false));
  }

  public ModuleSet modulesToVariableMap(boolean withTest) {
    return new ModuleSet(moduleSetAsMap(getModules(withTest)));
  }

  private String findClosestStarlarkBuiltinName(Object o) {
    Class<?> cls = o.getClass();
    while (cls != null && cls != Object.class) {
      StarlarkBuiltin annotation = cls.getAnnotation(StarlarkBuiltin.class);
      if (annotation != null) {
        return annotation.name();
      }
      cls = cls.getSuperclass();
    }
    throw new IllegalStateException("Cannot find @StarlarkBuiltin for " + o.getClass());
  }

  /**
   * A set of modules and options for evaluating a Skylark config file.
   */
  public static class ModuleSet {

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
}