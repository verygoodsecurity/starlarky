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

package com.google.devtools.build.lib.starlarkbuildapi.cpp;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.docgen.annot.DocCategory;
import com.google.devtools.build.lib.cmdline.Label;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkValue;

/** The C++ configuration fragment. */
@StarlarkBuiltin(
    name = "cpp",
    doc = "A configuration fragment for C++.",
    category = DocCategory.CONFIGURATION_FRAGMENT)
public interface CppConfigurationApi<InvalidConfigurationExceptionT extends Exception>
    extends StarlarkValue {

  @StarlarkMethod(
      name = "copts",
      structField = true,
      doc =
          "The flags passed to Bazel by <a href=\"../../user-manual.html#flag--copt\">"
              + "<code>--copt</code></a> option.")
  ImmutableList<String> getCopts() throws EvalException;

  @StarlarkMethod(
      name = "cxxopts",
      structField = true,
      doc =
          "The flags passed to Bazel by <a href=\"../../user-manual.html#flag--cxxopt\">"
              + "<code>--cxxopt</code></a> option.")
  ImmutableList<String> getCxxopts() throws EvalException;

  @StarlarkMethod(
      name = "conlyopts",
      structField = true,
      doc =
          "The flags passed to Bazel by <a href=\"../../user-manual.html#flag--conlyopt\">"
              + "<code>--conlyopt</code></a> option.")
  ImmutableList<String> getConlyopts() throws EvalException;

  @StarlarkMethod(
      name = "linkopts",
      structField = true,
      doc =
          "The flags passed to Bazel by <a href=\"../../user-manual.html#flag--linkopt\">"
              + "<code>--linkopt</code></a> option.")
  ImmutableList<String> getLinkopts() throws EvalException;

  @StarlarkMethod(
      name = "custom_malloc",
      structField = true,
      doc =
          "Returns label pointed to by <a href=\"../../user-manual.html#flag--custom_malloc\">"
              + "<code>--custom_malloc</code></a> option. Can be accessed with"
              + " <a href=\"globals.html#configuration_field\"><code>configuration_field"
              + "</code></a>:<br/>"
              + "<pre>attr.label(<br/>"
              + "    default = configuration_field(<br/>"
              + "        fragment = \"cpp\",<br/>"
              + "        name = \"custom_malloc\"<br/>"
              + "    )<br/>"
              + ")</pre>")
  Label customMalloc();
}
