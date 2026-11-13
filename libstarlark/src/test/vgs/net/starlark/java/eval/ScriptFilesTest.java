// Copyright 2025 Very Good Security Authors. All rights reserved.
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

import static com.google.common.truth.Truth.assertWithMessage;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Runs each ScriptTest file in testdata/ (upstream's, then VGS's in src/test/vgs and the bytecode VMs' in src/test/bc) as a JUnit test, so that Maven
 * runs them. Failure details are printed to stderr by ScriptTest.
 */
@RunWith(Parameterized.class)
public final class ScriptFilesTest {

  // json.star expects "nesting depth limit exceeded" from 10000 levels of nesting, which
  // relies on a StackOverflowError; a bounded stack makes that independent of the JVM default.
  private static final long STACK_SIZE = 512 * 1024;

  private static final File TESTDATA = new File("src/test/java/net/starlark/java/eval/testdata");
  private static final File VGS_TESTDATA = new File("src/test/vgs/net/starlark/java/eval/testdata");
  private static final File BC_TESTDATA = new File("src/test/bc/net/starlark/java/eval/testdata");

  /**
   * Edits applied to upstream test files before running them, for syntax that VGS rejects on
   * purpose. The files themselves stay identical to upstream so syncs don't conflict; if upstream
   * changes one of these lines, the test fails until the edit here is updated.
   */
  private static final ImmutableMap<String, ImmutableMap<String, String>> VGS_EDITS =
      ImmutableMap.of(
          "json.star",
          // Non-ASCII octal escapes are an error in VGS strings. The branch that uses it only runs
          // under Bazel's UTF-8 byte strings, which VGS doesn't have.
          ImmutableMap.of("'\"\\360\"'", "'\"\\u00f0\"'"));

  @Parameters(name = "{0}")
  public static List<Object[]> files() {
    List<Object[]> params = new ArrayList<>();
    for (File dir : new File[] {TESTDATA, VGS_TESTDATA, BC_TESTDATA}) {
      String[] names = dir.list((d, name) -> name.endsWith(".star"));
      if (names == null) {
        continue; // no such directory
      }
      Arrays.sort(names);
      for (String name : names) {
        params.add(new Object[] {name, new File(dir, name)});
      }
    }
    return params;
  }

  private final String name;
  private final File file;

  public ScriptFilesTest(String name, File file) {
    this.name = name;
    this.file = file;
  }

  @Test
  public void runFile() throws Throwable {
    AtomicReference<Boolean> ok = new AtomicReference<>();
    AtomicReference<Throwable> error = new AtomicReference<>();
    Thread thread =
        new Thread(
            null,
            () -> {
              try {
                ok.set(ScriptTest.runFile(withVgsEdits(file)));
              } catch (Throwable t) {
                error.set(t);
              }
            },
            "script-" + name,
            STACK_SIZE);
    thread.start();
    thread.join();
    if (error.get() != null) {
      throw error.get();
    }
    assertWithMessage("%s failed; see stderr for details", name).that(ok.get()).isTrue();
  }

  /** Returns {@code file}, or a temporary copy with this file's {@link #VGS_EDITS} applied. */
  private static File withVgsEdits(File file) throws Exception {
    ImmutableMap<String, String> edits = VGS_EDITS.get(file.getName());
    if (edits == null) {
      return file;
    }
    String content = Files.readString(file.toPath(), UTF_8);
    for (var edit : edits.entrySet()) {
      if (!content.contains(edit.getKey())) {
        throw new AssertionError(
            file + " no longer contains " + edit.getKey() + "; update ScriptFilesTest.VGS_EDITS");
      }
      content = content.replace(edit.getKey(), edit.getValue());
    }
    Path dir = Files.createTempDirectory("vgs-testdata");
    Path copy = dir.resolve(file.getName());
    Files.writeString(copy, content, UTF_8);
    copy.toFile().deleteOnExit();
    dir.toFile().deleteOnExit();
    return copy.toFile();
  }
}
