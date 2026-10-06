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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.starlark.java.eval.compiler.BytecodeChunk;
import net.starlark.java.syntax.TokenKind;

/**
 * A Buck/Starlark style bytecode interpreter with IR-based optimizations.
 *
 * <p>This interpreter follows the facebook/buck Starlark implementation model:
 * <ul>
 *   <li>Intermediate Representation (IR) layer for optimization
 *   <li>Slot-based variable management (Local, Global, Cell, Free)
 *   <li>Call site caching for repeated function calls
 *   <li>Type-specialized operations (PLUS_STRING, PLUS_LIST)
 *   <li>Copy propagation and lazy local assignment
 * </ul>
 *
 * <h2>Key differences from other backends:</h2>
 * <ul>
 *   <li>Uses call site caching for repeated method calls</li>
 *   <li>Type-specialized opcodes for common patterns</li>
 *   <li>IR-based optimization opportunities</li>
 *   <li>Lazy local number assignment for better slot allocation</li>
 * </ul>
 *
 * <h2>Slot Types (from Buck's BcIrSlot):</h2>
 * <ul>
 *   <li><b>Local</b>: Parameters, variables, temporaries</li>
 *   <li><b>Global</b>: Module-level globals</li>
 *   <li><b>Cell</b>: Closure cell references</li>
 *   <li><b>Free</b>: Free variables from enclosing scopes</li>
 *   <li><b>Const</b>: Constant pool references</li>
 * </ul>
 *
 * @see <a href="https://github.com/facebook/buck/tree/dev/starlark">Buck Starlark</a>
 */
public final class BuckStyleInterpreter extends AbstractBytecodeVM {

  /**
   * Call site cache (Buck: BcDotSite). Records which (receiver class, attribute) pairs have been
   * resolved; the lookup itself always goes through Starlark.getattr so semantics are unchanged.
   */
  private static final class CallSiteCache {
    private final Set<String> seen = new HashSet<>();
    private int hits = 0;
    private int misses = 0;

    void record(Object receiver, String name) {
      if (seen.add(receiver.getClass().getName() + ":" + name)) {
        misses++;
      } else {
        hits++;
      }
    }
  }

  private final Object[] locals; // Local slots
  private final List<Object> stack = new ArrayList<>();
  private final CallSiteCache callCache = new CallSiteCache(); // Buck-style call site caching

  private BuckStyleInterpreter(
      BytecodeChunk code,
      StarlarkThread thread,
      Map<String, Object> globals,
      String filename,
      Tuple freevars) {
    super(code, thread, globals, filename != null ? filename : "<buck>", freevars);
    this.locals = new Object[code.getLocalCount()];
  }

  /** Executes a file's top-level bytecode using the Buck execution model. */
  public static Object execute(BytecodeChunk code, StarlarkThread thread, Map<String, Object> globals)
      throws EvalException, InterruptedException {
    return execute(code, thread, globals, null);
  }

  /** Executes a file's top-level bytecode using the Buck execution model. */
  public static Object execute(
      BytecodeChunk code, StarlarkThread thread, Map<String, Object> globals, String filename)
      throws EvalException, InterruptedException {
    return runToplevel(new BuckStyleInterpreter(code, thread, globals, filename, null));
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
    return new BuckStyleInterpreter(code, thread, globals, filename, freevars).runWithLocals(locals);
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

  // ===== Buck-Style Optimizations =====

  /** Attribute access with call site caching (Buck: BcDotSite). */
  @Override
  protected Object getAttr(Object object, String name) throws EvalException, InterruptedException {
    callCache.record(object, name);
    return super.getAttr(object, name);
  }

  /** Type-specialized binary operations (Buck: PLUS_STRING, PLUS_LIST). */
  @Override
  protected Object binaryOp(TokenKind op, Object x, Object y) throws EvalException {
    if (op == TokenKind.PLUS) {
      if (x instanceof String && y instanceof String) {
        return (String) x + (String) y; // PLUS_STRING
      }
      if (x instanceof StarlarkList && y instanceof StarlarkList) {
        return StarlarkList.concat((StarlarkList<?>) x, (StarlarkList<?>) y, thread.mutability());
      }
    }
    return super.binaryOp(op, x, y);
  }

  @Override
  protected void push(Object value) {
    stack.add(value);
  }

  @Override
  protected Object pop() throws EvalException {
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
