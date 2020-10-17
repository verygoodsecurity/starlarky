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

package com.google.devtools.build.lib.starlarkbuildapi.platform;

import com.google.devtools.build.docgen.annot.DocCategory;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.starlarkbuildapi.core.StructApi;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;

/** Info object representing data about a specific toolchain type. */
@StarlarkBuiltin(
    name = "ToolchainTypeInfo",
    doc =
        "Provides access to data about a specific toolchain type. "
            + PlatformInfoApi.EXPERIMENTAL_WARNING,
    category = DocCategory.PROVIDER)
public interface ToolchainTypeInfoApi extends StructApi {

  @StarlarkMethod(
      name = "type_label",
      doc = "The label uniquely identifying this toolchain type.",
      structField = true)
  Label typeLabel();
}
