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
package com.google.devtools.build.lib.analysis.config;

import com.google.devtools.common.options.EnumConverter;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.StarlarkValue;

/** This class represents the debug/optimization mode the binaries will be built for. */
// TODO(bazel-team): Implementing StarlarkValue is a workaround until a well-defined Java-Starlark
// conversion interface has been created. Avoid replicating this workaround.
// See also https://github.com/bazelbuild/bazel/pull/11347#issuecomment-630260102
public enum CompilationMode implements StarlarkValue {

  // Fast build mode (-g0).
  FASTBUILD("fastbuild"),
  // Debug mode (-g).
  DBG("dbg"),
  // Release mode (-g0 -O2 -DNDEBUG).
  OPT("opt");

  private final String mode;

  private CompilationMode(String mode) {
    this.mode = mode;
  }

  @Override
  public String toString() {
    return mode;
  }

  /**
   * Converts to {@link CompilationMode}.
   */
  public static class Converter extends EnumConverter<CompilationMode> {
    public Converter() {
      super(CompilationMode.class, "compilation mode");
    }
  }

  @Override
  public void repr(Printer printer) {
    printer.append(toString());
  }
}
