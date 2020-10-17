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

package com.google.devtools.build.lib.starlarkbuildapi.config;

import com.google.devtools.build.lib.starlarkbuildapi.core.ProviderApi;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

/** Helper utility containing functions regarding configurations.ss */
@StarlarkBuiltin(
    name = "config_common",
    doc = "Functions for Starlark to interact with Blaze's configurability APIs.")
public interface ConfigStarlarkCommonApi extends StarlarkValue {

  @StarlarkMethod(
      name = "FeatureFlagInfo",
      doc = "The key used to retrieve the provider containing config_feature_flag's value.",
      structField = true)
  ProviderApi getConfigFeatureFlagProviderConstructor();
}
