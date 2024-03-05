package com.verygood.security.larky.modules.vgs.metrics.impl;

import com.verygood.security.larky.modules.vgs.metrics.constants.Currency;
import com.verygood.security.larky.modules.vgs.metrics.constants.PSP;
import com.verygood.security.larky.modules.vgs.metrics.constants.TransactionResult;
import com.verygood.security.larky.modules.vgs.metrics.constants.TransactionType;
import com.verygood.security.larky.modules.vgs.metrics.spi.LarkyMetrics;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;

public class NoopMetrics implements LarkyMetrics {

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
    throw Starlark.errorf("metrics.track operation must be overridden");
  }
}
