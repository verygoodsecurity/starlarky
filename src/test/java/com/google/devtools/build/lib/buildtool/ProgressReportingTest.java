// Copyright 2020 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.buildtool;

import static com.google.common.truth.Truth.assertThat;
import static com.google.devtools.build.lib.testutil.MoreAsserts.assertContainsEvent;
import static com.google.devtools.build.lib.testutil.MoreAsserts.assertDoesNotContainEvent;

import com.google.common.collect.Iterables;
import com.google.devtools.build.lib.analysis.util.AnalysisMock;
import com.google.devtools.build.lib.buildtool.util.BuildIntegrationTestCase;
import com.google.devtools.build.lib.events.EventCollector;
import com.google.devtools.build.lib.events.EventKind;
import com.google.devtools.build.lib.testutil.Suite;
import com.google.devtools.build.lib.testutil.TestSpec;
import com.google.devtools.build.lib.unix.UnixFileSystem;
import com.google.devtools.build.lib.vfs.DigestHashFunction;
import com.google.devtools.build.lib.vfs.FileSystem;
import com.google.devtools.build.lib.vfs.Path;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test for progress reporting.
 */
@TestSpec(size = Suite.MEDIUM_TESTS)
@RunWith(JUnit4.class)
public class ProgressReportingTest extends BuildIntegrationTestCase {
  private static enum PathOp {
    DELETE,
  }

  @FunctionalInterface
  private interface Receiver {
    public void accept(Path path, PathOp op);
  }

  private Receiver receiver;

  @Before
  public final void getIgnoreReceiver() throws Exception  {
    receiver = (x, y) -> {};
  }

  @Override
  protected boolean realFileSystem() {
    // Must have real filesystem for MockTools to give us an environment we can execute actions in.
    return true;
  }

  @Override
  protected FileSystem createFileSystem() {
    return new UnixFileSystem(DigestHashFunction.SHA256, /*hashAttributeName=*/ "") {
      private void recordAccess(PathOp op, Path path) {
        if (receiver != null) {
          receiver.accept(path, op);
        }
      }

      @Override
      public boolean delete(Path path) throws IOException {
        recordAccess(PathOp.DELETE, path);
        return super.delete(path);
      }
    };
  }

  /**
   * Tests that [for host] tags are added to the progress messages of actions in the
   * host configuration, but not in the target configuration.
   */
  @Test
  public void testAdditionalInfo() throws Exception {
    AnalysisMock.get().pySupport().setup(mockToolsConfig);
    write("x/BUILD",
        "py_binary(name = 'bin',",
        "          srcs = ['bin.py'])",
        "genrule(name = 'x',",
        "        outs = ['out']," +
        "        cmd = 'echo test > $@'," +
        "        tools = [':bin'])");
    write("x/bin.py");

    EventCollector collector = new EventCollector(EventKind.START);
    events.addHandler(collector);

    buildTarget("//x");

    assertContainsEvent(collector, "Expanding template x/bin [for host]");
    assertContainsEvent(collector, "Creating source manifest for //x:bin [for host]");
    assertContainsEvent(collector,
        "Creating runfiles tree blaze-out/host/bin/x/bin.runfiles [for host]");
    assertContainsEvent(collector, "Executing genrule //x:x");
    assertDoesNotContainEvent(collector, "Executing genrule //x:x [for host]");
  }

  @Test
  public void testPreparingMessage() throws Exception {
    write("x/BUILD",
        "genrule(name = 'x',",
        "        outs = ['slowdelete'],",
        "        cmd = 'touch $@')");
    buildTarget("//x");
    final Path output = Iterables.getOnlyElement(getArtifacts("//x:x")).getPath();
    assertThat(output.delete()).isTrue();
    receiver =
        (path, op) -> {
          if (output.equals(path) && op == PathOp.DELETE) {
            try {
              // When the action tries to delete its outputs (during the "preparing" stage of action
              // execution), we block on the deletion for enough time that the status reporter
              // prints out a "Preparing:" progress message.
              Thread.sleep(4000);
            } catch (InterruptedException e) {
              throw new IllegalStateException(e);
            }
          }
        };
    addOptions("--progress_report_interval=1");
    EventCollector collector = new EventCollector(EventKind.PROGRESS);
    events.addHandler(collector);

    buildTarget("//x");
    assertContainsEvent(collector, "Preparing:");
    assertContainsEvent(collector, "Executing genrule //x:x");
  }

  @Test
  public void testWaitForResources() throws Exception {
    write("x/BUILD",
        "genrule(name = 'x',",
        "        outs = ['x.out'],",
        "        local = 1,",
        "        cmd = 'sleep 3; touch $@')",
        "genrule(name = 'y',",
        "        outs = ['y.out'],",
        "        local = 1,",
        "        cmd = 'sleep 3; touch $@')"
        );
    // GenRuleAction currently specifies 300,1.0,0.0. If that changes, this may have to be changed
    // in order to keep exactly one genrule running at a time.
    addOptions(
        "--progress_report_interval=1",
        "--local_ram_resources=1000",
        "--local_cpu_resources=1",
        "--show_progress_rate_limit=-1");
    EventCollector collector = new EventCollector(EventKind.PROGRESS);
    events.addHandler(collector);
    buildTarget("//x:x", "//x:y");

    assertContainsEvent(collector, "Scheduling:");
    assertContainsEvent(collector, "Executing genrule //x:x");
    assertContainsEvent(collector, "Executing genrule //x:y");
  }
}
