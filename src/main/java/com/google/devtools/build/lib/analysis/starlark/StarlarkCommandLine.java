// Copyright 2014 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.analysis.starlark;

import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.collect.nestedset.Depset;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.starlarkbuildapi.StarlarkCommandLineApi;
import net.starlark.java.eval.EvalException;

/** A Starlark module class to create memory efficient command lines. */
public class StarlarkCommandLine implements StarlarkCommandLineApi {

  @Override
  public String joinPaths(String separator, Depset files) throws EvalException {
    NestedSet<Artifact> artifacts = Depset.cast(files, Artifact.class, "files");
    // TODO(bazel-team): This method should be deprecated and strongly discouraged, as it
    // flattens a depset during analysis.
    return Artifact.joinExecPaths(separator, artifacts.toList());
  }
}
