package com.verygood.security.larky.modules.nts;

import java.util.Optional;

public class NoopNetworkTokenService implements NetworkTokenService {
  @Override
  public Optional<NetworkToken> getNetworkToken(String panAlias) {
    throw new UnsupportedOperationException("Not implemented");
  }
}
