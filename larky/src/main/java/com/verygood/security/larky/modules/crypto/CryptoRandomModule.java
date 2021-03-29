package com.verygood.security.larky.modules.crypto;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.CryptoServicesRegistrar;

import java.math.BigInteger;
import java.security.SecureRandom;

public class CryptoRandomModule implements StarlarkValue {

  public static final CryptoRandomModule INSTANCE = new CryptoRandomModule();

  @StarlarkMethod(name = "urandom", parameters = {
      @Param(name = "n", allowedTypes = {@ParamType(type = StarlarkInt.class)}),

  }, useStarlarkThread = true)
  public LarkyByteLike urandom(StarlarkInt n, StarlarkThread thrd) throws EvalException {
    SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();
    byte[] key = new byte[n.toIntUnchecked()];
    secureRandom.nextBytes(key);
    return LarkyByte.builder(thrd).setSequence(key).build();
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
      parameters = {@Param(name = "start"), @Param(name = "stop"), @Param(name = "step")}
  )
  public StarlarkInt randrange(StarlarkInt start, StarlarkInt stop, StarlarkInt step) {
    SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();
  }

  @StarlarkMethod(
      name = "randint",
      doc = "Return a random integer N such that a <= N <= b.",
      parameters = {@Param(name = "b")}
  )
  public StarlarkInt randint(StarlarkInt f) {

  }

  @StarlarkMethod(
      name = "choice",
      doc = "Return a random element from a (non-empty) sequence. " +
          "If the seqence is empty, raises IndexError.",
      parameters = {@Param(name = "seq")}
  )
  public StarlarkInt choice(StarlarkInt seq) {

  }

  @StarlarkMethod(
      name = "shuffle",
      doc = "Shuffle the sequence in place.",
      parameters = {@Param(name = "x")}
  )
  public Sequence<?> shuffle(Sequence<?> x) {

  }

  @StarlarkMethod(
      name = "sample",
      doc = "Return a k-length list of unique elements chosen from the population sequence.",
      parameters = {@Param(name = "population"), @Param(name = "k")}
  )
  public Sequence<?> sample(Sequence<?> population, StarlarkInt k) {

  }


}
