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
    DiagnosticRedactor.Scope scope = thread.getThreadLocal(DiagnosticRedactor.Scope.class);
    DiagnosticPrinter printer = scope == null ? null : scope.printer();
    if (printer == null) {
      upstream.print(sep, args, thread);
      return;
    }
    String separator = "";
    for (Object arg : args) {
      printer.debugPrint(separator, thread).debugPrint(arg, thread);
      separator = sep;
    }
    if (thread.getSemantics().getBool(StarlarkSemantics.PRINT_TEST_MARKER)) {
      printer.append("<== Starlark flag test ==>");
    }
    scope.print(thread, printer);
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
    DiagnosticRedactor.Scope scope = thread.getThreadLocal(DiagnosticRedactor.Scope.class);
    DiagnosticPrinter printer = scope == null ? null : scope.printer();
    if (printer == null) {
      upstream.fail(msg, attr, sep, stackTrace, args, thread);
      return;
    }
    boolean includeStack =
        stackTrace || thread.getSemantics().getBool(StarlarkSemantics.FORCE_STARLARK_STACK_TRACE);
    String separator = "";
    if (attr != Starlark.NONE) {
      printer.append("attribute ").debugPrint(attr, thread).append(":");
      separator = sep;
    }
    if (msg != Starlark.NONE) {
      printer.debugPrint(separator, thread).debugPrint(msg, thread);
      separator = sep;
    }
    for (Object arg : args) {
      printer.debugPrint(separator, thread).debugPrint(arg, thread);
      separator = sep;
    }
    EvalException failure = new EvalException(printer.toString(), null, includeStack);
    scope.recordFailure(failure, printer);
    throw failure;
  }
}
