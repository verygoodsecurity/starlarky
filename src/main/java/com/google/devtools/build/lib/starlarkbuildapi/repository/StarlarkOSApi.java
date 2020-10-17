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

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.docgen.annot.DocCategory;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

/** A Starlark structure to deliver information about the system we are running on. */
@StarlarkBuiltin(
    name = "repository_os",
    category = DocCategory.BUILTIN,
    doc = "Various data about the current platform Bazel is running on.")
public interface StarlarkOSApi extends StarlarkValue {
  @StarlarkMethod(name = "environ", structField = true, doc = "The list of environment variables.")
  ImmutableMap<String, String> getEnvironmentVariables();

  @StarlarkMethod(
      name = "name",
      structField = true,
      doc = "A string identifying the current system Bazel is running on.")
  String getName();
}
