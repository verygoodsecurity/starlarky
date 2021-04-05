package com.verygood.security.larky.modules.utils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

public final class NumOpsUtils {

  // Default constructor
  private NumOpsUtils() {
  }

  static long  UINT8_MAX = 0xFF;
  static long UINT16_MAX = 0xFFFF;
  static long UINT24_MAX = 0XFFFFFF;
  static long UINT32_MAX = 0xFFFF_FFFFL;
  static long UINT40_MAX = 0xFFFF_FF00_0000_0000L;
  static long UINT48_MAX = 0xFFFF_0000_0000_0000L;
  static long UINT56_MAX = 0x00FF_FFFF_FFFF_FFFFL;
  static long UINT64_MAX = 0xFFFF_FFFF_FFFF_FFFFL;
  static BigInteger BI_UINT32_MAX = toUnsignedBigInteger(UINT32_MAX);
  static BigInteger BI_UINT64_MAX = toUnsignedBigInteger(UINT64_MAX);

  /**
  * Return a BigInteger equal to the unsigned value of the
  * argument.
  */
  public static BigInteger toUnsignedBigInteger(long i) {
     if (i >= 0L)
         return BigInteger.valueOf(i);
     else {
         int upper = (int) (i >>> 32);
         int lower = (int) i;

         // return (upper << 32) + lower
         return (
             BigInteger.valueOf(Integer.toUnsignedLong(upper))).shiftLeft(32).
             add(BigInteger.valueOf(Integer.toUnsignedLong(lower)));
     }
  }


  /**
   * Computes Jacobi(p,n).
   * Assumes n positive, odd, n>=3.
   * Compute the jacobi symbol <code>(a/n)</code>, as described in:
   * <a href="http://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.186-4.pdf">Digital signature standard (DSS). FIPS PUB 186-4, National Institute of Standards and
   * Technology (NIST), 2013.</a>, pp. 76-77
   * @param p the starting value of p
   * @param n the value of n
   * @return the computed jacobi symbol
   */
  public static int jacobiSymbol(int p, BigInteger n) {
      if (p == 0)
          return 0;

      // Algorithm and comments adapted from Colin Plumb's C library.
      int j = 1;
      int u = n.bitCount();//n.mag[n.mag.length-1];

      // Make p positive
      if (p < 0) {
          p = -p;
          int n8 = u & 7;
          if ((n8 == 3) || (n8 == 7))
              j = -j; // 3 (011) or 7 (111) mod 8
      }

      // Get rid of factors of 2 in p
      while ((p & 3) == 0)
          p >>= 2;
      if ((p & 1) == 0) {
          p >>= 1;
          if (((u ^ (u>>1)) & 2) != 0)
              j = -j; // 3 (011) or 5 (101) mod 8
      }
      if (p == 1)
          return j;
      // Then, apply quadratic reciprocity
      if ((p & u & 2) != 0)   // p = u = 3 (mod 4)?
          j = -j;
      // And reduce u mod p
      u = n.mod(BigInteger.valueOf(p)).intValue();

      // Now compute Jacobi(u,p), u < p
      while (u != 0) {
          while ((u & 3) == 0)
              u >>= 2;
          if ((u & 1) == 0) {
              u >>= 1;
              if (((p ^ (p>>1)) & 2) != 0)
                  j = -j;     // 3 (011) or 5 (101) mod 8
          }
          if (u == 1)
              return j;
          // Now both u and p are odd, so use quadratic reciprocity
          assert (u < p);
          int t = u; u = p; p = t;
          if ((u & p & 2) != 0) // u = p = 3 (mod 4)?
              j = -j;
          // Now u >= p, so it can be reduced
          u %= p;
      }
      return 0;
  }

  public static BigInteger lcm(BigInteger a, BigInteger b) {
    if (a.equals(BigInteger.ZERO) || b.equals(BigInteger.ZERO)) {
      return BigInteger.ZERO;
    }
    BigInteger gcd = a.gcd(b);
    return a.divide(gcd).multiply(b).abs();
  }

