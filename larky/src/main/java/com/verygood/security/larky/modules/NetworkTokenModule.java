package com.verygood.security.larky.modules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.verygood.security.larky.modules.nts.MockNetworkTokenService;
import com.verygood.security.larky.modules.nts.NetworkTokenService;
import com.verygood.security.larky.modules.nts.NoopNetworkTokenService;
import com.verygood.security.larky.modules.vgs.calm.LarkyNetworkToken;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;

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
            name = "expire_month",
            named = true,
            defaultValue = "None",
            doc =
                "JSONPath to insert the expire month from network token to the input payload and return",
            allowedTypes = {@ParamType(type = String.class), @ParamType(type = NoneType.class)}),
        @Param(
            name = "expire_year",
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
      String expireMonth,
      String expireYear,
      String cryptogramValue,
      String cryptogramEci)
      throws EvalException {
    final DocumentContext context = JsonPath.parse(input);

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
    final ImmutableMap<Optional<String>, Object> valuePlacements =
        ImmutableMap.<Optional<String>, Object>builder()
            .put(Optional.of(pan), networkToken.getToken())
            .put(Optional.ofNullable(expireMonth), networkToken.getExpireMonth())
            .put(Optional.ofNullable(expireYear), networkToken.getExpireYear())
            .put(Optional.ofNullable(cryptogramValue), networkToken.getCryptogramValue())
            .put(Optional.ofNullable(cryptogramEci), networkToken.getCryptogramEci())
            .build();
    // Set values for each JSON path and value pairs to the output JSON payload
    valuePlacements.entrySet().stream()
        // We are only interested in making insertions for present JSONPaths
        .filter(keyValue -> keyValue.getKey().isPresent())
        .forEach(
            keyValue -> {
              final String jsonPath = keyValue.getKey().get();
              context.set(jsonPath, keyValue.getValue());
            });
    return context.json();
  }
}
