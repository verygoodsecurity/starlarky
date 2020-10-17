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

package com.google.devtools.build.lib.skyframe;

import com.google.devtools.build.lib.actions.ActionLookupKey;
import com.google.devtools.build.lib.actions.Actions.GeneratingActions;
import com.google.devtools.build.lib.actions.BasicActionLookupValue;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.SerializationConstant;
import com.google.devtools.build.skyframe.SkyFunctionName;
import javax.annotation.Nullable;

/** A SkyValue to store the coverage report Action and Artifacts. */
public final class CoverageReportValue extends BasicActionLookupValue {

  // There should only ever be one CoverageReportValue value in the graph.
  @SerializationConstant
  public static final ActionLookupKey COVERAGE_REPORT_KEY = new CoverageReportKey();

  CoverageReportValue(GeneratingActions generatingActions) {
    super(generatingActions);
  }

  private static final class CoverageReportKey implements ActionLookupKey {
    private CoverageReportKey() {}

    @Override
    public SkyFunctionName functionName() {
      return SkyFunctions.COVERAGE_REPORT;
    }

    @Nullable
    @Override
    public Label getLabel() {
      return null;
    }
  }
}
