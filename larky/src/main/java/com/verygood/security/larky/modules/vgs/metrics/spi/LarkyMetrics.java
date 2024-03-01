package com.verygood.security.larky.modules.vgs.metrics.spi;

import com.verygood.security.larky.modules.vgs.metrics.constants.Currency;
import com.verygood.security.larky.modules.vgs.metrics.constants.PSP;
import com.verygood.security.larky.modules.vgs.metrics.constants.TransactionResult;
import com.verygood.security.larky.modules.vgs.metrics.constants.TransactionType;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkValue;

public interface LarkyMetrics extends StarlarkValue {

  void track(
    int amount,
    int bin,
    Currency currency,
    PSP psp,
    TransactionResult result,
    TransactionType type
  ) throws EvalException;

}