  public static BigInteger bytes2bigint(byte[] bytes, boolean bigEndian, boolean signed) {
    if (bytes.length == 0) {
        // in case of empty byte array
        return BigInteger.ZERO;
      }
      BigInteger result;
      if (bigEndian) {
        result = signed
            ? new BigInteger(bytes)
            : new BigInteger(1, bytes);
      } else {
        byte[] converted = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
          converted[bytes.length - i - 1] = bytes[i];
        }
        result = signed
            ? new BigInteger(converted)
            : new BigInteger(1, converted);
      }
      return result;
  }
  public static byte[] bigint2byte(BigInteger value, int byteCount, boolean bigEndian, boolean signed) {
    if (byteCount < 0) {
      throw new IllegalArgumentException("length argument must be non-negative");
    }
    byte signByte = 0;
    if (value.compareTo(BigInteger.ZERO) < 0) {
      if (!signed) {
        throw new IllegalArgumentException("can't convert negative big int to unsigned");
      }
      signByte = -1;
    }
    byte[] bytes = value.toByteArray();
    if (bytes.length > byteCount) {
      // Check, whether we need to cut unneeded sign bytes.
      int len = bytes.length;
      int startIndex = 0;
      if (!signed) {
        for (startIndex = 0; startIndex < bytes.length; startIndex++) {
          if (bytes[startIndex] != 0) {
            break;
          }
        }
        len = Math.max(bytes.length - startIndex, byteCount);
      }
      if (len > byteCount) {
        // the corrected len is still bigger then we need.
        throw new IllegalArgumentException("big int is too big to convert");
      }
      if (bytes.length > byteCount) {
        // the array starts with sign bytes and has to be truncated to the requested
        // size
        byte[] tmp = bytes;
        bytes = new byte[len];
        System.arraycopy(tmp, startIndex, bytes, 0, len);
      }
    }

    if (bigEndian) {
      if (byteCount > bytes.length) {
        // requested array is bigger then we obtained from BigInteger
        byte[] resultBytes = new byte[byteCount];
        System.arraycopy(bytes, 0, resultBytes, resultBytes.length - bytes.length, bytes.length);
        if (signByte == -1) {
          // add sign bytes
          for (int i = 0; i < resultBytes.length - bytes.length; i++) {
            resultBytes[i] = signByte;
          }
        }
        return resultBytes;
      } else {
        return bytes;
      }
    } else {
      // little endian -> need to switch bytes
      byte[] resultBytes = new byte[byteCount];
      for (int i = 0; i < bytes.length; i++) {
        resultBytes[i] = bytes[bytes.length - 1 - i];
      }
      if (byteCount > bytes.length && signByte == -1) {
        // add sign negative bytes
        for (int i = bytes.length; i < resultBytes.length; i++) {
          resultBytes[i] = signByte;
        }
      }
      return resultBytes;
    }
  }

  public static byte[] longlong2byte(long nlong, int byteCount, boolean bigEndian, boolean signed) {
    if (byteCount < 0) {
      throw new IllegalArgumentException("length argument must be non-negative");
    }
    byte signByte = 0;
    if (nlong < 0) {
      if (!signed) {
        throw new IllegalArgumentException("can't convert negative long to unsigned");
      }
      signByte = -1;
    }
    int index;
    int delta;
    if (bigEndian) {
      index = byteCount - 1;
      delta = -1;
    } else {
      index = 0;
      delta = 1;
    }

    byte[] bytes = new byte[byteCount];
    long number = nlong;

    while (number != 0 && 0 <= index && index <= (byteCount - 1)) {
      bytes[index] = (byte) (number & 0xFF);
      if (number == signByte) {
        number = 0;
      }
      number >>= 8;
      index += delta;
    }
    if ((number != 0 && bytes.length == 1 && bytes[0] != nlong)
        || (signed && bytes.length == 1 && bytes[0] != nlong)
        || (byteCount == 0 && nlong != 0)) {

      throw new IllegalArgumentException("long is too big to convert");
    }
    if (signed) {
      while (0 <= index && index <= (byteCount - 1)) {
        bytes[index] = signByte;
        index += delta;
      }
    }
    return bytes;
  }

  /**
   * Converts a 128-bit integer represented by the two given {@code long} components to a {@code
   * byte} array.
   */
  public static byte[] int128ToByteArray(long msb, long lsb) {
    byte[] bytes = new byte[16];
    for (int i = 0; i < 8; i++) {
      bytes[i] = (byte) ((msb >> (64 - 8 * (i + 1))) & 0xFFL);
      bytes[8 + i] = (byte) ((lsb >> (64 - 8 * (i + 1))) & 0xFFL);
    }
    return bytes;
  }

  /**
   * Maps the given unsigned 32-bit number into the range of a signed int. {@code 0} maps to {@link
   * Integer#MIN_VALUE} and {@code 0xFFFFFFFF} maps to {@link Integer#MAX_VALUE} This ensures that
   * the values will still be comparable by their natural ordering.
   *
   * @see com.google.common.primitives.UnsignedInteger
   */
  public static int packUnsignedInt(long unsigned) {
    if (unsigned < 0 || unsigned > 0xFFFF_FFFFL)
      throw new IllegalArgumentException("Expected value in range 0..0xffffffffL");
    return (int) (unsigned + Integer.MIN_VALUE);
  }

  /**
   * Returns the unsigned representation of the given integer as a long. This is the inverse of
   * {@link #packUnsignedInt(long)}
   *
   * @see com.google.common.primitives.UnsignedInteger
   */
  public static long unsignedInt(int i) {
    return (long) i - Integer.MIN_VALUE;
  }

  /**
   * Converts an 8 bit integer value (0..255) into a signed byte (-128..127). The results will still
   * be comparable by their natural ordering.
   *
   * @see com.google.common.primitives.UnsignedBytes
   */
  public static byte packUnsignedByte(int i) {
    if (i < 0 || i > 255)
      throw new IllegalArgumentException("Expected value in range 0..255");
    return (byte) (i + Byte.MIN_VALUE);
  }

  /**
   * Convert a signed byte (-128..127) to an "unsigned byte" (0..255) int value. This is the inverse
   * of {@link #packUnsignedByte(int)}
   *
   * @see com.google.common.primitives.UnsignedBytes
   */
  public static int unsignedByte(byte b) {
    return (int) b - Byte.MIN_VALUE;
  }

  /**
   * Pack non-negative long into output stream. It will occupy 1-10 bytes depending on value (lower
   * values occupy smaller space)
   *
   * @param os    the data output
   * @param value the long value
   * @return the number of bytes written
   * @throws IOException if an error occurs with the stream
   */
  static public int packLong(DataOutput os, long value)
      throws IOException {

    if (value < 0) {
      throw new IllegalArgumentException("negative value: v=" + value);
    }

    int i = 1;
    while ((value & ~0x7FL) != 0) {
      os.write((((int) value & 0x7F) | 0x80));
      value >>>= 7;
      i++;
    }
    os.write((byte) value);
    return i;
  }

  /**
   * Pack non-negative long into byte array. It will occupy 1-10 bytes depending on value (lower
   * values occupy smaller space)
   *
   * @param ba    the byte array
   * @param value the long value
   * @return the number of bytes written
   * @throws IOException if an error occurs with the stream
   */
  static public int packLong(byte[] ba, long value)
      throws IOException {

    if (value < 0) {
      throw new IllegalArgumentException("negative value: v=" + value);
    }

    int i = 1;
    while ((value & ~0x7FL) != 0) {
      ba[i - 1] = (byte) (((int) value & 0x7F) | 0x80);
      value >>>= 7;
      i++;
    }
    ba[i - 1] = (byte) value;
    return i;
  }

  /**
   * Unpack positive long value from the input stream.
   *
   * @param is The input stream.
   * @return the long value
   * @throws IOException if an error occurs with the stream
   */
  static public long unpackLong(DataInput is)
      throws IOException {

    long result = 0;
    for (int offset = 0; offset < 64; offset += 7) {
      long b = is.readUnsignedByte();
      result |= (b & 0x7F) << offset;
      if ((b & 0x80) == 0) {
        return result;
      }
    }
    throw new Error("Malformed long.");
  }

  /**
   * Unpack positive long value from the byte array.
   *
   * @param ba byte array
   * @return the long value
   */
  static public long unpackLong(byte[] ba) {
    return unpackLong(ba, 0);
  }

  /**
   * Unpack positive long value from the byte array.
   * <p>
   * The index value indicates the index in the given byte array.
   *
   * @param ba    byte array
   * @param index index in ba
   * @return the long value
   */
  static public long unpackLong(byte[] ba, int index) {
    long result = 0;
    for (int offset = 0; offset < 64; offset += 7) {
      long b = ba[index++];
      result |= (b & 0x7F) << offset;
      if ((b & 0x80) == 0) {
        return result;
      }
    }
    throw new Error("Malformed long.");
  }

  /**
   * Pack non-negative int into output stream. It will occupy 1-5 bytes depending on value (lower
   * values occupy smaller space)
   *
   * @param os    the data output
   * @param value the value
   * @return the number of bytes written
   * @throws IOException if an error occurs with the stream
   */
  static public int packInt(DataOutput os, int value)
      throws IOException {

    if (value < 0) {
      throw new IllegalArgumentException("negative value: v=" + value);
    }

    int i = 1;
    while ((value & ~0x7F) != 0) {
      os.write(((value & 0x7F) | 0x80));
      value >>>= 7;
      i++;
    }

    os.write((byte) value);
    return i;
  }

  /**
   * Unpack positive int value from the input stream.
   *
   * @param is The input stream.
   * @return the long value
   * @throws IOException if an error occurs with the stream
   */
  static public int unpackInt(DataInput is)
      throws IOException {
    for (int offset = 0, result = 0; offset < 32; offset += 7) {
      int b = is.readUnsignedByte();
      result |= (b & 0x7F) << offset;
      if ((b & 0x80) == 0) {
        return result;
      }
    }
    throw new Error("Malformed integer.");
  }

  /**
   * Unpack positive int value from the input byte buffer.
   *
   * @param bb The byte buffer
   * @return the long value
   * @throws IOException if an error occurs with the stream
   */
  static public int unpackInt(ByteBuffer bb)
      throws IOException {
    for (int offset = 0, result = 0; offset < 32; offset += 7) {
      int b = bb.get() & 0xffff;
      result |= (b & 0x7F) << offset;
      if ((b & 0x80) == 0) {
        return result;
      }
    }
    throw new Error("Malformed integer.");
  }

  /**
   * Returns primitive type size to pack the value
   * @param value
   * @return number of bytes to pack value to primitive
   */
  public static int bytesToPackValueToPrimitive(BigInteger value) {
    int nbytes;
    /*-1, 0 or 1 as this BigInteger is numerically less than, equal
         *         to, or greater than {@code val}. */
    if (BI_UINT32_MAX.compareTo(value) >= 0) {
      int asint = value.intValueExact();
      if (asint <= UINT24_MAX) {
        if (asint <= UINT16_MAX) {
          if (asint <= UINT8_MAX) nbytes = 1;
          else nbytes = 2;
        } else nbytes = 3;
      } else nbytes = 4;
    } else if (BI_UINT64_MAX.compareTo(value) >= 0) {
      long aslong = value.longValueExact();
      if (aslong < UINT56_MAX) {
        if (aslong < UINT48_MAX) {
          if (aslong < UINT40_MAX) nbytes = 5;
          else nbytes = 6;
        } else nbytes = 7;
      } else nbytes = 8;
    } else {
      nbytes = (value.bitLength() + 7) / 8;
//      throw new IllegalArgumentException(
//          String.format("cannot pack value %s into a single primitive", value.toString()));
    }
    return nbytes;
  }
  
}