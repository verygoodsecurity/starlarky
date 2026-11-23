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

import java.util.IllegalFormatException;

/**
 * String operations exactly as Starlark code performs them, for native code that must give the
 * same results as the {@code .star} code it replaces.
 */
public final class StarlarkStrings {

  private StarlarkStrings() {}

  /** {@code s.strip()}, under {@code semantics}. */
  public static String strip(String s, StarlarkSemantics semantics) {
    return StringModule.INSTANCE.stripSemantics(s, Starlark.NONE, semantics);
  }

  /** {@code s.lower()}, under the thread's semantics. */
  public static String lower(String s, StarlarkThread thread) {
    return StringModule.INSTANCE.lower(s, thread);
  }

  /** {@code s.capitalize()}, under the thread's semantics. */
  public static String capitalize(String s, StarlarkThread thread) throws EvalException {
    return StringModule.INSTANCE.capitalize(s, thread);
  }

  /** {@code s.title()}, under the thread's semantics. */
  public static String title(String s, StarlarkThread thread) throws EvalException {
    return StringModule.INSTANCE.title(s, thread);
  }

  /**
   * {@code s.title()} under {@code semantics}, for native code that has the semantics but no
   * thread. The same as {@link #title(String, StarlarkThread)}.
   */
  public static String title(String s, StarlarkSemantics semantics) {
    if (PythonStrings.unicode(semantics)) {
      return PythonStrings.title(s);
    }
    // StringModule.title's Starlark rules
    char[] data = s.toCharArray();
    boolean previousWasLetter = false;
    for (int pos = 0; pos < data.length; ++pos) {
      char current = data[pos];
      boolean currentIsLetter = Character.isLetter(current);
      if (currentIsLetter) {
        if (previousWasLetter && Character.isUpperCase(current)) {
          data[pos] = Character.toLowerCase(current);
        } else if (!previousWasLetter && Character.isLowerCase(current)) {
          data[pos] = Character.toUpperCase(current);
        }
      }
      previousWasLetter = currentIsLetter;
    }
    return new String(data);
  }

  /** {@code pattern % operand}, under the thread's semantics. */
  public static String percent(StarlarkThread thread, String pattern, Object operand)
      throws EvalException {
    StarlarkSemantics semantics = thread.getSemantics();
    if (semantics.getBool(PercentFormat.PYTHON_PERCENT_FORMAT)) {
      return PercentFormat.format(thread, pattern, operand);
    }
    try {
      return operand instanceof Tuple tuple
          ? Starlark.formatWithList(semantics, pattern, tuple)
          : Starlark.format(semantics, pattern, operand);
    } catch (IllegalFormatException ex) {
      throw new EvalException(ex);
    }
  }
}
