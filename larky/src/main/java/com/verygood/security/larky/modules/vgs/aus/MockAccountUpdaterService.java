package com.verygood.security.larky.modules.vgs.aus;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.modules.vgs.aus.spi.AccountUpdaterService;
import java.util.Map;

public class MockAccountUpdaterService implements AccountUpdaterService {

  private static final Map<String, Card> CARDS =
      ImmutableMap.of(
          "4111111111111111",
          Card.builder()
              .number("4111111111111111")
              .expireMonth(10)
              .expireYear(27)
              .name("John Doe")
              .build(),
          "4242424242424242",
          Card.builder()
              .number("4242424242424243")
              .expireMonth(12)
              .expireYear(27)
              .name("John Doe")
              .build());

  @Override
  public Card lookupCard(
      String pan,
      Integer expireMonth,
      Integer expireYear,
      String name,
      String clientId,
      String clientSecret) {
    if (!CARDS.containsKey(pan)) {
      return null;
    }
    return CARDS.get(pan);
  }
}
