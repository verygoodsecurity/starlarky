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
import java.util.Locale;

/**
 * Python's format specification mini-language, as used by the built-in {@code format(value,
 * format_spec)} (https://docs.python.org/3/library/string.html#formatspec) for strings, ints,
 * bools and floats: {@code [[fill]align][sign][#][0][width][grouping][.precision][type]}.
 *
 * <p>Floats are rounded as Python rounds them (see {@link PercentFormat}); a float with no
 * presentation type and no precision is formatted with the shortest digits that round-trip, as
 * Python's repr does. {@code n} is formatted like {@code d} or {@code g}: there is no locale.
 */
public final class FormatSpec {

  private FormatSpec() {}

  /** A parsed format specification. */
  private static final class Spec {
    int fill = ' ';
    char align; // 0 if not given
    char sign; // 0, '+', '-' or ' '
    boolean alt;
    int width = -1;
    char grouping; // 0, ',' or '_'
    int precision = -1;
    char type; // 0 if not given
  }

  /**
   * Returns {@code value} formatted by {@code spec}, as Python's {@code format(value, spec)}
   * formats a str, int, bool or float. Other values accept only the empty specification, which
   * formats them as {@code str()} does.
   */
  public static String format(Object value, String spec, StarlarkSemantics semantics)
      throws EvalException {
    if (value instanceof String s) {
      return formatString(s, parse(spec, '<', 's'));
    }
    if (value instanceof Boolean b) {
      return spec.isEmpty() ? (b ? "True" : "False") : formatInt(b ? BigInteger.ONE : BigInteger.ZERO, parse(spec, '>', 'd'), "bool");
    }
    if (value instanceof StarlarkInt i) {
      return formatInt(i.toBigInteger(), parse(spec, '>', 'd'), "int");
    }
    if (value instanceof StarlarkFloat f) {
      return formatFloat(f.toDouble(), parse(spec, '>', (char) 0), "float");
    }
    if (spec.isEmpty()) {
      return Starlark.str(value, semantics);
    }
    throw Starlark.errorf("unsupported format string passed to %s.__format__", Starlark.type(value));
  }

  private static boolean isAlign(int c) {
    return c == '<' || c == '>' || c == '=' || c == '^';
  }

  /** Parses {@code spec}, as CPython's parse_internal_render_format_spec does. */
  private static Spec parse(String spec, char defaultAlign, char defaultType)
      throws EvalException {
    Spec f = new Spec();
    int[] s = spec.codePoints().toArray();
    int n = s.length;
    int i = 0;
    boolean fillGiven = false;
    if (n - i >= 2 && isAlign(s[i + 1])) {
      f.fill = s[i];
      f.align = (char) s[i + 1];
      fillGiven = true;
      i += 2;
    } else if (n - i >= 1 && isAlign(s[i])) {
      f.align = (char) s[i];
      i++;
    }
    if (i < n && (s[i] == '+' || s[i] == '-' || s[i] == ' ')) {
      f.sign = (char) s[i++];
    }
    if (i < n && s[i] == '#') {
      f.alt = true;
      i++;
    }
    if (!fillGiven && i < n && s[i] == '0') {
      f.fill = '0';
      if (f.align == 0 && defaultAlign == '>') {
        f.align = '=';
      }
      i++;
    }
    int start = i;
    while (i < n && s[i] >= '0' && s[i] <= '9') {
      i++;
    }
    if (i > start) {
      f.width = size(s, start, i, "width");
    }
    if (i < n && s[i] == ',') {
      f.grouping = ',';
      i++;
    }
    if (i < n && s[i] == '_') {
      if (f.grouping != 0) {
        throw Starlark.errorf("Cannot specify both ',' and '_'.");
      }
      f.grouping = '_';
      i++;
    }
    if (i < n && s[i] == ',') {
      throw Starlark.errorf("Cannot specify both ',' and '_'.");
    }
    if (i < n && s[i] == '.') {
      i++;
      start = i;
      while (i < n && s[i] >= '0' && s[i] <= '9') {
        i++;
      }
      if (i == start) {
        throw Starlark.errorf("Format specifier missing precision");
      }
      f.precision = size(s, start, i, "precision");
    }
    if (n - i > 1) {
      throw Starlark.errorf("Invalid format specifier");
    }
    f.type = n - i == 1 ? (char) s[i] : 0;
    if (f.grouping != 0) {
      char t = f.type != 0 ? f.type : defaultType;
      boolean ok =
          switch (t) {
            case 0, 'd', 'e', 'f', 'g', 'E', 'F', 'G', '%' -> true;
            case 'b', 'o', 'x', 'X' -> f.grouping == '_';
            default -> false;
          };
      if (!ok) {
        throw Starlark.errorf("Cannot specify '%s' with '%s'.", f.grouping, t);
      }
    }
    return f;
  }

