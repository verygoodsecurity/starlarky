// Copyright 2016 The Bazel Authors. All rights reserved.
// Copyright 2026 Very Good Security Authors. All rights reserved.
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

package net.starlark.java.eval;

import com.google.common.collect.ImmutableMap;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;

/** The diagnostic built-ins installed by Larky as predeclared bindings. */
public final class DiagnosticMethodLibrary {
  private final MethodLibrary upstream = new MethodLibrary();

  public static ImmutableMap<String, Object> bindings() {
    ImmutableMap.Builder<String, Object> bindings = ImmutableMap.builder();
    Starlark.addMethods(bindings, new DiagnosticMethodLibrary());
    return bindings.buildOrThrow();
  }

  public static boolean isDefaultBinding(String name, Object value) {
    return ("print".equals(name) || "fail".equals(name))
        && value instanceof BuiltinFunction function
        && function.obj instanceof DiagnosticMethodLibrary;
  }

  @StarlarkMethod(
      name = "print",
      doc = "Prints a debug diagnostic through the evaluation's print handler.",
      parameters = {@Param(name = "sep", defaultValue = "\" \"", positional = false, named = true)},
      extraPositionals = @Param(name = "args"),
      useStarlarkThread = true)
  public void print(String sep, Sequence<?> args, StarlarkThread thread) throws EvalException {
    // Larky's print handler masks the complete diagnostic, including any caller location.
    upstream.print(sep, args, thread);
  }

  @StarlarkMethod(
      name = "fail",
      doc = "Raises a script failure with the given diagnostic arguments.",
      parameters = {
        @Param(name = "msg", defaultValue = "None", positional = false, named = true),
        @Param(
            name = "attr",
            allowedTypes = {@ParamType(type = String.class), @ParamType(type = NoneType.class)},
            defaultValue = "None",
            positional = false,
            named = true),
        @Param(name = "sep", defaultValue = "\" \"", positional = false, named = true),
        @Param(name = "stack_trace", defaultValue = "True", positional = false, named = true)
      },
      extraPositionals = @Param(name = "args"),
      useStarlarkThread = true,
      doesNotReturn = true)
  public void fail(
      Object msg, Object attr, String sep, Boolean stackTrace, Tuple args, StarlarkThread thread)
      throws EvalException {
    // Keep the exception raw for script catches. The host masks it at the diagnostic sink.
    upstream.fail(msg, attr, sep, stackTrace, args, thread);
  }
}
