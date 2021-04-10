package com.verygood.security.larky.modules.crypto;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteArray;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.DESedeParameters;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.util.Arrays;
import lombok.Getter;

public class CryptoCipherModule implements StarlarkValue {

  public static final CryptoCipherModule INSTANCE = new CryptoCipherModule();

  public enum SymCipherAlg implements StarlarkValue {
    AES, DES, TRIPLEDES;
    /**
     * SymCipherAlg <==> String enum representation
     *
     * @param algoName String
     * @return SymCipherAlg enum
     */
    public static SymCipherAlg fromName(String algoName) throws EvalException {
      switch(algoName.toUpperCase().trim()) {
        case "AES":
          return SymCipherAlg.AES;
        case "DES":
          return SymCipherAlg.DES;
        case "TRIPLEDES":
        case "TDES":
        case "DESede":
          return SymCipherAlg.TRIPLEDES;
      }
      throw Starlark.errorf("Unknown algorithm: %s", algoName);
    }

    public static String valueOf(SymCipherAlg algo) throws EvalException {
      switch(algo) {
        case AES:
          return "AES";
        case DES:
          return "DES";
        case TRIPLEDES:
          return "TRIPLEDES";
      }
      throw Starlark.errorf("Unknown algorithm: %s", algo);

    }

    /*
    PKCSObjectIdentifiers
     */


  }

  static class LarkyBlockCipher implements StarlarkValue {

    private final ParametersWithIV params;

    enum MODE {
      ENCRYPT, DECRYPT
    }
    private static final StarlarkInt SUCCESS = StarlarkInt.of(0);

    private final CBCBlockCipher cbcBlockCipher;
    private final Engine algo;

    public LarkyBlockCipher(CBCBlockCipher blockCipher, Engine algo, byte[] initializationVector) {
      this.cbcBlockCipher = blockCipher;
      this.algo = algo;
      this.params = new ParametersWithIV(algo.getParams(), initializationVector);
    }

    @StarlarkMethod(
        name = "encrypt",
        parameters = {
            @Param(name = "plaintext", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
            @Param(name = "output", allowedTypes = {@ParamType(type = LarkyByteArray.class)})
        },
        useStarlarkThread = true
    )
    public StarlarkInt encrypt(LarkyByteLike plaintext, LarkyByteArray output, StarlarkThread thread) throws EvalException {
      // padding will be done by pycryptodome, this method is dangerous to call
      // directly. DO NOT CALL DIRECTLY.
      BufferedBlockCipher cipher = new BufferedBlockCipher(this.cbcBlockCipher);
      byte[] cipherText = new byte[cipher.getOutputSize(plaintext.size())];
      operate(MODE.ENCRYPT, plaintext, cipher, cipherText);
      output.setSequenceStorage(LarkyByte.builder(thread).setSequence(cipherText).build());
      Arrays.fill(cipherText, (byte) 0);
      return SUCCESS;
    }

    private void operate(MODE encrypt, LarkyByteLike toprocess, BufferedBlockCipher cipher, byte[] cipherText) throws EvalException {
      cipher.init(encrypt == MODE.ENCRYPT, this.algo.params);
      int outputLen = cipher.processBytes(toprocess.getBytes(), 0, toprocess.size(), cipherText, 0);
      try {
        cipher.doFinal(cipherText, outputLen);
      } catch (InvalidCipherTextException e) {
        throw new EvalException(e.getMessage(), e);
      }
    }

    @StarlarkMethod(
        name = "decrypt",
        parameters = {
            @Param(name = "ciphertext", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
            @Param(name = "output", allowedTypes = {@ParamType(type = LarkyByteArray.class)})
        },
        useStarlarkThread = true
    )
    public StarlarkInt decrypt(LarkyByteLike cipherText, LarkyByteArray output, StarlarkThread thread) throws EvalException {
      BufferedBlockCipher cipher = new BufferedBlockCipher(this.cbcBlockCipher);
      byte[] plainText = new byte[cipher.getOutputSize(cipherText.size())];
      operate(MODE.DECRYPT, cipherText, cipher, plainText);
      output.setSequenceStorage(LarkyByte.builder(thread).setSequence(plainText).build());
      Arrays.fill(plainText, (byte) 0);
      return SUCCESS;
    }

  }


  public static class Engine implements StarlarkValue {

    @Getter
    private final DESedeEngine engine;
    @Getter
    private final DESedeParameters params;

    public Engine(DESedeEngine deSede, DESedeParameters params) {
      this.engine = deSede;
      this.params = params;
    }
  }

  @StarlarkMethod(name = "CBCMode", parameters = {
      @Param(name = "engine", allowedTypes = {@ParamType(type = Engine.class)}),
      @Param(name = "iv", allowedTypes = {@ParamType(type = LarkyByteLike.class)})
  })
  public LarkyBlockCipher CBCMode(Engine engine, LarkyByteLike iv) throws EvalException {

    return new LarkyBlockCipher(new CBCBlockCipher(engine.getEngine()), engine, iv.getBytes());
  }
  /**
   *     try
   *     {
   *         javax.crypto.Cipher c = helper.createCipher(transformation);
   *         int    mode = encrypt ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE;
   *
   *         if (paramSpec == null) // ECB block mode
   *         {
   *             c.init(mode, sKey);
   *         }
   *         else
   *         {
   *             c.init(mode, sKey, paramSpec);
   *         }
   *         return c.doFinal(bytes);
   *     }
   * @return
   */
  @StarlarkMethod(name = "DES3", parameters = {
      @Param(name = "key", allowedTypes = {@ParamType(type = LarkyByteLike.class)})
  })
  public Engine DES3(LarkyByteLike key) throws EvalException {
    DESedeEngine deSede = new DESedeEngine();
    try {
      DESedeParameters params = new DESedeParameters(key.getBytes());
      return new Engine(deSede, params);
    } catch(IllegalArgumentException e) {
      throw new EvalException(e.getMessage(), e);
    }
  }
}
