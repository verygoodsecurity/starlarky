// Copyright 2018 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.skyframe.actiongraph;

import com.google.devtools.build.lib.analysis.AnalysisProtos;
import com.google.devtools.build.lib.analysis.AnalysisProtos.ActionGraphContainer;

/**
 * Cache for RuleClassStrings in the action graph.
 */
public class KnownRuleClassStrings extends BaseCache<String, AnalysisProtos.RuleClass> {

  KnownRuleClassStrings(ActionGraphContainer.Builder actionGraphBuilder) {
    super(actionGraphBuilder);
  }

  @Override
  AnalysisProtos.RuleClass createProto(String ruleClassString, String id) {
    return AnalysisProtos.RuleClass.newBuilder()
        .setId(id)
        .setName(ruleClassString)
        .build();
  }

  @Override
  void addToActionGraphBuilder(AnalysisProtos.RuleClass ruleClassProto) {
    actionGraphBuilder.addRuleClasses(ruleClassProto);
  }
}
