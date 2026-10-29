package com.verygood.security.larky.modules.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.IntLimits;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.CryptoServicesRegistrar;

public class CryptoRandomModule implements StarlarkValue {

  public static final CryptoRandomModule INSTANCE = new CryptoRandomModule();

  /** System property that sets {@link #maxRandomBytes()}. */
  static final String MAX_RANDOM_BYTES_PROPERTY = "larky.random.maxBytes";

  /** The default of {@link #maxRandomBytes()}: 64 KiB. */
  static final int DEFAULT_MAX_RANDOM_BYTES = 64 << 10;

  /**
   * The most random bytes one urandom or randbytes call may produce: {@code
   * -Dlarky.random.maxBytes}, or 64 KiB if it is unset, malformed or negative. CPython has no
   * limit, but scripts are untrusted, and urandom(2**31 - 1) would otherwise allocate 2 GiB in one
   * call; scripts ask for keys, IVs and nonces (tens of bytes). Scripts cannot change it.
   * getrandbits is limited by the int limit instead ({@link IntLimits#MAX_BITS}).
   */
  static int maxRandomBytes() {
    int value = Integer.getInteger(MAX_RANDOM_BYTES_PROPERTY, DEFAULT_MAX_RANDOM_BYTES);
    return value < 0 ? DEFAULT_MAX_RANDOM_BYTES : value;
  }

  /** {@code n} as an int, failing if it is over the per-call limit of {@code max}. */
  private static int checkSize(StarlarkInt n, long max, String function, String unit)
      throws EvalException {
    if (n.compareTo(StarlarkInt.of(max)) > 0) {
      throw Starlark.errorf(
          "ValueError: %s() is limited to %d %s per call in Larky; got %s", function, max, unit, n);
    }
    return n.toIntUnchecked();
  }

  @StarlarkMethod(name = "urandom", parameters = {
      @Param(name = "n", allowedTypes = {@ParamType(type = StarlarkInt.class)}),

  }, useStarlarkThread = true)
  public StarlarkBytes urandom(StarlarkInt n, StarlarkThread thrd) throws EvalException {
    if (n.signum() < 0) {
      throw Starlark.errorf("ValueError: negative argument not allowed");
    }
    byte[] key = new byte[checkSize(n, maxRandomBytes(), "urandom", "bytes")];
    CryptoServicesRegistrar.getSecureRandom().nextBytes(key);
    return StarlarkBytes.of(thrd.mutability(), key);
  }

  @StarlarkMethod(
      name = "getrandbits",
      doc = "Return a non-negative integer with k random bits.",
      parameters = {@Param(name = "k")}
  )
  public StarlarkInt getrandbits(StarlarkInt k) throws EvalException {
    if (k.signum() < 0) {
      throw Starlark.errorf("ValueError: number of bits must be non-negative");
    }
    // Uniform over [0, 2**k); getrandbits(0) is 0, as in CPython.
    // The result has up to k bits, so k may not exceed the int limit.
    if (k.compareTo(StarlarkInt.of(IntLimits.MAX_BITS)) > 0) {
      IntLimits.checkBits(k.toBigInteger().min(BigInteger.valueOf(Long.MAX_VALUE)).longValue(), "getrandbits(k)");
    }
    return StarlarkInt.of(new BigInteger(k.toIntUnchecked(), CryptoServicesRegistrar.getSecureRandom()));
  }

  /** Returns a uniformly distributed integer in [0, n), for n > 0. */
  private static BigInteger randbelow(BigInteger n) {
    if (n.equals(BigInteger.ONE)) {
      return BigInteger.ZERO; // the only value; no need to draw
    }
    SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();
    int k = n.bitLength();
    // Rejection sampling: each draw is accepted with probability > 1/2.
    BigInteger r = new BigInteger(k, secureRandom);
    while (r.compareTo(n) >= 0) {
      r = new BigInteger(k, secureRandom);
    }
    return r;
  }

  @StarlarkMethod(
      name = "randrange",
      doc = "randrange([start,] stop[, step]):\n" +
          "Return a randomly-selected element from range(start, stop, step).",
      parameters = {
          @Param(name = "start", named = true, allowedTypes = {@ParamType(type = StarlarkInt.class)}),
          @Param(name = "stop", named = true, defaultValue = "None", allowedTypes = {
              @ParamType(type = StarlarkInt.class), @ParamType(type = NoneType.class)}),
          @Param(name = "step", named = true, defaultValue = "1", allowedTypes = {@ParamType(type = StarlarkInt.class)})
      }
  )
  public StarlarkInt randrange(StarlarkInt start, Object stop, StarlarkInt step) throws EvalException {
    // Follows CPython 3.12's Random.randrange, including its error messages.
    BigInteger istart = start.toBigInteger();
    BigInteger istep = step.toBigInteger();
    if (Starlark.isNullOrNone(stop)) {
      if (!istep.equals(BigInteger.ONE)) {
        throw Starlark.errorf("TypeError: Missing a non-None stop argument");
      }
      if (istart.signum() > 0) {
        return StarlarkInt.of(randbelow(istart));
      }
      throw Starlark.errorf("ValueError: empty range for randrange()");
    }
    BigInteger istop = ((StarlarkInt) stop).toBigInteger();
    BigInteger width = istop.subtract(istart);
    if (istep.equals(BigInteger.ONE)) {
      if (width.signum() > 0) {
        return StarlarkInt.of(istart.add(randbelow(width)));
      }
      throw Starlark.errorf("ValueError: empty range in randrange(%s, %s)", start, stop);
    }
    if (istep.signum() == 0) {
      throw Starlark.errorf("ValueError: zero step for randrange()");
    }
    // n = number of elements of range(start, stop, step). When width and step
    // have the same sign, both operands of the division share a sign, so
    // truncating division equals CPython's floor division.
    BigInteger n = BigInteger.ZERO;
    if (width.signum() == istep.signum()) {
      BigInteger adjust = istep.signum() > 0 ? BigInteger.ONE.negate() : BigInteger.ONE;
      n = width.add(istep).add(adjust).divide(istep);
    }
    if (n.signum() <= 0) {
      throw Starlark.errorf("ValueError: empty range in randrange(%s, %s, %s)", start, stop, step);
    }
    return StarlarkInt.of(istart.add(istep.multiply(randbelow(n))));
  }

