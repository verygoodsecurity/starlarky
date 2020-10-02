// Copyright 2020 The Bazel Authors. All rights reserved.
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

package net.starlark.java.syntax;

import lombok.Builder;

/**
 * FileOptions is a set of options that affect the static processing---scanning, parsing, validation
 * (identifier resolution), and compilation---of a single Starlark file. These options affect the
 * language accepted by the frontend (in effect, the dialect), and "code generation", analogous to
 * the command-line options of a typical compiler.
 *
 * <p>Different files within the same application and even executed within the same thread may be
 * subject to different file options. For example, in Bazel, load statements in WORKSPACE files may
 * need to be interleaved with other statements, whereas in .bzl files, load statements must appear
 * all at the top. A single thread may execute a WORKSPACE file and call functions defined in .bzl
 * files.
 *
 * <p>The {@link #DEFAULT} options represent the desired behavior for new uses of Starlark. It is a
 * goal to keep this set of options small and closed. Each represents a language feature, perhaps a
 * deprecated, obscure, or regrettable one. By contrast, {@link StarlarkSemantics} defines a
 * (soon-to-be) open-ended set of options that affect the dynamic behavior of Starlark threads and
 * (mostly application-defined) built-in functions, and particularly attribute selection operations
  * {@code x.f}.
 */
public abstract class FileOptions {

  /** The default options for Starlark static processing. New clients should use these defaults. */
  public static final FileOptions DEFAULT = fileOptions();

  // Options are presented in phase order: scanner, parser, validator, compiler.

  // --- scanner options ---

  /** Disallow ineffective escape sequences such as {@code \a} when scanning string literals. */
  public abstract boolean restrictStringEscapes();

  // --- validator options ---

  /**
   * During resolution, permit load statements to access private names such as {@code _x}. <br>
   * (Required for continued support of Bazel "WORKSPACE.resolved" files.)
   */
  public abstract boolean allowLoadPrivateSymbols();

  /**
   * During resolution, permit multiple bindings of top-level variables. <br>
   * (Required for continued support of Bazel BUILD files and Copybara files.)
   */
  public abstract boolean allowToplevelRebinding();

  // TODO(adonovan): implement this option to support the REPL and prelude.
  //
  // /**
  //  * During resolution, make load statements bind global variables of the module, not file-local
  //  * variables. (Intended for use in REPLs, and the prelude.)
  //  */
  // public abstract boolean loadBindsGlobally();

  /**
   * During resolution, require load statements to appear before other kinds of statements. <br>
   * (Required for continued support of Bazel BUILD and especially WORKSPACE files.)
   */
  public abstract boolean requireLoadStatementsFirst();

  /**
   * Record the results of name resolution in the syntax tree by setting {@code Identifer.scope}.
   * (Disabled for Bazel BUILD files, as its prelude's syntax trees are shared.)
   */
  public abstract boolean recordScope();

  public static FileOptions fileOptions() {
    return new DefaultFileOptions();
  }
}

class DefaultFileOptions extends FileOptions {

  @Override
  public boolean restrictStringEscapes() {
    return true;
  }

  @Override
  public boolean allowLoadPrivateSymbols() {
    return false;
  }

  @Override
  public boolean allowToplevelRebinding() {
    return false;
  }

  @Override
  public boolean requireLoadStatementsFirst() {
    return true;
  }

  @Override
  public boolean recordScope() {
    return true;
  }
}
