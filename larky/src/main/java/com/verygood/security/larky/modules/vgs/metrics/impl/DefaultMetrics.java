package com.verygood.security.larky.modules.vgs.metrics.impl;

import com.verygood.security.larky.modules.vgs.metrics.constants.Currency;
import com.verygood.security.larky.modules.vgs.metrics.constants.PSP;
import com.verygood.security.larky.modules.vgs.metrics.constants.TransactionResult;
import com.verygood.security.larky.modules.vgs.metrics.constants.TransactionType;
import com.verygood.security.larky.modules.vgs.metrics.spi.LarkyMetrics;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;

public class DefaultMetrics implements LarkyMetrics {

  public static final String OUTPUT_STRING = """
    amount %s
    bin %s
    currency %s
    psp %s
    result %s
    type %s
    dict %s
    _________________
    """;

  /**
   * Not used in production
   */
  @Override
  public void track(
    Integer amount,
    Integer bin,
    Currency currency,
    PSP psp,
    TransactionResult result,
    TransactionType type,
    Dict<String, Object> dictionary
  ) throws EvalException {
    System.out.printf(
      OUTPUT_STRING,
      amount,
      bin,
      currency,
      psp,
      result,
      type,
      dictionary.toString()
    );
  }
}
