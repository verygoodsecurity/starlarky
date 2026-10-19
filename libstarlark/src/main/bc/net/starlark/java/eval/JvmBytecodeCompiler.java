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

import static org.objectweb.asm.Opcodes.*;

import java.lang.invoke.MethodHandles;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.starlark.java.eval.compiler.BytecodeChunk;
import net.starlark.java.eval.compiler.Instruction;
import net.starlark.java.eval.compiler.Opcode;
import net.starlark.java.syntax.TokenKind;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodTooLargeException;
import org.objectweb.asm.MethodVisitor;

/**
 * Compiles a {@link BytecodeChunk} to a JVM method, so that HotSpot compiles Starlark code itself
 * rather than the interpreter loop.
 *
 * <p>The Starlark operand stack becomes JVM local variables: the stack depth before every
 * instruction is fixed (checked here), so stack slot {@code k} is always the same JVM local.
 * Starlark locals stay in {@link JvmFrame#locals}. Every operation that does more than move a
 * value calls the same {@link BcOps} helper the interpreting VMs use, so compiled and interpreted
 * code behave identically. Before an instruction that can fail or call, the generated code stores
 * its index in {@link JvmFrame#ip}, from which errors get the tree-walker's locations.
 *
 * <p>The generated class is a hidden class in this package, so it can be unloaded with its chunk.
 */
final class JvmBytecodeCompiler {

  private JvmBytecodeCompiler() {}

  private static final String FRAME = "net/starlark/java/eval/JvmFrame";
  private static final String BCFRAME = "Lnet/starlark/java/eval/BcFrame;";
  private static final String OPS = "net/starlark/java/eval/BcOps";
  private static final String OBJ = "Ljava/lang/Object;";
  private static final String OBJS = "[Ljava/lang/Object;";
  private static final String STR = "Ljava/lang/String;";

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

  /** Thrown when a chunk uses something this compiler does not handle. */
  static final class UnsupportedChunkException extends RuntimeException {
    UnsupportedChunkException(String message) {
      super(message);
    }
  }

  /**
   * Executions of a chunk on the interpreter before it is compiled: code that runs once or a few
   * times (a request's own script, a module's top level) is not worth generating a class for.
   * {@code -Dstarlark.jit.threshold=0} compiles everything on first use.
   */
  static int threshold = Integer.getInteger("starlark.jit.threshold", 50);

  /**
   * Returns the compiled code for {@code chunk} if it has run often enough to be worth compiling
   * (compiling it now if needed), or null to run it on the interpreter this time.
   */
  static JvmCode hotCodeFor(BytecodeChunk chunk) {
    Object code = chunk.getJitCode();
    if (code instanceof JvmCode jvmCode) {
      return jvmCode;
    }
    if (code == null && chunk.countExecution() > threshold) {
      return codeFor(chunk);
    }
    return null;
  }

  /**
   * Marks a chunk that is not compiled, because it is too large for one JVM method (64KB of
   * bytecode) or too costly to generate (see {@link #MAX_CODEGEN_COST}); it stays interpreted.
   */
  private static final Object TOO_LARGE = new Object();

  /**
   * Largest {@link #codegenCost} of a chunk that is compiled. Generating a chunk's class takes
   * memory proportional to its cost (ASM computes a frame of every local at every label), so a
   * chunk the script makes arbitrarily costly, such as one with a list literal of thousands of
   * elements, would otherwise exhaust the heap. Of Larky's own modules, only
   * Crypto/Util/number.star's top level exceeds the default; its functions all cost under 25,000.
   */
  static final long MAX_CODEGEN_COST = Long.getLong("starlark.jit.maxCost", 1_000_000);

  /**
   * Returns the compiled code for {@code chunk}, compiling it on first use, or null if the chunk
   * is too large or too costly to compile, in which case it runs on the interpreter.
   */
  static JvmCode codeFor(BytecodeChunk chunk) {
    Object code = chunk.getJitCode();
    if (code == null) {
      synchronized (chunk) {
        code = chunk.getJitCode();
        if (code == null) {
          if (codegenCost(chunk) > MAX_CODEGEN_COST) {
            code = TOO_LARGE;
          } else {
            try {
              code = compile(chunk);
            } catch (MethodTooLargeException | org.objectweb.asm.ClassTooLargeException e) {
              code = TOO_LARGE;
            }
          }
          chunk.setJitCode(code);
        }
      }
    }
    return code == TOO_LARGE ? null : (JvmCode) code;
  }

  static JvmCode compile(BytecodeChunk chunk) {
    byte[] bytes = generate(chunk);
    try {
      Class<?> clazz = LOOKUP.defineHiddenClass(bytes, true).lookupClass();
      return (JvmCode) clazz.getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("cannot load compiled chunk " + chunk.getName(), e);
    }
  }

  // ---- execution ----

