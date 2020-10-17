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

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.analysis.TransitiveInfoProvider;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.collect.nestedset.NestedSetBuilder;
import com.google.devtools.build.lib.collect.nestedset.Order;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import java.util.Iterator;

/** The collection of source jars from the transitive closure. */
@AutoValue
@Immutable
@AutoCodec
public abstract class JavaSourceJarsProvider implements TransitiveInfoProvider {

  @AutoCodec
  public static final JavaSourceJarsProvider EMPTY =
      create(NestedSetBuilder.emptySet(Order.STABLE_ORDER), ImmutableList.of());

  @AutoCodec.Instantiator
  public static JavaSourceJarsProvider create(
      NestedSet<Artifact> transitiveSourceJars, Iterable<Artifact> sourceJars) {
    return new AutoValue_JavaSourceJarsProvider(
        transitiveSourceJars, ImmutableList.copyOf(sourceJars));
  }

  /**
   * Returns all the source jars in the transitive closure, that can be reached by a chain of
   * JavaSourceJarsProvider instances.
   */
  public abstract NestedSet<Artifact> getTransitiveSourceJars();

  /** Return the source jars that are to be built when the target is on the command line. */
  public abstract ImmutableList<Artifact> getSourceJars();

  public static JavaSourceJarsProvider merge(Iterable<JavaSourceJarsProvider> providers) {
    Iterator<JavaSourceJarsProvider> it = providers.iterator();
    if (!it.hasNext()) {
      return EMPTY;
    }
    JavaSourceJarsProvider first = it.next();
    if (!it.hasNext()) {
      return first;
    }
    JavaSourceJarsProvider.Builder result = builder();
    result.mergeFrom(first);
    do {
      result.mergeFrom(it.next());
    } while (it.hasNext());
    return result.build();
  }

  /** Returns a builder for a {@link JavaSourceJarsProvider}. */
  public static Builder builder() {
    return new Builder();
  }

  /** A builder for {@link JavaSourceJarsProvider}. */
  public static final class Builder {

    private final ImmutableList.Builder<Artifact> sourceJars = ImmutableList.builder();
    private final NestedSetBuilder<Artifact> transitiveSourceJars = NestedSetBuilder.stableOrder();

    /** Add a source jar that is to be built when the target is on the command line. */
    public Builder addSourceJar(Artifact sourceJar) {
      sourceJars.add(Preconditions.checkNotNull(sourceJar));
      return this;
    }

    /** Add source jars to be built when the target is on the command line. */
    public Builder addAllSourceJars(Iterable<Artifact> sourceJars) {
      this.sourceJars.addAll(Preconditions.checkNotNull(sourceJars));
      return this;
    }

    /**
     * Add a source jar in the transitive closure, that can be reached by a chain of
     * JavaSourceJarsProvider instances.
     */
    public Builder addTransitiveSourceJar(Artifact transitiveSourceJar) {
      transitiveSourceJars.add(Preconditions.checkNotNull(transitiveSourceJar));
      return this;
    }

    /**
     * Add source jars in the transitive closure, that can be reached by a chain of
     * JavaSourceJarsProvider instances.
     */
    public Builder addAllTransitiveSourceJars(NestedSet<Artifact> transitiveSourceJars) {
      this.transitiveSourceJars.addTransitive(Preconditions.checkNotNull(transitiveSourceJars));
      return this;
    }

    /** Merge the source jars and transitive source jars from the provider into this builder. */
    public Builder mergeFrom(JavaSourceJarsProvider provider) {
      addAllTransitiveSourceJars(provider.getTransitiveSourceJars());
      addAllSourceJars(provider.getSourceJars());
      return this;
    }

    public JavaSourceJarsProvider build() {
      return JavaSourceJarsProvider.create(transitiveSourceJars.build(), sourceJars.build());
    }
  }
}
