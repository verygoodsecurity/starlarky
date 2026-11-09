/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.run.repl;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Drives the REPL with scripted input, without a terminal. */
@RunWith(JUnit4.class)
public final class LarkyReplTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private final StringWriter out = new StringWriter();
  private final StringWriter err = new StringWriter();
  private final PrintWriter outWriter = new PrintWriter(out, true);
  private final PrintWriter errWriter = new PrintWriter(err, true);

  private LarkyRepl repl(String input, ReplSession session) {
    ReplInput in =
        new ReplInput.Plain(new BufferedReader(new StringReader(input)), null, session::isComplete);
    return new LarkyRepl(session, in, outWriter, errWriter, false);
  }

  private int run(String input) {
    return repl(input, new ReplSession(outWriter, errWriter, tmp.getRoot().toPath())).run();
  }

  @Test
  public void multiLineDef() {
    int status =
        run(
            String.join(
                "\n",
                "def f(x):",
                "    y = x * 2",
                "    if y > 10:",
                "        return y",
                "    return 0",
                "",
                "f(21)",
                "f(1)",
                ""));
    assertThat(err.toString()).isEmpty();
    assertThat(out.toString()).isEqualTo("42\n0\n");
    assertThat(status).isEqualTo(0);
  }

  @Test
  public void openBracketsContinueAcrossBlankLines() {
    run("x = [\n  1,\n\n  2,\n]\nx\n");
    assertThat(err.toString()).isEmpty();
    assertThat(out.toString()).isEqualTo("[1, 2]\n");
  }

  @Test
  public void expressionsEchoTheirReprButNotNone() {
    run("1 + 2\nx = 3\nNone\nprint('hi')\nx\n'a'\n[x, None]\n");
    assertThat(err.toString()).isEmpty();
    assertThat(out.toString()).isEqualTo("3\nhi\n3\n\"a\"\n[3, None]\n");
  }

  @Test
  public void bindingsAndMutableValuesPersistAcrossInputs() {
    run("l = []\nl.append(1)\nd = {'k': l}\nd['k'].append(2)\nd\n");
    assertThat(err.toString()).isEmpty();
    assertThat(out.toString()).isEqualTo("{\"k\": [1, 2]}\n");
  }

  @Test
  public void larkyEnvironmentAndLoads() {
    run(
        String.join(
            "\n",
            "load(\"@stdlib//json\", \"json\")",
            "print(json.dumps({\"a\": [1, 2]}))",
            "hex(255)", // a Python builtin Larky adds
            "load(\"@stdlib//larky\", \"larky\")",
            "s = larky.struct(a = 1)",
            "s.a",
            ""));
    assertThat(err.toString()).isEmpty();
    assertThat(out.toString()).isEqualTo("{\"a\":[1,2]}\n\"0xff\"\n1\n");
  }

  @Test
  public void loadErrorsAreReportedWithoutJavaTraces() {
    int status = run("load(\"@stdlib//no_such_module\", \"x\")\n");
    assertThat(status).isEqualTo(1);
    assertThat(err.toString()).contains("no_such_module");
    assertThat(err.toString()).doesNotContain("\tat ");
  }

  @Test
  public void errorTracebacksUseSessionLineNumbersAndSource() {
    int status =
        run(
            String.join(
                "\n",
                "x = 1",
                "def f():",
                "    return 1 // 0",
                "",
                "y = 2",
                "f()",
                ""));
    assertThat(status).isEqualTo(1);
    String e = err.toString();
    assertThat(e).contains("File \"<stdin>\", line 6, column 2, in <toplevel>\n\t\tf()\n");
    assertThat(e).contains("File \"<stdin>\", line 3, column 14, in f\n\t\treturn 1 // 0\n");
    assertThat(e).contains("integer division by zero");
    assertThat(e).doesNotContain("\tat ");
  }

  @Test
  public void syntaxErrorsUseSessionLineNumbers() {
    run("x = 1\ny = 2\nz = )\n");
    assertThat(err.toString()).startsWith("<stdin>:3:5: ");
  }

  @Test
  public void resetForgetsBindings() {
    int status = run("x = 1\nx\n:reset\nx\n");
    assertThat(out.toString()).isEqualTo("1\nSession reset.\n");
    assertThat(err.toString()).contains("name 'x' is not defined");
    assertThat(status).isEqualTo(1);
  }

  @Test
  public void helpQuitAndUnknownCommands() {
    int status = run(":help\n:nope\n:quit\nprint('not reached')\n");
    assertThat(out.toString()).contains(":load <file>");
    assertThat(out.toString()).doesNotContain("not reached");
    assertThat(err.toString()).contains("Unknown command :nope");
    assertThat(status).isEqualTo(1);
  }

  @Test
  public void loadCommandRunsAFileInTheSession() throws Exception {
    Path file = tmp.newFile("lib.star").toPath();
    Files.writeString(file, "def g():\n    return base + 1\n\ndef bad():\n    fail('boom')\n");
    run("base = 41\n:load lib.star\ng()\nbad()\n");
    assertThat(out.toString()).isEqualTo("42\n");
    String e = err.toString();
    assertThat(e).contains("File \"" + file.toAbsolutePath().normalize() + "\", line 5");
    assertThat(e).contains(", line 5, column 9, in bad\n\t\tfail('boom')\n");
    assertThat(e).contains("Error: boom");
  }

  @Test
  public void ctrlCInterruptsEvaluationAndTheReplContinues() throws Exception {
    PipedOutputStream feed = new PipedOutputStream();
    PipedInputStream in = new PipedInputStream(feed);
    ReplSession session = new ReplSession(outWriter, errWriter, tmp.getRoot().toPath());
    LarkyRepl repl =
        new LarkyRepl(
            session,
            new ReplInput.Plain(
                new BufferedReader(new java.io.InputStreamReader(in, UTF_8)), null,
                session::isComplete),
            outWriter,
            errWriter,
            false);
    Thread runner = new Thread(repl::run);
    runner.start();
    // (Top-level for loops are not allowed, as in a script.)
    feed.write(
        "def spin():\n    for i in range(1000000000):\n        pass\n\nspin()\n".getBytes(UTF_8));
    feed.flush();
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
    while (!repl.isEvaluating() && System.nanoTime() < deadline) {
      Thread.sleep(10);
    }
    assertWithMessage("out=%s err=%s", out, err).that(repl.isEvaluating()).isTrue();
    Thread.sleep(100);
    repl.interruptEvaluation();
    feed.write("print('after')\n".getBytes(UTF_8));
    feed.close();
    runner.join(TimeUnit.SECONDS.toMillis(30));
    assertThat(runner.isAlive()).isFalse();
    assertThat(err.toString()).contains("KeyboardInterrupt");
    assertThat(out.toString()).isEqualTo("after\n");
  }

  @Test
  public void completionCandidates() throws Exception {
    ReplSession session = new ReplSession(outWriter, errWriter, tmp.getRoot().toPath());
    assertThat(session.eval("load(\"@stdlib//json\", \"json\")\nfoo_bar = 1\nfoo_baz = 2")).isTrue();

    assertThat(complete(session, "foo_")).containsExactly("foo_bar", "foo_baz").inOrder();
    assertThat(complete(session, "x = pri")).containsExactly("print");
    assertThat(complete(session, "Tr")).containsExactly("True");
    assertThat(complete(session, "hasa")).containsExactly("hasattr"); // Larky's environment
    assertThat(complete(session, "print(json.du")).containsExactly("json.dumps");
    assertThat(complete(session, "json.")).contains("json.loads");
    assertThat(complete(session, "json._")).isEmpty(); // nothing private
    assertThat(complete(session, "nope.")).isEmpty();
    assertThat(complete(session, "'abc'.upp")).isEmpty(); // not a name

    assertThat(complete(session, "load(\"@stdlib//js")).contains("@stdlib//json");
    assertThat(complete(session, "load('@vgs//")).isNotEmpty();
    assertThat(complete(session, "load(\"@stdlib//json\", \"j")).containsExactly("json");

    // The word replaced by a candidate.
    String line = "print(json.du";
    assertThat(ReplCompleter.complete(session, line, line.length()).start()).isEqualTo(6);
    String load = "load(\"@stdlib//js";
    assertThat(ReplCompleter.wordStart(load, load.length())).isEqualTo(6);
  }

  private static java.util.List<String> complete(ReplSession session, String line) {
    return ReplCompleter.complete(session, line, line.length()).candidates();
  }

  @Test
  public void nextIndentFollowsBlocks() {
    assertThat(JLineInput.nextIndent("def f():")).isEqualTo(4);
    assertThat(JLineInput.nextIndent("def f():\n    if x:  # comment")).isEqualTo(8);
    assertThat(JLineInput.nextIndent("def f():\n    x = [1,")).isEqualTo(4);
    assertThat(JLineInput.nextIndent("x = (1,")).isEqualTo(0);
  }

  @Test
  public void jlineEditsAMultiLineInputAsOneBuffer() throws Exception {
    // A terminal over in-memory streams: the line editor without a real terminal.
    byte[] keys = "def f():\rreturn 7\r\rf()\r:quit\r".getBytes(UTF_8);
    ByteArrayOutputStream screen = new ByteArrayOutputStream();
    Terminal terminal =
        TerminalBuilder.builder()
            .system(false)
            .streams(new ByteArrayInputStream(keys), screen)
            .type("xterm")
            .build();
    ReplSession session = new ReplSession(outWriter, errWriter, tmp.getRoot().toPath());
    JLineInput input = new JLineInput(terminal, session, null);
    assertThat(input.read()).isEqualTo("def f():\n    return 7\n    ");
    assertThat(input.read()).isEqualTo("f()");
    assertThat(input.read()).isEqualTo(":quit");
    input.close();
  }
}
