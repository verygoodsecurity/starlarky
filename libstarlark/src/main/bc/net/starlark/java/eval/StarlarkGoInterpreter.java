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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.starlark.java.eval.compiler.BytecodeChunk;
import net.starlark.java.eval.compiler.Opcode;

/**
 * A starlark-go style bytecode interpreter.
 *
 * <p>This interpreter follows the starlark-go execution model:
 * <ul>
 *   <li>Stack-based virtual machine with separate operand stack
 *   <li>Local variables stored in a separate array (not on the stack)
 *   <li>Simple switch-based opcode dispatch
 *   <li>Position tracking via delta-encoded line/column information
 *   <li>Free variables for closures stored separately
 * </ul>
 *
 * <p>Key differences from starlark-rust style:
 * <ul>
 *   <li>Operand stack is separate from local variable storage
 *   <li>Uses dynamic stack growth (ArrayList) rather than fixed slots
 *   <li>Simpler memory model at the cost of some performance
 * </ul>
 *
 * @see StarlarkRustInterpreter for the slot-based alternative
 */
public final class StarlarkGoInterpreter extends AbstractBytecodeVM {

  /** Execution statistics for profiling. */
  public static final class Stats {
    public long instructionsExecuted;
    public long stackOperations;
    public long functionCalls;
    public long iterations;

    @Override
    public String toString() {
      return String.format(
          "Stats{instructions=%d, stackOps=%d, calls=%d, iterations=%d}",
          instructionsExecuted, stackOperations, functionCalls, iterations);
    }
  }

  // Frame state - mirrors starlark-go's frame structure
  private final Object[] locals; // Local variables (separate from stack)
  private final List<Object> stack = new ArrayList<>(16); // Operand stack (dynamic)
  private final Stats stats;

  private StarlarkGoInterpreter(
      BytecodeChunk code,
      StarlarkThread thread,
      Map<String, Object> globals,
      Tuple freevars,
      String filename,
      boolean collectStats) {
    super(code, thread, globals, filename != null ? filename : "<starlark-go>", freevars);
    this.locals = new Object[code.getLocalCount()];
    this.stats = collectStats ? new Stats() : null;
  }

  /** Executes a file's top-level bytecode using the starlark-go interpreter model. */
  public static Object execute(BytecodeChunk code, StarlarkThread thread, Map<String, Object> globals)
      throws EvalException, InterruptedException {
    return execute(code, thread, globals, null, null, false);
  }

  /** Executes a file's top-level bytecode using the starlark-go interpreter model. */
  public static Object execute(
      BytecodeChunk code,
      StarlarkThread thread,
      Map<String, Object> globals,
      Object[] freeVars,
      String filename,
      boolean collectStats)
      throws EvalException, InterruptedException {
    Tuple freevars = freeVars != null ? Tuple.of(freeVars) : null;
    return runToplevel(
        new StarlarkGoInterpreter(code, thread, globals, freevars, filename, collectStats));
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
    return new StarlarkGoInterpreter(code, thread, globals, freevars, filename, false)
        .runWithLocals(locals);
  }

  /** Executes a bytecode chunk with the given arguments bound to its first locals. */
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

  @Override
  protected void onInstruction(Opcode opcode) {
    if (stats != null) {
      stats.instructionsExecuted++;
    }
  }

  @Override
  protected void onCall() {
    if (stats != null) {
      stats.functionCalls++;
    }
  }

  @Override
  protected void onIteration() {
    if (stats != null) {
      stats.iterations++;
    }
  }

  @Override
  protected void push(Object value) {
    if (stats != null) {
      stats.stackOperations++;
    }
    stack.add(value);
  }

  @Override
  protected Object pop() throws EvalException {
    if (stats != null) {
      stats.stackOperations++;
    }
    if (stack.isEmpty()) {
      throw new EvalException("stack underflow");
    }
    return stack.remove(stack.size() - 1);
  }

  @Override
  protected Object peek() throws EvalException {
    if (stack.isEmpty()) {
      throw new EvalException("stack underflow");
    }
    return stack.get(stack.size() - 1);
  }

  @Override
  protected Object stackGet(int offset) throws EvalException {
    int index = stack.size() - offset;
    if (index < 0 || index >= stack.size()) {
      throw new EvalException("stack index out of range: " + offset);
    }
    return stack.get(index);
  }

  @Override
  protected int stackSize() {
    return stack.size();
  }

  @Override
  public Object getLocal(int index) {
    return locals[index];
  }

  @Override
  protected void setLocal(int index, Object value) {
    locals[index] = value;
  }
}