  /** Runs a file's top-level code in a new {@link BytecodeToplevel} frame. */
  static Object runToplevel(
      BytecodeChunk chunk, StarlarkThread thread, Map<String, Object> globals, String filename)
      throws EvalException, InterruptedException {
    JvmCode code = hotCodeFor(chunk);
    if (code == null) {
      return BytecodeInterpreter.execute(chunk, thread, globals, filename);
    }
    JvmFrame f =
        new JvmFrame(chunk, thread, globals, filename, null, new Object[chunk.getLocalCount()]);
    return Starlark.positionalOnlyCall(
        thread,
        new BytecodeToplevel(chunk.getName(), filename, globals, () -> run(code, f)));
  }

  /** Runs a function body whose frame is pushed; {@code locals} is that frame's locals array. */
  static Object runWithLocals(
      BytecodeChunk chunk,
      StarlarkThread thread,
      Object[] locals,
      Map<String, Object> globals,
      String filename,
      Tuple freevars)
      throws EvalException, InterruptedException {
    if (locals.length < chunk.getLocalCount()) {
      locals = Arrays.copyOf(locals, chunk.getLocalCount());
    }
    JvmCode code = hotCodeFor(chunk);
    if (code == null) {
      return BytecodeInterpreter.executeWithLocals(chunk, thread, locals, globals, filename, freevars);
    }
    return run(code, new JvmFrame(chunk, thread, globals, filename, freevars, locals));
  }

  private static Object run(JvmCode code, JvmFrame f) throws EvalException, InterruptedException {
    try {
      return code.run(f);
    } catch (EvalException ex) {
      throw BcOps.withLocation(f, ex);
    } finally {
      // Release iteration locks of loops left by return or by an exception.
      if (f.hasIterators()) {
        BcOps.releaseIterators(f);
      }
    }
  }

  // ---- stack analysis ----

  /** Returns the stack depth before each instruction, or -1 for unreachable ones. */
  static int[] stackDepths(List<Instruction> code) {
    int n = code.size();
    int[] depth = new int[n];
    Arrays.fill(depth, -1);
    ArrayDeque<int[]> work = new ArrayDeque<>();
    work.add(new int[] {0, 0});
    while (!work.isEmpty()) {
      int[] item = work.poll();
      int i = item[0];
      int d = item[1];
      if (i >= n) {
        continue; // falls off the end: returns None
      }
      if (depth[i] >= 0) {
        if (depth[i] != d) {
          throw new UnsupportedChunkException(
              "inconsistent stack depth at " + i + ": " + depth[i] + " vs " + d);
        }
        continue;
      }
      depth[i] = d;
      Instruction in = code.get(i);
      Opcode op = in.getOpcode();
      switch (op) {
        case RETURN:
          break;
        case JUMP:
          work.add(new int[] {in.getOperand1(), d});
          break;
        case JUMP_IF_TRUE:
        case JUMP_IF_FALSE:
          work.add(new int[] {in.getOperand1(), d});
          work.add(new int[] {i + 1, d});
          break;
        case POP_JUMP_IF_TRUE:
        case POP_JUMP_IF_FALSE:
          work.add(new int[] {in.getOperand1(), d - 1});
          work.add(new int[] {i + 1, d - 1});
          break;
        case FOR_ITER:
          work.add(new int[] {in.getOperand1(), d}); // exhausted: iterator still on the stack
          work.add(new int[] {i + 1, d + 1});
          break;
        default:
          int after = d + stackEffect(in);
          if (after < 0) {
            throw new UnsupportedChunkException("stack underflow at " + i);
          }
          work.add(new int[] {i + 1, after});
      }
    }
    return depth;
  }

  private static int stackEffect(Instruction in) {
    int a = in.getOperand1();
    switch (in.getOpcode()) {
      case POP:
      case STORE_LOCAL:
      case STORE_GLOBAL:
      case STORE_FREE:
      case STORE_CELL:
      case END_FOR:
      case LIST_APPEND:
      case POST_ASSIGN:
      case INDEX:
      case ADD:
      case SUBTRACT:
      case MULTIPLY:
      case DIVIDE:
      case FLOOR_DIV:
      case MODULO:
      case BIT_AND:
      case BIT_OR:
      case BIT_XOR:
      case LEFT_SHIFT:
      case RIGHT_SHIFT:
      case EQUAL:
      case NOT_EQUAL:
      case LESS:
      case LESS_EQUAL:
      case GREATER:
      case GREATER_EQUAL:
      case IN:
      case NOT_IN:
      case INPLACE_OP:
      case AND:
      case OR:
        return -1;
      case DUP:
      case LOAD_CONST:
      case LOAD_BYTES:
      case LOAD_NONE:
      case LOAD_TRUE:
      case LOAD_FALSE:
      case LOAD_LOCAL:
      case LOAD_GLOBAL:
      case LOAD_BUILTIN:
      case LOAD_FREE:
      case LOAD_CELL:
      case LOAD_MODULE:
        return 1;
      case SWAP:
      case ROT_THREE:
      case NOP:
      case TYPE_ALIAS:
      case BIT_NOT:
      case NEGATE:
      case POSITIVE:
      case NOT:
      case GET_ITER:
      case LOAD_ATTR:
        return 0;
      case DUP_TOP_TWO:
        return 2;
      case DICT_ADD:
      case STORE_ATTR:
        return -2;
      case STORE_INDEX:
      case SLICE:
        return -3;
      case BUILD_LIST:
      case BUILD_TUPLE:
        return 1 - a;
      case BUILD_DICT:
        return 1 - 2 * a;
      case UNPACK_SEQUENCE:
        return a - 1;
      case MAKE_FUNCTION:
        return 1 - in.getOperand2();
      case CALL:
        {
          int enc = in.getOperand2();
          int kw = enc & 0x7FFF;
          boolean starStar = (enc & 0x8000) != 0;
          return 1 - (1 + a + 2 * kw + (starStar ? 1 : 0));
        }
      case CALL_EX:
        return 1 - (3 + (a & 1) + ((a >> 1) & 1));
      default:
        throw new UnsupportedChunkException("unsupported opcode " + in.getOpcode());
    }
  }

