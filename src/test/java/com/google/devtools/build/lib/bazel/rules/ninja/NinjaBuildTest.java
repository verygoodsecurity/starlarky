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

package com.google.devtools.build.lib.bazel.rules.ninja;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.devtools.build.lib.actions.ActionAnalysisMetadata;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.CommandLines.CommandLineAndParamFileInfo;
import com.google.devtools.build.lib.actions.util.ActionsTestUtil;
import com.google.devtools.build.lib.analysis.ConfiguredRuleClassProvider;
import com.google.devtools.build.lib.analysis.ConfiguredTarget;
import com.google.devtools.build.lib.analysis.OutputGroupInfo;
import com.google.devtools.build.lib.analysis.actions.SymlinkAction;
import com.google.devtools.build.lib.analysis.configuredtargets.RuleConfiguredTarget;
import com.google.devtools.build.lib.analysis.util.BuildViewTestCase;
import com.google.devtools.build.lib.bazel.rules.ninja.actions.NinjaAction;
import com.google.devtools.build.lib.bazel.rules.ninja.actions.NinjaBuildRule;
import com.google.devtools.build.lib.bazel.rules.ninja.actions.NinjaGraphRule;
import com.google.devtools.build.lib.testutil.TestRuleClassProvider;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for the {@code NinjaBuild} configured target factory. */
@RunWith(JUnit4.class)
public class NinjaBuildTest extends BuildViewTestCase {

  @Override
  protected ConfiguredRuleClassProvider createRuleClassProvider() {
    ConfiguredRuleClassProvider.Builder builder = new ConfiguredRuleClassProvider.Builder();
    TestRuleClassProvider.addStandardRules(builder);
    builder.addRuleDefinition(new NinjaGraphRule());
    builder.addRuleDefinition(new NinjaBuildRule());
    return builder.build();
  }

  @Before
  public void setUp() throws Exception {
    setBuildLanguageOptions("--experimental_ninja_actions");
  }

  @Test
  public void testSourceFileNotInSubtree() throws Exception {
    rewriteWorkspace("toplevel_output_directories(paths=['out'])");

    scratch.file("a/n.ninja", "rule cp", " command = cp $in $out", "build out/o: cp subdir/i");

    scratch.file(
        "a/BUILD",
        "ninja_graph(name='graph', output_root='out', main='n.ninja')",
        "ninja_build(name='build', ninja_graph=':graph', output_groups={'o': ['out/o']})");

    reporter.removeHandler(failFastHandler);
    getConfiguredTarget("//a:build");
    assertContainsEvent(
        "Source artifact 'subdir/i' is not under the package directory 'a' of ninja_build rule");
  }

  @Test
  public void testNinjaBuildRule() throws Exception {
    rewriteWorkspace(
        "workspace(name = 'test')", "toplevel_output_directories(paths = ['build_config'])");

    scratch.file("build_config/input.txt", "World");
    scratch.file(
        "build_config/build.ninja",
        "rule echo",
        "  command = echo \"Hello $$(cat ${in})!\" > ${out}",
        "  description = Creating ${out}",
        "build build_config/hello.txt: echo build_config/input.txt");

    // Working directory is workspace root.
    ConfiguredTarget configuredTarget =
        scratchConfiguredTarget(
            "",
            "ninja_target",
            "ninja_graph(name = 'graph', output_root = 'build_config',",
            " main = 'build_config/build.ninja',",
            " output_root_inputs = ['input.txt'])",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups= {'main': ['build_config/hello.txt']})");
    assertThat(configuredTarget).isInstanceOf(RuleConfiguredTarget.class);
    RuleConfiguredTarget ninjaConfiguredTarget = (RuleConfiguredTarget) configuredTarget;
    ImmutableList<ActionAnalysisMetadata> actions = ninjaConfiguredTarget.getActions();
    assertThat(actions).hasSize(1);
    ActionAnalysisMetadata action = Iterables.getOnlyElement(actions);
    assertThat(action).isInstanceOf(NinjaAction.class);
    NinjaAction ninjaAction = (NinjaAction) action;
    List<CommandLineAndParamFileInfo> commandLines =
        ninjaAction.getCommandLines().getCommandLines();
    assertThat(commandLines).hasSize(1);
    assertThat(commandLines.get(0).commandLine.toString())
        .endsWith("echo \"Hello $(cat build_config/input.txt)!\" > build_config/hello.txt");
    assertThat(ninjaAction.getPrimaryInput().getExecPathString())
        .isEqualTo("build_config/input.txt");
    assertThat(ninjaAction.getPrimaryOutput().getExecPathString())
        .isEqualTo("build_config/hello.txt");
    assertThat(ninjaAction.getProgressMessage()).isEqualTo("Creating build_config/hello.txt");
  }

