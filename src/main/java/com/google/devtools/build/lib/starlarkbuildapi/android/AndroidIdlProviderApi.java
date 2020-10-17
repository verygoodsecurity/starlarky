// Copyright 2017 The Bazel Authors. All rights reserved.
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
import com.google.devtools.build.lib.collect.nestedset.Depset;
import com.google.devtools.build.lib.starlarkbuildapi.FileApi;
import com.google.devtools.build.lib.starlarkbuildapi.core.ProviderApi;
import com.google.devtools.build.lib.starlarkbuildapi.core.StructApi;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;

/**
 * Configured targets implementing this provider can contribute Android IDL information to the
 * compilation.
 */
@StarlarkBuiltin(
    name = "AndroidIdlInfo",
    doc =
        "Do not use this module. It is intended for migration purposes only. If you depend on it, "
            + "you will be broken when it is removed."
            + "Information about Android IDLs",
    documented = false,
    category = DocCategory.PROVIDER)
public interface AndroidIdlProviderApi<FileT extends FileApi> extends StructApi {

  /** Name of this info object. */
  String NAME = "AndroidIdlInfo";

  /** The set of IDL import roots need for compiling the IDL sources in the transitive closure. */
  @StarlarkMethod(
      name = "transitive_idl_import_roots",
      structField = true,
      doc = "Returns a depset of strings of all the idl import roots.",
      documented = false)
  Depset /*<String>*/ getTransitiveIdlImportRootsForStarlark();

  /** The IDL files in the transitive closure. */
  @StarlarkMethod(
      name = "transitive_idl_imports",
      structField = true,
      doc = "Returns a depset of artifacts of all the idl imports.",
      documented = false)
  Depset /*<FileT>*/ getTransitiveIdlImportsForStarlark();

  /** The IDL jars in the transitive closure, both class and source jars. */
  @StarlarkMethod(
      name = "transitive_idl_jars",
      structField = true,
      doc = "Returns a depset of artifacts of all the idl class and source jars.",
      documented = false)
  Depset /*<FileT>*/ getTransitiveIdlJarsForStarlark();

  /** The preprocessed IDL files in the transitive closure. */
  @StarlarkMethod(
      name = "transitive_idl_preprocessed",
      structField = true,
      doc = "Returns a depset of artifacts of all the idl preprocessed files.",
      documented = false)
  Depset /*<FileT>*/ getTransitiveIdlPreprocessedForStarlark();

  /** The provider implementing this can construct the AndroidIdlInfo provider. */
  @StarlarkBuiltin(
      name = "Provider",
      doc =
          "Do not use this module. It is intended for migration purposes only. If you depend on "
              + "it, you will be broken when it is removed.",
      documented = false)
  interface Provider<FileT extends FileApi> extends ProviderApi {

    @StarlarkMethod(
        name = NAME,
        doc = "The <code>AndroidIdlInfo</code> constructor.",
        documented = false,
        parameters = {
          @Param(
              name = "transitive_idl_import_roots",
              doc = "A depset of strings of all the idl import roots in the transitive closure.",
              positional = true,
              named = false,
              type = Depset.class,
              generic1 = String.class),
          @Param(
              name = "transitive_idl_imports",
              doc = "A depset of artifacts of all the idl imports in the transitive closure.",
              positional = true,
              named = false,
              type = Depset.class,
              generic1 = FileApi.class),
          @Param(
              name = "transitive_idl_jars",
              doc =
                  "A depset of artifacts of all the idl class and source jars in the "
                      + "transitive closure.",
              positional = true,
              named = false,
              type = Depset.class,
              generic1 = FileApi.class),
          @Param(
              name = "transitive_idl_preprocessed",
              doc =
                  "A depset of artifacts of all the idl preprocessed files in the transitive "
                      + "closure.",
              positional = true,
              named = false,
              type = Depset.class,
              generic1 = FileApi.class),
        },
        selfCall = true)
    @StarlarkConstructor
    AndroidIdlProviderApi<FileT> createInfo(
        Depset transitiveIdlImportRoots,
        Depset transitiveIdlImports,
        Depset transitiveIdlJars,
        Depset transitiveIdlPreprocessed)
        throws EvalException;
  }
}
