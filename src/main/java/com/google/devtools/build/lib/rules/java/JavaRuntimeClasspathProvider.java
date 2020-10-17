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

package com.google.devtools.build.lib.rules.java;

import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.analysis.TransitiveInfoProvider;
import com.google.devtools.build.lib.collect.nestedset.Depset;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.lib.starlarkbuildapi.java.JavaRuntimeClasspathProviderApi;

/**
 * Provider for the runtime classpath contributions of a Java binary.
 *
 * <p>Used to exclude already-available artifacts from related binaries (e.g. plugins).
 */
@Immutable
@AutoCodec
public final class JavaRuntimeClasspathProvider
    implements TransitiveInfoProvider, JavaRuntimeClasspathProviderApi {

  private final NestedSet<Artifact> runtimeClasspath;

  public JavaRuntimeClasspathProvider(NestedSet<Artifact> runtimeClasspath) {
    this.runtimeClasspath = runtimeClasspath;
  }

  @Override
  public boolean isImmutable() {
    return true; // immutable and Starlark-hashable
  }

  /** Returns the artifacts included on the runtime classpath of this binary. */
  @Override
  public Depset /*<Artifact>*/ getRuntimeClasspath() {
    return Depset.of(Artifact.TYPE, runtimeClasspath);
  }

  public NestedSet<Artifact> getRuntimeClasspathNestedSet() {
    return runtimeClasspath;
  }
}
