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
package net.starlark.java.syntax;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class InteractiveInputTest {

  private static void assertComplete(String text, boolean expected) {
    assertWithMessage("isComplete(%s)", text.replace("\n", "\\n"))
        .that(InteractiveInput.isComplete(text, FileOptions.DEFAULT))
        .isEqualTo(expected);
  }

  @Test
  public void simpleStatementsAreComplete() {
    assertComplete("", true);
    assertComplete("x = 1", true);
    assertComplete("1 + 2", true);
    assertComplete("print('a')", true);
    assertComplete("# just a comment", true);
    assertComplete("load('@stdlib//json', 'json')", true);
  }

  @Test
  public void syntaxErrorsAreCompleteSoTheyAreReported() {
    assertComplete("x = 1 +", true);
    assertComplete("x = )", true);
    assertComplete("'unclosed", true);
    assertComplete("def f():\n", true); // blank line where the body should be
  }

  @Test
  public void openBracketsNeedMore() {
    assertComplete("x = [1,", false);
    assertComplete("f(1,\n  2", false);
    assertComplete("d = {\n", false); // a blank line does not end a bracket
    assertComplete("x = [1,\n 2]", true);
  }

  @Test
  public void tripleQuotedStringsNeedMore() {
    assertComplete("s = \"\"\"abc", false);
    assertComplete("s = '''abc\n", false);
    assertComplete("s = r'''abc", false);
    assertComplete("s = \"\"\"abc\ndef\"\"\"", true);
  }

  @Test
  public void lineContinuationNeedsMore() {
    assertComplete("x = 1 + \\", false);
    assertComplete("x = 1 + \\\n  2", true);
  }

  @Test
  public void blocksEndAtABlankLine() {
    assertComplete("def f():", false);
    assertComplete("def f():\n  return 1", false);
    assertComplete("def f():\n  return 1\n", true);
    assertComplete("if True: x = 1", false);
    assertComplete("if True: x = 1\n", true);
    assertComplete("for x in []:\n  pass\nelse_ = 1", false);
    assertComplete("if x:\n  y = 1\nelse:", false);
    assertComplete("if x:\n  y = 1\nelse:\n  y = 2\n", true);
    assertComplete("def f():\n  x = [\n", false); // the bracket wins over the blank line
  }
}