  // ---- code generation ----

  /**
   * Generates the class for {@code chunk}: one method if it fits the JVM's 64KB limit on a
   * method's code, otherwise a dispatcher plus one method per segment, splitting only where the
   * Starlark stack is empty so no stack slot crosses a segment boundary.
   */
  static byte[] generate(BytecodeChunk chunk) {
    try {
      return generate(chunk, Integer.MAX_VALUE);
    } catch (MethodTooLargeException e) {
      // Split into smaller and smaller segments until every method fits.
      for (int budget = 2048; budget >= 32; budget /= 2) {
        try {
          return generate(chunk, budget);
        } catch (MethodTooLargeException retry) {
          // try smaller segments
        }
      }
      throw e;
    }
  }

  /**
   * The cost of generating {@code chunk}'s class: its instructions times the JVM locals each of
   * its methods has, since every instruction gets a label and ASM keeps a frame of the locals at
   * each one.
   */
  static long codegenCost(BytecodeChunk chunk) {
    List<Instruction> code = chunk.getInstructions();
    return (long) code.size() * (SLOTS + maxStackDepth(code, stackDepths(code)));
  }

  /** The deepest the Starlark stack gets in {@code code}, including within an instruction. */
  private static int maxStackDepth(List<Instruction> code, int[] depth) {
    int maxDepth = 0;
    for (int i = 0; i < code.size(); i++) {
      if (depth[i] >= 0) {
        maxDepth = Math.max(maxDepth, depth[i] + Math.max(0, stackEffect0(code.get(i))));
      }
    }
    return maxDepth;
  }

  private static byte[] generate(BytecodeChunk chunk, int segmentBudget) {
    List<Instruction> code = chunk.getInstructions();
    int n = code.size();
    int[] depth = stackDepths(code);
    int maxDepth = maxStackDepth(code, depth);
    int[] starts = segmentStarts(code, depth, segmentBudget);

    // Basic-block leaders, for step counting: one addition per block instead of per instruction.
    boolean[] leader = new boolean[n + 1];
    leader[0] = true;
    for (int start : starts) {
      leader[start] = true;
    }
    for (int i = 0; i < n; i++) {
      Instruction in = code.get(i);
      switch (in.getOpcode()) {
        case JUMP:
        case JUMP_IF_TRUE:
        case JUMP_IF_FALSE:
        case POP_JUMP_IF_TRUE:
        case POP_JUMP_IF_FALSE:
        case FOR_ITER:
          leader[Math.min(in.getOperand1(), n)] = true;
          leader[i + 1] = true;
          break;
        case RETURN:
          leader[i + 1] = true;
          break;
        default:
      }
    }

    ClassWriter cw =
        new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
          @Override
          protected String getCommonSuperClass(String a, String b) {
            return "java/lang/Object"; // all merged values are Objects; avoid class loading
          }
        };
    cw.visit(
        V21,
        ACC_PUBLIC | ACC_FINAL | ACC_SUPER,
        "net/starlark/java/eval/JvmCompiledChunk",
        null,
        "java/lang/Object",
        new String[] {"net/starlark/java/eval/JvmCode"});
    cw.visitSource(chunk.getName(), null);

    MethodVisitor init = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
    init.visitCode();
    init.visitVarInsn(ALOAD, 0);
    init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    init.visitInsn(RETURN);
    init.visitMaxs(0, 0);
    init.visitEnd();

