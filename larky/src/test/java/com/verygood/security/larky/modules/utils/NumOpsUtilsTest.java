package com.verygood.security.larky.modules.utils;

import static com.verygood.security.larky.modules.utils.NumOpsUtils.packUnsignedByte;
import static com.verygood.security.larky.modules.utils.NumOpsUtils.packUnsignedInt;
import static com.verygood.security.larky.modules.utils.NumOpsUtils.unsignedByte;
import static com.verygood.security.larky.modules.utils.NumOpsUtils.unsignedInt;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.common.flogger.FluentLogger;

import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.UUID;

public class NumOpsUtilsTest {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Test
  public void testInt128ToByteArray() throws Exception {
    final int iterations = 100;
    for (int i = 0; i < iterations; i++) {
      UUID uuid = UUID.randomUUID();
      long msb = uuid.getMostSignificantBits();
      long lsb = uuid.getLeastSignificantBits();
      BigInteger bigInt = new BigInteger(Long.toHexString(msb) + Long.toHexString(lsb), 16);
      // compare BigInt's implementation to the one under test
      byte[] bigIntBytes = bigInt.toByteArray();
      logger.atFine().log("bigIntBytes = " + ByteArrayUtil.toHexString(bigIntBytes, 1, ""));
      // BigInt pads the array with a 0 byte in front to preserve the sign, if the number is positive
      // we do not need this padding
      if (bigIntBytes.length == 17) {
        byte[] bigIntBytesStripped = new byte[bigIntBytes.length - 1];
        System.arraycopy(bigIntBytes, 1, bigIntBytesStripped, 0, bigIntBytesStripped.length);
        bigIntBytes = bigIntBytesStripped;
      }
      // also, BigInt's implementation will not return leading zero bytes, but ours will
      // for the sake of comparison, we pad the BigInt array with leading zeros if needed
      if (bigIntBytes.length < 16) {
        byte[] bigIntBytesPadded = new byte[16];
        System.arraycopy(bigIntBytes, 0, bigIntBytesPadded, 16 - bigIntBytes.length, bigIntBytes.length);
        bigIntBytes = bigIntBytesPadded;
      }
      byte[] ourBytes = NumOpsUtils.int128ToByteArray(msb, lsb);
      System.out.println("ourBytes = " + ByteArrayUtil.toHexString(ourBytes, 1, ""));
      assertArrayEquals(bigIntBytes, ourBytes);
    }
  }

  @Test
  public void testPackUnsignedInt32() throws Exception {
    // check that the values come out in ascending order
    assertEquals(Integer.MIN_VALUE, packUnsignedInt(0));
    assertEquals(Integer.MIN_VALUE + 1, packUnsignedInt(1));
    assertEquals(Integer.MIN_VALUE + 2, packUnsignedInt(2));
    assertEquals(Integer.MAX_VALUE - 2, packUnsignedInt(0xfffffffdL));
    assertEquals(Integer.MAX_VALUE - 1, packUnsignedInt(0xfffffffeL));
    assertEquals(Integer.MAX_VALUE, packUnsignedInt(0xffffffffL));

    // it takes too long to test every 32-bit value,
    // so just we check the endpoints and a million random values
    for (long i : new long[]{0, 1, 2, 3, 0xfffffffcL, 0xfffffffeL, 0xfffffffeL, 0xffffffffL}) {
      assertEquals(i, unsignedInt(packUnsignedInt(i)));
    }

    Random rnd = new Random();
    for (int i = 0; i < 1000000; i++) {
      long value = Math.abs(rnd.nextLong()) % 0xffffffffL;  // put the long in the 32-bit range
      assertEquals(value, unsignedInt(packUnsignedInt(value)));
    }

    // make sure that only args in range 0..0xffffffffL are accepted
    int illegalValuesChecked = 0;
    for (long i = -1000; i < 0xffffffffL + 1000; i++) {
      if (i < 0 || i > 0xffffffffL) {
        final long value = i;
        assertThrows(IllegalArgumentException.class, () -> packUnsignedInt(value));
        illegalValuesChecked++;
      }
      if (i == 0)
        i = 0xffffffffL;  // skip the valid range
    }
    logger.atFine().log("illegalValuesChecked = " + illegalValuesChecked);
  }

  @Test
  public void testUnpackUnsignedInt32() throws Exception {
    // check that the values come out in ascending order
    assertEquals(0, unsignedInt(Integer.MIN_VALUE));
    assertEquals(1, unsignedInt(Integer.MIN_VALUE + 1));
    assertEquals(2, unsignedInt(Integer.MIN_VALUE + 2));
    assertEquals(0xfffffffdL, unsignedInt(Integer.MAX_VALUE - 2));
    assertEquals(0xfffffffeL, unsignedInt(Integer.MAX_VALUE - 1));
    assertEquals(0xffffffffL, unsignedInt(Integer.MAX_VALUE));

    // it takes too long to test every 32-bit value,
    // so just we check the endpoints and a million random values
    for (int i : new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE}) {
      assertEquals(i, packUnsignedInt(unsignedInt(i)));
    }
    assertEquals(Integer.MIN_VALUE, packUnsignedInt(unsignedInt(Integer.MIN_VALUE)));
    assertEquals(Integer.MAX_VALUE, packUnsignedInt(unsignedInt(Integer.MAX_VALUE)));

