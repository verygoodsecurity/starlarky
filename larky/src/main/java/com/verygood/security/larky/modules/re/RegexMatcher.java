package com.verygood.security.larky.modules.re;

import com.google.common.base.Joiner;
import com.google.re2j.Matcher;

import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkValue;

import java.util.Arrays;

public class RegexMatcher implements StarlarkValue {
  private final Matcher matcher;
  private final RegexPattern pattern;

  RegexMatcher(Matcher matcher) {
    this.matcher = matcher;
    this.pattern = new RegexPattern().pattern(matcher.pattern());
  }

  RegexMatcher(Matcher matcher, RegexPattern pattern) {
    this.matcher = matcher;
    this.pattern = pattern;
  }

  @StarlarkMethod(
      name = "pattern",
      doc = "Returns the RegexPattern associated with this RegexMatcher.\n"
  )
  public RegexPattern pattern() {
    return pattern;
  }

  @StarlarkMethod(
      name = "reset",
      doc = "Resets the RegexMatcher, rewinding input and discarding any match information.\n",
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
  public RegexMatcher reset(Object input) {
    if (NoneType.class.isAssignableFrom(input.getClass())) {
      matcher.reset();
    } else if (String.class.isAssignableFrom(input.getClass())) {
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
              allowedTypes = {
                  @ParamType(type = StarlarkInt.class),
                  @ParamType(type = String.class),
                  @ParamType(type = NoneType.class),
              },
              defaultValue = "None")
      })
  public Object group(Object group) {
    String g;
    if (Starlark.isNullOrNone(group)) {
      g = matcher.group();
    } else if (StarlarkInt.class.isAssignableFrom(group.getClass())) {
      g = matcher.group(((StarlarkInt) group).toIntUnchecked());
    }
    // default case
    else {
      g = matcher.group(String.valueOf(group));
    }

    if (g == null) {
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
    if (Starlark.isNullOrNone(start)) {
      return matcher.find();
    }
    StarlarkInt s = (StarlarkInt) StarlarkUtil.valueToStarlark(start);
    return matcher.find(s.toIntUnchecked());
  }

  @StarlarkMethod(
      name = "quote_replacement",
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
      name = "append_replacement",
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
  public RegexMatcher appendReplacement(StarlarkList<String> sb, String replacement) {
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
      name = "append_tail",
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
      name = "replace_all",
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
      name = "replace_first",
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