    String[] exceptions = {
      "net/starlark/java/eval/EvalException", "java/lang/InterruptedException"
    };
    MethodVisitor run =
        cw.visitMethod(ACC_PUBLIC | ACC_FINAL, "run", "(L" + FRAME + ";)" + OBJ, null, exceptions);
    run.visitCode();
    // Checkpoint on entry, as the interpreter does.
    run.visitVarInsn(ALOAD, 1);
    run.visitFieldInsn(GETFIELD, FRAME, "thread", "Lnet/starlark/java/eval/StarlarkThread;");
    run.visitMethodInsn(
        INVOKESTATIC, OPS, "checkpoint", "(Lnet/starlark/java/eval/StarlarkThread;)V", false);

    if (starts.length == 1) {
      emitSegment(run, chunk, code, depth, leader, maxDepth, 0, n, false);
    } else {
      // run(): ip = 0; loop { switch (ip) { case start_k: ip = seg_k(f); ... } }
      int ipLocal = 2;
      run.visitInsn(ICONST_0);
      run.visitVarInsn(ISTORE, ipLocal);
      Label loop = new Label();
      Label bad = new Label();
      Label done = new Label();
      Label returned = new Label();
      int[] keys = new int[starts.length + 2];
      Label[] targets = new Label[starts.length + 2];
      keys[0] = -1;
      targets[0] = returned;
      for (int k = 0; k < starts.length; k++) {
        keys[k + 1] = starts[k];
        targets[k + 1] = new Label();
      }
      keys[starts.length + 1] = n;
      targets[starts.length + 1] = done;
      run.visitLabel(loop);
      run.visitVarInsn(ILOAD, ipLocal);
      run.visitLookupSwitchInsn(bad, keys, targets);
      for (int k = 0; k < starts.length; k++) {
        run.visitLabel(targets[k + 1]);
        run.visitVarInsn(ALOAD, 0);
        run.visitVarInsn(ALOAD, 1);
        run.visitMethodInsn(
            INVOKEVIRTUAL, "net/starlark/java/eval/JvmCompiledChunk", "seg" + k,
            "(L" + FRAME + ";)I", false);
        run.visitVarInsn(ISTORE, ipLocal);
        run.visitJumpInsn(GOTO, loop);
      }
      run.visitLabel(returned);
      run.visitVarInsn(ALOAD, 1);
      run.visitFieldInsn(GETFIELD, FRAME, "retval", OBJ);
      run.visitInsn(ARETURN);
      run.visitLabel(done);
      run.visitFieldInsn(GETSTATIC, "net/starlark/java/eval/Starlark", "NONE",
          "Lnet/starlark/java/eval/NoneType;");
      run.visitInsn(ARETURN);
      run.visitLabel(bad);
      run.visitTypeInsn(NEW, "java/lang/IllegalStateException");
      run.visitInsn(DUP);
      run.visitLdcInsn("bad segment index");
      run.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>",
          "(Ljava/lang/String;)V", false);
      run.visitInsn(ATHROW);

