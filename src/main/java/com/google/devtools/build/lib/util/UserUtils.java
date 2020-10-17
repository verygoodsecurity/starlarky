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

package com.google.devtools.build.lib.util;

import static com.google.common.base.StandardSystemProperty.USER_NAME;

import com.google.common.base.Strings;

/**
 * User information utility methods.
 */
public final class UserUtils {

  private UserUtils() {
    // prohibit instantiation
  }

  private static class Holder {
    static final String userName = USER_NAME.value();
  }

  /**
   * Returns the user name as provided by system property 'user.name'.
   */
  public static String getUserName() {
    return Holder.userName;
  }

  /**
   * Returns the originating user for this build from the command-line or the environment.
   */
  public static String getOriginatingUser(String originatingUser) {
    if (!Strings.isNullOrEmpty(originatingUser)) {
      return originatingUser;
    }

    return UserUtils.getUserName();
  }
}
