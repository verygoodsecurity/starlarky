package com.verygood.security.larky.modules.crypto;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteLike;
import com.verygood.security.larky.modules.utils.BitwiseUtils;
import com.verygood.security.larky.modules.utils.ByteArrayUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.math.Primes;

import java.math.BigInteger;
import java.security.SecureRandom;

public class CryptoMathModule implements StarlarkValue {

  public static final CryptoMathModule INSTANCE = new CryptoMathModule();
  private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);

  @StarlarkMethod(name = "bit_length",
      doc = "If x is nonzero, then x.bit_length() is the unique positive " +
          "integer k such that 2**(k-1) <= abs(x) < 2**k. " +
          "" +
          "Equivalently, when abs(x) is small enough to have a correctly " +
          "rounded logarithm, then k = 1 + int(log(abs(x), 2)). " +
          "" +
          "If x is zero, then x.bit_length() returns 0.",
      parameters = {
          @Param(name = "n")
      })
  public StarlarkInt bitLength(StarlarkInt n) {
    return StarlarkInt.of(n.toBigInteger().bitLength());
  }

  private boolean isBigEndian(String order) throws EvalException {
    if (order.equals("big")) {
      return true;
    }
    if (order.equals("little")) {
      return false;
    }
    throw Starlark.errorf("byteorder must be 'little' or 'big'");
  }

  @StarlarkMethod(name = "int_to_bytes",
      doc = "Return an array of bytes representing an integer.\n" +
          "\n" +
          "length\n" +
          "  Length of bytes object to use.  An OverflowError is raised if the\n" +
          "  integer is not representable with the given number of bytes.\n" +
          "byteorder\n" +
          "  The byte order used to represent the integer.  If byteorder is 'big',\n" +
          "  the most significant byte is at the beginning of the byte array.  If\n" +
          "  byteorder is 'little', the most significant byte is at the end of the\n" +
          "  byte array.  To request the native byte order of the host system, use\n" +
          "  `sys.byteorder' as the byte order value.\n" +
          "signed\n" +
          "  Determines whether two's complement is used to represent the integer.\n" +
          "  If signed is False and a negative integer is given, an OverflowError\n" +
          "  is raised.\n",
      parameters = {
          @Param(name = "integer"),
          @Param(name = "length"),
          @Param(name = "byteorder"),
          @Param(name = "signed", defaultValue = "False"),
      }, useStarlarkThread = true)
  public LarkyByteLike toBytes(StarlarkInt integer,
                               StarlarkInt byteCount,
                               String byteorder,
                               boolean signed,
                               StarlarkThread thread) throws EvalException {
    // if we're trying to pack a very big integer..
    BigInteger value = integer.toBigInteger();
    byte[] bytes;
    int nbytes = byteCount.toInt("toBytes()");
    boolean minimizeLeadingZeroPads = false;
    if (nbytes == 0) {
      minimizeLeadingZeroPads = true;
      nbytes = BitwiseUtils.bytesToPackValueToPrimitive(value);
    }
    try {
      if (MAX_LONG.compareTo(value) > 0) {
        bytes = BitwiseUtils.longlong2byte(
            value.longValueExact(),
            nbytes,
            isBigEndian(byteorder),
            signed);
      } else {
        bytes = BitwiseUtils.bigint2byte(
            value,
            nbytes,
            isBigEndian(byteorder),
            signed);
      }
    } catch (IllegalArgumentException ex) {
      throw new EvalException(
          String.format("ValueError or OverflowError: %s", ex.getMessage()),
          ex.fillInStackTrace());
    }

    //System.out.printf("bytes.length: %d, nbytes: %s, array: %s %n", bytes.length, nbytes, Arrays.toString(bytes));
    if (minimizeLeadingZeroPads) {
      bytes = ByteArrayUtil.lstrip(bytes, new byte[]{0x00});
    }
    return LarkyByte.builder(thread).setSequence(bytes).build();
  }

  // this is just like using the struct lib..
  @StarlarkMethod(name = "int_from_bytes",
      doc = "Return the integer represented by the given array of bytes.\n" +
          "\n" +
          "bytes\n" +
          "  Holds the array of bytes to convert.  The argument must either\n" +
          "  support the buffer protocol or be an iterable object producing bytes.\n" +
          "  Bytes and bytearray are examples of built-in objects that support the\n" +
          "  buffer protocol.\n" +
          "byteorder\n" +
          "  The byte order used to represent the integer.  If byteorder is 'big',\n" +
          "  the most significant byte is at the beginning of the byte array.  If\n" +
          "  byteorder is 'little', the most significant byte is at the end of the\n" +
          "  byte array.  To request the native byte order of the host system, use\n" +
          "  `sys.byteorder' as the byte order value.\n" +
          "signed\n" +
          "  Indicates whether two's complement is used to represent the integer.\n",
      parameters = {
          @Param(name = "bytes"),
          @Param(name = "byteorder"),
          @Param(name = "signed", defaultValue = "False"),
      }, useStarlarkThread = true)
  public StarlarkInt fromBytes(LarkyByteLike bytesObj,
                               String byteorder,
                               boolean signed,
                               StarlarkThread thread) throws EvalException {

    byte[] bytes = bytesObj.getBytes();
    if (bytes.length == 0) {
      // in case of empty byte array
      return StarlarkInt.of(BigInteger.ZERO);
    }
    BigInteger result;
    if (isBigEndian(byteorder)) {
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
    return StarlarkInt.of(result);
  }

  //mpz_probab_prime_p
  public void isPrime1(StarlarkInt n, StarlarkInt iters) {/* int bitlength, BigInteger e, BigInteger sqrdBound) { */
    // https://github.com/aleaxit/gmpy/blob/master/src/gmpy2_mpz_misc.c#L1370-L1379
    int iterations = Math.min(iters.toIntUnchecked(), 1000);

//    return _fastmath.isPrime(int(N), false_positive_prob, randfunc)
    SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();
    Primes.enhancedMRProbablePrimeTest(n.toBigInteger(), secureRandom, iterations);

    //BigIntegers.createRandomPrime()
  }

  @StarlarkMethod(name = "is_prime", parameters = {
      @Param(name = "n"),
      @Param(name = "false_positive_prob"),
      @Param(name = "randfunc", defaultValue = "None")
  })
  public boolean isPrime(StarlarkInt n, StarlarkFloat falsePositiveProbability, Object randfunc) {
    SecureRandom random = CryptoServicesRegistrar.getSecureRandom();
    if (!Starlark.isNullOrNone(randfunc)) {
      System.err.println("Ignoring randfunc passed into isPrime. Defaulting to SecureRandom");
    }
    int iterations = (int) Math.ceil(-Math.log(falsePositiveProbability.toDouble()) / Math.log(4));
    BigInteger candidate = n.toBigInteger();
    if (candidate.signum() < 1 || candidate.bitLength() < 2) {
      return false;
    }
    //java.lang.IllegalArgumentException: 'candidate' must be non-null and >= 2
    return Primes.isMRProbablePrime(candidate, random, iterations);
  }

//  @StarlarkMethod(name = "get_strong_prime", parameters = {
//        @Param(name = "n"),
//        @Param(name = "e"),
//        @Param(name = "false_positive_prob"),
//        @Param(name = "randfunc", defaultValue = "None")
//  })
//  public StarlarkInt getStrongPrime(StarlarkInt n, StarlarkInt e, StarlarkFloat falsePositiveProbability, Object randfunc) throws EvalException {
//    SecureRandom random = CryptoServicesRegistrar.getSecureRandom();
//    BigInteger modE = e.toBigInteger();
//    BigInteger sqrdBound = BigDecimal.valueOf(falsePositiveProbability.toDouble()).toBigInteger();
//    int certainty = (int) Math.ceil(-Math.log(falsePositiveProbability.toDouble()) / Math.log(4));
//    int bitLength = n.toIntUnchecked();
//    for (int i = 0; i != 5 * bitLength; i++) {
//      BigInteger p = BigIntegers.createRandomPrime(bitLength, certainty, random);
//      if(modE.equals(BigInteger.ZERO)) {
//        return StarlarkInt.of(p);
//      }
//      if (!p.mod(modE).equals(BigInteger.ONE)
//          && p.multiply(p).compareTo(sqrdBound) >= 0
//          && Primes.isMRProbablePrime(p, random, certainty)
//          && modE.gcd(p.subtract(BigInteger.ONE)).equals(BigInteger.ONE)) {
//        return StarlarkInt.of(p);
//      }
//    }
//    throw Starlark.errorf("unable to generate strong prime number");
//  }

}
