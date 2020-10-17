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

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.devtools.build.lib.actions.Action;
import com.google.devtools.build.lib.actions.ActionAnalysisMetadata;
import com.google.devtools.build.lib.actions.ActionGraph;
import com.google.devtools.build.lib.actions.ActionGraphVisitor;
import com.google.devtools.build.lib.actions.ActionKeyContext;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.extra.ExtraActionInfo;
import com.google.devtools.build.lib.analysis.ConfiguredTarget;
import com.google.devtools.build.lib.analysis.ExtraActionArtifactsProvider;
import com.google.devtools.build.lib.analysis.ViewCreationFailedException;
import com.google.devtools.build.lib.analysis.extra.ExtraActionSpec;
import com.google.devtools.build.lib.buildtool.util.GoogleBuildIntegrationTestCase;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.rules.java.JavaInfo;
import com.google.devtools.build.lib.rules.java.JavaRuleOutputJarsProvider;
import com.google.devtools.build.lib.testutil.Suite;
import com.google.devtools.build.lib.testutil.TestSpec;
import com.google.devtools.build.lib.util.Fingerprint;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.common.options.OptionsParsingException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests the the action_listener/extra_action feature.
 * (--experimental_action_listener blaze option)
 */
@TestSpec(size = Suite.MEDIUM_TESTS)
@RunWith(JUnit4.class)
public class ActionListenerIntegrationTest extends GoogleBuildIntegrationTestCase {
  protected final ActionKeyContext actionKeyContext = new ActionKeyContext();

  private Map<ConfiguredTarget, Iterable<Artifact.DerivedArtifact>> getExtraArtifactMap() {
    Map<ConfiguredTarget, Iterable<Artifact.DerivedArtifact>> result = new LinkedHashMap<>();
    for (ConfiguredTarget configuredTarget : getAllConfiguredTargets()) {
      ExtraActionArtifactsProvider provider = configuredTarget.getProvider(
          ExtraActionArtifactsProvider.class);
      if (provider != null && !provider.getExtraActionArtifacts().isEmpty()) {
        result.put(configuredTarget, provider.getExtraActionArtifacts().toList());
      }
    }
    return result;
  }

  private NestedSet<Artifact.DerivedArtifact> getExtraActionArtifacts(ConfiguredTarget target) {
    return target.getProvider(ExtraActionArtifactsProvider.class).getExtraActionArtifacts();
  }

  private void assertExtraActionOutputForJavaLibraryRule(String rule, String extraAction,
      boolean shouldBePresent, boolean shouldDependOnOutput) throws Exception {
    final String extraActionPath = extraAction.substring(2).replace(':', '/');
    final String rulePackage = rule.substring(2, rule.indexOf(':'));
    final String extraActionOutputRoot =
        "extra_actions/" + extraActionPath + "/" + rulePackage + "/";

    final ConfiguredTarget javalib = getConfiguredTarget(rule);

    assertThat(javalib).isNotNull();

    NestedSet<Artifact.DerivedArtifact> extraArtifacts = getExtraActionArtifacts(javalib);
    assertThat(extraArtifacts).isNotNull();

    final Set<ActionAnalysisMetadata> actions = new HashSet<>();

    class JavacFinderVisitor extends ActionGraphVisitor {
      public JavacFinderVisitor(ActionGraph actionGraph) {
        super(actionGraph);
      }

      @Override
      protected boolean shouldVisit(ActionAnalysisMetadata action) {
        return action.getOwner().getLabel().equals(javalib.getLabel());
      }

      @Override
      protected void visitAction(ActionAnalysisMetadata action) {
        if (action.getMnemonic().equals("Javac")) {
          actions.add(action);
        }
      }
    }

    JavacFinderVisitor visitor = new JavacFinderVisitor(getActionGraph());

    Set<Artifact> outputs =
        Sets.newHashSet(
            Iterables.concat(
                JavaInfo.getProvider(JavaRuleOutputJarsProvider.class, javalib)
                    .getAllSrcOutputJars(),
                JavaInfo.getProvider(JavaRuleOutputJarsProvider.class, javalib)
                    .getAllClassOutputJars()));
    outputs.addAll(getFilesToBuild(javalib).toList());
    visitor.visitWhiteNodes(outputs);

    assertThat(actions).isNotEmpty();

    assertThat(extraArtifacts.toList()).hasSize(2 * actions.size());

    for (ActionAnalysisMetadata action : actions) {
      boolean hasProtoArtifact = false;
      boolean hasTestArtifact = false;

      String ownerDigest =
          new Fingerprint().addString(action.getOwner().getLabel().toString()).hexDigestAndReset();

      String actionId =
          ExtraActionSpec.getActionId(actionKeyContext, action.getOwner(), (Action) action);
      String testArtifactPath = extraActionOutputRoot + ownerDigest + "_" + actionId + ".tst";
      String protoArtifactPath = extraActionOutputRoot + actionId + ".xa";

      for (Artifact extraArtifact : extraArtifacts.toList()) {
        Path path = extraArtifact.getPath();
        if (path.toString().endsWith(protoArtifactPath)) {
          hasProtoArtifact = true;
          if (shouldBePresent) {
            ExtraActionInfo.Builder builder = ExtraActionInfo.newBuilder();
            InputStream inputStream = path.getInputStream();
            builder.mergeFrom(inputStream);
            ExtraActionInfo info = builder.build();
            assertThat(info.getOwner()).isEqualTo(rule);
          }
          continue;
        }
        ActionAnalysisMetadata artifactOwningExtraAction = getActionGraph()
            .getGeneratingAction(extraArtifact);

        assertThat(artifactOwningExtraAction).isNotNull();

        Set<Artifact> extraActionInputs = artifactOwningExtraAction.getInputs().toSet();
        Set<Artifact> actionOutputs = Sets.newHashSet(action.getOutputs());
        if (shouldDependOnOutput) {
          // If the extra_action has require_action_output set, all of the outputs of the
          // shadowed action should be part of the extra_action's inputs.
          assertThat(extraActionInputs).containsAtLeastElementsIn(actionOutputs);
        } else {
          // If the extra_action doesn't have requires_action_output set, none of the outputs of the
          // shadowed action should be part of the extra_action's inputs.
          assertThat(Sets.intersection(extraActionInputs, actionOutputs)).isEmpty();
        }

        assertThat(path.exists()).isEqualTo(shouldBePresent);

        if (path.toString().endsWith(testArtifactPath)) {
          hasTestArtifact = true;
          if (shouldBePresent) {
            String contents = readContentAsLatin1String(extraArtifact);
            String[] lines = contents.split("\n");
            assertThat(lines).isNotEmpty();
            String firstLine = lines[0];

            assertThat(firstLine).endsWith(protoArtifactPath);
          }
        }
      }
      assertThat(hasProtoArtifact).isTrue();
      assertThat(hasTestArtifact).isTrue();
    }
  }

