// Copyright 2018 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.skydoc.fakebuildapi.cpp;

import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.starlarkbuildapi.FileApi;
import com.google.devtools.build.lib.starlarkbuildapi.StarlarkActionFactoryApi;
import com.google.devtools.build.lib.starlarkbuildapi.StarlarkRuleContextApi;
import com.google.devtools.build.lib.starlarkbuildapi.core.ProviderApi;
import com.google.devtools.build.lib.starlarkbuildapi.cpp.BazelCcModuleApi;
import com.google.devtools.build.lib.starlarkbuildapi.cpp.CcCompilationContextApi;
import com.google.devtools.build.lib.starlarkbuildapi.cpp.CcCompilationOutputsApi;
import com.google.devtools.build.lib.starlarkbuildapi.cpp.CcInfoApi;
import com.google.devtools.build.lib.starlarkbuildapi.cpp.CcLinkingContextApi;
import com.google.devtools.build.lib.starlarkbuildapi.cpp.CcLinkingOutputsApi;
import com.google.devtools.build.lib.starlarkbuildapi.cpp.CcModuleApi;
import com.google.devtools.build.lib.starlarkbuildapi.cpp.CcToolchainConfigInfoApi;
import com.google.devtools.build.lib.starlarkbuildapi.cpp.CcToolchainProviderApi;
import com.google.devtools.build.lib.starlarkbuildapi.cpp.CcToolchainVariablesApi;
import com.google.devtools.build.lib.starlarkbuildapi.cpp.FeatureConfigurationApi;
import com.google.devtools.build.lib.starlarkbuildapi.cpp.LibraryToLinkApi;
import com.google.devtools.build.lib.starlarkbuildapi.cpp.LinkerInputApi;
import com.google.devtools.build.lib.starlarkbuildapi.platform.ConstraintValueInfoApi;
import com.google.devtools.build.skydoc.fakebuildapi.FakeProviderApi;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

