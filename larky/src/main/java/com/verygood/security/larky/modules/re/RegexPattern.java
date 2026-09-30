package com.verygood.security.larky.modules.re;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

import net.starlark.java.eval.StarlarkBytes;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.StarlarkSemantics;

// java <> larky objects
public class RegexPattern implements StarlarkValue {

  @StarlarkMethod(name = "CASE_INSENSITIVE", doc = "Flag: case insensitive matching.", structField = true)
  public StarlarkInt CASE_INSENSITIVE() {
    return StarlarkInt.of(Pattern.CASE_INSENSITIVE);
  }

  @StarlarkMethod(name = "DISABLE_UNICODE_GROUPS", doc = "Flag: Unicode groups (e.g. \\p\\ Greek\\ ) will be syntax errors", structField = true)
  public StarlarkInt DISABLE_UNICODE_GROUPS() {
    return StarlarkInt.of(Pattern.DISABLE_UNICODE_GROUPS);
  }

  @StarlarkMethod(name = "DOTALL", doc = "Flag: dot (.) matches all characters, including newline.", structField = true)
  public StarlarkInt DOTALL() {
    return StarlarkInt.of(Pattern.DOTALL);
  }

  @StarlarkMethod(name = "LONGEST_MATCH", doc = "Flag: matches longest possible string.", structField = true)
  public StarlarkInt LONGEST_MATCH() {
    return StarlarkInt.of(Pattern.LONGEST_MATCH);
  }

  @StarlarkMethod(name = "MULTILINE", doc = "Flag: multiline matching: ^ and $ match at beginning and end of line, not just beginning and end of input.", structField = true)
  public StarlarkInt MULTILINE() {
    return StarlarkInt.of(Pattern.MULTILINE);
  }

  private Pattern pattern;

  protected RegexPattern pattern(Pattern pattern) {
    this.pattern = pattern;
    return this;
  }

  public Map<String, Integer> namedGroups() {
    return pattern.namedGroups();
  }

  @Override
  public void str(Printer printer, StarlarkSemantics semantics) {
    printer.append(py != null ? py.source : pattern.toString());
  }

  @StarlarkMethod(
      name = "compile",
      doc = "Creates and returns a new Pattern corresponding to compiling regex with the given flags." +
          "If flags is not passed, it defaults to 0",
      parameters = {
          @Param(name = "regex"),
          @Param(
              name = "flags",
              allowedTypes = {
                  @ParamType(type = StarlarkInt.class),
              },
              defaultValue = "0")
      })
  public static RegexPattern compile(String regex, StarlarkInt flags) throws EvalException {
    int flag = flags.toIntUnchecked();
    try {
      return new RegexPattern().pattern(compiled(regex, flag));
    } catch (PatternSyntaxException | IllegalArgumentException e) {
      throw new EvalException("re.error: " + e.getMessage());
    }
  }

  // --- Python re semantics (used by stdlib/re.star) ---------------------------------------------

  /** Set for patterns made by py_compile: the Python pattern this RE2 pattern was translated from. */
  private PyRegex py;

  private record PyKey(String regex, int flags) {}

  // Translated patterns by source and flags; see COMPILED.
  private static final com.google.common.cache.Cache<PyKey, RegexPattern> PY_COMPILED =
      com.google.common.cache.CacheBuilder.newBuilder()
          .maximumWeight(1 << 20)
          .<PyKey, RegexPattern>weigher((key, pattern) -> key.regex().length() + 1)
          .build();

  @StarlarkMethod(
      name = "py_compile",
      doc = "Compiles a Python re pattern (translated to RE2). flags are re.star's RegexFlags.",
      parameters = {
          @Param(name = "regex", allowedTypes = {@ParamType(type = String.class)}),
          @Param(name = "flags", allowedTypes = {@ParamType(type = StarlarkInt.class)},
              defaultValue = "0")
      })
  public static RegexPattern pyCompile(String regex, StarlarkInt flags) throws EvalException {
    PyKey key = new PyKey(regex, flags.toIntUnchecked());
    RegexPattern compiled = PY_COMPILED.getIfPresent(key);
    if (compiled == null) {
      PyRegex py = new PyRegex(regex, key.flags());
      compiled = new RegexPattern().pattern(py.mainPattern());
      compiled.py = py;
      PY_COMPILED.put(key, compiled);
    }
    return compiled;
  }

  private PyRegex py() throws EvalException {
    if (py == null) {
      throw new EvalException("re: pattern was not compiled with py_compile");
    }
    return py;
  }

  @StarlarkMethod(name = "py_groups", doc = "Number of capturing groups.", structField = true)
  public StarlarkInt pyGroups() throws EvalException {
    return StarlarkInt.of(py().groups());
  }

