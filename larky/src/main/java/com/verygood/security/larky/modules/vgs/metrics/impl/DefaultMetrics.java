package com.verygood.security.larky.modules.vgs.metrics.impl;

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
    Object amount,
    Object bin,
    Object currency,
    Object psp,
    Object result,
    Object type,
    Dict<String, Object> attributes
  ) throws EvalException {
    System.out.printf(
      OUTPUT_STRING,
      amount,
      bin,
      currency,
      psp,
      result,
      type,
      attributes.toString()
    );
  }
}
