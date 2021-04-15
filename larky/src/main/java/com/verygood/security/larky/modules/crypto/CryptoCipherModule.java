package com.verygood.security.larky.modules.crypto;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteArray;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.DESEngine;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.DESedeParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.util.Arrays;
import javax.crypto.Cipher;
import lombok.Builder;
import lombok.Getter;

public class CryptoCipherModule implements StarlarkValue {

  public static final CryptoCipherModule INSTANCE = new CryptoCipherModule();

  static class LarkyBlockCipher implements StarlarkValue {

    private final ParametersWithIV params;

    private static final StarlarkInt SUCCESS = StarlarkInt.of(0);

    private final BlockCipher blockCipher;
    private final Engine algo;

    public LarkyBlockCipher(BlockCipher blockCipher, Engine algo, byte[] initializationVector) {
      this.blockCipher = blockCipher;
      this.algo = algo;
      this.params = new ParametersWithIV(algo.getKeyParams(), initializationVector);
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
      BufferedBlockCipher cipher = new BufferedBlockCipher(this.blockCipher);
      byte[] cipherText = new byte[cipher.getOutputSize(plaintext.size())];
      operate(Cipher.ENCRYPT_MODE, plaintext, cipher, cipherText);
      output.setSequenceStorage(LarkyByte.builder(thread).setSequence(cipherText).build());
      Arrays.fill(cipherText, (byte) 0);
      return SUCCESS;
    }

    private void operate(int mode, LarkyByteLike toprocess, BufferedBlockCipher cipher, byte[] out) throws EvalException {
      cipher.init(mode == Cipher.ENCRYPT_MODE, this.params);
      byte[] bytes = toprocess.getBytes();
      int outputLen = cipher.processBytes(bytes, 0, bytes.length, out, 0);
      try {
        cipher.doFinal(out, outputLen);
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
      byte[] plainText = new byte[cipherText.size()];
      BufferedBlockCipher cipher = new BufferedBlockCipher(this.blockCipher);
      operate(Cipher.DECRYPT_MODE, cipherText, cipher, plainText);
      output.setSequenceStorage(LarkyByte.builder(thread).setSequence(plainText).build());
      Arrays.fill(plainText, (byte) 0);
      return SUCCESS;
    }

  }

  @Builder(builderClassName = "Builder")
  static class LarkyAEADCipher implements StarlarkValue {
    private AEADCipher encipher;
    private AEADCipher decipher;
    private AEADParameters params;
    private byte[] currentMac;
    //private Engine engine;

