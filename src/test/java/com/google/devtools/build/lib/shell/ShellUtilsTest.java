// Copyright 2015 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.shell;

import static com.google.common.truth.Truth.assertThat;
import static com.google.devtools.build.lib.shell.ShellUtils.prettyPrintArgv;
import static com.google.devtools.build.lib.shell.ShellUtils.shellEscape;
import static com.google.devtools.build.lib.shell.ShellUtils.tokenize;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for ShellUtils. */
@RunWith(JUnit4.class)
public class ShellUtilsTest {

  @Test
  public void testShellEscape() throws Exception {
    assertThat(shellEscape("")).isEqualTo("''");
    assertThat(shellEscape("foo")).isEqualTo("foo");
    assertThat(shellEscape("foo bar")).isEqualTo("'foo bar'");
    assertThat(shellEscape("'foo'")).isEqualTo("''\\''foo'\\'''");
    assertThat(shellEscape("\\'foo\\'")).isEqualTo("'\\'\\''foo\\'\\'''");
    assertThat(shellEscape("${filename%.c}.o")).isEqualTo("'${filename%.c}.o'");
    assertThat(shellEscape("<html!>")).isEqualTo("'<html!>'");
  }

  @Test
  public void testPrettyPrintArgv() throws Exception {
    assertThat(prettyPrintArgv(Arrays.asList("echo", "$US", "100"))).isEqualTo("echo '$US' 100");
  }

  private void assertTokenize(String copts, String... expectedTokens)
      throws Exception {
    List<String> actualTokens = new ArrayList<>();
    tokenize(actualTokens, copts);
    assertThat(actualTokens).isEqualTo(Arrays.asList(expectedTokens));
  }

  @Test
  public void testTokenize() throws Exception {
    assertTokenize("-DASMV", "-DASMV");
    assertTokenize("-DNO_UNDERLINE", "-DNO_UNDERLINE");
    assertTokenize("-DASMV -DNO_UNDERLINE",
                   "-DASMV", "-DNO_UNDERLINE");
    assertTokenize("-DDES_LONG=\"unsigned int\" -wd310",
                   "-DDES_LONG=unsigned int", "-wd310");
    assertTokenize("-Wno-write-strings -Wno-pointer-sign "
                   + "-Wno-unused-variable -Wno-pointer-to-int-cast",
                   "-Wno-write-strings",
                   "-Wno-pointer-sign",
                   "-Wno-unused-variable",
                   "-Wno-pointer-to-int-cast");
  }

  @Test
  public void testTokenizeOnNestedQuotation() throws Exception {
    assertTokenize("-Dfoo='foo\"bar' -Dwiz",
                   "-Dfoo=foo\"bar",
                   "-Dwiz");
    assertTokenize("-Dfoo=\"foo'bar\" -Dwiz",
                   "-Dfoo=foo'bar",
                   "-Dwiz");
  }

  @Test
  public void testTokenizeOnBackslashEscapes() throws Exception {
    // This would be easier to grok if we forked+exec'd a shell.

    assertTokenize("-Dfoo=\\'foo -Dbar", // \' not quoted -> '
                   "-Dfoo='foo",
                   "-Dbar");
    assertTokenize("-Dfoo=\\\"foo -Dbar", // \" not quoted -> "
                   "-Dfoo=\"foo",
                   "-Dbar");
    assertTokenize("-Dfoo=\\\\foo -Dbar", // \\ not quoted -> \
                   "-Dfoo=\\foo",
                   "-Dbar");

    assertTokenize("-Dfoo='\\'foo -Dbar", // \' single quoted -> \, close quote
                   "-Dfoo=\\foo",
                   "-Dbar");
    assertTokenize("-Dfoo='\\\"foo' -Dbar", // \" single quoted -> \"
                   "-Dfoo=\\\"foo",
                   "-Dbar");
    assertTokenize("-Dfoo='\\\\foo' -Dbar", // \\ single quoted -> \\
                   "-Dfoo=\\\\foo",
                   "-Dbar");

    assertTokenize("-Dfoo=\"\\'foo\" -Dbar", // \' double quoted -> \'
                   "-Dfoo=\\'foo",
                   "-Dbar");
    assertTokenize("-Dfoo=\"\\\"foo\" -Dbar", // \" double quoted -> "
                   "-Dfoo=\"foo",
                   "-Dbar");
    assertTokenize("-Dfoo=\"\\\\foo\" -Dbar", // \\ double quoted -> \
                   "-Dfoo=\\foo",
                   "-Dbar");
  }

  private void assertTokenizeFails(String copts, String expectedError) {
    ShellUtils.TokenizationException e =
        assertThrows(
            ShellUtils.TokenizationException.class, () -> tokenize(new ArrayList<String>(), copts));
    assertThat(e).hasMessageThat().isEqualTo(expectedError);
  }

  @Test
  public void testTokenizeEmptyString() throws Exception {
    assertTokenize("");
  }