  @Test
  public void testBasicActionListener() throws Exception {
    write("nobuild/BUILD",
        "java_library(name= 'javalib',",
        "             srcs=[])",
        "extra_action(name = 'baz',",
        "             out_templates = ['$(OWNER_LABEL_DIGEST)_$(ACTION_ID).tst'],",
        "             cmd = " +
        "                 'echo $(EXTRA_ACTION_FILE)>$(output $(OWNER_LABEL_DIGEST)" +
            "_$(ACTION_ID).tst)')",
        "action_listener(name = 'bar',",
        "                mnemonics = ['Javac'],",
        "                extra_actions = [':baz'])");

    addOptions("--experimental_action_listener=//nobuild:bar");

    buildTarget("//nobuild:javalib");

    Map<ConfiguredTarget, Iterable<Artifact.DerivedArtifact>> extraArtifactsMap =
        getExtraArtifactMap();
    assertThat(extraArtifactsMap).hasSize(1);

    assertExtraActionOutputForJavaLibraryRule("//nobuild:javalib", "//nobuild:baz", true, false);
  }

   @Test
   public void testActionListenerThatRequiresActionOutputs() throws Exception {
    write("nobuild/BUILD",
        "java_library(name= 'javalib',",
        "             srcs=[])",
        "extra_action(name = 'baz',",
        "             out_templates = ['$(OWNER_LABEL_DIGEST)_$(ACTION_ID).tst'],",
        "             requires_action_output = 1,",
        "             cmd = " +
        "                 'echo $(EXTRA_ACTION_FILE)>$(output $(OWNER_LABEL_DIGEST)" +
        "_$(ACTION_ID).tst)')",
        "action_listener(name = 'bar',",
        "                mnemonics = ['Javac'],",
        "                extra_actions = [':baz'])");

    addOptions("--experimental_action_listener=//nobuild:bar");

    buildTarget("//nobuild:javalib");

    Map<ConfiguredTarget, Iterable<Artifact.DerivedArtifact>> extraArtifactsMap =
        getExtraArtifactMap();
    assertThat(extraArtifactsMap).hasSize(1);

    assertExtraActionOutputForJavaLibraryRule("//nobuild:javalib", "//nobuild:baz", true, true);
  }

