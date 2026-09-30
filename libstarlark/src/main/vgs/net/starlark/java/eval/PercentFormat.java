// Copyright 2025 Very Good Security Authors. All rights reserved.
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Python's printf-style string formatting, {@code str % args}, for Larky.
 *
 * <p>Starlark's {@code %} accepts only bare conversions ({@code %s %r %d %o %x %X %e %f %g %E %F
 * %G %%}). With {@link #PYTHON_PERCENT_FORMAT} set, {@code str % args} follows Python instead
 * (https://docs.python.org/3/library/stdtypes.html#printf-style-string-formatting): mapping keys
 * ({@code %(name)s}), the flags {@code # 0 - space +}, width and precision (either may be {@code
 * *}), length modifiers (ignored) and the conversions {@code d i o u x X e E f F g G c r s a %}.
 * Floats are rounded as Python rounds them (half to even, on the exact binary value).
 *
 * <p>Differences from Python: {@code %r} and {@code %a} use Starlark's {@code repr} (strings in
 * double quotes); {@code %o %x %X} accept a float and truncate it, as Starlark's {@code %} does;
 * width and precision are limited to {@link #MAX_WIDTH}.
 */
public final class PercentFormat {

  private PercentFormat() {}

  /** Semantics flag: {@code str % args} follows Python's printf-style formatting. */
  public static final String PYTHON_PERCENT_FORMAT = "-python_percent_format";

  /** Largest width or precision: the format string comes from the script. */
  static final int MAX_WIDTH = 1_000_000;

  /** Python's argument state (argidx/arglen in CPython's unicode_format_arg). */
  private static final class Args {
    private final StarlarkThread thread;
    private final Object mapping; // the operand, if it can be indexed by key
    private Object args;
    private int argidx;
    private int arglen;

    Args(StarlarkThread thread, Object operand) {
      this.thread = thread;
      this.args = operand;
      if (operand instanceof Tuple tuple) {
        arglen = tuple.size();
        argidx = 0;
      } else {
        arglen = -1;
        argidx = -2;
      }
      this.mapping =
          !(operand instanceof Tuple) && !(operand instanceof String) && isMapping(operand)
              ? operand
              : null;
    }

    private static boolean isMapping(Object x) {
      return x instanceof Map || x instanceof Sequence || x instanceof StarlarkIndexable;
    }

    Object next() throws EvalException {
      if (argidx < arglen) {
        argidx++;
        return arglen < 0 ? args : ((Tuple) args).get(argidx - 1);
      }
      throw Starlark.errorf("not enough arguments for format string");
    }

    /** Looks up {@code %(key)}: the next conversion formats that value, and only that value. */
    void selectKey(String key) throws EvalException {
      if (mapping == null) {
        throw Starlark.errorf("format requires a mapping");
      }
      args = EvalUtils.index(thread, mapping, key);
      arglen = -1;
      argidx = -2;
    }

    void checkAllConverted() throws EvalException {
      if (argidx < arglen && mapping == null) {
        throw Starlark.errorf("not all arguments converted during string formatting");
      }
    }
  }

  /** Returns {@code pattern % operand}. */
  static String format(StarlarkThread thread, String pattern, Object operand)
      throws EvalException {
    StarlarkSemantics semantics = thread.getSemantics();
    Args args = new Args(thread, operand);
    StringBuilder out = new StringBuilder(pattern.length() + 16);
    int n = pattern.length();
    int i = 0;
    while (i < n) {
      int percent = pattern.indexOf('%', i);
      if (percent < 0) {
        out.append(pattern, i, n);
        break;
      }
      out.append(pattern, i, percent);
      i = percent + 1;
      if (i >= n) {
        throw Starlark.errorf("incomplete format");
      }

      if (pattern.charAt(i) == '(') {
        int depth = 1;
        int start = ++i;
        while (i < n && depth > 0) {
          char c = pattern.charAt(i++);
          if (c == '(') {
            depth++;
          } else if (c == ')') {
            depth--;
          }
        }
        if (depth > 0) {
          throw Starlark.errorf("incomplete format key");
        }
        args.selectKey(pattern.substring(start, i - 1));
      }

      boolean left = false;
      boolean plus = false;
      boolean space = false;
      boolean alt = false;
      boolean zero = false;
      flags:
      for (; i < n; i++) {
        switch (pattern.charAt(i)) {
          case '-' -> left = true;
          case '+' -> plus = true;
          case ' ' -> space = true;
          case '#' -> alt = true;
          case '0' -> zero = true;
          default -> {
            break flags;
          }
        }
      }

      int width = -1;
      if (i < n && pattern.charAt(i) == '*') {
        i++;
        width = starArg(args.next());
        if (width < 0) {
          left = true;
          width = -width;
        }
        checkWidth(width, "width");
      } else {
        int start = i;
        while (i < n && isDigit(pattern.charAt(i))) {
          i++;
        }
        if (i > start) {
          width = parseSize(pattern, start, i, "width");
        }
      }

      int precision = -1;
      if (i < n && pattern.charAt(i) == '.') {
        i++;
        if (i < n && pattern.charAt(i) == '*') {
          i++;
          precision = Math.max(0, starArg(args.next()));
          checkWidth(precision, "precision");
        } else {
          int start = i;
          while (i < n && isDigit(pattern.charAt(i))) {
            i++;
          }
          precision = i > start ? parseSize(pattern, start, i, "precision") : 0;
        }
      }

      while (i < n && (pattern.charAt(i) == 'h' || pattern.charAt(i) == 'l'
          || pattern.charAt(i) == 'L')) {
        i++;
      }
      if (i >= n) {
        throw Starlark.errorf("incomplete format");
      }
      char conv = pattern.charAt(i++);
      if (conv == '%') {
        out.append('%');
        continue;
      }
      Object value = args.next();

      String sign = "";
      String prefix = "";
      String body;
      boolean numeric = true;
      switch (conv) {
        case 'd', 'i', 'u', 'o', 'x', 'X' -> {
          BigInteger v = integerArg(value, conv);
          int radix = conv == 'o' ? 8 : (conv == 'x' || conv == 'X') ? 16 : 10;
          body = v.abs().toString(radix);
          if (conv == 'X') {
            body = body.toUpperCase(java.util.Locale.ROOT);
          }
          if (precision > body.length()) {
            body = "0".repeat(precision - body.length()) + body;
          }
          if (alt && radix != 10) {
            prefix = conv == 'o' ? "0o" : conv == 'x' ? "0x" : "0X";
          }
          sign = v.signum() < 0 ? "-" : plus ? "+" : space ? " " : "";
        }
        case 'e', 'E', 'f', 'F', 'g', 'G' -> {
          double v = floatArg(value, conv);
          boolean upper = Character.isUpperCase(conv);
          boolean negative = !Double.isNaN(v) && (v < 0 || (v == 0 && 1 / v < 0));
          sign = negative ? "-" : plus ? "+" : space ? " " : "";
          double a = Math.abs(v);
          int p = precision < 0 ? 6 : precision;
          if (Double.isNaN(a) || Double.isInfinite(a)) {
            body = Double.isNaN(a) ? "nan" : "inf";
          } else if (conv == 'f' || conv == 'F') {
            body = fixed(a, p, alt);
          } else if (conv == 'e' || conv == 'E') {
            body = exponent(a, p, alt);
          } else {
            body = general(a, p, alt);
          }
          if (upper) {
            body = body.toUpperCase(java.util.Locale.ROOT);
          }
        }
        case 'c' -> {
          numeric = false;
          body = charArg(value);
        }
        case 's', 'r', 'a' -> {
          numeric = false;
          body =
              conv == 's'
                  ? Starlark.str(value, semantics)
                  : conv == 'r'
                      ? Starlark.repr(value, semantics)
                      : ascii(Starlark.repr(value, semantics));
          if (precision >= 0 && precision < body.codePointCount(0, body.length())) {
            body = body.substring(0, body.offsetByCodePoints(0, precision));
          }
        }
        default ->
            throw Starlark.errorf(
                "unsupported format character '%s' (0x%x) at index %d", conv, (int) conv, i - 1);
      }

      int length = sign.length() + prefix.length() + body.codePointCount(0, body.length());
      if (width <= length) {
        out.append(sign).append(prefix).append(body);
      } else if (left) {
        out.append(sign).append(prefix).append(body).append(" ".repeat(width - length));
      } else if (zero && numeric) {
        out.append(sign).append(prefix).append("0".repeat(width - length)).append(body);
      } else {
        out.append(" ".repeat(width - length)).append(sign).append(prefix).append(body);
      }
    }
    args.checkAllConverted();
    return out.toString();
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static int parseSize(String pattern, int start, int end, String what)
      throws EvalException {
    if (end - start > 7) {
      throw Starlark.errorf("%s too big", what);
    }
    int size = Integer.parseInt(pattern, start, end, 10);
    checkWidth(size, what);
    return size;
  }

  private static void checkWidth(int size, String what) throws EvalException {
    if (size > MAX_WIDTH) {
      throw Starlark.errorf("%s too big", what);
    }
  }

  private static int starArg(Object x) throws EvalException {
    if (x instanceof StarlarkInt i) {
      return i.toInt("*");
    }
    throw Starlark.errorf("* wants int");
  }

  private static BigInteger integerArg(Object x, char conv) throws EvalException {
    return switch (x) {
      case StarlarkInt i -> i.toBigInteger();
      case Boolean b -> b ? BigInteger.ONE : BigInteger.ZERO;
      case StarlarkFloat f -> {
        double d = f.toDouble();
        if (Double.isNaN(d)) {
          throw Starlark.errorf("cannot convert float NaN to integer");
        }
        if (Double.isInfinite(d)) {
          throw Starlark.errorf("cannot convert float infinity to integer");
        }
        yield new BigDecimal(d).toBigInteger(); // truncates toward zero, as int() does
      }
      default ->
          throw Starlark.errorf(
              conv == 'd' || conv == 'i' || conv == 'u'
                  ? "%%%s format: a real number is required, not %s"
                  : "%%%s format: an integer is required, not %s",
              conv,
              Starlark.type(x));
    };
  }

  private static double floatArg(Object x, char conv) throws EvalException {
    return switch (x) {
      case StarlarkFloat f -> f.toDouble();
      case StarlarkInt i -> i.toFiniteDouble();
      case Boolean b -> b ? 1 : 0;
      default ->
          throw Starlark.errorf("%%%s format: must be real number, not %s", conv, Starlark.type(x));
    };
  }

  private static String charArg(Object x) throws EvalException {
    if (x instanceof StarlarkInt i) {
      BigInteger v = i.toBigInteger();
      if (v.signum() < 0 || v.compareTo(BigInteger.valueOf(0x10FFFF)) > 0) {
        throw Starlark.errorf("%%c arg not in range(0x110000)");
      }
      return new String(Character.toChars(v.intValue()));
    }
    if (x instanceof String s && s.codePointCount(0, s.length()) == 1) {
      return s;
    }
    throw Starlark.errorf("%%c requires int or char");
  }

  /** {@code s} with every non-ASCII code point escaped, as Python's ascii() does. */
  private static String ascii(String s) {
    StringBuilder b = new StringBuilder(s.length());
    s.codePoints()
        .forEach(
            c -> {
              if (c < 0x80) {
                b.append((char) c);
              } else if (c <= 0xff) {
                b.append(String.format("\\x%02x", c));
              } else if (c <= 0xffff) {
                b.append(String.format("\\u%04x", c));
              } else {
                b.append(String.format("\\U%08x", c));
              }
            });
    return b.toString();
  }

  /** {@code a} (finite, not negative) in fixed notation with {@code precision} decimals. */
  private static String fixed(double a, int precision, boolean alt) {
    String s = new BigDecimal(a).setScale(precision, RoundingMode.HALF_EVEN).toPlainString();
    return precision == 0 && alt ? s + "." : s;
  }

  /** {@code a} (finite, not negative) in scientific notation with {@code precision} decimals. */
  private static String exponent(double a, int precision, boolean alt) {
    String digits;
    int exp;
    if (a == 0) {
      digits = "0".repeat(precision + 1);
      exp = 0;
    } else {
      BigDecimal r = new BigDecimal(a).round(new MathContext(precision + 1, RoundingMode.HALF_EVEN));
      digits = r.unscaledValue().toString();
      exp = digits.length() - 1 - r.scale();
      if (digits.length() < precision + 1) {
        digits += "0".repeat(precision + 1 - digits.length());
      }
    }
    StringBuilder b = new StringBuilder(precision + 8).append(digits.charAt(0));
    if (precision > 0 || alt) {
      b.append('.').append(digits, 1, precision + 1);
    }
    b.append('e').append(exp < 0 ? '-' : '+');
    if (Math.abs(exp) < 10) {
      b.append('0');
    }
    return b.append(Math.abs(exp)).toString();
  }

  /** {@code a} (finite, not negative) as Python's {@code %g} formats it. */
  private static String general(double a, int precision, boolean alt) {
    int p = precision == 0 ? 1 : precision;
    int exp =
        a == 0
            ? 0
            : precisionExponent(
                new BigDecimal(a).round(new MathContext(p, RoundingMode.HALF_EVEN)));
    String s = -4 <= exp && exp < p ? fixed(a, p - 1 - exp, alt) : exponent(a, p - 1, alt);
    if (alt) {
      return s;
    }
    int e = s.indexOf('e');
    String mantissa = e < 0 ? s : s.substring(0, e);
    if (mantissa.indexOf('.') >= 0) {
      int end = mantissa.length();
      while (mantissa.charAt(end - 1) == '0') {
        end--;
      }
      if (mantissa.charAt(end - 1) == '.') {
        end--;
      }
      mantissa = mantissa.substring(0, end);
    }
    return e < 0 ? mantissa : mantissa + s.substring(e);
  }

  private static int precisionExponent(BigDecimal r) {
    return r.precision() - r.scale() - 1;
  }
}
