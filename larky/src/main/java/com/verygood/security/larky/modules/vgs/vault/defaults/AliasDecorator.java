package com.verygood.security.larky.modules.vgs.vault.defaults;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.starlark.java.eval.EvalException;

public class AliasDecorator implements TokenizeFunction {

  private final TokenizeFunction tokenizeFunction;
  private final Pattern searchPattern;
  private final String replacePattern;


  public AliasDecorator(
      TokenizeFunction tokenizeFunction,
      String searchPattern,
      String replacePattern) {
    this.tokenizeFunction = tokenizeFunction;
    this.searchPattern = Pattern.compile(searchPattern);
    this.replacePattern = replacePattern;
  }

  @Override
  public String tokenize(String toTokenize) throws EvalException {

    final Matcher matcher = searchPattern.matcher(toTokenize);

    if (!matcher.find()) {
      // Fallback to generic
      return new UUIDAliasGenerator().generate(toTokenize);

    }
    return tokenize(matcher, replacePattern);
  }

  private String tokenize(Matcher matcher, String replacePattern) throws EvalException {
    final String tokenGroup = matcher.group("token");
    String tokenized = tokenizeFunction.tokenize(tokenGroup);
    String preFormatted = replacePattern.replace("${token}", "%s");
    return matcher.replaceFirst(String.format(preFormatted, tokenized));
  }
}
