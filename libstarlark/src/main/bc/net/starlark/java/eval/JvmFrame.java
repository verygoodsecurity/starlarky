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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.starlark.java.eval.compiler.BytecodeChunk;
import net.starlark.java.syntax.Location;

/**
 * The state of one activation of JVM-compiled Starlark code ({@link JvmBytecodeCompiler}).
 *
 * <p>Generated code reads the fields directly. {@link #locals} is the Starlark frame's locals
 * array itself (for functions, the array {@code StarlarkThread.Frame.getLocals} reads), so the
 * debugger sees current values without extra work. {@link #ip} is the index of the instruction
 * being executed; generated code stores it before each instruction that can fail or call, and
 * {@link #currentLocation} turns it into the error location.
 */
public final class JvmFrame implements BcFrame {

  public final StarlarkThread thread;
  public final BytecodeChunk chunk;
  public final Map<String, Object> globals;
  public final String filename;
  public final Tuple freevars;
  public final Object[] locals;
  public final Object[] consts;
  public int ip;
  public Object retval; // a segment method's RETURN value (see JvmBytecodeCompiler)
  private Map<Iterator<?>, Object> iterators;

  JvmFrame(
      BytecodeChunk chunk,
      StarlarkThread thread,
      Map<String, Object> globals,
      String filename,
      Tuple freevars,
      Object[] locals) {
    this.chunk = chunk;
    this.thread = thread;
    this.globals = globals != null ? globals : new HashMap<>();
    this.filename = filename != null ? filename : "<bytecode>";
    this.freevars = freevars != null ? freevars : Tuple.empty();
    this.locals = locals;
    this.consts = chunk.getConstantsArray();
  }

  @Override
  public StarlarkThread thread() {
    return thread;
  }

  @Override
  public BytecodeChunk chunk() {
    return chunk;
  }

  @Override
  public Map<String, Object> globals() {
    return globals;
  }

  @Override
  public String filename() {
    return filename;
  }

  @Override
  public Tuple freevars() {
    return freevars;
  }

  @Override
  public Map<Iterator<?>, Object> iterators() {
    if (iterators == null) {
      iterators = new HashMap<>();
    }
    return iterators;
  }

  boolean hasIterators() {
    return iterators != null && !iterators.isEmpty();
  }

  @Override
  public Object getLocal(int index) {
    return locals[index];
  }

  @Override
  public void storeLocal(int index, Object value) {
    locals[index] = value;
  }

  @Override
  public Location currentLocation() {
    int line = Math.max(0, chunk.getLineNumber(ip));
    return Location.fromFileLineColumn(filename, line, chunk.getColumnNumber(ip));
  }
}
