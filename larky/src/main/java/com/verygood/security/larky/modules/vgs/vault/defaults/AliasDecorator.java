package com.verygood.security.larky.modules.vgs.vault.defaults;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.starlark.java.eval.EvalException;
import org.apache.commons.lang3.StringUtils;

public class AliasDecorator implements TokenizeFunction {

  @AllArgsConstructor
  @Getter
  public static class TokenizerPatterns {

    private Pattern searchPattern;
    private String replacePattern;
  }

  public static class AliasDecoratorException extends RuntimeException {

    public AliasDecoratorException(String message) {
      super(message);
    }
  }

  private final List<TokenizerPatterns> tokenizerPatternsList;
  private final TokenizeFunction tokenizeFunction;

  public AliasDecorator(List<TokenizerPatterns> tokenizerPatternsList, TokenizeFunction tokenizeFunction) {
    this.tokenizerPatternsList = tokenizerPatternsList;
    this.tokenizeFunction = tokenizeFunction;
  }

  @Override
  public String tokenize(String toTokenize) throws EvalException {

    for (TokenizerPatterns tokenizerPatterns : tokenizerPatternsList) {
      final Matcher matcher = tokenizerPatterns.getSearchPattern().matcher(toTokenize);

      if (matcher.find()) {
        return tokenize(matcher, tokenizerPatterns.getReplacePattern());
      }
    }
    throw new AliasDecoratorException("None of the regex could not be matched against the secret");
  }

  private String tokenize(Matcher matcher, String replacePattern) throws EvalException {

    if (!(StringUtils.countMatches(replacePattern, "${token}") == 1)) {
      throw new AliasDecoratorException("Token pattern must contain only one token placeholder");
    }

    final String tokenGroup;
    try {
      tokenGroup = matcher.group("token");
    } catch (IllegalArgumentException e) {
      throw new AliasDecoratorException("Secret pattern must contain token placeholder");
    }

    String tokenized = tokenizeFunction.tokenize(tokenGroup);
    String preFormatted = replacePattern.replace("${token}", "%s");
    return matcher.replaceFirst(String.format(preFormatted, tokenized));
  }
}
