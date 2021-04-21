package com.verygood.security.larky.modules.crypto.Cipher;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteArray;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.util.Arrays;
import javax.crypto.Cipher;

public class LarkyBlockCipher implements StarlarkValue {

  private final ParametersWithIV parametersWithIV;

  private static final StarlarkInt SUCCESS = StarlarkInt.of(0);

  private final BlockCipher blockCipher;
  private final Engine algo;

  public LarkyBlockCipher(BlockCipher blockCipher, Engine algo, byte[] initializationVector) {
    this.blockCipher = blockCipher;
    this.algo = algo;
    this.parametersWithIV = initializationVector != null
        ? new ParametersWithIV(algo.getKeyParams(), initializationVector)
        : null;
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
    if (this.parametersWithIV != null) {
      cipher.init(mode == Cipher.ENCRYPT_MODE, this.parametersWithIV);
    } else {
      cipher.init(mode == Cipher.ENCRYPT_MODE, this.algo.getKeyParams());
    }
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
          @Param(name = "output", allowedTypes = {@ParamType(type = LarkyByteArray.class)}),
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