  @Test
  public void testNinjaBuildRule_progressMessageFromRule() throws Exception {
    rewriteWorkspace(
        "workspace(name = 'test')", "toplevel_output_directories(paths = ['build_config'])");

    scratch.file("build_config/input.txt", "World");
    scratch.file(
        "build_config/build.ninja",
        "file_variable = with greetings",
        "rule echo",
        "  command = echo \"Hello $$(cat ${in})!\" > ${out}",
        "  description = ${action} ${out} ${file_variable}",
        "build build_config/hello.txt: echo build_config/input.txt",
        "  action = Creating");

    // Working directory is workspace root.
    ConfiguredTarget configuredTarget =
        scratchConfiguredTarget(
            "",
            "ninja_target",
            "ninja_graph(name = 'graph', output_root = 'build_config',",
            " main = 'build_config/build.ninja',",
            " output_root_inputs = ['input.txt'])",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups= {'main': ['build_config/hello.txt']})");
    RuleConfiguredTarget ninjaConfiguredTarget = (RuleConfiguredTarget) configuredTarget;
    ImmutableList<ActionAnalysisMetadata> actions = ninjaConfiguredTarget.getActions();
    NinjaAction ninjaAction = (NinjaAction) Iterables.getOnlyElement(actions);

    // The rule description is expanded with rule, build and file level variables.
    assertThat(ninjaAction.getProgressMessage())
        .isEqualTo("Creating build_config/hello.txt with greetings");
  }

  @Test
  public void testNinjaBuildRule_progressMessageFromBuildStatement() throws Exception {
    rewriteWorkspace(
        "workspace(name = 'test')", "toplevel_output_directories(paths = ['build_config'])");

    scratch.file("build_config/input.txt", "World");
    scratch.file(
        "build_config/build.ninja",
        "file_variable = foo bar baz",
        "rule echo",
        "  command = echo \"Hello $$(cat ${in})!\" > ${out}",
        "  description = Creating ${out}",
        "build build_config/hello.txt: echo build_config/input.txt",
        "  description = ${file_variable} qux ");

    // Working directory is workspace root.
    ConfiguredTarget configuredTarget =
        scratchConfiguredTarget(
            "",
            "ninja_target",
            "ninja_graph(name = 'graph', output_root = 'build_config',",
            " main = 'build_config/build.ninja',",
            " output_root_inputs = ['input.txt'])",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups= {'main': ['build_config/hello.txt']})");
    RuleConfiguredTarget ninjaConfiguredTarget = (RuleConfiguredTarget) configuredTarget;
    ImmutableList<ActionAnalysisMetadata> actions = ninjaConfiguredTarget.getActions();
    NinjaAction ninjaAction = (NinjaAction) Iterables.getOnlyElement(actions);

    // The build description is expanded with the file level variable.
    assertThat(ninjaAction.getProgressMessage()).isEqualTo("foo bar baz qux ");
  }

  @Test
  public void testNinjaBuildRule_progressMessageGeneratedAtRuntime() throws Exception {
    rewriteWorkspace(
        "workspace(name = 'test')", "toplevel_output_directories(paths = ['build_config'])");

    scratch.file("build_config/input.txt", "World");
    scratch.file(
        "build_config/build.ninja",
        "rule echo",
        "  command = echo \"Hello $$(cat ${in})!\" > ${out}",
        "build build_config/hello.txt: echo build_config/input.txt");

    // Working directory is workspace root.
    ConfiguredTarget configuredTarget =
        scratchConfiguredTarget(
            "",
            "ninja_target",
            "ninja_graph(name = 'graph', output_root = 'build_config',",
            " main = 'build_config/build.ninja',",
            " output_root_inputs = ['input.txt'])",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups= {'main': ['build_config/hello.txt']})");
    RuleConfiguredTarget ninjaConfiguredTarget = (RuleConfiguredTarget) configuredTarget;
    ImmutableList<ActionAnalysisMetadata> actions = ninjaConfiguredTarget.getActions();
    NinjaAction ninjaAction = (NinjaAction) Iterables.getOnlyElement(actions);

    // No description in either rule or build statements, so pretty print with rule name and output
    // basenames.
    assertThat(ninjaAction.getProgressMessage()).isEqualTo("[rule echo] Outputs: hello.txt");
  }

