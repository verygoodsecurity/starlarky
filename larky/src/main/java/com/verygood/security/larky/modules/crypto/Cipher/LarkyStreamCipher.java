package com.verygood.security.larky.modules.crypto.Cipher;

import static net.starlark.java.eval.StarlarkBytes.StarlarkByteArray;

import java.util.Arrays;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.DefaultBufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.SkippingStreamCipher;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.params.ParametersWithIV;

import javax.crypto.Cipher;

public class LarkyStreamCipher<T extends SkippingStreamCipher & StreamCipher & BlockCipher> implements StarlarkValue {

  private final ParametersWithIV parametersWithIV;

  private static final StarlarkInt SUCCESS = StarlarkInt.of(0);

  private final T blockCipher;
  private final Engine algo;
  private boolean initialized;
  private int processedBytes;

  public LarkyStreamCipher(T blockCipher, Engine algo, byte[] initializationVector) {
    this.blockCipher = blockCipher;
    this.algo = algo;
    this.initialized = false;
    this.processedBytes = 0;
    this.parametersWithIV = initializationVector != null
        ? new ParametersWithIV(algo.getKeyParams(), initializationVector)
        : null;
  }

  @StarlarkMethod(
      name = "encrypt",
      parameters = {
          @Param(name = "plaintext", allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
          @Param(name = "output", allowedTypes = {@ParamType(type = StarlarkByteArray.class)})
      },
      useStarlarkThread = true
  )
  public StarlarkInt encrypt(StarlarkBytes plaintext, StarlarkByteArray output, StarlarkThread thread) throws EvalException {
    // padding will be done by pycryptodome, this method is dangerous to call
    // directly. DO NOT CALL DIRECTLY.
    if (this.initialized && this.processedBytes != 0) {
      this.blockCipher.skip(this.processedBytes);
    }
    BufferedBlockCipher cipher = new DefaultBufferedBlockCipher(this.blockCipher);
    byte[] cipherText = new byte[cipher.getOutputSize(plaintext.size())];
    operate(Cipher.ENCRYPT_MODE, plaintext, cipher, cipherText);
    output.replaceAll(StarlarkBytes.immutableOf(cipherText));
    Arrays.fill(cipherText, (byte) 0);
    return SUCCESS;
  }

  private void operate(int mode, StarlarkBytes toprocess, BufferedBlockCipher cipher, byte[] out) throws EvalException {
    if(!this.initialized) {
      if (this.parametersWithIV != null) {
        cipher.init(mode == Cipher.ENCRYPT_MODE, this.parametersWithIV);
      } else {
        cipher.init(mode == Cipher.ENCRYPT_MODE, this.algo.getKeyParams());
      }
      this.initialized = true;
    }
    byte[] bytes = toprocess.toByteArray();
    int outputLen = cipher.processBytes(bytes, 0, bytes.length, out, 0);
    try {
      cipher.doFinal(out, outputLen);
      this.processedBytes += out.length;
    } catch (InvalidCipherTextException e) {
      throw new EvalException(e.getMessage(), e);
    }
  }

  @StarlarkMethod(
      name = "decrypt",
      parameters = {
          @Param(name = "ciphertext", allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
          @Param(name = "output", allowedTypes = {@ParamType(type = StarlarkByteArray.class)}),
      },
      useStarlarkThread = true
  )
  public StarlarkInt decrypt(StarlarkBytes cipherText, StarlarkByteArray output, StarlarkThread thread) throws EvalException {
    byte[] plainText = new byte[cipherText.size()];
    if (this.initialized && this.processedBytes != 0) {
      this.blockCipher.skip(this.processedBytes);
    }
    BufferedBlockCipher cipher = new DefaultBufferedBlockCipher(this.blockCipher);
    operate(Cipher.DECRYPT_MODE, cipherText, cipher, plainText);
    output.replaceAll(StarlarkBytes.immutableOf(plainText));
    Arrays.fill(plainText, (byte) 0);
    return SUCCESS;
  }

}
