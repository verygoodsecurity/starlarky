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

package net.starlark.java.annot.processor.testsources;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

/**
 * Test case for a StarlarkMethod method which has a parameter with "None" set as the default, but
 * noneable is not true.
 */
public class InvalidParamNoneDefault implements StarlarkValue {

  @StarlarkMethod(
      name = "method_with_invalid_default",
      documented = false,
      parameters = {
        @Param(name = "a_parameter", type = String.class, named = true, defaultValue = "None"),
      })
  public String methodWithParams(String x) {
    return "lamb";
  }
}
