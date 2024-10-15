package com.verygood.security.larky.modules.vgs.nts;

import com.verygood.security.larky.modules.vgs.nts.spi.NetworkTokenServiceV2;
import java.util.Optional;

public class NoopNetworkTokenService implements NetworkTokenServiceV2 {

  @Override
  public Optional<NetworkToken> getNetworkToken(
      String panAlias,
      String cvv,
      String amount,
      String currencyCode,
      String cryptogramType,
      String merchantId) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Optional<NetworkToken> getNetworkTokenV2(GetNetworkTokenRequest request) {
    throw new UnsupportedOperationException("Not implemented");
  }
}