  @Test
  public void testTokenizeFailsOnUnterminatedQuotation() {
    assertTokenizeFails("-Dfoo=\"bar", "unterminated quotation");
    assertTokenizeFails("-Dfoo='bar", "unterminated quotation");
    assertTokenizeFails("-Dfoo=\"b'ar", "unterminated quotation");
  }

  private void assertWindowsEscapeArg(String arg, String expected) {
    assertThat(ShellUtils.windowsEscapeArg(arg)).isEqualTo(expected);
  }

  @Test
  public void testEscapeCreateProcessArg() {
    assertWindowsEscapeArg("", "\"\"");
    assertWindowsEscapeArg(" ", "\" \"");
    assertWindowsEscapeArg("\"", "\"\\\"\"");
    assertWindowsEscapeArg("\"\\", "\"\\\"\\\\\"");
    assertWindowsEscapeArg("\\", "\\");
    assertWindowsEscapeArg("\\\"", "\"\\\\\\\"\"");
    assertWindowsEscapeArg("with space", "\"with space\"");
    assertWindowsEscapeArg("with^caret", "with^caret");
    assertWindowsEscapeArg("space ^caret", "\"space ^caret\"");
    assertWindowsEscapeArg("caret^ space", "\"caret^ space\"");
    assertWindowsEscapeArg("with\"quote", "\"with\\\"quote\"");
    assertWindowsEscapeArg("with\\backslash", "with\\backslash");
    assertWindowsEscapeArg("one\\ backslash and \\space", "\"one\\ backslash and \\space\"");
    assertWindowsEscapeArg("two\\\\backslashes", "two\\\\backslashes");
    assertWindowsEscapeArg(
        "two\\\\ backslashes \\\\and space", "\"two\\\\ backslashes \\\\and space\"");
    assertWindowsEscapeArg("one\\\"x", "\"one\\\\\\\"x\"");
    assertWindowsEscapeArg("two\\\\\"x", "\"two\\\\\\\\\\\"x\"");
    assertWindowsEscapeArg("a \\ b", "\"a \\ b\"");
    assertWindowsEscapeArg("a \\\" b", "\"a \\\\\\\" b\"");
    assertWindowsEscapeArg("A", "A");
    assertWindowsEscapeArg("\"a\"", "\"\\\"a\\\"\"");
    assertWindowsEscapeArg("B C", "\"B C\"");
    assertWindowsEscapeArg("\"b c\"", "\"\\\"b c\\\"\"");
    assertWindowsEscapeArg("D\"E", "\"D\\\"E\"");
    assertWindowsEscapeArg("\"d\"e\"", "\"\\\"d\\\"e\\\"\"");
    assertWindowsEscapeArg("C:\\F G", "\"C:\\F G\"");
    assertWindowsEscapeArg("\"C:\\f g\"", "\"\\\"C:\\f g\\\"\"");
    assertWindowsEscapeArg("C:\\H\"I", "\"C:\\H\\\"I\"");
    assertWindowsEscapeArg("\"C:\\h\"i\"", "\"\\\"C:\\h\\\"i\\\"\"");
    assertWindowsEscapeArg("C:\\J\\\"K", "\"C:\\J\\\\\\\"K\"");
    assertWindowsEscapeArg("\"C:\\j\\\"k\"", "\"\\\"C:\\j\\\\\\\"k\\\"\"");
    assertWindowsEscapeArg("C:\\L M ", "\"C:\\L M \"");
    assertWindowsEscapeArg("\"C:\\l m \"", "\"\\\"C:\\l m \\\"\"");
    assertWindowsEscapeArg("C:\\N O\\", "\"C:\\N O\\\\\"");
    assertWindowsEscapeArg("\"C:\\n o\\\"", "\"\\\"C:\\n o\\\\\\\"\"");
    assertWindowsEscapeArg("C:\\P Q\\ ", "\"C:\\P Q\\ \"");
    assertWindowsEscapeArg("\"C:\\p q\\ \"", "\"\\\"C:\\p q\\ \\\"\"");
    assertWindowsEscapeArg("C:\\R\\S\\", "C:\\R\\S\\");
    assertWindowsEscapeArg("C:\\R x\\S\\", "\"C:\\R x\\S\\\\\"");
    assertWindowsEscapeArg("\"C:\\r\\s\\\"", "\"\\\"C:\\r\\s\\\\\\\"\"");
    assertWindowsEscapeArg("\"C:\\r x\\s\\\"", "\"\\\"C:\\r x\\s\\\\\\\"\"");
    assertWindowsEscapeArg("C:\\T U\\W\\", "\"C:\\T U\\W\\\\\"");
    assertWindowsEscapeArg("\"C:\\t u\\w\\\"", "\"\\\"C:\\t u\\w\\\\\\\"\"");
    assertWindowsEscapeArg("\"a", "\"\\\"a\"");
  }
}
