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

package com.google.devtools.build.lib.analysis.util;

import com.google.devtools.build.lib.analysis.ConfiguredRuleClassProvider;
import com.google.devtools.build.lib.analysis.config.BuildOptions;
import com.google.devtools.common.options.OptionsParsingException;

/**
 * Static helper to provide {@link BuildOptions} that are used as default build options in tests.
 */
public class DefaultBuildOptionsForTesting {

  public static BuildOptions getDefaultBuildOptionsForTest(
      ConfiguredRuleClassProvider ruleClassProvider) {
    try {
      return BuildOptions.of(ruleClassProvider.getConfigurationOptions());
    } catch (OptionsParsingException e) {
      throw new IllegalArgumentException("Failed to create default BuildOptions for test", e);
    }
  }
}