  @StarlarkMethod(
      name = "randint",
      doc = "Return a random integer N such that a <= N <= b.",
      parameters = {
          @Param(name = "min", allowedTypes = {@ParamType(type = StarlarkInt.class)}),
          @Param(name = "max", allowedTypes = {@ParamType(type = StarlarkInt.class)})
      }
  )
  public StarlarkInt randint(StarlarkInt min, StarlarkInt max) throws EvalException {
    // CPython: randint(a, b) is randrange(a, b + 1).
    return randrange(min, StarlarkInt.of(max.toBigInteger().add(BigInteger.ONE)), StarlarkInt.of(1));
  }

  @StarlarkMethod(
       name = "choice",
       doc = "Return a random element from a (non-empty) sequence. " +
           "If the sequence is empty, raises IndexError.",
       parameters = {@Param(name = "seq")}
   )
   public Object choice(Sequence<?> seq) throws EvalException {
     if(seq.size() == 0) {
       throw Starlark.errorf("IndexError: Cannot choose from an empty sequence");
     }
     SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();
     int ri = secureRandom.nextInt(seq.size());
     return seq.get(ri);
   }

  @StarlarkMethod(
      name = "randbytes",
      doc = "Generate n random bytes.",
      parameters = {@Param(name = "n")},
     useStarlarkThread = true
  )
  public StarlarkBytes randbytes(StarlarkInt n, StarlarkThread thread) throws EvalException {
    if (n.signum() < 0) {
      // CPython's randbytes(n) is getrandbits(n * 8).to_bytes(n, 'little').
      throw Starlark.errorf("ValueError: number of bits must be non-negative");
    }
    byte[] randomBytes = new byte[checkSize(n, maxRandomBytes(), "randbytes", "bytes")];
    CryptoServicesRegistrar.getSecureRandom().nextBytes(randomBytes);
    return StarlarkBytes.of(thread.mutability(), randomBytes);
  }

  @StarlarkMethod(
      name = "shuffle",
      doc = "Shuffle the list x in place, and return None.",
      parameters = {@Param(name = "x")}
  )
  public Object shuffle(Object x) throws EvalException {
    if (!(x instanceof StarlarkList)) {
      throw Starlark.errorf(
          "TypeError: '%s' object does not support item assignment", Starlark.type(x));
    }
    @SuppressWarnings("unchecked")
    StarlarkList<Object> list = (StarlarkList<Object>) x;
    Starlark.checkMutable(list); // a frozen list fails before anything moves
    SecureRandom random = CryptoServicesRegistrar.getSecureRandom();
    // Fisher-Yates, as CPython's random.shuffle.
    for (int i = list.size() - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      Object tmp = list.get(i);
      list.setElementAt(i, list.get(j));
      list.setElementAt(j, tmp);
    }
    return Starlark.NONE;
  }

  @StarlarkMethod(
      name = "sample",
      doc = "Return a k-length list of unique elements chosen from the population sequence.",
      parameters = {@Param(name = "population"), @Param(name = "k")},
      useStarlarkThread = true
  )
  public Sequence<?> sample(Sequence<?> population, StarlarkInt k, StarlarkThread thread)
      throws EvalException {
    if (k.signum() < 0 || k.toBigInteger().compareTo(BigInteger.valueOf(population.size())) > 0) {
      throw Starlark.errorf("ValueError: Sample larger than population or is negative");
    }
    int n = k.toInt("k");
    List<?> shufflable = new ArrayList<>(population.getImmutableList());
    List<?> objects = pickNRandomElements(shufflable, n);
    return StarlarkList.copyOf(thread.mutability(), objects);
  }

  // taken from: https://stackoverflow.com/a/35278327/133514
  public static <E> List<E> pickNRandomElements(List<E> list, int n, SecureRandom r) {
      int length = list.size();

      if (length < n) return null;

      //We don't need to shuffle the whole list
      for (int i = length - 1; i >= length - n; --i)
      {
          Collections.swap(list, i , r.nextInt(i + 1));
      }
      return list.subList(length - n, length);
  }

  public static <E> List<E> pickNRandomElements(List<E> list, int n) {
      return pickNRandomElements(list, n, CryptoServicesRegistrar.getSecureRandom());
  }

}
