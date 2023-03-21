package com.verygood.security.larky.modules.vgs.nts;

import com.verygood.security.larky.modules.vgs.nts.spi.NetworkTokenService;
import java.util.Optional;

public class NoopNetworkTokenService implements NetworkTokenService {
  @Override
  public Optional<NetworkToken> getNetworkToken(
      String panAlias, String cvv, String amount, String currencyCode) {
    throw new UnsupportedOperationException("Not implemented");
  }
}
