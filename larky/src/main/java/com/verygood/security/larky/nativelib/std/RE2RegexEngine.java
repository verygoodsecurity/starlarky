package com.verygood.security.larky.nativelib.std;

import com.google.common.base.Joiner;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;

import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkValue;

import java.util.ArrayList;
import java.util.Arrays;


@StarlarkBuiltin(
    name = "re2j",
    category = "BUILTIN",
    doc = "This module provides access to the linear regular expression matching engine.\n" +
        "\n" +
        "This package provides an implementation of regular expression matching based on Russ Cox's linear-time RE2 algorithm.\n" +
        "\n" +
        "The API presented by com.google.re2j mimics that of java.util.regex.Matcher and java.util.regex.Pattern. While not identical, they are similar enough that most users can switch implementations simply by changing their imports.\n" +
        "\n" +
        "The syntax of the regular expressions accepted is the same general syntax used by Perl, Python, and other languages. More precisely, it is the syntax accepted by the C++ and Go implementations of RE2 described at https://github.com/google/re2/wiki/Syntax, except for \\C (match any byte), which is not supported because in this implementation, the matcher's input is conceptually a stream of Unicode code points, not bytes.\n" +
        "\n" +
        "The current API is rather small and intended for compatibility with java.util.regex, but the underlying implementation supports some additional features, such as the ability to process input character streams encoded as UTF-8 byte arrays. These may be exposed in a future release if there is sufficient interest." +
        "\n" +
        "More on syntax here: https://github.com/google/re2/wiki/Syntax")
public class RE2RegexEngine implements StarlarkValue {

  public static final RE2RegexEngine INSTANCE = new RE2RegexEngine();

  private static final LarkyRegexPattern _Pattern = new LarkyRegexPattern();

  @StarlarkMethod(name = "Pattern", doc = "pattern", structField = true)
  public static LarkyRegexPattern Pattern() { return _Pattern; }

  // java <> larky objects
  public static class  LarkyRegexPattern implements StarlarkValue {

    @StarlarkMethod(name = "CASE_INSENSITIVE", doc = "Flag: case insensitive matching.", structField = true)
    public StarlarkInt CASE_INSENSITIVE() { return StarlarkInt.of(Pattern.CASE_INSENSITIVE); }

    @StarlarkMethod(name = "DISABLE_UNICODE_GROUPS", doc = "Flag: Unicode groups (e.g. \\p\\ Greek\\ ) will be syntax errors", structField = true)
    public StarlarkInt DISABLE_UNICODE_GROUPS() { return StarlarkInt.of(Pattern.DISABLE_UNICODE_GROUPS); }

    @StarlarkMethod(name = "DOTALL", doc = "Flag: dot (.) matches all characters, including newline.", structField = true)
    public StarlarkInt DOTALL() { return StarlarkInt.of(Pattern.DOTALL); }

    @StarlarkMethod(name = "LONGEST_MATCH", doc = "Flag: matches longest possible string.", structField = true)
    public StarlarkInt LONGEST_MATCH() { return StarlarkInt.of(Pattern.LONGEST_MATCH); }

    @StarlarkMethod(name = "MULTILINE", doc = "Flag: multiline matching: ^ and $ match at beginning and end of line, not just beginning and end of input.", structField = true)
    public StarlarkInt MULTILINE() { return StarlarkInt.of(Pattern.MULTILINE); }

    private Pattern pattern;

    protected LarkyRegexPattern pattern(Pattern pattern) {
      this.pattern = pattern;
      return this;
    }

    @StarlarkMethod(
      name = "compile",
      doc = "Creates and returns a new Pattern corresponding to compiling regex with the given flags." +
          "If flags is not passed, it defaults to 0",
      parameters = {
          @Param(name = "regex"),
          @Param(
              name = "flags",
              allowedTypes =  {
                  @ParamType(type = StarlarkInt.class),
              },
              defaultValue = "0")
    })
    public static LarkyRegexPattern compile(String regex, StarlarkInt flags) {
      int flag = flags.toIntUnchecked();
      return new LarkyRegexPattern().pattern(Pattern.compile(regex, flag));
    }

