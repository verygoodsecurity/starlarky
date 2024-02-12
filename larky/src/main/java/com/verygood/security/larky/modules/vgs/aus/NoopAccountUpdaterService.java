package com.verygood.security.larky.modules.vgs.aus;

import com.verygood.security.larky.modules.vgs.aus.spi.AccountUpdaterService;

public class NoopAccountUpdaterService implements AccountUpdaterService {
  @Override
  public Card getCard(
      String number, Integer expireMonth, Integer expireYear, String name, String vgsMerchantId) {
    throw new UnsupportedOperationException("Not implemented");
  }
}
