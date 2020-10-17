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
package com.google.devtools.build.android.desugar;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Variant of {@link DesugarJava8FunctionalTest} that doesn't expect any bridge methods already
 * present on functional interfaces to be also present on generated classes, even where functional
 * interfaces are defined in other compilations, which requires compiling against regular jar files
 * instead of a classpath of -hjars.
 */
@RunWith(JUnit4.class)
public final class DesugarJava8LikeAndroidStudioFunctionalTest extends DesugarJava8FunctionalTest {

  public DesugarJava8LikeAndroidStudioFunctionalTest() {
    super(/*expectBridgesFromSeparateTarget*/ false, /*expectDefaultMethods*/ true);
  }
}