  private static int size(int[] s, int start, int end, String what) throws EvalException {
    if (end - start > 7) {
      throw Starlark.errorf("%s too big", what); // Python allows more; the spec comes from the script
    }
    int v = 0;
    for (int k = start; k < end; k++) {
      v = v * 10 + (s[k] - '0');
    }
    if (v > PercentFormat.MAX_WIDTH) {
      throw Starlark.errorf("%s too big", what);
    }
    return v;
  }

  private static String formatString(String s, Spec f) throws EvalException {
    if (f.type != 0 && f.type != 's') {
      throw Starlark.errorf("Unknown format code '%s' for object of type 'str'", f.type);
    }
    if (f.sign != 0) {
      throw Starlark.errorf("Sign not allowed in string format specifier");
    }
    if (f.alt) {
      throw Starlark.errorf("Alternate form (#) not allowed in string format specifier");
    }
    if (f.align == '=') {
      throw Starlark.errorf("'=' alignment not allowed in string format specifier");
    }
    if (f.precision >= 0 && f.precision < s.codePointCount(0, s.length())) {
      s = s.substring(0, s.offsetByCodePoints(0, f.precision));
    }
    return pad("", "", s, "", f, '<');
  }

  private static String formatInt(BigInteger v, Spec f, String typeName) throws EvalException {
    char t = f.type == 0 ? 'd' : f.type;
    switch (t) {
      case 'e', 'E', 'f', 'F', 'g', 'G', '%' -> {
        double d = v.doubleValue();
        if (Double.isInfinite(d)) {
          throw Starlark.errorf("int too large to convert to float");
        }
        return formatFloat(d, f, typeName);
      }
      case 'd', 'n', 'b', 'o', 'x', 'X', 'c' -> {}
      default ->
          throw Starlark.errorf(
              "Unknown format code '%s' for object of type '%s'", t, typeName);
    }
    if (f.precision >= 0) {
      throw Starlark.errorf("Precision not allowed in integer format specifier");
    }
    if (t == 'c') {
      if (f.sign != 0) {
        throw Starlark.errorf("Sign not allowed with integer format specifier 'c'");
      }
      if (f.alt) {
        throw Starlark.errorf("Alternate form (#) not allowed with integer format specifier 'c'");
      }
      if (v.signum() < 0 || v.compareTo(BigInteger.valueOf(0x10FFFF)) > 0) {
        throw Starlark.errorf("%%c arg not in range(0x110000)");
      }
      return pad("", "", new String(Character.toChars(v.intValue())), "", f, '>');
    }
    int radix = t == 'b' ? 2 : t == 'o' ? 8 : t == 'x' || t == 'X' ? 16 : 10;
    String digits = v.abs().toString(radix);
    if (t == 'X') {
      digits = digits.toUpperCase(Locale.ROOT);
    }
    String prefix = !f.alt || radix == 10 ? "" : t == 'b' ? "0b" : t == 'o' ? "0o" : t == 'x' ? "0x" : "0X";
    return number(sign(v.signum() < 0, f), prefix, digits, "", f, radix == 10 ? 3 : 4);
  }

  private static String formatFloat(double v, Spec f, String typeName) throws EvalException {
    char t = f.type;
    switch (t) {
      case 0, 'e', 'E', 'f', 'F', 'g', 'G', 'n', '%' -> {}
      default ->
          throw Starlark.errorf(
              "Unknown format code '%s' for object of type '%s'", t, typeName);
    }
    if (t == '%') {
      v *= 100; // as Python does, in floating point
    }
    boolean negative = !Double.isNaN(v) && (v < 0 || (v == 0 && 1 / v < 0));
    String sign = sign(negative, f);
    double a = Math.abs(v);
    String body;
    if (Double.isNaN(a) || Double.isInfinite(a)) {
      body = Double.isNaN(a) ? "nan" : "inf";
      if (t == 'E' || t == 'F' || t == 'G') {
        body = body.toUpperCase(Locale.ROOT);
      }
      return number(sign, "", body, t == '%' ? "%" : "", f, 0);
    }
    int p = f.precision;
    switch (t) {
      case 'e', 'E' -> body = PercentFormat.exponent(a, p < 0 ? 6 : p, f.alt);
      case 'f', 'F', '%' -> body = PercentFormat.fixed(a, p < 0 ? 6 : p, f.alt);
      case 'g', 'G', 'n' -> body = PercentFormat.general(a, p < 0 ? 6 : p, f.alt);
      default -> { // no type: repr's digits, or 'g' with at least one decimal
        if (p < 0) {
          body = repr(a);
          int e = body.indexOf('e');
          if (f.alt && e >= 0 && body.lastIndexOf('.', e) < 0) {
            body = body.substring(0, e) + "." + body.substring(e); // 1e-05 -> 1.e-05
          }
        } else {
          body = generalWithDot(a, p, f.alt);
        }
      }
    }
    if (t == 'E' || t == 'G') {
      body = body.toUpperCase(Locale.ROOT);
    }
    // Split the digits before the decimal point (grouped and zero-padded) from the rest.
    int end = 0;
    while (end < body.length() && Character.isDigit(body.charAt(end))) {
      end++;
    }
    return number(sign, "", body.substring(0, end), body.substring(end) + (t == '%' ? "%" : ""), f, 3);
  }

