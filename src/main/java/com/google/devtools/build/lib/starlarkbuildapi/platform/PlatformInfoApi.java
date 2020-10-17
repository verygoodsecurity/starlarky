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
import com.google.devtools.build.docgen.annot.StarlarkConstructor;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.packages.semantics.BuildLanguageOptions;
import com.google.devtools.build.lib.starlarkbuildapi.core.ProviderApi;
import com.google.devtools.build.lib.starlarkbuildapi.core.StructApi;
import java.util.Map;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.StarlarkThread;

/** Info object representing data about a specific platform. */
@StarlarkBuiltin(
    name = "PlatformInfo",
    doc =
        "Provides access to data about a specific platform. See "
            + "<a href='../../platforms.html#defining-constraints-and-platforms'>Defining "
            + "Constraints and Platforms</a> for more information."
            + PlatformInfoApi.EXPERIMENTAL_WARNING,
    category = DocCategory.PROVIDER)
public interface PlatformInfoApi<
        ConstraintSettingInfoT extends ConstraintSettingInfoApi,
        ConstraintValueInfoT extends ConstraintValueInfoApi>
    extends StructApi {

  String EXPERIMENTAL_WARNING =
      "<br/><i>Note: This API is experimental and may change at any time. It is disabled by"
          + " default, but may be enabled with <code>--experimental_platforms_api</code></i>";

  @StarlarkMethod(
      name = "label",
      doc = "The label of the target that created this platform.",
      structField = true,
      enableOnlyWithFlag = BuildLanguageOptions.EXPERIMENTAL_PLATFORMS_API)
  Label label();

  @StarlarkMethod(
      name = "constraints",
      doc =
          "The <a href=\"ConstraintValueInfo.html\">ConstraintValueInfo</a> instances that define "
              + "this platform.",
      structField = true,
      enableOnlyWithFlag = BuildLanguageOptions.EXPERIMENTAL_PLATFORMS_API)
  ConstraintCollectionApi<ConstraintSettingInfoT, ConstraintValueInfoT> constraints();

  @StarlarkMethod(
      name = "remoteExecutionProperties",
      doc = "Properties that are available for the use of remote execution.",
      structField = true,
      enableOnlyWithFlag = BuildLanguageOptions.EXPERIMENTAL_PLATFORMS_API)
  String remoteExecutionProperties();

  @StarlarkMethod(
      name = "exec_properties",
      doc = "Properties to configure a remote execution platform.",
      structField = true,
      enableOnlyWithFlag = BuildLanguageOptions.EXPERIMENTAL_PLATFORMS_API)
  Map<String, String> execProperties();

  /** Provider for {@link PlatformInfoApi} objects. */
  @StarlarkBuiltin(name = "Provider", documented = false, doc = "")
  interface Provider<
          ConstraintSettingInfoT extends ConstraintSettingInfoApi,
          ConstraintValueInfoT extends ConstraintValueInfoApi,
          PlatformInfoT extends PlatformInfoApi<ConstraintSettingInfoT, ConstraintValueInfoT>>
      extends ProviderApi {

    @StarlarkMethod(
        name = "PlatformInfo",
        doc = "The <code>PlatformInfo</code> constructor.",
        documented = false,
        parameters = {
          @Param(
              name = "label",
              type = Label.class,
              named = true,
              positional = false,
              doc = "The label for this platform."),
          @Param(
              name = "parent",
              type = PlatformInfoApi.class,
              defaultValue = "None",
              named = true,
              positional = false,
              noneable = true,
              doc = "The parent of this platform."),
          @Param(
              name = "constraint_values",
              type = Sequence.class,
              defaultValue = "[]",
              generic1 = ConstraintValueInfoApi.class,
              named = true,
              positional = false,
              doc = "The constraint values for the platform"),
          @Param(
              name = "exec_properties",
              type = Dict.class,
              defaultValue = "None",
              named = true,
              positional = false,
              noneable = true,
              doc = "The exec properties for the platform.")
        },
        selfCall = true,
        useStarlarkThread = true,
        enableOnlyWithFlag = BuildLanguageOptions.EXPERIMENTAL_PLATFORMS_API)
    @StarlarkConstructor
    PlatformInfoT platformInfo(
        Label label,
        Object parent,
        Sequence<?> constraintValues,
        Object execProperties,
        StarlarkThread thread)
        throws EvalException;
  }
}
