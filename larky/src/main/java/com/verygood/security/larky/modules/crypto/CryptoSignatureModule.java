package com.verygood.security.larky.modules.crypto;

import net.starlark.java.eval.StarlarkValue;

@com.google.errorprone.annotations.Immutable
public class CryptoSignatureModule implements StarlarkValue {

  public static final CryptoSignatureModule INSTANCE = new CryptoSignatureModule();

}
