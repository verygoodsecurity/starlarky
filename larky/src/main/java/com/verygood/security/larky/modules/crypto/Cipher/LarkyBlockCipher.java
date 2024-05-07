package com.verygood.security.larky.modules.crypto.Cipher;

import java.util.Arrays;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkBytes.StarlarkByteArray;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DefaultBufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.params.ParametersWithIV;

import jakarta.annotation.Nonnull;


public class LarkyBlockCipher implements StarlarkValue {

  private static final StarlarkInt SUCCESS = StarlarkInt.of(0);
  private final boolean bufferedMode;
  private final CipherParameters cipherParams;
  private final BufferedBlockCipher bufferedBlockCipher;
  private boolean ENCRYPT_MODE;
  private boolean initialized;

  public LarkyBlockCipher(BlockCipher blockCipher, Engine algo, byte[] initializationVector) {
    this(blockCipher, algo, initializationVector, true);
  }

  LarkyBlockCipher(BlockCipher blockCipher, Engine algo, byte[] initializationVector, boolean bufferedMode) {
    this.cipherParams = initializationVector != null
                          ? new ParametersWithIV(algo.getKeyParams(), initializationVector)
                          : algo.getKeyParams();
    this.bufferedMode = bufferedMode;
    this.bufferedBlockCipher = new DefaultBufferedBlockCipher(blockCipher);
    init(true);
  }

  private void init(boolean mode) {
    this.ENCRYPT_MODE = mode;
    this.bufferedBlockCipher.init(this.ENCRYPT_MODE, cipherParams);
    this.initialized = true;
  }

  /**
   * NOTE: padding will be done by pycryptodome (in Larky code)
   * THIS METHOD IS DANGEROUS TO CALL DIRECTLY. DO NOT CALL DIRECTLY.
   */
  @StarlarkMethod(
    name = "encrypt",
    parameters = {
      @Param(name = "plaintext", allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
      @Param(name = "output", allowedTypes = {@ParamType(type = StarlarkByteArray.class)})
    },
    useStarlarkThread = true
  )
  public StarlarkInt encrypt(StarlarkBytes plaintext, StarlarkByteArray output, StarlarkThread thread) throws EvalException {
    initializeForEncryption();
    return process(plaintext, output);
  }

  /**
   * Initializes the cipher for encryption if ANY of the following holds true:
   *   - the cipher is not initialized
   *   - the cipher is initialized, but is in DECRYPTION mode.
   *
   * @see #initializeForDecryption()
   */
  private void initializeForEncryption() {
    if (!initialized || !this.ENCRYPT_MODE) {
      init(true);
    }
  }

  private int getOutputSize(StarlarkBytes input) {
    return this.bufferedMode
             ? this.bufferedBlockCipher.getUpdateOutputSize(input.size())
             : this.bufferedBlockCipher.getUnderlyingCipher().getBlockSize();
  }

  /**
   * NOTE: padding will be done by pycryptodome (in Larky code)
   * THIS METHOD IS DANGEROUS TO CALL DIRECTLY. DO NOT CALL DIRECTLY.
   */
  @StarlarkMethod(
    name = "decrypt",
    parameters = {
      @Param(name = "ciphertext", allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
      @Param(name = "output", allowedTypes = {@ParamType(type = StarlarkByteArray.class)}),
    },
    useStarlarkThread = true
  )
  public StarlarkInt decrypt(StarlarkBytes cipherText, StarlarkByteArray output, StarlarkThread thread) throws EvalException {
    initializeForDecryption();
    return process(cipherText, output);
  }

  @Nonnull
  private StarlarkInt process(StarlarkBytes input, StarlarkByteArray result) throws EvalException {
    byte[] outBytes = new byte[getOutputSize(input)];
    final byte[] inBytes = input.toByteArray();

    final int totalOutputLength;
    final int outLength =
      this.bufferedMode
        ? this.bufferedBlockCipher.processBytes(inBytes, 0, inBytes.length, outBytes, 0)
        : this.bufferedBlockCipher.getUnderlyingCipher().processBlock(inBytes, 0, outBytes, 0);

    if (this.bufferedMode && this.bufferedBlockCipher.getUpdateOutputSize(0) != 0) {
      final int additional;
      try {
        additional = this.bufferedBlockCipher.doFinal(outBytes, outLength);
      } catch (InvalidCipherTextException e) {
        throw new EvalException(e.getMessage(), e);
      }
      //noinspection UnusedAssignment
      totalOutputLength = outLength + additional;
    } else {
      //noinspection UnusedAssignment
      totalOutputLength = outLength;
    }

    result.replaceAll(StarlarkBytes.immutableOf(outBytes));
    Arrays.fill(outBytes, (byte) 0);
    return SUCCESS;
  }

  /**
   * Initializes the cipher for decryption if ANY of the following holds true:
   *   - the cipher is not initialized
   *   - the cipher is initialized, but is in ENCRYPTION mode.
   *
   * @see #initializeForEncryption()
   */
  private void initializeForDecryption() {
    if (!initialized || this.ENCRYPT_MODE) {
      init(false);
    }
  }

}
