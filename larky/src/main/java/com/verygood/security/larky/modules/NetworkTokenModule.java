package com.verygood.security.larky.modules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.verygood.security.larky.modules.vgs.nts.LarkyNetworkToken;
import com.verygood.security.larky.modules.vgs.nts.MockNetworkTokenService;
import com.verygood.security.larky.modules.vgs.nts.NoopNetworkTokenService;
import com.verygood.security.larky.modules.vgs.nts.spi.NetworkTokenService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;

@StarlarkBuiltin(name = "nts", category = "BUILTIN", doc = "Overridable Network Token API in Larky")
public class NetworkTokenModule implements LarkyNetworkToken {
  public static final NetworkTokenModule INSTANCE = new NetworkTokenModule();

  public static final String ENABLE_MOCK_PROPERTY = "larky.modules.vgs.nts.enableMockNetworkToken";

  private final NetworkTokenService networkTokenService;

  public NetworkTokenModule() {
    ServiceLoader<NetworkTokenService> loader = ServiceLoader.load(NetworkTokenService.class);
    List<NetworkTokenService> networkTokenProviders = ImmutableList.copyOf(loader.iterator());

    if (Boolean.getBoolean(ENABLE_MOCK_PROPERTY)) {
      networkTokenService = new MockNetworkTokenService();
    } else if (networkTokenProviders.isEmpty()) {
      networkTokenService = new NoopNetworkTokenService();
    } else {
      if (networkTokenProviders.size() != 1) {
        throw new IllegalArgumentException(
            String.format(
                "NetworkTokenModule expecting only 1 network token provider of type NetworkTokenService, found %d",
                networkTokenProviders.size()));
      }
      networkTokenService = networkTokenProviders.get(0);
    }
  }

  @StarlarkMethod(
      name = "render",
      doc =
          "Get network token and the cryptogram for given pan alias and inject the network token values into the original input and return",
      useStarlarkThread = true,
      parameters = {
        @Param(
            name = "input",
            doc = "Input json payload",
            named = true,
            allowedTypes = {@ParamType(type = Object.class)}),
        @Param(
            name = "pan",
            named = true,
            doc =
                "JSONPath to the PAN alias in the input payload for looking up the corresponding network token to be used and replace the original value at given JSONPath",
            allowedTypes = {@ParamType(type = String.class)}),
        @Param(
            name = "exp_month",
            named = true,
            defaultValue = "None",
            doc =
                "JSONPath to insert the expire month from network token to the input payload and return",
            allowedTypes = {@ParamType(type = String.class), @ParamType(type = NoneType.class)}),
        @Param(
            name = "exp_year",
            named = true,
            defaultValue = "None",
            doc =
                "JSONPath to insert the expire year from network token to the input payload and return",
            allowedTypes = {@ParamType(type = String.class), @ParamType(type = NoneType.class)}),
        @Param(
            name = "cryptogram_value",
            named = true,
            defaultValue = "None",
            doc =
                "JSONPath to insert the cryptogram value from network token to the input payload and return",
            allowedTypes = {@ParamType(type = String.class), @ParamType(type = NoneType.class)}),
        @Param(
            name = "cryptogram_eci",
            named = true,
            defaultValue = "None",
            doc =
                "JSONPath to insert the cryptogram eci from network token to the input payload and return",
            allowedTypes = {@ParamType(type = String.class), @ParamType(type = NoneType.class)}),
      })
  @Override
  public Object render(
      Object input,
      String pan,
      Object expireMonth,
      Object expireYear,
      Object cryptogramValue,
      Object cryptogramEci,
      StarlarkThread thread)
      throws EvalException {
    final Object jsonPayload = fromStarlark(input);
    final DocumentContext context = JsonPath.parse(jsonPayload);

    final String panAlias = context.read(pan);
    if (panAlias == null || panAlias.trim().isEmpty()) {
      throw new IllegalArgumentException("Pan argument is required");
    }

    final Optional<NetworkTokenService.NetworkToken> networkTokenOptional;
    try {
      networkTokenOptional = networkTokenService.getNetworkToken(panAlias);
    } catch (UnsupportedOperationException exception) {
      throw Starlark.errorf("nts.render's getNetworkToken operation must be overridden");
    }
    if (!networkTokenOptional.isPresent()) {
      throw Starlark.errorf("network token not found");
    }
    final NetworkTokenService.NetworkToken networkToken = networkTokenOptional.get();

    // Map from JSON path to its corresponding value to insert into the input JSON payload
    final ImmutableList<Map.Entry<Optional<String>, Object>> valuePlacements =
        ImmutableList.<Map.Entry<Optional<String>, Object>>builder()
            .add(Maps.immutableEntry(Optional.of(pan), networkToken.getToken()))
            .add(Maps.immutableEntry(optionalValue(expireYear), networkToken.getExpireYear()))
            .add(Maps.immutableEntry(optionalValue(expireMonth), networkToken.getExpireMonth()))
            .add(
                Maps.immutableEntry(
                    optionalValue(cryptogramValue), networkToken.getCryptogramValue()))
            .add(Maps.immutableEntry(optionalValue(cryptogramEci), networkToken.getCryptogramEci()))
            .build();
    // Set values for each JSON path and value pairs to the output JSON payload
    valuePlacements.stream()
        // We are only interested in making insertions for present JSONPaths
        .filter(keyValue -> keyValue.getKey().isPresent())
        .forEach(
            keyValue -> {
              final String jsonPath = keyValue.getKey().get();
              context.set(jsonPath, keyValue.getValue());
            });
    return toStarlark(thread, context.json());
  }

  // Convert sparkly JSON object into ordinary Java JSON object
  private static Object fromStarlark(Object object) {
    if (object instanceof Dict) {
      return fromStarlarkDict((Dict) object);
    }
    if (object instanceof StarlarkList) {
      return fromStarlarkList((StarlarkList) object);
    }
    return object;
  }

  private static Map<Object, Object> fromStarlarkDict(Dict<Object, Object> dict) {
    return dict.entrySet().stream()
        .map(
            entry ->
                Maps.immutableEntry(fromStarlark(entry.getKey()), fromStarlark(entry.getValue())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static List<Object> fromStarlarkList(StarlarkList<Object> list) {
    return list.stream().map(value -> fromStarlark(value)).collect(Collectors.toList());
  }

  private static Object toStarlark(StarlarkThread thread, Object object) {
    if (object instanceof Map) {
      return toStarlarkDict(thread, (Map<Object, Object>) object);
    }
    if (object instanceof List) {
      return toStarlarkList(thread, (List<Object>) object);
    }
    if (object instanceof Integer) {
      return StarlarkInt.of((Integer) object);
    }
    return object;
  }

  private static Dict<Object, Object> toStarlarkDict(
      StarlarkThread thread, Map<Object, Object> map) {
    final Map<Object, Object> convertedMap =
        map.entrySet().stream()
            .map(
                entry ->
                    Maps.immutableEntry(
                        toStarlark(thread, entry.getKey()), toStarlark(thread, entry.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return Dict.copyOf(thread.mutability(), convertedMap);
  }

  private static StarlarkList<Object> toStarlarkList(StarlarkThread thread, List<Object> list) {
    final List<Object> convertedList =
        list.stream().map(value -> toStarlark(thread, value)).collect(Collectors.toList());
    return StarlarkList.copyOf(thread.mutability(), convertedList);
  }

  private static <T> Optional<T> optionalValue(Object obj) {
    return obj == Starlark.NONE ? Optional.empty() : Optional.of((T) obj);
  }
}
