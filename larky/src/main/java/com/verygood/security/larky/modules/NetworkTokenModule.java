package com.verygood.security.larky.modules;

import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.verygood.security.larky.modules.vgs.calm.LarkyNetworkToken;
import com.verygood.security.messages.calm.CalmServiceGrpc;
import com.verygood.security.messages.calm.CalmSvc;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import java.util.Optional;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.NoneType;

@StarlarkBuiltin(name = "nts", category = "BUILTIN", doc = "Overridable Network Token API in Larky")
public class NetworkTokenModule implements LarkyNetworkToken {
  public static final NetworkTokenModule INSTANCE = new NetworkTokenModule();

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
      String cryptogramEci) {
    final DocumentContext context = JsonPath.parse(input);

    final String panAlias = context.read(pan);
    if (panAlias == null || panAlias.trim().isEmpty()) {
      throw new IllegalArgumentException("Pan argument is required");
    }

    // TODO:
    final ManagedChannel channel =
        Grpc.newChannelBuilder("localhost:9090", InsecureChannelCredentials.create()).build();
    final CalmServiceGrpc.CalmServiceBlockingStub stub = CalmServiceGrpc.newBlockingStub(channel);

    final CalmSvc.NetworkTokenRequest request =
        CalmSvc.NetworkTokenRequest.newBuilder()
            .setPanAlias(panAlias)
            .setVaultId("foobar")
            .setPaymentDetails(CalmSvc.PaymentDetails.newBuilder().build())
            .build();
    final CalmSvc.NetworkTokenResponse response = stub.getActiveNetworkToken(request);

    final Optional<CalmSvc.NetworkToken> networkToken =
        Optional.ofNullable(response).map(CalmSvc.NetworkTokenResponse::getToken);
    final Optional<CalmSvc.Cryptogram> cryptogram =
        Optional.ofNullable(response).map(CalmSvc.NetworkTokenResponse::getCryptogram);

    // Map from JSON path to its corresponding value to insert into the input JSON payload
    final ImmutableMap<Optional<String>, Optional<Object>> valuePlacements =
        ImmutableMap.<Optional<String>, Optional<Object>>builder()
            .put(Optional.of(pan), networkToken.map(CalmSvc.NetworkToken::getToken))
            .put(
                Optional.ofNullable(expireMonth),
                networkToken.map(CalmSvc.NetworkToken::getExpMonth))
            .put(
                Optional.ofNullable(expireYear), networkToken.map(CalmSvc.NetworkToken::getExpYear))
            .put(Optional.ofNullable(cryptogramValue), cryptogram.map(CalmSvc.Cryptogram::getValue))
            .put(Optional.ofNullable(cryptogramEci), cryptogram.map(CalmSvc.Cryptogram::getEci))
            .build();

    valuePlacements.entrySet().stream()
        // We are only interested in making insertions for present JSONPaths
        .filter(keyValue -> keyValue.getKey().isPresent())
        .forEach(
            keyValue -> {
              final String jsonPath = keyValue.getKey().get();
              context.set(jsonPath, keyValue.getValue().orElse(null));
            });

    return context.json();
  }
}
