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

import com.google.common.collect.ImmutableSet;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.cmdline.PackageIdentifier;
import com.google.devtools.build.lib.cmdline.ResolvedTargets;
import com.google.devtools.build.lib.packages.NoSuchTargetException;
import com.google.devtools.build.lib.packages.Package;
import com.google.devtools.build.lib.packages.Target;
import com.google.devtools.build.lib.packages.TargetUtils;
import com.google.devtools.build.lib.skyframe.TestsForTargetPatternValue.TestsForTargetPatternKey;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Returns all tests that need to be run when testing is requested for a given set of targets.
 *
 * <p>This requires resolving {@code test_suite} rules.
 */
final class TestsForTargetPatternFunction implements SkyFunction {
  @Override
  public SkyValue compute(SkyKey key, Environment env) throws InterruptedException {
    TestsForTargetPatternKey expansion = (TestsForTargetPatternKey) key.argument();
    ResolvedTargets<Target> targets = labelsToTargets(env, expansion.getTargets(), false);
    List<SkyKey> testsInSuitesKeys = new ArrayList<>();
    for (Target target : targets.getTargets()) {
      if (TargetUtils.isTestSuiteRule(target)) {
        testsInSuitesKeys.add(TestExpansionValue.key(target, true));
      }
    }
    Map<SkyKey, SkyValue> testsInSuites = env.getValues(testsInSuitesKeys);
    if (env.valuesMissing()) {
      return null;
    }

    Set<Label> result = new LinkedHashSet<>();
    boolean hasError = targets.hasError();
    for (Target target : targets.getTargets()) {
      if (TargetUtils.isTestRule(target)) {
        result.add(target.getLabel());
      } else if (TargetUtils.isTestSuiteRule(target)) {
        TestExpansionValue value =
            (TestExpansionValue) testsInSuites.get(TestExpansionValue.key(target, true));
        if (value != null) {
          result.addAll(value.getLabels().getTargets());
          hasError |= value.getLabels().hasError();
        }
      } else {
        result.add(target.getLabel());
      }
    }
    if (env.valuesMissing()) {
      return null;
    }
    // We use ResolvedTargets in order to associate an error flag; the result should never contain
    // any filtered targets.
    return new TestsForTargetPatternValue(new ResolvedTargets<>(result, hasError));
  }

  static ResolvedTargets<Target> labelsToTargets(
      Environment env, ImmutableSet<Label> labels, boolean hasError) throws InterruptedException {
    Set<PackageIdentifier> pkgIdentifiers = new LinkedHashSet<>();
    for (Label label : labels) {
      pkgIdentifiers.add(label.getPackageIdentifier());
    }
    // Don't bother to check for exceptions - the incoming list should only contain valid targets.
    Map<SkyKey, SkyValue> packages = env.getValues(PackageValue.keys(pkgIdentifiers));
    if (env.valuesMissing()) {
      return null;
    }

    ResolvedTargets.Builder<Target> builder = ResolvedTargets.builder();
    builder.mergeError(hasError);
    Map<PackageIdentifier, Package> packageMap = new HashMap<>();
    for (Map.Entry<SkyKey, SkyValue> entry : packages.entrySet()) {
      packageMap.put(
          (PackageIdentifier) entry.getKey().argument(),
          ((PackageValue) entry.getValue()).getPackage());
    }

    for (Label label : labels) {
      Package pkg = packageMap.get(label.getPackageIdentifier());
      if (pkg == null) {
        continue;
      }
      try {
        builder.add(pkg.getTarget(label.getName()));
        if (pkg.containsErrors()) {
          builder.setError();
        }
      } catch (NoSuchTargetException e) {
        builder.setError();
      }
    }
    return builder.build();
  }

  @Nullable
  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }
}