  /**
   * {@code a} (finite, not negative) with a precision and no presentation type: like {@code g},
   * but switching to exponent notation one digit earlier and keeping at least one digit after
   * the decimal point (CPython's 'g' with Py_DTSF_ADD_DOT_0).
   */
  private static String generalWithDot(double a, int precision, boolean alt) {
    int p = precision == 0 ? 1 : precision;
    int exp =
        a == 0
            ? 0
            : precisionExponent(new BigDecimal(a).round(new MathContext(p, RoundingMode.HALF_EVEN)));
    boolean exponent = exp < -4 || exp >= p - 1;
    String s = exponent ? PercentFormat.exponent(a, p - 1, alt) : PercentFormat.fixed(a, p - 1 - exp, alt);
    if (!alt) {
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
      s = e < 0 ? mantissa : mantissa + s.substring(e);
    }
    if (!exponent && s.indexOf('.') < 0) {
      s += ".0";
    }
    return s;
  }

  private static int precisionExponent(BigDecimal r) {
    return r.precision() - r.scale() - 1;
  }

  /** {@code a} (finite, not negative) as Python's repr formats a float. */
  private static String repr(double a) {
    if (a == 0) {
      return "0.0";
    }
    // Double.toString gives the shortest decimal that round-trips, but with at least two
    // significant digits (4.9E-324); Python uses one when one round-trips (5e-324).
    BigDecimal d = new BigDecimal(Double.toString(a)).stripTrailingZeros();
    if (d.precision() == 2) {
      BigDecimal one = new BigDecimal(a).round(new MathContext(1, RoundingMode.HALF_EVEN));
      if (one.doubleValue() == a) {
        d = one.stripTrailingZeros();
      }
    }
    String digits = d.unscaledValue().toString();
    int decpt = digits.length() - d.scale(); // value = 0.digits * 10^decpt
    if (-4 < decpt && decpt <= 16) {
      if (decpt <= 0) {
        return "0." + "0".repeat(-decpt) + digits;
      }
      if (decpt >= digits.length()) {
        return digits + "0".repeat(decpt - digits.length()) + ".0";
      }
      return digits.substring(0, decpt) + "." + digits.substring(decpt);
    }
    int exp = decpt - 1;
    String mantissa = digits.length() == 1 ? digits : digits.charAt(0) + "." + digits.substring(1);
    return mantissa + "e" + (exp < 0 ? "-" : "+") + (Math.abs(exp) < 10 ? "0" : "") + Math.abs(exp);
  }

  private static String sign(boolean negative, Spec f) {
    return negative ? "-" : f.sign == '+' ? "+" : f.sign == ' ' ? " " : "";
  }

  /**
   * Lays out a number: sign, prefix, integer digits (grouped every {@code interval} digits if
   * the spec asks), and the rest (fraction, exponent, %). Zero padding ({@code 0} fill with
   * {@code =} alignment) goes into the digits, grouped as they are.
   */
  private static String number(
      String sign, String prefix, String digits, String rest, Spec f, int interval) {
    boolean groupable = !digits.isEmpty() && Character.isDigit(digits.charAt(0));
    char sep = groupable ? f.grouping : 0;
    if (f.fill == '0' && f.align == '=' && f.width > 0) {
      int need = f.width - sign.length() - prefix.length() - rest.codePointCount(0, rest.length());
      String padded = digits;
      String grouped = group(padded, sep, interval);
      while (grouped.length() < need) {
        padded = "0" + padded;
        grouped = group(padded, sep, interval);
      }
      return sign + prefix + grouped + rest;
    }
    return pad(sign, prefix, group(digits, sep, interval), rest, f, '>');
  }

  /** {@code digits} with {@code sep} between every {@code interval} digits from the right. */
  private static String group(String digits, char sep, int interval) {
    if (sep == 0 || digits.length() <= interval) {
      return digits;
    }
    StringBuilder b = new StringBuilder(digits.length() + digits.length() / interval);
    int first = digits.length() % interval;
    if (first == 0) {
      first = interval;
    }
    b.append(digits, 0, first);
    for (int k = first; k < digits.length(); k += interval) {
      b.append(sep).append(digits, k, k + interval);
    }
    return b.toString();
  }

  /** Pads sign + prefix + body + rest to the spec's width with its fill and alignment. */
  private static String pad(
      String sign, String prefix, String body, String rest, Spec f, char defaultAlign) {
    String text = sign + prefix + body + rest;
    int length = text.codePointCount(0, text.length());
    if (f.width <= length) {
      return text;
    }
    String fill = new String(Character.toChars(f.fill));
    int padding = f.width - length;
    return switch (f.align == 0 ? defaultAlign : f.align) {
      case '<' -> text + fill.repeat(padding);
      case '^' -> fill.repeat(padding / 2) + text + fill.repeat(padding - padding / 2);
      case '=' -> sign + prefix + fill.repeat(padding) + body + rest;
      default -> fill.repeat(padding) + text;
    };
  }
}