  @Test
  public void testNinjaGraphRuleWithPhonyTarget() throws Exception {
    rewriteWorkspace(
        "workspace(name = 'test')", "toplevel_output_directories(paths = ['build_config'])");

    scratch.file("build_config/input.txt", "World");
    scratch.file(
        "build_config/build.ninja",
        "rule echo",
        "  command = echo \"Hello $$(cat ${in})!\" > ${out}",
        "build hello.txt: echo input.txt",
        "build alias: phony hello.txt");

    ConfiguredTarget configuredTarget =
        scratchConfiguredTarget(
            "",
            "ninja_target",
            "ninja_graph(name = 'graph', output_root = 'build_config',",
            " working_directory = 'build_config',",
            " main = 'build_config/build.ninja',",
            " output_root_inputs = ['input.txt'])",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups= {'main': ['alias']})");
    assertThat(configuredTarget).isInstanceOf(RuleConfiguredTarget.class);
    RuleConfiguredTarget ninjaConfiguredTarget = (RuleConfiguredTarget) configuredTarget;
    ImmutableList<ActionAnalysisMetadata> actions = ninjaConfiguredTarget.getActions();
    ActionAnalysisMetadata action = Iterables.getOnlyElement(actions);

    assertThat(actions).hasSize(1);
    assertThat(action).isInstanceOf(NinjaAction.class);
    NinjaAction ninjaAction = (NinjaAction) action;
    List<CommandLineAndParamFileInfo> commandLines =
        ninjaAction.getCommandLines().getCommandLines();
    assertThat(commandLines).hasSize(1);
    assertThat(commandLines.get(0).commandLine.toString())
        .endsWith("cd build_config && echo \"Hello $(cat input.txt)!\" > hello.txt");
    assertThat(ninjaAction.getPrimaryInput().getExecPathString())
        .isEqualTo("build_config/input.txt");
    assertThat(ninjaAction.getPrimaryOutput().getExecPathString())
        .isEqualTo("build_config/hello.txt");
    assertThat(ninjaAction.getProgressMessage()).isEqualTo("[rule echo] Outputs: hello.txt");
  }

  @Test
  public void testNinjaGraphRuleWithPhonyTree() throws Exception {
    rewriteWorkspace(
        "workspace(name = 'test')", "toplevel_output_directories(paths = ['build_config'])");

    scratch.file("build_config/a.txt", "A");
    scratch.file("build_config/b.txt", "B");
    scratch.file("build_config/c.txt", "C");
    scratch.file("build_config/d.txt", "D");
    scratch.file("build_config/e.txt", "E");

    scratch.file(
        "build_config/build.ninja",
        "rule cat",
        "  command = cat ${in} > ${out}",
        "rule echo",
        "  command = echo \"Hello $$(cat ${in} | tr '\\r\\n' ' ')!\" > ${out}",
        "build a: cat a.txt",
        "build b: cat b.txt",
        "build c: cat c.txt",
        "build d: cat d.txt",
        // e should be executed unconditionally as it depends on always-dirty phony action
        "build e: cat e.txt always_dirty",
        "build always_dirty: phony",
        "build group1: phony a b c",
        "build group2: phony d e",
        "build inputs_alias: phony group1 group2",
        "build hello.txt: echo inputs_alias",
        "build alias: phony hello.txt");

    ConfiguredTarget configuredTarget =
        scratchConfiguredTarget(
            "",
            "ninja_target",
            "ninja_graph(name = 'graph', output_root = 'build_config',",
            " working_directory = 'build_config',",
            " main = 'build_config/build.ninja',",
            " output_root_inputs = ['a.txt', 'b.txt', 'c.txt', 'd.txt', 'e.txt'])",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups= {'main': ['alias']})");
    assertThat(configuredTarget).isInstanceOf(RuleConfiguredTarget.class);
    RuleConfiguredTarget ninjaConfiguredTarget = (RuleConfiguredTarget) configuredTarget;
    ImmutableList<ActionAnalysisMetadata> actions = ninjaConfiguredTarget.getActions();
    assertThat(actions).hasSize(6);
    List<String> outputs = Lists.newArrayList();
    actions.forEach(a -> outputs.add(Iterables.getOnlyElement(a.getOutputs()).getExecPathString()));
    assertThat(outputs)
        .containsExactlyElementsIn(
            new String[] {
              "build_config/hello.txt",
              "build_config/a",
              "build_config/b",
              "build_config/c",
              "build_config/d",
              "build_config/e"
            });

    for (ActionAnalysisMetadata action : actions) {
      Artifact artifact = action.getPrimaryOutput();
      if ("hello.txt".equals(artifact.getFilename())) {
        assertThat(action).isInstanceOf(NinjaAction.class);
        NinjaAction ninjaAction = (NinjaAction) action;
        List<CommandLineAndParamFileInfo> commandLines =
            ninjaAction.getCommandLines().getCommandLines();
        assertThat(commandLines).hasSize(1);
        assertThat(commandLines.get(0).commandLine.toString())
            .contains(
                "cd build_config && echo \"Hello $(cat inputs_alias | tr '\\r\\n' ' ')!\""
                    + " > hello.txt");
        List<String> inputPaths =
            ninjaAction.getInputs().toList().stream()
                .map(Artifact::getExecPathString)
                .collect(Collectors.toList());
        assertThat(inputPaths)
            .containsExactly(
                "build_config/a",
                "build_config/b",
                "build_config/c",
                "build_config/d",
                "build_config/e");
        assertThat(ninjaAction.getPrimaryOutput().getExecPathString())
            .isEqualTo("build_config/hello.txt");
      } else if ("e".equals(artifact.getFilename())) {
        assertThat(action).isInstanceOf(NinjaAction.class);
        NinjaAction ninjaAction = (NinjaAction) action;
        List<CommandLineAndParamFileInfo> commandLines =
            ninjaAction.getCommandLines().getCommandLines();
        assertThat(commandLines).hasSize(1);
        assertThat(commandLines.get(0).commandLine.toString())
            .endsWith("cd build_config && cat e.txt always_dirty > e");
        assertThat(ninjaAction.executeUnconditionally()).isTrue();
      }
    }
  }

