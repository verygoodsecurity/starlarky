package com.verygood.security.larky.modules.re;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.starlark.java.eval.EvalException;

/**
 * Translates a Python {@code re} pattern into an equivalent RE2 pattern.
 *
 * <p>Python and RE2 share most syntax but differ in the details that make a translation necessary:
 *
 * <ul>
 *   <li>{@code \d \w \s} (and their negations) are Unicode-aware for Python str patterns; in RE2
 *       they are ASCII-only. They are expanded to explicit Unicode ranges (or Python's ASCII sets
 *       under {@code re.ASCII}/{@code (?a)}, or for bytes input).
 *   <li>Python's {@code $} (without MULTILINE) also matches before a newline that ends the string,
 *       and {@code \Z} is RE2's {@code \z}. Both are emitted as {@link Marker}s that the caller
 *       expands per input (see {@link RegexPattern}).
 *   <li>Escapes RE2 lacks or spells differently ({@code \v \\u \U \N{...}}, octal) become
 *       {@code \x{...}}; every literal outside ASCII alphanumerics is emitted as {@code \x{...}},
 *       so RE2-only syntax (POSIX classes, {@code \Q..\E}, ...) cannot sneak in through literals.
 *   <li>Named groups are recorded here and emitted as plain capturing groups, so group names follow
 *       Python's rules (any identifier) rather than RE2's.
 *   <li>Constructs RE2 cannot run in linear time (backreferences, look-around, conditionals,
 *       atomic groups, possessive quantifiers) raise a {@code re.error} instead of a Java exception.
 * </ul>
 *
 * <p>Errors use CPython's messages and positions ({@code "re.error: <msg> at position <n>"}).
 */
final class PyRegexTranslator {

  // Flag bits as defined by re.star's RegexFlags (1 << index); these are not CPython's values.
  static final int FLAG_I = 1;
  static final int FLAG_S = 2;
  static final int FLAG_M = 4;
  static final int FLAG_U = 8;
  static final int FLAG_LONGEST_MATCH = 16;
  static final int FLAG_A = 32;
  static final int FLAG_DEBUG = 64;
  static final int FLAG_L = 128;
  static final int FLAG_X = 256;
  static final int FLAG_T = 512;

  /** Placeholders for the constructs whose RE2 spelling depends on the input. */
  enum Marker {
    /** Python {@code $} outside MULTILINE: end of string, or before a newline that ends it. */
    DOLLAR,
    /** Python {@code \Z}: end of string only. */
    END_OF_STRING
  }

  /** The translated pattern: RE2 text interleaved with {@link Marker}s, plus group metadata. */
  static final class Translation {
    final List<Object> pieces;
    final int groups;
    final Map<String, Integer> groupIndex;
    final boolean hasDollar;
    /** Global inline flags found in the pattern (e.g. {@code (?i)}), merged with the call's. */
    final int flags;

    Translation(List<Object> pieces, int groups, Map<String, Integer> groupIndex, int flags) {
      this.pieces = pieces;
      this.groups = groups;
      this.groupIndex = Collections.unmodifiableMap(groupIndex);
      this.hasDollar = pieces.contains(Marker.DOLLAR);
      this.flags = flags;
    }
  }

  static EvalException error(String msg, int pos) {
    return new EvalException(String.format("re.error: %s at position %d", msg, pos));
  }

  private static EvalException unsupported(String what, int pos) {
    return error(what + " are not supported (Larky's regex engine runs in linear time)", pos);
  }

  /**
   * Translates {@code source} compiled with {@code flags}; {@code forceAscii} selects ASCII
   * classes (used for bytes input).
   */
  static Translation translate(String source, int flags, boolean forceAscii) throws EvalException {
    if ((flags & FLAG_L) != 0) {
      throw new EvalException("ValueError: cannot use LOCALE flag with a str pattern");
    }
    if ((flags & FLAG_A) != 0 && (flags & FLAG_U) != 0) {
      throw new EvalException("ValueError: ASCII and UNICODE flags are incompatible");
    }
    // Global inline flags ((?x), (?a), ...) apply to the whole pattern wherever they appear (as in
    // CPython before 3.11, which only warned about them not being at the start). They change how
    // earlier text translates, so translate again until the set of global flags is stable.
    int global = flags;
    for (int attempt = 0; ; attempt++) {
      PyRegexTranslator t = new PyRegexTranslator(source, global, forceAscii);
      t.parseAlternation(false);
      t.flushOut();
      if (t.globalFlags == global || attempt > 8) {
        return new Translation(t.pieces, t.groups, t.groupIndex, t.globalFlags);
      }
      global = t.globalFlags;
    }
  }

