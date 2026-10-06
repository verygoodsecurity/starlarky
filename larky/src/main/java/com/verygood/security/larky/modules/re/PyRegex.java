package com.verygood.security.larky.modules.re;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.verygood.security.larky.modules.re.PyRegexTranslator.Marker;
import com.verygood.security.larky.modules.re.PyRegexTranslator.Translation;

import net.starlark.java.eval.EvalException;

/**
 * A Python {@code re} pattern running on RE2.
 *
 * <p>The pattern is translated once per class mode (Unicode for str input, ASCII for bytes input)
 * and compiled lazily into up to a handful of RE2 variants, which differ in how the input-dependent
 * {@link Marker}s are spelled and in how the match is anchored:
 *
 * <ul>
 *   <li>Python's {@code $} (without MULTILINE) matches at the end and before a final newline. When
 *       no newline occurs before the last character of the searched text this is exactly RE2's
 *       {@code (?m:$)}. Otherwise the pattern runs twice: with {@code $} as {@code \z} on the text,
 *       and on the text without its final newline; the leftmost match wins (the full text on a
 *       tie).
 *   <li>A match anchored at {@code pos > 0} runs on {@code text[pos-1:]} with the pattern prefixed
 *       by {@code (?s:.)}, so that {@code ^}, {@code \A} and {@code \b} see the character before
 *       {@code pos}, as in Python.
 * </ul>
 *
 * <p>All positions are UTF-16 indices, like Larky's string indices.
 */
final class PyRegex {

  static final int SEARCH = 0;
  static final int MATCH = 1;
  static final int FULLMATCH = 2;

  /** How {@code $} and {@code \Z} are spelled in a variant. */
  private static final int DOLLAR_MULTILINE = 0; // $ -> (?m:$), \Z -> \z
  private static final int DOLLAR_END = 1; // $ -> \z, \Z -> \z
  private static final int DOLLAR_TRUNCATED = 2; // $ -> \z, \Z -> never (text lost its final \n)

  private static final String NEVER = "[^\\x{0}-\\x{10ffff}]";

  final String source;
  final int flags;
  private final Translation unicode;
  private volatile Translation ascii;
  // Indexed by variantKey(); RE2 Patterns are immutable, so a racy lazy init is harmless.
  private final Pattern[] variants = new Pattern[2 * 3 * 2 * 2];

  PyRegex(String source, int flags) throws EvalException {
    this.source = source;
    this.flags = flags;
    this.unicode = PyRegexTranslator.translate(source, flags, false);
    // Compile the main variant now so that errors RE2 reports surface at compile time.
    variant(false, DOLLAR_END, false, false);
  }

  int groups() {
    return unicode.groups;
  }

  Map<String, Integer> groupIndex() {
    return unicode.groupIndex;
  }

  /** The RE2 pattern used for plain searches of str input (for the legacy re2j API). */
  Pattern mainPattern() throws EvalException {
    return variant(false, DOLLAR_END, false, false);
  }

  private Translation translation(boolean asciiMode) throws EvalException {
    if (!asciiMode) {
      return unicode;
    }
    Translation t = ascii;
    if (t == null) {
      t = PyRegexTranslator.translate(source, flags, true);
      ascii = t;
    }
    return t;
  }

  private Pattern variant(boolean asciiMode, int dollar, boolean prefixed, boolean full)
      throws EvalException {
    int key = ((asciiMode ? 1 : 0) * 3 + dollar) * 4 + (prefixed ? 2 : 0) + (full ? 1 : 0);
    Pattern p = variants[key];
    if (p != null) {
      return p;
    }
    Translation t = translation(asciiMode);
    StringBuilder sb = new StringBuilder();
    int re2Inline = t.flags & (PyRegexTranslator.FLAG_I | PyRegexTranslator.FLAG_M
        | PyRegexTranslator.FLAG_S);
    if (re2Inline != 0) {
      sb.append("(?");
      PyRegexTranslator.appendRe2Flags(sb, re2Inline);
      sb.append(')');
    }
    if (prefixed) {
      sb.append("(?s:.)");
    }
    sb.append("(?:");
    for (Object piece : t.pieces) {
      if (piece == Marker.DOLLAR) {
        sb.append(dollar == DOLLAR_MULTILINE ? "(?m:$)" : "\\z");
      } else if (piece == Marker.END_OF_STRING) {
        sb.append(dollar == DOLLAR_TRUNCATED ? NEVER : "\\z");
      } else {
        sb.append((String) piece);
      }
    }
    sb.append(')');
    if (full) {
      sb.append("\\z");
    }
    int re2Flags = (t.flags & PyRegexTranslator.FLAG_LONGEST_MATCH) != 0
        ? Pattern.LONGEST_MATCH : 0;
    try {
      p = Pattern.compile(sb.toString(), re2Flags);
    } catch (PatternSyntaxException e) {
      throw new EvalException("re.error: " + e.getDescription() + " (unsupported by Larky's "
          + "linear-time regex engine)");
    }
    variants[key] = p;
    return p;
  }

