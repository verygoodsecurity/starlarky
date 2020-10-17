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

package com.google.devtools.build.lib.starlarkbuildapi.java;

import com.google.devtools.build.docgen.annot.DocCategory;
import com.google.devtools.build.docgen.annot.StarlarkConstructor;
import com.google.devtools.build.lib.packages.semantics.BuildLanguageOptions;
import com.google.devtools.build.lib.starlarkbuildapi.FileApi;
import com.google.devtools.build.lib.starlarkbuildapi.core.ProviderApi;
import com.google.devtools.build.lib.starlarkbuildapi.cpp.CcInfoApi;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkValue;

/** A target that provides C++ libraries to be linked into Java targets. */
@StarlarkBuiltin(
    name = "JavaCcLinkParamsInfo",
    doc =
        "Do not use this module. It is intended for migration purposes only. If you depend on it, "
            + "you will be broken when it is removed."
            + "Information about the c++ libraries to be linked into Java targets.",
    documented = true,
    category = DocCategory.PROVIDER)
public interface JavaCcLinkParamsProviderApi<
        FileT extends FileApi, CcInfoApiT extends CcInfoApi<FileT>>
    extends StarlarkValue {
  /** Name of this info object. */
  String NAME = "JavaCcLinkParamsInfo";

  /** Returns the cc linking info */
  @StarlarkMethod(
      name = "cc_info",
      structField = true,
      doc = "Returns the CcLinkingInfo provider.",
      documented = true,
      enableOnlyWithFlag = BuildLanguageOptions.EXPERIMENTAL_ENABLE_ANDROID_MIGRATION_APIS)
  CcInfoApiT getCcInfo();

  /** The provider implementing this can construct the JavaCcLinkParamsInfo provider. */
  @StarlarkBuiltin(
      name = "Provider",
      doc =
          "Do not use this module. It is intended for migration purposes only. If you depend on "
              + "it, you will be broken when it is removed.",
      documented = false)
  public interface Provider<FileT extends FileApi, CcInfoApiT extends CcInfoApi<FileT>>
      extends ProviderApi {

    @StarlarkMethod(
        name = NAME,
        doc = "The <code>JavaCcLinkParamsInfo</code> constructor.",
        documented = true,
        enableOnlyWithFlag = BuildLanguageOptions.EXPERIMENTAL_ENABLE_ANDROID_MIGRATION_APIS,
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
    public JavaCcLinkParamsProviderApi<FileT, CcInfoApiT> createInfo(CcInfoApiT store)
        throws EvalException;
  }
}