  private final String src;
  private final int n;
  private final boolean forceAscii;
  private int i;
  private int globalFlags;
  private final List<Object> pieces = new ArrayList<>();
  private final StringBuilder out = new StringBuilder();
  private int groups;
  private final Map<String, Integer> groupIndex = new LinkedHashMap<>();
  /** Groups whose closing parenthesis has not been seen yet (Python: "open group"). */
  private final Deque<Integer> openGroups = new ArrayDeque<>();
  /** Flags in effect in the current scope: the verbose, ASCII and multiline bits matter here. */
  private int scopeFlags;

  private PyRegexTranslator(String src, int flags, boolean forceAscii) {
    this.src = src;
    this.n = src.length();
    this.forceAscii = forceAscii;
    this.globalFlags = flags;
    this.scopeFlags = flags;
  }

  private boolean verbose() {
    return (scopeFlags & FLAG_X) != 0;
  }

  private boolean ascii() {
    return forceAscii || (scopeFlags & FLAG_A) != 0;
  }

  private boolean multiline() {
    return (scopeFlags & FLAG_M) != 0;
  }

  private boolean caseless() {
    return (scopeFlags & FLAG_I) != 0;
  }

  private void flushOut() {
    if (out.length() > 0) {
      pieces.add(out.toString());
      out.setLength(0);
    }
  }

  private void emitMarker(Marker m) {
    flushOut();
    pieces.add(m);
  }

  /** What the previous item was, for the "nothing to repeat" / "multiple repeat" checks. */
  private enum Prev {
    NOTHING,
    ATOM,
    REPEAT
  }

  // --- Top level: alternation / sequence -------------------------------------------------------