  @Test
  public void testDepsMapping() throws Exception {
    rewriteWorkspace(
        "workspace(name = 'test')", "toplevel_output_directories(paths = ['build_config'])");

    scratch.file("input.txt", "World");
    scratch.file(
        "build_config/build.ninja",
        "rule echo",
        "  command = echo \"Hello $$(cat ${in})!\" > ${out}",
        "build hello.txt: echo placeholder");

    ConfiguredTarget configuredTarget =
        scratchConfiguredTarget(
            "",
            "ninja_target",
            "ninja_graph(name = 'graph', output_root = 'build_config',",
            " working_directory = 'build_config',",
            " main = 'build_config/build.ninja')",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups= {'main': ['hello.txt']},",
            " deps_mapping = {'placeholder': ':input.txt'})");
    assertThat(configuredTarget).isInstanceOf(RuleConfiguredTarget.class);
    RuleConfiguredTarget ninjaConfiguredTarget = (RuleConfiguredTarget) configuredTarget;
    ImmutableList<ActionAnalysisMetadata> actions = ninjaConfiguredTarget.getActions();
    assertThat(actions).hasSize(2);

    ActionAnalysisMetadata symlinkAction = actions.get(0);
    assertThat(symlinkAction).isInstanceOf(SymlinkAction.class);
    assertThat(symlinkAction.getPrimaryInput().getExecPathString()).isEqualTo("input.txt");
    assertThat(symlinkAction.getPrimaryOutput().getExecPathString())
        .isEqualTo("build_config/placeholder");

    ActionAnalysisMetadata action = actions.get(1);
    assertThat(action).isInstanceOf(NinjaAction.class);
    NinjaAction ninjaAction = (NinjaAction) action;
    List<CommandLineAndParamFileInfo> commandLines =
        ninjaAction.getCommandLines().getCommandLines();
    assertThat(commandLines).hasSize(1);
    assertThat(commandLines.get(0).commandLine.toString())
        .endsWith("cd build_config && echo \"Hello $(cat placeholder)!\" > hello.txt");
    assertThat(ninjaAction.getPrimaryInput().getExecPathString())
        .isEqualTo("build_config/placeholder");
    assertThat(ninjaAction.getPrimaryOutput().getExecPathString())
        .isEqualTo("build_config/hello.txt");
  }

