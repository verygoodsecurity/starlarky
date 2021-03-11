package com.verygood.security.larky.modules.utils;

import java.math.BigInteger;

/**
 * FNV hash functions
 */
public class FnvHash {
  private FnvHash() {}

  public static class FnvHash64 {
    private FnvHash64 () {}

    private static final BigInteger FNV_PRIME = new BigInteger("1099511628211");
    private static final BigInteger FNV_OFFSET_BASIS = new BigInteger("14695981039346656037");
    private static final BigInteger M = new BigInteger("18446744073709551616");

    /**
     * 64 bit FNV-1 hash function. This method correctly hash strings of data where each character
     * is represented by one octet. Any bits more significant than bit 0-7 are ignored. If multi
     * byte characters are used, the input should be provided as a byte array
     *
     * @param inpString The data to hash
     * @return A 64 bit integer representation of the FNV-1 hash
     */
    static public BigInteger getFNV1(String inpString) {

      BigInteger digest = FNV_OFFSET_BASIS;

      for (int i = 0; i < inpString.length(); i++) {
        digest = digest.multiply(FNV_PRIME).mod(M);
        digest = digest.xor(BigInteger.valueOf((int) inpString.substring(i, i + 1).charAt(0) & 255));
      }
      return (digest);
    }

    /**
     * 64 bit FNV-1a hash function. This method correctly hash strings of data where each character
     * is represented by one octet. Any bits more significant than bit 0-7 are ignored. If multi
     * byte characters are used, the input should be provided as a byte array
     *
     * @param inpString The data to hash
     * @return A 64 bit integer representation of the FNV-1a hash
     */
    static public BigInteger getFNV1a(String inpString) {

      BigInteger digest = FNV_OFFSET_BASIS;

      for (int i = 0; i < inpString.length(); i++) {
        digest = digest.xor(BigInteger.valueOf((int) inpString.substring(i, i + 1).charAt(0) & 255));
        digest = digest.multiply(FNV_PRIME).mod(M);
      }
      return (digest);
    }

    /**
     * 64 bit FNV-1a hash function.      *
     *
     * @param inp The data to hash
     * @return BigInteger holding the FNV-1a hash
     */
    static public BigInteger getFNV1a(byte[] inp) {

      BigInteger digest = FNV_OFFSET_BASIS;

      for (byte b : inp) {
        digest = digest.xor(BigInteger.valueOf((int) b & 255));
        digest = digest.multiply(FNV_PRIME).mod(M);
      }
      return digest;
    }

    /**
     * 64 bit FNV-1 hash function. This method correctly hash strings of data where each character
     * is represented by one octet. Any bits more significant than bit 0-7 are ignored. If multi
     * byte characters are used, the input should be provided as a byte array
     *
     * @param inpString The data to hash
     * @return A String holding the hex representation of the FNV-1 hash
     */
    static public String getFNV1ToHex(String inpString) {
      BigInteger digest = FNV_OFFSET_BASIS;

      for (int i = 0; i < inpString.length(); i++) {
        digest = digest.multiply(FNV_PRIME).mod(M);
        digest = digest.xor(BigInteger.valueOf((int) inpString.substring(i, i + 1).charAt(0) & 255));
      }
      return padHexString(digest);
    }

    /**
     * 64 bit FNV-1a hash function. This method correctly hash strings of data where each character
     * is represented by one octet. Any bits more significant than bit 0-7 are ignored. If multi
     * byte characters are used, the input should be provided as a byte array
     *
     * @param inpString The data to hash
     * @return A String holding the hex representation of the FNV-1a hash
     */
    static public String getFNV1aToHex(String inpString) {

      BigInteger digest = FNV_OFFSET_BASIS;

      for (int i = 0; i < inpString.length(); i++) {
        digest = digest.xor(BigInteger.valueOf((int) inpString.substring(i, i + 1).charAt(0) & 255));
        digest = digest.multiply(FNV_PRIME).mod(M);
      }
      return padHexString(digest);
    }

    /**
     * 64 bit FNV-1a hash function.      *
     *
     * @param inp The data to hash
     * @return A String holding the hex representationof the FNV-1a hash
     */
    static public String getFNV1aToHex(byte[] inp) {

      BigInteger digest = FNV_OFFSET_BASIS;

      for (byte b : inp) {
        digest = digest.xor(BigInteger.valueOf((int) b & 255));
        digest = digest.multiply(FNV_PRIME).mod(M);
      }
      return padHexString(digest);
    }
  }

  public static class FnvHash32 {
    private FnvHash32() {}

    /**
     * Length of the hash is 32-bits (4-bytes), {@value}.
     */
    private static final int LENGTH = 4;

    /**
     * Default FNV-1 seed, {@value} == (signed) 2166136261
     */
    private static final int DEFAULT_SEED_INT = -2128831035;

    /**
     * Byte representation of DEFAULT_SEED_INT
     */
    protected static final byte[] DEFAULT_SEED = (BigInteger
        .valueOf(DEFAULT_SEED_INT)
        .toByteArray());

    /**
     * Default FNV-1 prime, {@value}.
     */
    public static final long DEFAULT_PRIME = 16777619;

    /**
     * FNV-1a 32-bit hash function
     *
     * @param input Input to hash
     * @return 32-bit FNV-1a hash
     */
    public static int hash(byte[] input) {
      return hash(input, DEFAULT_SEED_INT);
    }

    /**
     * FNV-1a 32-bit hash function
     *
     * @param input Input to hash
     * @param seed  Seed to use as the offset
     * @return 32-bit FNV-1a hash
     */
    public static int hash(byte[] input, int seed) {
      if (input == null) {
        return 0;
      }

      int hash = seed;
      for (byte b : input) {
        hash ^= b;
        hash *= DEFAULT_PRIME;
      }
      return hash;
    }
  }

  static private String padHexString(BigInteger digest) {
    return padHexString(digest.toString(16), 16);
  }

  static private String padHexString(String hexString) {
    return padHexString(hexString, 16);
  }

  static private String padHexString(String hexString, int len) {
    StringBuffer b = new StringBuffer();

    for (int i = 0; i < (len - hexString.length()); i++) {
      b.append("0");
    }
    b.append(hexString);

    return b.toString();
  }
}