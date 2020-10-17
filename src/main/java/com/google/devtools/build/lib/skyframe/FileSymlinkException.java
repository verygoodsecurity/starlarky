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

import java.io.IOException;

/** Exception indicating a problem with symlinks. */
public abstract class FileSymlinkException extends IOException {
  protected FileSymlinkException(String message) {
    super(message);
  }

  /** Returns a description of the problem that is suitable for printing to users. */
  // TODO(nharmata): Consider unifying this with AbstractChainUniquenessFunction.
  public abstract String getUserFriendlyMessage();
}
