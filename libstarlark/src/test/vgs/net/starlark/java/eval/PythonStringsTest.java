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
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Runs {@code pythonstrings/python_strings.star}, whose expected values were computed by CPython,
 * with the Python string semantics flags set. (ScriptFilesTest runs testdata/ with the default
 * semantics only.)
 */
@RunWith(JUnit4.class)
public final class PythonStringsTest {

  private static final File SCRIPT =
      new File("src/test/vgs/net/starlark/java/eval/pythonstrings/python_strings.star");

  private static StarlarkSemantics pythonSemantics() {
    return StarlarkSemantics.builder()
        .setBool("-python_string_bounds", true)
        .setBool("-python_unicode_strings", true)
        .build();
  }

  private static List<String> run(String source, StarlarkSemantics semantics) throws Exception {
    List<String> errors = new ArrayList<>();
    ImmutableMap.Builder<String, Object> predeclared = ImmutableMap.builder();
    Starlark.addMethods(predeclared, new ScriptTest()); // assert_eq, assert_fails
    Module module = Module.withPredeclared(semantics, predeclared.buildOrThrow());
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, semantics);
      thread.setThreadLocal(
          ScriptTest.Reporter.class,
          (t, message) -> errors.add(t.getCallerLocation() + ": " + message));
      Starlark.execFile(
          ParserInput.fromString(source, SCRIPT.toString()), FileOptions.DEFAULT, module, thread);
    }
    return errors;
  }

  @Test
  public void pythonStringSemantics() throws Exception {
    String source = java.nio.file.Files.readString(SCRIPT.toPath());
    List<String> errors = run(source, pythonSemantics());
    assertWithMessage("%s mismatches:\n%s", errors.size(), String.join("\n", errors))
        .that(errors)
        .isEmpty();
  }

  @Test
  public void pythonBoundsStillCheckArguments() throws Exception {
    String source =
        String.join(
            "\n",
            "assert_fails(lambda: 'abc'.startswith(('a', 1), 5), 'at index 1 of sub, got element of"
                + " type int, want string')",
            "assert_fails(lambda: 'abc'.endswith(1, 5), 'got value of type .int.')",
            "assert_fails(lambda: 'abc'.find('', 1 << 40), 'want value in signed 32-bit range')",
            "");
    assertThat(run(source, pythonSemantics())).isEmpty();
  }

  @Test
  public void defaultSemanticsUnchanged() throws Exception {
    // Without the flags, strings keep Starlark's behaviour (spec and starlark-go agree).
    String source =
        String.join(
            "\n",
            "assert_eq('abcabc'.find('', 7), 6)",
            "assert_eq('abc'.startswith('', 4), True)",
            "assert_eq('abc'.count('', 4), 1)",
            "assert_eq('abc'.index('', 4), 3)",
            "assert_eq('\\u00e9t\\u00e9'.upper(), '\\u00e9T\\u00e9')",
            "assert_eq('\\u00df'.upper(), '\\u00df')",
            "assert_eq('\\u00e9'.isalpha(), False)",
            "assert_eq('\\u00b2'.isdigit(), False)",
            "assert_eq('\\u3000x'.strip(), '\\u3000x')",
            "");
    assertThat(run(source, StarlarkSemantics.DEFAULT)).isEmpty();
  }
}