    @StarlarkMethod(
      name = "matches",
      doc = "Matches a string against a regular expression.",
      parameters = {
          @Param(name = "regex"),
          @Param(
              name = "input",
              allowedTypes =  {
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
              allowedTypes =  {
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

    @StarlarkMethod(name="pattern", doc="")
    public String pattern() {
      return pattern.pattern();
    }

    @StarlarkMethod(
      name = "matcher",
      doc = "Creates a new Matcher matching the pattern against the input.\n",
      parameters = {
          @Param(
            name = "input",
            allowedTypes =  {
                @ParamType(type = String.class),
          })
    })
    public LarkyRegexMatcher matcher(String input) {
      return new LarkyRegexMatcher(pattern.matcher(input), this);
    }

    @StarlarkMethod(
       name = "split",
       doc = "",
       parameters = {
           @Param(
             name = "input",
             allowedTypes =  {
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

      while(m.find()) {
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
        if(has_capture) {
          // Check if there's capture groups and add them
          for(int i = 0; i < m.groupCount(); ++i) {
            match = m.group(i+1);
            matchList.add(match == null ? Starlark.NONE : match);
          }
        }
      }

      // If no match was found, return this
      if (index == 0) {
        return new String[] {input.toString()};
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

  public static class LarkyRegexMatcher implements StarlarkValue {
    private final Matcher matcher;
    private final LarkyRegexPattern pattern;

    LarkyRegexMatcher(Matcher matcher) {
      this.matcher = matcher;
      this.pattern = new LarkyRegexPattern().pattern(matcher.pattern());
    }

    LarkyRegexMatcher(Matcher matcher, LarkyRegexPattern pattern) {
      this.matcher = matcher;
      this.pattern = pattern;
    }

    @StarlarkMethod(
      name = "pattern",
      doc = "Returns the LarkyRegexPattern associated with this LarkyRegexMatcher.\n"
    )
    public LarkyRegexPattern pattern() {
      return pattern;
    }

    @StarlarkMethod(
      name = "reset",
      doc = "Resets the LarkyRegexMatcher, rewinding input and discarding any match information.\n",
      parameters = {
        @Param(
            name = "input",
            allowedTypes = {
                @ParamType(type = String.class),
                @ParamType(type = NoneType.class)
            },
            defaultValue = "None"
        )
      }
    )
    public LarkyRegexMatcher reset(Object input) {
      if(NoneType.class.isAssignableFrom(input.getClass())) {
        matcher.reset();
      }
      else if(String.class.isAssignableFrom(input.getClass())) {
        matcher.reset(String.valueOf(input));
      }
      return this;
    }

    @StarlarkMethod(
      name = "start",
      doc = "Returns the start position of the most recent match." +
          "\n" +
          "Accepts a group index position, or defaults to 0 if it's the overall match.",
      parameters = {
        @Param(
          name = "index",
          allowedTypes = {
              @ParamType(type = StarlarkInt.class),
          },
          defaultValue = "0"
        )
      }
    )
    public StarlarkInt start(StarlarkInt index) {
      return StarlarkInt.of(matcher.start(index.toIntUnchecked()));
    }
    @StarlarkMethod(
      name = "end",
      doc = "Returns the end position of the most recent match." +
           "\n" +
           "Accepts a group index position, or defaults to 0 if it's the overall match.",
      parameters = {
         @Param(
           name = "index",
           allowedTypes = {
               @ParamType(type = StarlarkInt.class),
           },
           defaultValue = "0"
         )
       }
    )
    public StarlarkInt end(StarlarkInt index) {
      return StarlarkInt.of(matcher.end(index.toIntUnchecked()));
    }

    @StarlarkMethod(
      name = "group",
      doc = "Returns the most recent match." +
          "\n" +
          "If no argument or None is passed in, returns the most recent match, or " +
          "null if the group was not matched." +
          "\n" +
          "If a valid integer is returned, returns the subgroup of the most recent match." +
          "\n" +
          "Throws an exception if group < 0 or group > group_count()",
      parameters = {
          @Param(
            name = "group",
            allowedTypes =  {
                @ParamType(type = StarlarkInt.class),
                @ParamType(type = String.class),
                @ParamType(type = NoneType.class),
          },
          defaultValue = "None")
    })
    public Object group(Object group) {
      String g;
      if(Starlark.isNullOrNone(group)) {
        g = matcher.group();
      }
      else if(StarlarkInt.class.isAssignableFrom(group.getClass())) {
        g = matcher.group(((StarlarkInt)group).toIntUnchecked());
      }
      // default case
      else {
        g = matcher.group(String.valueOf(group));
      }

      if(g == null) {
        return Starlark.NONE;
      }
      return g;

    }
    @StarlarkMethod(
      name = "group_count",
      doc = "Returns the number of subgroups in this pattern.\n" +
          "the number of subgroups; the overall match (group 0) does not count\n"
    )
    public StarlarkInt groupCount() {
      return StarlarkInt.of(matcher.groupCount());
    }

    @StarlarkMethod(
      name = "matches",
      doc = "Matches the entire input against the pattern (anchored start and end). " +
          "If there is a match, matches sets the match state to describe it.\n" +
          "the number of subgroups; the overall match (group 0) does not count\n" +
          "\n" +
          "Returns: true if the entire input matches the pattern"
    )
    public boolean matches() {
      return matcher.matches();
    }

    @StarlarkMethod(
      name = "looking_at",
      doc = "Matches the beginning of input against the pattern (anchored start). " +
          "If there is a match, looking_at sets the match state to describe it." +
          "\n" +
          "Returns true if the beginning of the input matches the pattern\n"
    )
    public boolean lookingAt() {
      return matcher.lookingAt();
    }

    @StarlarkMethod(
      name = "find",
      doc = "Matches the input against the pattern (unanchored), starting at a specified position." +
          " If there is a match, find sets the match state to describe it." +
          "\n" +
          "start - the input position where the search begins\n" +
          "\n" +
          "Returns true if it finds a match or throw if start is not a valid input position\n",
      parameters = {
        @Param(
            name = "start",
            allowedTypes = {
                @ParamType(type = StarlarkInt.class),
                @ParamType(type = NoneType.class),
            },
            defaultValue = "None"
        )
      }
    )
    public boolean find(Object start) {
      if(Starlark.isNullOrNone(start)) {
        return matcher.find();
      }
      StarlarkInt s = (StarlarkInt) StarlarkUtil.valueToStarlark(start);
      return matcher.find(s.toIntUnchecked());
    }

    @StarlarkMethod(
      name="quote_replacement",
      doc = "Quotes '\\' and '$' in s, so that the returned string could be used in " +
          "append_replacement(appendable_string, s) as a literal replacement of s.\n" +
          "\n" +
          "Returns: the quoted string",
      parameters = {
        @Param(
            name = "s",
            allowedTypes = {
                @ParamType(type = String.class),
            }
        )
      }
    )
    public static String quoteReplacement(String s) {
      return Matcher.quoteReplacement(s);
    }

    @StarlarkMethod(
      name="append_replacement",
      doc = "Appends to sb two strings: the text from the append position up to the " +
          "beginning of the most recent match, and then the replacement with submatch groups" +
          " substituted for references of the form $n, where n is the group number in decimal" +
          ". It advances the append position to where the most recent match ended." +
          "\n" +
          "To embed a literal $, use \\$ (actually \"\\\\$\" with string escapes). The " +
          "escape is only necessary when $ is followed by a digit, but it is always allowed. " +
          "Only $ and \\ need escaping, but any character can be escaped." +
          "\n" +
          "\n" +
          "The group number n in $n is always at least one digit and expands to use more " +
          "digits as long as the resulting number is a valid group number for this pattern. " +
          "To cut it off earlier, escape the first digit that should not be used." +
          "\n" +
          "Returns: the Matcher itself, for chained method calls\n",
      parameters = {
        @Param(
          name = "sb",
          allowedTypes = {
              @ParamType(type = StarlarkList.class),
          }
        ),
        @Param(
          name = "replacement",
          allowedTypes = {
              @ParamType(type = String.class),
          }
        )}
    )
    public LarkyRegexMatcher appendReplacement(StarlarkList<String> sb, String replacement) {
      StringBuilder builder = new StringBuilder().append(Joiner.on("").join(sb));
      matcher.appendReplacement(builder, replacement);
      try {
        sb.clearElements();
        sb.addElements(Arrays.asList(builder.toString().split("")));
      } catch (EvalException e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    @StarlarkMethod(
      name="append_tail",
      doc = "Appends to sb the substring of the input from the append position to the " +
          "end of the input." +
          "\n" +
          "Returns the argument sb, for method chaining\n",
      parameters = {
        @Param(
          name = "s",
          allowedTypes = {
              @ParamType(type = String.class),
          }
        )}
    )
    public String appendTail(String s) {
      return matcher.appendTail(new StringBuilder().append(s)).toString();
    }

    @StarlarkMethod(
      name="replace_all",
      doc = "Returns the input with all matches replaced by replacement, interpreted as for" +
         " append_replacement." +
         "\n" +
         "The input string with the matches replaced\n",
      parameters = {
       @Param(
         name = "replacement",
         allowedTypes = {
             @ParamType(type = String.class),
         }
       )}
     )
    public String replaceAll(String replacement) {
      return matcher.replaceAll(replacement);
    }

    @StarlarkMethod(
      name="replace_first",
      doc = "Returns the input with the first match replaced by replacement, " +
          "interpreted as for append_replacement.\n" +
          "\n" +
          "The input string with the first matches replaced\n",
      parameters = {
         @Param(
           name = "replacement",
           allowedTypes = {
               @ParamType(type = String.class),
           }
         )}
     )
    public String replaceFirst(String replacement) {
      return matcher.replaceFirst(replacement);
    }

  }
}
