package com.verygood.security.larky.modules;

import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.internal.JsonFormatter;
import com.verygood.security.larky.modules.vgs.calm.LarkyNetworkToken;
import com.verygood.security.messages.calm.CalmServiceGrpc;
import com.verygood.security.messages.calm.CalmSvc;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.NoneType;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

@StarlarkBuiltin(name = "nts", category = "BUILTIN", doc = "Overridable Network Token API in Larky")
public class NetworkTokenModule implements LarkyNetworkToken {
  public static final NetworkTokenModule INSTANCE = new NetworkTokenModule();

  private static final String BASE_URL = "http://localhost:8080";

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
              // TODO: insert json path
            });

    final String number = JsonPath.read(input, config.get("pan"));
    //    final NetworkToken token = requestToken(isEnrolled(config), number);
    final String rendered =
        JsonPath.parse(input)
            .put(
                breakField(config.get("pan")).getKey(),
                breakField(config.get("pan")).getValue(),
                "foo")
            .put(
                breakField(config.get("expMonth")).getKey(),
                breakField(config.get("expMonth")).getValue(),
                "bar")
            //            .put(
            //                breakField(config.get("expYear")).getKey(),
            //                breakField(config.get("expYear")).getValue(),
            //                token.getExpYear())
            //            .put("$", "mpiData", new HashMap<>())
            //            .put(
            //                breakField(config.get("cvv")).getKey(),
            //                breakField(config.get("cvv")).getValue(),
            //                token.getCryptogram())
            .jsonString();
    System.out.println("\n" + JsonFormatter.prettyPrint(rendered));
    return rendered;
  }

  public NewFild breakField(String field) {
    return new NewFild(
        field.substring(0, field.lastIndexOf(".")), field.substring(field.lastIndexOf(".") + 1));
  }

  private boolean isEnrolled(Map<String, String> config) {
    return "enrolled".equals(config.get("psp"));
  }

  private NetworkToken requestToken(boolean isEnrolled, String cardId) {
    HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + "/network_tokens").newBuilder();
    urlBuilder.addQueryParameter("card_id", cardId);

    String url = urlBuilder.build().toString();

    Request request = new Request.Builder().header("Calm-Tenant", "tnt9hg3iiwy").url(url).build();

    //    Call call = client.newCall(request);
    //    try {
    //      Response response = call.execute();
    //      final String responseBody = response.body().string();
    //      final String tokenId = JsonPath.read(responseBody, "$.data[0].id");
    //      final String token = JsonPath.read(responseBody, "$.data[0].token");
    //      final int expMonth = JsonPath.read(responseBody, "$.data[0].exp_month");
    //      final int expYear = JsonPath.read(responseBody, "$.data[0].exp_year");
    //      final String cryptogram = requestCryptogram(tokenId, isEnrolled);
    //      return new NetworkToken(token, expMonth, expYear, cryptogram);
    //    } catch (IOException e) {
    //      throw new RuntimeException(e);
    //    }
    return new NetworkToken("", 1, 1, "");
  }

  private String requestCryptogram(String tokenId, boolean isEnrolled) {

    final String cType = isEnrolled ? "TAVV" : "DTVV";

    RequestBody body =
        RequestBody.create(
            String.format("{\"type\": \"%s\"}", cType),
            MediaType.parse("application/json; charset=utf-8"));

    Request request =
        new Request.Builder()
            .header("Calm-Tenant", "tnt9hg3iiwy")
            .url(BASE_URL + "/network_tokens/" + tokenId + "/cryptograms")
            .post(body)
            .build();

    //    Call call = client.newCall(request);
    //    try {
    //      Response response = call.execute();
    //      final String responseBody = response.body().string();
    //      return JsonPath.read(responseBody, "$.data.value");
    //    } catch (IOException e) {
    //      throw new RuntimeException(e);
    //    }
    return "";
  }

  @AllArgsConstructor
  @Getter
  private static class NetworkToken {
    private String token;
    private int expMonth;
    private int expYear;
    private String cryptogram;
  }

  @AllArgsConstructor
  @Getter
  private static class NewFild {
    private final String key;
    private final String value;
  }
}
