package com.verygood.security.larky.modules.vgs.metrics;

import com.verygood.security.larky.modules.vgs.metrics.constants.Currency;
import com.verygood.security.larky.modules.vgs.metrics.constants.PSP;
import com.verygood.security.larky.modules.vgs.metrics.constants.TransactionResult;
import com.verygood.security.larky.modules.vgs.metrics.constants.TransactionType;
import com.verygood.security.larky.modules.vgs.metrics.spi.LarkyMetrics;
import lombok.extern.slf4j.Slf4j;
import net.starlark.java.eval.EvalException;

@Slf4j
public class DefaultMetrics implements LarkyMetrics {

  public static final String OUTPUT_STRING = """
    amount %s
    bin %s
    currency %s
    psp %s
    result %s
    type %s
    """;

  /**
   * Not used in production
   */
  @Override
  public void track(
    int amount,
    int bin,
    Currency currency,
    PSP psp,
    TransactionResult result,
    TransactionType type
  ) throws EvalException {
    System.out.printf(
      OUTPUT_STRING,
      amount,
      bin,
      currency,
      psp,
      result,
      type
    );
  }
}
