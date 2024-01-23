package com.verygood.security.larky.modules.vgs.aus;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.modules.vgs.aus.spi.AccountUpdaterService;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class MockAccountUpdaterService implements AccountUpdaterService {
  
  private static final String DEFAULT_MERCHANT_ID = "MC8SWErAVLuooPFYz9WTx5W1";

  private static final Map<String, Card.CardBuilder> CARDS = ImmutableMap.of(
      "CRD7TPQLA4BXpN3LAYpu3mDSy", Card.builder()
          .number("4111111111111111")
          .expireMonth(10)
          .expireYear(27)
          .name("John Doe"),
      DEFAULT_MERCHANT_ID, Card.builder()
          .number("4242424242424242")
          .expireMonth(12)
          .expireYear(27)
          .name("John Doe")
  );

  @Override
  public Card lookupCard(
          String number, Integer expireMonth, Integer expireYear, String name, String vgsMerchantId) {
    if (StringUtils.isBlank(vgsMerchantId)) {
      return getForDefaultMerchant();
    }
    final Card card =
       CARDS.get(vgsMerchantId)
             .number("5204731600014784")
             .expireMonth(12)
             .expireYear(24)
             .name("John Doe")
             .build();

    return card;
  }
  
  private Card getForDefaultMerchant() {
    return CARDS.get(DEFAULT_MERCHANT_ID)
        .number("5204731600014784")
        .expireMonth(12)
        .expireYear(24)
        .name("John Doe")
        .build();
  } 
}