    @StarlarkMethod(
      name = "encrypt",
      parameters = {
          @Param(name = "plaintext", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
          @Param(name = "output", named = true, allowedTypes = {@ParamType(type = LarkyByteArray.class), @ParamType(type=NoneType.class)})
      },
      useStarlarkThread = true
    )
    public Tuple encrypt(LarkyByteLike plaintext, Object outputO, StarlarkThread thread) throws EvalException {
      // let's use CipherOutputStream?
      byte[] cipherText = new byte[encipher.getOutputSize(plaintext.size())];
      int len = encipher.processBytes(plaintext.getBytes(), 0, plaintext.size(), cipherText, 0);
      try {
        len += encipher.doFinal(cipherText, len);
      } catch (InvalidCipherTextException e) {
        throw new EvalException(e.getMessage(), e);
      }
      currentMac = encipher.getMac();
      //int macLength = currentMac.length;
      byte[] data = new byte[plaintext.size()];
      System.arraycopy(cipherText, 0, data, 0, data.length);
      byte[] btail = new byte[cipherText.length - plaintext.size()];
      System.arraycopy(cipherText, plaintext.size(), btail, 0, btail.length);

      LarkyByteLike ct = LarkyByteArray.builder(thread)
          .setSequence(cipherText)
          .build();
      LarkyByteLike tail = LarkyByteArray.builder(thread)
                .setSequence(btail)
                .build();
      LarkyByteLike mac = LarkyByteArray.builder(thread)
                      .setSequence(currentMac)
                      .build();
      if(Starlark.isNullOrNone(outputO)) {
        return Tuple.of(ct, tail, mac);
      }

      LarkyByteArray output = ((LarkyByteArray)outputO);
      output.setSequenceStorage(ct);
      return Tuple.of(output, tail, mac);
    }


      @StarlarkMethod(
          name = "decrypt",
          parameters = {
              @Param(name = "ciphertext", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
              @Param(name = "output", named = true, allowedTypes = {@ParamType(type = LarkyByteArray.class), @ParamType(type=NoneType.class)})
          },
          useStarlarkThread = true
      )
      public Tuple decrypt(LarkyByteLike cipherText, Object outputO, StarlarkThread thread) throws EvalException {
        byte[] plainText = new byte[decipher.getOutputSize(cipherText.size())];
//        byte[] ct;
//        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(truelength)) {
//          baos.write(cipherText.getBytes());
//          baos.write(currentMac);
//          ct = baos.toByteArray();
//        } catch (IOException e) {
//          throw new EvalException(e.getMessage(), e);
//        }

//        int len = decipher.processBytes(ct, 0, ct.length, plainText, 0);
        int len = decipher.processBytes(cipherText.getBytes(), 0,cipherText.size(),plainText,0);
        //encipher.processAADBytes(currentMac, currentMac.length, SA.length - split);
        try {
         len += decipher.doFinal(plainText, len);
        } catch (InvalidCipherTextException e) {
         throw new EvalException(e.getMessage(), e);
        }
        currentMac = decipher.getMac();
        //int macLength = currentMac.length;
        LarkyByteLike pt = LarkyByteArray.builder(thread)
           .setSequence(plainText)
           .build();
        LarkyByteLike mac = LarkyByteArray.builder(thread)
                             .setSequence(currentMac)
                             .build();
        if(Starlark.isNullOrNone(outputO)) {
          return Tuple.of(pt, Starlark.NONE, mac);
        }

        LarkyByteArray output = ((LarkyByteArray)outputO);
        output.setSequenceStorage(pt);
        return Tuple.of(output, Starlark.NONE, mac);
      }

    @StarlarkMethod(
       name = "digest",
       doc = "Return the digest of the bytes passed to the update() method\n" +
           "so far as a bytes object.",
       useStarlarkThread = true
    )
    public LarkyByteLike digest(StarlarkThread thread) throws EvalException {
//      try {
//             len += encipher.doFinal(cipherText, len);
//           } catch (InvalidCipherTextException e) {
//             throw new EvalException(e.getMessage(), e);
//           }
//           byte[] mac = encipher.getMac();
//           int macLength = mac.length;
      return null;
    }

    @StarlarkMethod(
      name = "update",
      parameters = {
          @Param(name = "associated_data", allowedTypes = {@ParamType(type = LarkyByteLike.class)})
      },
      useStarlarkThread = true
    )
    public void update(LarkyByteLike associatedData, StarlarkThread thread) throws EvalException {
      this.encipher.processAADBytes(associatedData.getBytes(),0,associatedData.size());
    }
//    public doit() {

      /**
       *  byte[] enc = new byte[encCipher.getOutputSize(P.length)];
       *         if (SA != null)
       *         {
       *             encCipher.processAADBytes(SA, 0, SA.length);
       *         }
       *         int len = encCipher.processBytes(P, 0, P.length, enc, 0);
       *         len += encCipher.doFinal(enc, len);
       *
       *         if (enc.length != len)
       *         {
       * //            System.out.println("" + enc.length + "/" + len);
       *             fail("encryption reported incorrect length: " + testName);
       *         }
       *
       *         byte[] mac = encCipher.getMac();
       * //         System.err.println(Hex.toHexString(enc));
       *         byte[] data = new byte[P.length];
       *         System.arraycopy(enc, 0, data, 0, data.length);
       *         byte[] tail = new byte[enc.length - P.length];
       *         System.arraycopy(enc, P.length, tail, 0, tail.length);
       */
      // key
      // macSize
      // nonce
      // associatedText (A) // SA?
      // P = plaintext
      // C = ciphertext
      // Tag = 4d5c2af327cd64a62cf35abd2ba6fab4
//      AEADParameters parameters = new AEADParameters(
//          engine.getKeyParams(), T.length * 8,
//          iv, A);
//      GCMBlockCipher encCipher = initCipher(encM, true, parameters);
//      GCMBlockCipher decCipher = initCipher(decM, false, parameters);
//    }
//    private GCMBlockCipher initCipher(GCMMultiplier m, boolean forEncryption, AEADParameters parameters)
//       {
//           GCMBlockCipher c = new GCMBlockCipher(createAESEngine(), m);
//           c.init(forEncryption, parameters);
//           return c;
//       }
  }

  public static class Engine implements StarlarkValue {

    @Getter
    private final BlockCipher engine;
    @Getter
    private final KeyParameter keyParams;

    public Engine(BlockCipher deSede, KeyParameter keyParams) {
      this.engine = deSede;
      this.keyParams = keyParams;;
    }

  }

  @StarlarkMethod(name = "GCMMode", parameters = {
      @Param(name = "engine", allowedTypes = {@ParamType(type = Engine.class)}),
      @Param(name = "mac_size", allowedTypes = {@ParamType(type = StarlarkInt.class)}),
      @Param(
          name = "nonce",
          allowedTypes = {@ParamType(type = NoneType.class), @ParamType(type = LarkyByteLike.class)},
          defaultValue="None"),
      @Param(
          name = "associatedText",
          allowedTypes = {@ParamType(type = NoneType.class), @ParamType(type = LarkyByteLike.class)},
          defaultValue="None"),
      @Param(
          name = "multiplier",
          allowedTypes = {@ParamType(type = NoneType.class), @ParamType(type = String.class)},
          defaultValue="None")
  })
  public LarkyAEADCipher GCMMode(Engine engine, StarlarkInt macSize, Object nonceO, Object atO, Object multiplier) {
    GCMBlockCipher encipher = new GCMBlockCipher(engine.getEngine());
    GCMBlockCipher decipher = new GCMBlockCipher(engine.getEngine());
    AEADParameters aeadParameters = new AEADParameters(
        engine.getKeyParams(),
        macSize.toIntUnchecked() * 8,
        /* nonce */Starlark.isNullOrNone(nonceO) ? null : ((LarkyByteLike) nonceO).getBytes(),
        /* associatedText */ Starlark.isNullOrNone(atO) ? null : ((LarkyByteLike) atO).getBytes());
    encipher.init(true, aeadParameters);
    decipher.init(false, aeadParameters);
    return new LarkyAEADCipher.Builder()
        .encipher(encipher)
        .decipher(decipher)
        .params(aeadParameters)
        //.engine(engine)
        .build();
  }


  @StarlarkMethod(name = "CBCMode", parameters = {
      @Param(name = "engine", allowedTypes = {@ParamType(type = Engine.class)}),
      @Param(name = "iv", allowedTypes = {@ParamType(type = LarkyByteLike.class)})
  })
  public LarkyBlockCipher CBCMode(Engine engine, LarkyByteLike iv) {

    return new LarkyBlockCipher(new CBCBlockCipher(engine.getEngine()), engine, iv.getBytes());
  }

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

  @StarlarkMethod(name = "DES", parameters = {
      @Param(name = "key", allowedTypes = {@ParamType(type = LarkyByteLike.class)})
    })
    public Engine DES(LarkyByteLike key) throws EvalException {
    DESEngine desEngine = new DESEngine();
      try {
        KeyParameter params = new KeyParameter(key.getBytes());
        return new Engine(desEngine, params);
      } catch(IllegalArgumentException e) {
        throw new EvalException(e.getMessage(), e);
      }
    }

  @StarlarkMethod(name = "AES", parameters = {
      @Param(name = "key", allowedTypes = {@ParamType(type = LarkyByteLike.class)})
  })
  public Engine AES(LarkyByteLike key) throws EvalException {
    AESEngine aesEngine = new AESEngine();
    try {
      KeyParameter params = new KeyParameter(key.getBytes());
      return new Engine(aesEngine, params);
    } catch(IllegalArgumentException e) {
      throw new EvalException(e.getMessage(), e);
    }
  }
}
