package com.verygood.security.larky.modules.crypto.Cipher;

import java.math.BigInteger;

import com.verygood.security.larky.modules.utils.ScopedProperty;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSABlindedEngine;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.util.BigIntegers;

public class LarkyPKCS1 implements StarlarkValue {

  public static final LarkyPKCS1 INSTANCE = new LarkyPKCS1();
  // https://github.com/Legrandin/pycryptodome/blob/5dace638b70ac35bb5d9b565f3e75f7869c9d851/src/pkcs1_decode.c#L190
  private static final int EM_PREFIX_LEN = 10;
  private static final StarlarkInt FAIL = StarlarkInt.of(-1);

  @StarlarkMethod(
    name="decode",
    parameters = {
      @Param(name="xx"),
      @Param(name="sentinel"),
      @Param(name="expected_pt_len"),
      @Param(name="output"),
      @Param(name="key_parts", allowedTypes = {
        @ParamType(type=Dict.class)
      })
    },
    useStarlarkThread = true
  )
  public StarlarkInt decode(StarlarkBytes xx, StarlarkBytes sentinel, StarlarkInt expectedPtSize, StarlarkBytes output, Dict<String, StarlarkInt> keyParts, StarlarkThread thread) throws EvalException {
    int len_em_output = output.size();
    int len_sentinel = sentinel.length();
    int expected_pt_len = expectedPtSize.toIntUnchecked();

    if (len_em_output < (EM_PREFIX_LEN + 2)) {
      return FAIL;
    }
    if (len_sentinel > len_em_output) {
      return FAIL;
    }
    if (expected_pt_len > 0 && expected_pt_len > (len_em_output - EM_PREFIX_LEN - 1)) {
      return FAIL;
    }

    final byte[] pt;
    try(ScopedProperty x = new ScopedProperty()) {
//      x.setProperty(PKCS1Encoding.NOT_STRICT_LENGTH_ENABLED_PROPERTY, "true");
      AsymmetricBlockCipher eng = new RSABlindedEngine();
      CipherParameters privParameters = getRsaKeyParameters(keyParts);

      if(len_sentinel != 0) {
        eng = new PKCS1Encoding(eng, sentinel.toByteArray());
      }
      else if(expected_pt_len != 0) {
        privParameters = new ParametersWithRandom(
          privParameters,
          CryptoServicesRegistrar.getSecureRandom());
        eng = new PKCS1Encoding(eng, expected_pt_len);
      }
      else {
        eng = new PKCS1Encoding(eng);
      }
      eng.init(false, privParameters);
      pt = eng.processBlock(xx.toByteArray(), 0, xx.size());
    } catch (Exception e) {
      throw new EvalException(e);
    }

    if(expected_pt_len > 0 && pt.length != expected_pt_len) {
      return FAIL;
    }
    output.add(StarlarkBytes.immutableOf(pt));
    return StarlarkInt.of(pt.length);
  }

  private RSAKeyParameters getRsaKeyParameters(Dict<String, StarlarkInt> keyParts) {
    BigInteger d = keyParts.get("d").toBigInteger();
    BigInteger p = keyParts.get("p").toBigInteger();
    BigInteger q = keyParts.get("q").toBigInteger();
    return new RSAPrivateCrtKeyParameters(
      keyParts.get("n").toBigInteger(),
      keyParts.get("e").toBigInteger(),
      d,
      p,
      q,
      d.remainder(p.subtract(BigInteger.ONE)),
      d.remainder(q.subtract(BigInteger.ONE)),
      BigIntegers.modOddInverse(p, q)
      );
  }
}
