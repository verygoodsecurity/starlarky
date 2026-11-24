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

package com.verygood.security.larky.parser;

import com.verygood.security.larky.runtime.StarlarkInterop;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import net.starlark.java.eval.Module;

import java.util.Map;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;


/**
 * A Parsed Larky script file.
 *
 * <p> Objects of this class represent a parsed Larky script.
 */
@EqualsAndHashCode
@ToString
public final class ParsedStarFile {
  private final String location;
  @Getter
  private final Map<String, Object> globals;

  @Getter
  private final Module module;

  @EqualsAndHashCode.Exclude @ToString.Exclude
  private final StarlarkInterop interop;

  @EqualsAndHashCode.Exclude @ToString.Exclude
  @Nullable
  private final EvaluationMetadata metadata;

  public ParsedStarFile(String location, Map<String, Object> globals, Module module) {
    this(location, globals, module, StarlarkInterop.create());
  }

  public ParsedStarFile(
      String location, Map<String, Object> globals, Module module, StarlarkInterop interop) {
    this(location, globals, module, interop, null);
  }

  public ParsedStarFile(
      String location,
      Map<String, Object> globals,
      Module module,
      StarlarkInterop interop,
      @Nullable EvaluationMetadata metadata) {
    this.location = Preconditions.checkNotNull(location);
    this.globals = ImmutableMap.copyOf(Preconditions.checkNotNull(globals));
    this.module = module;
    this.interop = Preconditions.checkNotNull(interop);
    this.metadata = metadata;
  }

  /**
   * What happened in the evaluation that produced this result: its steps (the script's own
   * code, and initializing Larky's modules), how long it took and the settings it ran with. Null for a result not made by an evaluation.
   */
  @Nullable
  public EvaluationMetadata metadata() {
    return metadata;
  }

  /**
   * Returns a helper for using Starlark values from Java after the script finishes.
   * Use it to read fields, convert values to strings, or call functions.
   *
   * <p>These operations can run Starlark code, such as a property's getter. {@link StarlarkInterop}
   * runs each operation on a fresh Starlark thread with the evaluation's settings and remaining
   * step budget.
   */
  public StarlarkInterop interop() {
    return interop;
  }

  /**
   * Location of the top-level config file. An arbitrary string meant to be used
   * for logging/debugging. It shouldn't be parsed, as the format might change.
   */
  public String getLocation() {
    return location;
  }

  /**
   * Reads values from the global frame of the skylark environment, i.e. global variables.
   */
  @Nullable
  public <T> T getGlobalEnvironmentVariable(String name, Class<T> clazz) {
    if (module.getGlobals().containsKey(name)) {
      return clazz.cast(module.getGlobal(name));
    }
    if(module.getPredeclaredBindings().containsKey(name)) {
      return clazz.cast(module.getPredeclaredBindings().get(name));
    }
    if (globals.containsKey(name)) {
      return clazz.cast(globals.get(name));
    }
    return null;
  }

}
