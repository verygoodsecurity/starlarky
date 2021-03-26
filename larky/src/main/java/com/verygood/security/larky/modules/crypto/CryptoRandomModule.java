package com.verygood.security.larky.modules.crypto;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.CryptoServicesRegistrar;

import java.security.SecureRandom;

public class CryptoRandomModule implements StarlarkValue {

  public static final CryptoRandomModule INSTANCE = new CryptoRandomModule();

  @StarlarkMethod(name="urandom", parameters = {
      @Param(name = "n", allowedTypes = {@ParamType(type = StarlarkInt.class)}),

  }, useStarlarkThread = true)
  public LarkyByteLike urandom(StarlarkInt n, StarlarkThread thrd) throws EvalException {
    SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();
    byte[] key = new byte[n.toIntUnchecked()];
    secureRandom.nextBytes(key);
    return LarkyByte.builder(thrd).setSequence(key).build();
  }
}
