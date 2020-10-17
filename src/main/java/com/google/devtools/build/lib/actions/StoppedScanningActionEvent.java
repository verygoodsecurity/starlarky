// Copyright 2019 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.actions;

import com.google.devtools.build.lib.events.ExtendedEventHandler;

/** Counterpart to {@link ScanningActionEvent}: indicates that scanning is over. */
public class StoppedScanningActionEvent implements ExtendedEventHandler.ProgressLike {
  private final Action action;

  public StoppedScanningActionEvent(Action action) {
    this.action = action;
  }

  public Action getAction() {
    return action;
  }
}
