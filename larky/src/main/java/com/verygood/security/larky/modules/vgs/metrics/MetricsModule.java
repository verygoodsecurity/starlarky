package com.verygood.security.larky.modules.vgs.metrics;

import com.google.common.collect.ImmutableList;
import com.verygood.security.larky.modules.vgs.metrics.constants.Currency;
import com.verygood.security.larky.modules.vgs.metrics.constants.PSP;
import com.verygood.security.larky.modules.vgs.metrics.constants.TransactionResult;
import com.verygood.security.larky.modules.vgs.metrics.constants.TransactionType;
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
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ServiceLoader;

import static com.verygood.security.larky.modules.vgs.metrics.constants.TransactionMetricsKeys.*;

@Getter
@Slf4j
@StarlarkBuiltin(
  name = "native_metrics",
  category = "BUILTIN",
  doc = "Overridable Metrics API in Larky")
public class MetricsModule implements StarlarkValue {

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
        doc = "Amount",
        positional = false,
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = String.class),
          @ParamType(type = NoneType.class),
        }
      ),
      @Param(
        name = KEY_BIN,
        named = true,
        positional = false,
        doc = "Bank Identification Number",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = String.class),
          @ParamType(type = NoneType.class),
        }
      ),
      @Param(
        name = KEY_CURRENCY,
        named = true,
        positional = false,
        doc = "Currency",
        allowedTypes = {
          @ParamType(type = String.class),
          @ParamType(type = NoneType.class),
        }
      ),
      @Param(
        name = KEY_PSP,
        named = true,
        positional = false,
        doc = "Payment Service Provider",
        allowedTypes = {
          @ParamType(type = String.class),
          @ParamType(type = NoneType.class),
        }
      ),
      @Param(
        name = KEY_RESULT,
        named = true,
        positional = false,
        doc = "Transaction Result",
        allowedTypes = {
          @ParamType(type = String.class),
          @ParamType(type = NoneType.class),
        }
      ),
      @Param(
        name = KEY_TYPE,
        named = true,
        positional = false,
        doc = "Transaction Type",
        allowedTypes = {
          @ParamType(type = String.class),
          @ParamType(type = NoneType.class),
        }
      ),
      @Param(
        name = "dictionary",
        named = true,
        positional = false,
        doc = "Dictionary",
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
    Dict<String, Object> dictionary
  ) throws EvalException {
    metrics.track(
      transform(amount, KEY_AMOUNT),
      transform(bin, KEY_BIN),
      getValue(Currency.class, currency, Currency.NOT_SPECIFIED, Currency.UNKNOWN),
      getValue(PSP.class, psp, PSP.NOT_SPECIFIED, PSP.UNKNOWN),
      getValue(TransactionResult.class, result, TransactionResult.NOT_SPECIFIED, TransactionResult.UNKNOWN),
      getValue(TransactionType.class, type, TransactionType.NOT_SPECIFIED, TransactionType.UNKNOWN),
      dictionary
    );
  }

  private static <T extends Enum<T>> T getValue(Class<T> enumClass, Object value, T notSpecified, T unknown) {
    if (value instanceof String) {
      try {
        return Enum.valueOf(enumClass, (String) value);
      } catch (Exception e) {
        return unknown;
      }
    }
    return notSpecified;
  }


  private static Integer transform(Object value, String key) throws EvalException {
    Integer valueInt = null;
    if (value instanceof Integer) {
      valueInt = (Integer) value;
    } else if (value instanceof StarlarkInt starlarkInt) {
      valueInt = getInteger(key, starlarkInt.toString());
    } else if (value instanceof String valueString) {
      valueInt = getInteger(key, valueString);
    }
    if (valueInt != null && valueInt < 0) {
      throw Starlark.errorf("negative '%s' not allowed", key);
    }
    if (valueInt != null && key.equals(KEY_BIN)) {
      int binLength = valueInt.toString().length();
      if (binLength < 6 || binLength > 9) {
        throw Starlark.errorf("parameter '%s' length must be in range [%d,%d]", key, 6, 9);
      }
    }
    return valueInt == null ? 0 : valueInt;
  }

  @NotNull
  private static Integer getInteger(String key, String valueString) throws EvalException {
    int value_int;
    if (NumberUtils.isCreatable(valueString)
      && NumberUtils.createNumber(valueString) instanceof Integer) {
      value_int = Integer.parseInt(valueString);
    } else {
      throw Starlark.errorf("parameter '%s' invalid value, want 'int'", key);
    }
    return value_int;
  }

}
