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

import com.google.devtools.build.lib.collect.nestedset.Depset;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

/** Provider for the runtime classpath contributions of a Java binary. */
@StarlarkBuiltin(name = "JavaRuntimeClasspathProvider", doc = "", documented = false)
public interface JavaRuntimeClasspathProviderApi extends StarlarkValue {

  @StarlarkMethod(name = "runtime_classpath", documented = false, structField = true)
  Depset /*<File>*/ getRuntimeClasspath();
}