  @Test
  public void testOnlySubGraphIsCreated() throws Exception {
    rewriteWorkspace(
        "workspace(name = 'test')", "toplevel_output_directories(paths = ['build_config'])");

    scratch.file("build_config/a.txt", "A");
    scratch.file("build_config/b.txt", "B");
    scratch.file("build_config/c.txt", "C");
    scratch.file("build_config/d.txt", "D");
    scratch.file("build_config/e.txt", "E");

    scratch.file(
        "build_config/build.ninja",
        "rule cat",
        "  command = cat ${in} > ${out}",
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

    ConfiguredTarget configuredTarget =
        scratchConfiguredTarget(
            "",
            "ninja_target",
            "ninja_graph(name = 'graph', output_root = 'build_config',",
            " working_directory = 'build_config',",
            " main = 'build_config/build.ninja',",
            " output_root_inputs = ['a.txt', 'b.txt', 'c.txt', 'd.txt', 'e.txt'])",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups= {'main': ['group1']})");
    assertThat(configuredTarget).isInstanceOf(RuleConfiguredTarget.class);
    RuleConfiguredTarget ninjaConfiguredTarget = (RuleConfiguredTarget) configuredTarget;
    ImmutableList<ActionAnalysisMetadata> actions = ninjaConfiguredTarget.getActions();
    assertThat(actions).hasSize(3);
    List<String> outputs = Lists.newArrayList();
    actions.forEach(a -> outputs.add(Iterables.getOnlyElement(a.getOutputs()).getExecPathString()));
    assertThat(outputs)
        .containsExactlyElementsIn(
            new String[] {
              "build_config/a", "build_config/b", "build_config/c",
            });
  }

  @Test
  public void testRuleWithDepfileVariable() throws Exception {
    rewriteWorkspace(
        "workspace(name = 'test')", "toplevel_output_directories(paths = ['build_config'])");

    scratch.file("input");
    scratch.file(
        "build_config/build.ninja",
        "rule rule123",
        "  command = executable -d ${depfile} ${in} > ${out}",
        "  depfile = ${out}.d",
        "  deps = gcc",
        "build out_file: rule123 ../input");

    ConfiguredTarget configuredTarget =
        scratchConfiguredTarget(
            "",
            "ninja_target",
            "ninja_graph(name = 'graph', output_root = 'build_config',",
            " working_directory = 'build_config',",
            " main = 'build_config/build.ninja')",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups= {'main': ['out_file']})");
    assertThat(configuredTarget).isInstanceOf(RuleConfiguredTarget.class);
    RuleConfiguredTarget ninjaConfiguredTarget = (RuleConfiguredTarget) configuredTarget;
    ImmutableList<ActionAnalysisMetadata> actions = ninjaConfiguredTarget.getActions();
    assertThat(actions).hasSize(1);

    ActionAnalysisMetadata action = Iterables.getOnlyElement(actions);
    assertThat(action).isInstanceOf(NinjaAction.class);
    List<CommandLineAndParamFileInfo> commandLines =
        ((NinjaAction) action).getCommandLines().getCommandLines();
    assertThat(commandLines).hasSize(1);
    assertThat(commandLines.get(0).commandLine.toString())
        .endsWith("cd build_config && executable -d out_file.d ../input > out_file");

    assertThat(ActionsTestUtil.baseArtifactNames(action.getOutputs()))
        .containsExactly("out_file", "out_file.d");
  }

  @Test
  public void testCreateOutputSymlinkArtifacts() throws Exception {
    rewriteWorkspace(
        "workspace(name = 'test')", "toplevel_output_directories(paths = ['build_config'])");

    scratch.file(
        "build_config/build.ninja",
        "rule symlink_rule",
        "  command = ln -s fictive-file ${out}",
        "  symlink_outputs = $out",
        "build dangling_symlink: symlink_rule");

    ConfiguredTarget configuredTarget =
        scratchConfiguredTarget(
            "",
            "ninja_target",
            "ninja_graph(name = 'graph', output_root = 'build_config',",
            " working_directory = 'build_config',",
            " main = 'build_config/build.ninja')",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups= {'main': ['dangling_symlink']})");
    assertThat(configuredTarget).isInstanceOf(RuleConfiguredTarget.class);
    RuleConfiguredTarget ninjaConfiguredTarget = (RuleConfiguredTarget) configuredTarget;
    ImmutableList<ActionAnalysisMetadata> actions = ninjaConfiguredTarget.getActions();
    assertThat(actions).hasSize(1);

    ActionAnalysisMetadata action = Iterables.getOnlyElement(actions);
    Artifact primaryOutput = action.getPrimaryOutput();
    assertThat(primaryOutput.isSymlink()).isTrue();
    assertThat(action).isInstanceOf(NinjaAction.class);

    List<CommandLineAndParamFileInfo> commandLines =
        ((NinjaAction) action).getCommandLines().getCommandLines();
    assertThat(commandLines).hasSize(1);
    assertThat(commandLines.get(0).commandLine.toString())
        .endsWith("cd build_config && ln -s fictive-file dangling_symlink");
  }

  @Test
  public void testCreateIntermediateOutputSymlinkArtifacts() throws Exception {
    rewriteWorkspace(
        "workspace(name = 'test')", "toplevel_output_directories(paths = ['build_config'])");

    scratch.file(
        "build_config/build.ninja",
        "rule symlink_rule",
        "  command = ln -s fictive-file ${out}",
        "rule cat",
        "  command = cat ${in} > ${out}",
        "build dangling_symlink: symlink_rule",
        "  symlink_outputs = dangling_symlink",
        "build mybuild: cat dangling_symlink");

    ConfiguredTarget configuredTarget =
        scratchConfiguredTarget(
            "",
            "ninja_target",
            "ninja_graph(name = 'graph', output_root = 'build_config',",
            " working_directory = 'build_config',",
            " main = 'build_config/build.ninja')",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups= {'main': ['mybuild']})");
    assertThat(configuredTarget).isInstanceOf(RuleConfiguredTarget.class);
    RuleConfiguredTarget ninjaConfiguredTarget = (RuleConfiguredTarget) configuredTarget;
    ImmutableList<ActionAnalysisMetadata> actions = ninjaConfiguredTarget.getActions();
    assertThat(actions).hasSize(2);

    ActionAnalysisMetadata symlinkAction = actions.get(1);
    Artifact primaryOutput = symlinkAction.getPrimaryOutput();
    assertThat(primaryOutput.isSymlink()).isTrue();
    assertThat(symlinkAction).isInstanceOf(NinjaAction.class);

    List<CommandLineAndParamFileInfo> commandLines =
        ((NinjaAction) symlinkAction).getCommandLines().getCommandLines();
    assertThat(commandLines).hasSize(1);
    assertThat(commandLines.get(0).commandLine.toString())
        .endsWith("cd build_config && ln -s fictive-file dangling_symlink");
  }

  @Test
  public void testOutputRootInputsWithConflictingNinjaActionOutput() throws Exception {
    rewriteWorkspace(
        "workspace(name = 'test')", "toplevel_output_directories(paths = ['build_config'])");

    scratch.file("build_config/hello.txt", "hello");
    scratch.file(
        "build_config/build.ninja",
        "rule hello_world",
        "  command = echo \"Hello World!\" > ${out}",
        "build build_config/hello.txt: hello_world",
        "rule echo",
        "  command = echo \"Hello $$(cat ${in})!\" > ${out}",
        "build build_config/out.txt: echo build_config/hello.txt");

    // Working directory is workspace root.
    ConfiguredTarget configuredTarget =
        scratchConfiguredTarget(
            "",
            "ninja_target",
            "ninja_graph(name = 'graph', output_root = 'build_config',",
            " main = 'build_config/build.ninja',",
            // hello.txt will be symlinked using a symlink action, but will also be an output of
            // the NinjaAction created from the ninja rule above.
            " output_root_inputs = ['hello.txt'])",
            "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
            " output_groups= {'main': ['build_config/out.txt']})");

    RuleConfiguredTarget ninjaConfiguredTarget = (RuleConfiguredTarget) configuredTarget;
    ImmutableList<ActionAnalysisMetadata> actions = ninjaConfiguredTarget.getActions();
    // The build.ninja file has rules for two actions, but only one of them should have been
    // registered. Normally this would produce an ActionsConflictException, but we skip the action.
    assertThat(actions).hasSize(1);
    assertThat(actions.get(0).getOutputs().asList().get(0).getExecPathString())
        .isEqualTo("build_config/out.txt");
  }

  @Test
  public void testOutputRootInputsWithConflictingNinjaActionOutputThrowsErrorOnNonSymlinkOutput()
      throws Exception {

    rewriteWorkspace(
        "workspace(name = 'test')", "toplevel_output_directories(paths = ['build_config'])");

    scratch.file("build_config/hello.txt", "hello");
    scratch.file(
        "build_config/build.ninja",
        "rule hello_world",
        "  command = echo \"Hello World!\" > ${out}",
        "build build_config/hello.txt build_config/not_an_input_symlink.txt: hello_world",
        "rule echo",
        "  command = echo \"Hello $$(cat ${in})!\" > ${out}",
        "build build_config/out.txt: echo build_config/hello.txt");

    String message =
        "in ninja_build rule //:ninja_target: Ninja target hello_world has outputs in "
            + "output_root_inputs and other outputs not in output_root_inputs:\n"
            + "Outputs in output_root_inputs:\n"
            + "  build_config/hello.txt\n"
            + "Outputs not in output_root_inputs:\n"
            + "  build_config/not_an_input_symlink.txt";

    // A GenericParsingException is what's actually created, however the rule code and the
    // testing framework only relays the exception's messasge wrapped in an AssertionError.
    Throwable throwable =
        assertThrows(
            AssertionError.class,
            () ->
                scratchConfiguredTarget(
                    "",
                    "ninja_target",
                    "ninja_graph(name = 'graph', output_root = 'build_config',",
                    " main = 'build_config/build.ninja',",
                    // hello.txt will be symlinked using a symlink action, but will also be an
                    // output of
                    // the NinjaAction created from the ninja rule above.
                    " output_root_inputs = ['hello.txt'])",
                    "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
                    " output_groups= {'main': ['build_config/out.txt']})"));
    assertThat(throwable).hasMessageThat().contains(message);
  }

  /**
   * Tests that outputs from validation actions are correctly read from Ninja files and added to the
   * validation output groups. Note that validation inputs syntax ("|@") is specific to AOSP's
   * implementation of Ninja.
   */
  @Test
  public void testNinjaValidationInputs() throws Exception {
    rewriteWorkspace(
        "workspace(name = 'test')", "toplevel_output_directories(paths = ['build_config'])");

    scratch.file("build_config/input.txt", "World");
    scratch.file("build_config/validation_input.txt", "6\n7\n8");
    scratch.file(
        "build_config/build.ninja",
        "rule validate",
        "  command = grep 7 ${in} > ${out}",
        "  description = Validating input",
        "rule echo",
        "  command = echo \"Hello $$(cat ${in})!\" > ${out}",
        "  description = Creating ${out}",
        "build build_config/validation_output.txt: validate build_config/validation_input.txt",
        "build build_config/hello.txt: echo build_config/input.txt "
            + "|@ build_config/validation_output.txt",
        "build build_config/hello2.txt: echo build_config/input.txt");

    // Working directory is workspace root.
    RuleConfiguredTarget configuredTarget =
        (RuleConfiguredTarget)
            scratchConfiguredTarget(
                "",
                "ninja_target",
                "ninja_graph(name = 'graph', output_root = 'build_config',",
                " main = 'build_config/build.ninja',",
                " output_root_inputs = ['input.txt', 'validation_input.txt'])",
                "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
                " output_groups = {'main': ['build_config/hello.txt']})",
                "ninja_build(name = 'ninja_target2', ninja_graph = 'graph',",
                " output_groups = {'main': ['build_config/hello2.txt']})");

    OutputGroupInfo outputGroupInfo = OutputGroupInfo.get(configuredTarget);
    List<Artifact> validationArtifacts =
        outputGroupInfo.getOutputGroup(OutputGroupInfo.VALIDATION).toList();
    assertThat(validationArtifacts).hasSize(1);
    assertThat(validationArtifacts.get(0).getExecPathString())
        .isEqualTo("build_config/validation_output.txt");

    ActionAnalysisMetadata echoAction = getGeneratingAction(configuredTarget, "hello.txt");

    // Sanity check that we're specifying the paths correctly and double check that we have the
    // right action.
    assertThat(actionInputsToPaths(echoAction.getInputs())).contains("build_config/input.txt");
    // The validation output should not be in the direct inputs of the action which depends on it,
    // so that this action is not blocked on generating that validation input.
    assertThat(actionInputsToPaths(echoAction.getInputs()))
        .doesNotContain("build_config/validation_output.txt");

    // ninja_target2 does not have a ninja build rule with a validation input, so it should not
    // have any validation outputs in its validation output group.
    RuleConfiguredTarget targetNoValidations =
        (RuleConfiguredTarget) getConfiguredTarget("//:ninja_target2");
    assertThat(
            OutputGroupInfo.get(targetNoValidations)
                .getOutputGroup(OutputGroupInfo.VALIDATION)
                .toList())
        .isEmpty();
  }

  @Test
  public void testNinjaTransitiveValidationInputs() throws Exception {
    rewriteWorkspace(
        "workspace(name = 'test')", "toplevel_output_directories(paths = ['build_config'])");

    scratch.file("build_config/input.txt", "World");
    scratch.file("build_config/validation_input.txt", "6\n7\n8");
    scratch.file(
        "build_config/build.ninja",
        "rule validate",
        "  command = grep 7 ${in} > ${out}",
        "  description = Validating input",
        "rule echo",
        "  command = echo \"Hello $$(cat ${in})!\" > ${out}",
        "  description = Creating ${out}",
        "build build_config/validation_output.txt: validate build_config/validation_input.txt",
        "build build_config/hello.txt: echo build_config/input.txt "
            + "|@ build_config/validation_output.txt",
        "build build_config/hello2.txt: echo build_config/hello.txt");

    // Working directory is workspace root.
    RuleConfiguredTarget configuredTarget =
        (RuleConfiguredTarget)
            scratchConfiguredTarget(
                "",
                "ninja_target",
                "ninja_graph(name = 'graph', output_root = 'build_config',",
                " main = 'build_config/build.ninja',",
                " output_root_inputs = ['input.txt', 'validation_input.txt'])",
                "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
                // requesting hello2, whose generating action itself does not have a validation
                // action
                " output_groups = {'main': ['build_config/hello2.txt']})");

    OutputGroupInfo outputGroupInfo = OutputGroupInfo.get(configuredTarget);
    List<Artifact> validationArtifacts =
        outputGroupInfo.getOutputGroup(OutputGroupInfo.VALIDATION).toList();
    assertThat(validationArtifacts).hasSize(1);
    // The validation output group should still have the output of the validation action even if
    // that action is only in the transitive deps of the requested top-level artifact.
    assertThat(validationArtifacts.get(0).getExecPathString())
        .isEqualTo("build_config/validation_output.txt");
  }

  /**
   * Tests that the validation outputs from two independent build graphs in a ninja file are all
   * registered in the validation output group when those graphs are referenced from a nina_build
   * target.
   */
  @Test
  public void testNinjaValidationInputsIndependentValidationsAdded() throws Exception {
    rewriteWorkspace(
        "workspace(name = 'test')", "toplevel_output_directories(paths = ['build_config'])");

    scratch.file("build_config/input.txt", "World");
    scratch.file("build_config/validation_input.txt", "6\n7\n8");
    scratch.file("build_config/validation_input2.txt", "9\n0\n1");
    scratch.file(
        "build_config/build.ninja",
        "rule validate",
        "  command = grep 7 ${in} > ${out}",
        "  description = Validating input",
        "rule echo",
        "  command = echo \"Hello $$(cat ${in})!\" > ${out}",
        "  description = Creating ${out}",
        // two validation outputs
        "build build_config/validation_output.txt: validate build_config/validation_input.txt",
        "build build_config/validation_output2.txt: validate build_config/validation_input2.txt",
        // two build rules that consume validation inputs
        "build build_config/hello.txt: echo build_config/input.txt "
            + "|@ build_config/validation_output.txt",
        "build build_config/hello2.txt: echo build_config/input.txt "
            + "|@ build_config/validation_output2.txt");

    // Working directory is workspace root.
    RuleConfiguredTarget configuredTarget =
        (RuleConfiguredTarget)
            scratchConfiguredTarget(
                "",
                "ninja_target",
                "ninja_graph(name = 'graph', output_root = 'build_config',",
                " main = 'build_config/build.ninja',",
                " output_root_inputs = ['input.txt', 'validation_input.txt',"
                    + " 'validation_input2.txt'])",
                "ninja_build(name = 'ninja_target', ninja_graph = 'graph',",
                " output_groups = {",
                "   'out1': ['build_config/hello.txt'],",
                "   'out2': ['build_config/hello2.txt'],",
                " })");

    OutputGroupInfo outputGroupInfo = OutputGroupInfo.get(configuredTarget);
    List<Artifact> validationArtifacts =
        outputGroupInfo.getOutputGroup(OutputGroupInfo.VALIDATION).toList();
    assertThat(ActionsTestUtil.execPaths(validationArtifacts))
        .containsExactly(
            "build_config/validation_output.txt", "build_config/validation_output2.txt");
  }
}
