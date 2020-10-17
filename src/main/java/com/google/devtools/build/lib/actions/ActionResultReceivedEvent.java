// Copyright 2017 The Bazel Authors. All rights reserved.
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

import com.google.devtools.build.lib.events.ExtendedEventHandler.ProgressLike;

/**
 * An event that is fired when a non-empty {@link ActionResult} is returned by the execution of an
 * {@link Action}.
 */
public final class ActionResultReceivedEvent implements ProgressLike {
  private final Action action;
  private final ActionResult actionResult;

  public ActionResultReceivedEvent(Action action, ActionResult actionResult) {
    this.action = action;
    this.actionResult = actionResult;
  }

  /** Returns the {@link Action} that created the {@link ActionResult}. */
  public Action getAction() {
    return action;
  }

  /** Returns the {@link ActionResult} returned by the execution of the {@link Action}. */
  public ActionResult getActionResult() {
    return actionResult;
  }
}