    Random rnd = new Random();
    for (int i = 0; i < 1000000; i++) {
      int value = rnd.nextInt();
      assertEquals(value, packUnsignedInt(unsignedInt(value)));
    }
  }

  @Test
  public void testPackUnsignedInt8() throws Exception {
    assertEquals(Byte.MIN_VALUE, packUnsignedByte(0));
    assertEquals(Byte.MIN_VALUE + 1, packUnsignedByte(1));
    assertEquals(Byte.MIN_VALUE + 2, packUnsignedByte(2));
    assertEquals(Byte.MAX_VALUE - 2, packUnsignedByte(0xfd));
    assertEquals(Byte.MAX_VALUE - 1, packUnsignedByte(0xfe));
    assertEquals(Byte.MAX_VALUE, packUnsignedByte(0xff));


    int expected = Byte.MIN_VALUE;
    for (int i = 0; i < 256; i++) {
      assertEquals(expected++, packUnsignedByte(i));
    }

    // make sure that only args in range 0..255 are accepted
    int illegalValuesChecked = 0;
    for (int i = -1000; i < 1000; i++) {
      if (i < 0 || i > 255) {
        final int value = i;
        assertThrows(IllegalArgumentException.class,
            () -> packUnsignedByte(value));
        illegalValuesChecked++;
      }
    }
    logger.atFine().log("illegalValuesChecked = " + illegalValuesChecked);
  }

  public void testUnpackUnsignedInt8() throws Exception {
    assertEquals(0, unsignedByte(Byte.MIN_VALUE));
    assertEquals(1, unsignedByte((byte) (Byte.MIN_VALUE + 1)));
    assertEquals(2, unsignedByte((byte) (Byte.MIN_VALUE + 2)));
    assertEquals(0xfd, unsignedByte((byte) (Byte.MAX_VALUE - 2)));
    assertEquals(0xfe, unsignedByte((byte) (Byte.MAX_VALUE - 1)));
    assertEquals(0xff, unsignedByte(Byte.MAX_VALUE));

    int expected = 0;
    for (int i = Byte.MIN_VALUE; i < Byte.MAX_VALUE; i++) {
      assertEquals(expected++, unsignedByte((byte) i));
    }
    for (int i = 0; i < 256; i++) {
      assertEquals(i, unsignedByte(packUnsignedByte(i)));
    }
  }

  @Test
  public void testPackInt()
      throws IOException {
    ReadWriteDataBuffer dio = new ReadWriteDataBuffer();
    NumOpsUtils.packInt(dio.reset(), 42);
    assertEquals(NumOpsUtils.unpackInt(dio.reset(dio.toByteArray())), 42);
  }

  @Test
  public void testPackIntZero()
      throws IOException {
    ReadWriteDataBuffer dio = new ReadWriteDataBuffer();
    NumOpsUtils.packInt(dio.reset(), 0);
    assertEquals(NumOpsUtils.unpackInt(dio.reset(dio.toByteArray())), 0);
  }

  @Test
  public void testPackIntMax()
      throws IOException {
    ReadWriteDataBuffer dio = new ReadWriteDataBuffer();
    NumOpsUtils.packInt(dio.reset(), Integer.MAX_VALUE);
    assertEquals(NumOpsUtils.unpackInt(dio.reset(dio.toByteArray())), Integer.MAX_VALUE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPackIntNeg()
      throws IOException {
    ReadWriteDataBuffer dio = new ReadWriteDataBuffer();
    NumOpsUtils.packInt(dio.reset(), -42);
  }

  @Test
  public void testPackLong()
      throws IOException {
    ReadWriteDataBuffer dio = new ReadWriteDataBuffer();
    NumOpsUtils.packLong(dio.reset(), 42l);
    assertEquals(NumOpsUtils.unpackLong(dio.reset(dio.toByteArray())), 42);
  }

  @Test
  public void testPackLongZero()
      throws IOException {
    ReadWriteDataBuffer dio = new ReadWriteDataBuffer();
    NumOpsUtils.packLong(dio.reset(), 0l);
    assertEquals(NumOpsUtils.unpackLong(dio.reset(dio.toByteArray())), 0l);
  }

  @Test
  public void testPackLongBytes()
      throws IOException {
    byte[] buf = new byte[15];
    NumOpsUtils.packLong(buf, 42l);
    assertEquals(NumOpsUtils.unpackLong(buf), 42l);
  }

  @Test
  public void testPackLongMax()
      throws IOException {
    ReadWriteDataBuffer dio = new ReadWriteDataBuffer();
    NumOpsUtils.packLong(dio.reset(), Long.MAX_VALUE);
    assertEquals(NumOpsUtils.unpackLong(dio.reset(dio.toByteArray())), Long.MAX_VALUE);
  }

  @Test
  public void testPackLongBytesMax()
      throws IOException {
    byte[] buf = new byte[15];
    NumOpsUtils.packLong(buf, Long.MAX_VALUE);
    assertEquals(NumOpsUtils.unpackLong(buf), Long.MAX_VALUE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPackLongNeg()
      throws IOException {
    ReadWriteDataBuffer dio = new ReadWriteDataBuffer();
    NumOpsUtils.packLong(dio.reset(), -42l);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPackLongBytesNeg()
      throws IOException {
    NumOpsUtils.packLong(new byte[15], -42l);
  }

  @Test
  public void test()
      throws IOException {
    ReadWriteDataBuffer dio = new ReadWriteDataBuffer();
    NumOpsUtils.packInt(dio.reset(), 5);
    ByteBuffer bb = ByteBuffer.wrap(dio.getBuf());
    assertEquals(NumOpsUtils.unpackInt(bb), 5);
  }
}