  @StarlarkMethod(
      name = "py_groupindex",
      doc = "A new dict mapping group names to group numbers.",
      useStarlarkThread = true)
  public Dict<String, StarlarkInt> pyGroupIndexDict(StarlarkThread thread) throws EvalException {
    Dict<String, StarlarkInt> d = Dict.of(thread.mutability());
    for (Map.Entry<String, Integer> e : py().groupIndex().entrySet()) {
      d.putEntry(e.getKey(), StarlarkInt.of(e.getValue()));
    }
    return d;
  }

  @StarlarkMethod(
      name = "py_group_index",
      doc = "The group number for a group number or name; fails with IndexError if there is none.",
      parameters = {@Param(name = "group")})
  public StarlarkInt pyGroupIndex(Object group) throws EvalException {
    PyRegex p = py();
    if (group instanceof StarlarkInt) {
      StarlarkInt g = (StarlarkInt) group;
      int idx = g.signum() < 0 ? -1 : g.toInt("group");
      if (idx >= 0 && idx <= p.groups()) {
        return StarlarkInt.of(idx);
      }
    } else if (group instanceof String) {
      Integer idx = p.groupIndex().get(group);
      if (idx != null) {
        return StarlarkInt.of(idx);
      }
    }
    throw new EvalException("IndexError: no such group");
  }

  private static CharSequence pyInput(Object input) {
    if (input instanceof StarlarkBytes) {
      return new ByteArrayCharSequence(((StarlarkBytes) input).toByteArray());
    }
    return (String) input;
  }

  private Object pyRun(Object input, StarlarkInt pos, Object endpos, int kind, boolean mustAdvance)
      throws EvalException {
    CharSequence text = pyInput(input);
    int len = text.length();
    // As CPython's state_init: clamp pos and endpos to [0, len].
    int start = Math.max(0, Math.min(pos.toInt("pos"), len));
    int end = len;
    if (endpos != Starlark.NONE) {
      end = Math.max(0, Math.min(((StarlarkInt) endpos).toInt("endpos"), len));
    }
    if (end < start) {
      return Starlark.NONE;
    }
    if (end < len) {
      text = text.subSequence(0, end);
    }
    int[] spans = py().run(text, start, kind, mustAdvance, input instanceof StarlarkBytes);
    if (spans == null) {
      return Starlark.NONE;
    }
    return intTuple(spans);
  }

  private static Tuple intTuple(int[] values) {
    Object[] out = new Object[values.length];
    for (int i = 0; i < values.length; i++) {
      out[i] = StarlarkInt.of(values[i]);
    }
    return Tuple.of(out);
  }

  @StarlarkMethod(
      name = "py_search",
      doc = "Python's Pattern.search: None, or the flat tuple of group spans (-1 when unmatched)."
          + " must_advance rejects an empty match at pos (for iterating after an empty match).",
      parameters = {
          @Param(name = "string", allowedTypes = {
              @ParamType(type = String.class), @ParamType(type = StarlarkBytes.class)}),
          @Param(name = "pos", allowedTypes = {@ParamType(type = StarlarkInt.class)},
              defaultValue = "0"),
          @Param(name = "endpos", allowedTypes = {
              @ParamType(type = StarlarkInt.class), @ParamType(type = NoneType.class)},
              defaultValue = "None"),
          @Param(name = "must_advance", allowedTypes = {@ParamType(type = Boolean.class)},
              defaultValue = "False")
      })
  public Object pySearch(Object string, StarlarkInt pos, Object endpos, Boolean mustAdvance)
      throws EvalException {
    return pyRun(string, pos, endpos, PyRegex.SEARCH, mustAdvance);
  }

  @StarlarkMethod(
      name = "py_match",
      doc = "Python's Pattern.match (anchored at pos); returns spans as py_search.",
      parameters = {
          @Param(name = "string", allowedTypes = {
              @ParamType(type = String.class), @ParamType(type = StarlarkBytes.class)}),
          @Param(name = "pos", allowedTypes = {@ParamType(type = StarlarkInt.class)},
              defaultValue = "0"),
          @Param(name = "endpos", allowedTypes = {
              @ParamType(type = StarlarkInt.class), @ParamType(type = NoneType.class)},
              defaultValue = "None")
      })
  public Object pyMatch(Object string, StarlarkInt pos, Object endpos) throws EvalException {
    return pyRun(string, pos, endpos, PyRegex.MATCH, false);
  }

