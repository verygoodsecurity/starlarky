package com.verygood.security.larky.modules.vgs.metrics;

import com.google.common.collect.ImmutableList;
import com.verygood.security.larky.modules.vgs.metrics.impl.NoopMetrics;
import com.verygood.security.larky.modules.vgs.metrics.spi.LarkyMetrics;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.ServiceLoader;

import static com.verygood.security.larky.modules.vgs.metrics.constants.TransactionMetricsKeys.*;

@Getter
@Slf4j
@StarlarkBuiltin(
  name = "native_metrics",
  category = "BUILTIN",
  doc = "Overridable Metrics API in Larky")
public class MetricsModule implements LarkyMetrics {

  public static final MetricsModule INSTANCE = new MetricsModule();
  private final LarkyMetrics metrics;

  public MetricsModule() {
    ServiceLoader<LarkyMetrics> loader = ServiceLoader.load(LarkyMetrics.class);
    List<LarkyMetrics> metricsProviders = ImmutableList.copyOf(loader.iterator());

    if (metricsProviders.isEmpty()) {
      log.error("Using NoopMetrics, should not be used in Production");
      metrics = new NoopMetrics();
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
        name = KEY_AMOUNT,
        named = true,
        positional = false,
        doc = "Amount"
      ),
      @Param(
        name = KEY_BIN,
        named = true,
        positional = false,
        doc = "Bank Identification Number"
      ),
      @Param(
        name = KEY_CURRENCY,
        named = true,
        positional = false,
        doc = "Currency"
      ),
      @Param(
        name = KEY_PSP,
        named = true,
        positional = false,
        doc = "Payment Service Provider"
      ),
      @Param(
        name = KEY_RESULT,
        named = true,
        positional = false,
        doc = "Transaction Result"
      ),
      @Param(
        name = KEY_TYPE,
        named = true,
        positional = false,
        doc = "Transaction Type"
      ),
      @Param(
        name = "attributes",
        named = true,
        positional = false,
        doc = "kwargs",
        defaultValue = "{}",
        allowedTypes = {
          @ParamType(type = Dict.class)
        }
      ),
    }
  )
  public void track(
    Object amount,
    Object bin,
    Object currency,
    Object psp,
    Object result,
    Object type,
    Dict<String, Object> attributes
  ) throws EvalException {
    metrics.track(
      getNullIfNoneOrBlank(amount),
      getNullIfNoneOrBlank(bin),
      getNullIfNoneOrBlank(currency),
      getNullIfNoneOrBlank(psp),
      getNullIfNoneOrBlank(result),
      getNullIfNoneOrBlank(type),
      attributes
    );
  }

  private static Object getNullIfNoneOrBlank(Object value) {
    if (value instanceof NoneType
      || (value instanceof String valStr && StringUtils.isBlank(valStr))) {
      return null;
    }
    return value.toString();
  }
}
