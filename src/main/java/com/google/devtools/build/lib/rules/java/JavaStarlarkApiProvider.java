// Copyright 2015 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.rules.java;

import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.analysis.starlark.StarlarkApiProvider;
import com.google.devtools.build.lib.packages.StarlarkProviderIdentifier;
import com.google.devtools.build.lib.starlarkbuildapi.java.JavaStarlarkApiProviderApi;

/**
 * A class that exposes the Java providers to Starlark. It is intended to provide a simple and
 * stable interface for Starlark users.
 */
public final class JavaStarlarkApiProvider extends StarlarkApiProvider
    implements JavaStarlarkApiProviderApi<Artifact> {
  /** The name of the field in Starlark used to access this class. */
  public static final String NAME = "java";
  /** The name of the field in Starlark proto aspects used to access this class. */
  public static final StarlarkProviderIdentifier STARLARK_NAME =
      StarlarkProviderIdentifier.forLegacy(NAME);

  /**
   * Creates a Starlark API provider that reads information from its associated target's providers.
   */
  public static JavaStarlarkApiProvider fromRuleContext() {
    return new JavaStarlarkApiProvider();
  }
}
