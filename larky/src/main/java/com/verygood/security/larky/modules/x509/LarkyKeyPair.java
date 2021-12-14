package com.verygood.security.larky.modules.x509;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAKey;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;

import javax.crypto.interfaces.DHKey;

public class LarkyKeyPair implements StarlarkValue {

  enum KEY_TYPE {
    UNKNOWN("UNKNOWN"), RSA("RSA"), DSA("DSA"), EC("EC"), DH("DiffieHellman");

    private final String val;

    KEY_TYPE(String val) {
      this.val = val;
    }

    @Override
    public String toString() {
      return this.val;
    }
  }

  public static KEY_TYPE keyType(PrivateKey aPrivate) {
    if (aPrivate instanceof RSAKey) {
      return KEY_TYPE.RSA;
    } else if (aPrivate instanceof DSAKey) {
      return KEY_TYPE.DSA;
    } else if (aPrivate instanceof ECKey) {
      return KEY_TYPE.EC;
    } else if (aPrivate instanceof DHKey) {
      return KEY_TYPE.DH;
    } else {
      return KEY_TYPE.UNKNOWN;
    }
  }

  final private PrivateKey privateKey;
  final private PublicKey publicKey;
  final private KEY_TYPE type;

  public LarkyKeyPair(PublicKey aPublic, PrivateKey aPrivate, KEY_TYPE type) {
    this.privateKey = aPrivate;
    this.publicKey = aPublic;
    this.type = type;
  }

  public static LarkyKeyPair of(AsymmetricKeyParameter publicParam, AsymmetricKeyParameter privateParam, KEY_TYPE keyType) throws EvalException {
    final KeyFactory kf;
    try {
      kf = KeyFactory.getInstance(keyType.toString());
    } catch (NoSuchAlgorithmException e) {
      throw new EvalException(e);
    }
    KeySpec publicKeySpec = null, privateKeySpec = null;
    switch(keyType) {
      case RSA:
        publicKeySpec = new RSAPublicKeySpec(
          ((RSAKeyParameters)publicParam).getModulus(),
          ((RSAKeyParameters)publicParam).getExponent()
        );
        if(privateParam != null) {
          privateKeySpec = new RSAPrivateKeySpec(
            ((RSAPrivateCrtKeyParameters)privateParam).getModulus(),
            ((RSAPrivateCrtKeyParameters)privateParam).getExponent()
          );
        }
        break;
      case DSA:
      case EC:
      case DH:
      case UNKNOWN:
      default:
        throw Starlark.errorf("Not valid key type: %s", keyType);
    }
    try {
      PublicKey publicKey = kf.generatePublic(publicKeySpec);
      PrivateKey privateKey = null;
      if(privateKeySpec != null) {
        privateKey = kf.generatePrivate(privateKeySpec);
      }
      return new LarkyKeyPair(publicKey, privateKey, keyType);
    } catch (InvalidKeySpecException e) {
      throw new EvalException(e);
    }

  }
  public LarkyKeyPair(PublicKey aPublic, PrivateKey aPrivate) {
   this(aPublic, aPrivate, keyType(aPrivate));
  }

  public LarkyKeyPair(KeyPair kp) {
    this(kp.getPublic(), kp.getPrivate());
  }

  public PrivateKey getPrivateKey() {
    return privateKey;
  }

  public PublicKey getPublicKey() {
    return publicKey;
  }

  @StarlarkMethod(name="public_key")
  public StarlarkBytes publicKey() {
    return StarlarkBytes.immutableOf(getPublicKey().getEncoded());
  }

  @StarlarkMethod(name="private_key")
  public StarlarkBytes privateKey() {
    return loadPrivateKey();
  }

  @StarlarkMethod(name = "pkey", structField = true)
  public StarlarkBytes loadPrivateKey() {
    return StarlarkBytes.immutableOf(getPrivateKey().getEncoded());
  }

  @StarlarkMethod(name = "bits", structField = true)
  public StarlarkInt bits() throws EvalException {
    switch (type) {
      case RSA:
        return StarlarkInt.of(((RSAKey)getPrivateKey()).getModulus().bitLength());
      case DSA:
        return StarlarkInt.of(((DSAKey)getPrivateKey()).getParams().getP().bitLength());
      case EC:
        return StarlarkInt.of(((ECKey) getPrivateKey()).getParams().getCurve().getField().getFieldSize());
      case DH:
        return StarlarkInt.of(((DHKey)getPrivateKey()).getParams().getL());
      default:
        throw new EvalException("Unable to determine length in bits of specified Key instance");
    }
  }

  @StarlarkMethod(name = "key_type", structField = true)
  public String keytype() {
    return type.toString();
  }

}
