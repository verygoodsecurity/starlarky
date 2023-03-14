package com.verygood.security.larky.modules.nts;

import java.util.Optional;

public class MockNetworkTokenService implements NetworkTokenService {
  @Override
  public Optional<NetworkToken> getNetworkToken(String panAlias) {
    return Optional.of(
        NetworkToken.builder()
            .token("4242424242424242")
            .expireMonth(12)
            .expireYear(27)
            .cryptogramValue("MOCK_CRYPTOGRAM_VALUE")
            .cryptogramEci("MOCK_CRYPTOGRAM_ECI")
            .build());
  }
}
