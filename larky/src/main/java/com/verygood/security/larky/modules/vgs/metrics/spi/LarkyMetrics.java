package com.verygood.security.larky.modules.vgs.metrics.spi;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkValue;

public interface LarkyMetrics extends StarlarkValue {

  void track(Dict<String, String> dict) throws EvalException;

}
