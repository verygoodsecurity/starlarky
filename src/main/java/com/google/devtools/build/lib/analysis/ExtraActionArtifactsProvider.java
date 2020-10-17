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

package com.google.devtools.build.lib.analysis;

import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.collect.nestedset.NestedSetBuilder;
import com.google.devtools.build.lib.collect.nestedset.Order;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec.VisibleForSerialization;

/** A {@link TransitiveInfoProvider} that creates extra actions. */
@Immutable
@AutoCodec
public final class ExtraActionArtifactsProvider implements TransitiveInfoProvider {
  public static final ExtraActionArtifactsProvider EMPTY =
      new ExtraActionArtifactsProvider(
          NestedSetBuilder.emptySet(Order.STABLE_ORDER),
          NestedSetBuilder.emptySet(Order.STABLE_ORDER));

  public static ExtraActionArtifactsProvider create(
      NestedSet<Artifact.DerivedArtifact> extraActionArtifacts,
      NestedSet<Artifact.DerivedArtifact> transitiveExtraActionArtifacts) {
    if (extraActionArtifacts.isEmpty() && transitiveExtraActionArtifacts.isEmpty()) {
      return EMPTY;
    }
    return new ExtraActionArtifactsProvider(extraActionArtifacts, transitiveExtraActionArtifacts);
  }

  public static ExtraActionArtifactsProvider merge(
      Iterable<ExtraActionArtifactsProvider> providers) {
    NestedSetBuilder<Artifact.DerivedArtifact> artifacts = NestedSetBuilder.stableOrder();
    NestedSetBuilder<Artifact.DerivedArtifact> transitiveExtraActionArtifacts =
        NestedSetBuilder.stableOrder();

    for (ExtraActionArtifactsProvider provider : providers) {
      artifacts.addTransitive(provider.getExtraActionArtifacts());
      transitiveExtraActionArtifacts.addTransitive(provider.getTransitiveExtraActionArtifacts());
    }
    return ExtraActionArtifactsProvider.create(
        artifacts.build(), transitiveExtraActionArtifacts.build());
  }

  /** The outputs of the extra actions associated with this target. */
  private final NestedSet<Artifact.DerivedArtifact> extraActionArtifacts;

  private final NestedSet<Artifact.DerivedArtifact> transitiveExtraActionArtifacts;

  /** Use {@link #create} instead. */
  @AutoCodec.Instantiator
  @VisibleForSerialization
  ExtraActionArtifactsProvider(
      NestedSet<Artifact.DerivedArtifact> extraActionArtifacts,
      NestedSet<Artifact.DerivedArtifact> transitiveExtraActionArtifacts) {
    this.extraActionArtifacts = extraActionArtifacts;
    this.transitiveExtraActionArtifacts = transitiveExtraActionArtifacts;
  }

  /** The outputs of the extra actions associated with this target. */
  public NestedSet<Artifact.DerivedArtifact> getExtraActionArtifacts() {
    return extraActionArtifacts;
  }

  /** The outputs of the extra actions in the whole transitive closure. */
  public NestedSet<Artifact.DerivedArtifact> getTransitiveExtraActionArtifacts() {
    return transitiveExtraActionArtifacts;
  }
}
