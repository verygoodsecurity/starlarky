/*
 * Copyright 2026 Very Good Security Authors
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;

/** A synchronous host operation within one evaluation of a compiled script. */
@FunctionalInterface
public interface Computation<I, O> {

  /**
   * Runs after the entry module's top level. The scope is valid on this thread for this call.
   * Starlark calls use the evaluation's remaining budget. Java work runs synchronously, and
   * Larky checks the deadline again before accepting the result.
   */
  O run(Scope scope, I input) throws EvalException, InterruptedException;

  /** Calls and field reads on the entry module's active evaluation thread. */
  final class Scope {
    private final Module module;
    private final Map<String, Module> imports;
    private final Mutability mutability;

    Scope(Module module, Map<String, Module> imports, Mutability mutability) {
      this.module = module;
      this.imports = imports;
      this.mutability = mutability;
    }

    /** Calls a global function from the entry module. */
    public Object call(String name, Object... arguments) throws EvalException, InterruptedException {
      checkActive();
      Object function = module.getGlobal(name);
      if (function == null) {
        throw Starlark.errorf("no function '%s' in the entry module", name);
      }
      return callValue(function, Arrays.asList(arguments), Map.of());
    }

    /** Calls a Starlark value with positional and keyword arguments converted in this scope. */
    public Object callValue(Object callable, List<?> arguments, Map<String, ?> keywords)
        throws EvalException, InterruptedException {
      StarlarkThread thread = checkActive();
      List<Object> values = new ArrayList<>(arguments.size());
      for (Object argument : arguments) {
        values.add(toStarlark(argument));
      }
      Map<String, Object> named = Map.of();
      if (!keywords.isEmpty()) {
        named = new LinkedHashMap<>();
        for (var entry : keywords.entrySet()) {
          named.put(entry.getKey(), toStarlark(entry.getValue()));
        }
      }
      return Starlark.call(thread, callable, values, named);
    }

    /** Reads an attribute, including a Starlark property getter. */
    public Object getField(Object value, String name) throws EvalException, InterruptedException {
      StarlarkThread thread = checkActive();
      return Starlark.getattr(mutability, thread.getSemantics(), value, name, null);
    }

    /** Calls a method with positional arguments converted in this scope. */
    public Object callMethod(Object value, String name, Object... arguments)
        throws EvalException, InterruptedException {
      return callValue(getField(value, name), Arrays.asList(arguments), Map.of());
    }

    /** Gets an export from a module loaded by the script or listed in the computation's imports. */
    public Object export(String label, String name) throws EvalException {
      checkActive();
      Module imported = imports.get(label);
      if (imported == null) {
        throw Starlark.errorf("module '%s' was not loaded", label);
      }
      Object value = name.startsWith("_") ? null : imported.getGlobal(name);
      if (value == null) {
        throw Starlark.errorf("module '%s' does not export '%s'", label, name);
      }
      return value;
    }

    // A Starlark value passes through as it is, as in LarkyCalls: converting a mutable dict of
    // lists would hand the script a copy, so its changes would not reach the host's value.
    private Object toStarlark(Object value) {
      return Starlark.valid(value) ? value : StarlarkUtil.valueToStarlark(value, mutability);
    }

    private StarlarkThread checkActive() {
      // Every entry evaluation owns a distinct mutability. Use it to check ownership
      // without keeping the evaluation thread alive if the host retains this scope.
      StarlarkThread thread = CurrentEvaluation.currentOrNull();
      if (thread == null || thread.mutability() != mutability || mutability.isFrozen()) {
        throw new IllegalStateException("computation scope is not active on this thread");
      }
      return thread;
    }
  }
}
