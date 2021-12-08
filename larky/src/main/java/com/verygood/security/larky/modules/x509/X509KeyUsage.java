package com.verygood.security.larky.modules.x509;

import org.bouncycastle.asn1.x509.KeyUsage;

/**
 * Enumeration of bits used in key usage extension field
 */
public enum X509KeyUsage {

  /**
   * Digital signature
   *  Binary: 10000000
   *  Hex: 0x80
   *  Dec: 128
   *
   */
  DIGITAL_SIGNATURE(KeyUsage.digitalSignature, "digitalSignature"),

  /**
   * Non repudiation
   *  Binary: 1000000
   *  Hex: 0x40
   *  Dec: 64
   *
   */
  NON_REPUDIATION(KeyUsage.nonRepudiation, "nonRepudiation"),

  /**
   * Key encipherment
   *  Binary: 100000
   *  Hex: 0x20
   *  Dec: 32
   *
   */
  KEY_ENCIPHERMENT(KeyUsage.keyEncipherment, "keyEncipherment"),

  /**
   * Data encipherment
   *  Binary: 10000
   *  Hex: 0x10
   *  Dec: 16
   *
   */
  DATA_ENCIPHERMENT(KeyUsage.dataEncipherment, "dataEncipherment"),

  /**
   * Key agreement
   *  Binary: 100
   *  Hex: 0x08
   *  Dec: 8
   *
   */
  KEY_AGREEMENT(KeyUsage.keyAgreement, "keyAgreement"),

  /**
   * Certificate signing
   *  Binary: 100
   *  Hex: 0x04
   *  Dec: 4
   *
   */
  KEY_CERTIFICATE_SIGNING(KeyUsage.keyCertSign, "keyCertSign"),

  /**
   * CRL signing
   *  Binary: 10
   *  Hex: 0x02
   *  Dec: 2
   *
   */
  CRL_SIGNING(KeyUsage.cRLSign, "crlSign"),

  /**
   * Encipher only
   *  Binary: 1
   *  Hex: 0x01
   *  Dec: 1
   *
   */
  ENCIPHER_ONLY(KeyUsage.encipherOnly, "encipherOnly"),

  /**
   * Decipherment only
   *  Binary: 1000000000000000
   *  Hex: 0x8000
   *  Dec: 32768
   *
   */
  DECIPHERMENT_ONLY(KeyUsage.decipherOnly, "decipherOnly");

  private final int bit;
  private final String name;

  X509KeyUsage(int bit, String name) {
    this.bit = bit;
    this.name = name;
  }

  /**
   * Gets the key usage bit as an integer.
   *
   * @return The key usage bit as an integer.
   */
  public int usageBit() {
    return bit;
  }

  /**
   * Gets the name of the key usage bit.
   *
   * @return The name of the key usage bit
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the key usage by name.
   *
   * @param name Key Usage Bit Name
   * @return X509KeyUsage with given name.
   *
   * @throws IllegalArgumentException If no X509KeyUsage with given name exists.
   */
  public static X509KeyUsage fromName(final String name) {
    try {
      return X509KeyUsage.valueOf(X509KeyUsage.class, name);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid X509KeyUsage name: " + name);
    }
  }
}