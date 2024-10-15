package com.verygood.security.larky.modules.vgs.nts.spi;

import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

public interface NetworkTokenService {
  /**
   * Get network token for the given PAN alias with transaction type.
   *
   * @param request Get network token request
   * @return the network token value
   */
  Optional<NetworkToken> getNetworkToken(GetNetworkTokenRequest request);

  @Data
  @Builder
  class GetNetworkTokenRequest {
    // PAN alias of the network token to get
    @NonNull private final String panAlias;
    // cvv of card for retrieving cryptogram
    @Nullable private final String cvv;
    // amount of payment for retrieving cryptogram
    @Nullable private final String amount;
    // currency code of payment for retrieving cryptogram
    @Nullable private final String currencyCode;
    // type of cryptogram
    @Nullable private final String cryptogramType;
    // id of merchant
    @Nullable private final String merchantId;
    // type of transaction for requesting cryptogram
    @Nullable private final String transactionType;
  }

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
