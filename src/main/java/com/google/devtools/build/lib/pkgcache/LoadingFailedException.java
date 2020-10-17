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
package com.google.devtools.build.lib.pkgcache;

import com.google.devtools.build.lib.skyframe.DetailedException;
import com.google.devtools.build.lib.util.DetailedExitCode;

/**
 * An exception indicating that there was a problem during the loading phase for one or more targets
 * in such a way that the build cannot proceed (for example because keep_going is disabled).
 */
public class LoadingFailedException extends Exception implements DetailedException {

  private final DetailedExitCode detailedExitCode;

  public LoadingFailedException(String message, DetailedExitCode detailedExitCode) {
    super(message);
    this.detailedExitCode = detailedExitCode;
  }

  @Override
  public DetailedExitCode getDetailedExitCode() {
    return detailedExitCode;
  }
}
