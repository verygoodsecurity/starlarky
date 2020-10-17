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

package com.google.devtools.build.lib.blackbox.tests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.devtools.build.lib.blackbox.framework.BuilderRunner;
import com.google.devtools.build.lib.blackbox.framework.ProcessResult;
import com.google.devtools.build.lib.blackbox.junit.AbstractBlackBoxTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/** Integration test for Ninja execution functionality. */
public class NinjaBlackBoxTest extends AbstractBlackBoxTest {
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    context().write(".bazelignore", "build_dir");
    context()
        .write(
            WORKSPACE,
            String.format("workspace(name = '%s')", testName.getMethodName()),
            "toplevel_output_directories(paths = ['build_dir'])");
  }

  @Test
  public void testOneTarget() throws Exception {
    context().write("build_dir/input.txt", "World");
    context()
        .write(
            "build_dir/build.ninja",
            "rule echo",
            "  command = echo \"Hello $$(cat ${in})!\" > ${out}",
            "build hello.txt: echo input.txt");
    context()
        .write(
            "BUILD",
            "ninja_graph(name = 'graph', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build.ninja',",
            " output_root_inputs = ['input.txt'])",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups = {'group': ['hello.txt']})");

    BuilderRunner bazel = context().bazel().withFlags("--experimental_ninja_actions");
    assertConfigured(bazel.build("//:ninja_target"));
    Path path = context().resolveExecRootPath(bazel, "build_dir/hello.txt");
    assertThat(path.toFile().exists()).isTrue();
    assertThat(Files.readAllLines(path)).containsExactly("Hello World!");

    // React to input change.
    context().write("build_dir/input.txt", "Sun");
    assertNothingConfigured(bazel.build("//:ninja_target"));
    assertThat(Files.readAllLines(path)).containsExactly("Hello Sun!");

    // React to Ninja file change.
    context()
        .write(
            "build_dir/build.ninja",
            "rule echo",
            "  command = echo \"Hello $$(cat ${in}):)\" > ${out}",
            "build hello.txt: echo input.txt");
    assertConfigured(bazel.build("//:ninja_target"));
    assertThat(Files.readAllLines(path)).containsExactly("Hello Sun:)");
  }

  @Test
  public void testWithoutExperimentalFlag() throws Exception {
    context().write("build_dir/input.txt", "World");
    context()
        .write(
            "build_dir/build.ninja",
            "rule echo",
            "  command = echo \"Hello $$(cat ${in})!\" > ${out}",
            "build hello.txt: echo input.txt");
    context()
        .write(
            "BUILD",
            "ninja_graph(name = 'graph', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build.ninja',",
            " output_root_inputs = ['input.txt'])",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups = {'group': ['hello.txt']})");

    BuilderRunner bazel = context().bazel();
    ProcessResult result = bazel.shouldFail().build("//:ninja_target");
    assertThat(result.errString()).contains("name 'toplevel_output_directories' is not defined");
    assertThat(result.errString()).contains("FAILED: Build did NOT complete successfully");
  }

  @Test
  public void testWithoutMainNinja() throws Exception {
    context().write("build_dir/input.txt", "World");
    context()
        .write(
            "build_dir/build.ninja",
            "rule echo",
            "  command = echo \"Hello $$(cat ${in})!\" > ${out}",
            "build hello.txt: echo input.txt");
    context()
        .write(
            "BUILD",
            "ninja_graph(name = 'graph', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " output_root_inputs = ['input.txt'])",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups = {'group': ['hello.txt']})");

    BuilderRunner bazel = context().bazel().withFlags("--experimental_ninja_actions");
    ProcessResult result = bazel.shouldFail().build("//:ninja_target");
    assertThat(result.errString())
        .contains("//:graph: missing value for mandatory attribute 'main' in 'ninja_graph' rule");
    assertThat(result.errString()).contains("FAILED: Build did NOT complete successfully");
  }

  @Test
  public void testSourceFileIsMissingUnderOutputRoot() throws Exception {
    context().write("input.txt", "World");
    context()
        .write(
            "build_dir/build.ninja",
            "rule echo",
            "  command = echo \"Hello $$(cat ${in})!\" > ${out}",
            "build hello.txt: echo build_dir/input.txt");
    context()
        .write(
            "BUILD",
            "ninja_graph(name = 'graph', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build.ninja')",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups = {'group': ['hello.txt']})");

    BuilderRunner bazel = context().bazel().withFlags("--experimental_ninja_actions");
    ProcessResult result = bazel.shouldFail().build("//:ninja_target");
    assertThat(result.errString())
        .contains(
            "in ninja_build rule //:ninja_target: The following artifacts do not have a generating "
                + "action in Ninja file: build_dir/build_dir/input.txt");
    assertThat(result.errString()).contains("FAILED: Build did NOT complete successfully");
  }

  private static void assertNothingConfigured(ProcessResult result) {
    assertThat(result.errString())
        .contains(
            "INFO: Analyzed target //:ninja_target (0 packages loaded, 0 targets configured).");
  }

  private static void assertConfigured(ProcessResult result) {
    assertThat(result.errString())
        .doesNotContain(
            "INFO: Analyzed target //:ninja_target (0 packages loaded, 0 targets configured).");
  }

  @Test
  public void testNullBuild() throws Exception {
    // Print nanoseconds fraction of the current time into the output file.
    context()
        .write(
            "build_dir/build.ninja",
            "rule echo_time",
            "  command = date +%N >> ${out}",
            "build nano.txt: echo_time");
    context()
        .write(
            "BUILD",
            "ninja_graph(name = 'graph', ",
            "output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build.ninja')",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups = {'group': ['nano.txt']})");

    BuilderRunner bazel = context().bazel().withFlags("--experimental_ninja_actions");
    assertConfigured(bazel.build("//:ninja_target"));
    Path path = context().resolveExecRootPath(bazel, "build_dir/nano.txt");
    assertThat(path.toFile().exists()).isTrue();
    List<String> text = Files.readAllLines(path);
    assertThat(text).isNotEmpty();
    long lastModified = path.toFile().lastModified();

    // Should be null build, as nothing changed.
    assertNothingConfigured(bazel.build("//:ninja_target"));
    assertThat(Files.readAllLines(path)).containsExactly(text.get(0));
    assertThat(path.toFile().lastModified()).isEqualTo(lastModified);
  }

  @Test
  public void testInteroperabilityWithBazel() throws Exception {
    context().write("bazel_input.txt", "World");
    context()
        .write(
            "build_dir/build.ninja",
            "rule echo",
            "  command = echo \"Hello $$(cat ${in})!\" > ${out}",
            "build hello.txt: echo placeholder",
            "build hello2.txt: echo placeholder2");
    context()
        .write(
            "BUILD",
            "ninja_graph(name = 'graph', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build.ninja')",
            "filegroup(name = 'bazel_built_input', srcs = [':bazel_input.txt'])",
            "ninja_build(name = 'ninja_target1', ninja_graph = 'graph',",
            " deps_mapping = {'placeholder': ':bazel_built_input'},",
            " output_groups = {'group': ['hello.txt']})",
            "filegroup(name = 'bazel_middle', srcs = [':ninja_target1'])",
            "ninja_build(name = 'ninja_target2', ninja_graph = 'graph',",
            " deps_mapping = {'placeholder2': ':bazel_middle'},",
            " output_groups = {'group': ['hello2.txt']})");

    BuilderRunner bazel = context().bazel().withFlags("--experimental_ninja_actions");
    assertConfigured(bazel.build("//..."));
    Path path1 = context().resolveExecRootPath(bazel, "build_dir/hello.txt");
    Path path2 = context().resolveExecRootPath(bazel, "build_dir/hello2.txt");

    assertThat(Files.readAllLines(path1)).containsExactly("Hello World!");
    assertThat(Files.readAllLines(path2)).containsExactly("Hello Hello World!!");
  }

  @Test
  public void testInteroperabilityWithBazelCycle() throws Exception {
    context().write("bazel_input.txt", "World");
    context()
        .write(
            "build_dir/build.ninja",
            "rule echo",
            "  command = echo \"Hello $$(cat ${in})!\" > ${out}",
            "build hello.txt: echo placeholder",
            "build hello2.txt: echo placeholder2");
    context()
        .write(
            "BUILD",
            "ninja_graph(name = 'graph', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build.ninja')",

            // Cycle here with bazel_middle.
            "filegroup(name = 'bazel_built_input', srcs = [':bazel_input.txt', ':bazel_middle'])",
            "ninja_build(name = 'ninja_target1', ninja_graph = 'graph',",
            " deps_mapping = {'placeholder': ':bazel_built_input'},",
            " output_groups = {'group': ['hello.txt']})",
            "filegroup(name = 'bazel_middle', srcs = [':ninja_target1'])",
            "ninja_build(name = 'ninja_target2', ninja_graph = 'graph',",
            " deps_mapping = {'placeholder2': ':bazel_middle'},",
            " output_groups = {'group': ['hello2.txt']})");

    BuilderRunner bazel = context().bazel().withFlags("--experimental_ninja_actions");
    Exception exception = assertThrows(Exception.class, () -> bazel.build("//..."));
    assertThat(exception).hasMessageThat().contains("cycle in dependency graph");
  }

  @Test
  public void testDisjointPhonyNinjaParts() throws Exception {
    context().write("build_dir/a.txt", "A");
    context().write("build_dir/b.txt", "B");
    context().write("build_dir/c.txt", "C");
    context().write("build_dir/d.txt", "D");
    context().write("build_dir/e.txt", "E");

    context()
        .write(
            "build_dir/build.ninja",
            "rule cat",
            "  command = echo '<<' $$(cat ${in}) '>>' > ${out}",
            "rule echo",
            "  command = echo \"Hello $$(cat ${in} | tr '\\r\\n' ' ')!\" > ${out}",
            "build a: cat a.txt",
            "build b: cat b.txt",
            "build c: cat c.txt",
            "build d: cat d.txt",
            "build e: cat e.txt",
            "build group1: phony a b c",
            "build group2: phony d e",
            "build inputs_alias: phony group1 group2",
            "build hello.txt: echo inputs_alias",
            "build alias: phony hello.txt");

    context()
        .write(
            "BUILD",
            "ninja_graph(name = 'graph', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build.ninja',",
            " output_root_inputs = ['a.txt', 'b.txt', 'c.txt', 'd.txt', 'e.txt'])",
            "ninja_build(name = 'ninja_target1', ninja_graph = 'graph',",
            " output_groups= {'main': ['group1']})",
            "ninja_build(name = 'ninja_target2', ninja_graph = 'graph',",
            " output_groups= {'main': ['group2']})");

    BuilderRunner bazel = context().bazel().withFlags("--experimental_ninja_actions");
    assertConfigured(bazel.build("//..."));
    Path pathA = context().resolveExecRootPath(bazel, "build_dir/a");
    Path pathB = context().resolveExecRootPath(bazel, "build_dir/b");
    Path pathC = context().resolveExecRootPath(bazel, "build_dir/c");
    Path pathD = context().resolveExecRootPath(bazel, "build_dir/d");
    Path pathE = context().resolveExecRootPath(bazel, "build_dir/e");

    assertThat(Files.readAllLines(pathA)).containsExactly("<< A >>");
    assertThat(Files.readAllLines(pathB)).containsExactly("<< B >>");
    assertThat(Files.readAllLines(pathC)).containsExactly("<< C >>");
    assertThat(Files.readAllLines(pathD)).containsExactly("<< D >>");
    assertThat(Files.readAllLines(pathE)).containsExactly("<< E >>");
  }

  @Test
  public void testPhonyNinjaPartsWithSharedPart() throws Exception {
    context().write("build_dir/a.txt", "A");
    context().write("build_dir/b.txt", "B");
    context().write("build_dir/c.txt", "C");
    context().write("build_dir/d.txt", "D");
    context().write("build_dir/e.txt", "E");

    context()
        .write(
            "build_dir/build.ninja",
            "rule cat",
            "  command = echo '<<' $$(cat ${in}) '>>' > ${out}",
            "build a: cat a.txt",
            "build b: cat b.txt",
            "build c: cat c.txt",
            "build d: cat d.txt",
            "build e: cat e.txt",
            // 'a' is present in both groups, built by Bazel since file 'a' is produced by
            // equal-without-owner actions.
            "build group1: phony a b c",
            "build group2: phony a d e");

    context()
        .write(
            "BUILD",
            "ninja_graph(name = 'graph', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build.ninja',",
            " output_root_inputs = ['a.txt', 'b.txt', 'c.txt', 'd.txt', 'e.txt'])",
            "ninja_build(name = 'ninja_target1', ninja_graph = 'graph',",
            " output_groups= {'main': ['group1']})",
            "ninja_build(name = 'ninja_target2', ninja_graph = 'graph',",
            " output_groups= {'main': ['group2']})");

    BuilderRunner bazel = context().bazel().withFlags("--experimental_ninja_actions");
    assertConfigured(bazel.build("//..."));
    Path pathA = context().resolveExecRootPath(bazel, "build_dir/a");
    Path pathB = context().resolveExecRootPath(bazel, "build_dir/b");
    Path pathC = context().resolveExecRootPath(bazel, "build_dir/c");
    Path pathD = context().resolveExecRootPath(bazel, "build_dir/d");
    Path pathE = context().resolveExecRootPath(bazel, "build_dir/e");

    assertThat(Files.readAllLines(pathA)).containsExactly("<< A >>");
    assertThat(Files.readAllLines(pathB)).containsExactly("<< B >>");
    assertThat(Files.readAllLines(pathC)).containsExactly("<< C >>");
    assertThat(Files.readAllLines(pathD)).containsExactly("<< D >>");
    assertThat(Files.readAllLines(pathE)).containsExactly("<< E >>");
  }

  @Test
  public void testDisjointNinjaParts() throws Exception {
    context().write("build_dir/a.txt", "A");
    context().write("build_dir/b.txt", "B");
    context().write("build_dir/c.txt", "C");
    context().write("build_dir/d.txt", "D");
    context().write("build_dir/e.txt", "E");

    context()
        .write(
            "build_dir/build.ninja",
            "rule cat",
            "  command = echo '<<' $$(cat ${in}) '>>' > ${out}",
            "build a: cat a.txt",
            "build b: cat b.txt",
            "build c: cat c.txt",
            "build d: cat d.txt",
            "build e: cat e.txt",
            "build group1: phony a b c",
            "build group2: phony d e",
            "build inputs_alias: phony group1 group2",
            "build hello.txt: cat inputs_alias",
            "build alias: phony hello.txt");

    context()
        .write(
            "BUILD",
            "ninja_graph(name = 'graph', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build.ninja',",
            " output_root_inputs = ['a.txt', 'b.txt', 'c.txt', 'd.txt', 'e.txt'])",
            "ninja_build(name = 'ninja_target1', ninja_graph = 'graph',",
            " output_groups= {'main': ['a']})",
            "ninja_build(name = 'ninja_target2', ninja_graph = 'graph',",
            " output_groups= {'main': ['e']})");

    BuilderRunner bazel = context().bazel().withFlags("--experimental_ninja_actions");
    assertConfigured(bazel.build("//..."));
    Path pathA = context().resolveExecRootPath(bazel, "build_dir/a");
    Path pathE = context().resolveExecRootPath(bazel, "build_dir/e");
    assertThat(Files.readAllLines(pathA)).containsExactly("<< A >>");
    assertThat(Files.readAllLines(pathE)).containsExactly("<< E >>");

    Path pathB = context().resolveExecRootPath(bazel, "build_dir/b");
    Path pathC = context().resolveExecRootPath(bazel, "build_dir/c");
    Path pathD = context().resolveExecRootPath(bazel, "build_dir/d");
    assertThat(Files.exists(pathB)).isFalse();
    assertThat(Files.exists(pathC)).isFalse();
    assertThat(Files.exists(pathD)).isFalse();
  }

  @Test
  public void testDuplicateNinjaParts() throws Exception {
    context().write("build_dir/a.txt", "A");
    context().write("build_dir/b.txt", "B");
    context().write("build_dir/c.txt", "C");
    context().write("build_dir/d.txt", "D");
    context().write("build_dir/e.txt", "E");

    context()
        .write(
            "build_dir/build.ninja",
            "rule cat",
            "  command = echo '<<' $$(cat ${in}) '>>' > ${out}",
            "build a: cat a.txt",
            "build b: cat b.txt");

    context()
        .write(
            "BUILD",
            "ninja_graph(name = 'graph', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build.ninja',",
            " output_root_inputs = ['a.txt', 'b.txt', 'c.txt', 'd.txt', 'e.txt'])",
            // 'a' is present in both ninja_build targets, built by Bazel since file 'a' is produced
            // by equal-without-owner actions.
            "ninja_build(name = 'ninja_target1', ninja_graph = 'graph',",
            " output_groups= {'main': ['a']})",
            "ninja_build(name = 'ninja_target2', ninja_graph = 'graph',",
            " output_groups= {'main': ['a']})");

    BuilderRunner bazel = context().bazel().withFlags("--experimental_ninja_actions");
    assertConfigured(bazel.build("//..."));
    Path pathA = context().resolveExecRootPath(bazel, "build_dir/a");
    assertThat(Files.readAllLines(pathA)).containsExactly("<< A >>");

    Path pathB = context().resolveExecRootPath(bazel, "build_dir/b");
    assertThat(Files.exists(pathB)).isFalse();
  }

  @Test
  public void testDuplicateNinjaPartsDifferentMappings() throws Exception {
    context().write("variant1.txt", "variant1");
    context().write("variant2.txt", "variant2");

    context()
        .write(
            "build_dir/build.ninja",
            "rule append",
            "  command = echo '<<' $$(cat ${in}) '>>' >> ${out}",
            "build a: append a.txt");

    context()
        .write(
            "BUILD",
            "ninja_graph(name = 'graph', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build.ninja')",
            "ninja_build(name = 'ninja_target1', ninja_graph = 'graph',",
            " output_groups= {'main': ['a']}, deps_mapping = {'a.txt': ':variant1.txt'})",
            "ninja_build(name = 'ninja_target2', ninja_graph = 'graph',",
            " output_groups= {'main': ['a']}, deps_mapping = {'a.txt': ':variant2.txt'})");

    BuilderRunner bazel = context().bazel().withFlags("--experimental_ninja_actions");

    Exception exception = assertThrows(Exception.class, () -> bazel.build("//..."));
    assertThat(exception)
        .hasMessageThat()
        .contains("ERROR: file 'a.txt' is generated by these conflicting actions:");
    assertThat(exception)
        .hasMessageThat()
        .containsMatch(
            "for a\\.txt, previous action: action "
                + "'Symlinking deps_mapping entry 'variant[12]\\.txt' to 'build_dir/a\\.txt'', "
                + "attempted action: action "
                + "'Symlinking deps_mapping entry 'variant[12]\\.txt' to 'build_dir/a\\.txt''");
  }

  @Test
  public void testDependentNinjaActions() throws Exception {
    context().write("build_dir/a.txt", "A");

    context()
        .write(
            "build_dir/build1.ninja",
            "rule cat",
            "  command = echo '<<' $$(cat ${in}) '>>' > ${out}",
            "build first.txt: cat a.txt");
    context()
        .write(
            "build_dir/build2.ninja",
            "rule cat",
            "  command = echo '<<' $$(cat ${in}) '>>' > ${out}",
            "build second.txt: cat input");

    // For the dependent Ninja actions from the same Ninja graph, Ninja mechanisms should be used.
    context()
        .write(
            "BUILD",
            "ninja_graph(name = 'graph1', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build1.ninja',",
            " output_root_inputs = ['a.txt'])",
            "ninja_build(name = 'ninja_target1', ninja_graph = 'graph1',",
            " output_groups= {'main': ['first.txt']})",
            "ninja_graph(name = 'graph2', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build2.ninja')",
            "ninja_build(name = 'ninja_target2', ninja_graph = 'graph2',",
            " output_groups= {'main': ['second.txt']}, deps_mapping = {'input':"
                + " ':ninja_target1'})");

    BuilderRunner bazel = context().bazel().withFlags("--experimental_ninja_actions");
    assertConfigured(bazel.build("//..."));
    Path pathFirst = context().resolveExecRootPath(bazel, "build_dir/first.txt");
    assertThat(Files.readAllLines(pathFirst)).containsExactly("<< A >>");
    Path pathSecond = context().resolveExecRootPath(bazel, "build_dir/second.txt");
    assertThat(Files.readAllLines(pathSecond)).containsExactly("<< << A >> >>");
  }

  @Test
  public void testDependentNinjaActionsCycle() throws Exception {
    context()
        .write(
            "build_dir/build1.ninja",
            "rule cat",
            "  command = echo '<<' $$(cat ${in}) '>>' > ${out}",
            "build first.txt: cat input");
    context()
        .write(
            "build_dir/build2.ninja",
            "rule cat",
            "  command = echo '<<' $$(cat ${in}) '>>' > ${out}",
            "build second.txt: cat input");

    // For the dependent Ninja actions from the same Ninja graph, Ninja mechanisms should be used.
    context()
        .write(
            "BUILD",
            "ninja_graph(name = 'graph1', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build1.ninja')",
            "ninja_build(name = 'ninja_target1', ninja_graph = 'graph1',",
            " output_groups= {'main': ['first.txt']}, deps_mapping = {'input': ':ninja_target2'})",
            "ninja_graph(name = 'graph2', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build2.ninja')",
            "ninja_build(name = 'ninja_target2', ninja_graph = 'graph2',",
            " output_groups= {'main': ['second.txt']}, deps_mapping = {'input':"
                + " ':ninja_target1'})");

    BuilderRunner bazel = context().bazel().withFlags("--experimental_ninja_actions");
    Exception exception = assertThrows(Exception.class, () -> bazel.build("//..."));
    assertThat(exception).hasMessageThat().contains("cycle in dependency graph");
  }

  @Test
  public void testRspFileWritten() throws Exception {
    context().write("input.txt", "input");
    context()
        .write(
            "build_dir/build.ninja",
            "rule cat",
            "  command = echo '<<' $$(cat ${in}) '>>' > ${out}",
            "  rspfile = ${out}.rsp",
            "  rspfile_content = ${in}",
            "build first.txt: cat ../input.txt");

    context()
        .write(
            "BUILD",
            "ninja_graph(name = 'graph', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build.ninja')",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups= {'main': ['first.txt']})");

    BuilderRunner bazel = context().bazel().withFlags("--experimental_ninja_actions");
    assertConfigured(bazel.build("//..."));
    Path pathFirst = context().resolveExecRootPath(bazel, "build_dir/first.txt");
    assertThat(Files.readAllLines(pathFirst)).containsExactly("<< input >>");

    Path rspFile = context().resolveExecRootPath(bazel, "build_dir/first.txt.rsp");
    assertThat(Files.exists(rspFile)).isTrue();
    assertThat(Files.readAllLines(rspFile)).containsExactly("../input.txt");
  }

  @Test
  public void testOrderOnlyInputsDoNotCauseRebuild() throws Exception {
    context().write("input.txt", "abc");
    context().write("order-only-input.txt", "123");
    context()
        .write(
            "build_dir/build.ninja",
            "rule cat",
            "  command = cat ${in} ${order_only_input} > ${out}",
            "build output.txt: cat ../input.txt || ../order-only-input.txt",
            "  order_only_input = ../order-only-input.txt");

    context()
        .write(
            "BUILD",
            "ninja_graph(name = 'graph', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build.ninja')",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups = {'main': ['output.txt']})");

    BuilderRunner bazel = context().bazel().withFlags("--experimental_ninja_actions");

    assertConfigured(bazel.build("//:ninja_target"));
    Path outputPath = context().resolveExecRootPath(bazel, "build_dir/output.txt");
    assertThat(outputPath.toFile().exists()).isTrue();
    assertThat(Files.readAllLines(outputPath)).containsExactly("abc", "123");

    // Change the order-only input...
    context().write("order-only-input.txt", "456");
    // ...but nothing should rerun....
    assertNothingConfigured(bazel.build("//:ninja_target"));
    // ...and the output should remain the same.
    assertThat(Files.readAllLines(outputPath)).containsExactly("abc", "123");

    // Change the main input...
    context().write("input.txt", "xyz");
    bazel.build("//:ninja_target");
    // ...and the output should have the new content from the order-only input.
    assertThat(Files.readAllLines(outputPath)).containsExactly("xyz", "456");
  }

  @Test
  public void testOrderOnlyInputsInDepFileCauseRebuild() throws Exception {
    context().write("build_dir/in.txt", "abc");
    context().write("build_dir/order-only-input1.txt", "123");
    context().write("build_dir/order-only-input2.txt", "456");
    context()
        .write(
            "build_dir/build.ninja",
            "rule cat",
            "  command = cat ${in} ${order_only_input1} ${order_only_input2} > ${out} ; "
                // TODO(b/156961649): Having to put "build_dir/" in front of ${order_only_input2}
                // might be a bug
                + "echo \"${out} : build_dir/${order_only_input2}\" > ${depfile}",
            "  depfile = df",
            "build output.txt: cat in.txt || order-only-input1.txt order-only-input2.txt",
            "  order_only_input1 = order-only-input1.txt",
            "  order_only_input2 = order-only-input2.txt");

    context()
        .write(
            "BUILD",
            "ninja_graph(name = 'graph', output_root = 'build_dir',",
            " working_directory = 'build_dir',",
            " main = 'build_dir/build.ninja',",
            " output_root_inputs = ['in.txt', 'order-only-input1.txt', 'order-only-input2.txt'],)",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups = {'main': ['output.txt']})");

    BuilderRunner bazel = context().bazel().withFlags("--experimental_ninja_actions");

    ProcessResult pr = bazel.build("//:ninja_target");
    System.out.println(pr.outString());
    assertConfigured(pr);
    Path outputPath = context().resolveExecRootPath(bazel, "build_dir/output.txt");
    assertThat(outputPath.toFile().exists()).isTrue();
    assertThat(Files.readAllLines(outputPath)).containsExactly("abc", "123", "456");

    // Change the order-only input...
    context().write("build_dir/order-only-input1.txt", "789");
    // ...but nothing should rerun....
    assertNothingConfigured(bazel.build("//:ninja_target"));
    // ...and the output should remain the same.
    assertThat(Files.readAllLines(outputPath)).containsExactly("abc", "123", "456");

    // But change the order-only input that's listed in the depfile...
    context().write("build_dir/order-only-input2.txt", "xyz");
    bazel.build("//:ninja_target");
    // ...and the output should have the new content from both order-only inputs.
    assertThat(Files.readAllLines(outputPath)).containsExactly("abc", "789", "xyz");
  }
}
