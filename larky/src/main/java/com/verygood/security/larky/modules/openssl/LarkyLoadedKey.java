package com.verygood.security.larky.modules.openssl;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.DSAKey;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;

public class LarkyLoadedKey implements StarlarkValue {

  enum KEY_TYPE {
    UNKNOWN("UNKNOWN"), RSA("RSA"), DSA("DSA"), ECKey("ECKey");

    private final String val;

    KEY_TYPE(String val) {
      this.val = val;
    }

    @Override
    public String toString() {
      return this.val;
    }
  }

  final private KeyPair kp;
  final private KEY_TYPE type;

  public LarkyLoadedKey(KeyPair kp) {
    this.kp = kp;
    PrivateKey key = kp.getPrivate();
    if (key instanceof RSAKey) {
      type = KEY_TYPE.RSA;
    } else if (key instanceof DSAKey) {
      type = KEY_TYPE.DSA;
    } else if (kp.getPrivate() instanceof ECKey) {
      type = KEY_TYPE.ECKey;
    } else {
      type = KEY_TYPE.UNKNOWN;
    }
  }

  @StarlarkMethod(name = "pkey", structField = true)
  public StarlarkBytes loadPrivateKey() throws EvalException {
    return StarlarkBytes.immutableOf(kp.getPrivate().getEncoded());
//      return StarlarkBytes.builder(null).setSequence(kp.getPrivate().getEncoded()).build();
  }

  @StarlarkMethod(name = "bits", structField = true)
  public StarlarkInt bits() throws EvalException {
    switch (type) {
      case RSA:
        return StarlarkInt.of(((RSAKey) kp.getPrivate()).getModulus().bitLength());
      case DSA:
        return StarlarkInt.of(((DSAKey) kp.getPrivate()).getParams().getP().bitLength());
      case ECKey:
        return StarlarkInt.of(((ECKey) kp.getPrivate()).getParams().getCurve().getField().getFieldSize());
      default:
        throw new EvalException("Unable to determine length in bits of specified Key instance");

    }
  }

  @StarlarkMethod(name = "key_type", structField = true)
  public String keytype() {
    return type.toString();
  }

}
