package com.verygood.security.larky.modules.nts;

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
    final String token;
    final Integer expireMonth;
    final Integer expireYear;
    final String cryptogramValue;
    final String cryptogramEci;
  }
}
