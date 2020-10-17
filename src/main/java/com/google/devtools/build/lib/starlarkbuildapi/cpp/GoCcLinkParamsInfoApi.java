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

package com.google.devtools.build.lib.starlarkbuildapi.cpp;

import com.google.devtools.build.docgen.annot.DocCategory;
import com.google.devtools.build.docgen.annot.StarlarkConstructor;
import com.google.devtools.build.lib.starlarkbuildapi.FileApi;
import com.google.devtools.build.lib.starlarkbuildapi.core.ProviderApi;
import com.google.devtools.build.lib.starlarkbuildapi.core.StructApi;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;

/** A target that provides C++ libraries to be linked into Go targets. */
@StarlarkBuiltin(
    name = "GoCcLinkParamsInfo",
    doc = "",
    documented = false,
    category = DocCategory.PROVIDER)
public interface GoCcLinkParamsInfoApi extends StructApi {

  /** Provider for GoContextInfo objects. */
  @StarlarkBuiltin(name = "Provider", doc = "", documented = false)
  public interface Provider<
          FileT extends FileApi, CcLinkingContextT extends CcLinkingContextApi<FileT>>
      extends ProviderApi {
    @StarlarkMethod(
        name = "GoCcLinkParamsInfo",
        doc = "The <code>GoCcLinkParamsInfo</code> constructor.",
        parameters = {
          @Param(
              name = "linking_context",
              doc = "The CC linking context.",
              positional = false,
              named = true,
              type = CcLinkingContextApi.class),
        },
        selfCall = true)
    @StarlarkConstructor
    public GoCcLinkParamsInfoApi createInfo(CcLinkingContextT ccLinkingContext)
        throws EvalException;
  }
}
