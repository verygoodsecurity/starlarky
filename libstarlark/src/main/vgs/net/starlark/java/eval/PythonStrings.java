// Copyright 2026 Very Good Security Authors. All rights reserved.
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

/**
 * Python 3 behaviour for string methods, for Larky. {@link StringModule} calls in here when one of
 * the semantics flags below is set; with none set, strings behave as in Bazel's Starlark.
 *
 * <p>{@link #PYTHON_STRING_BOUNDS}: {@code find rfind index rindex count startswith endswith}
 * treat a {@code start} past {@code end} (after Python's index adjustment) as an empty range that
 * matches nothing, not even {@code ""}. Starlark instead clamps {@code start} to the slice, as the
 * spec's indexing conventions say, so {@code "abc".find("", 4)} is 3 in Starlark (and in
 * starlark-go) but -1 in Python.
 */
public final class PythonStrings {

  private PythonStrings() {}

  /** Semantics flag: out-of-range {@code start}/{@code end} in string searches follow Python. */
  public static final String PYTHON_STRING_BOUNDS = "-python_string_bounds";

  static boolean bounds(StarlarkThread thread) {
    return thread.getSemantics().getBool(PYTHON_STRING_BOUNDS);
  }

  /**
   * Reports whether {@code s[start:end]} is an empty range that Python's string searches treat as
   * matching nothing: after CPython's ADJUST_INDICES, which clamps {@code end} to the string but
   * not {@code start}, {@code start > end}. Starlark would instead search an empty slice there, and
   * find {@code ""}.
   */
  static boolean emptySearchRange(String s, Object start, Object end) throws EvalException {
    int n = s.length();
    long istart = start == Starlark.NONE ? 0 : Starlark.toInt(start, "start");
    long iend = end == Starlark.NONE ? n : Starlark.toInt(end, "end");
    if (istart < 0) {
      istart = Math.max(0, istart + n);
    }
    if (iend > n) {
      iend = n;
    } else if (iend < 0) {
      iend = Math.max(0, iend + n);
    }
    return istart > iend;
  }

  /**
   * As {@link #emptySearchRange(String, Object, Object)}, for startswith/endswith, whose {@code
   * sub} (a string or a tuple of strings) is checked as the full method would.
   */
  static boolean emptySearchRange(String s, Object sub, Object start, Object end)
      throws EvalException {
    if (!emptySearchRange(s, start, end)) {
      return false;
    }
    if (!(sub instanceof String)) {
      Sequence.cast(sub, String.class, "sub");
    }
    return true;
  }
}
