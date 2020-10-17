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

package com.google.devtools.build.lib.starlarkbuildapi;

import com.google.devtools.build.docgen.annot.DocCategory;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

/**
 * A root for a file. The roots are the directories containing files, and they are mapped together
 * into a single directory tree to form the execution environment.
 */
@StarlarkBuiltin(
    name = "root",
    category = DocCategory.BUILTIN,
    doc =
        "A root for files. The roots are the directories containing files, and they are mapped "
            + "together into a single directory tree to form the execution environment.")
public interface FileRootApi extends StarlarkValue {
  @StarlarkMethod(
      name = "path",
      structField = true,
      doc = "Returns the relative path from the exec root to the actual root.")
  String getExecPathString();
}
