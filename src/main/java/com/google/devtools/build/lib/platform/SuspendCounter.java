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

package com.google.devtools.build.lib.platform;

import com.google.devtools.build.lib.jni.JniLoader;

/** Native methods for dealing with suspension events. */
public final class SuspendCounter {

  static {
    JniLoader.loadJni();
  }

  private SuspendCounter() {}

  static native int suspendCountJNI();

  /**
   * The number of times the build has been suspended. Currently this is a hardware sleep and/or the
   * platform equivalents to a SIGSTOP/SIGTSTP.
   */
  public static int suspendCount() {
    return JniLoader.isJniAvailable() ? suspendCountJNI() : 0;
  }
}
