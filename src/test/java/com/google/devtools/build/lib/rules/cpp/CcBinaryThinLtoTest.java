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
package com.google.devtools.build.lib.rules.cpp;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.actions.Action;
import com.google.devtools.build.lib.actions.ActionAnalysisMetadata;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.util.ActionsTestUtil;
import com.google.devtools.build.lib.analysis.ConfiguredTarget;
import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.analysis.actions.SpawnAction;
import com.google.devtools.build.lib.analysis.util.AnalysisMock;
import com.google.devtools.build.lib.analysis.util.BuildViewTestCase;
import com.google.devtools.build.lib.cmdline.RepositoryName;
import com.google.devtools.build.lib.packages.util.Crosstool.CcToolchainConfig;
import com.google.devtools.build.lib.packages.util.MockCcSupport;
import com.google.devtools.build.lib.rules.cpp.CppConfiguration.Tool;
import com.google.devtools.build.lib.skyframe.ConfiguredTargetKey;
import com.google.devtools.build.lib.skyframe.ConfiguredTargetValue;
import com.google.devtools.build.lib.vfs.PathFragment;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for cc_binary with ThinLTO. */
@RunWith(JUnit4.class)
public class CcBinaryThinLtoTest extends BuildViewTestCase {

  @Before
  public void createBasePkg() throws IOException {
    scratch.overwriteFile(
        "base/BUILD", "cc_library(name = 'system_malloc', visibility = ['//visibility:public'])");
  }

  public void createBuildFiles(String targetName, String... extraCcBinaryParameters)
      throws Exception {
    scratch.file(
        "pkg/BUILD",
        "package(features = ['thin_lto'])",
        "",
        "cc_binary(name = '" + targetName + "',",
        "          srcs = ['binfile.cc', ],",
        "          deps = [ ':lib' ], ",
        String.join("", extraCcBinaryParameters),
        "          malloc = '//base:system_malloc')",
        "cc_library(name = 'lib',",
        "        srcs = ['libfile.cc'],",
        "        hdrs = ['libfile.h'],",
        "        linkstamp = 'linkstamp.cc',",
        "       )");

    scratch.file("pkg/binfile.cc", "#include \"pkg/libfile.h\"", "int main() { return pkg(); }");
    scratch.file("pkg/libfile.cc", "int pkg() { return 42; }");
    scratch.file("pkg/libfile.h", "int pkg();");
    scratch.file("pkg/linkstamp.cc");
  }

  public void createTestFiles(String extraTestParameters, String extraLibraryParameters)
      throws Exception {
    scratch.file(
        "pkg/BUILD",
        "package(features = ['thin_lto'])",
        "cc_test(",
        "    name = 'bin_test',",
        "    srcs = ['bin_test.cc', ],",
        "    deps = [ ':lib' ], ",
        extraTestParameters,
        "    malloc = '//base:system_malloc'",
        ")",
        "cc_test(",
        "    name = 'bin_test2',",
        "    srcs = ['bin_test2.cc', ],",
        "    deps = [ ':lib' ], ",
        extraTestParameters,
        "    malloc = '//base:system_malloc'",
        ")",
        "cc_library(",
        "    name = 'lib',",
        "    srcs = ['libfile.cc'],",
        "    hdrs = ['libfile.h'],",
        extraLibraryParameters,
        "    linkstamp = 'linkstamp.cc',",
        ")");

    scratch.file("pkg/bin_test.cc", "#include \"pkg/libfile.h\"", "int main() { return pkg(); }");
    scratch.file("pkg/bin_test2.cc", "#include \"pkg/libfile.h\"", "int main() { return pkg(); }");
    scratch.file("pkg/libfile.cc", "int pkg() { return 42; }");
    scratch.file("pkg/libfile.h", "int pkg();");
    scratch.file("pkg/linkstamp.cc");
  }

