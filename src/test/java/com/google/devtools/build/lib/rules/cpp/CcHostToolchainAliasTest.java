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

package com.google.devtools.build.lib.rules.cpp;

import static com.google.common.truth.Truth.assertThat;

import com.google.devtools.build.lib.analysis.ConfiguredTarget;
import com.google.devtools.build.lib.analysis.platform.ToolchainInfo;
import com.google.devtools.build.lib.analysis.util.BuildViewTestCase;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.packages.util.MockCcSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for toolchain features. */
@RunWith(JUnit4.class)
public class CcHostToolchainAliasTest extends BuildViewTestCase {

  @Test
  public void testCcHostToolchainAliasRuleHasHostConfiguration() throws Exception {
    scratch.file("a/BUILD", "cc_host_toolchain_alias(name='current_cc_host_toolchain')");

    ConfiguredTarget target = getConfiguredTarget("//a:current_cc_host_toolchain");
    CcToolchainProvider toolchainProvider =
        (CcToolchainProvider) target.get(ToolchainInfo.PROVIDER);

    assertThat(toolchainProvider.isToolConfiguration()).isTrue();
  }

  @Test
  public void testThatHostCrosstoolTopCommandLineArgumentWorks() throws Exception {
    scratch.file(
        "b/BUILD",
        "load(':cc_toolchain_config.bzl', 'cc_toolchain_config')",
        "cc_toolchain_suite(",
        "  name = 'my_custom_toolchain_suite',",
        "  toolchains = {",
        "    'k8': '//b:toolchain_b',",
        "    'k8|gcc-4.4.0': '//b:toolchain_b',",
        "    'k8|compiler': '//b:toolchain_b',",
        "    'x64_windows|windows_msys64': '//b:toolchain_b',",
        "    'darwin|compiler': '//b:toolchain_b',",
        "})",
        "cc_toolchain(",
        "    name = 'toolchain_b',",
        "    toolchain_identifier = 'mock-llvm-toolchain-k8',",
        "    toolchain_config = ':mock_config',",
        "    all_files = ':banana',",
        "    ar_files = ':empty',",
        "    as_files = ':empty',",
        "    compiler_files = ':empty',",
        "    dwp_files = ':empty',",
        "    linker_files = ':empty',",
        "    strip_files = ':empty',",
        "    objcopy_files = ':empty')",
        "cc_toolchain_config(name='mock_config')");

    scratch.file("b/cc_toolchain_config.bzl", MockCcSupport.EMPTY_CC_TOOLCHAIN);

    scratch.file("a/BUILD", "cc_host_toolchain_alias(name='current_cc_host_toolchain')");

    useConfiguration("--host_crosstool_top=//b:my_custom_toolchain_suite", "--host_cpu=k8");
    ConfiguredTarget target = getConfiguredTarget("//a:current_cc_host_toolchain");

    assertThat(target.getLabel())
        .isEqualTo(Label.parseAbsoluteUnchecked("//b:my_custom_toolchain_suite"));
  }
}
