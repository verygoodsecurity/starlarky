// Copyright 2026 Very Good Security Authors
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

import net.starlark.java.syntax.TokenKind;

/**
 * Starlark operators for Java code that implements a Starlark type: the same evaluation as the
 * statement or expression, without running Starlark code to reach it.
 */
public final class StarlarkOperators {

  private StarlarkOperators() {}

  /** {@code object[key] = value}. */
  public static void setIndex(StarlarkThread thread, Object object, Object key, Object value)
      throws EvalException {
    EvalUtils.setIndex(thread, object, key, value);
  }

  /** {@code key in container}. */
  public static boolean in(StarlarkThread thread, Object key, Object container)
      throws EvalException {
    return (Boolean) EvalUtils.binaryOp(TokenKind.IN, key, container, thread);
  }

  /**
   * {@code getattr(object, name, defaultValue)}: the attribute as the expression {@code
   * object.name} reads it, with the thread, or {@code defaultValue} if there is none. With a null
   * default it fails as the expression does.
   */
  public static Object getattr(
      StarlarkThread thread, Object object, String name, @javax.annotation.Nullable Object defaultValue)
      throws EvalException, InterruptedException {
    return Starlark.getattr(thread, object, name, defaultValue);
  }

  /** {@code object.name = value}. */
  public static void setattr(StarlarkThread thread, Object object, String name, Object value)
      throws EvalException, InterruptedException {
    EvalUtils.setField(thread, object, name, value);
  }
}