  @StarlarkMethod(
      name = "py_fullmatch",
      doc = "Python's Pattern.fullmatch; returns spans as py_search.",
      parameters = {
          @Param(name = "string", allowedTypes = {
              @ParamType(type = String.class), @ParamType(type = StarlarkBytes.class)}),
          @Param(name = "pos", allowedTypes = {@ParamType(type = StarlarkInt.class)},
              defaultValue = "0"),
          @Param(name = "endpos", allowedTypes = {
              @ParamType(type = StarlarkInt.class), @ParamType(type = NoneType.class)},
              defaultValue = "None")
      })
  public Object pyFullmatch(Object string, StarlarkInt pos, Object endpos) throws EvalException {
    return pyRun(string, pos, endpos, PyRegex.FULLMATCH, false);
  }

  @StarlarkMethod(
      name = "py_split",
      doc = "Python's Pattern.split, as a flat tuple of (start, end) spans of the pieces and"
          + " groups; (-1, -1) for a group that did not participate.",
      parameters = {
          @Param(name = "string", allowedTypes = {
              @ParamType(type = String.class), @ParamType(type = StarlarkBytes.class)}),
          @Param(name = "maxsplit", allowedTypes = {@ParamType(type = StarlarkInt.class)},
              defaultValue = "0")
      })
  public Tuple pySplit(Object string, StarlarkInt maxsplit) throws EvalException {
    return intTuple(py().split(pyInput(string), maxsplit.toInt("maxsplit"),
        string instanceof StarlarkBytes));
  }

  @StarlarkMethod(
      name = "py_template",
      doc = "Parses a replacement template (as for re.sub) into a tuple of str literals and"
          + " group numbers. A bytes template is read as Latin-1.",
      parameters = {
          @Param(name = "repl", allowedTypes = {
              @ParamType(type = String.class), @ParamType(type = StarlarkBytes.class)})
      })
  public Tuple pyTemplate(Object repl) throws EvalException {
    List<Object> items = py().parseTemplate(pyInput(repl).toString());
    Object[] out = new Object[items.size()];
    for (int i = 0; i < out.length; i++) {
      Object item = items.get(i);
      out[i] = item instanceof Integer ? StarlarkInt.of((Integer) item) : item;
    }
    return Tuple.of(out);
  }

  @StarlarkMethod(
      name = "py_text",
      doc = "The text matches are taken from: the string itself, or bytes read as Latin-1 (as"
          + " earlier Larky versions did, matches on bytes are str).",
      parameters = {
          @Param(name = "string", allowedTypes = {
              @ParamType(type = String.class), @ParamType(type = StarlarkBytes.class)})
      })
  public String pyText(Object string) {
    return pyInput(string).toString();
  }

  private record CompileKey(String regex, int flags) {}

  // Compiled patterns by source and flags: re.match(pattern_string, ...) and the other
  // module-level functions compile their pattern on every call. re2j Patterns are immutable and
  // thread-safe; the cache is bounded by the total length of the patterns, which come from scripts.
  private static final com.google.common.cache.Cache<CompileKey, Pattern> COMPILED =
      com.google.common.cache.CacheBuilder.newBuilder()
          .maximumWeight(1 << 20)
          .<CompileKey, Pattern>weigher((key, pattern) -> key.regex().length() + 1)
          .build();

  private static Pattern compiled(String regex, int flags) {
    CompileKey key = new CompileKey(regex, flags);
    Pattern pattern = COMPILED.getIfPresent(key);
    if (pattern == null) {
      pattern = Pattern.compile(regex, flags); // throws for an invalid pattern, which is not cached
      COMPILED.put(key, pattern);
    }
    return pattern;
  }

  @StarlarkMethod(
      name = "matches",
      doc = "Matches a string against a regular expression.",
      parameters = {
          @Param(name = "regex"),
          @Param(
              name = "input",
              allowedTypes = {
                  @ParamType(type = String.class),
              })
      })
  public static boolean matches(String regex, String input) {
    return Pattern.matches(regex, input);
  }

  @StarlarkMethod(
      name = "quote",
      doc = "",
      parameters = {
          @Param(
              name = "s",
              allowedTypes = {
                  @ParamType(type = String.class),
              })
      })
  public static String quote(String s) {
    return Pattern.quote(s);
  }

  @StarlarkMethod(
      name = "flags",
      doc = ""
  )
  public StarlarkInt flags() {
    return StarlarkInt.of(pattern.flags());
  }

  @StarlarkMethod(name = "pattern", doc = "")
  public String pattern() {
    return py != null ? py.source : pattern.pattern();
  }

  /** The RE2 source of the compiled pattern (for py_compile patterns, the translation). */
  String re2Source() {
    return pattern.pattern();
  }

