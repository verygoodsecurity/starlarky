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

package net.starlark.java.eval.compiler;

/**
 * The bytecode VMs, selected with {@code -Dstarlark.bytecode.vm=<id>} (see {@link #configuredVm}).
 *
 * <ul>
 *   <li><b>interpreter</b> (default): {@link net.starlark.java.eval.BytecodeInterpreter}
 * </ul>
 *
 * All of them share the instruction semantics in {@code BcOps}; they differ only in storage.
 */
public enum BytecodeTarget {
  INTERPRETER("interpreter");

  private final String id;

  BytecodeTarget(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  /** Returns the VM named by {@code -Dstarlark.bytecode.vm}, or {@link #INTERPRETER}. */
  public static BytecodeTarget configuredVm() {
    String id = System.getProperty("starlark.bytecode.vm");
    if (id == null || id.isEmpty()) {
      return INTERPRETER;
    }
    BytecodeTarget target = fromId(id);
    if (target == null) {
      throw new IllegalArgumentException("starlark.bytecode.vm: unknown VM: " + id);
    }
    return target;
  }

  public static BytecodeTarget fromId(String id) {
    for (BytecodeTarget target : values()) {
      if (target.id.equals(id)) {
        return target;
      }
    }
    return null;
  }
}
