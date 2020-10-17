// Copyright 2017 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.rules.platform;

import static com.google.common.truth.Truth.assertThat;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.IterableSubject;
import com.google.devtools.build.lib.analysis.platform.ConstraintSettingInfo;
import com.google.devtools.build.lib.analysis.platform.ConstraintValueInfo;
import com.google.devtools.build.lib.analysis.platform.DeclaredToolchainInfo;
import com.google.devtools.build.lib.analysis.platform.PlatformInfo;
import com.google.devtools.build.lib.analysis.platform.ToolchainTypeInfo;
import com.google.devtools.build.lib.analysis.util.BuildViewTestCase;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.cmdline.PackageIdentifier;
import com.google.devtools.build.lib.skyframe.RegisteredToolchainsValue;
import com.google.devtools.build.lib.skyframe.util.SkyframeExecutorTestUtils;
import com.google.devtools.build.skyframe.EvaluationResult;
import com.google.devtools.build.skyframe.SkyKey;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.Before;

/** Utility methods for setting up platform and toolchain related tests. */
public abstract class ToolchainTestCase extends BuildViewTestCase {

  public PlatformInfo linuxPlatform;
  public PlatformInfo macPlatform;

  public ConstraintSettingInfo setting;
  public ConstraintSettingInfo defaultedSetting;
  public ConstraintValueInfo linuxConstraint;
  public ConstraintValueInfo macConstraint;
  public ConstraintValueInfo defaultedConstraint;

  public Label testToolchainTypeLabel;
  public ToolchainTypeInfo testToolchainType;

  protected static IterableSubject assertToolchainLabels(
      RegisteredToolchainsValue registeredToolchainsValue) {
    return assertToolchainLabels(registeredToolchainsValue, null);
  }

  protected static IterableSubject assertToolchainLabels(
      RegisteredToolchainsValue registeredToolchainsValue,
      @Nullable PackageIdentifier packageRoot) {
    assertThat(registeredToolchainsValue).isNotNull();
    ImmutableList<DeclaredToolchainInfo> declaredToolchains =
        registeredToolchainsValue.registeredToolchains();
    List<Label> labels = collectToolchainLabels(declaredToolchains, packageRoot);
    return assertThat(labels);
  }

  protected static List<Label> collectToolchainLabels(
      List<DeclaredToolchainInfo> toolchains, @Nullable PackageIdentifier packageRoot) {
    return toolchains.stream()
        .map(toolchain -> toolchain.toolchainLabel())
        .filter(label -> filterLabel(packageRoot, label))
        .collect(Collectors.toList());
  }

  protected static boolean filterLabel(@Nullable PackageIdentifier packageRoot, Label label) {
    if (packageRoot == null) {
      return true;
    }

    // Make sure the label is under the packageRoot.
    if (!label.getPackageIdentifier().getRepository().equals(packageRoot.getRepository())) {
      return false;
    }

    return label
        .getPackageIdentifier()
        .getPackageFragment()
        .startsWith(packageRoot.getPackageFragment());
  }

  private static String formatConstraints(Collection<String> constraints) {
    return constraints.stream().map(c -> String.format("'%s'", c)).collect(joining(", "));
  }

  @Before
  public void createConstraints() throws Exception {
    scratch.file(
        "constraints/BUILD",
        "constraint_setting(name = 'os')",
        "constraint_value(name = 'linux',",
        "    constraint_setting = ':os')",
        "constraint_value(name = 'mac',",
        "    constraint_setting = ':os')",
        "constraint_setting(name = 'setting_with_default',",
        "    default_constraint_value = ':default_value')",
        "constraint_value(name = 'default_value',",
        "    constraint_setting = ':setting_with_default')",
        "constraint_value(name = 'non_default_value',",
        "    constraint_setting = ':setting_with_default')");

    scratch.file(
        "platforms/BUILD",
        "platform(name = 'linux',",
        "    constraint_values = ['//constraints:linux', '//constraints:non_default_value'])",
        "platform(name = 'mac',",
        "    constraint_values = ['//constraints:mac', '//constraints:non_default_value'])");

    setting = ConstraintSettingInfo.create(makeLabel("//constraints:os"));
    linuxConstraint = ConstraintValueInfo.create(setting, makeLabel("//constraints:linux"));
    macConstraint = ConstraintValueInfo.create(setting, makeLabel("//constraints:mac"));
    defaultedSetting =
        ConstraintSettingInfo.create(makeLabel("//constraints:setting_with_default"));
    defaultedConstraint =
        ConstraintValueInfo.create(defaultedSetting, makeLabel("//constraints:non_default_value"));

    linuxPlatform =
        PlatformInfo.builder()
            .setLabel(makeLabel("//platforms:linux"))
            .addConstraint(linuxConstraint)
            .addConstraint(defaultedConstraint)
            .build();
    macPlatform =
        PlatformInfo.builder()
            .setLabel(makeLabel("//platforms:mac"))
            .addConstraint(macConstraint)
            .addConstraint(defaultedConstraint)
            .build();
  }

  public void addToolchain(
      String packageName,
      String toolchainName,
      Collection<String> execConstraints,
      Collection<String> targetConstraints,
      String data)
      throws Exception {
    scratch.appendFile(
        packageName + "/BUILD",
        "load('//toolchain:toolchain_def.bzl', 'test_toolchain')",
        "toolchain(",
        "    name = '" + toolchainName + "',",
        "    toolchain_type = '//toolchain:test_toolchain',",
        "    exec_compatible_with = [" + formatConstraints(execConstraints) + "],",
        "    target_compatible_with = [" + formatConstraints(targetConstraints) + "],",
        "    toolchain = ':" + toolchainName + "_impl')",
        "test_toolchain(",
        "  name='" + toolchainName + "_impl',",
        "  data = '" + data + "')");
  }

  @Before
  public void createToolchains() throws Exception {
    rewriteWorkspace("register_toolchains('//toolchain:toolchain_1', '//toolchain:toolchain_2')");

    scratch.file(
        "toolchain/toolchain_def.bzl",
        "def _impl(ctx):",
        "  toolchain = platform_common.ToolchainInfo(",
        "      data = ctx.attr.data)",
        "  return [toolchain]",
        "test_toolchain = rule(",
        "    implementation = _impl,",
        "    attrs = {",
        "       'data': attr.string()})");

    scratch.file("toolchain/BUILD", "toolchain_type(name = 'test_toolchain')");
    addToolchain(
        "toolchain",
        "toolchain_1",
        ImmutableList.of("//constraints:linux"),
        ImmutableList.of("//constraints:mac"),
        "foo");
    addToolchain(
        "toolchain",
        "toolchain_2",
        ImmutableList.of("//constraints:mac"),
        ImmutableList.of("//constraints:linux"),
        "bar");

    testToolchainTypeLabel = makeLabel("//toolchain:test_toolchain");
    testToolchainType = ToolchainTypeInfo.create(testToolchainTypeLabel);
  }

  protected EvaluationResult<RegisteredToolchainsValue> requestToolchainsFromSkyframe(
      SkyKey toolchainsKey) throws InterruptedException {
    try {
      getSkyframeExecutor().getSkyframeBuildView().enableAnalysis(true);
      return SkyframeExecutorTestUtils.evaluate(
          getSkyframeExecutor(), toolchainsKey, /*keepGoing=*/ false, reporter);
    } finally {
      getSkyframeExecutor().getSkyframeBuildView().enableAnalysis(false);
    }
  }
}