      for (int k = 0; k < starts.length; k++) {
        int end = k + 1 < starts.length ? starts[k + 1] : n;
        MethodVisitor seg =
            cw.visitMethod(ACC_PRIVATE | ACC_FINAL, "seg" + k, "(L" + FRAME + ";)I", null,
                exceptions);
        seg.visitCode();
        emitSegment(seg, chunk, code, depth, leader, maxDepth, starts[k], end, true);
        seg.visitMaxs(0, 0);
        seg.visitEnd();
      }
    }
    run.visitMaxs(0, 0);
    run.visitEnd();
    cw.visitEnd();
    return cw.toByteArray();
  }

  /**
   * Chooses segment starts: 0, then instructions where the Starlark stack is empty, at most every
   * {@code budget} instructions, plus every target of a jump that would cross segments.
   */
  private static int[] segmentStarts(List<Instruction> code, int[] depth, int budget) {
    int n = code.size();
    java.util.TreeSet<Integer> starts = new java.util.TreeSet<>();
    starts.add(0);
    if (budget == Integer.MAX_VALUE) {
      return new int[] {0};
    }
    int size = 0;
    for (int i = 0; i < n; i++) {
      if (size >= budget && depth[i] == 0) {
        starts.add(i);
        size = 0;
      }
      size++;
    }
    // A jump to another segment must land on a segment start (where the stack is empty).
    boolean changed = true;
    while (changed) {
      changed = false;
      for (int i = 0; i < n; i++) {
        if (depth[i] < 0 || !isJump(code.get(i).getOpcode())) {
          continue;
        }
        int t = code.get(i).getOperand1();
        if (t >= n || starts.contains(t)) {
          continue;
        }
        if (!java.util.Objects.equals(starts.floor(i), starts.floor(t))) {
          if (depth[t] != 0) {
            throw new MethodTooLargeException("JvmCompiledChunk", "seg", "", 0);
          }
          starts.add(t);
          changed = true;
        }
      }
    }
    return starts.stream().mapToInt(Integer::intValue).toArray();
  }

  private static boolean isJump(Opcode op) {
    switch (op) {
      case JUMP:
      case JUMP_IF_TRUE:
      case JUMP_IF_FALSE:
      case POP_JUMP_IF_TRUE:
      case POP_JUMP_IF_FALSE:
      case FOR_ITER:
        return true;
      default:
        return false;
    }
  }

  /**
   * Emits instructions [from, to). As a segment method ({@code segment}), leaving the range
   * returns the next instruction index, and RETURN stores f.retval and returns -1; otherwise the
   * code is the whole of run().
   */
  private static void emitSegment(
      MethodVisitor mv,
      BytecodeChunk chunk,
      List<Instruction> code,
      int[] depth,
      boolean[] leader,
      int maxDepth,
      int from,
      int to,
      boolean segment) {
    int n = code.size();
    Gen g = new Gen(mv, SLOTS + maxDepth, segment, from, to);
    Label[] labels = new Label[n + 1];
    for (int i = 0; i <= n; i++) {
      labels[i] = new Label();
    }
    // Keep f.consts and f.locals in JVM locals.
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(GETFIELD, FRAME, "consts", OBJS);
    mv.visitVarInsn(ASTORE, CONSTS);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(GETFIELD, FRAME, "locals", OBJS);
    mv.visitVarInsn(ASTORE, LOCALS);

    for (int i = from; i < to; i++) {
      mv.visitLabel(labels[i]);
      if (depth[i] < 0) {
        continue; // unreachable
      }
      mv.visitLineNumber(Math.max(1, chunk.getLineNumber(i)), labels[i]);
      if (leader[i]) {
        int len = 0;
        for (int j = i; j < n && (j == i || !leader[j]); j++) {
          len++;
        }
        g.frame();
        g.getfield("thread", "Lnet/starlark/java/eval/StarlarkThread;");
        mv.visitLdcInsn((long) len);
        mv.visitMethodInsn(
            INVOKESTATIC, OPS, "addSteps", "(Lnet/starlark/java/eval/StarlarkThread;J)V", false);
      }
      g.emit(code.get(i), i, depth[i], labels);
    }
    mv.visitLabel(labels[to]);
    if (segment) {
      g.iconst(to); // fall through into the next segment (or off the end: n)
      mv.visitInsn(IRETURN);
    } else {
      mv.visitFieldInsn(GETSTATIC, "net/starlark/java/eval/Starlark", "NONE",
          "Lnet/starlark/java/eval/NoneType;");
      mv.visitInsn(ARETURN);
    }
  }

  /** The instruction's net push count at its peak (for sizing the slot area). */
  private static int stackEffect0(Instruction in) {
    switch (in.getOpcode()) {
      case RETURN:
      case JUMP:
        return 0;
      case JUMP_IF_TRUE:
      case JUMP_IF_FALSE:
        return 0;
      case POP_JUMP_IF_TRUE:
      case POP_JUMP_IF_FALSE:
        return 0;
      case FOR_ITER:
        return 1;
      default:
        return stackEffect(in);
    }
  }

  private static final TokenKind[] BINARY = new TokenKind[Opcode.values().length];

  static {
    BINARY[Opcode.ADD.ordinal()] = TokenKind.PLUS;
    BINARY[Opcode.SUBTRACT.ordinal()] = TokenKind.MINUS;
    BINARY[Opcode.MULTIPLY.ordinal()] = TokenKind.STAR;
    BINARY[Opcode.DIVIDE.ordinal()] = TokenKind.SLASH;
    BINARY[Opcode.FLOOR_DIV.ordinal()] = TokenKind.SLASH_SLASH;
    BINARY[Opcode.MODULO.ordinal()] = TokenKind.PERCENT;
    BINARY[Opcode.BIT_AND.ordinal()] = TokenKind.AMPERSAND;
    BINARY[Opcode.BIT_OR.ordinal()] = TokenKind.PIPE;
    BINARY[Opcode.BIT_XOR.ordinal()] = TokenKind.CARET;
    BINARY[Opcode.LEFT_SHIFT.ordinal()] = TokenKind.LESS_LESS;
    BINARY[Opcode.RIGHT_SHIFT.ordinal()] = TokenKind.GREATER_GREATER;
    BINARY[Opcode.EQUAL.ordinal()] = TokenKind.EQUALS_EQUALS;
    BINARY[Opcode.NOT_EQUAL.ordinal()] = TokenKind.NOT_EQUALS;
    BINARY[Opcode.LESS.ordinal()] = TokenKind.LESS;
    BINARY[Opcode.LESS_EQUAL.ordinal()] = TokenKind.LESS_EQUALS;
    BINARY[Opcode.GREATER.ordinal()] = TokenKind.GREATER;
    BINARY[Opcode.GREATER_EQUAL.ordinal()] = TokenKind.GREATER_EQUALS;
    BINARY[Opcode.IN.ordinal()] = TokenKind.IN;
    BINARY[Opcode.NOT_IN.ordinal()] = TokenKind.NOT_IN;
  }

  // JVM locals: 0 this, 1 the frame, 2 f.consts, 3 f.locals, then the stack slots.
  private static final int CONSTS = 2;
  private static final int LOCALS = 3;
  private static final int SLOTS = 4;

  /** Emits the code for single instructions. Stack slot k lives in JVM local {@code SLOTS + k}. */
  private static final class Gen {
    private final MethodVisitor mv;
    private final int temp; // first free JVM local after the stack slots
    private final boolean segment; // emitting a segment method: exits return the next index
    private final int from;
    private final int to;

    Gen(MethodVisitor mv, int temp, boolean segment, int from, int to) {
      this.mv = mv;
      this.temp = temp;
      this.segment = segment;
      this.from = from;
      this.to = to;
    }

    /** Jumps to instruction {@code target}, leaving the segment if it lies outside it. */
    void jump(int jvmOp, int target, Label[] labels) {
      if (!segment || (target >= from && target < to)) {
        mv.visitJumpInsn(jvmOp, labels[target]);
        return;
      }
      if (jvmOp == GOTO) {
        iconst(target);
        mv.visitInsn(IRETURN);
        return;
      }
      Label stay = new Label();
      mv.visitJumpInsn(jvmOp == IFNE ? IFEQ : IFNE, stay);
      iconst(target);
      mv.visitInsn(IRETURN);
      mv.visitLabel(stay);
    }

    void frame() {
      mv.visitVarInsn(ALOAD, 1);
    }

    void getfield(String name, String desc) {
      mv.visitFieldInsn(GETFIELD, FRAME, name, desc);
    }

    void load(int slot) {
      mv.visitVarInsn(ALOAD, SLOTS + slot);
    }

    void store(int slot) {
      mv.visitVarInsn(ASTORE, SLOTS + slot);
    }

    void localsArray() {
      mv.visitVarInsn(ALOAD, LOCALS);
    }

    void iconst(int v) {
      if (v >= -1 && v <= 5) {
        mv.visitInsn(ICONST_0 + v);
      } else if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
        mv.visitIntInsn(BIPUSH, v);
      } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
        mv.visitIntInsn(SIPUSH, v);
      } else {
        mv.visitLdcInsn(v);
      }
    }

    /** f.ip = i */
    void setIp(int i) {
      frame();
      iconst(i);
      mv.visitFieldInsn(PUTFIELD, FRAME, "ip", "I");
    }

    /** Pushes f.consts[index]. */
    void constant(int index) {
      mv.visitVarInsn(ALOAD, CONSTS);
      iconst(index);
      mv.visitInsn(AALOAD);
    }

    void constantString(int index) {
      constant(index);
      mv.visitTypeInsn(CHECKCAST, "java/lang/String");
    }

    void ops(String name, String desc) {
      mv.visitMethodInsn(INVOKESTATIC, OPS, name, desc, false);
    }

    /** Builds an Object[] from stack slots [from, from + count). */
    void array(int from, int count) {
      iconst(count);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      for (int k = 0; k < count; k++) {
        mv.visitInsn(DUP);
        iconst(k);
        load(from + k);
        mv.visitInsn(AASTORE);
      }
    }

    void truth(int slot) {
      load(slot);
      ops("truth", "(" + OBJ + ")Z");
    }

    void emit(Instruction in, int i, int d, Label[] labels) {
      int a = in.getOperand1();
      int top = d - 1; // slot of TOS
      Opcode op = in.getOpcode();
      switch (op) {
        case POP:
        case NOP:
          return;
        case DUP:
          load(top);
          store(d);
          return;
        case SWAP:
          load(top);
          mv.visitVarInsn(ASTORE, temp);
          load(top - 1);
          store(top);
          mv.visitVarInsn(ALOAD, temp);
          store(top - 1);
          return;
        case DUP_TOP_TWO:
          load(top - 1);
          store(d);
          load(top);
          store(d + 1);
          return;
        case ROT_THREE:
          // [a, b, c] -> [c, a, b]
          load(top);
          mv.visitVarInsn(ASTORE, temp);
          load(top - 1);
          store(top);
          load(top - 2);
          store(top - 1);
          mv.visitVarInsn(ALOAD, temp);
          store(top - 2);
          return;

        case LOAD_CONST:
          constant(a);
          store(d);
          return;
        case LOAD_BYTES:
          frame();
          constant(a);
          ops("loadBytes", "(" + BCFRAME + OBJ + ")" + OBJ);
          store(d);
          return;
        case LOAD_NONE:
          mv.visitFieldInsn(GETSTATIC, "net/starlark/java/eval/Starlark", "NONE",
              "Lnet/starlark/java/eval/NoneType;");
          store(d);
          return;
        case LOAD_TRUE:
        case LOAD_FALSE:
          mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", op == Opcode.LOAD_TRUE ? "TRUE" : "FALSE",
              "Ljava/lang/Boolean;");
          store(d);
          return;

        case LOAD_LOCAL:
          setIp(i);
          frame();
          localsArray();
          iconst(a);
          mv.visitInsn(AALOAD);
          iconst(a);
          ops("checkLocal", "(" + BCFRAME + OBJ + "I)" + OBJ);
          store(d);
          return;
        case STORE_LOCAL:
          localsArray();
          iconst(a);
          load(top);
          mv.visitInsn(AASTORE);
          return;
        case LOAD_GLOBAL:
        case LOAD_BUILTIN:
          setIp(i);
          frame();
          constantString(a);
          ops(op == Opcode.LOAD_GLOBAL ? "loadGlobal" : "loadBuiltin",
              "(" + BCFRAME + STR + ")" + OBJ);
          store(d);
          return;
        case STORE_GLOBAL:
          frame();
          constantString(a);
          load(top);
          ops("storeGlobal", "(" + BCFRAME + STR + OBJ + ")V");
          return;
        case LOAD_FREE:
          frame();
          iconst(a);
          ops("loadFree", "(" + BCFRAME + "I)" + OBJ);
          store(d);
          return;
        case STORE_FREE:
          frame();
          iconst(a);
          load(top);
          ops("storeFree", "(" + BCFRAME + "I" + OBJ + ")V");
          return;
        case LOAD_CELL:
          localsArray();
          iconst(a);
          mv.visitInsn(AALOAD);
          ops("loadCell", "(" + OBJ + ")" + OBJ);
          store(d);
          return;
        case STORE_CELL:
          localsArray();
          iconst(a);
          mv.visitInsn(AALOAD);
          load(top);
          ops("storeCell", "(" + OBJ + OBJ + ")V");
          return;

        case ADD:
        case SUBTRACT:
        case MULTIPLY:
        case DIVIDE:
        case FLOOR_DIV:
        case MODULO:
        case BIT_AND:
        case BIT_OR:
        case BIT_XOR:
        case LEFT_SHIFT:
        case RIGHT_SHIFT:
        case EQUAL:
        case NOT_EQUAL:
        case LESS:
        case LESS_EQUAL:
        case GREATER:
        case GREATER_EQUAL:
        case IN:
        case NOT_IN:
          setIp(i);
          frame();
          iconst(BINARY[op.ordinal()].ordinal());
          load(top - 1);
          load(top);
          ops("binary", "(" + BCFRAME + "I" + OBJ + OBJ + ")" + OBJ);
          store(top - 1);
          return;
        case INPLACE_OP:
          setIp(i);
          frame();
          iconst(a);
          load(top - 1);
          load(top);
          ops("inplace", "(" + BCFRAME + "I" + OBJ + OBJ + ")" + OBJ);
          store(top - 1);
          return;
        case BIT_NOT:
        case NEGATE:
        case POSITIVE:
          setIp(i);
          iconst((op == Opcode.BIT_NOT ? TokenKind.TILDE
              : op == Opcode.NEGATE ? TokenKind.MINUS : TokenKind.PLUS).ordinal());
          load(top);
          ops("unary", "(I" + OBJ + ")" + OBJ);
          store(top);
          return;
        case AND:
        case OR:
          load(top - 1);
          load(top);
          ops(op == Opcode.AND ? "and" : "or", "(" + OBJ + OBJ + ")" + OBJ);
          store(top - 1);
          return;
        case NOT:
          load(top);
          ops("not", "(" + OBJ + ")" + OBJ);
          store(top);
          return;

        case BUILD_LIST:
          setIp(i);
          frame();
          array(d - a, a);
          ops("buildList", "(" + BCFRAME + OBJS + ")" + OBJ);
          store(d - a);
          return;
        case BUILD_TUPLE:
          array(d - a, a);
          ops("buildTuple", "(" + OBJS + ")" + OBJ);
          store(d - a);
          return;
        case BUILD_DICT:
          setIp(i);
          frame();
          array(d - 2 * a, 2 * a);
          ops("buildDict", "(" + BCFRAME + OBJS + ")" + OBJ);
          store(d - 2 * a);
          return;
        case UNPACK_SEQUENCE:
          setIp(i);
          load(top);
          iconst(a);
          ops("unpack", "(" + OBJ + "I)" + OBJS);
          mv.visitVarInsn(ASTORE, temp);
          for (int k = 0; k < a; k++) {
            mv.visitVarInsn(ALOAD, temp);
            iconst(k);
            mv.visitInsn(AALOAD);
            store(top + k);
          }
          return;

        case INDEX:
          setIp(i);
          frame();
          load(top - 1);
          load(top);
          ops("index", "(" + BCFRAME + OBJ + OBJ + ")" + OBJ);
          store(top - 1);
          return;
        case STORE_INDEX:
          // [value, object, key]
          setIp(i);
          frame();
          load(top - 2);
          load(top - 1);
          load(top);
          ops("setIndex", "(" + BCFRAME + OBJ + OBJ + OBJ + ")V");
          return;
        case SLICE:
          setIp(i);
          frame();
          load(top - 3);
          load(top - 2);
          load(top - 1);
          load(top);
          ops("slice", "(" + BCFRAME + OBJ + OBJ + OBJ + OBJ + ")" + OBJ);
          store(top - 3);
          return;
        case LOAD_ATTR:
          setIp(i);
          frame();
          load(top);
          constantString(a);
          ops("getattr", "(" + BCFRAME + OBJ + STR + ")" + OBJ);
          store(top);
          return;
        case STORE_ATTR:
          // [value, object]
          setIp(i);
          load(top - 1);
          load(top);
          constantString(a);
          ops("setattr", "(" + OBJ + OBJ + STR + ")V");
          return;

        case CALL:
          {
            int enc = in.getOperand2();
            int kw = enc & 0x7FFF;
            boolean starStar = (enc & 0x8000) != 0;
            int base = d - (1 + a + 2 * kw + (starStar ? 1 : 0)); // slot of the function
            setIp(i);
            frame();
            load(base);
            array(base + 1, a);
            array(base + 1 + a, 2 * kw);
            if (starStar) {
              load(base + 1 + a + 2 * kw);
              ops("callStarStar", "(" + BCFRAME + OBJ + OBJS + OBJS + OBJ + ")" + OBJ);
            } else {
              ops("call", "(" + BCFRAME + OBJ + OBJS + OBJS + ")" + OBJ);
            }
            store(base);
            return;
          }
        case CALL_EX:
          {
            boolean star = (a & 1) != 0;
            boolean starStar = (a & 2) != 0;
            int base = d - (3 + (star ? 1 : 0) + (starStar ? 1 : 0));
            setIp(i);
            frame();
            load(base);
            load(base + 1);
            load(base + 2);
            if (star) {
              load(base + 3);
            } else {
              mv.visitInsn(ACONST_NULL);
            }
            if (starStar) {
              load(base + 3 + (star ? 1 : 0));
            } else {
              mv.visitInsn(ACONST_NULL);
            }
            ops("callEx", "(" + BCFRAME + OBJ + OBJ + OBJ + OBJ + OBJ + ")" + OBJ);
            store(base);
            return;
          }
        case RETURN:
          if (segment) {
            frame();
            load(top);
            mv.visitFieldInsn(PUTFIELD, FRAME, "retval", OBJ);
            iconst(-1);
            mv.visitInsn(IRETURN);
          } else {
            load(top);
            mv.visitInsn(ARETURN);
          }
          return;

        case JUMP:
          jump(GOTO, a, labels);
          return;
        case JUMP_IF_TRUE:
        case POP_JUMP_IF_TRUE:
          truth(top);
          jump(IFNE, a, labels);
          return;
        case JUMP_IF_FALSE:
        case POP_JUMP_IF_FALSE:
          truth(top);
          jump(IFEQ, a, labels);
          return;

        case GET_ITER:
          setIp(i);
          frame();
          load(top);
          ops("getIter", "(" + BCFRAME + OBJ + ")" + OBJ);
          store(top);
          return;
        case FOR_ITER:
          setIp(i);
          frame();
          load(top);
          ops("hasNext", "(" + BCFRAME + OBJ + ")Z");
          jump(IFEQ, a, labels);
          load(top);
          ops("next", "(" + OBJ + ")" + OBJ);
          store(d);
          return;
        case END_FOR:
          frame();
          load(top);
          ops("endFor", "(" + BCFRAME + OBJ + ")V");
          return;
        case LIST_APPEND:
          setIp(i);
          load(d - a);
          load(top);
          ops("listAppend", "(" + OBJ + OBJ + ")V");
          return;
        case DICT_ADD:
          setIp(i);
          load(d - a);
          load(top - 1);
          load(top);
          ops("dictAdd", "(" + OBJ + OBJ + OBJ + ")V");
          return;

        case POST_ASSIGN:
          setIp(i);
          frame();
          constantString(a);
          load(top);
          ops("postAssign", "(" + BCFRAME + STR + OBJ + ")V");
          return;
        case TYPE_ALIAS:
          setIp(i);
          frame();
          constant(a);
          ops("typeAlias", "(" + BCFRAME + OBJ + ")V");
          return;
        case MAKE_FUNCTION:
          {
            int nd = in.getOperand2();
            setIp(i);
            frame();
            constant(a);
            array(d - nd, nd);
            ops("makeFunction", "(" + BCFRAME + OBJ + OBJS + ")" + OBJ);
            store(d - nd);
            return;
          }
        case LOAD_MODULE:
          setIp(i);
          frame();
          constantString(a);
          ops("loadModule", "(" + BCFRAME + STR + ")" + OBJ);
          store(d);
          return;

        default:
          throw new UnsupportedChunkException("unsupported opcode " + op);
      }
    }
  }
}
