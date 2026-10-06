/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.verygood.security.run;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.parser.PrependMergedStarFile;
import com.verygood.security.larky.parser.StarFile;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import picocli.CommandLine;

@RunWith(JUnit4.class)
public final class LarkyEntrypointTest {

  // A wasm header plus bytes that are not valid UTF-8, to check modules stay binary.
  private static final byte[] WASM = {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, (byte) 0xff};

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void readModules_readsEachFileUnderItsName_inOrder() throws Exception {
    File wasm = tmp.newFile("echo.wasm");
    Files.write(wasm.toPath(), WASM);
    File star = tmp.newFile("helper.star");
    Files.writeString(star.toPath(), "x = 1\n");

    ImmutableMap<String, byte[]> modules =
        LarkyEntrypoint.readModules(
            List.of("echo.wasm=" + wasm, "lib/helper.star=" + star), "main.star");

    assertThat(modules.keySet()).containsExactly("echo.wasm", "lib/helper.star").inOrder();
    assertThat(modules.get("echo.wasm")).isEqualTo(WASM);
    assertThat(new String(modules.get("lib/helper.star"), UTF_8)).isEqualTo("x = 1\n");
  }

  @Test
  public void readModules_splitsAtTheFirstEquals() throws Exception {
    File f = tmp.newFile("a=b.wasm");
    Files.write(f.toPath(), WASM);

    assertThat(LarkyEntrypoint.readModules(List.of("m.wasm=" + f), "main.star").get("m.wasm"))
        .isEqualTo(WASM);
  }

  @Test
  public void readModules_rejectsBadSyntax() {
    for (String arg : List.of("echo.wasm", "=x.wasm", "echo.wasm=", "")) {
      IllegalArgumentException e =
          assertThrows(
              IllegalArgumentException.class,
              () -> LarkyEntrypoint.readModules(List.of(arg), "main.star"));
      assertThat(e).hasMessageThat().contains("--module expects NAME=PATH");
    }
  }

  @Test
  public void readModules_rejectsUnreadableFile() {
    String missing = new File(tmp.getRoot(), "missing.wasm").getPath();

    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> LarkyEntrypoint.readModules(List.of("m.wasm=" + missing), "main.star"));
    assertThat(e).hasMessageThat().contains("cannot read '" + missing + "'");
  }

  @Test
  public void readModules_rejectsDirectory() {
    String dir = tmp.getRoot().getPath();

    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> LarkyEntrypoint.readModules(List.of("m.wasm=" + dir), "main.star"));
    assertThat(e).hasMessageThat().contains("cannot read");
  }

  @Test
  public void readModules_rejectsRepeatedNameAndScriptName() throws Exception {
    File f = tmp.newFile("m.wasm");

    IllegalArgumentException repeated =
        assertThrows(
            IllegalArgumentException.class,
            () -> LarkyEntrypoint.readModules(List.of("m.wasm=" + f, "m.wasm=" + f), "main.star"));
    assertThat(repeated).hasMessageThat().contains("more than once");

    IllegalArgumentException script =
        assertThrows(
            IllegalArgumentException.class,
            () -> LarkyEntrypoint.readModules(List.of("main.star=" + f), "main.star"));
    assertThat(script).hasMessageThat().contains("script's own name");
  }

  @Test
  public void scriptName_isTheScriptsFileName() {
    assertThat(LarkyEntrypoint.scriptName("/a/b/main.star")).isEqualTo("main.star");
    assertThat(LarkyEntrypoint.scriptName(null)).isEqualTo(LarkyEntrypoint.DEFAULT_SCRIPT_NAME);
    assertThat(LarkyEntrypoint.scriptName("")).isEqualTo(LarkyEntrypoint.DEFAULT_SCRIPT_NAME);
  }

  @Test
  public void rootStarFile_withoutModules_isTheMergedScript() throws Exception {
    PrependMergedStarFile merged = new PrependMergedStarFile("x = 1\n");

    assertThat(LarkyEntrypoint.rootStarFile(merged, "main.star", ImmutableMap.of()))
        .isSameInstanceAs(merged);
  }

  @Test
  public void rootStarFile_withModules_holdsScriptAndModuleBytes() throws Exception {
    PrependMergedStarFile merged = new PrependMergedStarFile("x = 1\n");

    StarFile root =
        LarkyEntrypoint.rootStarFile(merged, "main.star", ImmutableMap.of("echo.wasm", WASM));

    assertThat(root.path()).isEqualTo("main.star");
    assertThat(root.readContentBytes()).isEqualTo(merged.readContentBytes());
    assertThat(root.resolve("echo.wasm").readContentBytes()).isEqualTo(WASM);
  }

  @Test
  public void run_scriptLoadsAModuleGivenOnTheCommandLine() throws Exception {
    File helper = tmp.newFile("helper_src.star");
    Files.writeString(helper.toPath(), "def greet(who):\n    return 'hello ' + who\n");
    File script = tmp.newFile("main.star");
    Files.writeString(
        script.toPath(),
        // The runner writes the value of the script's last expression.
        "load('helper', 'greet')\n" + "greet('wasm')\n");
    File out = new File(tmp.getRoot(), "out.txt");
    File log = new File(tmp.getRoot(), "log.txt");

    int exit =
        new CommandLine(new LarkyEntrypoint())
            .execute(
                "-s", script.getPath(),
                "-o", out.getPath(),
                "-l", log.getPath(),
                "--module", "helper.star=" + helper.getPath());

    assertThat(exit).isEqualTo(0);
    assertThat(Files.readString(out.toPath())).contains("hello wasm");
  }

  @Test
  public void run_badModuleArgument_failsWithUsageError() throws Exception {
    File script = tmp.newFile("main.star");
    Files.writeString(script.toPath(), "output = 1\n");
    StringWriter err = new StringWriter();

    CommandLine cli = new CommandLine(new LarkyEntrypoint());
    cli.setErr(new PrintWriter(err));
    int exit =
        cli.execute(
            "-s", script.getPath(),
            "-o", new File(tmp.getRoot(), "out.txt").getPath(),
            "-l", new File(tmp.getRoot(), "log.txt").getPath(),
            "--module", "echo.wasm=" + new File(tmp.getRoot(), "nope.wasm").getPath());

    assertThat(exit).isEqualTo(CommandLine.ExitCode.USAGE);
    assertThat(err.toString()).contains("cannot read");
  }
}
