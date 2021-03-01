package com.verygood.security.larky.modules;

import com.verygood.security.larky.modules.re.RegexPattern;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;


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
public class RegexModule implements StarlarkValue {

  public static final RegexModule INSTANCE = new RegexModule();

  private static final RegexPattern _Pattern = new RegexPattern();

  @StarlarkMethod(name = "Pattern", doc = "pattern", structField = true)
  public static RegexPattern Pattern() { return _Pattern; }

}
