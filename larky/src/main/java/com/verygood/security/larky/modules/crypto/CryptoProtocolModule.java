package com.verygood.security.larky.modules.crypto;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteArray;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkByte;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.GeneralDigest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.digests.SHA224Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.generators.BCrypt;
import org.bouncycastle.crypto.generators.OpenBSDBCrypt;
import org.bouncycastle.crypto.generators.PKCS5S1ParametersGenerator;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
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
    @Param(name = "password", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
    @Param(name = "salt", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
    @Param(name = "dkLen", allowedTypes = {@ParamType(type = StarlarkInt.class)}, defaultValue = "16"),
    @Param(name = "count", allowedTypes = {@ParamType(type = StarlarkInt.class)}, defaultValue = "1000"),
    @Param(name = "prf", allowedTypes = {@ParamType(type = NoneType.class), @ParamType(type = StarlarkCallable.class)}, defaultValue = "None"),
    @Param(name = "hmac_hash_module", allowedTypes = {@ParamType(type = String.class), @ParamType(type = NoneType.class)}, defaultValue = "None")
  }, useStarlarkThread = true)
  public LarkyByteLike PBKDF2(LarkyByteLike password, LarkyByteLike salt, StarlarkInt dkLen, StarlarkInt count, Object prfO, Object hmacHashModuleO, StarlarkThread thread) throws EvalException {

    // first make sure only one of prf or hmac_hash_module were passed (mutually exclusive args)
    if(!Starlark.isNullOrNone(prfO) && !Starlark.isNullOrNone(hmacHashModuleO)) {
      throw Starlark.errorf("ValueError: 'prf' and 'hmac_hash_module' are mutually exclusive");
    }

    // if neither passed, set hmac_hash_module as SHA1
    if(Starlark.isNullOrNone(prfO) && Starlark.isNullOrNone(hmacHashModuleO)) {
      hmacHashModuleO = "SHA1";
    }

    // use prf of hmac module to create key of dklen (not used in java)
    BiFunction<char[], byte[], byte[]> prf = Starlark.isNullOrNone(prfO)
                   ? null
                   : (passwd, saltbytes) -> {
      try {
        LarkyByteLike res = (LarkyByteLike) Starlark.call(
          thread,
          prfO,
          Tuple.of(
            LarkyByte.builder(thread)
              .setSequence(PKCS5S2ParametersGenerator.PKCS5PasswordToBytes(passwd))
              .build(),
            LarkyByte.builder(thread)
              .setSequence(saltbytes)
              .build()),
          Dict.empty());
        return res.getBytes();
      } catch (EvalException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    };

    byte[] results = PBKDF2(
      password.toCharArray(),
      salt.getBytes(),
      dkLen.toIntUnchecked(),
      count.toIntUnchecked(),
      prf,
      hmacHashModuleO);
    return LarkyByteArray.builder(thread).setSequence(results).build();
  }

  @VisibleForTesting
  byte[] PBKDF2(char[] password, byte[] salt, int dkLen_, int count, BiFunction<char[],byte[],byte[]> prf, Object hmacHashModuleO) {
    if(prf != null) {
      return prf.apply(password, salt);
    }
    int dkLen = dkLen_ * BITS_IN_BYTES;
    String hmacModule = Starlark.isNullOrNone(hmacHashModuleO) ? "sha1" : String.valueOf(hmacHashModuleO);
    PKCS5S2ParametersGenerator keygen = new PKCS5S2ParametersGenerator(resolvePRF(hmacModule));
    keygen.init(PKCS5S2ParametersGenerator.PKCS5PasswordToBytes(password), salt, count);
    KeyParameter cipherParams = (KeyParameter) keygen.generateDerivedParameters(dkLen);
    return cipherParams.getKey();
  }

  @VisibleForTesting
  Digest resolvePRF(final String prf) {
    if (Strings.isNullOrEmpty(prf)) {
      throw new IllegalArgumentException("Cannot resolve empty PRF");
    }
    String formattedPRF = prf.toLowerCase().replaceAll("[\\W]+", "");
    switch (formattedPRF) {
      case "md5":
        return new MD5Digest();
      case "sha":
        // fallthrough
      case "sha1":
        return new SHA1Digest();
      case "sha224":
        return new SHA224Digest();
      case "sha256":
        return new SHA256Digest();
      case "sha384":
        return new SHA384Digest();
      case "sha512":
      default:
        return new SHA512Digest();
    }
  }

  @StarlarkMethod(
    name = "bcrypt", parameters = {
      @Param(name = "password", allowedTypes = {@ParamType(type = LarkyByteLike.class), @ParamType(type = StarlarkByte.class)}),
      @Param(name = "salt", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
      @Param(name = "count", allowedTypes = {@ParamType(type = StarlarkInt.class)}),
    }, useStarlarkThread = true)
  public LarkyByteLike bcrypt(Object passwordO, LarkyByteLike salt, StarlarkInt count, StarlarkThread thread) throws EvalException {
    byte[] password = larkyByteTypeToPrimitive(passwordO);
    byte[] results = OpenBSDBCrypt
                       .generate("2a", password, salt.getBytes(), count.toIntUnchecked())
                       .getBytes(StandardCharsets.UTF_8);
    return LarkyByteArray.builder(thread).setSequence(results).build();
  }

  @StarlarkMethod(
    name = "bcrypt_hashpw", parameters = {
      @Param(name = "password", allowedTypes = {@ParamType(type = LarkyByteLike.class), @ParamType(type = StarlarkByte.class)}),
      @Param(name = "salt", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
      @Param(name = "count", allowedTypes = {@ParamType(type = StarlarkInt.class)}),
    }, useStarlarkThread = true)
  public LarkyByteLike bcryptHash(Object passwordO, LarkyByteLike salt, StarlarkInt count, StarlarkThread thread) throws EvalException {
    byte[] password = larkyByteTypeToPrimitive(passwordO);
    byte[] results = BCrypt.generate(password, salt.getBytes(), count.toIntUnchecked());
    return LarkyByte.builder(thread).setSequence(results).build();
  }

  @StarlarkMethod(
    name = "bcrypt_checkpw", parameters = {
      @Param(name = "password", allowedTypes = {@ParamType(type=String.class), @ParamType(type = LarkyByteLike.class), @ParamType(type = StarlarkByte.class)}),
      @Param(name = "bcrypt_hash", allowedTypes = {@ParamType(type = LarkyByteLike.class), @ParamType(type = StarlarkByte.class)}),
    }, useStarlarkThread = true)
  public boolean bcryptCheckPassword(Object passwordO, Object bcryptHashO, StarlarkThread thread) throws EvalException {
    byte[] bcryptHash = larkyByteTypeToPrimitive(bcryptHashO);
    if(String.class.isAssignableFrom(passwordO.getClass())) {
      return OpenBSDBCrypt.checkPassword(
        new String(bcryptHash),
        ((String) passwordO).toCharArray()
      );
    }
    byte[] password = larkyByteTypeToPrimitive(passwordO);
    return OpenBSDBCrypt.checkPassword(new String(bcryptHash), password);
  }

  private byte[] larkyByteTypeToPrimitive(Object o) {
    byte[] result;
    if (StarlarkByte.class.isAssignableFrom(o.getClass())) {
      result = ((StarlarkByte) o).getBytes();
    } else if(LarkyByteLike.class.isAssignableFrom(o.getClass())) {
      result = ((LarkyByteLike) o).getBytes();
    } else {
      throw new IllegalArgumentException("Invalid larky byte type! " + o.getClass());
    }
    return result;
  }

}
