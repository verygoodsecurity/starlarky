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

package com.google.devtools.build.lib.buildtool.buildevent;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.analysis.ConfiguredTarget;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration;
import com.google.devtools.build.lib.analysis.test.TestProvider;
import com.google.devtools.build.lib.skyframe.BuildConfigurationValue;
import java.util.Collection;
import java.util.Map;
import javax.annotation.concurrent.Immutable;

/**
 * This event is fired after test filtering.
 *
 * The test filtering phase always expands test_suite rules, so
 * the set of active targets should never contain test_suites.
 */
@Immutable
public class TestFilteringCompleteEvent {
  private final Collection<ConfiguredTarget> targets;
  private final Collection<ConfiguredTarget> testTargets;
  private final Map<BuildConfigurationValue.Key, BuildConfiguration> configurationMap;

  /**
   * Construct the event.
   *
   * @param targets The set of active targets that remain.
   * @param testTargets The collection of tests to be run. May be null.
   * @param configurationMap A map from configuration keys of all targets to the configurations.
   */
  public TestFilteringCompleteEvent(
      Collection<? extends ConfiguredTarget> targets,
      Collection<? extends ConfiguredTarget> testTargets,
      Map<BuildConfigurationValue.Key, BuildConfiguration> configurationMap) {
    this.targets = ImmutableList.copyOf(targets);
    this.testTargets = testTargets == null ? null : ImmutableList.copyOf(testTargets);
    this.configurationMap = configurationMap;
    if (testTargets == null) {
      return;
    }

    for (ConfiguredTarget testTarget : testTargets) {
      Preconditions.checkState(testTarget.getProvider(TestProvider.class) != null);
    }
  }

  /**
   * @return The set of active targets remaining. This is a subset of
   *     the targets that passed analysis, after test_suite expansion.
   */
  public Collection<ConfiguredTarget> getTargets() {
    return targets;
  }

  /**
   * @return The set of test targets to be run. May be null.
   */
  public Collection<ConfiguredTarget> getTestTargets() {
    return testTargets;
  }

  public BuildConfiguration getConfigurationForTarget(ConfiguredTarget target) {
    return Preconditions.checkNotNull(configurationMap.get(target.getConfigurationKey()));
  }
}
