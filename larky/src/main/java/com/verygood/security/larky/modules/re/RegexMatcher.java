package com.verygood.security.larky.modules.re;

import com.google.common.base.Joiner;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

public class RegexMatcher implements StarlarkValue {
  // This is only non-final because match() function has to modify the pattern region matcher
  private Matcher matcher;

  /**
   * The Pattern object that created this Matcher.
   */
  private RegexPattern parentPattern;
  private final CharSequence input;

  private int lastMatchStart;
  private int lastMatchEnd;

  RegexMatcher(RegexPattern parentPattern, Matcher matcher, CharSequence input) {
    this.matcher = matcher;
    this.parentPattern = parentPattern;
    this.input = input;
  }

  public RegexMatcher usePattern(Pattern newPattern) {
    // Per JVM documentation, current search position & last append position
    // do not change. region behavior is not mentioned, but JVM preserves
    // the region.
    //
    // Info on groups from lastmatch is lost.
    if (newPattern == null) {
      throw new IllegalArgumentException("Pattern cannot be null");
    }
    this.parentPattern = this.parentPattern.pattern(newPattern);
    this.matcher = newPattern.matcher(input); // ???
    return this;
  }

  @StarlarkMethod(
      name = "pattern",
      doc = "Returns the parent RegexPattern associated with this RegexMatcher.\n"
  )
  public RegexPattern parentPattern() {
    return parentPattern;
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
    lastMatchStart = 0;
    lastMatchEnd = 0;
    return this;
  }

  @StarlarkMethod(
      name = "groupdict",
      doc = "Return a dictionary containing all the named subgroups of the match, keyed by " +
              "the subgroup name. The default argument is used for groups that did not " +
              "participate in the match; it defaults to None.",
      parameters = {
          @Param(
              name = "default",
              allowedTypes = {
                @ParamType(type = NoneType.class),
                @ParamType(type = String.class),
              },
              defaultValue = "None"
          )
      }, useStarlarkThread = true
  )
  public Dict<String, Object> groupdict(Object defaulto, StarlarkThread thread) {
    Dict.Builder<String, Object> d = new Dict.Builder<>();
    Stream<Map.Entry<String, Integer>> sorted = ( // must be sorted to match python behavior
      parentPattern
      .namedGroups()
      .entrySet()
      .stream()
      .sorted(Map.Entry.comparingByValue()));
    sorted.forEach(entry -> d.put(entry.getKey(), group(entry.getValue())));
    return d.build(thread.mutability());
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
    int idx = index.toIntUnchecked();
    // for region
    if(idx == 0 && lastMatchStart != 0) {
      return StarlarkInt.of(lastMatchStart);
    }
    return StarlarkInt.of(matcher.start(idx));
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
    int idx = index.toIntUnchecked();
    // for region
    if(idx == 0 && lastMatchEnd != 0) {
      return StarlarkInt.of(lastMatchEnd);
    }
    return StarlarkInt.of(matcher.end(idx));
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
    } else if (Integer.class.isAssignableFrom(group.getClass())) {
      g = matcher.group(((Integer) group));
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
        "Returns: true if the entire input matches the pattern",
    parameters = {
      @Param(
        name = "pos",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
        },
        defaultValue = "0"
      ),
      @Param(
        name = "endpos",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
        },
        defaultValue = "-1"
      )
    }
  )
  public boolean matches(StarlarkInt pos, StarlarkInt endpos) {
    return matcher.matches();
  }

  @StarlarkMethod(
    name = "looking_at",
    doc = "Matches the beginning of input against the pattern (anchored start). " +
        "If there is a match, looking_at sets the match state to describe it." +
        "\n" +
        "Returns true if the beginning of the input matches the pattern\n",
    parameters = {
      @Param(
        name = "pos",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
        },
        defaultValue = "0"
      ),
      @Param(
        name = "endpos",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
        },
        defaultValue = "-1"
      )
    }
  )
  public boolean lookingAt(StarlarkInt spos, StarlarkInt sendpos) {
    int pos = spos.toIntUnchecked();
    int endpos = sendpos.toIntUnchecked();
    pos = Math.max(pos, 0);
    endpos = (endpos == -1) ? input.length() : endpos;
    //LarkyRE2Matcher.genMatch(matcher, input,pos, endpos);
    if(pos != 0 && !parentPattern.pattern().startsWith("^")) {
      /*

        var x = m.pattern();
        var matcher = x.matcher(s.subSequence(startByte,endByte));
        if(matcher.lookingAt()) {
          System.out.println(Arrays.toString(matcher.groups));
          System.out.println(matcher.group());
        }
        */

      // match is like search but pattern must start with ^
      // if pos is passed in, we have to reset the pattern.
      matcher = Pattern.compile("^" + parentPattern.pattern())
               .matcher(input.subSequence(pos, endpos));
    }
    boolean ok = matcher.lookingAt();
    if(ok) {
      lastMatchStart = matcher.start() + pos;
      lastMatchEnd = matcher.end() + pos;
    }
    return ok;
  }

  @StarlarkMethod(
        name = "search",
        doc = "Scan through string looking for the first location where this regular expression" +
                " produces a match, and return a corresponding match object. Return None if no" +
                " position in the string matches the pattern; note that this is different from" +
                " finding a zero-length match at some point in the string.\n" +
                "\n" +
                "The optional second parameter pos gives an index in the string where the " +
                "search is to start; it defaults to 0. This is not completely equivalent to" +
                " slicing the string; the '^' pattern character matches at the real beginning" +
                " of the string and at positions just after a newline, but not necessarily at" +
                " the index where the search is to start.\n" +
                "\n" +
                "The optional parameter endpos limits how far the string will be searched; it " +
                "will be as if the string is endpos characters long, so only the characters " +
                "from pos to endpos - 1 will be searched for a match. If endpos is less than" +
                " pos, no match will be found; otherwise, if rx is a compiled regular expression" +
                " object, rx.search(string, 0, 50) is equivalent to rx.search(string[:50], 0).",
        parameters = {
            @Param(
                name = "start",
                allowedTypes = {
                    @ParamType(type = StarlarkInt.class),
                },
                defaultValue = "0"
            ),
            @Param(
              name = "endpos",
              allowedTypes = {
                @ParamType(type = StarlarkInt.class),
              },
              defaultValue = "-1"
            )
        }
    )
    public boolean search(StarlarkInt s, StarlarkInt e) {
      int pos = Math.max(s.toIntUnchecked(), 0);
      int endpos = (e.toIntUnchecked() == -1) ? input.length() : e.toIntUnchecked();
      if(pos != 0 && !parentPattern.pattern().startsWith("^")) {
        matcher = Pattern.compile(parentPattern.pattern())
                 .matcher(input.subSequence(pos, endpos));
      }
      boolean ok = matcher.find();
      if(ok) {
        lastMatchStart = matcher.start() + pos;
        lastMatchEnd = matcher.end() + pos;
      }
      return ok;
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
    StringBuffer builder = new StringBuffer().append(Joiner.on("").join(sb));
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
    return matcher.appendTail(new StringBuffer().append(s)).toString();
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
