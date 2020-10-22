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
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

/** Test source file verifying various proper uses of StarlarkMethod. */
public class GoldenCase implements StarlarkValue {

  @StarlarkMethod(name = "struct_field_method", documented = false, structField = true)
  public String structFieldMethod() {
    return "foo";
  }

  @StarlarkMethod(name = "zero_arg_method", documented = false)
  public Integer zeroArgMethod() {
    return 0;
  }

  @StarlarkMethod(
      name = "zero_arg_method_with_thread",
      documented = false,
      useStarlarkThread = true)
  public Integer zeroArgMethodWithThread(StarlarkThread thread) {
    return 0;
  }

  @StarlarkMethod(
      name = "three_arg_method",
      documented = false,
      parameters = {
        @Param(name = "one", named = true),
        @Param(name = "two", named = true),
        @Param(
            name = "three",
            allowedTypes = {
              @ParamType(type = String.class),
              @ParamType(type = NoneType.class),
            },
            named = true,
            defaultValue = "None"),
      })
  public String threeArgMethod(String one, StarlarkInt two, Object three) {
    return "bar";
  }

  @StarlarkMethod(
      name = "three_arg_method_with_params_and_thread",
      documented = false,
      parameters = {
        @Param(name = "one", named = true),
        @Param(name = "two", named = true),
        @Param(name = "three", named = true),
      },
      useStarlarkThread = true)
  public String threeArgMethodWithParams(
      String one, StarlarkInt two, String three, StarlarkThread thread) {
    return "baz";
  }

  @StarlarkMethod(
      name = "many_arg_method_mixing_positional_and_named",
      documented = false,
      parameters = {
        @Param(name = "one", positional = true, named = false),
        @Param(name = "two", positional = true, named = true),
        @Param(name = "three", positional = true, named = true, defaultValue = "three"),
        @Param(name = "four", positional = false, named = true),
        @Param(name = "five", positional = false, named = true, defaultValue = "five"),
        @Param(name = "six", positional = false, named = true),
      })
  public String manyArgMethodMixingPositionalAndNamed(
      String one, String two, String three, String four, String five, String six) {
    return "baz";
  }

  @StarlarkMethod(
      name = "two_arg_method_with_params_and_thread_and_kwargs",
      documented = false,
      parameters = {
        @Param(name = "one", named = true),
        @Param(name = "two", named = true),
      },
      extraKeywords = @Param(name = "kwargs"),
      useStarlarkThread = true)
  public String twoArgMethodWithParamsAndInfoAndKwargs(
      String one, StarlarkInt two, Dict<String, Object> kwargs, StarlarkThread thread) {
    return "blep";
  }

  @StarlarkMethod(
      name = "two_arg_method_with_env_and_args_and_kwargs",
      documented = false,
      parameters = {
        @Param(name = "one", named = true),
        @Param(name = "two", named = true),
      },
      extraPositionals = @Param(name = "args"),
      extraKeywords = @Param(name = "kwargs"),
      useStarlarkThread = true)
  public String twoArgMethodWithParamsAndInfoAndKwargs(
      String one, StarlarkInt two, Sequence<?> args, Dict<?, ?> kwargs, StarlarkThread thread) {
    return "yar";
  }

  @StarlarkMethod(
      name = "selfCallMethod",
      selfCall = true,
      parameters = {
        @Param(name = "one", named = true),
        @Param(name = "two", named = true),
      },
      documented = false)
  public Integer selfCallMethod(String one, StarlarkInt two) {
    return 0;
  }

  @StarlarkMethod(
      name = "struct_field_method_with_semantics",
      documented = false,
      structField = true,
      useStarlarkSemantics = true)
  public String structFieldMethodWithSemantics(StarlarkSemantics starlarkSemantics) {
    return "dragon";
  }

  @StarlarkMethod(
      name = "method_with_list_and_dict",
      documented = false,
      parameters = {
        @Param(name = "one", named = true),
        @Param(name = "two", named = true),
      })
  public String methodWithListandDict(Sequence<?> one, Dict<?, ?> two) {
    return "bar";
  }
}
