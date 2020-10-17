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

package com.google.devtools.build.lib.analysis.test;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.analysis.TransitiveInfoProvider;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;

/**
 * A {@link TransitiveInfoProvider} for configured targets to remember tags for test rules.
 *
 * <p>Temporary hack to allow dependencies on test_suite targets to continue to work for the time
 * being.
 */
@Immutable
public final class TestTagsProvider implements TransitiveInfoProvider {
  private final ImmutableList<String> testTags;

  public TestTagsProvider(ImmutableList<String> testTags) {
    this.testTags = testTags;
  }

  public ImmutableList<String> getTestTags() {
    return testTags;
  }
}
