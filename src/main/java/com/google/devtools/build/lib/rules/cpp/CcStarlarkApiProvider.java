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

package com.google.devtools.build.lib.rules.cpp;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.analysis.RuleConfiguredTargetBuilder;
import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.analysis.starlark.StarlarkApiProvider;
import com.google.devtools.build.lib.collect.nestedset.Depset;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.collect.nestedset.NestedSetBuilder;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.lib.starlarkbuildapi.cpp.CcStarlarkApiProviderApi;
import com.google.devtools.build.lib.vfs.PathFragment;

/**
 * A class that exposes the C++ providers to Starlark. It is intended to provide a simple and stable
 * interface for Starlark users.
 */
@AutoCodec
public final class CcStarlarkApiProvider extends StarlarkApiProvider
    implements CcStarlarkApiProviderApi<Artifact> {
  /** The name of the field in Starlark used to access this class. */
  public static final String NAME = "cc";

  public static void maybeAdd(RuleContext ruleContext, RuleConfiguredTargetBuilder builder) {
    if (ruleContext.getFragment(CppConfiguration.class).enableLegacyCcProvider()) {
      builder.addStarlarkTransitiveInfo(NAME, new CcStarlarkApiProvider());
    }
  }

  @Override
  public Depset /*<Artifact>*/ getTransitiveHeadersForStarlark() {
    return Depset.of(Artifact.TYPE, getTransitiveHeaders());
  }

  NestedSet<Artifact> getTransitiveHeaders() {
    CcCompilationContext ccCompilationContext =
        getInfo().get(CcInfo.PROVIDER).getCcCompilationContext();
    return ccCompilationContext.getDeclaredIncludeSrcs();
  }

  @Override
  public Depset /*<Artifact>*/ getLibrariesForStarlark() {
    return Depset.of(Artifact.TYPE, getLibraries());
  }

  NestedSet<Artifact> getLibraries() {
    NestedSetBuilder<Artifact> libs = NestedSetBuilder.linkOrder();
    CcInfo ccInfo = getInfo().get(CcInfo.PROVIDER);
    if (ccInfo == null) {
      return libs.build();
    }
    for (Artifact lib : ccInfo.getCcLinkingContext().getStaticModeParamsForExecutableLibraries()) {
      libs.add(lib);
    }
    return libs.build();
  }

  @Override
  public ImmutableList<String> getLinkopts() {
    CcInfo ccInfo = getInfo().get(CcInfo.PROVIDER);
    if (ccInfo == null) {
      return ImmutableList.of();
    }
    return ccInfo.getCcLinkingContext().getFlattenedUserLinkFlags();
  }

  @Override
  public ImmutableList<String> getDefines() {
    CcCompilationContext ccCompilationContext =
        getInfo().get(CcInfo.PROVIDER).getCcCompilationContext();
    return ccCompilationContext == null
        ? ImmutableList.<String>of()
        : ccCompilationContext.getDefines().toList();
  }

  @Override
  public ImmutableList<String> getSystemIncludeDirs() {
    CcCompilationContext ccCompilationContext =
        getInfo().get(CcInfo.PROVIDER).getCcCompilationContext();
    if (ccCompilationContext == null) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (PathFragment path : ccCompilationContext.getSystemIncludeDirs()) {
      builder.add(path.getSafePathString());
    }
    return builder.build();
  }

  @Override
  public ImmutableList<String> getIncludeDirs() {
    CcCompilationContext ccCompilationContext =
        getInfo().get(CcInfo.PROVIDER).getCcCompilationContext();
    if (ccCompilationContext == null) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (PathFragment path : ccCompilationContext.getIncludeDirs()) {
      builder.add(path.getSafePathString());
    }
    return builder.build();
  }

  @Override
  public ImmutableList<String> getQuoteIncludeDirs() {
    CcCompilationContext ccCompilationContext =
        getInfo().get(CcInfo.PROVIDER).getCcCompilationContext();
    if (ccCompilationContext == null) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (PathFragment path : ccCompilationContext.getQuoteIncludeDirs()) {
      builder.add(path.getSafePathString());
    }
    return builder.build();
  }

  @Override
  public ImmutableList<String> getCcFlags() {
    CcCompilationContext ccCompilationContext =
        getInfo().get(CcInfo.PROVIDER).getCcCompilationContext();

    ImmutableList.Builder<String> options = ImmutableList.builder();
    for (String define : ccCompilationContext.getDefines().toList()) {
      options.add("-D" + define);
    }
    for (PathFragment path : ccCompilationContext.getSystemIncludeDirs()) {
      options.add("-isystem " + path.getSafePathString());
    }
    for (PathFragment path : ccCompilationContext.getIncludeDirs()) {
      options.add("-I " + path.getSafePathString());
    }
    for (PathFragment path : ccCompilationContext.getQuoteIncludeDirs()) {
      options.add("-iquote " + path.getSafePathString());
    }

    return options.build();
  }
}
