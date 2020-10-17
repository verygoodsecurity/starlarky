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

package com.google.devtools.build.lib.starlarkbuildapi;

import com.google.devtools.build.docgen.annot.DocumentMethods;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkThread;

/** A collection of global Starlark build API functions that belong in the global namespace. */
@DocumentMethods
public interface StarlarkBuildApiGlobals {

  @StarlarkMethod(
      name = "configuration_field",
      // TODO(cparsons): Provide a link to documentation for available StarlarkConfigurationFields.
      doc =
          "References a late-bound default value for an attribute of type "
              + "<a href=\"attr.html#label\">label</a>. A value is 'late-bound' if it requires "
              + "the configuration to be built before determining the value. Any attribute using "
              + "this as a value must <a href=\"../rules.html#private-attributes\">be private</a>. "
              + "<p>Example usage: "
              + "<p>Defining a rule attribute: <br><pre class=language-python>"
              + "'_foo': attr.label(default=configuration_field(fragment='java', "
              + "name='toolchain'))</pre>"
              + "<p>Accessing in rule implementation: <br><pre class=language-python>"
              + "  def _rule_impl(ctx):\n"
              + "    foo_info = ctx.attr._foo\n"
              + "    ...</pre>",
      parameters = {
        @Param(
            name = "fragment",
            type = String.class,
            named = true,
            doc = "The name of a configuration fragment which contains the late-bound value."),
        @Param(
            name = "name",
            type = String.class,
            named = true,
            doc = "The name of the value to obtain from the configuration fragment."),
      },
      useStarlarkThread = true)
  LateBoundDefaultApi configurationField(String fragment, String name, StarlarkThread thread)
      throws EvalException;
}
