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

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.docgen.annot.DocCategory;
import com.google.devtools.build.docgen.annot.StarlarkConstructor;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.starlarkbuildapi.core.ProviderApi;
import com.google.devtools.build.lib.starlarkbuildapi.core.StructApi;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;

/** */
@StarlarkBuiltin(
    name = "AndroidFeatureFlagSetInfo",
    doc =
        "Do not use this module. It is intended for migration purposes only. If you depend on it, "
            + "you will be broken when it is removed."
            + "Information about the android_binary feature flags",
    documented = false,
    category = DocCategory.PROVIDER)
public interface AndroidFeatureFlagSetProviderApi extends StructApi {

  /** The name of the provider for this info object. */
  String NAME = "AndroidFeatureFlagSet";

  @StarlarkMethod(
      name = "flags",
      doc = "Returns the flags contained by the provider.",
      documented = false,
      structField = true)
  ImmutableMap<Label, String> getFlagMap();

  /** The provider implementing this can construct the AndroidIdeInfo provider. */
  @StarlarkBuiltin(
      name = "Provider",
      doc =
          "Do not use this module. It is intended for migration purposes only. If you depend on "
              + "it, you will be broken when it is removed.",
      documented = false)
  interface Provider extends ProviderApi {

    @StarlarkMethod(
        name = NAME,
        doc = "The <code>AndroidFeatureFlagSetProvider</code> constructor.",
        documented = false,
        parameters = {
          @Param(
              name = "flags",
              doc = "Map of flags",
              positional = true,
              named = false,
              type = Dict.class),
        },
        selfCall = true)
    @StarlarkConstructor
    AndroidFeatureFlagSetProviderApi create(Dict<?, ?> flags /* <Label, String> */)
        throws EvalException;
  }
}
