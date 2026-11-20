// Copyright 2025 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package net.starlark.java.eval;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.starlark.java.syntax.TypeTable;

/**
 * The global namespace of a file executed by a bytecode VM: a live view of the {@link Module}'s
 * globals, so that functions see later assignments to the module (for example by another file
 * executed in the same module, as a REPL does), as in the tree-walker.
 *
 * <p>Predeclared and universal names are kept apart in {@link #builtins} and read with
 * LOAD_BUILTIN, so that a file-level binding such as {@code len = 1} shadows the builtin for the
 * whole file, and an access before that assignment fails.
 */
final class BytecodeGlobals extends AbstractMap<String, Object> {

  private final Module module;
  private final Map<String, Object> builtins;
  // The type table of the program whose code runs in this namespace, or null if it is untyped.
  @Nullable private final TypeTable typeTable;

  BytecodeGlobals(Module module, Map<String, Object> builtins) {
    this(module, builtins, null);
  }

  BytecodeGlobals(Module module, Map<String, Object> builtins, @Nullable TypeTable typeTable) {
    this.module = module;
    this.builtins = builtins;
    this.typeTable = typeTable;
  }

  /** Returns the type table of the program running in a VM globals map, or null. */
  @Nullable
  static TypeTable typeTableOf(Map<String, Object> globals) {
    return globals instanceof BytecodeGlobals g ? g.typeTable : null;
  }

  @Override
  public Object get(Object name) {
    return name instanceof String ? module.getGlobal((String) name) : null;
  }

  @Override
  public boolean containsKey(Object name) {
    return get(name) != null;
  }

  @Override
  public Object put(String name, Object value) {
    Object previous = module.getGlobal(name);
    module.setGlobal(name, value);
    return previous;
  }

  @Override
  public Set<Map.Entry<String, Object>> entrySet() {
    return module.getGlobals().entrySet();
  }

  /** Returns the module behind a VM globals map, or null for a plain map. */
  @Nullable
  static Module moduleOf(Map<String, Object> globals) {
    return globals instanceof BytecodeGlobals ? ((BytecodeGlobals) globals).module : null;
  }

  /**
   * Returns the value of a PREDECLARED or UNIVERSAL name. {@code globals} may be a plain map
   * supplied by a caller of the VM, in which case it is consulted before the universe.
   */
  static Object lookupBuiltin(Map<String, Object> globals, String name) throws EvalException {
    Object value =
        globals instanceof BytecodeGlobals
            ? ((BytecodeGlobals) globals).builtins.get(name)
            : globals.get(name);
    if (value == null) {
      value = Starlark.UNIVERSE.get(name);
    }
    if (value == null) {
      throw Starlark.errorf("name '%s' is not defined", name);
    }
    return value;
  }
}
