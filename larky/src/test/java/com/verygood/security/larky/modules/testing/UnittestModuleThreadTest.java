/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.verygood.security.larky.modules.testing;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.testing.TestingConsole;
import com.verygood.security.larky.parser.InMemMapBackedStarFile;
import com.verygood.security.larky.parser.LarkyScript;
import java.util.Map;
import net.starlark.java.eval.EvalException;
import org.junit.jupiter.api.Test;

/**
 * A test case runs on the evaluation that runs the suite, not the one that built it, so a failing
 * test's traceback goes back to the code that called {@code TextTestRunner().run(...)}.
 */
public class UnittestModuleThreadTest {

  private static String failureTraceback(String script) {
    Throwable thrown =
        assertThrows(
            Throwable.class,
            () ->
                new LarkyScript(
                        ModuleSupplier.CORE_MODULES,
                        LarkyScript.StarlarkMode.STRICT,
                        Map.of(),
                        new ModuleSupplier().modulesToVariableMap(true),
                        com.verygood.security.larky.LarkySemantics.LARKY_SEMANTICS)
                    .evaluate(
                        InMemMapBackedStarFile.createStarFile("main.star", script),
                        new TestingConsole()));
    for (Throwable c = thrown; c != null; c = c.getCause()) {
      if (c instanceof EvalException e) {
        return e.getMessageWithStack();
      }
    }
    throw new AssertionError("no EvalException in " + thrown, thrown);
  }

  @Test
  public void aSuiteBuiltInALoadedModuleRunsOnTheCallersThread() {
    String traceback =
        failureTraceback(
            String.join(
                "\n",
                "load(\"@stdlib//unittest\", \"unittest\")",
                "load(\"@vendor//_unittest_fixtures/suite_built_at_load\", \"SUITE\")",
                "def main():",
                "    unittest.TextTestRunner().run(SUITE)",
                "main()",
                ""));

    assertThat(traceback).contains("File \"main.star\", line 5, column 5, in <toplevel>");
    assertThat(traceback).contains("File \"main.star\", line 4, column 34, in main");
    assertThat(traceback).contains("in test_fails");
    assertThat(traceback).contains("in _inner");
    assertThat(traceback).contains("Error: boom");
  }

  @Test
  public void aSuiteBuiltInTheSameFileKeepsItsFullTraceback() {
    String traceback =
        failureTraceback(
            String.join(
                "\n",
                "load(\"@stdlib//unittest\", \"unittest\")",
                "def _inner():",
                "    fail(\"boom\")",
                "def test_fails():",
                "    _inner()",
                "def _suite():",
                "    s = unittest.TestSuite()",
                "    s.addTest(unittest.FunctionTestCase(test_fails))",
                "    return s",
                "def main():",
                "    unittest.TextTestRunner().run(_suite())",
                "main()",
                ""));

    assertThat(traceback).contains("File \"main.star\", line 12, column 5, in <toplevel>");
    assertThat(traceback).contains("File \"main.star\", line 11, column 34, in main");
    assertThat(traceback).contains("File \"main.star\", line 5, column 11, in test_fails");
    assertThat(traceback).contains("File \"main.star\", line 3, column 9, in _inner");
  }
}
