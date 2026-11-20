// Copyright 2026 Very Good Security Authors. All rights reserved.
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

import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.Program;
import net.starlark.java.syntax.StarlarkFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests that a BytecodeFunction presents itself as the equivalent StarlarkFunction does. */
@RunWith(JUnit4.class)
public final class BytecodeFunctionTest {

  private static final String SRC =
      String.join(
          "\n",
          "def f(a, b=1, *args, c, d=2, **kwargs):",
          "  pass",
          "def g():",
          "  pass",
          "h = lambda x, y: x",
          "");

  private static Object define(String name, boolean bytecode) throws Exception {
    Module module = Module.create();
    Program prog =
        Program.compileFile(StarlarkFile.parse(ParserInput.fromString(SRC, "t.star")), module, bytecode);
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      Starlark.execFileProgram(prog, module, thread);
    }
    return module.getGlobal(name);
  }

  @Test
  public void toStringMatchesStarlarkFunction() throws Exception {
    for (String name : new String[] {"f", "g", "h"}) {
      Object tree = define(name, false);
      Object bytecode = define(name, true);
      assertThat(tree).isInstanceOf(StarlarkFunction.class);
      assertThat(bytecode).isInstanceOf(BytecodeFunction.class);
      assertThat(bytecode.toString()).isEqualTo(tree.toString());
    }
  }

  @Test
  public void reprMatchesStarlarkFunction() throws Exception {
    assertThat(Starlark.repr(define("f", true), StarlarkSemantics.DEFAULT))
        .isEqualTo(Starlark.repr(define("f", false), StarlarkSemantics.DEFAULT));
  }
}
