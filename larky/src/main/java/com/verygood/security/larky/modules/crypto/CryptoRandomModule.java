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
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.util.BigIntegers;

public class CryptoRandomModule implements StarlarkValue {

  public static final CryptoRandomModule INSTANCE = new CryptoRandomModule();

  @StarlarkMethod(name = "urandom", parameters = {
      @Param(name = "n", allowedTypes = {@ParamType(type = StarlarkInt.class)}),

  }, useStarlarkThread = true)
  public StarlarkBytes urandom(StarlarkInt n, StarlarkThread thrd) throws EvalException {
    SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();
    byte[] key = new byte[n.toIntUnchecked()];
    secureRandom.nextBytes(key);
    return StarlarkBytes.of(thrd.mutability(), key);
//    return StarlarkBytes.builder(thrd).setSequence(key).build();
  }

  @StarlarkMethod(
      name = "getrandbits",
      doc = "Return an integer with k random bits.",
      parameters = {@Param(name = "k")}
  )
  public StarlarkInt getrandbits(StarlarkInt k) {
    SecureRandom secRandom = CryptoServicesRegistrar.getSecureRandom();
    int numBits = k.toIntUnchecked();
    BigInteger candidate = new BigInteger(numBits, secRandom);
    while(candidate.equals(BigInteger.ZERO)) {
        candidate = new BigInteger(numBits, secRandom);
    }
    return StarlarkInt.of(candidate);
  }

  @StarlarkMethod(
      name = "randrange",
      doc = "randrange([start,] stop[, step]):\n" +
          "Return a randomly-selected element from range(start, stop, step).",
      parameters = {@Param(name = "start"), @Param(name = "stop", defaultValue = "None"), @Param(name = "step", defaultValue = "1")}
  )
  public StarlarkInt randrange(Object startObj, Object stopObj, StarlarkInt step_) throws EvalException {
    SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();
    int start, stop;
    int step = step_.toIntUnchecked();

    if (Starlark.isNullOrNone(stopObj)) {
        stop = Starlark.toInt(startObj, "stop");
        start = 0;
    } else {
        start = Starlark.toInt(startObj, "start");
        stop = Starlark.toInt(stopObj, "stop");
    }

    int width = stop - start;

    if (step == 0) {
        throw Starlark.errorf("zero step for radrange()");
    }

    if ((step == 1) && (width > 0)) {
      return StarlarkInt.of(start + secureRandom.nextInt(width));
    }

    int n = (step > 0) ? (width + step - 1) /step : (width + step + 1) /step;

    if (n <= 0) {
      throw Starlark.errorf("empty range for randrange()");
    }

    return StarlarkInt.of(start + step * secureRandom.nextInt(n));
  }

  @StarlarkMethod(
      name = "randint",
      doc = "Return a random integer N such that a <= N <= b.",
      parameters = {@Param(name = "min"), @Param(name = "max")}
  )
  public StarlarkInt randint(StarlarkInt min, StarlarkInt max) {
    SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();
    BigInteger rir = BigIntegers.createRandomInRange(
        min.toBigInteger(),
        max.toBigInteger(),
        secureRandom);
    return StarlarkInt.of(rir);
  }

  @StarlarkMethod(
       name = "choice",
       doc = "Return a random element from a (non-empty) sequence. " +
           "If the sequence is empty, raises IndexError.",
       parameters = {@Param(name = "seq")}
   )
   public Object choice(Sequence<?> seq) throws EvalException {
     if(seq.size() == 0) {
       throw Starlark.errorf("IndexError: sequence index is out of range");
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
  public StarlarkBytes randbytes(StarlarkInt n, StarlarkThread thread) {
    SecureRandom secRandom = CryptoServicesRegistrar.getSecureRandom();
    int numBytes = n.toIntUnchecked();
    byte[] randomBytes = new byte[numBytes];
    secRandom.nextBytes(randomBytes);
    return StarlarkBytes.of(thread.mutability(), randomBytes);
  }

  @StarlarkMethod(
      name = "shuffle",
      doc = "Shuffle the sequence (NOT IN PLACE) and will return a new shuffled list",
      parameters = {@Param(name = "x")},
      useStarlarkThread = true
  )
  public Sequence<?> shuffle(Sequence<?> x, StarlarkThread thread) {
    List<?> shufflable = new ArrayList<>(x.getImmutableList());
    Collections.shuffle(shufflable);
    return StarlarkList.copyOf(thread.mutability(), shufflable);
  }

  @StarlarkMethod(
      name = "sample",
      doc = "Return a k-length list of unique elements chosen from the population sequence.",
      parameters = {@Param(name = "population"), @Param(name = "k")},
      useStarlarkThread = true
  )
  public Sequence<?> sample(Sequence<?> population, StarlarkInt k, StarlarkThread thread) {
    int n = k.toIntUnchecked();
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