  @StarlarkMethod(
      name = "matcher",
      doc = "Creates a new Matcher matching the pattern against the input.\n",
      parameters = {
          @Param(
              name = "input",
              allowedTypes = {
                @ParamType(type = String.class),
                @ParamType(type = StarlarkBytes.class),
                @ParamType(type = StarlarkBytes.class),
              })
      })
  public RegexMatcher matcher(Object inputO) throws EvalException {
    if(String.class.isAssignableFrom(inputO.getClass())) {
      String input = (String) inputO;
      return new RegexMatcher(this, pattern.matcher(input), input);
    }

    byte[] input;
    if (StarlarkBytes.class.isAssignableFrom(inputO.getClass())) {
      StarlarkBytes b = ((StarlarkBytes) inputO);
      input =new byte[b.size()];
      for (int i = 0, loopLength = b.size(); i < loopLength; i++) {
        input[i] = b.byteAt(i);
      }
    } else if(StarlarkBytes.class.isAssignableFrom(inputO.getClass())) {
      input = ((StarlarkBytes) inputO).toByteArray();
    } else {
      throw new EvalException("Invalid larky byte type! " + inputO.getClass());
    }
    return new RegexMatcher(this, pattern.matcher(input), new ByteArrayCharSequence(input));
  }

  static class ByteArrayCharSequence implements CharSequence {
    public static final byte[] EMPTY_ARRAY = {};

    /** The underlying byte array. */
    	private byte[] b;
    	/** The first valid byte in {@link #b}. */
    	private int offset;
    	/** The number of valid bytes in {@link #b}, starting at {@link #offset}. */
    	private int length;

    	/** Creates a new byte-array character sequence using the provided byte-array fragment.
    	 *
    	 * @param b a byte array.
    	 * @param offset the first valid byte in <code>b</code>.
    	 * @param length the number of valid bytes in <code>b</code>, starting at <code>offset</code>.
    	 */
    	public ByteArrayCharSequence(final byte[] b, int offset, int length) {
    		wrap(b, offset, length);
    	}

    	/** Creates a new byte-array character sequence using the provided byte array.
    	 *
    	 * @param b a byte array.
    	 */
    	public ByteArrayCharSequence(final byte[] b) {
    		this(b, 0, b.length);
    	}

    	/** Creates a new empty byte-array character sequence.
    	 */
    	public ByteArrayCharSequence() {
    		this(EMPTY_ARRAY);
    	}

    	/** Wraps a byte-array fragment into this byte-array character sequence.
    	 *
    	 * @param b a byte array.
    	 * @param offset the first valid byte in <code>b</code>.
    	 * @param length the number of valid bytes in <code>b</code>, starting at <code>offset</code>.
    	 * @return this byte-array character sequence.
    	 */
    	public ByteArrayCharSequence wrap(final byte[] b, int offset, int length) {
          ensureOffsetLength(b.length, offset, length);
          this.b = b;
          this.offset = offset;
          this.length = length;
          return this;
    	}
    /** Ensures that a range given by an offset and a length fits an array of given length.
    	 *
    	 * <p>This method may be used whenever an array range check is needed.
    	 *
    	 * @param arrayLength an array length.
    	 * @param offset a start index for the fragment
    	 * @param length a length (the number of elements in the fragment).
    	 * @throws IllegalArgumentException if {@code length} is negative.
    	 * @throws ArrayIndexOutOfBoundsException if {@code offset} is negative or {@code offset}+{@code length} is greater than {@code arrayLength}.
    	 */
    	public static void ensureOffsetLength(final int arrayLength, final int offset, final int length) {
    		if (offset < 0) throw new ArrayIndexOutOfBoundsException("Offset (" + offset + ") is negative");
    		if (length < 0) throw new IllegalArgumentException("Length (" + length + ") is negative");
    		if (offset + length > arrayLength) throw new ArrayIndexOutOfBoundsException("Last index (" + (offset + length) + ") is greater than array length (" + arrayLength + ")");
    	}

    	/** Wraps a byte array into this byte-array character sequence.
    	 *
    	 * @param b a byte array.
    	 */
    	public void wrap(final byte[] b) {
    		wrap(b, 0, b.length);
    	}

    	@Override
    	public int length() {
    		return length;
    	}

    	@Override
    	public char charAt(int index) {
    		if (index < 0 || index >= length) throw new IndexOutOfBoundsException(Integer.toString(index));
    		return (char)(b[offset + index] & 0xFF);
    	}

    	@Override
    	public CharSequence subSequence(int start, int end) {
    		if (start < 0 || end > length || end < 0 || end < start) throw new IndexOutOfBoundsException();
    		return new ByteArrayCharSequence(b, start + offset, end - start);
    	}

