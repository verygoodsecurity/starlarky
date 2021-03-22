package com.verygood.security.larky.modules;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.util.encoders.Base64;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;


@StarlarkBuiltin(
    name = "rsakey",
    category = "BUILTIN",
    doc = "This module provides RSA keypair generation")
public class RSAKeyModule implements StarlarkValue {

  public static final RSAKeyModule INSTANCE = new RSAKeyModule();

  @StarlarkMethod(
      name = "publicKeyFromPrivateKey",
      doc = "Get public key from RSA private key",
      parameters = {
          @Param(name = "privateKey", doc = "RSA Private key pem string")
      })
  public PublicKey publicKeyFromPrivateKey(String pemPrivateKey) throws
          InvalidKeySpecException,
          NoSuchAlgorithmException
  {
    PrivateKey privateKey = readRsaPrivateKey(pemPrivateKey);
    RSAPrivateCrtKey privateCrtKey = (RSAPrivateCrtKey)privateKey;

    RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(privateCrtKey.getModulus(), privateCrtKey.getPublicExponent());
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    return keyFactory.generatePublic(publicKeySpec);
  }

  private static PrivateKey readRsaPrivateKey(String pemPrivateKey) throws
          java.security.NoSuchAlgorithmException,
          java.security.spec.InvalidKeySpecException
  {
    pemPrivateKey = pemPrivateKey.replace("-----BEGIN RSA PRIVATE KEY-----\n", "");
    pemPrivateKey = pemPrivateKey.replace("-----END RSA PRIVATE KEY-----", "");

    byte[] decoded = Base64.decode(pemPrivateKey);

    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    return keyFactory.generatePrivate(keySpec);
  }

}