  /**
   * Runs the pattern on {@code text} (already cut at endpos) from {@code pos}. Returns the group
   * spans {start0, end0, start1, end1, ...} (-1 for groups that did not participate), or null.
   *
   * <p>{@code mustAdvance} (for searches that continue after an empty match at {@code pos}) rejects
   * an empty match at {@code pos}; the search then continues from the next character.
   */
  int[] run(CharSequence text, int pos, int kind, boolean mustAdvance, boolean asciiMode)
      throws EvalException {
    int n = text.length();
    if (pos > n) {
      return null;
    }
    Translation t = translation(asciiMode);
    if (!t.hasDollar) {
      return exec(asciiMode, DOLLAR_END, text, pos, kind, mustAdvance);
    }
    boolean interiorNewline = false;
    for (int j = pos; j < n - 1; j++) {
      if (text.charAt(j) == '\n') {
        interiorNewline = true;
        break;
      }
    }
    if (!interiorNewline) {
      return exec(asciiMode, DOLLAR_MULTILINE, text, pos, kind, mustAdvance);
    }
    int[] best = exec(asciiMode, DOLLAR_END, text, pos, kind, mustAdvance);
    if (kind != FULLMATCH && text.charAt(n - 1) == '\n') {
      int[] cut = exec(asciiMode, DOLLAR_TRUNCATED, text.subSequence(0, n - 1), pos, kind,
          mustAdvance);
      if (cut != null && (best == null || cut[0] < best[0])) {
        best = cut;
      }
    }
    return best;
  }

  private int[] exec(boolean asciiMode, int dollar, CharSequence text, int pos, int kind,
      boolean mustAdvance) throws EvalException {
    int n = text.length();
    if (pos > n) {
      return null;
    }
    if (kind == SEARCH) {
      Matcher m = variant(asciiMode, dollar, false, false).matcher(text);
      if (!m.find(pos)) {
        return null;
      }
      if (mustAdvance && m.start() == pos && m.end() == pos) {
        if (pos >= n) {
          return null;
        }
        int next = pos + Character.charCount(Character.codePointAt(text, pos));
        if (!m.find(next)) {
          return null;
        }
      }
      return spans(m, 0, false);
    }
    boolean full = kind == FULLMATCH;
    if (pos == 0) {
      Matcher m = variant(asciiMode, dollar, false, full).matcher(text);
      return m.lookingAt() ? spans(m, 0, false) : null;
    }
    Matcher m = variant(asciiMode, dollar, true, full).matcher(text.subSequence(pos - 1, n));
    return m.lookingAt() ? spans(m, pos - 1, true) : null;
  }

  private static int[] spans(Matcher m, int offset, boolean prefixed) {
    int groups = m.groupCount();
    int[] spans = new int[2 * (groups + 1)];
    for (int g = 0; g <= groups; g++) {
      int s = m.start(g);
      if (s < 0) {
        spans[2 * g] = -1;
        spans[2 * g + 1] = -1;
      } else {
        spans[2 * g] = s + offset;
        spans[2 * g + 1] = m.end(g) + offset;
      }
    }
    if (prefixed) {
      // The (?s:.) prefix is part of group 0 in the prefixed variant.
      spans[0] += Character.charCount(Character.codePointAt(m.group(), 0));
    }
    return spans;
  }

  /**
   * Python's re.split: pieces of the text between matches, and the groups of each match.
   * Returns flat (start, end) pairs, (-1, -1) for groups that did not participate.
   */
  int[] split(CharSequence text, int maxsplit, boolean asciiMode) throws EvalException {
    List<int[]> out = new ArrayList<>();
    int last = 0;
    int pos = 0;
    int count = 0;
    boolean mustAdvance = false;
    while (maxsplit <= 0 || count < maxsplit) {
      int[] s = run(text, pos, SEARCH, mustAdvance, asciiMode);
      if (s == null) {
        break;
      }
      out.add(new int[] {last, s[0]});
      for (int g = 1; 2 * g < s.length; g++) {
        out.add(new int[] {s[2 * g], s[2 * g + 1]});
      }
      last = s[1];
      pos = s[1];
      mustAdvance = s[0] == s[1];
      count++;
    }
    out.add(new int[] {last, text.length()});
    int[] flat = new int[out.size() * 2];
    for (int k = 0; k < out.size(); k++) {
      flat[2 * k] = out.get(k)[0];
      flat[2 * k + 1] = out.get(k)[1];
    }
    return flat;
  }

