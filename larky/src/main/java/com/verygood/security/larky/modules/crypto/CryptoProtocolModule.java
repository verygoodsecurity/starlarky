package com.verygood.security.larky.modules.crypto;

import com.google.common.annotations.VisibleForTesting;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.digests.GeneralDigest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S1ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

public class CryptoProtocolModule implements StarlarkValue {

  public static final CryptoProtocolModule INSTANCE = new CryptoProtocolModule();
  private static final int BITS_IN_BYTES = 8;

  @StarlarkMethod(
      name = "PBKDF1", parameters = {
      @Param(name = "password", allowedTypes = { @ParamType(type = String.class)}),
      @Param(name = "salt", allowedTypes = { @ParamType(type = LarkyByteLike.class)}),
      @Param(name = "dkLen", allowedTypes = { @ParamType(type = StarlarkInt.class)}),
      @Param(name = "count", allowedTypes = { @ParamType(type = StarlarkInt.class)}, defaultValue = "1000"),
      @Param(name = "hashAlgo", allowedTypes = { @ParamType(type = String.class)}, defaultValue = "SHA1"),
  }, useStarlarkThread = true)
  public LarkyByteLike PBKDF1(String password, LarkyByteLike salt, StarlarkInt dkLen, StarlarkInt count, String hashAlgo, StarlarkThread thread) throws EvalException {
    byte[] results = PBKDF1(
        password,
        salt.getBytes(),
        dkLen.toIntUnchecked(),
        count.toIntUnchecked(),
        hashAlgo);
    return LarkyByte.builder(thread).setSequence(results).build();
  }


  @VisibleForTesting
  byte[] PBKDF1(String password, byte[] salt, int dkLen_, int count, String hashAlgo) throws EvalException {
    int dkLen = dkLen_ * BITS_IN_BYTES;
    GeneralDigest digest;
    switch(hashAlgo.toUpperCase()) {
      case "MD5":
        digest = new MD5Digest();
      case "SHA256":
        digest = new SHA256Digest();
      case "SHA1":
      case "SHA":
        // fallthrough
      default:
        digest = new SHA1Digest();
    }
    PKCS5S1ParametersGenerator keygen = new PKCS5S1ParametersGenerator(digest);
    keygen.init(PKCS5S1ParametersGenerator.PKCS5PasswordToBytes(password.toCharArray()), salt, count);
    KeyParameter cipherParams = (KeyParameter) keygen.generateDerivedParameters(dkLen);
    return cipherParams.getKey();
  }

}
