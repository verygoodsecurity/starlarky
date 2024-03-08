package com.verygood.security.larky.modules.vgs.metrics.impl;

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
    Object amount,
    Object bin,
    Object currency,
    Object psp,
    Object result,
    Object type,
    Dict<String, String> attributes
  ) throws EvalException {
    throw Starlark.errorf("metrics.track operation must be overridden");
  }
}
