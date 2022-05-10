package com.verygood.security.larky.modules.vgs.crypto;

import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkValue;

public interface ChaseCrypto extends StarlarkValue {

  String decrypt(StarlarkBytes jweBytes);

  String getKeys();
}
