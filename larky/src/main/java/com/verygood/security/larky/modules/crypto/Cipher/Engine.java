package com.verygood.security.larky.modules.crypto.Cipher;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;

import lombok.Getter;

public class Engine implements StarlarkValue {

  @Getter
  private final BlockCipher engine;
  @Getter
  private final KeyParameter keyParams;

  public Engine(BlockCipher engine, KeyParameter keyParams) {
    this.engine = engine;
    this.keyParams = keyParams;
  }

  @StarlarkMethod(name = "block_size", structField = true)
  public StarlarkInt block_size() {
    return StarlarkInt.of(this.engine.getBlockSize());
  }

}
