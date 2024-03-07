package com.verygood.security.larky.modules.vgs.metrics.spi;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkValue;


public interface LarkyMetrics extends StarlarkValue {

  void track(
    Object amount,
    Object bin,
    Object currency,
    Object psp,
    Object result,
    Object type,
    Dict<String, Object> attributes
  ) throws EvalException;

}
