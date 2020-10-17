// Copyright 2019 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.starlarkbuildapi.python;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.packages.semantics.BuildLanguageOptions;
import com.google.devtools.build.lib.starlarkbuildapi.core.Bootstrap;
import com.google.devtools.build.lib.starlarkbuildapi.python.PyInfoApi.PyInfoProviderApi;
import com.google.devtools.build.lib.starlarkbuildapi.python.PyRuntimeInfoApi.PyRuntimeInfoProviderApi;
import net.starlark.java.eval.FlagGuardedValue;

/** {@link Bootstrap} for Starlark objects related to the Python rules. */
public class PyBootstrap implements Bootstrap {

  private final PyInfoProviderApi pyInfoProviderApi;
  private final PyRuntimeInfoProviderApi pyRuntimeInfoProviderApi;
  private final PyStarlarkTransitionsApi pyStarlarkTransitionsApi;

  public PyBootstrap(
      PyInfoProviderApi pyInfoProviderApi,
      PyRuntimeInfoProviderApi pyRuntimeInfoProviderApi,
      PyStarlarkTransitionsApi pyStarlarkTransitionsApi) {
    this.pyInfoProviderApi = pyInfoProviderApi;
    this.pyRuntimeInfoProviderApi = pyRuntimeInfoProviderApi;
    this.pyStarlarkTransitionsApi = pyStarlarkTransitionsApi;
  }

  @Override
  public void addBindingsToBuilder(ImmutableMap.Builder<String, Object> builder) {
    builder.put("PyInfo", pyInfoProviderApi);
    builder.put("PyRuntimeInfo", pyRuntimeInfoProviderApi);
    builder.put(
        "py_transitions",
        FlagGuardedValue.onlyWhenExperimentalFlagIsTrue(
            BuildLanguageOptions.EXPERIMENTAL_GOOGLE_LEGACY_API, pyStarlarkTransitionsApi));
  }
}
