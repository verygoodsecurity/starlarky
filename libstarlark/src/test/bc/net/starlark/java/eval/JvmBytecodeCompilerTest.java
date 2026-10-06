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

import static com.google.common.truth.Truth.assertThat;

import net.starlark.java.eval.compiler.BytecodeChunk;
import net.starlark.java.eval.compiler.BytecodeCompiler;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.Program;
import net.starlark.java.syntax.StarlarkFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of compiling bytecode chunks to JVM methods, including chunks over the 64KB limit. */
@RunWith(JUnit4.class)
public final class JvmBytecodeCompilerTest {

  private static final FileOptions OPTIONS =
      FileOptions.DEFAULT.toBuilder().allowToplevelRebinding(true).build();

  private Module module;
  private int savedThreshold;

  @org.junit.Before
  public void compileEverything() {
    savedThreshold = JvmBytecodeCompiler.threshold;
    JvmBytecodeCompiler.threshold = 0; // exercise compiled code, not the interpreter
  }

  @org.junit.After
  public void restoreThreshold() {
    JvmBytecodeCompiler.threshold = savedThreshold;
  }

  @Test
  public void compilesAChunkOnceItIsHot() throws Exception {
    JvmBytecodeCompiler.threshold = 3;
    BytecodeChunk chunk = compile("r = 1\n");
    for (int i = 0; i < 3; i++) {
      assertThat(JvmBytecodeCompiler.hotCodeFor(chunk)).isNull(); // interpreted
    }
    JvmCode code = JvmBytecodeCompiler.hotCodeFor(chunk);
    assertThat(code).isNotNull();
    assertThat(JvmBytecodeCompiler.hotCodeFor(chunk)).isSameInstanceAs(code);
  }

  /** Compiles {@code source} to bytecode and runs it on JVM-compiled code; returns global "r". */
  private Object run(String source) throws Exception {
    module = Module.create();
    StarlarkFile file = StarlarkFile.parse(ParserInput.fromString(source, "t.star"), OPTIONS);
    Program program = Program.compileFile(file, module, /* enableBytecode= */ true);
    BytecodeChunk chunk = program.getBytecode();
    assertThat(chunk).isNotNull();
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      java.util.HashMap<String, Object> builtins = new java.util.HashMap<>(Starlark.UNIVERSE);
      JvmBytecodeCompiler.runToplevel(
          chunk, thread, new BytecodeGlobals(module, builtins), "t.star");
    }
    return module.getGlobal("r");
  }

  private static BytecodeChunk compile(String source) throws Exception {
    StarlarkFile file = StarlarkFile.parse(ParserInput.fromString(source, "t.star"), OPTIONS);
    return BytecodeCompiler.compileProgram(
        Program.compileFile(file, Module.create(), false).getResolvedFunction(), true);
  }

  private BytecodeChunk functionChunk(String name) {
    return ((BytecodeFunction) module.getGlobal(name)).getChunk();
  }

  @Test
  public void largeToplevelIsSplitIntoSegments() throws Exception {
    StringBuilder src = new StringBuilder("r = 0\n");
    for (int i = 0; i < 20_000; i++) {
      src.append("r = r + ").append(i % 7).append("\n");
    }
    BytecodeChunk chunk = compile(src.toString());
    assertThat(JvmBytecodeCompiler.codeFor(chunk)).isNotNull();
    int expected = 0;
    for (int i = 0; i < 20_000; i++) {
      expected += i % 7;
    }
    assertThat(run(src.toString())).isEqualTo(StarlarkInt.of(expected));
  }

  @Test
  public void jumpsBetweenSegments() throws Exception {
    // if/else chains in a large function: forward jumps whose targets land in other segments.
    StringBuilder src = new StringBuilder("def f():\n  r = []\n");
    for (int i = 0; i < 4_000; i++) {
      src.append("  if ").append(i).append(" % 3 == 0:\n    r.append(").append(i)
          .append(")\n  else:\n    r.append(-1)\n");
    }
    src.append("  return r\nr = f()\n");
    Object r = run(src.toString());
    assertThat(JvmBytecodeCompiler.codeFor(functionChunk("f"))).isNotNull(); // compiled, split
    assertThat(((StarlarkList<?>) r).size()).isEqualTo(4_000);
    assertThat(((StarlarkList<?>) r).get(3)).isEqualTo(StarlarkInt.of(3));
    assertThat(((StarlarkList<?>) r).get(4)).isEqualTo(StarlarkInt.of(-1));
  }

  @Test
  public void largeFunctionBodyIsSplitIntoSegments() throws Exception {
    StringBuilder src = new StringBuilder("def f():\n  x = 0\n");
    for (int i = 0; i < 20_000; i++) {
      src.append("  x = x + 1\n");
    }
    src.append("  return x\nr = f()\n");
    assertThat(run(src.toString())).isEqualTo(StarlarkInt.of(20_000));
    assertThat(JvmBytecodeCompiler.codeFor(functionChunk("f"))).isNotNull(); // compiled, split
  }

  @Test
  public void costlyChunkFallsBackToTheInterpreter() throws Exception {
    // Generating a class for a function with a list literal this long would take gigabytes
    // (each pending element is a JVM local, and ASM records every local at every instruction).
    StringBuilder src = new StringBuilder("def f():\n  return [");
    for (int i = 0; i < 20_000; i++) {
      src.append("(").append(i).append(", \"s\"), ");
    }
    src.append("]\nr = len(f())\n");
    assertThat(run(src.toString())).isEqualTo(StarlarkInt.of(20_000));
    assertThat(JvmBytecodeCompiler.codegenCost(functionChunk("f")))
        .isGreaterThan(JvmBytecodeCompiler.MAX_CODEGEN_COST);
    assertThat(JvmBytecodeCompiler.codeFor(functionChunk("f"))).isNull(); // interpreted
  }

  @Test
  public void oversizedLoopBodyFallsBackToTheInterpreter() throws Exception {
    // A loop keeps its iterator on the stack, so its body cannot be split.
    StringBuilder src = new StringBuilder("def f():\n  x = 0\n  for i in range(2):\n");
    for (int i = 0; i < 20_000; i++) {
      src.append("    x = x + 1\n");
    }
    src.append("  return x\nr = f()\n");
    assertThat(run(src.toString())).isEqualTo(StarlarkInt.of(40_000));
    assertThat(JvmBytecodeCompiler.codeFor(functionChunk("f"))).isNull(); // interpreted
  }
}
