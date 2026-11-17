package com.verygood.security.larky.modules.vgs.nts;

import com.verygood.security.larky.modules.vgs.nts.spi.NetworkTokenService;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class MockNetworkTokenService implements NetworkTokenService {

  private static final String DEFAULT_MERCHANT_ID = "MC8SWErAVLuooPFYz9WTx5W1";

  private static final String MOCK_CRYPTOGRAM_ECI = "MOCK_CRYPTOGRAM_ECI";

  /**
   * Returns a new builder holding the immutable per-merchant data. A builder is never shared
   * between requests: Lombok builders are mutable, so the per-request cryptogram fields set on a
   * shared one would race with concurrent requests.
   */
  private static NetworkToken.NetworkTokenBuilder newBuilder(String merchantId) {
    switch (merchantId) {
      case "MCdAhTydCJMZEzxgqhvVdkgo":
        return NetworkToken.builder()
            .token("4111111111111111")
            .expireMonth(10)
            .expireYear(27)
            .cryptogramEci(MOCK_CRYPTOGRAM_ECI);
      case DEFAULT_MERCHANT_ID:
        return NetworkToken.builder()
            .token("4242424242424242")
            .expireMonth(12)
            .expireYear(27)
            .cryptogramEci(MOCK_CRYPTOGRAM_ECI);
      default:
        return null;
    }
  }

  private static NetworkToken.NetworkTokenBuilder newAftBuilder() {
    return NetworkToken.builder()
        .token("5555555555554444")
        .expireMonth(12)
        .expireYear(27)
        .cryptogramEci(MOCK_CRYPTOGRAM_ECI);
  }

  private static NetworkToken withCryptogram(
      NetworkToken.NetworkTokenBuilder builder, String cryptogramType) {
    return builder
        .cryptogramValue(
            cryptogramType.equals("DTVV") ? "MOCK_DYNAMIC_CVV" : "MOCK_CRYPTOGRAM_VALUE")
        .cryptogramType(cryptogramType)
        .build();
  }

  @Override
  public Optional<NetworkToken> getNetworkToken(GetNetworkTokenRequest request) {
    if (request.getPanAlias().equals("NOT_FOUND")) {
      return Optional.empty();
    }
    if (request.getTransactionType() != null && request.getTransactionType().equals("AFT")) {
      return Optional.of(withCryptogram(newAftBuilder(), request.getCryptogramType()));
    }
    final String merchantId =
        StringUtils.isBlank(request.getMerchantId()) ? DEFAULT_MERCHANT_ID : request.getMerchantId();
    final NetworkToken.NetworkTokenBuilder builder = newBuilder(merchantId);
    if (builder == null) {
      return Optional.empty();
    }
    return Optional.of(withCryptogram(builder, request.getCryptogramType()));
  }
}
