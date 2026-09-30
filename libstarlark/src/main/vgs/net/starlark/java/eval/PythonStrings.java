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

import com.google.common.base.CharMatcher;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Python 3 behaviour for string methods, for Larky. {@link StringModule} calls in here when one of
 * the semantics flags below is set; with none set, strings behave as in Bazel's Starlark.
 *
 * <p>{@link #PYTHON_STRING_BOUNDS}: {@code find rfind index rindex count startswith endswith}
 * treat a {@code start} past {@code end} (after Python's index adjustment) as an empty range that
 * matches nothing, not even {@code ""}. Starlark instead clamps {@code start} to the slice, as the
 * spec's indexing conventions say, so {@code "abc".find("", 4)} is 3 in Starlark (and in
 * starlark-go) but -1 in Python.
 *
 * <p>{@link #PYTHON_UNICODE_STRINGS}: {@code upper lower capitalize title} use Unicode's full case
 * mappings as CPython does ({@code "ß".upper() == "SS"}, final sigma, {@code "ﬁ".title() ==
 * "Fi"}); {@code isalpha isalnum isdigit isspace islower isupper istitle} classify by Unicode
 * properties as CPython does; {@code strip lstrip rstrip} with no argument remove Python's
 * whitespace. Bazel's Starlark maps case and classifies letters and digits in ASCII only;
 * starlark-go uses Unicode but simple (one-to-one) case mappings.
 *
 * <p>Differences from CPython: strings are UTF-16, so an unpaired surrogate is an uncased,
 * non-letter code point as in Python, but lengths and indices count UTF-16 units; the Unicode
 * version is the JDK's (15.0 in Java 21, the same as CPython 3.12).
 */
public final class PythonStrings {

  private PythonStrings() {}

  /** Semantics flag: out-of-range {@code start}/{@code end} in string searches follow Python. */
  public static final String PYTHON_STRING_BOUNDS = "-python_string_bounds";

  /** Semantics flag: string case mapping and classification follow Python's Unicode rules. */
  public static final String PYTHON_UNICODE_STRINGS = "-python_unicode_strings";

  static boolean bounds(StarlarkThread thread) {
    return thread.getSemantics().getBool(PYTHON_STRING_BOUNDS);
  }

  static boolean unicode(StarlarkThread thread) {
    return thread.getSemantics().getBool(PYTHON_UNICODE_STRINGS);
  }

  static boolean unicode(StarlarkSemantics semantics) {
    return semantics.getBool(PYTHON_UNICODE_STRINGS);
  }

  // ---- start/end ----

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

  // ---- case mapping ----

  /** {@code str.upper()}: full Unicode uppercase mapping. */
  static String upper(String s) {
    return s.toUpperCase(Locale.ROOT); // SpecialCasing has no conditional uppercase rules
  }

  /** {@code str.lower()}: full Unicode lowercase mapping, with CPython's final-sigma rule. */
  static String lower(String s) {
    if (s.indexOf(CAPITAL_SIGMA) < 0) {
      // Without a sigma, lowercasing is context-free (the only other special case, U+0130, is
      // unconditional).
      return s.toLowerCase(Locale.ROOT);
    }
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); ) {
      int c = s.codePointAt(i);
      appendLower(sb, s, i, c);
      i += Character.charCount(c);
    }
    return sb.toString();
  }

  /** {@code str.capitalize()}: titlecase the first code point, lowercase the rest (CPython 3.8+). */
  static String capitalize(String s) {
    if (s.isEmpty()) {
      return s;
    }
    StringBuilder sb = new StringBuilder(s.length());
    int c = s.codePointAt(0);
    appendTitle(sb, c);
    for (int i = Character.charCount(c); i < s.length(); ) {
      c = s.codePointAt(i);
      appendLower(sb, s, i, c);
      i += Character.charCount(c);
    }
    return sb.toString();
  }

  /**
   * {@code str.title()}: titlecase each code point that follows an uncased one, lowercase the
   * others.
   */
  static String title(String s) {
    StringBuilder sb = new StringBuilder(s.length());
    boolean previousIsCased = false;
    for (int i = 0; i < s.length(); ) {
      int c = s.codePointAt(i);
      if (previousIsCased) {
        appendLower(sb, s, i, c);
      } else {
        appendTitle(sb, c);
      }
      previousIsCased = isCased(c);
      i += Character.charCount(c);
    }
    return sb.toString();
  }

  private static final char CAPITAL_SIGMA = 'Σ';

  // Appends the full lowercase mapping of c, the code point at s[i] (CPython's lower_ucs4).
  private static void appendLower(StringBuilder sb, String s, int i, int c) {
    if (c == CAPITAL_SIGMA) {
      sb.append(isFinalSigma(s, i) ? 'ς' : 'σ');
    } else if (c == 0x130) {
      sb.append("i̇"); // the only multi-code-point lowercase mapping
    } else {
      sb.appendCodePoint(Character.toLowerCase(c));
    }
  }

  // Appends the full titlecase mapping of c.
  private static void appendTitle(StringBuilder sb, int c) {
    String special = FULL_TITLE.get(c);
    if (special != null) {
      sb.append(special);
    } else {
      sb.appendCodePoint(Character.toTitleCase(c));
    }
  }

  // CPython's handle_capital_sigma: final if preceded by a cased code point and not followed by
  // one, skipping case-ignorable code points in both directions.
  private static boolean isFinalSigma(String s, int i) {
    int j = i;
    int c = 0;
    boolean found = false;
    while (j > 0) {
      c = s.codePointBefore(j);
      j -= Character.charCount(c);
      if (!isCaseIgnorable(c)) {
        found = true;
        break;
      }
    }
    if (!found || !isCased(c)) {
      return false;
    }
    j = i + 1; // sigma is one UTF-16 unit
    while (j < s.length()) {
      c = s.codePointAt(j);
      j += Character.charCount(c);
      if (!isCaseIgnorable(c)) {
        return !isCased(c);
      }
    }
    return true;
  }

  // Unicode's Cased property: Lowercase, Uppercase (both include the Other_* properties, as
  // Character.isLowerCase/isUpperCase do) or Lt.
  private static boolean isCased(int c) {
    return Character.isLowerCase(c) || Character.isUpperCase(c) || Character.isTitleCase(c);
  }

  // Unicode's Case_Ignorable property: Mn, Me, Cf, Lm, Sk, or Word_Break MidLetter, MidNumLet or
  // Single_Quote (the explicit list).
  private static boolean isCaseIgnorable(int c) {
    switch (Character.getType(c)) {
      case Character.NON_SPACING_MARK:
      case Character.ENCLOSING_MARK:
      case Character.FORMAT:
      case Character.MODIFIER_LETTER:
      case Character.MODIFIER_SYMBOL:
        return true;
      default:
        break;
    }
    switch (c) {
      case 0x0027: case 0x002E: case 0x003A: case 0x00B7: case 0x0387: case 0x055F: case 0x05F4:
      case 0x2018: case 0x2019: case 0x2024: case 0x2027: case 0xFE13: case 0xFE52: case 0xFE55:
      case 0xFF07: case 0xFF0E: case 0xFF1A:
        return true;
      default:
        return false;
    }
  }

  // Titlecase mappings to more than one code point (SpecialCasing.txt); the rest are
  // Character.toTitleCase. Generated with CPython 3.12: [c for c in all if len(c.title()) > 1].
  private static final Map<Integer, String> FULL_TITLE = new HashMap<>();

  static {
    Object[] pairs = {
      0x00DF, "Ss", 0x0149, "ʼN", 0x01F0, "J̌",
      0x0390, "Ϊ́", 0x03B0, "Ϋ́", 0x0587, "Եւ",
      0x1E96, "H̱", 0x1E97, "T̈", 0x1E98, "W̊",
      0x1E99, "Y̊", 0x1E9A, "Aʾ", 0x1F50, "Υ̓",
      0x1F52, "Υ̓̀", 0x1F54, "Υ̓́", 0x1F56, "Υ̓͂",
      0x1FB2, "Ὰͅ", 0x1FB4, "Άͅ", 0x1FB6, "Α͂",
      0x1FB7, "ᾼ͂", 0x1FC2, "Ὴͅ", 0x1FC4, "Ήͅ",
      0x1FC6, "Η͂", 0x1FC7, "ῌ͂", 0x1FD2, "Ϊ̀",
      0x1FD3, "Ϊ́", 0x1FD6, "Ι͂", 0x1FD7, "Ϊ͂",
      0x1FE2, "Ϋ̀", 0x1FE3, "Ϋ́", 0x1FE4, "Ρ̓",
      0x1FE6, "Υ͂", 0x1FE7, "Ϋ͂", 0x1FF2, "Ὼͅ",
      0x1FF4, "Ώͅ", 0x1FF6, "Ω͂", 0x1FF7, "ῼ͂",
      0xFB00, "Ff", 0xFB01, "Fi", 0xFB02, "Fl",
      0xFB03, "Ffi", 0xFB04, "Ffl", 0xFB05, "St",
      0xFB06, "St", 0xFB13, "Մն", 0xFB14, "Մե",
      0xFB15, "Մի", 0xFB16, "Վն", 0xFB17, "Մխ",
    };
    for (int i = 0; i < pairs.length; i += 2) {
      FULL_TITLE.put((Integer) pairs[i], (String) pairs[i + 1]);
    }
  }

  // ---- classification ----

  /** {@code str.isalpha()}: non-empty, and every code point is a letter (L*). */
  static boolean isAlpha(String s) {
    return !s.isEmpty() && s.codePoints().allMatch(Character::isLetter);
  }

  /** {@code str.isdigit()}: non-empty, and every code point has Numeric_Type Decimal or Digit. */
  static boolean isDigit(String s) {
    return !s.isEmpty() && s.codePoints().allMatch(PythonStrings::isDigit);
  }

  /** {@code str.isalnum()}: non-empty, and every code point is alphabetic or numeric. */
  static boolean isAlnum(String s) {
    return !s.isEmpty()
        && s.codePoints()
            .allMatch(
                c -> {
                  switch (Character.getType(c)) {
                    case Character.DECIMAL_DIGIT_NUMBER:
                    case Character.LETTER_NUMBER:
                    case Character.OTHER_NUMBER:
                      return true; // Numeric_Type Decimal, Digit or Numeric
                    default:
                      return Character.isLetter(c);
                  }
                });
  }

  /** {@code str.isspace()}: non-empty, and every code point is Python whitespace. */
  static boolean isSpace(String s) {
    return !s.isEmpty() && WHITESPACE.matchesAllOf(s);
  }

  /** {@code str.islower()}: some cased code point, and no uppercase or titlecase one. */
  static boolean isLower(String s) {
    boolean cased = false;
    for (int i = 0; i < s.length(); ) {
      int c = s.codePointAt(i);
      if (Character.isUpperCase(c) || Character.isTitleCase(c)) {
        return false;
      }
      cased |= Character.isLowerCase(c);
      i += Character.charCount(c);
    }
    return cased;
  }

  /** {@code str.isupper()}: some cased code point, and no lowercase or titlecase one. */
  static boolean isUpper(String s) {
    boolean cased = false;
    for (int i = 0; i < s.length(); ) {
      int c = s.codePointAt(i);
      if (Character.isLowerCase(c) || Character.isTitleCase(c)) {
        return false;
      }
      cased |= Character.isUpperCase(c);
      i += Character.charCount(c);
    }
    return cased;
  }

  /**
   * {@code str.istitle()}: some cased code point; uppercase and titlecase code points follow only
   * uncased ones, lowercase ones only cased ones.
   */
  static boolean isTitle(String s) {
    boolean cased = false;
    boolean previousIsCased = false;
    for (int i = 0; i < s.length(); ) {
      int c = s.codePointAt(i);
      if (Character.isUpperCase(c) || Character.isTitleCase(c)) {
        if (previousIsCased) {
          return false;
        }
        previousIsCased = true;
        cased = true;
      } else if (Character.isLowerCase(c)) {
        if (!previousIsCased) {
          return false;
        }
        previousIsCased = true;
        cased = true;
      } else {
        previousIsCased = false;
      }
      i += Character.charCount(c);
    }
    return cased;
  }

  private static boolean isDigit(int c) {
    if (Character.getType(c) == Character.DECIMAL_DIGIT_NUMBER) {
      return true;
    }
    // Numeric_Type=Digit, all in category No. Generated with CPython 3.12:
    // [c for c in all if c.isdigit() and not c.isdecimal()].
    return (c >= 0xB2 && c <= 0xB3)
        || c == 0xB9
        || (c >= 0x1369 && c <= 0x1371)
        || c == 0x19DA
        || c == 0x2070
        || (c >= 0x2074 && c <= 0x2079)
        || (c >= 0x2080 && c <= 0x2089)
        || (c >= 0x2460 && c <= 0x2468)
        || (c >= 0x2474 && c <= 0x247C)
        || (c >= 0x2488 && c <= 0x2490)
        || c == 0x24EA
        || (c >= 0x24F5 && c <= 0x24FD)
        || c == 0x24FF
        || (c >= 0x2776 && c <= 0x277E)
        || (c >= 0x2780 && c <= 0x2788)
        || (c >= 0x278A && c <= 0x2792)
        || (c >= 0x10A40 && c <= 0x10A43)
        || (c >= 0x10E60 && c <= 0x10E68)
        || (c >= 0x11052 && c <= 0x1105A)
        || (c >= 0x1F100 && c <= 0x1F10A);
  }

  /**
   * Python's whitespace (str.isspace, str.strip()): Unicode White_Space characters with
   * bidirectional type WS, B or S, or category Zs. Unlike Java's, it includes U+00A0 and U+2007 and
   * U+202F, and U+001C..U+001F; it excludes U+180E and U+200B.
   */
  static final CharMatcher WHITESPACE =
      CharMatcher.anyOf(
          "\t\n\u000B\u000C\r\u001C\u001D\u001E\u001F \u0085  "
              + "           "
              + "    　");
}