  // --- Replacement templates ------------------------------------------------------------------

  /**
   * Parses a replacement template as CPython's {@code re._parser.parse_template} does. Returns a
   * list of literal Strings and Integer group numbers.
   */
  List<Object> parseTemplate(String repl) throws EvalException {
    List<Object> items = new ArrayList<>();
    StringBuilder literal = new StringBuilder();
    int n = repl.length();
    int i = 0;
    while (i < n) {
      char c = repl.charAt(i);
      if (c != '\\') {
        literal.append(c);
        i++;
        continue;
      }
      int start = i;
      i++;
      if (i >= n) {
        throw PyRegexTranslator.error("bad escape (end of pattern)", start);
      }
      c = repl.charAt(i);
      i++;
      if (c == 'g') {
        if (i >= n || repl.charAt(i) != '<') {
          throw PyRegexTranslator.error("missing <", i);
        }
        i++;
        int nameStart = i;
        int close = repl.indexOf('>', i);
        if (close < 0) {
          throw PyRegexTranslator.error("missing >, unterminated name", nameStart);
        }
        String name = repl.substring(nameStart, close);
        if (name.isEmpty()) {
          throw PyRegexTranslator.error("missing group name", nameStart);
        }
        i = close + 1;
        int index;
        if (name.chars().allMatch(ch -> ch >= '0' && ch <= '9')) {
          try {
            index = Integer.parseInt(name);
          } catch (NumberFormatException e) {
            index = Integer.MAX_VALUE;
          }
          if (index > groups()) {
            throw PyRegexTranslator.error("invalid group reference " + name, nameStart);
          }
        } else if (PyRegexTranslator.isIdentifier(name)) {
          Integer idx = groupIndex().get(name);
          if (idx == null) {
            throw new EvalException(
                "IndexError: unknown group name " + PyRegexTranslator.pyRepr(name));
          }
          index = idx;
        } else {
          throw PyRegexTranslator.error(
              "bad character in group name " + PyRegexTranslator.pyRepr(name), nameStart);
        }
        addGroup(items, literal, index);
      } else if (c == '0') {
        int value = 0;
        for (int k = 0; k < 2 && i < n && isOctal(repl.charAt(i)); k++) {
          value = value * 8 + (repl.charAt(i++) - '0');
        }
        literal.append((char) (value & 0xff));
      } else if (c >= '1' && c <= '9') {
        int digitsEnd = i;
        if (i < n && Character.isDigit(repl.charAt(i)) && repl.charAt(i) < 128) {
          digitsEnd = i + 1;
          if (isOctal(c) && isOctal(repl.charAt(i)) && i + 1 < n && isOctal(repl.charAt(i + 1))) {
            int value = (c - '0') * 64 + (repl.charAt(i) - '0') * 8 + (repl.charAt(i + 1) - '0');
            if (value > 0377) {
              throw PyRegexTranslator.error("octal escape value " + repl.substring(start, i + 2)
                  + " outside of range 0-0o377", start);
            }
            literal.append((char) value);
            i += 2;
            continue;
          }
        }
        int group = Integer.parseInt(repl.substring(start + 1, digitsEnd));
        i = digitsEnd;
        if (group > groups()) {
          throw PyRegexTranslator.error("invalid group reference " + group, start + 1);
        }
        addGroup(items, literal, group);
      } else {
        switch (c) {
          case 'a':
            literal.append((char) 7);
            break;
          case 'b':
            literal.append('\b');
            break;
          case 'f':
            literal.append('\f');
            break;
          case 'n':
            literal.append('\n');
            break;
          case 'r':
            literal.append('\r');
            break;
          case 't':
            literal.append('\t');
            break;
          case 'v':
            literal.append((char) 0x0b);
            break;
          case '\\':
            literal.append('\\');
            break;
          default:
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
              throw PyRegexTranslator.error("bad escape \\" + c, start);
            }
            // Unknown escapes of other characters are kept as they are.
            literal.append('\\').append(c);
        }
      }
    }
    if (literal.length() > 0) {
      items.add(literal.toString());
    }
    return items;
  }

  private static void addGroup(List<Object> items, StringBuilder literal, int group) {
    if (literal.length() > 0) {
      items.add(literal.toString());
      literal.setLength(0);
    }
    items.add(group);
  }

  private static boolean isOctal(char c) {
    return c >= '0' && c <= '7';
  }
}
