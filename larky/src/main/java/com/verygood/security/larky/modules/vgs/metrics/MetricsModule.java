package com.verygood.security.larky.modules.vgs.metrics;

import com.google.common.collect.ImmutableList;
import com.verygood.security.larky.modules.vgs.metrics.constants.Currency;
import com.verygood.security.larky.modules.vgs.metrics.constants.PSP;
import com.verygood.security.larky.modules.vgs.metrics.constants.TransactionResult;
import com.verygood.security.larky.modules.vgs.metrics.constants.TransactionType;
import com.verygood.security.larky.modules.vgs.metrics.spi.LarkyMetrics;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;

import java.util.List;
import java.util.ServiceLoader;

import static com.verygood.security.larky.modules.vgs.metrics.constants.Currency.NOT_SPECIFIED;

@Getter
@Slf4j
@StarlarkBuiltin(
  name = "metrics",
  category = "BUILTIN",
  doc = "Overridable Metrics API in Larky")
public class MetricsModule implements LarkyMetrics {

  public static final MetricsModule INSTANCE = new MetricsModule();

  private final LarkyMetrics metrics;

  public MetricsModule() {
    ServiceLoader<LarkyMetrics> loader = ServiceLoader.load(LarkyMetrics.class);
    List<LarkyMetrics> metricsProviders = ImmutableList.copyOf(loader.iterator());

    if (metricsProviders.isEmpty()) {
      metrics = new NoopMetrics(); // Not used in production
    } else if (metricsProviders.size() == 1) {
      metrics = metricsProviders.get(0);
    } else {
      throw new IllegalArgumentException(
        String.format(
          "MetricsModule expecting only 1 metrics provider of type LarkyMetrics, found %d",
          metricsProviders.size()));

    }
  }

  @StarlarkMethod(
    name = "track",
    doc = "Logs the amount, bin, currency, psp, result and type",
    parameters = {
      @Param(
        name = "amount",
        named = true,
        doc = "Amount",
        allowedTypes = {
          @ParamType(type = Integer.class)
        }
      ),
      @Param(
        name = "bin",
        named = true,
        doc = "Bank Identification Number",
        allowedTypes = {
          @ParamType(type = Integer.class)
        }
      ),
      @Param(
        name = "currency",
        named = true,
        doc = "Currency",
        defaultValue = "NOT_SPECIFIED",
        allowedTypes = {
          @ParamType(type = Currency.class)
        }
      ),
      @Param(
        name = "psp",
        named = true,
        doc = "Payment Service Provider",
        defaultValue = "NOT_SPECIFIED",
        allowedTypes = {
          @ParamType(type = PSP.class)
        }
      ),
      @Param(
        name = "result",
        named = true,
        doc = "Transaction Result",
        defaultValue = "NOT_SPECIFIED",
        allowedTypes = {
          @ParamType(type = TransactionResult.class)
        }
      ),
      @Param(
        name = "type",
        named = true,
        doc = "Transaction Type",
        defaultValue = "NOT_SPECIFIED",
        allowedTypes = {
          @ParamType(type = TransactionType.class)
        }
      )
    }
  )
  @Override
  public void track(
    int amount,
    int bin,
    Currency currency,
    PSP psp,
    TransactionResult result,
    TransactionType type
  ) throws EvalException {
    metrics.track(amount, bin, currency, psp, result, type);
  }
}
