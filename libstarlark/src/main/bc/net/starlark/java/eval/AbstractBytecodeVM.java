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
import java.util.List;
import java.util.Map;
import net.starlark.java.eval.compiler.BytecodeChunk;
import net.starlark.java.eval.compiler.Instruction;
import net.starlark.java.eval.compiler.Opcode;
import net.starlark.java.syntax.Location;
import net.starlark.java.syntax.TokenKind;

/**
 * The semantics of every bytecode instruction, shared by all bytecode VMs.
 *
 * <p>Subclasses decide only how the operand stack and local variables are stored ({@link #push},
 * {@link #pop}, {@link #getLocal}, ...) and may observe or specialize a few operations through
 * hooks ({@link #binaryOp}, {@link #getAttr}, {@link #onInstruction}, ...). Because the opcode
 * semantics live here, every VM behaves the same way as the tree-walker.
 */
abstract class AbstractBytecodeVM implements BcFrame {

  protected final BytecodeChunk chunk;
  protected final StarlarkThread thread;
  protected final Map<String, Object> globals;
  protected final String filename;
  private final Tuple freevars; // cells captured from enclosing functions
  private final Map<Iterator<?>, Object> iteratorToIterable; // iteration locks to release
  protected int ip; // instruction pointer

  protected AbstractBytecodeVM(
      BytecodeChunk chunk,
      StarlarkThread thread,
      Map<String, Object> globals,
      String filename,
      Tuple freevars) {
    this.chunk = chunk;
    this.thread = thread;
    this.globals = globals != null ? globals : new HashMap<>();
    this.filename = filename != null ? filename : "<bytecode>";
    this.freevars = freevars != null ? freevars : Tuple.empty();
    this.iteratorToIterable = new HashMap<>();
    this.ip = 0;
  }

