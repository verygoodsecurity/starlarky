package com.verygood.security.larky.modules.vgs.nts;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.modules.vgs.nts.spi.NetworkTokenService;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class MockNetworkTokenService implements NetworkTokenService {
  
  private static final String DEFAULT_MERCHANT_ID = "MC8SWErAVLuooPFYz9WTx5W1";

  private static final Map<String, NetworkToken.NetworkTokenBuilder> NETWORK_TOKENS = ImmutableMap.of(
      "MCdAhTydCJMZEzxgqhvVdkgo", NetworkToken.builder()
          .token("4111111111111111")
          .expireMonth(10)
          .expireYear(27)
          .cryptogramEci("MOCK_CRYPTOGRAM_ECI"),
      DEFAULT_MERCHANT_ID, NetworkToken.builder()
          .token("4242424242424242")
          .expireMonth(12)
          .expireYear(27)
          .cryptogramEci("MOCK_CRYPTOGRAM_ECI")
  );

  @Override
  public Optional<NetworkToken> getNetworkToken(
      String panAlias, String cvv, String amount, String currencyCode, String cryptogramType,
      String vgsMerchantId) {
    if (panAlias.equals("NOT_FOUND")) {
      return Optional.empty();
    }
    if (StringUtils.isBlank(vgsMerchantId)) {
      return Optional.of(
          getForDefaultMerchant(cryptogramType)
      ); 
    }
    if (!NETWORK_TOKENS.containsKey(vgsMerchantId)) {
      return Optional.empty(); 
    }
    final NetworkToken networkToken =
        NETWORK_TOKENS.get(vgsMerchantId)
            .cryptogramValue(
                cryptogramType.equals("DTVV") ? "MOCK_DYNAMIC_CVV" : "MOCK_CRYPTOGRAM_VALUE")
            .cryptogramType(cryptogramType)
            .build();

    return Optional.of(networkToken);
  }
  
  private NetworkToken getForDefaultMerchant(String cryptogramType) {
    return NETWORK_TOKENS.get(DEFAULT_MERCHANT_ID)
        .cryptogramValue(
            cryptogramType.equals("DTVV") ? "MOCK_DYNAMIC_CVV" : "MOCK_CRYPTOGRAM_VALUE")
        .cryptogramType(cryptogramType)
        .build();
  } 
}
