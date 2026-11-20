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

import java.util.Map;
import javax.annotation.Nullable;
import net.starlark.java.syntax.Location;

/** The call-stack frame for a file's top-level code executed by a bytecode VM. */
final class BytecodeToplevel implements StarlarkCallable {

  private final String name;
  private final Location location;
  @Nullable private final Module module;
  private final Body body;

  /** Runs the top-level code of a file. */
  interface Body {
    Object run() throws EvalException, InterruptedException;
  }

  BytecodeToplevel(
      String name, String filename, Map<String, Object> globals, Body body) {
    this.name = name != null ? name : "<toplevel>";
    this.location =
        filename != null ? Location.fromFileLineColumn(filename, 0, 0) : Location.BUILTIN;
    this.module = BytecodeGlobals.moduleOf(globals);
    this.body = body;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Location getLocation() {
    return location;
  }

  /** Returns the module whose top-level code this frame runs, if known. */
  @Nullable
  Module getModule() {
    return module;
  }

  /** Runs the top-level code; the caller ({@link Starlark#positionalOnlyCall}) pushed the frame. */
  @Override
  public Object positionalOnlyCall(StarlarkThread thread, Object... positional)
      throws EvalException, InterruptedException {
    return body.run();
  }

  @Override
  public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) {
    throw new UnsupportedOperationException("top-level code cannot be called");
  }
}