    	@Override
    	public String toString() {
    		final StringBuilder builder = new StringBuilder();
    		for(int i = 0; i < length; i++) builder.append((char)(b[offset + i] & 0xFF));
    		return builder.toString();
    	}

    	@Override
        public int hashCode() {
        	int h = 0;
        	for (int i = 0; i < length; i++) h = 31 * h + b[offset + i];
            return h;
        }

  }

  @StarlarkMethod(
      name = "split",
      doc = "",
      parameters = {
          @Param(
              name = "input",
              allowedTypes = {
                  @ParamType(type = String.class),
              }),
          @Param(
              name = "limit",
              allowedTypes = {
                  @ParamType(type = StarlarkInt.class)
              },
              defaultValue = "0"
          )
      })
  public StarlarkList<Object> split(String input, StarlarkInt limit) {
    Object[] strings = _py_re_split_impl(input, limit.toIntUnchecked());
    return StarlarkList.immutableCopyOf(Arrays.asList(strings));
  }

  private String[] _jdk_split_impl(CharSequence input, int limit) {
    ArrayList<String> matchList = new ArrayList<>();
    Matcher m = pattern.matcher(input);

    int index = 0;
    boolean matchLimited = limit > 0;
    // Add segments before each match found
    while (m.find()) {
      if (!matchLimited || matchList.size() < limit - 1) {
        if (index == 0 && index == m.start() && m.start() == m.end()) {
          // no empty leading substring included for zero-width match
          // at the beginning of the input char sequence.
          continue;
        }
        String match = input.subSequence(index, m.start()).toString();
        matchList.add(match);
        index = m.end();
      } else if (matchList.size() == limit - 1) { // last one
        String match = input.subSequence(index,
            input.length()).toString();
        matchList.add(match);
        index = m.end();

      }
    }
    // If no match was found, return this
    if (index == 0) {
      return new String[]{input.toString()};
    }
    if (!matchLimited || matchList.size() < limit) {
      // Add remaining segment
      matchList.add(input.subSequence(index, input.length()).toString());
    }
    // Construct result
    int resultSize = matchList.size();
    if (limit == 0) {
      while (resultSize > 0 && matchList.get(resultSize - 1).equals("")) {
        resultSize--;
      }
    }
    String[] result = new String[resultSize];
    return matchList.subList(0, resultSize).toArray(result);
  }

  private Object[] _py_re_split_impl(CharSequence input, int limit) {
    Matcher m = pattern.matcher(input);
    ArrayList<Object> matchList = new ArrayList<>();
    boolean matchLimited = limit > 0;
    boolean has_capture = m.groupCount() > 0;
    int index = 0;
    String match;

    while (m.find()) {
      if (!matchLimited || matchList.size() <= limit - 1) {
        match = input.subSequence(index, m.start()).toString();
        matchList.add(match);
        index = m.end();
      } else if (matchList.size() == limit - 1) { // last one
        match = input.subSequence(index,
            input.length()).toString();
        matchList.add(match);
        index = m.end();
      }
      if (has_capture) {
        // Check if there's capture groups and add them
        for (int i = 0; i < m.groupCount(); ++i) {
          match = m.group(i + 1);
          matchList.add(match == null ? Starlark.NONE : match);
        }
      }
    }

    // If no match was found, return this
    if (index == 0) {
      return new String[]{input.toString()};
    }
    // NOTE: If maxsplit is nonzero, at most maxsplit splits occur,
    //       and the remainder of the string is returned as the final
    //       element of the list.
    if (!matchLimited || matchList.size() <= limit) {
      // Add remaining segment
      matchList.add(input.subSequence(index, input.length()).toString());
    }

    return matchList.toArray(new Object[0]);
  }

  @StarlarkMethod(
      name = "group_count",
      doc = "Returns the number of subgroups in this pattern.\n" +
          "the number of subgroups; the overall match (group 0) does not count\n"
  )
  public StarlarkInt groupCount() {
    return StarlarkInt.of(pattern.groupCount());
  }

//    @StarlarkMethod(
//      name = "findall",
//      doc = "Return a list of all non-overlapping matches in the string.\n" +
//          "\n" +
//          "If one or more capturing groups are present in the pattern, return\n" +
//          "a list of groups; this will be a list of tuples if the pattern\n" +
//          "has more than one group.\n" +
//          "\n" +
//          "Empty matches are included in the result.",
//      parameters = {
//        @Param(name = "input", allowedTypes = {@ParamType(type = String.class)})
//      }
//    )
//    public StarlarkList<Object> findall(String input) {
//
//    }

}
