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

package com.google.devtools.build.lib.starlarkbuildapi.repository;

import com.google.devtools.build.docgen.annot.DocCategory;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

/**
 * A structure callable from Starlark that stores the result of repository_ctx.execute() method. It
 * contains the standard output stream content, the standard error stream content and the execution
 * return code.
 */
@StarlarkBuiltin(
    name = "exec_result",
    category = DocCategory.BUILTIN,
    doc =
        "A structure storing result of repository_ctx.execute() method. It contains the standard"
            + " output stream content, the standard error stream content and the execution return"
            + " code.")
public interface StarlarkExecutionResultApi extends StarlarkValue {
  @StarlarkMethod(
      name = "return_code",
      structField = true,
      doc =
          "The return code returned after the execution of the program. 256 if the process was"
              + " terminated by a time out; values larger than 128 indicate termination by a"
              + " signal.")
  int getReturnCode();

  @StarlarkMethod(
      name = "stdout",
      structField = true,
      doc = "The content of the standard output returned by the execution.")
  String getStdout();

  @StarlarkMethod(
      name = "stderr",
      structField = true,
      doc = "The content of the standard error output returned by the execution.")
  String getStderr();
}
