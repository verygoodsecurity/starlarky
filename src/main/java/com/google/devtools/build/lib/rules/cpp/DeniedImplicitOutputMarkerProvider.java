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

package com.google.devtools.build.lib.rules.cpp;

import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.packages.NativeInfo;
import com.google.devtools.build.lib.packages.NativeProvider;

/** TODO(plf): Remove once implicit outputs are removed from cc_library */
@Immutable
public class DeniedImplicitOutputMarkerProvider extends NativeInfo {
  public static final NativeProvider<DeniedImplicitOutputMarkerProvider> PROVIDER =
      new NativeProvider<DeniedImplicitOutputMarkerProvider>(
          DeniedImplicitOutputMarkerProvider.class, "DeniedImplicitOutputMarkerProvider") {};

  public DeniedImplicitOutputMarkerProvider() {
    super(PROVIDER);
  }
}
