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

package com.google.devtools.build.lib.starlarkbuildapi.apple;

import com.google.devtools.build.docgen.annot.DocCategory;
import com.google.devtools.build.lib.starlarkbuildapi.FileApi;
import com.google.devtools.build.lib.starlarkbuildapi.core.StructApi;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;

/**
 * A provider containing the executable binary output that was built using an apple_binary target
 * with the 'loadable_bundle' type.
 */
@StarlarkBuiltin(
    name = "AppleLoadableBundleBinary",
    category = DocCategory.PROVIDER,
    doc =
        "A provider containing the executable binary output that was built using an "
            + "apple_binary target with the 'loadable_bundle' type.")
public interface AppleLoadableBundleBinaryApi extends StructApi {

  @StarlarkMethod(
      name = "binary",
      structField = true,
      doc = "The binary file output by apple_binary.")
  FileApi getAppleLoadableBundleBinary();

  @StarlarkMethod(
      name = "objc",
      structField = true,
      doc =
          "A provider which contains information about the transitive dependencies linked into "
              + "the binary.")
  ObjcProviderApi<?> getDepsObjcProvider();
}
