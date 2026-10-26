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

import java.util.Arrays;
import java.util.Map;
import net.starlark.java.eval.compiler.BytecodeChunk;

/**
 * A starlark-rust style bytecode interpreter using slot-based execution.
 *
 * <p>This interpreter follows the starlark-rust execution model:
 * <ul>
 *   <li>Slot-based memory model: unified array for locals AND stack
 *   <li>Frame size fixed per call (locals plus stack slots, grown if needed)
 *   <li>Slots indexed by position: [locals...][stack...]
 *   <li>Type-safe instruction dispatch via handler pattern
 *   <li>Optimized for cache-friendly sequential memory access
 * </ul>
 *
 * <p>Memory Layout (starlark-rust style):
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │ Local Variables (0..localCount-1) │ Stack (localCount...) │
 * └─────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>Key differences from starlark-go style:
 * <ul>
 *   <li>Locals and stack share the same contiguous slot array
 *   <li>Stack pointer (sp) is an index into the slot array
 *   <li>Better cache locality for hot loops
 * </ul>
 *
 * @see StarlarkGoInterpreter for the stack-based alternative
 */
public final class StarlarkRustInterpreter extends AbstractBytecodeVM {

  /**
   * Initial number of stack slots after the locals. starlark-rust sizes each frame from a
   * max_stack_size computed by its compiler; ours does not compute one, so the slot array grows
   * when full (for example for a list literal with thousands of elements).
   */
  private static final int INITIAL_STACK_SLOTS = 64;

  // Frame state - mirrors starlark-rust's BcFrame
  private Object[] slots; // Unified: [locals | stack]
  private final int localCount;
  private int sp; // Stack pointer (index into slots)

  private StarlarkRustInterpreter(
      BytecodeChunk code,
      StarlarkThread thread,
      Map<String, Object> globals,
      String filename,
      Tuple freevars) {
    super(code, thread, globals, filename != null ? filename : "<starlark-rust>", freevars);
    this.localCount = code.getLocalCount();
    this.slots = new Object[localCount + INITIAL_STACK_SLOTS];
    this.sp = localCount; // Stack starts after locals
  }

  /** Executes a file's top-level bytecode using the slot-based model. */
  public static Object execute(BytecodeChunk code, StarlarkThread thread, Map<String, Object> globals)
      throws EvalException, InterruptedException {
    return execute(code, thread, globals, null);
  }

  /** Executes a file's top-level bytecode using the slot-based model. */
  public static Object execute(
      BytecodeChunk code, StarlarkThread thread, Map<String, Object> globals, String filename)
      throws EvalException, InterruptedException {
    return runToplevel(new StarlarkRustInterpreter(code, thread, globals, filename, null));
  }

  /** Executes a function body with pre-processed locals and captured cells. */
  public static Object executeWithLocals(
      BytecodeChunk code,
      StarlarkThread thread,
      Object[] locals,
      Map<String, Object> globals,
      String filename,
      Tuple freevars)
      throws EvalException, InterruptedException {
    return new StarlarkRustInterpreter(code, thread, globals, filename, freevars)
        .runWithLocals(locals);
  }

  /** Executes a bytecode chunk with the given arguments bound to its first local slots. */
  public static Object executeWithArgs(
      BytecodeChunk code,
      StarlarkThread thread,
      Object[] args,
      Map<String, Object> globals,
      String filename)
      throws EvalException, InterruptedException {
    Object[] params = Arrays.copyOf(args, Math.min(args.length, code.getParameterCount()));
    return executeWithLocals(code, thread, params, globals, filename, null);
  }

  // ===== Slot Operations (starlark-rust style) =====

  @Override
  protected void push(Object value) {
    if (sp >= slots.length) {
      slots = Arrays.copyOf(slots, localCount + 2 * (slots.length - localCount));
    }
    slots[sp++] = value;
  }

  @Override
  protected Object pop() throws EvalException {
    if (sp <= localCount) {
      throw new EvalException("stack underflow");
    }
    Object value = slots[--sp];
    slots[sp] = null;
    return value;
  }

  @Override
  protected Object peek() throws EvalException {
    if (sp <= localCount) {
      throw new EvalException("stack underflow");
    }
    return slots[sp - 1];
  }

  @Override
  protected Object stackGet(int offset) throws EvalException {
    int index = sp - offset;
    if (index < localCount) {
      throw new EvalException("stack access out of range");
    }
    return slots[index];
  }

  @Override
  protected int stackSize() {
    return sp - localCount;
  }

  @Override
  public Object getLocal(int index) {
    return slots[index];
  }

  @Override
  protected void setLocal(int index, Object value) {
    slots[index] = value;
  }
}
