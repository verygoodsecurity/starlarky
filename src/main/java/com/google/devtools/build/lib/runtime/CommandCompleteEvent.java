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

package com.google.devtools.build.lib.runtime;

import com.google.devtools.build.lib.util.DetailedExitCode;

/**
 * This event is fired when the Blaze command is complete (clean, build, test, etc.). It is fired
 * even if the command terminated abnormally, possibly even before {@link CommandStartEvent} was
 * fired. Subscribers should be tolerant to such a situation.
 */
public class CommandCompleteEvent {
  private final DetailedExitCode detailedExitCode;

  public CommandCompleteEvent(DetailedExitCode detailedExitCode) {
    this.detailedExitCode = detailedExitCode;
  }

  /** Returns the exit code of the blaze command. */
  public DetailedExitCode getExitCode() {
    return detailedExitCode;
  }
}
