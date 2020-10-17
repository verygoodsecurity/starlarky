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

package com.google.devtools.build.lib.starlarkbuildapi.apple;

import com.google.devtools.build.docgen.annot.DocCategory;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

/** An interface for a configuration type containing info for Apple platforms and tools. */
@StarlarkBuiltin(
    name = "apple",
    doc = "A configuration fragment for Apple platforms.",
    category = DocCategory.CONFIGURATION_FRAGMENT)
public interface AppleConfigurationApi<ApplePlatformTypeApiT extends ApplePlatformTypeApi>
    extends StarlarkValue {

  @StarlarkMethod(
      name = "ios_cpu",
      doc =
          "<b>Deprecated. Use <a href='#single_arch_cpu'>single_arch_cpu</a> instead.</b> "
              + "The value of ios_cpu for this configuration.")
  String getIosCpu();

  @StarlarkMethod(
      name = "single_arch_cpu",
      structField = true,
      doc =
          "The single \"effective\" architecture for this configuration (e.g., <code>i386</code>"
              + " or <code>arm64</code>) in the context of rule logic that is only concerned with"
              + " a single architecture (such as <code>objc_library</code>, which registers"
              + " single-architecture compile actions).")
  String getSingleArchitecture();

  @StarlarkMethod(
      name = "single_arch_platform",
      doc =
          "The platform of the current configuration. This should only be invoked in a context "
              + "where only a single architecture may be supported; consider "
              + "<a href='#multi_arch_platform'>multi_arch_platform</a> for other cases.",
      structField = true)
  ApplePlatformApi getSingleArchPlatform();

  @StarlarkMethod(
      name = "multi_arch_platform",
      doc =
          "The platform of the current configuration for the given platform type. This should only "
              + "be invoked in a context where multiple architectures may be supported; consider "
              + "<a href='#single_arch_platform'>single_arch_platform</a> for other cases.",
      parameters = {
        @Param(
            name = "platform_type",
            positional = true,
            named = false,
            type = ApplePlatformTypeApi.class,
            doc = "The apple platform type.")
      })
  ApplePlatformApi getMultiArchPlatform(ApplePlatformTypeApiT platformType);

  @StarlarkMethod(
      name = "ios_cpu_platform",
      doc =
          "<b>Deprecated. Use <a href='#single_arch_platform'>single_arch_platform</a> or "
              + "<a href='#multi_arch_platform'>multi_arch_platform</a> instead.</b> "
              + "The platform given by the ios_cpu flag.")
  ApplePlatformApi getIosCpuPlatform();

  @StarlarkMethod(
      name = "bitcode_mode",
      doc =
          "Returns the Bitcode mode to use for compilation steps.<p>This field is only valid for"
              + " device builds; for simulator builds, it always returns <code>'none'</code>.",
      structField = true)
  AppleBitcodeModeApi getBitcodeMode();
}
