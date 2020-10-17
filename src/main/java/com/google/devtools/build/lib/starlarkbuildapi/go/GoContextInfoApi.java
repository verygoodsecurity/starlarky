// Copyright 2019 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.starlarkbuildapi.go;

import com.google.devtools.build.docgen.annot.DocCategory;
import com.google.devtools.build.lib.starlarkbuildapi.core.ProviderApi;
import com.google.devtools.build.lib.starlarkbuildapi.core.StructApi;
import net.starlark.java.annot.StarlarkBuiltin;

/** Information about the transitive closure of a target that is relevant to Go compilation. */
@StarlarkBuiltin(
    name = "GoContextInfo",
    doc = "",
    documented = false,
    category = DocCategory.PROVIDER)
public interface GoContextInfoApi extends StructApi {

  /** Provider for GoContextInfo objects. */
  @StarlarkBuiltin(name = "Provider", doc = "", documented = false)
  public interface Provider extends ProviderApi {}
}
