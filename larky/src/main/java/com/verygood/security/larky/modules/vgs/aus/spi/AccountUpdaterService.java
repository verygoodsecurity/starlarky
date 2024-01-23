package com.verygood.security.larky.modules.vgs.aus.spi;

import lombok.Builder;
import lombok.Data;

public interface AccountUpdaterService {
  /**
   * Get updated info for the provided card
   *
   * @param number card's number
   * @param expireMonth card's expiration month
   * @param expireYear card's expiration year
   * @param name the name on the card
   * @param clientId client id of service account to access calm API
   * @param clientSecret client secret of service account to access calm API
   * @return the updated card
   */
  Card lookupCard(
      String number,
      Integer expireMonth,
      Integer expireYear,
      String name,
      String clientId,
      String clientSecret);

  @Data
  @Builder
  class Card {
    private final String number;
    private final Integer expireMonth;
    private final Integer expireYear;
    private final String name;
  }
}
