/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.larky.parser;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Utilities for dealing with Starlark parameter objects and converting them to Java ones.
 */
public final class StarlarkUtil {

  private StarlarkUtil() {
  }

  /**
   * Converts an object that can be the NoneType to the actual object if it is not
   * or returns the default value if none.
   */
  @SuppressWarnings("unchecked")
  public static <T> T convertFromNoneable(Object obj, @Nullable T defaultValue) {
    if (Starlark.isNullOrNone(obj)) {
      return defaultValue;
    }
    return (T) obj; // wildly unsound cast!
  }

  /**
   * Converts a string to the corresponding enum or fail if invalid value.
   *
   * @param fieldName name of the field to convert
   * @param value value to convert
   * @param enumType the type class of the enum to use for conversion
   * @param <T> the enum class
   */
  public static <T extends Enum<T>> T stringToEnum(
      String fieldName, String value, Class<T> enumType) throws EvalException {
    try {
      return Enum.valueOf(enumType, value);
    } catch (IllegalArgumentException e) {
      throw Starlark.errorf(
          "Invalid value '%s' for field '%s'. Valid values are: %s",
          value, fieldName, Joiner.on(", ").join(enumType.getEnumConstants()));
    }
  }

  /** Checks that a mandatory string field is not empty. */
  public static String checkNotEmpty(@Nullable String value, String name) throws EvalException {
    check(!Strings.isNullOrEmpty(value), "Invalid empty field '%s'.", name);
    return value;
  }

  /** Checks a condition or throw {@link EvalException}. */
   /** Checks a condition or throw {@link EvalException}. */
  @FormatMethod
  public static void check(boolean condition, @FormatString String format, Object... args)
      throws EvalException {
    if (!condition) {
      throw Starlark.errorf(format, args);
    }
  }

  /**
   * convertStringList converts a Starlark sequence value (such as a list or tuple) to a Java list
   * of strings. The result is a new, mutable copy. It throws EvalException if x is not a Starlark
   * iterable or if any of its elements are not strings. The message argument is prefixed to any
   * error message.
   */
  public static List<String> convertStringList(Object x, String message) throws EvalException {
    // TODO(adonovan): replace all calls to this function with:
    //  Sequence.cast(x, String.class, message).
    // But beware its result should not be modified.
    if (!(x instanceof Sequence)) {
      throw Starlark.errorf("%s: got %s, want sequence", message, Starlark.type(x));
    }

    ArrayList<String> result = new ArrayList<>();
    for (Object elem : (Sequence<?>) x) {
      if (!(elem instanceof String)) {
        throw Starlark.errorf(
            "%s: at index #%d, got %s, want string", message, result.size(), Starlark.type(elem));
      }
      result.add((String) elem);
    }
    return result;
  }

  /**
   * convertStringMap converts a Starlark dict value to a Java map of strings to strings. The result
   * is a new, mutable copy. It throws EvalException if x is not a Starlark dict or if any of its
   * keys or values are not strings. The message argument is prefixed to any error message.
   */
  public static Map<String, String> convertStringMap(Object x, String message)
      throws EvalException {
    // TODO(adonovan): replace all calls to this function with:
    //    Dict.cast(x, String.class, String.class, message)
    // and fix up tests. Beware: its result is not to be modified.
    if (!(x instanceof Dict)) {
      throw Starlark.errorf("%s: got %s, want dict", message, Starlark.type(x));
    }
    Map<String, String> result = new HashMap<>();
    for (Map.Entry<?, ?> e : ((Dict<?, ?>) x).entrySet()) {
      if (!(e.getKey() instanceof String)) {
        throw Starlark.errorf(
            "%s: in dict key, got %s, want string", message, Starlark.type(e.getKey()));
      }
      if (!(e.getValue() instanceof String)) {
        throw Starlark.errorf(
            "%s: in value for dict key '%s', got %s, want string",
            message, e.getKey(), Starlark.type(e.getValue()));
      }
      result.put((String) e.getKey(), (String) e.getValue());
    }
    return result;
  }

  /**
   * convertOptionalString converts a Starlark optional string value (string or None) to a Java
   * String reference, which may be null. It throws ClassCastException if called with any other
   * value.
   */
  @Nullable
  public static String convertOptionalString(Object x) {
    return x == Starlark.NONE ? null : (String) x;
  }

  public static Object valueToStarlark(Object x) throws EvalException {
      // Is x a non-empty string_list_dict?
      if (x instanceof Map) {
        Map<?, ?> map = (Map<?,?>) x;
        if (!map.isEmpty() && map.values().iterator().next() instanceof List) {
          // Recursively convert subelements.
          Dict<Object, Object> dict = Dict.of(null);
          for (Map.Entry<?, ?> e : map.entrySet()) {
            dict.putEntry(
                e.getKey(),
                Starlark.fromJava(e.getValue(),null));
          }
          return dict;
        }
      }
      // For all other attribute values, shallow conversion is safe.
      return Starlark.fromJava(x, null);
    }
}
