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

import net.starlark.java.eval.compiler.BytecodeCompiler;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.Types;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests of programs with a type table. Runs on whichever execution path the build selects
 * (-Dstarlark.bytecode, -Dstarlark.bytecode.vm); with bytecode, typed programs run on the VM.
 */
@RunWith(JUnit4.class)
public final class TypedProgramTest {

  private static final FileOptions TYPED =
      FileOptions.DEFAULT.toBuilder().allowTypeSyntax(true).resolveTypeSyntax(true).build();

  private static final StarlarkSemantics DYNAMIC =
      StarlarkSemantics.builder()
          .setBool(StarlarkSemantics.EXPERIMENTAL_STARLARK_DYNAMIC_TYPE_CHECKING, true)
          .build();

  private static Module exec(String... lines) throws Exception {
    Module module = Module.create();
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, DYNAMIC);
      Starlark.execFile(ParserInput.fromLines(lines), TYPED, module, thread);
    }
    return module;
  }

  private static String error(String... lines) {
    return assertThrows(EvalException.class, () -> exec(lines)).getMessage();
  }

  @Test
  public void typedFunctionsRunOnTheSelectedPath() throws Exception {
    Module module = exec("def f(x: int) -> str:", "  return str(x)", "y = f(1)");
    Object f = module.getGlobal("f");
    assertThat(f)
        .isInstanceOf(
            BytecodeCompiler.enabledByDefault() ? BytecodeFunction.class : StarlarkFunction.class);
    assertThat(module.getGlobal("y")).isEqualTo("1");
    Types.CallableType type =
        (Types.CallableType) Starlark.getStarlarkType(f, StarlarkSemantics.DEFAULT);
    assertThat(type.getParameterTypeByPos(0)).isEqualTo(Types.INT);
    assertThat(type.getReturnType()).isEqualTo(Types.STR);
  }

  @Test
  public void argumentReturnAndDefaultTypesAreChecked() {
    assertThat(error("def f(a: int): pass", "f('abc')"))
        .isEqualTo("in call to f(), parameter 'a' got value of type 'str', want 'int'");
    assertThat(error("def f(a, b: int = 1): pass", "f(1, b = 'x')"))
        .isEqualTo("in call to f(), parameter 'b' got value of type 'str', want 'int'");
    assertThat(error("def f() -> int: return 'abc'", "f()"))
        .isEqualTo("f(): returns value of type 'str', declares 'int'");
    assertThat(error("def f(a: int = 'abc'): pass"))
        .isEqualTo("f(): parameter 'a' has default value of type 'str', declares 'int'");
    assertThat(error("def f(a, *, b: int = 'abc'): pass"))
        .isEqualTo("f(): parameter 'b' has default value of type 'str', declares 'int'");
  }

  @Test
  public void nestedAndLambdaFunctionsAreTyped() {
    assertThat(error("def f():", "  def g(a: int): pass", "  g('x')", "f()"))
        .isEqualTo("in call to g(), parameter 'a' got value of type 'str', want 'int'");
  }

  @Test
  public void typeAliasBindsItsTypeConstructor() throws Exception {
    Module module = exec("type Num = int | float", "def f(x: Num) -> Num:", "  return x", "f(1)");
    assertThat(module.getGlobal("Num")).isInstanceOf(TypeConstructorValue.class);
  }

  @Test
  public void globalsRecordTheirDeclaredTypes() throws Exception {
    Module module = exec("x: int = 1", "y = 2");
    assertThat(module.getExportType("x")).isEqualTo(Types.INT);
  }
}
