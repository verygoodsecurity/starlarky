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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.packages.Target;

import java.util.Collection;

/**
 * This event is fired before the analysis phase is started.
 */
public class AnalysisPhaseStartedEvent {

  private final ImmutableSet<Target> targets;

  /**
   * Construct the event.
   * @param targets The set of active targets that remain.
   */
  public AnalysisPhaseStartedEvent(Collection<Target> targets) {
    this.targets = ImmutableSet.copyOf(targets);
  }

  /**
   * @return The set of active targets remaining, which is a subset
   *     of the targets we attempted to load.
   */
  public Iterable<Label> getLabels() {
    return Iterables.transform(targets, new Function<Target, Label>() {
      @Override
      public Label apply(Target input) {
        return input.getLabel();
      }
    });
  }

  public ImmutableSet<Target> getTargets() {
    return targets;
  }
}