  // ---- BcFrame ----

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
    return iteratorToIterable;
  }

  // ---- storage, provided by each VM ----

  protected abstract void push(Object value) throws EvalException;

  protected abstract Object pop() throws EvalException;

  protected abstract Object peek() throws EvalException;

  /** Returns the value at stack[-offset] (1 = top of stack). */
  protected abstract Object stackGet(int offset) throws EvalException;

  protected abstract int stackSize();

  public abstract Object getLocal(int index);

  protected abstract void setLocal(int index, Object value);

  // ---- hooks ----

  /** Applies a binary operator. VMs may specialize common cases but must keep the semantics. */
  protected Object binaryOp(TokenKind op, Object x, Object y) throws EvalException {
    return EvalUtils.binaryOp(op, x, y, thread);
  }

  /** Reads an attribute. VMs may cache lookups but must keep the semantics. */
  protected Object getAttr(Object object, String name) throws EvalException, InterruptedException {
    return Starlark.getattr(
        thread.mutability(), thread.getSemantics(), object, name, /*defaultValue=*/ null);
  }

  protected void onInstruction(Opcode opcode) {}

  protected void onCall() {}

  protected void onIteration() {}

  // ---- entry points ----

  /**
   * Runs a file's top-level code in a new {@link BytecodeToplevel} frame. {@link
   * Starlark#positionalOnlyCall} pushes the frame and wraps exceptions as it does for any callee.
   */
  static Object runToplevel(AbstractBytecodeVM vm) throws EvalException, InterruptedException {
    return Starlark.positionalOnlyCall(
        vm.thread, new BytecodeToplevel(vm.chunk.getName(), vm.filename, vm.globals, vm::run));
  }

  /**
   * Runs a function body whose frame is already pushed, starting from the given locals. {@code
   * locals} is the frame's array (see {@code StarlarkThread.Frame.getLocals}); stores are written
   * through to it so the debugger sees current values.
   */
  final Object runWithLocals(Object[] locals) throws EvalException, InterruptedException {
    int n = Math.min(locals.length, chunk.getLocalCount());
    for (int i = 0; i < n; i++) {
      setLocal(i, locals[i]);
    }
    frameLocals = locals;
    return run();
  }

  // The pushed frame's locals array, kept current for the debugger; null for top-level code.
  private Object[] frameLocals;

  @Override
  public void storeLocal(int index, Object value) {
    setLocal(index, value);
    if (frameLocals != null && index < frameLocals.length) {
      frameLocals[index] = value;
    }
  }

  // ---- execution ----

  /** Creates a Location object for the current instruction. */
  @Override
  public final Location currentLocation() {
    int lineNum = Math.max(0, chunk.getLineNumber(ip));
    return Location.fromFileLineColumn(filename, lineNum, chunk.getColumnNumber(ip));
  }

  private EvalException withLocation(EvalException ex) {
    return BcOps.withLocation(this, ex);
  }


  private void checkpoint() throws EvalException, InterruptedException {
    BcOps.checkpoint(thread);
  }

  final Object run() throws EvalException, InterruptedException {
    List<Instruction> instructions = chunk.getInstructions();
    boolean debug = Boolean.getBoolean("debug.bytecode");

    if (debug) {
      System.out.println("=== BYTECODE CHUNK (" + instructions.size() + " instructions) ===");
      for (int i = 0; i < instructions.size(); i++) {
        Instruction instr = instructions.get(i);
        System.out.printf("[%3d] %-20s", i, instr.getOpcode());
        if (instr.getOpcode().getOperandCount() >= 1) {
          System.out.printf(" %d", instr.getOperand1());
        }
        if (instr.getOpcode().getOperandCount() >= 2) {
          System.out.printf(" %d", instr.getOperand2());
        }
        System.out.println();
      }
      System.out.println("=== EXECUTION ===");
    }

    try {
      checkpoint();
      while (ip < instructions.size()) {
        // Every instruction counts as a step; the limit, interrupts and expiry are checked at
        // checkpoints (function entry, each loop iteration, each call), which every unbounded
        // computation passes through.
        thread.steps++;

        Instruction instr = instructions.get(ip);
        Opcode opcode = instr.getOpcode();
        onInstruction(opcode);

        if (debug) {
          System.out.printf("[%3d] %-20s  stack=%d", ip, opcode, stackSize());
          if (opcode.getOperandCount() >= 1) {
            System.out.printf(" op1=%d", instr.getOperand1());
          }
          if (opcode.getOperandCount() >= 2) {
            System.out.printf(" op2=%d", instr.getOperand2());
          }
          System.out.println();
        }

        // Execute instruction
        switch (opcode) {
          // Stack manipulation
          case POP:
            pop();
            break;

          case DUP:
            push(peek());
            break;

          case SWAP:
            {
              Object a = pop();
              Object b = pop();
              push(a);
              push(b);
            }
            break;

          case DUP_TOP_TWO:
            {
              Object b = pop();
              Object a = pop();
              push(a);
              push(b);
              push(a);
              push(b);
            }
            break;

          case ROT_THREE:
            {
              Object c = pop();
              Object b = pop();
              Object a = pop();
              push(c);
              push(a);
              push(b);
            }
            break;

          // Constants
          case LOAD_CONST:
            push(chunk.getConstantPool().getConstant(instr.getOperand1()));
            break;

          case LOAD_BYTES:
            push(StarlarkBytes.wrap(
                thread.mutability(), (byte[]) chunk.getConstantPool().getConstant(instr.getOperand1())));
            break;

          case LOAD_NONE:
            push(Starlark.NONE);
            break;

          case LOAD_TRUE:
            push(true);
            break;

          case LOAD_FALSE:
            push(false);
            break;

          // Variables
          case LOAD_LOCAL:
            {
              int index = instr.getOperand1();
              push(BcOps.checkLocal(this, getLocal(index), index));
            }
            break;

          case STORE_LOCAL:
            {
              int index = instr.getOperand1();
              storeLocal(index, pop());
            }
            break;

          case LOAD_GLOBAL:
            push(BcOps.loadGlobal(this, (String) chunk.getConstantPool().getConstant(instr.getOperand1())));
            break;

          case LOAD_BUILTIN:
            push(BcOps.loadBuiltin(this, (String) chunk.getConstantPool().getConstant(instr.getOperand1())));
            break;

          case STORE_GLOBAL:
            BcOps.storeGlobal(this, (String) chunk.getConstantPool().getConstant(instr.getOperand1()), pop());
            break;

          case LOAD_FREE:
            push(BcOps.loadFree(this, instr.getOperand1()));
            break;

          case STORE_FREE:
            BcOps.storeFree(this, instr.getOperand1(), pop());
            break;

          case LOAD_CELL:
            push(BcOps.loadCell(getLocal(instr.getOperand1())));
            break;

          case STORE_CELL:
            BcOps.storeCell(getLocal(instr.getOperand1()), pop());
            break;

          // Arithmetic
          case ADD:
            binary(TokenKind.PLUS);
            break;

          case SUBTRACT:
            binary(TokenKind.MINUS);
            break;

          case MULTIPLY:
            binary(TokenKind.STAR);
            break;

          case DIVIDE:
            binary(TokenKind.SLASH);
            break;

          case FLOOR_DIV:
            binary(TokenKind.SLASH_SLASH);
            break;

          case MODULO:
            binary(TokenKind.PERCENT);
            break;

          case BIT_AND:
            binary(TokenKind.AMPERSAND);
            break;

          case BIT_OR:
            binary(TokenKind.PIPE);
            break;

          case BIT_XOR:
            binary(TokenKind.CARET);
            break;

          case LEFT_SHIFT:
            binary(TokenKind.LESS_LESS);
            break;

          case RIGHT_SHIFT:
            binary(TokenKind.GREATER_GREATER);
            break;

          case INPLACE_OP:
            {
              Object y = pop();
              Object x = pop();
              push(BcOps.inplace(this, instr.getOperand1(), x, y));
            }
            break;

          case BIT_NOT:
            push(EvalUtils.unaryOp(TokenKind.TILDE, pop()));
            break;

          case NEGATE:
            push(EvalUtils.unaryOp(TokenKind.MINUS, pop()));
            break;

          case POSITIVE:
            push(EvalUtils.unaryOp(TokenKind.PLUS, pop()));
            break;

          // Comparison
          case EQUAL:
            binary(TokenKind.EQUALS_EQUALS);
            break;

          case NOT_EQUAL:
            binary(TokenKind.NOT_EQUALS);
            break;

          case LESS:
            binary(TokenKind.LESS);
            break;

          case LESS_EQUAL:
            binary(TokenKind.LESS_EQUALS);
            break;

          case GREATER:
            binary(TokenKind.GREATER);
            break;

          case GREATER_EQUAL:
            binary(TokenKind.GREATER_EQUALS);
            break;

          case IN:
            binary(TokenKind.IN);
            break;

          case NOT_IN:
            binary(TokenKind.NOT_IN);
            break;

          // Logical
          case AND:
            {
              Object b = pop();
              Object a = pop();
              push(BcOps.and(a, b));
            }
            break;

          case OR:
            {
              Object b = pop();
              Object a = pop();
              push(BcOps.or(a, b));
            }
            break;

          case NOT:
            push(BcOps.not(pop()));
            break;

          // Collections
          case BUILD_LIST:
            push(BcOps.buildList(this, popN(instr.getOperand1())));
            break;

          case BUILD_TUPLE:
            push(BcOps.buildTuple(popN(instr.getOperand1())));
            break;

          case BUILD_DICT:
            push(BcOps.buildDict(this, popN(2 * instr.getOperand1())));
            break;

          case UNPACK_SEQUENCE:
            for (Object elem : BcOps.unpack(pop(), instr.getOperand1())) {
              push(elem); // leftmost first, so the rightmost is on top of the stack
            }
            break;

          // Indexing
          case INDEX:
            {
              Object key = pop();
              Object object = pop();
              push(BcOps.index(this, object, key));
            }
            break;

          case STORE_INDEX:
            {
              // Stack: [value, object, key] (value pushed first by the RHS, then the LHS parts)
              Object key = pop();
              Object object = pop();
              Object value = pop();
              BcOps.setIndex(this, value, object, key);
            }
            break;

          case SLICE:
            {
              Object step = pop();
              Object stop = pop();
              Object start = pop();
              Object object = pop();
              push(BcOps.slice(this, object, start, stop, step));
            }
            break;

          // Attributes
          case LOAD_ATTR:
            {
              String name = (String) chunk.getConstantPool().getConstant(instr.getOperand1());
              Object object = pop();
              push(object instanceof BcOps.ModuleWithName
                  ? BcOps.getattr(this, object, name)
                  : getAttr(object, name));
            }
            break;

          case STORE_ATTR:
            {
              // Stack: [value, object] (the RHS value first, then the LHS object)
              String name = (String) chunk.getConstantPool().getConstant(instr.getOperand1());
              Object object = pop();
              Object value = pop();
              BcOps.setattr(value, object, name);
            }
            break;

          // Function calls
          case CALL:
            {
              // Stack: [function, pos1..posN, name1, value1, ..., nameK, valueK, (starstar)?]
              // Arguments reach the callee in source order, as in the tree-walker, so duplicate
              // and unexpected keyword errors come from the callee.
              int posArgs = instr.getOperand1();
              int encodedKwArgs = instr.getOperand2();
              boolean hasStarStar = (encodedKwArgs & 0x8000) != 0;
              int kwArgs = encodedKwArgs & 0x7FFF;
              Object starStar = hasStarStar ? pop() : null;
              Object[] named = popN(2 * kwArgs);
              Object[] positional = popN(posArgs);
              Object function = pop();
              onCall();
              push(hasStarStar
                  ? BcOps.callStarStar(this, function, positional, named, starStar)
                  : BcOps.call(this, function, positional, named));
            }
            break;

          case CALL_EX:
            {
              // Stack: [func, pos_list, kw_dict, star_arg?, starstar_arg?]
              int flags = instr.getOperand1();
              Object starStarArg = (flags & 2) != 0 ? pop() : null;
              Object starArg = (flags & 1) != 0 ? pop() : null;
              Object kwDict = pop();
              Object posList = pop();
              Object function = pop();
              onCall();
              push(BcOps.callEx(this, function, posList, kwDict, starArg, starStarArg));
            }
            break;

          case RETURN:
            return pop();

          // Control flow
          case JUMP:
            ip = instr.getOperand1() - 1; // -1 because we increment at end of loop
            break;

          case JUMP_IF_TRUE:
            if (Starlark.truth(peek())) {
              ip = instr.getOperand1() - 1;
            }
            break;

          case JUMP_IF_FALSE:
            if (!Starlark.truth(peek())) {
              ip = instr.getOperand1() - 1;
            }
            break;

          case POP_JUMP_IF_TRUE:
            if (Starlark.truth(pop())) {
              ip = instr.getOperand1() - 1;
            }
            break;

          case POP_JUMP_IF_FALSE:
            if (!Starlark.truth(pop())) {
              ip = instr.getOperand1() - 1;
            }
            break;

          // Iteration
          case GET_ITER:
            push(BcOps.getIter(this, pop()));
            break;

          case FOR_ITER:
            {
              Object iterator = peek();
              if (!BcOps.hasNext(this, iterator)) {
                // Exhausted: jump to END_FOR, which pops the iterator and releases its lock.
                ip = instr.getOperand1() - 1;
              } else {
                onIteration();
                push(BcOps.next(iterator));
              }
            }
            break;

          case END_FOR:
            BcOps.endFor(this, pop());
            break;

          case LIST_APPEND:
            {
              // LIST_APPEND(i): append TOS to the list at stack[-i] (offset before the pop).
              Object list = stackGet(instr.getOperand1());
              BcOps.listAppend(list, pop());
            }
            break;

          case DICT_ADD:
            {
              // DICT_ADD(i): add TOS1: TOS to the dict at stack[-i] (offset before the pops).
              Object dict = stackGet(instr.getOperand1());
              Object value = pop();
              Object key = pop();
              BcOps.dictAdd(dict, key, value);
            }
            break;

          case NOP:
            // No operation
            break;

          case POST_ASSIGN:
            BcOps.postAssign(
                this, (String) chunk.getConstantPool().getConstant(instr.getOperand1()), pop());
            break;

          case TYPE_ALIAS:
            BcOps.typeAlias(this, chunk.getConstantPool().getConstant(instr.getOperand1()));
            break;

          case MAKE_FUNCTION:
            {
              Object descriptor = chunk.getConstantPool().getConstant(instr.getOperand1());
              Object[] defaults = popN(instr.getOperand2());
              push(BcOps.makeFunction(this, descriptor, defaults));
            }
            break;

          case LOAD_MODULE:
            push(BcOps.loadModule(
                this, (String) chunk.getConstantPool().getConstant(instr.getOperand1())));
            break;

          default:
            throw new UnsupportedOperationException("Unsupported opcode: " + opcode);
        }

        ip++;
      }

      // If we reach here without returning, return None
      return Starlark.NONE;

    } catch (EvalException ex) {
      throw withLocation(ex);
    } finally {
      // Release iteration locks of loops left by return or by an exception, as the
      // tree-walker's finally blocks do. END_FOR has already released loops that completed.
      BcOps.releaseIterators(this);
    }
    // Other exceptions propagate unchanged, as in the tree-walker: Starlark.fastcall already
    // reports failures inside callees as UncheckedEvalException.
  }

  private void binary(TokenKind op) throws EvalException {
    Object y = pop();
    Object x = pop();
    push(binaryOp(op, x, y));
  }

  /** Pops {@code n} values, returning them deepest first. */
  private Object[] popN(int n) throws EvalException {
    Object[] values = new Object[n];
    for (int i = n - 1; i >= 0; i--) {
      values[i] = pop();
    }
    return values;
  }
}
