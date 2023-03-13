package com.verygood.security.larky.modules;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.internal.JsonFormatter;
import com.verygood.security.larky.modules.vgs.calm.Calm;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@StarlarkBuiltin(
    name = "nts",
    category = "BUILTIN",
    doc = "Overridable Vault API in Larky")
public class CalmModule implements Calm {

  public static final CalmModule INSTANCE = new CalmModule();
  
  private static final String BASE_URL = "http://localhost:8080";

  OkHttpClient client = new OkHttpClient();

  @StarlarkMethod(
      name = "render",
      doc = "generates an alias for value",
      parameters = {
          @Param(
              name = "input",
              doc = "test input",
              allowedTypes = {
                  @ParamType(type = String.class)
              }),
          @Param(
              name = "config",
              doc = "test config",
              allowedTypes = {
                  @ParamType(type = Map.class)
              }),
      })
  @Override
  public String render(String input, Map<String, String> config) {
    final String number = JsonPath.read(input, config.get("pan"));
    final NetworkToken token = requestToken(isEnrolled(config), number);
    final String rendered = JsonPath.parse(input)
        .put(breakField(config.get("pan")).getKey(), breakField(config.get("pan")).getValue(), token.getToken())
        .put(breakField(config.get("expMonth")).getKey(), breakField(config.get("expMonth")).getValue(), token.getExpMonth())
        .put(breakField(config.get("expYear")).getKey(), breakField(config.get("expYear")).getValue(), token.getExpYear())
        .put("$", "mpiData", new HashMap<>())
        .put(breakField(config.get("cvv")).getKey(), breakField(config.get("cvv")).getValue(), token.getCryptogram())
        .jsonString();
    System.out.println("\n" + JsonFormatter.prettyPrint(rendered));
    return rendered;
  }
  
  public NewFild breakField(String field) {
    return new NewFild(
        field.substring(0, field.lastIndexOf(".")),
        field.substring(field.lastIndexOf(".") + 1)
    );
  }

  private boolean isEnrolled(Map<String, String> config) {
    return "enrolled".equals(config.get("psp"));
  }
  
  private NetworkToken requestToken(boolean isEnrolled, String cardId) {
    

    HttpUrl.Builder urlBuilder
        = HttpUrl.parse(BASE_URL + "/network_tokens")
        .newBuilder();
    urlBuilder.addQueryParameter("card_id", cardId);

    String url = urlBuilder.build().toString();
    
    Request request = new Request.Builder()
        .header("Calm-Tenant", "tnt9hg3iiwy")
        .url(url)
        .build();

    Call call = client.newCall(request);
    try {
      Response response = call.execute();
      final String responseBody = response.body().string();
      final String tokenId = JsonPath.read(responseBody, "$.data[0].id");
      final String token = JsonPath.read(responseBody, "$.data[0].token");
      final int expMonth = JsonPath.read(responseBody, "$.data[0].exp_month");
      final int expYear = JsonPath.read(responseBody, "$.data[0].exp_year");
      final String cryptogram = requestCryptogram(tokenId, isEnrolled);
      return new NetworkToken(token, expMonth, expYear, cryptogram);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  private String requestCryptogram(String tokenId, boolean isEnrolled) {
    
    final String cType = isEnrolled ? "TAVV": "DTVV";

    RequestBody body = RequestBody.create(
        String.format("{\"type\": \"%s\"}", cType), 
        MediaType.parse("application/json; charset=utf-8")
    ); 

    Request request = new Request.Builder()
        .header("Calm-Tenant", "tnt9hg3iiwy")
        .url(BASE_URL + "/network_tokens/" + tokenId + "/cryptograms")
        .post(body)
        .build();

    Call call = client.newCall(request);
    try {
      Response response = call.execute();
      final String responseBody = response.body().string();
      return JsonPath.read(responseBody, "$.data.value");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