  /** Parses up to the end of the pattern, or (if {@code nested}) up to an unconsumed ')'. */
  private void parseAlternation(boolean nested) throws EvalException {
    Prev prev = Prev.NOTHING;
    while (i < n) {
      char c = src.charAt(i);
      if (verbose()) {
        if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\u000b' || c == '\f') {
          i++;
          continue;
        }
        if (c == '#') {
          while (i < n && src.charAt(i) != '\n') {
            i++;
          }
          continue;
        }
      }
      switch (c) {
        case '|':
          out.append('|');
          i++;
          prev = Prev.NOTHING;
          continue;
        case ')':
          if (nested) {
            return;
          }
          throw error("unbalanced parenthesis", i);
        case '*':
        case '+':
        case '?':
          prev = parseRepeat(prev, i + 1, c == '*' ? "*" : c == '+' ? "+" : "?");
          continue;
        case '{':
          {
            int[] bounds = repeatBounds(i);
            if (bounds != null) {
              int start = i;
              if (bounds[1] >= 0 && bounds[0] > bounds[1]) {
                throw error("min repeat greater than max repeat", start + 1);
              }
              String rep = "{" + bounds[0] + "," + (bounds[1] >= 0 ? bounds[1] : "") + "}";
              prev = parseRepeat(prev, bounds[2], rep);
              continue;
            }
            emitLiteral('{');
            i++;
            prev = Prev.ATOM;
            continue;
          }
        case '(':
          prev = parseGroup();
          continue;
        case '[':
          parseClass();
          prev = Prev.ATOM;
          continue;
        case '.':
          out.append('.');
          i++;
          prev = Prev.ATOM;
          continue;
        case '^':
          out.append('^');
          i++;
          prev = Prev.NOTHING;
          continue;
        case '$':
          if (multiline()) {
            out.append("(?m:$)");
          } else {
            emitMarker(Marker.DOLLAR);
          }
          i++;
          prev = Prev.NOTHING;
          continue;
        case '\\':
          prev = parseEscape();
          continue;
        default:
          int cp = src.codePointAt(i);
          emitLiteral(cp);
          i += Character.charCount(cp);
          prev = Prev.ATOM;
      }
    }
  }

  /**
   * Parses {@code {m}}, {@code {m,}}, {@code {,n}}, {@code {m,n}} at {@code at}. Returns
   * {min, max or -1, index after '}'}, or null if the brace is a literal (as in Python).
   */
  private int[] repeatBounds(int at) throws EvalException {
    int j = at + 1;
    int loStart = j;
    while (j < n && isDigit(src.charAt(j))) {
      j++;
    }
    String lo = src.substring(loStart, j);
    String hi = null;
    if (j < n && src.charAt(j) == ',') {
      j++;
      int hiStart = j;
      while (j < n && isDigit(src.charAt(j))) {
        j++;
      }
      hi = src.substring(hiStart, j);
    } else if (lo.isEmpty()) {
      return null;
    }
    if (j >= n || src.charAt(j) != '}') {
      return null;
    }
    int min = lo.isEmpty() ? 0 : parseCount(lo, loStart);
    int max = hi == null ? min : hi.isEmpty() ? -1 : parseCount(hi, at + 1);
    return new int[] {min, max, j + 1};
  }

  private static int parseCount(String digits, int pos) throws EvalException {
    try {
      int v = Integer.parseInt(digits);
      if (v < Integer.MAX_VALUE) {
        return v;
      }
    } catch (NumberFormatException e) {
      // fall through
    }
    throw error("the repetition number is too large", pos);
  }

  /** Emits a quantifier ending at {@code next} (exclusive), plus an optional lazy '?'. */
  private Prev parseRepeat(Prev prev, int next, String rep) throws EvalException {
    int start = i;
    if (prev == Prev.REPEAT) {
      throw error("multiple repeat", start);
    }
    if (prev == Prev.NOTHING) {
      throw error("nothing to repeat", start);
    }
    out.append(rep);
    i = next;
    if (i < n && src.charAt(i) == '?') {
      out.append('?');
      i++;
    } else if (i < n && src.charAt(i) == '+') {
      throw unsupported("possessive quantifiers", start);
    }
    return Prev.REPEAT;
  }

  // --- Groups ----------------------------------------------------------------------------------

  private Prev parseGroup() throws EvalException {
    int open = i;
    i++; // '('
    int savedScope = scopeFlags;
    if (i < n && src.charAt(i) == '?') {
      i++;
      if (i >= n) {
        throw error("unexpected end of pattern", i);
      }
      char c = src.charAt(i);
      switch (c) {
        case ':':
          i++;
          out.append("(?:");
          return finishGroup(open, savedScope, -1);
        case 'P':
          i++;
          if (i < n && src.charAt(i) == '<') {
            i++;
            String name = parseName(">");
            if (groupIndex.containsKey(name)) {
              throw error(
                  String.format(
                      "redefinition of group name %s as group %d; was group %d",
                      pyRepr(name), groups + 1, groupIndex.get(name)),
                  i - name.length() - 1);
            }
            int group = ++groups;
            groupIndex.put(name, group);
            out.append('(');
            return finishGroup(open, savedScope, group);
          }
          if (i < n && src.charAt(i) == '=') {
            i++;
            int nameStart = i;
            String name = parseName(")");
            if (!groupIndex.containsKey(name)) {
              throw error("unknown group name " + pyRepr(name), nameStart);
            }
            throw unsupported("backreferences", nameStart - 4);
          }
          if (i >= n) {
            throw error("unexpected end of pattern", i);
          }
          throw error("unknown extension ?P" + src.charAt(i), i - 2);
        case '#':
          while (i < n && src.charAt(i) != ')') {
            i++;
          }
          if (i >= n) {
            throw error("missing ), unterminated comment", open);
          }
          i++;
          // A comment is not an item: what precedes it keeps its repeatability, as in Python.
          return Prev.ATOM;
        case '=':
        case '!':
          throw unsupported("look-ahead assertions", open);
        case '<':
          if (i + 1 < n && (src.charAt(i + 1) == '=' || src.charAt(i + 1) == '!')) {
            throw unsupported("look-behind assertions", open);
          }
          if (i + 1 >= n) {
            throw error("unexpected end of pattern", i + 1);
          }
          throw error("unknown extension ?<" + src.charAt(i + 1), open + 1);
        case '(':
          throw unsupported("conditional groups", open);
        case '>':
          throw unsupported("atomic groups", open);
        default:
          if (isFlagChar(c) || c == '-') {
            return parseFlags(open, savedScope);
          }
          throw error("unknown extension ?" + c, open + 1);
      }
    }
    int group = ++groups;
    out.append('(');
    return finishGroup(open, savedScope, group);
  }

  private Prev finishGroup(int open, int savedScope, int group) throws EvalException {
    if (group > 0) {
      openGroups.push(group);
    }
    parseAlternation(true);
    if (i >= n) {
      throw error("missing ), unterminated subpattern", open);
    }
    i++; // ')'
    out.append(')');
    if (group > 0) {
      openGroups.pop();
    }
    scopeFlags = savedScope;
    return Prev.ATOM;
  }

  private static boolean isFlagChar(char c) {
    return c == 'a' || c == 'i' || c == 'L' || c == 'm' || c == 's' || c == 'u' || c == 'x';
  }

  private static int flagBit(char c) {
    switch (c) {
      case 'a':
        return FLAG_A;
      case 'i':
        return FLAG_I;
      case 'L':
        return FLAG_L;
      case 'm':
        return FLAG_M;
      case 's':
        return FLAG_S;
      case 'u':
        return FLAG_U;
      case 'x':
        return FLAG_X;
      default:
        return 0;
    }
  }

  /** Parses {@code (?flags)} or {@code (?flags-flags:...)}; {@code i} is at the first flag. */
  private Prev parseFlags(int open, int savedScope) throws EvalException {
    int add = 0;
    int del = 0;
    while (i < n && isFlagChar(src.charAt(i))) {
      char c = src.charAt(i);
      if (c == 'L') {
        throw error("bad inline flag: cannot use 'L' flag with a str pattern", i);
      }
      add |= flagBit(c);
      i++;
    }
    if ((add & FLAG_A) != 0 && (add & FLAG_U) != 0) {
      throw error("bad inline flag: flags 'a', 'u' and 'L' are incompatible", i);
    }
    if (i < n && src.charAt(i) == '-') {
      i++;
      if (i >= n) {
        throw error("missing flag", i);
      }
      while (i < n && isFlagChar(src.charAt(i))) {
        char c = src.charAt(i);
        if (c == 'a' || c == 'u' || c == 'L') {
          throw error("bad inline flag: cannot turn off flags 'a', 'u' and 'L'", i);
        }
        del |= flagBit(c);
        i++;
      }
      if (del == 0) {
        throw error("missing flag", i);
      }
      if (i >= n || src.charAt(i) != ':') {
        throw error("missing :", i);
      }
    }
    if (i >= n) {
      throw error("missing -, : or )", i);
    }
    char c = src.charAt(i);
    if (c == ')') {
      i++;
      if (del != 0) {
        throw error("missing :", i - 1);
      }
      // A global flag group: applies to the whole pattern (see translate()).
      globalFlags |= add;
      scopeFlags |= add;
      return Prev.NOTHING;
    }
    if (c != ':') {
      throw error(isLetter(c) ? "unknown flag" : "missing -, : or )", i);
    }
    i++;
    if ((add & del) != 0) {
      throw error("bad inline flag: flag turned on and off", i - 1);
    }
    scopeFlags = (scopeFlags | add) & ~del;
    if ((add & FLAG_A) != 0) {
      scopeFlags &= ~FLAG_U;
    }
    if ((add & FLAG_U) != 0) {
      scopeFlags &= ~FLAG_A;
    }
    StringBuilder re2 = new StringBuilder("(?");
    appendRe2Flags(re2, add);
    int re2Del = del & (FLAG_I | FLAG_M | FLAG_S);
    if (re2Del != 0) {
      re2.append('-');
      appendRe2Flags(re2, re2Del);
    }
    if (re2.length() == 2) {
      out.append("(?:");
    } else {
      out.append(re2).append(':');
    }
    return finishGroup(open, savedScope, -1);
  }

  static void appendRe2Flags(StringBuilder sb, int flags) {
    if ((flags & FLAG_I) != 0) {
      sb.append('i');
    }
    if ((flags & FLAG_M) != 0) {
      sb.append('m');
    }
    if ((flags & FLAG_S) != 0) {
      sb.append('s');
    }
  }

  /** Parses a group name terminated by {@code terminator} (consumed). */
  private String parseName(String terminator) throws EvalException {
    int start = i;
    int end = src.indexOf(terminator, i);
    if (end < 0) {
      throw error("missing " + terminator + ", unterminated name", start);
    }
    String name = src.substring(start, end);
    if (name.isEmpty()) {
      throw error("missing group name", start);
    }
    if (!isIdentifier(name)) {
      throw error("bad character in group name " + pyRepr(name), start);
    }
    i = end + 1;
    return name;
  }

  static boolean isIdentifier(String s) {
    if (s.isEmpty()) {
      return false;
    }
    int cp = s.codePointAt(0);
    if (cp != '_' && !Character.isUnicodeIdentifierStart(cp)) {
      return false;
    }
    for (int j = Character.charCount(cp); j < s.length(); j += Character.charCount(cp)) {
      cp = s.codePointAt(j);
      if (!Character.isUnicodeIdentifierPart(cp) || Character.isIdentifierIgnorable(cp)) {
        return false;
      }
    }
    return true;
  }

  static String pyRepr(String s) {
    return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
  }

  // --- Escapes outside classes -------------------------------------------------------------

  private Prev parseEscape() throws EvalException {
    int start = i;
    i++; // backslash
    if (i >= n) {
      throw error("bad escape (end of pattern)", start);
    }
    char c = src.charAt(i);
    switch (c) {
      case 'A':
        i++;
        out.append("\\A");
        return Prev.NOTHING;
      case 'Z':
        i++;
        emitMarker(Marker.END_OF_STRING);
        return Prev.NOTHING;
      case 'z':
        // Python 3.14 spelling of \Z (and RE2's).
        i++;
        emitMarker(Marker.END_OF_STRING);
        return Prev.NOTHING;
      case 'b':
        // RE2's word boundary uses ASCII word characters, even for Unicode patterns.
        i++;
        out.append("\\b");
        return Prev.NOTHING;
      case 'B':
        i++;
        out.append("\\B");
        return Prev.NOTHING;
      case 'd':
      case 'D':
      case 'w':
      case 'W':
      case 's':
      case 'S':
        i++;
        // Python does not case-fold \d \w \s under IGNORECASE; letting RE2 fold these large
        // classes would also make compiling them very slow.
        out.append(caseless() ? "(?-i:[" : "[").append(classRanges(c, ascii()))
            .append(caseless() ? "])" : "]");
        return Prev.ATOM;
      case 'p':
      case 'P':
        // RE2 Unicode classes (\pL, \p{Greek}); not Python syntax, kept for existing scripts.
        {
          int j = i + 1;
          if (j < n && src.charAt(j) == '{') {
            int close = src.indexOf('}', j);
            if (close < 0) {
              throw error("bad escape \\" + c, start);
            }
            j = close + 1;
          } else if (j < n) {
            j++;
          } else {
            throw error("bad escape \\" + c, start);
          }
          out.append('\\').append(src, i, j);
          i = j;
          return Prev.ATOM;
        }
      default:
        break;
    }
    if (c == '0') {
      i++;
      int value = 0;
      for (int k = 0; k < 2 && i < n && isOctal(src.charAt(i)); k++) {
        value = value * 8 + (src.charAt(i++) - '0');
      }
      emitLiteral(value);
      return Prev.ATOM;
    }
    if (isDigit(c)) {
      // An octal escape if three octal digits, else a group reference (as in sre_parse).
      i++;
      if (i < n && isDigit(src.charAt(i))) {
        char c2 = src.charAt(i);
        if (isOctal(c) && isOctal(c2) && i + 1 < n && isOctal(src.charAt(i + 1))) {
          int value = (c - '0') * 64 + (c2 - '0') * 8 + (src.charAt(i + 1) - '0');
          i += 2;
          if (value > 0377) {
            throw error("octal escape value " + src.substring(start, i) + " outside of range 0-0o377",
                start);
          }
          emitLiteral(value);
          return Prev.ATOM;
        }
        i++;
      }
      int group = Integer.parseInt(src.substring(start + 1, i));
      if (group <= groups) {
        if (openGroups.contains(group)) {
          throw error("cannot refer to an open group", start);
        }
        throw unsupported("backreferences", start);
      }
      throw error("invalid group reference " + group, start + 1);
    }
    int cp = parseCommonEscape(start, false);
    emitLiteral(cp);
    return Prev.ATOM;
  }

  /**
   * Parses an escape that means the same inside and outside a class and yields one code point;
   * {@code i} is at the character after the backslash and ends after the escape.
   */
  private int parseCommonEscape(int start, boolean inClass) throws EvalException {
    char c = src.charAt(i);
    switch (c) {
      case 'a':
        i++;
        return 7;
      case 'f':
        i++;
        return '\f';
      case 'n':
        i++;
        return '\n';
      case 'r':
        i++;
        return '\r';
      case 't':
        i++;
        return '\t';
      case 'v':
        i++;
        return 0x0b;
      case 'x':
        return parseHex(start, 2);
      case 'u':
        return parseHex(start, 4);
      case 'U':
        {
          int cp = parseHex(start, 8);
          if (cp > Character.MAX_CODE_POINT) {
            throw error("bad escape " + src.substring(start, i), start);
          }
          return cp;
        }
      case 'N':
        {
          i++;
          if (i >= n || src.charAt(i) != '{') {
            throw error("missing {", i);
          }
          int close = src.indexOf('}', i);
          if (close < 0 || close == i + 1) {
            throw error("missing character name", i + 1);
          }
          String name = src.substring(i + 1, close);
          try {
            int cp = Character.codePointOf(name);
            i = close + 1;
            return cp;
          } catch (IllegalArgumentException e) {
            throw error("undefined character name " + pyRepr(name), start);
          }
        }
      default:
        break;
    }
    if (c < 128 && (isLetter(c) || isDigit(c))) {
      throw error("bad escape \\" + c, start);
    }
    int cp = src.codePointAt(i);
    i += Character.charCount(cp);
    return cp;
  }

  private int parseHex(int start, int digits) throws EvalException {
    char kind = src.charAt(i);
    i++;
    int value = 0;
    for (int k = 0; k < digits; k++) {
      if (i >= n || Character.digit(src.charAt(i), 16) < 0 || src.charAt(i) > 127) {
        throw error("incomplete escape \\" + kind + src.substring(start + 2, i), start);
      }
      value = value * 16 + Character.digit(src.charAt(i), 16);
      i++;
    }
    return value;
  }

  // --- Character classes -----------------------------------------------------------------

  private void parseClass() throws EvalException {
    int open = i;
    i++; // '['
    boolean negate = false;
    if (i < n && src.charAt(i) == '^') {
      negate = true;
      i++;
    }
    // \d \w \s items, and literal code point ranges (lo, hi pairs), kept apart because Python
    // case-folds only the literals under IGNORECASE.
    StringBuilder sets = new StringBuilder();
    List<int[]> lits = new ArrayList<>();
    boolean first = true;
    while (true) {
      if (i >= n) {
        throw error("unterminated character set", open);
      }
      char c = src.charAt(i);
      if (c == ']' && !first) {
        i++;
        break;
      }
      first = false;
      int itemStart = i;
      String set = null;
      int lo = -1;
      if (c == '\\') {
        set = classEscapeSet();
        if (set == null) {
          lo = parseClassEscape();
        }
      } else {
        lo = src.codePointAt(i);
        i += Character.charCount(lo);
      }
      if (i + 1 < n && src.charAt(i) == '-' && src.charAt(i + 1) != ']') {
        i++; // '-'
        String hiSet = null;
        int hi;
        if (src.charAt(i) == '\\') {
          hiSet = classEscapeSet();
          hi = hiSet == null ? parseClassEscape() : -1;
        } else {
          hi = src.codePointAt(i);
          i += Character.charCount(hi);
        }
        if (set != null || hiSet != null || hi < lo) {
          throw error("bad character range " + src.substring(itemStart, i), itemStart);
        }
        lits.add(new int[] {lo, hi});
        continue;
      }
      if (set != null) {
        sets.append(set);
      } else {
        lits.add(new int[] {lo, lo});
      }
    }
    String neg = negate ? "^" : "";
    if (!caseless() || sets.length() == 0) {
      out.append('[').append(neg).append(sets);
      appendRanges(out, lits);
      out.append(']');
    } else if (!negate) {
      // RE2 folds the literals; the sets are matched case-sensitively.
      out.append("(?:(?-i:[").append(sets).append("])");
      if (!lits.isEmpty()) {
        out.append("|[");
        appendRanges(out, lits);
        out.append(']');
      }
      out.append(')');
    } else {
      // A negated class cannot be split into alternatives: fold the literals here.
      out.append("(?-i:[^").append(sets);
      appendRanges(out, CaseFolding.fold(lits));
      out.append("])");
    }
  }

  private static void appendRanges(StringBuilder sb, List<int[]> ranges) {
    for (int[] r : ranges) {
      appendClassChar(sb, r[0]);
      if (r[1] != r[0]) {
        sb.append('-');
        appendClassChar(sb, r[1]);
      }
    }
  }

  /** Simple case folding orbits (code points that match each other case-insensitively). */
  private static final class CaseFolding {
    private static final Map<Integer, int[]> ORBITS = build();

    private static Map<Integer, int[]> build() {
      Map<Integer, List<Integer>> byKey = new java.util.HashMap<>();
      for (int cp = 0; cp <= Character.MAX_CODE_POINT; cp++) {
        int key = Character.toLowerCase(Character.toUpperCase(cp));
        if (key != cp || Character.toUpperCase(cp) != cp) {
          byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(cp);
        }
      }
      Map<Integer, int[]> orbits = new java.util.HashMap<>();
      for (Map.Entry<Integer, List<Integer>> e : byKey.entrySet()) {
        List<Integer> members = e.getValue();
        if (!members.contains(e.getKey())) {
          members.add(e.getKey());
        }
        int[] orbit = members.stream().mapToInt(Integer::intValue).toArray();
        for (int cp : orbit) {
          orbits.put(cp, orbit);
        }
      }
      return orbits;
    }

    /** The ranges plus the case variants of their members (ranges over 4096 are kept as is). */
    static List<int[]> fold(List<int[]> ranges) {
      List<int[]> out = new ArrayList<>(ranges);
      for (int[] r : ranges) {
        if (r[1] - r[0] > 4096) {
          continue;
        }
        for (int cp = r[0]; cp <= r[1]; cp++) {
          int[] orbit = ORBITS.get(cp);
          if (orbit != null) {
            for (int other : orbit) {
              out.add(new int[] {other, other});
            }
          }
        }
      }
      return out;
    }
  }

  /** If {@code i} is at a {@code \d}-style escape, consumes it and returns its ranges. */
  private String classEscapeSet() {
    if (i + 1 < n) {
      char c = src.charAt(i + 1);
      if (c == 'd' || c == 'D' || c == 'w' || c == 'W' || c == 's' || c == 'S') {
        i += 2;
        return classRanges(c, ascii());
      }
    }
    return null;
  }

  /** Parses a single-character escape inside a class ({@code i} at the backslash). */
  private int parseClassEscape() throws EvalException {
    int start = i;
    i++;
    if (i >= n) {
      throw error("bad escape (end of pattern)", start);
    }
    char c = src.charAt(i);
    if (c == 'b') {
      i++;
      return 8;
    }
    if (isOctal(c)) {
      int value = 0;
      for (int k = 0; k < 3 && i < n && isOctal(src.charAt(i)); k++) {
        value = value * 8 + (src.charAt(i++) - '0');
      }
      if (value > 0377) {
        throw error("octal escape value " + src.substring(start, i) + " outside of range 0-0o377",
            start);
      }
      return value;
    }
    return parseCommonEscape(start, true);
  }

  // --- Literals and Unicode classes --------------------------------------------------------

  private void emitLiteral(int cp) {
    appendClassChar(out, cp);
  }

  /** Appends a code point in a form RE2 reads literally both inside and outside classes. */
  static void appendClassChar(StringBuilder sb, int cp) {
    if (cp < 128 && (isLetter((char) cp) || isDigit((char) cp) || cp == '_')) {
      sb.append((char) cp);
    } else {
      sb.append("\\x{").append(Integer.toHexString(cp)).append('}');
    }
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isOctal(char c) {
    return c >= '0' && c <= '7';
  }

  private static boolean isLetter(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
  }

  /** The class body (without brackets) for {@code \d \D \w \W \s \S}. */
  static String classRanges(char c, boolean ascii) {
    return (ascii ? UnicodeSets.ASCII : UnicodeSets.UNICODE).get(c);
  }

  /** Python's {@code \d \w \s} sets, as RE2 class bodies. */
  private static final class UnicodeSets {
    static final Map<Character, String> UNICODE = build(false);
    static final Map<Character, String> ASCII = build(true);

    private static Map<Character, String> build(boolean ascii) {
      int limit = ascii ? 128 : Character.MAX_CODE_POINT + 1;
      boolean[] digit = new boolean[limit];
      boolean[] word = new boolean[limit];
      boolean[] space = new boolean[limit];
      for (int cp = 0; cp < limit; cp++) {
        int type = Character.getType(cp);
        // CPython: \d is Unicode category Nd; \w is str.isalnum() or '_' (categories L* and N*);
        // \s is str.isspace(). For ASCII (re.ASCII or bytes) these are [0-9], [a-zA-Z0-9_] and
        // [ \t\n\r\f\v].
        digit[cp] = ascii ? (cp >= '0' && cp <= '9') : type == Character.DECIMAL_DIGIT_NUMBER;
        if (ascii) {
          word[cp] = Character.isLetterOrDigit(cp) || cp == '_';
          space[cp] = cp == ' ' || (cp >= '\t' && cp <= '\r');
        } else {
          switch (type) {
            case Character.UPPERCASE_LETTER:
            case Character.LOWERCASE_LETTER:
            case Character.TITLECASE_LETTER:
            case Character.MODIFIER_LETTER:
            case Character.OTHER_LETTER:
            case Character.DECIMAL_DIGIT_NUMBER:
            case Character.LETTER_NUMBER:
            case Character.OTHER_NUMBER:
              word[cp] = true;
              break;
            default:
              word[cp] = cp == '_';
          }
          space[cp] = isPythonSpace(cp);
        }
      }
      Map<Character, String> m = new java.util.HashMap<>();
      m.put('d', ranges(digit, true, ascii));
      m.put('D', ranges(digit, false, ascii));
      m.put('w', ranges(word, true, ascii));
      m.put('W', ranges(word, false, ascii));
      m.put('s', ranges(space, true, ascii));
      m.put('S', ranges(space, false, ascii));
      return m;
    }

    /** CPython's Py_UNICODE_ISSPACE (Unicode 15.0). */
    private static boolean isPythonSpace(int cp) {
      return (cp >= 0x09 && cp <= 0x0d)
          || (cp >= 0x1c && cp <= 0x20)
          || cp == 0x85
          || cp == 0xa0
          || cp == 0x1680
          || (cp >= 0x2000 && cp <= 0x200a)
          || cp == 0x2028
          || cp == 0x2029
          || cp == 0x202f
          || cp == 0x205f
          || cp == 0x3000;
    }

    /**
     * Ranges where {@code set[cp] == want}. For an ASCII complement the non-ASCII code points
     * belong to the complement too.
     */
    private static String ranges(boolean[] set, boolean want, boolean ascii) {
      StringBuilder sb = new StringBuilder();
      int limit = set.length;
      int cp = 0;
      while (cp < limit) {
        if (set[cp] != want) {
          cp++;
          continue;
        }
        int start = cp;
        while (cp < limit && set[cp] == want) {
          cp++;
        }
        int end = cp - 1;
        if (!want && ascii && cp == limit) {
          end = Character.MAX_CODE_POINT;
        }
        appendRange(sb, start, end);
      }
      if (!want && ascii && (limit == 0 || set[limit - 1])) {
        appendRange(sb, 128, Character.MAX_CODE_POINT);
      }
      return sb.toString();
    }

    private static void appendRange(StringBuilder sb, int start, int end) {
      appendClassChar(sb, start);
      if (end != start) {
        sb.append('-');
        appendClassChar(sb, end);
      }
    }
  }
}