  @Test
  public void testFilteredActionListener() throws Exception {
    write("filtered/BUILD",
        "java_library(name= 'a',",
        "             srcs=[])",
        "java_library(name= 'b',",
        "             exports=[':a'])",
        "extra_action(name = 'baz',",
        "             out_templates = ['$(OWNER_LABEL_DIGEST)_$(ACTION_ID).tst'],",
        "             cmd = " +
        "                 'echo $(EXTRA_ACTION_FILE)>$(output $(OWNER_LABEL_DIGEST)" +
            "_$(ACTION_ID).tst)')",
        "action_listener(name = 'bar',",
        "                mnemonics = ['Javac'],",
        "                extra_actions = [':baz'])");

    addOptions("--experimental_action_listener=//filtered:bar",
        "--experimental_extra_action_filter=.*\\:a");

    buildTarget("//filtered:b");

    Map<ConfiguredTarget, Iterable<Artifact.DerivedArtifact>> extraArtifactsMap =
        getExtraArtifactMap();
    assertThat(extraArtifactsMap).hasSize(2);

    assertExtraActionOutputForJavaLibraryRule("//filtered:a", "//filtered:baz", true, false);
    assertExtraActionOutputForJavaLibraryRule("//filtered:b", "//filtered:baz", false, false);
  }

  @Test
  public void testTopLevelOnlyActionListener() throws Exception {
    write("filtered/BUILD",
        "java_library(name= 'a',",
        "             srcs=[])",
        "java_library(name= 'b',",
        "             exports=[':a'])",
        "extra_action(name = 'baz',",
        "             out_templates = ['$(OWNER_LABEL_DIGEST)_$(ACTION_ID).tst'],",
        "             cmd = " +
        "                 'echo $(EXTRA_ACTION_FILE)>$(output $(OWNER_LABEL_DIGEST)" +
            "_$(ACTION_ID).tst)')",
        "action_listener(name = 'bar',",
        "                mnemonics = ['Javac'],",
        "                extra_actions = [':baz'])");

    addOptions("--experimental_action_listener=//filtered:bar",
        "--experimental_extra_action_top_level_only");

    buildTarget("//filtered:b");

    Map<ConfiguredTarget, Iterable<Artifact.DerivedArtifact>> extraArtifactsMap =
        getExtraArtifactMap();
    assertThat(extraArtifactsMap).hasSize(2);

    assertExtraActionOutputForJavaLibraryRule("//filtered:a", "//filtered:baz", false, false);
    assertExtraActionOutputForJavaLibraryRule("//filtered:b", "//filtered:baz", true, false);
  }

  @Test
  public void testCcTestActionListener() throws Exception {
    write("nobuild/main.cc",
        "int  main() { return 0; }");
    write("nobuild/BUILD",
        "cc_test(name= 'cctest',",
        "             srcs=['main.cc'])",
        "extra_action(name = 'baz',",
        "             out_templates = ['$(ACTION_ID).tst'],",
        "             cmd = " +
        "                 'echo $(EXTRA_ACTION_FILE)>$(output $(ACTION_ID).tst)')",
        "action_listener(name = 'bar',",
        "                mnemonics = ['CppCompile'],",
        "                extra_actions = [':baz'])");

    addOptions("--experimental_action_listener=//nobuild:bar");

    buildTarget("//nobuild:cctest");
    final ConfiguredTarget cctest = getConfiguredTarget("//nobuild:cctest");

    assertThat(cctest).isNotNull();

    NestedSet<Artifact.DerivedArtifact> extraArtifacts = getExtraActionArtifacts(cctest);
    assertThat(extraArtifacts).isNotNull();

    final Set<ActionAnalysisMetadata> actions = new HashSet<>();
    class CppCompileActionFinder extends ActionGraphVisitor {

      public CppCompileActionFinder(ActionGraph actionGraph) {
        super(actionGraph);
      }

      @Override
      protected boolean shouldVisit(ActionAnalysisMetadata action) {
        return action.getOwner().getLabel().equals(cctest.getLabel());
      }

      @Override
      protected void visitAction(ActionAnalysisMetadata action) {
        if (action.getMnemonic().equals("CppCompile")) {
          actions.add(action);
        }
      }
    }

    CppCompileActionFinder visitor = new CppCompileActionFinder(getActionGraph());

    Set<Artifact> outputs = new HashSet<>();
    outputs.addAll(getFilesToBuild(cctest).toList());
    visitor.visitWhiteNodes(outputs);

    assertThat(actions).isNotEmpty();
    assertThat(extraArtifacts.toList()).hasSize(2 * actions.size());

    for (ActionAnalysisMetadata action : actions) {
      boolean hasProtoArtifact = false;
      boolean hasTestArtifact = false;
      String actionId =
          ExtraActionSpec.getActionId(actionKeyContext, action.getOwner(), (Action) action);

      String testArtifactPath = "extra_actions/nobuild/baz/nobuild/" + actionId + ".tst";
      String protoArtifactPath = "extra_actions/nobuild/baz/nobuild/" + actionId + ".xa";

      for (Artifact extraArtifact : extraArtifacts.toList()) {
        Path path = extraArtifact.getPath();
        assertThat(path.exists()).isTrue();

        if (path.toString().endsWith(testArtifactPath)) {
          hasTestArtifact = true;

          String contents = readContentAsLatin1String(extraArtifact);
          String[] lines = contents.split("\n");
          assertThat(lines).isNotEmpty();
          String firstLine = lines[0];

          assertThat(firstLine).endsWith(protoArtifactPath);
        }
        if (path.toString().endsWith(protoArtifactPath)) {
          hasProtoArtifact = true;

          ExtraActionInfo.Builder builder = ExtraActionInfo.newBuilder();
          InputStream inputStream = path.getInputStream();
          builder.mergeFrom(inputStream);
          ExtraActionInfo info = builder.build();
          assertThat(info.getOwner()).isEqualTo("//nobuild:cctest");
        }
      }
      assertThat(hasProtoArtifact).isTrue();
      assertThat(hasTestArtifact).isTrue();
    }
  }


