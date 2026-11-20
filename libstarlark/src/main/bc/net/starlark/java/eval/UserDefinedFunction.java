// Copyright 2025 The Bazel Authors. All rights reserved.
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

package net.starlark.java.eval;

import com.google.common.collect.ImmutableList;

/**
 * Interface for user-defined Starlark functions (those defined with 'def').
 *
 * <p>This interface is implemented by both {@link StarlarkFunction} (tree-walking)
 * and {@link BytecodeFunction} (bytecode execution), providing a common API for
 * testing and introspection of function signatures.
 *
 * <p>This allows code to work uniformly with user-defined functions regardless
 * of which execution backend is being used.
 */
public interface UserDefinedFunction extends StarlarkCallable {

  /**
   * Returns the names of all parameters in declaration order.
   *
   * <p>For a function like {@code def f(a, b=1, *args, c, d=2, **kwargs)},
   * this returns {@code ["a", "b", "c", "d", "args", "kwargs"]}.
   *
   * <p>Note: The order puts *args and **kwargs at the end, with keyword-only
   * parameters (c, d) between the positional parameters and *args.
   */
  ImmutableList<String> getParameterNames();

  /**
   * Returns true if this function has a *args parameter.
   */
  boolean hasVarargs();

  /**
   * Returns true if this function has a **kwargs parameter.
   */
  boolean hasKwargs();

  /**
   * Returns the number of ordinary (non-residual, non-keyword-only) parameters. They come first in
   * {@link #getParameterNames}.
   */
  int getNumOrdinaryParameters();

  /**
   * Returns the number of keyword-only parameters: those after *args or a bare *. They follow the
   * ordinary parameters in {@link #getParameterNames}.
   */
  int getNumKeywordOnlyParameters();

  /** Returns the function's identity token (see {@link SymbolGenerator}). */
  SymbolGenerator.Symbol<?> getToken();

  /**
   * Returns the default value for the i-th parameter, or null if the parameter
   * has no default value.
   *
   * @param i the parameter index (0-based)
   * @return the default value, or null if the parameter is required
   */
  Object getDefaultValue(int i);
}
