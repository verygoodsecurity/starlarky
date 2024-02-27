package com.verygood.security.larky.modules.vgs.metrics;

public enum TransactionMetricsKeys {

  KEY_AMOUNT("amount"),
  KEY_BIN("bin"),
  KEY_CURRENCY("currency"),
  KEY_PSP("psp"),
  KEY_RESULT("result"),
  KEY_TYPE("type");

  public final String key;

  TransactionMetricsKeys(String key) {
    this.key = key;
  }
}
