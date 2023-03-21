package com.verygood.security.larky.modules.vgs.nts;

import com.verygood.security.larky.modules.vgs.nts.spi.NetworkTokenService;
import java.util.Optional;

public class MockNetworkTokenService implements NetworkTokenService {
  @Override
  public Optional<NetworkToken> getNetworkToken(String panAlias) {
    if (panAlias.equals("NOT_FOUND")) {
      return Optional.empty();
    }
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
