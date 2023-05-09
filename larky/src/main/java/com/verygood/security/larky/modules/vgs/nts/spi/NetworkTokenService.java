package com.verygood.security.larky.modules.vgs.nts.spi;

import java.util.Optional;
import lombok.Builder;
import lombok.Data;

public interface NetworkTokenService {
  /**
   * Get network token for the given PAN alias.
   *
   * @param panAlias PAN alias of the network token to get
   * @param cvv cvv of card for retrieving cryptogram
   * @param amount amount of payment for retrieving cryptogram
   * @param currencyCode currency code of payment for retrieving cryptogram
   * @param cryptogramType type of cryptogram
   * @return the network token value
   */
  Optional<NetworkToken> getNetworkToken(
      String panAlias, String cvv, String amount, String currencyCode, String cryptogramType);

  @Data
  @Builder
  class NetworkToken {
    private final String token;
    private final Integer expireMonth;
    private final Integer expireYear;
    private final String cryptogramValue;
    private final String cryptogramEci;
    private final String cryptogramType;
  }
}
