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
package com.google.devtools.build.lib.analysis;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.build.lib.analysis.platform.PlatformInfo;
import com.google.devtools.build.lib.analysis.platform.ToolchainTypeInfo;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.skyframe.ToolchainContextKey;

/** Represents the data needed for a specific target's use of toolchains and platforms. */
public interface ToolchainContext {
  /** Returns the key that identifies this context. */
  ToolchainContextKey key();

  /** Returns the selected execution platform that these toolchains use. */
  PlatformInfo executionPlatform();

  /** Returns the target platform that these toolchains generate output for. */
  PlatformInfo targetPlatform();

  /** Returns the toolchain types that were requested. */
  ImmutableSet<ToolchainTypeInfo> requiredToolchainTypes();

  /** Returns the labels of the specific toolchains being used. */
  ImmutableSet<Label> resolvedToolchainLabels();
}