/** Fake implementation of {@link CcModuleApi}. */
public class FakeCcModule
    implements BazelCcModuleApi<
        StarlarkActionFactoryApi,
        FileApi,
        ConstraintValueInfoApi,
        StarlarkRuleContextApi<ConstraintValueInfoApi>,
        CcToolchainProviderApi<FeatureConfigurationApi>,
        FeatureConfigurationApi,
        CcCompilationContextApi<FileApi>,
        CcCompilationOutputsApi<FileApi>,
        CcLinkingOutputsApi<FileApi>,
        LinkerInputApi<LibraryToLinkApi<FileApi>, FileApi>,
        LibraryToLinkApi<FileApi>,
        CcLinkingContextApi<FileApi>,
        CcToolchainVariablesApi,
        CcToolchainConfigInfoApi> {

  @Override
  public ProviderApi getCcToolchainProvider() {
    return new FakeProviderApi();
  }

  @Override
  public FeatureConfigurationApi configureFeatures(
      Object ruleContextOrNone,
      CcToolchainProviderApi<FeatureConfigurationApi> toolchain,
      Sequence<?> requestedFeatures,
      Sequence<?> unsupportedFeatures)
      throws EvalException {
    return null;
  }

  @Override
  public String getToolForAction(FeatureConfigurationApi featureConfiguration, String actionName) {
    return "";
  }

  @Override
  public Sequence<String> getExecutionRequirements(
      FeatureConfigurationApi featureConfiguration, String actionName) {
    return StarlarkList.empty();
  }

  @Override
  public boolean isEnabled(FeatureConfigurationApi featureConfiguration, String featureName) {
    return false;
  }

  @Override
  public boolean actionIsEnabled(FeatureConfigurationApi featureConfiguration, String actionName) {
    return false;
  }

  @Override
  public Sequence<String> getCommandLine(
      FeatureConfigurationApi featureConfiguration,
      String actionName,
      CcToolchainVariablesApi variables) {
    return null;
  }

  @Override
  public Dict<String, String> getEnvironmentVariable(
      FeatureConfigurationApi featureConfiguration,
      String actionName,
      CcToolchainVariablesApi variables) {
    return null;
  }

  @Override
  public CcToolchainVariablesApi getCompileBuildVariables(
      CcToolchainProviderApi<FeatureConfigurationApi> ccToolchainProvider,
      FeatureConfigurationApi featureConfiguration,
      Object sourceFile,
      Object outputFile,
      Object userCompileFlags,
      Object includeDirs,
      Object quoteIncludeDirs,
      Object systemIncludeDirs,
      Object frameworkIncludeDirs,
      Object defines,
      Object thinLtoIndex,
      Object thinLtoInputBitcodeFile,
      Object thinLtoOutputObjectFile,
      boolean usePic,
      boolean addLegacyCxxOptions)
      throws EvalException {
    return null;
  }

  @Override
  public CcToolchainVariablesApi getLinkBuildVariables(
      CcToolchainProviderApi<FeatureConfigurationApi> ccToolchainProvider,
      FeatureConfigurationApi featureConfiguration,
      Object librarySearchDirectories,
      Object runtimeLibrarySearchDirectories,
      Object userLinkFlags,
      Object outputFile,
      Object paramFile,
      Object defFile,
      boolean isUsingLinkerNotArchiver,
      boolean isCreatingSharedLibrary,
      boolean mustKeepDebug,
      boolean useTestOnlyFlags,
      boolean isStaticLinkingMode)
      throws EvalException {
    return null;
  }

  @Override
  public CcToolchainVariablesApi getVariables() {
    return null;
  }

  @Override
  public LibraryToLinkApi<FileApi> createLibraryLinkerInput(
      Object actions,
      Object featureConfiguration,
      Object ccToolchainProvider,
      Object staticLibrary,
      Object picStaticLibrary,
      Object dynamicLibrary,
      Object interfaceLibrary,
      Object picObjectFiles,
      Object objectFiles,
      boolean alwayslink,
      String dynamicLibraryPath,
      String interfaceLibraryPath,
      StarlarkThread thread) {
    return null;
  }

  @Override
  public LinkerInputApi<LibraryToLinkApi<FileApi>, FileApi> createLinkerInput(
      Label owner,
      Object librariesToLinkObject,
      Object userLinkFlagsObject,
      Object nonCodeInputs,
      StarlarkThread thread) {
    return null;
  }

  @Override
  public void checkExperimentalCcSharedLibrary(StarlarkThread thread) {}

  @Override
  public void checkExperimentalStarlarkCcImport(
      StarlarkActionFactoryApi starlarkActionFactoryApi) {}

  @Override
  public CcLinkingContextApi<FileApi> createCcLinkingInfo(
      Object linkerInputs,
      Object librariesToLinkObject,
      Object userLinkFlagsObject,
      Object nonCodeInputs,
      StarlarkThread thread) {
    return null;
  }

  @Override
  public CcInfoApi<FileApi> mergeCcInfos(Sequence<?> directCcInfos, Sequence<?> ccInfos) {
    return null;
  }

  @Override
  public CcCompilationContextApi<FileApi> createCcCompilationContext(
      Object headers,
      Object systemIncludes,
      Object includes,
      Object quoteIncludes,
      Object frameworkIncludes,
      Object defines,
      Object localDefines)
      throws EvalException {
    return null;
  }

  @Override
  public String legacyCcFlagsMakeVariable(
      CcToolchainProviderApi<FeatureConfigurationApi> ccToolchain) {
    return "";
  }

  @Override
  public boolean isCcToolchainResolutionEnabled(
      StarlarkRuleContextApi<ConstraintValueInfoApi> context) {
    return false;
  }

  @Override
  public Tuple<Object> compile(
      StarlarkActionFactoryApi starlarkActionFactoryApi,
      FeatureConfigurationApi starlarkFeatureConfiguration,
      CcToolchainProviderApi<FeatureConfigurationApi> starlarkCcToolchainProvider,
      Sequence<?> sources,
      Sequence<?> publicHeaders,
      Sequence<?> privateHeaders,
      Sequence<?> includes,
      Sequence<?> quoteIncludes,
      Sequence<?> systemIncludes,
      Sequence<?> frameworkIncludes,
      Sequence<?> defines,
      Sequence<?> localDefines,
      String includePrefix,
      String stripIncludePrefix,
      Sequence<?> userCompileFlags,
      Sequence<?> ccCompilationContexts,
      String name,
      boolean disallowPicOutputs,
      boolean disallowNopicOutputs,
      Sequence<?> additionalInputs,
      StarlarkThread thread)
      throws EvalException, InterruptedException {
    return null;
  }

  @Override
  public Tuple<Object> createLinkingContextFromCompilationOutputs(
      StarlarkActionFactoryApi starlarkActionFactoryApi,
      FeatureConfigurationApi starlarkFeatureConfiguration,
      CcToolchainProviderApi<FeatureConfigurationApi> starlarkCcToolchainProvider,
      CcCompilationOutputsApi<FileApi> compilationOutputs,
      Sequence<?> userLinkFlags,
      Sequence<?> ccLinkingContextApis,
      String name,
      String language,
      boolean alwayslink,
      Sequence<?> nonCodeInputs,
      boolean disallowStaticLibraries,
      boolean disallowDynamicLibraries,
      Object grepIncludes,
      StarlarkThread thread)
      throws InterruptedException, EvalException {
    return null;
  }

  @Override
  public CcLinkingOutputsApi<FileApi> link(
      StarlarkActionFactoryApi starlarkActionFactoryApi,
      FeatureConfigurationApi starlarkFeatureConfiguration,
      CcToolchainProviderApi<FeatureConfigurationApi> starlarkCcToolchainProvider,
      Object compilationOutputs,
      Sequence<?> userLinkFlags,
      Sequence<?> linkingContexts,
      String name,
      String language,
      String outputType,
      boolean linkDepsStatically,
      StarlarkInt stamp,
      Sequence<?> additionalInputs,
      Object grepIncludes,
      StarlarkThread thread)
      throws InterruptedException, EvalException {
    return null;
  }

  @Override
  public CcToolchainConfigInfoApi ccToolchainConfigInfoFromStarlark(
      StarlarkRuleContextApi<ConstraintValueInfoApi> starlarkRuleContext,
      Sequence<?> features,
      Sequence<?> actionConfigs,
      Sequence<?> artifactNamePatterns,
      Sequence<?> cxxBuiltInIncludeDirectories,
      String toolchainIdentifier,
      String hostSystemName,
      String targetSystemName,
      String targetCpu,
      String targetLibc,
      String compiler,
      String abiVersion,
      String abiLibcVersion,
      Sequence<?> toolPaths,
      Sequence<?> makeVariables,
      Object builtinSysroot,
      Object ccTargetOs)
      throws EvalException {
    return null;
  }

  @Override
  public CcCompilationOutputsApi<FileApi> createCompilationOutputsFromStarlark(
      Object objectsObject, Object picObjectsObject) {
    return null;
  }

  @Override
  public CcCompilationOutputsApi<FileApi> mergeCcCompilationOutputsFromStarlark(
      Sequence<?> compilationOutputs) {
    return null;
  }
}