  @Test
  public void testActionListenerNotEnabled() throws Exception {
    write("nobuild/BUILD",
        "java_library(name= 'javalib',",
        "             srcs=[])",
        "extra_action(name = 'baz',",
        "             out_templates = ['$(ACTION_ID).tst'],",
        "             cmd = " +
            "'echo $(EXTRA_ACTION_FILE)>$(output $(ACTION_ID).tst)')",
        "action_listener(name = 'bar',",
        "                mnemonics = ['Javac'],",
        "                extra_actions = [':baz'])");

    buildTarget("//nobuild:javalib");
    ConfiguredTarget javalib = getConfiguredTarget("//nobuild:javalib");

    assertThat(javalib).isNotNull();

    Map<ConfiguredTarget, Iterable<Artifact.DerivedArtifact>> extraArtifactsMap =
        getExtraArtifactMap();
    assertThat(extraArtifactsMap).isEmpty();
  }

  @Test
  public void testBuildActionListener() throws Exception {
    write("nobuild/BUILD",
      "extra_action(name = 'action',",
      "             cmd = '')",
      "action_listener(name = 'listener',",
      "                mnemonics = ['Foo'],",
      "                extra_actions = [':action'])");
    buildTarget("//nobuild:listener");
    // Confirm target exists.
    getExistingConfiguredTarget("//nobuild:listener");
  }

  @Test
  public void testNotActionListenerLabel() throws Exception {
    write("nobuild/BUILD",
        "java_library(name= 'javalib1',",
        "             srcs=[])",
        "java_library(name= 'javalib2',",
        "             srcs=[])");
    addOptions("--experimental_action_listener=//nobuild:javalib1");
    try {
      buildTarget("//nobuild:javalib2");
      Assert.fail("expected failure");
    } catch (ViewCreationFailedException expected) {
      assertThat(expected)
          .hasMessageThat()
          .contains(
              String.format("Analysis of target '%s' failed; build aborted", "//nobuild:javalib2"));
    }
  }

  @Test
  public void testInvalidActionListenerLabel() throws Exception {
    write("nobuild/BUILD",
        "java_library(name= 'javalib',",
        "             srcs=[])");
    try {
      createOptionsParser().parse("--experimental_action_listener='this is \\not\\ a valid label'");
      Assert.fail("expected failure");
    } catch (OptionsParsingException ope) {
      assertThat(ope)
          .hasMessageThat()
          .isEqualTo(
              String.format(
                  "While parsing option %s='%s': invalid package name ''%s'': "
                      + "package names may contain "
                      + "A-Z, a-z, 0-9, or any of ' !\"#$%%&'()*+,-./;<=>?[]^_`{|}~' "
                      + "(most 7-bit ascii characters except 0-31, 127, ':', or '\\')",
                  "--experimental_action_listener",
                  "this is \\not\\ a valid label",
                  "this is \\not\\ a valid label"));
    }
  }

  /**
   * Ensure outputs checked for uniqueness.
   */
  @Test
  public void testNonUniqueOutputs() throws Exception {
    write("nobuild/BUILD",
        "java_library(name= 'javalib',",
        "             srcs=[])",
        "extra_action(name = 'baz',",
        "             out_templates = ['test.tst'],",
        "             cmd = 'echo $(output test.tst)')",
        "action_listener(name = 'bar',",
        "                mnemonics = ['Javac', 'JavaSourceJar'],",
        "                extra_actions = [':baz'])");

    addOptions("--experimental_action_listener=//nobuild:bar");

    try {
      buildTarget("//nobuild:javalib");
      Assert.fail("expected failure");
    } catch (ViewCreationFailedException vcfe) {
      assertThat(vcfe)
          .hasMessageThat()
          .contains(
              String.format("Analysis of target '%s' failed; build aborted", "//nobuild:javalib"));
    }
  }
}
