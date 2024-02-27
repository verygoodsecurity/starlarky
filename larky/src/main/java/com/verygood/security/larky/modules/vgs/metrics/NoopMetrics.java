package com.verygood.security.larky.modules.vgs.metrics;

import com.verygood.security.larky.modules.vgs.metrics.spi.LarkyMetrics;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;

public class NoopMetrics implements LarkyMetrics {

  /**
   * Not used in production
   */
  @Override
  public void track(Dict<String, Object> dict) throws EvalException {
    throw Starlark.errorf("metrics.track operation must be overridden");
  }
}
