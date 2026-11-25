package com.verygood.security.run;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import picocli.CommandLine;

/** {@code larky-runner --steps} reports the steps a host reads, so a script can be measured locally. */
public class LarkyEntrypointStepsTest {

  @Rule public TemporaryFolder tmp = new TemporaryFolder();

  private static final Pattern LINE =
      Pattern.compile(
          "larky: outcome=(\\w+) steps=(\\d+) library_init_steps=(\\d+) engine=(\\S+) elapsed_ms=\\d+");

  private static Matcher run(Path dir, String script, String... options) throws Exception {
    Path file = dir.resolve("script.star");
    Files.writeString(file, script);
    String[] args = new String[options.length + 6];
    args[0] = "-s";
    args[1] = file.toString();
    args[2] = "-o";
    args[3] = dir.resolve("out.txt").toString();
    args[4] = "-l";
    args[5] = dir.resolve("log.txt").toString();
    System.arraycopy(options, 0, args, 6, options.length);

    PrintStream err = System.err;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
    try {
      new CommandLine(new LarkyEntrypoint()).execute(args);
    } finally {
      System.setErr(err);
    }
    Matcher m = LINE.matcher(captured.toString(StandardCharsets.UTF_8));
    assertTrue("no steps line in: " + captured, m.find());
    return m;
  }

  @Test
  public void stepsAreReportedAndTheSameOnEveryRun() throws Exception {
    Path dir = tmp.getRoot().toPath();
    String script = "def f():\n    return [i * i for i in range(50)]\noutput = f()\n";

    Matcher first = run(dir, script, "--steps");
    Matcher second = run(dir, script, "--steps");

    assertEquals("OK", first.group(1));
    assertEquals("tree-walker", first.group(4));
    assertNotEquals("0", first.group(2));
    assertEquals(first.group(2), second.group(2));
  }

  @Test
  public void aStepLimitStopsTheRunAndIsReported() throws Exception {
    Path dir = tmp.getRoot().toPath();
    Matcher m =
        run(
            dir,
            "def f():\n    t = 0\n    for i in range(100000):\n        t += i\n    return t\noutput = f()\n",
            "--steps",
            "--step-limit",
            "1000");

    assertEquals("STEP_LIMIT", m.group(1));
  }
}
