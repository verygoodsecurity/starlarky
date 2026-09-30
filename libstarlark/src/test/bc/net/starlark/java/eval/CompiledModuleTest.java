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
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.Program;
import net.starlark.java.syntax.StarlarkFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests of serializing compiled modules and running them without the source. */
@RunWith(JUnit4.class)
public final class CompiledModuleTest {

  private static final String SOURCE =
      String.join(
          "\n",
          "\"\"\"Module doc.\"\"\"",
          "big = 123456789012345678901234567890 * 2",
          "f = 1.5e3",
          "b = b'\\x00\\xffab'",
          "def adder(k):",
          "    total = [0]",
          "    def add(x, *, scale = 1, **kw):",
          "        total[0] += x * k * scale + len(kw)",
          "        return total[0]",
          "    return add",
          "a = adder(3)",
          "_ = a(1)",
          "r1 = a(2, scale = 10, extra = True)",
          "r2 = sorted({str(i): [j for j in range(i) if j % 2] for i in range(5)}.items())",
          "r3 = (lambda x, y = 2: x * x + y if hasattr(x, 'bit_length') else None)(4)",
          "r4 = [x + y for x, y in zip('abc'.elems(), 'xyz'.elems())]",
          "r5 = pre + 1",
          "");

  private static final ImmutableMap<String, Object> ENV = ImmutableMap.of("pre", StarlarkInt.of(41));

  private static Module newModule() {
    return Module.withPredeclared(StarlarkSemantics.DEFAULT, ENV);
  }

  private static CompiledModule compile(Module module) throws Exception {
    StarlarkFile file =
        StarlarkFile.parse(ParserInput.fromString(SOURCE, "mod.star"), FileOptions.DEFAULT);
    Program program = Program.compileFile(file, module, /* enableBytecode= */ true);
    return CompiledModule.of(program, file, new byte[] {1, 2, 3});
  }

  private static Module runTreeWalker() throws Exception {
    Module module = newModule();
    try (Mutability mu = Mutability.create("test")) {
      StarlarkFile file =
          StarlarkFile.parse(ParserInput.fromString(SOURCE, "mod.star"), FileOptions.DEFAULT);
      Program program = Program.compileFile(file, module, /* enableBytecode= */ false);
      Starlark.execFileProgram(program, module, StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT));
    }
    return module;
  }

  @Test
  public void roundTripRunsLikeTheSource() throws Exception {
    byte[] bytes = compile(newModule()).toBytes();
    CompiledModule loaded = CompiledModule.read(new ByteArrayInputStream(bytes));

    Module module = newModule();
    assertThat(loaded.resolvesTheSameIn(module)).isTrue();
    try (Mutability mu = Mutability.create("test")) {
      loaded.exec(module, StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT));
    }
    Module expected = runTreeWalker();
    for (String name : new String[] {"big", "f", "b", "r1", "r2", "r3", "r4", "r5"}) {
      assertThat(Starlark.repr(module.getGlobal(name), StarlarkSemantics.DEFAULT))
          .isEqualTo(Starlark.repr(expected.getGlobal(name), StarlarkSemantics.DEFAULT));
    }
    assertThat(module.getDocumentation()).isEqualTo("Module doc.");
    assertThat(loaded.getFilename()).isEqualTo("mod.star");
    assertThat(loaded.getSourceDigest()).isEqualTo(new byte[] {1, 2, 3});
  }

  @Test
  public void serializationIsDeterministic() throws Exception {
    byte[] bytes = compile(newModule()).toBytes();
    byte[] again = CompiledModule.read(new ByteArrayInputStream(bytes)).toBytes();
    assertThat(again).isEqualTo(bytes);
  }

  @Test
  public void rejectsDataFromAnotherCompilerVersion() throws Exception {
    byte[] bytes = compile(newModule()).toBytes();
    bytes[11] ^= 1; // BytecodeCompiler.VERSION (third int of the header)
    IOException e =
        assertThrows(IOException.class, () -> CompiledModule.read(new ByteArrayInputStream(bytes)));
    assertThat(e).hasMessageThat().contains("incompatible build");
  }

  @Test
  public void rejectsGarbage() {
    assertThrows(
        IOException.class,
        () -> CompiledModule.read(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6, 7, 8})));
  }

  @Test
  public void checksTheResolutionEnvironment() throws Exception {
    CompiledModule compiled = compile(newModule());
    // `pre` is no longer predeclared.
    assertThat(compiled.resolvesTheSameIn(Module.create())).isFalse();
    // `len` (resolved as universal) is now shadowed by a predeclared value.
    assertThat(
            compiled.resolvesTheSameIn(
                Module.withPredeclared(
                    StarlarkSemantics.DEFAULT,
                    ImmutableMap.of("pre", StarlarkInt.of(1), "len", StarlarkInt.of(0)))))
        .isFalse();
    // Unrelated extra names don't matter.
    assertThat(
            compiled.resolvesTheSameIn(
                Module.withPredeclared(
                    StarlarkSemantics.DEFAULT,
                    ImmutableMap.of("pre", StarlarkInt.of(1), "other", StarlarkInt.of(0)))))
        .isTrue();
  }
}
