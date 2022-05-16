package com.verygood.security.larky.modules.crypto.Cipher;

import java.math.BigInteger;

import com.verygood.security.larky.modules.utils.ScopedProperty;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
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
      @Param(name="em"),
      @Param(name="sentinel"),
      @Param(name="expected_pt_len"),
      @Param(name="output"),
      @Param(name="key_parts", allowedTypes = {
        @ParamType(type=Dict.class)
      })
    },
    useStarlarkThread = true
  )
  public StarlarkInt decode(StarlarkBytes encryptedMessage, StarlarkBytes sentinel, StarlarkInt expectedPlainTextLen, StarlarkBytes output, Dict<String, StarlarkInt> keyParts, StarlarkThread thread) throws EvalException {
    final int len_em_output = output.size();
    final byte[] fallback = sentinel.toByteArray();
    final int len_sentinel = Starlark.isNullOrNone(sentinel) ? 0 : sentinel.length();
    final int expected_pt_len = expectedPlainTextLen.toIntUnchecked();

    if (len_em_output < (EM_PREFIX_LEN + 2)) {
      return FAIL;
    }
    if (len_sentinel > len_em_output) {
      return FAIL;
    }
    if (expected_pt_len != -1) {
      if (expected_pt_len > (len_em_output - EM_PREFIX_LEN - 1)) {
        return FAIL;
      }
    }

    byte[] pt;
    try(ScopedProperty x = new ScopedProperty()) {
      //x.setProperty(PKCS1Encoding.NOT_STRICT_LENGTH_ENABLED_PROPERTY, "true");
      AsymmetricBlockCipher eng = new RSABlindedEngine();
      CipherParameters privParameters = getRsaKeyParameters(keyParts);

      if(expected_pt_len == -1) {
        eng = new PKCS1Encoding(eng, expected_pt_len);
      }
      else if(len_sentinel != 0) {
        eng = new PKCS1Encoding(eng, sentinel.toByteArray());
      }
      else {
        privParameters = new ParametersWithRandom(
          privParameters,
          CryptoServicesRegistrar.getSecureRandom());
        eng = new PKCS1Encoding(eng, expected_pt_len);
      }
      eng.init(false, privParameters);
      pt = eng.processBlock(encryptedMessage.toByteArray(), 0, encryptedMessage.size());
    } catch (Exception e) {
      /*
       WARNING: 🐲 👉 THERE BE DRAGONS 👈 🐲

       We are using exception for control flow here and as a result,
       this is no longer constant time now.

       That is OK though. PKCS#1 v1.5 decryption is intrinsically
       vulnerable to timing attacks such as adaptive chosen-ciphertext attack (i.e. CCA2)

       Please see:
        - Bleichenbacher's attack (https://medium.com/@c0D3M/bleichenbacher-attack-explained-bc630f88ff25)
        - https://en.wikipedia.org/wiki/Adaptive_chosen-ciphertext_attack

       We attempt to mitigate the risk with some constant-time constructs.
       However, they are not sufficient by themselves: the type of protocol we
       implement and the way we handle errors make a big difference.

       Specifically, we should make it very hard for the (malicious)
       party that submitted the ciphertext to quickly understand if decryption
       succeeded or not.

       To this end, it is recommended that a protocol only encrypts
       plaintexts of fixed length, that the sentinel is a random byte string
       of the same length, and that processing continues for as long
       as possible even if sentinel is returned (i.e. in case of
       incorrect decryption).

       Given that we are *catching* the exception, there is an additional flow
       which demonstrates that the decryption has failed, because in a non-exceptional
       flow, there is no exception that is thrown. Using exceptions for control
       flow becomes definitively *NOT CONSTANT TIME*, but again, that's *OK* for
       this specific type of flow that requires sentinel as we are still continuing
       execution time and this protocol is not secure anyway, we should be using
       PKCS#1 with OAEP padding instead.
       */
      if (len_sentinel == 0) {
        throw new EvalException(e);
      }
      pt = fallback;
    }

    if(expected_pt_len != -1 && pt.length != expected_pt_len) {
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
