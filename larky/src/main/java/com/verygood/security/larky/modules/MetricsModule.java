package com.verygood.security.larky.modules;

import com.google.common.collect.ImmutableList;
import com.verygood.security.larky.modules.vgs.metrics.NoopMetrics;
import com.verygood.security.larky.modules.vgs.metrics.defaults.DefaultMetrics;
import com.verygood.security.larky.modules.vgs.metrics.spi.LarkyMetrics;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

@Getter
@Slf4j
@StarlarkBuiltin(
  name = "metrics",
  category = "BUILTIN",
  doc = "Overridable Metrics API in Larky")
public class MetricsModule implements LarkyMetrics {

  public static final MetricsModule INSTANCE = new MetricsModule();
  public static final String ENABLE_DEFAULT_PROPERTY = "larky.modules.vgs.metrics.enableDefaultMetrics";

  private final LarkyMetrics metrics;

  public MetricsModule() {
    ServiceLoader<LarkyMetrics> loader = ServiceLoader.load(LarkyMetrics.class);
    List<LarkyMetrics> metricsProviders = ImmutableList.copyOf(loader.iterator());

    if (Boolean.getBoolean(ENABLE_DEFAULT_PROPERTY)) {
      metrics = new DefaultMetrics(); // Not used in production
    } else if (metricsProviders.isEmpty()) {
      metrics = new NoopMetrics(); // Not used in production
    } else {
      if (metricsProviders.size() != 1) {
        throw new IllegalArgumentException(
          String.format(
            "MetricsModule expecting only 1 metrics provider of type LarkyMetrics, found %d",
            metricsProviders.size()));
      }
      metrics = metricsProviders.get(0);
    }
  }

  @StarlarkMethod(
    name = "track",
    doc = "logs only selected dictionary items",
    parameters = {
      @Param(
        name = "dict",
        doc = "dictionary",
        allowedTypes = {
          @ParamType(type = Dict.class),
          @ParamType(type = Map.class)
        }
      )
    }
  )
  @Override
  public void track(Dict<String, String> dict) throws EvalException {
    metrics.track(dict);
  }
}
