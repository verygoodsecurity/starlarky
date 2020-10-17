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
 * Test case for a StarlarkMethod method which has a parameter with is neither named nor positional.
 */
public class ParamNeitherNamedNorPositional implements StarlarkValue {

  @StarlarkMethod(
      name = "param_neither_named_nor_positional",
      documented = false,
      parameters = {@Param(name = "a_parameter", named = false, positional = false)})
  public Integer paramUndecidable(Object x) {
    return 42;
  }
}
