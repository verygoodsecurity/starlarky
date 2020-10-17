// Copyright 2014 The Bazel Authors. All rights reserved.
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

import com.google.devtools.build.lib.analysis.TransitiveInfoProvider;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;

/**
 * A target that provides native libraries in the transitive closure of its deps that are needed for
 * executing C++ code.
 */
@Immutable
@AutoCodec
public final class CcNativeLibraryProvider implements TransitiveInfoProvider {
  private final NestedSet<LibraryToLink> transitiveCcNativeLibraries;

  public CcNativeLibraryProvider(NestedSet<LibraryToLink> transitiveCcNativeLibraries) {
    this.transitiveCcNativeLibraries = transitiveCcNativeLibraries;
  }

  /**
   * Collects native libraries in the transitive closure of its deps that are needed for executing
   * C/C++ code.
   *
   * <p>In effect, returns all dynamic library (.so) artifacts provided by the transitive closure.
   */
  public NestedSet<LibraryToLink> getTransitiveCcNativeLibraries() {
    return transitiveCcNativeLibraries;
  }
}