  @Test
  public void testActionGraph() throws Exception {
    createBuildFiles("bin");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.SUPPORTS_PIC,
                    CppRuleClasses.SUPPORTS_START_END_LIB));
    useConfiguration("--noincompatible_make_thinlto_command_lines_standalone");

    /*
    We follow the chain from the final product backwards.

    binary <=[Link]=
    .lto/...o <=[LTOBackend]=
    {.o.thinlto.bc,.o.imports} <=[LTOIndexing]=
    .o <= [CppCompile] .cc
    */
    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin");

    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(pkgArtifact);
    assertThat(ActionsTestUtil.getFirstArtifactEndingWith(linkAction.getInputs(), "linkstamp.o"))
        .isNotNull();

    List<String> commandLine = linkAction.getLinkCommandLine().getRawLinkArgv();
    String prefix = getTargetConfiguration().getOutputDirectory(RepositoryName.MAIN)
        .getExecPathString();
    assertThat(commandLine)
        .containsAtLeast(
            prefix + "/bin/pkg/bin.lto.merged.o",
            "thinlto_param_file=" + prefix + "/bin/pkg/bin-lto-final.params")
        .inOrder();

    // We have no bitcode files: all files have pkg/bin.lto/
    for (String arg : commandLine) {
      if (arg.contains("_objs") && !arg.contains("linkstamp.o")) {
        assertThat(arg).contains("pkg/bin.lto");
      }
    }

    assertThat(artifactsToStrings(linkAction.getInputs()))
        .containsAtLeast(
            "bin pkg/bin.lto/pkg/_objs/bin/binfile.pic.o",
            "bin pkg/bin.lto/pkg/_objs/lib/libfile.pic.o",
            "bin pkg/bin-2.params",
            "bin pkg/bin-lto-final.params");

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.pic.o");
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");

    assertThat(artifactsToStrings(backendAction.getInputs()))
        .containsAtLeast(
            "bin pkg/bin.lto/pkg/_objs/bin/binfile.pic.o.thinlto.bc",
            "bin pkg/bin.lto/pkg/_objs/bin/binfile.pic.o.imports");

    assertThat(backendAction.getArguments())
        .containsAtLeast(
            "thinlto_index=" + prefix + "/bin/pkg/bin.lto/pkg/_objs/bin/binfile.pic.o.thinlto.bc",
            "thinlto_output_object_file=" + prefix + "/bin/pkg/bin.lto/pkg/_objs/bin/binfile.pic.o",
            "thinlto_input_bitcode_file=" + prefix + "/bin/pkg/_objs/bin/binfile.pic.o");

    CppLinkAction indexAction =
        (CppLinkAction)
            getPredecessorByInputName(
                backendAction, "pkg/bin.lto/pkg/_objs/bin/binfile.pic.o.thinlto.bc");

    ConfiguredTargetValue configuredTargetValue =
        (ConfiguredTargetValue)
            getSkyframeExecutor()
                .getEvaluatorForTesting()
                .getExistingEntryAtCurrentlyEvaluatingVersion(
                    ConfiguredTargetKey.builder()
                        .setLabel(pkg.getLabel())
                        .setConfiguration(getConfiguration(pkg))
                        .build())
                .getValue();
    ImmutableList<ActionAnalysisMetadata> linkstampCompileActions =
        configuredTargetValue
            .getActions()
            .stream()
            .filter(a -> a.getMnemonic().equals("CppLinkstampCompile"))
            .collect(ImmutableList.toImmutableList());
    assertThat(linkstampCompileActions).hasSize(1);
    ActionAnalysisMetadata linkstampCompileAction = linkstampCompileActions.get(0);
    assertThat(indexAction.getInputs().toList())
        .containsNoneIn(linkstampCompileAction.getOutputs());

    assertThat(indexAction.getArguments())
        .containsAtLeast(
            "param_file=" + prefix + "/bin/pkg/bin-lto-final.params",
            "prefix_replace=" + prefix + "/bin;" + prefix + "/bin/pkg/bin.lto",
            "thinlto_merged_object_file=" + prefix + "/bin/pkg/bin.lto.merged.o",
            "object_suffix_replace=.indexing.o;.o");
    assertThat(indexAction.getArguments())
        .doesNotContain("thinlto_param_file=" + prefix + "/bin/pkg/bin-lto-final.params");

    assertThat(artifactsToStrings(indexAction.getOutputs()))
        .containsAtLeast(
            "bin pkg/bin.lto/pkg/_objs/bin/binfile.pic.o.imports",
            "bin pkg/bin.lto/pkg/_objs/bin/binfile.pic.o.thinlto.bc",
            "bin pkg/bin.lto/pkg/_objs/lib/libfile.pic.o.imports",
            "bin pkg/bin.lto/pkg/_objs/lib/libfile.pic.o.thinlto.bc",
            "bin pkg/bin-lto-final.params");

    assertThat(indexAction.getMnemonic()).isEqualTo("CppLTOIndexing");

    assertThat(artifactsToStrings(indexAction.getInputs()))
        .containsAtLeast(
            "bin pkg/_objs/bin/binfile.pic.indexing.o", "bin pkg/_objs/lib/libfile.pic.indexing.o");

    CppCompileAction bitcodeAction =
        (CppCompileAction)
            getPredecessorByInputName(indexAction, "pkg/_objs/bin/binfile.pic.indexing.o");
    assertThat(bitcodeAction.getMnemonic()).isEqualTo("CppCompile");
    assertThat(bitcodeAction.getArguments())
        .contains("lto_indexing_bitcode=" + prefix + "/bin/pkg/_objs/bin/binfile.pic.indexing.o");
  }

  @Test
  public void testLinkshared() throws Exception {
    createBuildFiles("bin.so", "linkshared = 1,");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    CppRuleClasses.SUPPORTS_PIC,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.SUPPORTS_START_END_LIB));
    useConfiguration();

    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin.so");

    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(pkgArtifact);

    Action backendAction =
        getPredecessorByInputName(linkAction, "pkg/bin.so.lto/pkg/_objs/bin.so/binfile.pic.o");
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");
  }

  @Test
  public void testNoLinkstatic() throws Exception {
    createBuildFiles("bin", "linkstatic = 0,");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.SUPPORTS_DYNAMIC_LINKER,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    CppRuleClasses.SUPPORTS_PIC,
                    CppRuleClasses.SUPPORTS_INTERFACE_SHARED_LIBRARIES));
    useConfiguration("--noincompatible_make_thinlto_command_lines_standalone");

    /*
    We follow the chain from the final product backwards to verify intermediate actions.

    binary <=[Link]=
    .ifso <=[SolibSymlink]=
    _S...ifso <=[SolibSymlink]=
    .ifso <=[Link]=
    .lto/...o <=[LTOBackend]=
    {.o.thinlto.bc,.o.imports} <=[LTOIndexing]=
    .o <= [CppCompile] .cc
    */
    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin");

    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(pkgArtifact);

    List<String> commandLine = linkAction.getLinkCommandLine().getRawLinkArgv();
    String prefix = getTargetConfiguration().getOutputDirectory(RepositoryName.MAIN)
        .getExecPathString();

    assertThat(commandLine).contains("-Wl,@" + prefix + "/bin/pkg/bin-lto-final.params");

    // We have no bitcode files: all files have pkg/bin.lto/
    for (String arg : commandLine) {
      if (arg.contains("_objs") && !arg.contains("linkstamp.o")) {
        assertThat(arg).contains("pkg/bin.lto");
      }
    }

    assertThat(artifactsToStrings(linkAction.getInputs()))
        .containsAtLeast(
            "bin pkg/bin.lto/pkg/_objs/bin/binfile.pic.o",
            "bin _solib_k8/libpkg_Sliblib.ifso",
            "bin pkg/bin-2.params",
            "bin pkg/bin-lto-final.params");

    SolibSymlinkAction solibSymlinkAction =
        (SolibSymlinkAction) getPredecessorByInputName(linkAction, "_solib_k8/libpkg_Sliblib.ifso");
    assertThat(solibSymlinkAction.getMnemonic()).isEqualTo("SolibSymlink");

    CppLinkAction libLinkAction =
        (CppLinkAction) getPredecessorByInputName(solibSymlinkAction, "bin/pkg/liblib.ifso");
    assertThat(libLinkAction.getMnemonic()).isEqualTo("CppLink");

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(
                libLinkAction, "pkg/liblib.so.lto/pkg/_objs/lib/libfile.pic.o");
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");

    assertThat(artifactsToStrings(backendAction.getInputs()))
        .contains("bin pkg/liblib.so.lto/pkg/_objs/lib/libfile.pic.o.thinlto.bc");

    assertThat(backendAction.getArguments())
        .containsAtLeast(
            "thinlto_index="
                + prefix
                + "/bin/pkg/liblib.so.lto/pkg/_objs/lib/libfile.pic.o.thinlto.bc",
            "thinlto_output_object_file="
                + prefix
                + "/bin/pkg/liblib.so.lto/pkg/_objs/lib/libfile.pic.o",
            "thinlto_input_bitcode_file=" + prefix + "/bin/pkg/_objs/lib/libfile.pic.o");

    CppLinkAction indexAction =
        (CppLinkAction)
            getPredecessorByInputName(
                backendAction, "pkg/liblib.so.lto/pkg/_objs/lib/libfile.pic.o.thinlto.bc");

    assertThat(indexAction.getArguments())
        .containsAtLeast(
            "param_file=" + prefix + "/bin/pkg/liblib.so-lto-final.params",
            "prefix_replace=" + prefix + "/bin;" + prefix + "/bin/pkg/liblib.so.lto",
            "object_suffix_replace=.indexing.o;.o");

    assertThat(artifactsToStrings(indexAction.getOutputs()))
        .containsAtLeast(
            "bin pkg/liblib.so.lto/pkg/_objs/lib/libfile.pic.o.imports",
            "bin pkg/liblib.so.lto/pkg/_objs/lib/libfile.pic.o.thinlto.bc",
            "bin pkg/liblib.so-lto-final.params");

    assertThat(indexAction.getMnemonic()).isEqualTo("CppLTOIndexing");

    assertThat(artifactsToStrings(indexAction.getInputs()))
        .contains("bin pkg/_objs/lib/libfile.pic.indexing.o");

    CppCompileAction bitcodeAction =
        (CppCompileAction)
            getPredecessorByInputName(indexAction, "pkg/_objs/lib/libfile.pic.indexing.o");
    assertThat(bitcodeAction.getMnemonic()).isEqualTo("CppCompile");
    assertThat(bitcodeAction.getArguments())
        .contains("lto_indexing_bitcode=" + prefix + "/bin/pkg/_objs/lib/libfile.pic.indexing.o");
  }

  /** Helper method to get the root prefix from the given dwpFile. */
  private static PathFragment dwpRootPrefix(Artifact dwpFile) throws Exception {
    return dwpFile
        .getExecPath()
        .subFragment(
            0, dwpFile.getExecPath().segmentCount() - dwpFile.getRootRelativePath().segmentCount());
  }

  /** Helper method that checks that a .dwp has the expected generating action structure. */
  private void validateDwp(
      RuleContext ruleContext,
      Artifact dwpFile,
      CcToolchainProvider toolchain,
      List<String> expectedInputs)
      throws Exception {
    SpawnAction dwpAction = (SpawnAction) getGeneratingAction(dwpFile);
    String dwpToolPath = toolchain.getToolPathFragment(Tool.DWP, ruleContext).getPathString();
    assertThat(dwpAction.getMnemonic()).isEqualTo("CcGenerateDwp");
    assertThat(dwpToolPath).isEqualTo(dwpAction.getCommandFilename());
    List<String> commandArgs = dwpAction.getArguments();
    // The first argument should be the command being executed.
    assertThat(dwpToolPath).isEqualTo(commandArgs.get(0));
    // The final two arguments should be "-o dwpOutputFile".
    assertThat(commandArgs.subList(commandArgs.size() - 2, commandArgs.size()))
        .containsExactly("-o", dwpFile.getExecPathString())
        .inOrder();
    // The remaining arguments should be the set of .dwo inputs (in any order).
    assertThat(commandArgs.subList(1, commandArgs.size() - 2))
        .containsExactlyElementsIn(expectedInputs);
  }

  @Test
  public void testFission() throws Exception {
    createBuildFiles("bin");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    CppRuleClasses.SUPPORTS_PIC,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    CppRuleClasses.PER_OBJECT_DEBUG_INFO));
    useConfiguration("--fission=yes", "--copt=-g0");

    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin");
    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();
    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.pic.o");
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");
    assertThat(artifactsToStrings(backendAction.getOutputs()))
        .containsExactly(
            "bin pkg/bin.lto/pkg/_objs/bin/binfile.pic.o",
            "bin pkg/bin.lto/pkg/_objs/bin/binfile.pic.dwo");

    assertThat(backendAction.getArguments()).containsAtLeast("-g0", "per_object_debug_info_option");

    backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/lib/libfile.pic.o");
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");
    assertThat(artifactsToStrings(backendAction.getOutputs()))
        .containsExactly(
            "bin pkg/bin.lto/pkg/_objs/lib/libfile.pic.o",
            "bin pkg/bin.lto/pkg/_objs/lib/libfile.pic.dwo");

    assertThat(backendAction.getArguments()).contains("per_object_debug_info_option");

    // Now check the dwp action.
    Artifact dwpFile = getFileConfiguredTarget(pkg.getLabel() + ".dwp").getArtifact();
    PathFragment rootPrefix = dwpRootPrefix(dwpFile);
    RuleContext ruleContext = getRuleContext(pkg);
    CcToolchainProvider toolchain =
        CppHelper.getToolchainUsingDefaultCcToolchainAttribute(ruleContext);
    validateDwp(
        ruleContext,
        dwpFile,
        toolchain,
        ImmutableList.of(
            rootPrefix + "/pkg/bin.lto/pkg/_objs/lib/libfile.pic.dwo",
            rootPrefix + "/pkg/bin.lto/pkg/_objs/bin/binfile.pic.dwo"));
  }

  @Test
  public void testNoLinkstaticFission() throws Exception {
    createBuildFiles("bin", "linkstatic = 0,");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.SUPPORTS_PIC,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    CppRuleClasses.SUPPORTS_INTERFACE_SHARED_LIBRARIES,
                    CppRuleClasses.SUPPORTS_DYNAMIC_LINKER,
                    CppRuleClasses.PER_OBJECT_DEBUG_INFO));
    useConfiguration("--fission=yes");

    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin");
    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();
    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);

    SolibSymlinkAction solibSymlinkAction =
        (SolibSymlinkAction) getPredecessorByInputName(linkAction, "_solib_k8/libpkg_Sliblib.ifso");
    assertThat(solibSymlinkAction.getMnemonic()).isEqualTo("SolibSymlink");

    CppLinkAction libLinkAction =
        (CppLinkAction) getPredecessorByInputName(solibSymlinkAction, "bin/pkg/liblib.ifso");
    assertThat(libLinkAction.getMnemonic()).isEqualTo("CppLink");

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(
                libLinkAction, "pkg/liblib.so.lto/pkg/_objs/lib/libfile.pic.o");
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");
    assertThat(artifactsToStrings(backendAction.getOutputs()))
        .containsExactly(
            "bin pkg/liblib.so.lto/pkg/_objs/lib/libfile.pic.o",
            "bin pkg/liblib.so.lto/pkg/_objs/lib/libfile.pic.dwo");

    assertThat(backendAction.getArguments()).contains("per_object_debug_info_option");

    // Check the dwp action.
    Artifact dwpFile = getFileConfiguredTarget(pkg.getLabel() + ".dwp").getArtifact();
    PathFragment rootPrefix = dwpRootPrefix(dwpFile);
    RuleContext ruleContext = getRuleContext(pkg);
    CcToolchainProvider toolchain =
        CppHelper.getToolchainUsingDefaultCcToolchainAttribute(ruleContext);
    validateDwp(
        ruleContext,
        dwpFile,
        toolchain,
        ImmutableList.of(rootPrefix + "/pkg/bin.lto/pkg/_objs/bin/binfile.pic.dwo"));
  }

  @Test
  public void testLinkstaticCcTestFission() throws Exception {
    createTestFiles("linkstatic = 1,", "");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    CppRuleClasses.SUPPORTS_PIC,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    CppRuleClasses.THIN_LTO_LINKSTATIC_TESTS_USE_SHARED_NONLTO_BACKENDS,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.PER_OBJECT_DEBUG_INFO));
    useConfiguration(
        "--fission=yes", "--features=thin_lto_linkstatic_tests_use_shared_nonlto_backends");

    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin_test");
    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();
    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);

    // All backends should be shared non-LTO in this case
    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(
                linkAction, "shared.nonlto/pkg/_objs/bin_test/bin_test.pic.o");
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");
    assertThat(artifactsToStrings(backendAction.getOutputs()))
        .containsExactly(
            "bin shared.nonlto/pkg/_objs/bin_test/bin_test.pic.o",
            "bin shared.nonlto/pkg/_objs/bin_test/bin_test.pic.dwo");

    assertThat(backendAction.getArguments()).contains("per_object_debug_info_option");

    backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "shared.nonlto/pkg/_objs/lib/libfile.pic.o");
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");
    assertThat(backendAction.getArguments()).contains("-fPIC");
    assertThat(artifactsToStrings(backendAction.getOutputs()))
        .containsExactly(
            "bin shared.nonlto/pkg/_objs/lib/libfile.pic.o",
            "bin shared.nonlto/pkg/_objs/lib/libfile.pic.dwo");

    assertThat(backendAction.getArguments()).contains("per_object_debug_info_option");

    // Now check the dwp action.
    Artifact dwpFile = getFileConfiguredTarget(pkg.getLabel() + ".dwp").getArtifact();
    PathFragment rootPrefix = dwpRootPrefix(dwpFile);
    RuleContext ruleContext = getRuleContext(pkg);
    CcToolchainProvider toolchain =
        CppHelper.getToolchainUsingDefaultCcToolchainAttribute(ruleContext);
    validateDwp(
        ruleContext,
        dwpFile,
        toolchain,
        ImmutableList.of(
            rootPrefix + "/shared.nonlto/pkg/_objs/lib/libfile.pic.dwo",
            rootPrefix + "/shared.nonlto/pkg/_objs/bin_test/bin_test.pic.dwo"));
  }

  @Test
  public void testLinkstaticCcTest() throws Exception {
    createTestFiles("linkstatic = 1,", "");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    CppRuleClasses.SUPPORTS_PIC,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    CppRuleClasses.THIN_LTO_LINKSTATIC_TESTS_USE_SHARED_NONLTO_BACKENDS,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.PER_OBJECT_DEBUG_INFO));
    useConfiguration("--features=thin_lto_linkstatic_tests_use_shared_nonlto_backends");

    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin_test");
    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();
    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);

    ConfiguredTarget pkg2 = getConfiguredTarget("//pkg:bin_test2");
    Artifact pkgArtifact2 = getFilesToBuild(pkg2).getSingleton();
    CppLinkAction linkAction2 = (CppLinkAction) getGeneratingAction(pkgArtifact2);

    // All backends should be shared non-LTO in this case
    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(
                linkAction, "shared.nonlto/pkg/_objs/bin_test/bin_test.pic.o");
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");

    backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "shared.nonlto/pkg/_objs/lib/libfile.pic.o");
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");
    assertThat(backendAction.getArguments()).contains("-fPIC");

    LtoBackendAction backendAction2 =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction2, "shared.nonlto/pkg/_objs/lib/libfile.pic.o");
    assertThat(backendAction2.getMnemonic()).isEqualTo("CcLtoBackendCompile");

    assertThat(backendAction).isEqualTo(backendAction2);
  }

  @Test
  public void testTestOnlyTarget() throws Exception {
    createBuildFiles("bin", "testonly = 1,");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.SUPPORTS_PIC,
                    CppRuleClasses.THIN_LTO,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    CppRuleClasses.THIN_LTO_LINKSTATIC_TESTS_USE_SHARED_NONLTO_BACKENDS,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES));
    useConfiguration("--features=thin_lto_linkstatic_tests_use_shared_nonlto_backends");

    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin");
    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();
    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "shared.nonlto/pkg/_objs/bin/binfile.pic.o");
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");
  }

  @Test
  public void testUseSharedAllLinkstatic() throws Exception {
    createBuildFiles("bin");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    CppRuleClasses.THIN_LTO_ALL_LINKSTATIC_USE_SHARED_NONLTO_BACKENDS,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    CppRuleClasses.SUPPORTS_PIC,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES));
    useConfiguration("--features=thin_lto_all_linkstatic_use_shared_nonlto_backends");

    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin");
    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();
    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "shared.nonlto/pkg/_objs/bin/binfile.pic.o");
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");
  }

  private Action getPredecessorByInputName(Action action, String str) {
    for (Artifact a : action.getInputs().toList()) {
      if (a.getExecPathString().contains(str)) {
        return getGeneratingAction(a);
      }
    }
    return null;
  }

  @Test
  public void testAssemblerSource() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "package(features = ['thin_lto'])",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ],",
        "          deps = [ ':lib' ], ",
        "          malloc = '//base:system_malloc')",
        "cc_library(name = 'lib',",
        "        srcs = ['tracing.cc', 'tracing_x86-64.S'],",
        "       )");

    scratch.file("pkg/binfile.cc", "int main() { return pkg(); }");
    scratch.file("pkg/tracing.cc", "// hello");
    scratch.file("pkg/tracing_x86-64.S", "NOP");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.SUPPORTS_START_END_LIB));
    useConfiguration();

    ConfiguredTarget bin = getConfiguredTarget("//pkg:bin");

    Artifact binArtifact = getFilesToBuild(bin).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    Action dataGen = getPredecessorByInputName(linkAction, "tracing_x86-64");
    assertWithMessage(linkAction.getInputs().toString()).that(dataGen).isNotNull();
    assertThat(dataGen.getMnemonic()).isEqualTo("CppCompile");
  }

  // Make sure we don't choke on a cc_library without sources and therefore, without bitcode files.
  @Test
  public void testNoSourceFiles() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "package(features = ['thin_lto'])",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ],",
        "          deps = [ ':lib' ], ",
        "          malloc = '//base:system_malloc')",
        "cc_library(name = 'lib',",
        "        srcs = ['static.a'],",
        "       )");

    scratch.file("pkg/binfile.cc", "int main() { return 1; }");
    scratch.file("pkg/static.a", "xyz");
    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.SUPPORTS_START_END_LIB));
    useConfiguration();

    getConfiguredTarget("//pkg:bin");
  }

  @Test
  public void testFdoInstrument() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "package(features = ['thin_lto'])",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ],",
        "          malloc = '//base:system_malloc')");

    scratch.file("pkg/binfile.cc", "int main() {}");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.SUPPORTS_PIC,
                    CppRuleClasses.FDO_INSTRUMENT));
    useConfiguration("--fdo_instrument=profiles");

    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin");

    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(pkgArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.pic.o");
    // If the LtoBackendAction incorrectly tries to add the fdo_instrument
    // feature, we will fail with an "unknown variable 'fdo_instrument_path'"
    // error. But let's also explicitly confirm that the fdo_instrument
    // option didn't end up here.
    assertThat(backendAction.getArguments()).doesNotContain("fdo_instrument_option");
  }

  @Test
  public void testLtoIndexOpt() throws Exception {
    createBuildFiles("bin");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.SUPPORTS_PIC,
                    CppRuleClasses.SUPPORTS_START_END_LIB));
    useConfiguration(
        "--ltoindexopt=anltoindexopt", "--noincompatible_make_thinlto_command_lines_standalone");

    /*
    We follow the chain from the final product backwards.

    binary <=[Link]=
    .lto/...o <=[LTOBackend]=
    {.o.thinlto.bc,.o.imports} <=[LTOIndexing]=
    .o <= [CppCompile] .cc
    */
    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin");

    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(pkgArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.pic.o");
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");

    CppLinkAction indexAction =
        (CppLinkAction)
            getPredecessorByInputName(
                backendAction, "pkg/bin.lto/pkg/_objs/bin/binfile.pic.o.thinlto.bc");

    assertThat(indexAction.getArguments()).contains("anltoindexopt");
  }

  @Test
  public void testLtoStandaloneCommandLines() throws Exception {
    createBuildFiles("bin");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.SUPPORTS_PIC,
                    CppRuleClasses.SUPPORTS_START_END_LIB));
    useConfiguration(
        "--ltoindexopt=anltoindexopt",
        "--incompatible_make_thinlto_command_lines_standalone",
        "--features=thin_lto");

    /*
    We follow the chain from the final product backwards.

    binary <=[Link]=
    .lto/...o <=[LTOBackend]=
    {.o.thinlto.bc,.o.imports} <=[LTOIndexing]=
    .o <= [CppCompile] .cc
    */
    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin");

    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(pkgArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.pic.o");
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");

    CppLinkAction indexAction =
        (CppLinkAction)
            getPredecessorByInputName(
                backendAction, "pkg/bin.lto/pkg/_objs/bin/binfile.pic.o.thinlto.bc");

    assertThat(indexAction.getArguments())
        .contains("--i_come_from_standalone_lto_index=anltoindexopt");
  }

  @Test
  public void testCopt() throws Exception {
    createBuildFiles("bin");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    CppRuleClasses.SUPPORTS_PIC));
    useConfiguration("--copt=acopt");

    /*
    We follow the chain from the final product backwards.

    binary <=[Link]=
    .lto/...o <=[LTOBackend]=
    */
    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin");

    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(pkgArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.pic.o");
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");
    assertThat(backendAction.getArguments()).contains("acopt");
  }

  @Test
  public void testPerFileCopt() throws Exception {
    createBuildFiles("bin");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.SUPPORTS_PIC,
                    CppRuleClasses.SUPPORTS_START_END_LIB));
    useConfiguration(
        "--per_file_copt=binfile\\.cc@copt1",
        "--per_file_copt=libfile\\.cc@copt2",
        "--per_file_copt=.*\\.cc,-binfile\\.cc@copt2");

    /*
    We follow the chain from the final product backwards.

    binary <=[Link]=
    .lto/...o <=[LTOBackend]=
    */
    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin");
    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(pkgArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.pic.o");
    assertThat(backendAction.getArguments()).contains("copt1");
    assertThat(backendAction.getArguments()).doesNotContain("copt2");

    backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/lib/libfile.pic.o");
    assertThat(backendAction.getArguments()).doesNotContain("copt1");
    assertThat(backendAction.getArguments()).contains("copt2");
  }

  @Test
  public void testCoptNoCoptAttributes() throws Exception {
    createBuildFiles("bin", "copts = ['acopt', 'nocopt1'], nocopts = 'nocopt1|nocopt2',");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.SUPPORTS_PIC,
                    CppRuleClasses.SUPPORTS_START_END_LIB));
    useConfiguration("--copt=nocopt2", "--noincompatible_disable_nocopts");

    /*
    We follow the chain from the final product backwards.

    binary <=[Link]=
    .lto/...o <=[LTOBackend]=
    */
    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin");

    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(pkgArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.pic.o");
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");
    assertThat(backendAction.getArguments()).contains("acopt");
    // TODO(b/122303926): Remove when nocopts are removed, or uncomment and fix if not removing.
    // assertThat(backendAction.getArguments()).doesNotContain("nocopt1");
    // assertThat(backendAction.getArguments()).doesNotContain("nocopt2");
  }

  @Test
  public void testLtoBackendOpt() throws Exception {
    createBuildFiles("bin");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    CppRuleClasses.SUPPORTS_PIC,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    MockCcSupport.USER_COMPILE_FLAGS));
    useConfiguration("--ltobackendopt=anltobackendopt");

    /*
    We follow the chain from the final product backwards.

    binary <=[Link]=
    .lto/...o <=[LTOBackend]=
    */
    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin");

    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(pkgArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.pic.o");
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");
    assertThat(backendAction.getArguments())
        .containsAtLeast("--default-compile-flag", "anltobackendopt");
  }

  @Test
  public void testPerFileLtoBackendOpt() throws Exception {
    createBuildFiles("bin");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.SUPPORTS_PIC,
                    CppRuleClasses.SUPPORTS_START_END_LIB));
    useConfiguration(
        "--per_file_ltobackendopt=binfile\\.pic\\.o@ltobackendopt1",
        "--per_file_ltobackendopt=.*\\.o,-binfile\\.pic\\.o@ltobackendopt2");

    /*
    We follow the chain from the final product backwards.

    binary <=[Link]=
    .lto/...o <=[LTOBackend]=
    */
    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin");
    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(pkgArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.pic.o");
    assertThat(backendAction.getArguments()).contains("ltobackendopt1");
    assertThat(backendAction.getArguments()).doesNotContain("ltobackendopt2");

    backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/lib/libfile.pic.o");
    assertThat(backendAction.getArguments()).doesNotContain("ltobackendopt1");
    assertThat(backendAction.getArguments()).contains("ltobackendopt2");
  }

  @Test
  public void testNoUseLtoIndexingBitcodeFile() throws Exception {
    createBuildFiles("bin");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    CppRuleClasses.NO_USE_LTO_INDEXING_BITCODE_FILE,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.SUPPORTS_PIC,
                    CppRuleClasses.SUPPORTS_START_END_LIB));
    useConfiguration("--features=no_use_lto_indexing_bitcode_file");

    /*
    We follow the chain from the final product backwards.

    binary <=[Link]=
    .lto/...o <=[LTOBackend]=
    {.o.thinlto.bc,.o.imports} <=[LTOIndexing]=
    .o <= [CppCompile] .cc
    */
    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin");

    Artifact pkgArtifact = getFilesToBuild(pkg).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(pkgArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.pic.o");

    CppLinkAction indexAction =
        (CppLinkAction)
            getPredecessorByInputName(
                backendAction, "pkg/bin.lto/pkg/_objs/bin/binfile.pic.o.thinlto.bc");

    assertThat(indexAction.getArguments()).doesNotContain("object_suffix_replace");

    assertThat(artifactsToStrings(indexAction.getInputs()))
        .containsAtLeast("bin pkg/_objs/bin/binfile.pic.o", "bin pkg/_objs/lib/libfile.pic.o");

    CppCompileAction bitcodeAction =
        (CppCompileAction) getPredecessorByInputName(indexAction, "pkg/_objs/bin/binfile.pic.o");
    assertThat(bitcodeAction.getArguments()).doesNotContain("lto_indexing_bitcode=");
  }

  @Test
  public void testAutoFdo() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "package(features = ['thin_lto'])",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ],",
        "          malloc = '//base:system_malloc')");

    scratch.file("pkg/binfile.cc", "int main() {}");
    scratch.file("pkg/profile.afdo", "");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.AUTOFDO));
    useConfiguration("--fdo_optimize=pkg/profile.afdo", "--compilation_mode=opt");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");

    // Checks that -fauto-profile is added to the LtoBackendAction.
    assertThat(Joiner.on(" ").join(backendAction.getArguments())).containsMatch(
        "-fauto-profile=[^ ]*/profile.afdo");
    assertThat(ActionsTestUtil.baseArtifactNames(backendAction.getInputs())).contains(
        "profile.afdo");
  }

  private void setupAutoFdoThinLtoCrosstool() throws Exception {
    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.AUTOFDO,
                    CppRuleClasses.ENABLE_AFDO_THINLTO,
                    CppRuleClasses.AUTOFDO_IMPLICIT_THINLTO));
  }

  /**
   * Tests that ThinLTO is not enabled for AFDO with LLVM without
   * --features=autofdo_implicit_thinlto.
   */
  @Test
  public void testAutoFdoNoImplicitThinLto() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ],",
        "          malloc = '//base:system_malloc')");

    scratch.file("pkg/binfile.cc", "int main() {}");
    scratch.file("pkg/profile.afdo", "");

    setupAutoFdoThinLtoCrosstool();
    useConfiguration("--fdo_optimize=pkg/profile.afdo", "--compilation_mode=opt");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");
    // We should not have a ThinLTO backend action
    assertThat(backendAction).isNull();
  }

  /** Tests that --features=autofdo_implicit_thinlto enables ThinLTO for AFDO with LLVM. */
  @Test
  public void testAutoFdoImplicitThinLto() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ],",
        "          malloc = '//base:system_malloc')");

    scratch.file("pkg/binfile.cc", "int main() {}");
    scratch.file("pkg/profile.afdo", "");

    setupAutoFdoThinLtoCrosstool();
    useConfiguration(
        "--fdo_optimize=pkg/profile.afdo",
        "--compilation_mode=opt",
        "--features=autofdo_implicit_thinlto");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");
    // For ThinLTO compilation we should have a non-null backend action
    assertThat(backendAction).isNotNull();
  }

  /**
   * Tests that --features=-thin_lto overrides --features=autofdo_implicit_thinlto and prevents
   * enabling ThinLTO for AFDO with LLVM.
   */
  @Test
  public void testAutoFdoImplicitThinLtoDisabledOption() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ],",
        "          malloc = '//base:system_malloc')");

    scratch.file("pkg/binfile.cc", "int main() {}");
    scratch.file("pkg/profile.afdo", "");

    setupAutoFdoThinLtoCrosstool();
    useConfiguration(
        "--fdo_optimize=pkg/profile.afdo",
        "--compilation_mode=opt",
        "--features=autofdo_implicit_thinlto",
        "--features=-thin_lto");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");
    // We should not have a ThinLTO backend action
    assertThat(backendAction).isNull();
  }

  /**
   * Tests that features=[-thin_lto] in the build rule overrides --features=autofdo_implicit_thinlto
   * and prevents enabling ThinLTO for AFDO with LLVM.
   */
  @Test
  public void testAutoFdoImplicitThinLtoDisabledRule() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ],",
        "          features = ['-thin_lto'],",
        "          malloc = '//base:system_malloc')");

    scratch.file("pkg/binfile.cc", "int main() {}");
    scratch.file("pkg/profile.afdo", "");

    setupAutoFdoThinLtoCrosstool();
    useConfiguration(
        "--fdo_optimize=pkg/profile.afdo",
        "--compilation_mode=opt",
        "--features=autofdo_implicit_thinlto");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");
    // We should not have a ThinLTO backend action
    assertThat(backendAction).isNull();
  }

  /**
   * Tests that features=[-thin_lto] in the package overrides --features=autofdo_implicit_thinlto
   * and prevents enabling ThinLTO for AFDO with LLVM.
   */
  @Test
  public void testAutoFdoImplicitThinLtoDisabledPackage() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "package(features = ['-thin_lto'])",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ],",
        "          malloc = '//base:system_malloc')");

    scratch.file("pkg/binfile.cc", "int main() {}");
    scratch.file("pkg/profile.afdo", "");

    setupAutoFdoThinLtoCrosstool();
    useConfiguration(
        "--fdo_optimize=pkg/profile.afdo",
        "--compilation_mode=opt",
        "--features=autofdo_implicit_thinlto");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");
    // We should not have a ThinLTO backend action
    assertThat(backendAction).isNull();
  }

  private void setupFdoThinLtoCrosstool() throws Exception {
    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.FDO_OPTIMIZE,
                    CppRuleClasses.ENABLE_FDO_THINLTO,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    MockCcSupport.FDO_IMPLICIT_THINLTO));
  }

  /**
   * Tests that ThinLTO is not enabled for FDO with LLVM without --features=fdo_implicit_thinlto.
   */
  @Test
  public void testFdoNoImplicitThinLto() throws Exception {
    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(CppRuleClasses.THIN_LTO, CppRuleClasses.SUPPORTS_START_END_LIB));
    scratch.file(
        "pkg/BUILD",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ],",
        "          malloc = '//base:system_malloc')");

    scratch.file("pkg/binfile.cc", "int main() {}");
    scratch.file("pkg/profile.zip", "");

    setupFdoThinLtoCrosstool();
    useConfiguration("--fdo_optimize=pkg/profile.zip", "--compilation_mode=opt");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");
    // We should not have a ThinLTO backend action
    assertThat(backendAction).isNull();
  }

  /** Tests that --features=fdo_implicit_thinlto enables ThinLTO for FDO with LLVM. */
  @Test
  public void testFdoImplicitThinLto() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ],",
        "          malloc = '//base:system_malloc')");

    scratch.file("pkg/binfile.cc", "int main() {}");
    scratch.file("pkg/profile.zip", "");

    setupFdoThinLtoCrosstool();
    useConfiguration(
        "--fdo_optimize=pkg/profile.zip",
        "--compilation_mode=opt",
        "--features=fdo_implicit_thinlto");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");
    // For ThinLTO compilation we should have a non-null backend action
    assertThat(backendAction).isNotNull();
  }

  /**
   * Tests that --features=-thin_lto overrides --features=fdo_implicit_thinlto and prevents enabling
   * ThinLTO for FDO with LLVM.
   */
  @Test
  public void testFdoImplicitThinLtoDisabledOption() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ],",
        "          malloc = '//base:system_malloc')");

    scratch.file("pkg/binfile.cc", "int main() {}");
    scratch.file("pkg/profile.zip", "");

    setupFdoThinLtoCrosstool();
    useConfiguration(
        "--fdo_optimize=pkg/profile.zip",
        "--compilation_mode=opt",
        "--features=fdo_implicit_thinlto",
        "--features=-thin_lto");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");
    // We should not have a ThinLTO backend action
    assertThat(backendAction).isNull();
  }

  /**
   * Tests that features=[-thin_lto] in the build rule overrides --features=fdo_implicit_thinlto and
   * prevents enabling ThinLTO for FDO with LLVM.
   */
  @Test
  public void testFdoImplicitThinLtoDisabledRule() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ],",
        "          features = ['-thin_lto'],",
        "          malloc = '//base:system_malloc')");

    scratch.file("pkg/binfile.cc", "int main() {}");
    scratch.file("pkg/profile.zip", "");

    setupFdoThinLtoCrosstool();
    useConfiguration(
        "--fdo_optimize=pkg/profile.zip",
        "--compilation_mode=opt",
        "--features=fdo_implicit_thinlto");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");
    // We should not have a ThinLTO backend action
    assertThat(backendAction).isNull();
  }

  /**
   * Tests that features=[-thin_lto] in the package overrides --features=fdo_implicit_thinlto and
   * prevents enabling ThinLTO for FDO with LLVM.
   */
  @Test
  public void testFdoImplicitThinLtoDisabledPackage() throws Exception {
    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(CppRuleClasses.THIN_LTO, CppRuleClasses.SUPPORTS_START_END_LIB));
    scratch.file(
        "pkg/BUILD",
        "package(features = ['-thin_lto'])",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ],",
        "          malloc = '//base:system_malloc')");

    scratch.file("pkg/binfile.cc", "int main() {}");
    scratch.file("pkg/profile.zip", "");

    setupFdoThinLtoCrosstool();
    useConfiguration(
        "--fdo_optimize=pkg/profile.zip",
        "--compilation_mode=opt",
        "--features=fdo_implicit_thinlto");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");
    // We should not have a ThinLTO backend action
    assertThat(backendAction).isNull();
  }

  private void setupXBinaryFdoThinLtoCrosstool() throws Exception {
    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.XBINARYFDO,
                    CppRuleClasses.ENABLE_XFDO_THINLTO,
                    MockCcSupport.XFDO_IMPLICIT_THINLTO));
  }

  /**
   * Tests that ThinLTO is not enabled for XFDO with LLVM without
   * --features=xbinaryfdo_implicit_thinlto.
   */
  @Test
  public void testXBinaryFdoNoImplicitThinLto() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ])",
        "fdo_profile(name='out.xfdo', profile='profiles.xfdo')");

    scratch.file("pkg/binfile.cc", "int main() {}");

    setupXBinaryFdoThinLtoCrosstool();
    useConfiguration("--xbinary_fdo=//pkg:out.xfdo", "--compilation_mode=opt");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");
    // We should not have a ThinLTO backend action
    assertThat(backendAction).isNull();
  }

  /** Tests that --features=xbinaryfdo_implicit_thinlto enables ThinLTO for XFDO with LLVM. */
  @Test
  public void testXBinaryFdoImplicitThinLto() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ])",
        "fdo_profile(name='out.xfdo', profile='profiles.xfdo')");

    scratch.file("pkg/binfile.cc", "int main() {}");

    setupXBinaryFdoThinLtoCrosstool();
    useConfiguration(
        "--xbinary_fdo=//pkg:out.xfdo",
        "--compilation_mode=opt",
        "--features=xbinaryfdo_implicit_thinlto");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");
    // For ThinLTO compilation we should have a non-null backend action
    assertThat(backendAction).isNotNull();
  }

  /**
   * Tests that --features=-thin_lto overrides --features=xbinaryfdo_implicit_thinlto and prevents
   * enabling ThinLTO for XFDO with LLVM.
   */
  @Test
  public void testXBinaryFdoImplicitThinLtoDisabledOption() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ])",
        "fdo_profile(name='out.xfdo', profile='profiles.xfdo')");

    scratch.file("pkg/binfile.cc", "int main() {}");

    setupXBinaryFdoThinLtoCrosstool();
    useConfiguration(
        "--xbinary_fdo=//pkg:out.xfdo",
        "--compilation_mode=opt",
        "--features=xbinaryfdo_implicit_thinlto",
        "--features=-thin_lto");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");
    // We should not have a ThinLTO backend action
    assertThat(backendAction).isNull();
  }

  /**
   * Tests that features=[-thin_lto] in the build rule overrides
   * --features=xbinaryfdo_implicit_thinlto and prevents enabling ThinLTO for XFDO with LLVM.
   */
  @Test
  public void testXBinaryFdoImplicitThinLtoDisabledRule() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ],",
        "          features = ['-thin_lto'])",
        "fdo_profile(name='out.xfdo', profile='profiles.xfdo')");

    scratch.file("pkg/binfile.cc", "int main() {}");

    setupXBinaryFdoThinLtoCrosstool();
    useConfiguration(
        "--xbinary_fdo=//pkg:out.xfdo",
        "--compilation_mode=opt",
        "--features=xbinaryfdo_implicit_thinlto");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");
    // We should not have a ThinLTO backend action
    assertThat(backendAction).isNull();
  }

  /**
   * Tests that features=[-thin_lto] in the package overrides --features=fdo_implicit_thinlto and
   * prevents enabling ThinLTO for XFDO with LLVM.
   */
  @Test
  public void testXBinaryFdoImplicitThinLtoDisabledPackage() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "package(features = ['-thin_lto'])",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ])",
        "fdo_profile(name='out.xfdo', profile='profiles.xfdo')");

    scratch.file("pkg/binfile.cc", "int main() {}");

    setupXBinaryFdoThinLtoCrosstool();
    useConfiguration(
        "--xbinary_fdo=//pkg:out.xfdo",
        "--compilation_mode=opt",
        "--features=xbinaryfdo_implicit_thinlto");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");
    // We should not have a ThinLTO backend action
    assertThat(backendAction).isNull();
  }

  @Test
  public void testXBinaryFdo() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "package(features = ['thin_lto'])",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ],",
        "          malloc = '//base:system_malloc')",
        "fdo_profile(name='out.xfdo', profile='profiles.xfdo')");

    scratch.file("pkg/binfile.cc", "int main() {}");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.XBINARYFDO));
    useConfiguration("--xbinary_fdo=//pkg:out.xfdo", "--compilation_mode=opt");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");

    // Checks that -fauto-profile is added to the LtoBackendAction.
    assertThat(Joiner.on(" ").join(backendAction.getArguments()))
        .containsMatch("-fauto-profile=[^ ]*/profiles.xfdo");
    assertThat(ActionsTestUtil.baseArtifactNames(backendAction.getInputs()))
        .contains("profiles.xfdo");
  }

  /**
   * Tests that ThinLTO is not enabled for XBINARYFDO with --features=autofdo_implicit_thinlto and
   * --features=fdo_implicit_thinlto.
   */
  @Test
  public void testXBinaryFdoNoAutoFdoOrFdoImplicitThinLto() throws Exception {
    scratch.file(
        "pkg/BUILD",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ],",
        "          malloc = '//base:system_malloc')",
        "fdo_profile(name='out.xfdo', profile='profiles.xfdo')");

    scratch.file("pkg/binfile.cc", "int main() {}");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.ENABLE_FDO_THINLTO,
                    MockCcSupport.FDO_IMPLICIT_THINLTO,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    CppRuleClasses.ENABLE_AFDO_THINLTO,
                    MockCcSupport.AUTOFDO_IMPLICIT_THINLTO,
                    CppRuleClasses.XBINARYFDO));
    useConfiguration(
        "--xbinary_fdo=//pkg:out.xfdo",
        "--compilation_mode=opt",
        "--features=autofdo_implicit_thinlto",
        "--features=fdo_implicit_thinlto");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/pkg/binfile.o");
    // We should not have a ThinLTO backend action
    assertThat(backendAction).isNull();
  }

  @Test
  public void testPICBackendOrder() throws Exception {
    createBuildFiles("bin");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.SUPPORTS_PIC,
                    CppRuleClasses.SUPPORTS_START_END_LIB));
    useConfiguration("--copt=-fno-PIE");

    ConfiguredTarget pkg = getConfiguredTarget("//pkg:bin");
    LtoBackendAction backendAction =
        (LtoBackendAction)
        getGeneratingAction(artifactByPath(getFilesToBuild(pkg), "bin", "binfile.pic.o"));
    assertThat(backendAction.getMnemonic()).isEqualTo("CcLtoBackendCompile");
    assertThat(backendAction.getArguments()).containsAtLeast("-fno-PIE", "-fPIC").inOrder();
  }

  private void testPropellerOptimizeOption(boolean label) throws Exception {
    scratch.file(
        "pkg/BUILD",
        "package(features = ['thin_lto'])",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ])");

    if (label) {
      scratch.file(
          "fdo/BUILD",
          "propeller_optimize(name='test_propeller_optimize', cc_profile=':cc_profile.txt',"
              + " ld_profile=':ld_profile.txt')");
    } else {
      scratch.file(
          "fdo/BUILD",
          "propeller_optimize(name='test_propeller_optimize',"
              + "absolute_cc_profile='/tmp/cc_profile.txt',"
              + "absolute_ld_profile='/tmp/ld_profile.txt')");
    }

    scratch.file("pkg/binfile.cc", "int main() {}");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    CppRuleClasses.SUPPORTS_PIC,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.AUTOFDO));

    useConfiguration(
        "--propeller_optimize=//fdo:test_propeller_optimize", "--compilation_mode=opt");

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    List<String> commandLine = linkAction.getLinkCommandLine().getRawLinkArgv();
    assertThat(commandLine.toString())
        .containsMatch("-Wl,--symbol-ordering-file=.*/ld_profile.txt");

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");

    String expectedCompilerFlag = "-fbasic-block-sections=list=.*/cc_profile.txt";
    assertThat(Joiner.on(" ").join(backendAction.getArguments()))
        .containsMatch(expectedCompilerFlag);
    assertThat(ActionsTestUtil.baseArtifactNames(backendAction.getInputs()))
        .contains("cc_profile.txt");
  }

  @Test
  public void testPropellerOptimizeOptionFromAbsolutePath() throws Exception {
    testPropellerOptimizeOption(false);
  }

  @Test
  public void testPropellerOptimizeOptionFromLabel() throws Exception {
    testPropellerOptimizeOption(true);
  }

  private void testLLVMCachePrefetchBackendOption(String extraOption, boolean asLabel)
      throws Exception {
    scratch.file(
        "pkg/BUILD",
        "package(features = ['thin_lto'])",
        "",
        "cc_binary(name = 'bin',",
        "          srcs = ['binfile.cc', ])");
     if (asLabel) {
      scratch.file(
          "fdo/BUILD",
          "fdo_prefetch_hints(name='test_profile', profile=':prefetch.afdo')");
    } else {
      scratch.file(
          "fdo/BUILD",
          "fdo_prefetch_hints(name='test_profile', absolute_path_profile='/tmp/prefetch.afdo')");
    }

    scratch.file("pkg/binfile.cc", "int main() {}");

    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig,
            CcToolchainConfig.builder()
                .withFeatures(
                    CppRuleClasses.THIN_LTO,
                    CppRuleClasses.SUPPORTS_START_END_LIB,
                    CppRuleClasses.SUPPORTS_PIC,
                    MockCcSupport.HOST_AND_NONHOST_CONFIGURATION_FEATURES,
                    CppRuleClasses.AUTOFDO));
    useConfiguration(
        "--fdo_prefetch_hints=//fdo:test_profile",
        "--compilation_mode=opt",
        extraOption);

    Artifact binArtifact = getFilesToBuild(getConfiguredTarget("//pkg:bin")).getSingleton();

    CppLinkAction linkAction = (CppLinkAction) getGeneratingAction(binArtifact);
    assertThat(linkAction.getOutputs()).containsExactly(binArtifact);

    LtoBackendAction backendAction =
        (LtoBackendAction)
            getPredecessorByInputName(linkAction, "pkg/bin.lto/pkg/_objs/bin/binfile.o");

    String expectedCompilerFlag =
        "-prefetch-hints-file="
            + (asLabel ? ".*/prefetch.afdo" : "(blaze|bazel)-out/.*/fdo/.*/prefetch.afdo");
    assertThat(Joiner.on(" ").join(backendAction.getArguments()))
        .containsMatch("-mllvm " + expectedCompilerFlag);

    assertThat(ActionsTestUtil.baseArtifactNames(backendAction.getInputs()))
        .contains("prefetch.afdo");
  }

  @Test
  public void testFdoCachePrefetchLLVMOptionsToBackendFromPath() throws Exception {
    testLLVMCachePrefetchBackendOption("", false);
  }

  @Test
  public void testFdoCachePrefetchAndFdoLLVMOptionsToBackendFromPath() throws Exception {
    testLLVMCachePrefetchBackendOption("--fdo_optimize=./profile.zip", false);
  }

  @Test
  public void testFdoCachePrefetchLLVMOptionsToBackendFromLabel() throws Exception {
    testLLVMCachePrefetchBackendOption("", true);
  }

  @Test
  public void testFdoCachePrefetchAndFdoLLVMOptionsToBackendFromLabel() throws Exception {
    testLLVMCachePrefetchBackendOption("--fdo_optimize=./profile.zip", true);
  }

  @Test
  public void testThinLtoWithoutSupportsStartEndLibError() throws Exception {
    createBuildFiles("bin", "testonly = 1,");
    AnalysisMock.get()
        .ccSupport()
        .setupCcToolchainConfig(
            mockToolsConfig, CcToolchainConfig.builder().withFeatures(CppRuleClasses.THIN_LTO));
    checkError("//pkg:bin", "The feature supports_start_end_lib must be enabled.");
  }
}
