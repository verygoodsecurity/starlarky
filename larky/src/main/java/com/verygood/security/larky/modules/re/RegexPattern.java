package com.verygood.security.larky.modules.re;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkValue;

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
  public void str(Printer printer) {
    printer.append(pattern.toString());
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
  public static RegexPattern compile(String regex, StarlarkInt flags) {
    int flag = flags.toIntUnchecked();
    return new RegexPattern().pattern(Pattern.compile(regex, flag));
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
              })
      })
  public RegexMatcher matcher(String input) {
    return new RegexMatcher(pattern.matcher(input), this);
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
