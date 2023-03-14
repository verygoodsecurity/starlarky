package com.verygood.security.larky.modules.vgs.nts.spi;

import java.util.Optional;
import lombok.Builder;
import lombok.Data;

public interface NetworkTokenService {
  /**
   * Get network token for the given PAN alias.
   *
   * @param panAlias PAN alias of the network token to get
   * @return the network token value
   */
  Optional<NetworkToken> getNetworkToken(final String panAlias);

  @Data
  @Builder
  class NetworkToken {
    private final String token;
    private final Integer expireMonth;
    private final Integer expireYear;
    private final String cryptogramValue;
    private final String cryptogramEci;
  }
}
