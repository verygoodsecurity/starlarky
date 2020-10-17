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

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.docgen.annot.DocCategory;
import com.google.devtools.build.docgen.annot.StarlarkConstructor;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.collect.nestedset.Depset;
import com.google.devtools.build.lib.starlarkbuildapi.FileApi;
import com.google.devtools.build.lib.starlarkbuildapi.core.ProviderApi;
import com.google.devtools.build.lib.starlarkbuildapi.core.StructApi;
import javax.annotation.Nullable;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;

/** Provides information about transitive Android assets. */
@StarlarkBuiltin(
    name = "AndroidAssetsInfo",
    doc =
        "Do not use this module. It is intended for migration purposes only. If you depend on it, "
            + "you will be broken when it is removed."
            + "Information about the Android assets provided by a rule.",
    documented = false,
    category = DocCategory.PROVIDER)
public interface AndroidAssetsInfoApi<FileT extends FileApi, AssetsT extends ParsedAndroidAssetsApi>
    extends StructApi {

  /** The name of the provider for this info object. */
  String NAME = "AndroidAssetsInfo";

  @StarlarkMethod(name = "label", structField = true, doc = "", documented = false)
  Label getLabel();

  @StarlarkMethod(
      name = "validation_result",
      structField = true,
      allowReturnNones = true,
      doc =
          "If not None, represents the output of asset merging and validation for this target. The"
              + " action to merge and validate assets is not run be default; to force it, add this"
              + " artifact to your target's outputs. The validation action is somewhat expensive -"
              + " in native code, this artifact is added to the top-level output group (so"
              + " validation is only done if the target is requested on the command line). The"
              + " contents of this artifact are subject to change and should not be relied upon.",
      documented = false)
  @Nullable
  FileApi getValidationResult();

  @StarlarkMethod(name = "direct_parsed_assets", structField = true, doc = "", documented = false)
  Depset /*<AssetsT>*/ getDirectParsedAssetsForStarlark();

  /** Returns the local assets for the target. */
  @StarlarkMethod(
      name = "local_assets",
      doc = "Returns the local assets for the target.",
      documented = false,
      allowReturnNones = true,
      structField = true)
  ImmutableList<FileT> getLocalAssets();

  /** Returns the local asset dir for the target. */
  @StarlarkMethod(
      name = "local_asset_dir",
      doc = "Returns the local asset directory for the target.",
      documented = false,
      allowReturnNones = true,
      structField = true)
  String getLocalAssetDir();

  @StarlarkMethod(
      name = "transitive_parsed_assets",
      structField = true,
      doc = "",
      documented = false)
  Depset /*<AssetsT>*/ getTransitiveParsedAssetsForStarlark();

  @StarlarkMethod(name = "assets", structField = true, doc = "", documented = false)
  Depset /*<FileT>*/ getAssetsForStarlark();

  @StarlarkMethod(name = "symbols", structField = true, doc = "", documented = false)
  Depset /*<FileT>*/ getSymbolsForStarlark();

  @StarlarkMethod(name = "compiled_symbols", structField = true, doc = "", documented = false)
  Depset /*<FileT>*/ getCompiledSymbolsForStarlark();

  /** The provider implementing this can construct the AndroidAssetsInfo provider. */
  @StarlarkBuiltin(
      name = "Provider",
      doc =
          "Do not use this module. It is intended for migration purposes only. If you depend on "
              + "it, you will be broken when it is removed.",
      documented = false)
  interface Provider<FileT extends FileApi, AssetsT extends ParsedAndroidAssetsApi>
      extends ProviderApi {

    @StarlarkMethod(
        name = NAME,
        doc = "The <code>AndroidAssetsInfo</code> constructor.",
        documented = false,
        parameters = {
          @Param(
              name = "label",
              doc = "The label of the target.",
              positional = true,
              named = false,
              type = Label.class),
          @Param(
              name = "validation_result",
              doc = "An artifact of the validation result.",
              positional = true,
              named = true,
              noneable = true,
              type = FileApi.class),
          @Param(
              name = "direct_parsed_assets",
              doc = "A depset of all the parsed assets in the target.",
              positional = true,
              named = true,
              type = Depset.class,
              generic1 = ParsedAndroidAssetsApi.class),
          @Param(
              name = "transitive_parsed_assets",
              doc = "A depset of all the parsed assets in the transitive closure.",
              positional = true,
              named = true,
              type = Depset.class,
              generic1 = ParsedAndroidAssetsApi.class),
          @Param(
              name = "transitive_assets",
              doc = "A depset of all the assets in the transitive closure.",
              positional = true,
              named = true,
              type = Depset.class,
              generic1 = FileApi.class),
          @Param(
              name = "transitive_symbols",
              doc = "A depset of all the symbols in the transitive closure.",
              positional = true,
              named = true,
              type = Depset.class,
              generic1 = FileApi.class),
          @Param(
              name = "transitive_compiled_symbols",
              doc = "A depset of all the compiled symbols in the transitive closure.",
              positional = true,
              named = true,
              type = Depset.class,
              generic1 = FileApi.class),
        },
        selfCall = true)
    @StarlarkConstructor
    AndroidAssetsInfoApi<FileT, AssetsT> createInfo(
        Label label,
        Object validationResult,
        Depset directParsedAssets,
        Depset transitiveParsedAssets,
        Depset transitiveAssets,
        Depset transitiveSymbols,
        Depset transitiveCompiledSymbols)
        throws EvalException;
  }
}
