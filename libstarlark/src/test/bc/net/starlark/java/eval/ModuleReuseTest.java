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

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests of executing several files in one module, as a REPL does. Runs on whichever execution
 * path the build selects (-Dstarlark.bytecode, -Dstarlark.bytecode.vm).
 */
@RunWith(JUnit4.class)
public final class ModuleReuseTest {

  private static final FileOptions REBINDING =
      FileOptions.DEFAULT.toBuilder().allowToplevelRebinding(true).build();

  private static void exec(Module module, StarlarkThread thread, String... lines)
      throws Exception {
    Starlark.execFile(ParserInput.fromLines(lines), REBINDING, module, thread);
  }

  @Test
  public void functionSeesLaterRebindingOfGlobal() throws Exception {
    Module module = Module.create();
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      exec(module, thread, "x = 1", "def f():", "  return x");
      exec(module, thread, "x = 2");
      exec(module, thread, "y = f()");
    }
    assertThat(module.getGlobal("y")).isEqualTo(StarlarkInt.of(2));
  }

  @Test
  public void innermostEnclosingModuleIsFoundAtToplevelAndInFunctions() throws Exception {
    List<Module> seen = new ArrayList<>();
    StarlarkCallable record =
        new StarlarkCallable() {
          @Override
          public String getName() {
            return "record";
          }

          @Override
          public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs) {
            seen.add(Module.ofInnermostEnclosingStarlarkFunction(thread));
            return Starlark.NONE;
          }
        };
    Module module =
        Module.withPredeclared(StarlarkSemantics.DEFAULT, ImmutableMap.of("record", record));
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, StarlarkSemantics.DEFAULT);
      exec(module, thread, "record()", "def f():", "  record()", "f()");
    }
    assertThat(seen).containsExactly(module, module);
  }
}
