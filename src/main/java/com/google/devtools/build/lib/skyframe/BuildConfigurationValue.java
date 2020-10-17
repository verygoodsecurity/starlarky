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
package com.google.devtools.build.lib.skyframe;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Interner;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration;
import com.google.devtools.build.lib.analysis.config.BuildOptions;
import com.google.devtools.build.lib.analysis.config.Fragment;
import com.google.devtools.build.lib.analysis.config.FragmentClassSet;
import com.google.devtools.build.lib.concurrent.BlazeInterners;
import com.google.devtools.build.lib.concurrent.ThreadSafety.ThreadSafe;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec.VisibleForSerialization;
import com.google.devtools.build.skyframe.SkyFunctionName;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;
import com.google.devtools.common.options.OptionsParsingException;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/** A Skyframe value representing a {@link BuildConfiguration}. */
// TODO(bazel-team): mark this immutable when BuildConfiguration is immutable.
// @Immutable
@AutoCodec
@ThreadSafe
public class BuildConfigurationValue implements SkyValue {
  private final BuildConfiguration configuration;

  BuildConfigurationValue(BuildConfiguration configuration) {
    this.configuration = configuration;
  }

  public BuildConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * Creates a new configuration key based on the given diff, after applying a platform mapping
   * transformation.
   *
   * @param platformMappingValue sky value that can transform a configuration key based on a
   *     platform mapping
   * @param defaultBuildOptions set of native build options without modifications based on parsing
   *     flags
   * @param fragments set of options fragments this configuration should cover
   * @param optionsDiff diff between the default options and the desired configuration
   * @throws OptionsParsingException if the platform mapping cannot be parsed
   */
  public static Key keyWithPlatformMapping(
      PlatformMappingValue platformMappingValue,
      BuildOptions defaultBuildOptions,
      FragmentClassSet fragments,
      BuildOptions.OptionsDiffForReconstruction optionsDiff)
      throws OptionsParsingException {
    return platformMappingValue.map(
        keyWithoutPlatformMapping(fragments, optionsDiff), defaultBuildOptions);
  }

  /**
   * Creates a new configuration key based on the given diff, after applying a platform mapping
   * transformation.
   *
   * @param platformMappingValue sky value that can transform a configuration key based on a
   *     platform mapping
   * @param defaultBuildOptions set of native build options without modifications based on parsing
   *     flags
   * @param fragments set of options fragments this configuration should cover
   * @param optionsDiff diff between the default options and the desired configuration
   * @throws OptionsParsingException if the platform mapping cannot be parsed
   */
  public static Key keyWithPlatformMapping(
      PlatformMappingValue platformMappingValue,
      BuildOptions defaultBuildOptions,
      Set<Class<? extends Fragment>> fragments,
      BuildOptions.OptionsDiffForReconstruction optionsDiff)
      throws OptionsParsingException {
    return platformMappingValue.map(
        keyWithoutPlatformMapping(fragments, optionsDiff), defaultBuildOptions);
  }

  /**
   * Returns the key for a requested configuration.
   *
   * <p>Callers are responsible for applying the platform mapping or ascertaining that a platform
   * mapping is not required.
   *
   * @param fragments the fragments the configuration should contain
   * @param optionsDiff the {@link BuildOptions.OptionsDiffForReconstruction} object the {@link
   *     BuildOptions} should be rebuilt from
   */
  @ThreadSafe
  static Key keyWithoutPlatformMapping(
      Set<Class<? extends Fragment>> fragments,
      BuildOptions.OptionsDiffForReconstruction optionsDiff) {
    return Key.create(
        FragmentClassSet.of(
            ImmutableSortedSet.copyOf(BuildConfiguration.lexicalFragmentSorter, fragments)),
        optionsDiff);
  }

  private static Key keyWithoutPlatformMapping(
      FragmentClassSet fragmentClassSet, BuildOptions.OptionsDiffForReconstruction optionsDiff) {
    return Key.create(fragmentClassSet, optionsDiff);
  }

  /**
   * Returns a configuration key for the given configuration.
   *
   * <p>Note that this key creation method does not apply a platform mapping, it is assumed that the
   * passed configuration was created with one such and thus its key does not need to be mapped
   * again.
   *
   * @param buildConfiguration configuration whose key is requested
   */
  public static Key key(BuildConfiguration buildConfiguration) {
    return keyWithoutPlatformMapping(
        buildConfiguration.fragmentClasses(), buildConfiguration.getBuildOptionsDiff());
  }

  /** {@link SkyKey} for {@link BuildConfigurationValue}. */
  @AutoCodec
  public static final class Key implements SkyKey, Serializable {
    private static final Interner<Key> keyInterner = BlazeInterners.newWeakInterner();

    private final FragmentClassSet fragments;
    final BuildOptions.OptionsDiffForReconstruction optionsDiff;
    // If hashCode really is -1, we'll recompute it from scratch each time. Oh well.
    private volatile int hashCode = -1;

    @AutoCodec.Instantiator
    @VisibleForSerialization
    static Key create(
        FragmentClassSet fragments, BuildOptions.OptionsDiffForReconstruction optionsDiff) {
      return keyInterner.intern(new Key(fragments, optionsDiff));
    }

    private Key(FragmentClassSet fragments, BuildOptions.OptionsDiffForReconstruction optionsDiff) {
      this.fragments = Preconditions.checkNotNull(fragments);
      this.optionsDiff = Preconditions.checkNotNull(optionsDiff);
    }

    @VisibleForTesting
    public ImmutableSortedSet<Class<? extends Fragment>> getFragments() {
      return fragments.fragmentClasses();
    }

    public BuildOptions.OptionsDiffForReconstruction getOptionsDiff() {
      return optionsDiff;
    }

    @Override
    public SkyFunctionName functionName() {
      return SkyFunctions.BUILD_CONFIGURATION;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Key)) {
        return false;
      }
      Key otherConfig = (Key) o;
      return optionsDiff.equals(otherConfig.optionsDiff) && fragments.equals(otherConfig.fragments);
    }

    @Override
    public int hashCode() {
      if (hashCode == -1) {
        hashCode = Objects.hash(fragments, optionsDiff);
      }
      return hashCode;
    }

    @Override
    public String toString() {
      // This format is depended on by integration tests.
      return "BuildConfigurationValue.Key[" + optionsDiff.getChecksum() + "]";
    }
  }
}
