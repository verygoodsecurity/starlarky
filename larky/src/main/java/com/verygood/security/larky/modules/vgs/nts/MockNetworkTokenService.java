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

  @Override
  public Optional<NetworkToken> getNetworkToken(
      String panAlias,
      String cvv,
      String amount,
      String currencyCode,
      String cryptogramType,
      String merchantId) {
    return getNetworkTokenV2(
        GetNetworkTokenRequest.builder()
            .panAlias(panAlias)
            .cvv(cvv)
            .amount(amount)
            .currencyCode(currencyCode)
            .cryptogramType(cryptogramType)
            .merchantId(merchantId)
            .build());
  }

  @Override
  public Optional<NetworkToken> getNetworkTokenV2(GetNetworkTokenRequest request) {
    if (request.getPanAlias().equals("NOT_FOUND")) {
      return Optional.empty();
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
