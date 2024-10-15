package com.verygood.security.larky.modules.vgs.nts;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.modules.vgs.nts.spi.NetworkTokenService;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class MockNetworkTokenService implements NetworkTokenService {

  private static final String DEFAULT_MERCHANT_ID = "MC8SWErAVLuooPFYz9WTx5W1";

  private static final Map<String, NetworkToken.NetworkTokenBuilder> NETWORK_TOKENS =
      ImmutableMap.of(
          "MCdAhTydCJMZEzxgqhvVdkgo",
          NetworkToken.builder()
              .token("4111111111111111")
              .expireMonth(10)
              .expireYear(27)
              .cryptogramEci("MOCK_CRYPTOGRAM_ECI"),
          DEFAULT_MERCHANT_ID,
          NetworkToken.builder()
              .token("4242424242424242")
              .expireMonth(12)
              .expireYear(27)
              .cryptogramEci("MOCK_CRYPTOGRAM_ECI"));

  private static final NetworkToken.NetworkTokenBuilder AFT_NETWORK_TOKEN =
      NetworkToken.builder()
          .token("5555555555554444")
          .expireMonth(12)
          .expireYear(27)
          .cryptogramEci("MOCK_CRYPTOGRAM_ECI");

  @Override
  public Optional<NetworkToken> getNetworkToken(GetNetworkTokenRequest request) {
    if (request.getPanAlias().equals("NOT_FOUND")) {
      return Optional.empty();
    }
    if (request.getTransactionType() != null && request.getTransactionType().equals("AFT")) {
      return Optional.of(
          AFT_NETWORK_TOKEN
              .cryptogramValue(
                  request.getCryptogramType().equals("DTVV")
                      ? "MOCK_DYNAMIC_CVV"
                      : "MOCK_CRYPTOGRAM_VALUE")
              .cryptogramType(request.getCryptogramType())
              .build());
    }
    if (StringUtils.isBlank(request.getMerchantId())) {
      return Optional.of(getForDefaultMerchant(request.getCryptogramType()));
    }
    if (!NETWORK_TOKENS.containsKey(request.getMerchantId())) {
      return Optional.empty();
    }
    final NetworkToken networkToken =
        NETWORK_TOKENS
            .get(request.getMerchantId())
            .cryptogramValue(
                request.getCryptogramType().equals("DTVV")
                    ? "MOCK_DYNAMIC_CVV"
                    : "MOCK_CRYPTOGRAM_VALUE")
            .cryptogramType(request.getCryptogramType())
            .build();

    return Optional.of(networkToken);
  }

  private NetworkToken getForDefaultMerchant(String cryptogramType) {
    return NETWORK_TOKENS
        .get(DEFAULT_MERCHANT_ID)
        .cryptogramValue(
            cryptogramType.equals("DTVV") ? "MOCK_DYNAMIC_CVV" : "MOCK_CRYPTOGRAM_VALUE")
        .cryptogramType(cryptogramType)
        .build();
  }
}
