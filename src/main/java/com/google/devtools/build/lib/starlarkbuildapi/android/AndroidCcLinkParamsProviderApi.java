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

package com.google.devtools.build.lib.starlarkbuildapi.android;

import com.google.devtools.build.docgen.annot.DocCategory;
import com.google.devtools.build.docgen.annot.StarlarkConstructor;
import com.google.devtools.build.lib.starlarkbuildapi.FileApi;
import com.google.devtools.build.lib.starlarkbuildapi.core.ProviderApi;
import com.google.devtools.build.lib.starlarkbuildapi.core.StructApi;
import com.google.devtools.build.lib.starlarkbuildapi.cpp.CcInfoApi;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;

/** A target that provides C++ libraries to be linked into Android targets. */
@StarlarkBuiltin(
    name = "AndroidCcLinkParamsInfo",
    doc =
        "Do not use this module. It is intended for migration purposes only. If you depend on it, "
            + "you will be broken when it is removed."
            + "Information about the c++ libraries to be linked into Android targets.",
    documented = false,
    category = DocCategory.PROVIDER)
public interface AndroidCcLinkParamsProviderApi<
        FileT extends FileApi, CcInfoT extends CcInfoApi<FileT>>
    extends StructApi {
  /** Name of this info object. */
  String NAME = "AndroidCcLinkParamsInfo";

  /** Returns the cc link params. */
  @StarlarkMethod(name = "link_params", structField = true, doc = "", documented = false)
  CcInfoT getLinkParams();

  /** The provider implementing this can construct the AndroidCcLinkParamsInfo provider. */
  @StarlarkBuiltin(
      name = "Provider",
      doc =
          "Do not use this module. It is intended for migration purposes only. If you depend on "
              + "it, you will be broken when it is removed.",
      documented = false)
  interface Provider<FileT extends FileApi, CcInfoT extends CcInfoApi<FileT>> extends ProviderApi {

    @StarlarkMethod(
        name = NAME,
        doc = "The <code>AndroidCcLinkParamsInfo</code> constructor.",
        documented = false,
        parameters = {
          @Param(
              name = "store",
              doc = "The CcInfo provider.",
              positional = true,
              named = false,
              type = CcInfoApi.class),
        },
        selfCall = true)
    @StarlarkConstructor
    public AndroidCcLinkParamsProviderApi<FileT, CcInfoT> createInfo(CcInfoT store)
        throws EvalException;
  }
}
