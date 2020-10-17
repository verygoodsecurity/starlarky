// Copyright 2018 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.skydoc.fakebuildapi.test;

import com.google.devtools.build.lib.starlarkbuildapi.test.ExecutionInfoApi;
import com.google.devtools.build.lib.starlarkbuildapi.test.TestEnvironmentInfoApi;
import com.google.devtools.build.lib.starlarkbuildapi.test.TestingModuleApi;
import net.starlark.java.eval.Dict;

/**
 * Fake implementation of {@link TestingModuleApi}.
 */
public class FakeTestingModule implements TestingModuleApi {

  @Override
  public ExecutionInfoApi executionInfo(Dict<?, ?> requirements) {
    return new FakeExecutionInfo();
  }

  @Override
  public TestEnvironmentInfoApi testEnvironment(Dict<?, ?> environment) {
    return new FakeTestingEnvironmentInfo();
  }
}
