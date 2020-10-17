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

package com.google.devtools.build.lib.rules.repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.actions.MutableActionGraph.ActionConflictException;
import com.google.devtools.build.lib.analysis.AliasProvider;
import com.google.devtools.build.lib.analysis.ConfiguredTarget;
import com.google.devtools.build.lib.analysis.RuleConfiguredTargetFactory;
import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.analysis.TransitiveInfoProvider;
import com.google.devtools.build.lib.analysis.VisibilityProvider;
import com.google.devtools.build.lib.analysis.VisibilityProviderImpl;
import com.google.devtools.build.lib.collect.nestedset.NestedSetBuilder;
import com.google.devtools.build.lib.collect.nestedset.Order;
import com.google.devtools.build.lib.packages.PackageSpecification;
import com.google.devtools.build.lib.packages.PackageSpecification.PackageGroupContents;
import com.google.devtools.build.lib.rules.AliasConfiguredTarget;

/**
 * Implementation for the bind rule.
 */
public class Bind implements RuleConfiguredTargetFactory {

  @Override
  public ConfiguredTarget create(RuleContext ruleContext)
      throws InterruptedException, RuleErrorException, ActionConflictException {
    if (ruleContext.getPrerequisite("actual") == null) {
      ruleContext.ruleError(String.format("The external label '%s' is not bound to anything",
          ruleContext.getLabel()));
      return null;
    }

    ConfiguredTarget actual = (ConfiguredTarget) ruleContext.getPrerequisite("actual");
    return new AliasConfiguredTarget(
        ruleContext,
        actual,
        ImmutableMap.<Class<? extends TransitiveInfoProvider>, TransitiveInfoProvider>of(
            AliasProvider.class,
            AliasProvider.fromAliasRule(ruleContext.getLabel(), actual),
            VisibilityProvider.class,
            new VisibilityProviderImpl(
                NestedSetBuilder.create(
                    Order.STABLE_ORDER,
                    PackageGroupContents.create(
                        ImmutableList.of(PackageSpecification.everything()))))));
  }
}
