package com.verygood.security.larky.modules.crypto;

import com.google.common.annotations.VisibleForTesting;

import com.verygood.security.larky.modules.types.LarkyByteArray;
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
      @Param(name = "password", allowedTypes = { @ParamType(type = LarkyByteLike.class)}),
      @Param(name = "salt", allowedTypes = { @ParamType(type = LarkyByteLike.class)}),
      @Param(name = "dkLen", allowedTypes = { @ParamType(type = StarlarkInt.class)}),
      @Param(name = "count", allowedTypes = { @ParamType(type = StarlarkInt.class)}, defaultValue = "1000"),
      @Param(name = "hashAlgo", allowedTypes = { @ParamType(type = String.class)}, defaultValue = "'SHA1'"),
  }, useStarlarkThread = true)

  public LarkyByteLike PBKDF1(LarkyByteLike password, LarkyByteLike salt, StarlarkInt dkLen, StarlarkInt count, String hashAlgo, StarlarkThread thread) throws EvalException {

    byte[] results = PBKDF1(
        password.toCharArray(),
        salt.getBytes(),
        dkLen.toIntUnchecked(),
        count.toIntUnchecked(),
        hashAlgo);
    return LarkyByteArray.builder(thread).setSequence(results).build();
  }


  @VisibleForTesting
  byte[] PBKDF1(char[] password, byte[] salt, int dkLen_, int count, String hashAlgo) throws EvalException {
    int dkLen = dkLen_ * BITS_IN_BYTES;
    GeneralDigest digest;
    switch(hashAlgo.toUpperCase()) {
      case "MD5":
        digest = new MD5Digest();
        break;
      case "SHA256":
        digest = new SHA256Digest();
        break;
      case "SHA1":
      case "SHA":
        // fallthrough
      default:
        digest = new SHA1Digest();
    }
    PKCS5S1ParametersGenerator keygen = new PKCS5S1ParametersGenerator(digest);
    keygen.init(PKCS5S1ParametersGenerator.PKCS5PasswordToBytes(password), salt, count);
    KeyParameter cipherParams = (KeyParameter) keygen.generateDerivedParameters(dkLen);
    return cipherParams.getKey();
  }

  @StarlarkMethod(
      name = "PBKDF2", parameters = {
      @Param(name = "password", allowedTypes = { @ParamType(type = LarkyByteLike.class)}),
      @Param(name = "salt", allowedTypes = { @ParamType(type = LarkyByteLike.class)}),
      @Param(name = "dkLen", allowedTypes = { @ParamType(type = StarlarkInt.class)}, defaultValue = "16" ),
      @Param(name = "count", allowedTypes = { @ParamType(type = StarlarkInt.class)}, defaultValue = "1000"),
      @Param(name = "prf", allowedTypes = { @ParamType(type = LarkyByteLike.class)}, defaultValue = ),// need to determine proper param type for function?
      @Param(name = "hmac_hash_module", allowedTypes = { @ParamType(type= String.class)})
  }, useStarLarkThread = true)
  public LarkyByteLike PBKDF2(LarkyByteLike password, LarkyByteLike salt, StarlarkInt dkLen, StarlarkInt count, String prf, Starlark Thread thread) throws EvalException {
      byte [] results = PBKDF2(
          password.toCharArray(),
          salt.getBytes(),
          dkLen.toIntUnchecked,
          count.toIntUnchecked(),
          prf,
          hmac_hash_module);
      return LarkyByteArray.builder(thread).setSequence(results).build();
  }

  @VisibleForTesting
  byte[] PBKDF2(char[] password, byte[] salt, int dkLen_, int count, String prf, String hmac_hash_module) throws EvalException {
    int dkLen = dkLen_ * BITS_IN_BYTES;
    // first make sure only one of prf or hmac_hash_module were passed (mutually exclusive args)
    // if neither passed, set hmac_hash_module as SHA1
    // use prf of hmac module to create key of dklen
    PKCS5S2ParametersGenerator keygen = new PKCS5S2ParametersGenerator();
    keygen.init(PKCS5S2ParametersGenerator.PKCS5PasswordToBytes(password), salt, count);
    KeyParameter cipherParams = (KeyParameter) keygen.generateDerivedParameters(dkLen);
    return cipherParams.getKey();
  }

}
