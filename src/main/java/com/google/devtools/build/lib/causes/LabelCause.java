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
package com.google.devtools.build.lib.causes;

import com.google.common.base.MoreObjects;
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.util.DetailedExitCode;
import java.util.Objects;

/** Failure due to something associated with a label. */
public class LabelCause implements Cause {
  private final Label label;
  private final DetailedExitCode detailedExitCode;

  public LabelCause(Label label, DetailedExitCode detailedExitCode) {
    this.label = label;
    this.detailedExitCode = detailedExitCode;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("label", label)
        .add("detailedExitCode", detailedExitCode)
        .toString();
  }

  @Override
  public Label getLabel() {
    return label;
  }

  @Override
  public DetailedExitCode getDetailedExitCode() {
    return detailedExitCode;
  }

  public String getMessage() {
    return detailedExitCode.getFailureDetail().getMessage();
  }

  @Override
  public BuildEventStreamProtos.BuildEventId getIdProto() {
    return BuildEventStreamProtos.BuildEventId.newBuilder()
        .setUnconfiguredLabel(
            BuildEventStreamProtos.BuildEventId.UnconfiguredLabelId.newBuilder()
                .setLabel(label.toString())
                .build())
        .build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (!(o instanceof LabelCause)) {
      return false;
    }
    LabelCause a = (LabelCause) o;
    return label.equals(a.label) && detailedExitCode.equals(a.detailedExitCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, detailedExitCode);
  }
}
