package com.verygood.security.larky.modules.crypto;

import com.google.common.flogger.FluentLogger;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.RSABlindedEngine;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.util.encoders.DecoderException;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;

public class CryptoPublicKeyModule implements StarlarkValue {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  public static final CryptoPublicKeyModule INSTANCE = new CryptoPublicKeyModule();

  @StarlarkMethod(name = "RSA", structField = true)
  public CryptoPublicKeyModule RSA() {
    return CryptoPublicKeyModule.INSTANCE;
  }


  @StarlarkMethod(name = "generate", parameters = {@Param(name = "bits"), @Param(name = "e")}, useStarlarkThread = true)
  public Dict<String, StarlarkInt> RSA_generate(StarlarkInt bits_, StarlarkInt e_, StarlarkThread thread) throws EvalException {
    BigInteger e = Starlark.isNullOrNone(e_) ? BigInteger.valueOf(65537) : e_.toBigInteger();
    int bits = bits_.toIntUnchecked();
    if (bits != 1024 && bits != 2048 && bits != 3072 && bits != 4096) {
      throw Starlark.errorf("Odd bit size: expected 1024, 2048, 3072, or 4096. Received %d", bits);
    }
    SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();
    RSAKeyPairGenerator rsaKeyPairGenerator = new RSAKeyPairGenerator();
    RSAKeyGenerationParameters rsaKeyGenerationParameters = new RSAKeyGenerationParameters(e, secureRandom, bits, 100);
    rsaKeyPairGenerator.init(rsaKeyGenerationParameters);
    AsymmetricCipherKeyPair asymKeyPair = rsaKeyPairGenerator.generateKeyPair();
    /*
       n : integer
            The modulus.
          e : integer
            The public exponent.
          d : integer
            The private exponent. Only required for private keys.
          p : integer
            The first factor of the modulus. Only required for private keys.
          q : integer
            The second factor of the modulus. Only required for private keys.
          u : integer
            The CRT coefficient (inverse of p modulo q). Only required for
            private keys.
     */
    RSAKeyParameters pubKey = ((RSAKeyParameters) asymKeyPair.getPublic());
    RSAPrivateCrtKeyParameters privateKey = ((RSAPrivateCrtKeyParameters) asymKeyPair.getPrivate());
    return Dict.<String, StarlarkInt>builder()
        .put("n", StarlarkInt.of(pubKey.getModulus()))
        .put("e", StarlarkInt.of(pubKey.getExponent()))
        .put("d", StarlarkInt.of(privateKey.getExponent()))
        .put("p", StarlarkInt.of(privateKey.getP()))
        .put("q", StarlarkInt.of(privateKey.getQ()))
        .put("u", StarlarkInt.of((privateKey.getP().modInverse(privateKey.getQ()))))
        .build(thread.mutability());
  }

  public void RSA_construct(BigInteger n, BigInteger e, BigInteger d, BigInteger p, BigInteger q, BigInteger qInv) {
    BigInteger pSub1 = p.subtract(BigInteger.ONE);
    BigInteger qSub1 = q.subtract(BigInteger.ONE);
    BigInteger dP = d.remainder(pSub1);
    BigInteger dQ = d.remainder(qSub1);
    //qInv = BigIntegers.modOddInverse(p, q);
    new AsymmetricCipherKeyPair(
        new RSAKeyParameters(false, n, e),
        new RSAPrivateCrtKeyParameters(n, e, d, p, q, dP, dQ, qInv));
  }

  public RSAPrivateKey RSA_import_key(String externKey, String passPhrase) throws SignatureException {
    StringReader stringReader = new StringReader(externKey);
    try (PEMParser pemParser = new PEMParser(stringReader)) {
      JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
      Object pemKeyPairObject = pemParser.readObject();
      KeyPair keyPair = extractKeyPair(pemKeyPairObject, converter, passPhrase);
      return (RSAPrivateKey) keyPair.getPrivate();
    } catch (IOException e) {
      throw new SignatureException("Unable to parse RSA private key", e);
    } catch (IllegalArgumentException | NullPointerException | DecoderException e) {
      throw new SignatureException("Unable to parse RSA private key. Input is malformed", e);
    }
  }

  private KeyPair extractKeyPair(Object pemKeyPairObject, JcaPEMKeyConverter converter, String passPhrase) throws IOException, SignatureException {
    if (pemKeyPairObject instanceof SubjectPublicKeyInfo) {
      throw new SignatureException("Input is an RSA Public Key, but private key is expected");
    }
    if (pemKeyPairObject instanceof PEMEncryptedKeyPair) {
      logger.atInfo().log("Encrypted key - using a provided password");
      PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(passPhrase.toCharArray());
      return converter.getKeyPair(((PEMEncryptedKeyPair) pemKeyPairObject).decryptKeyPair(decProv));
    }
    logger.atInfo().log("Unencrypted key - no password needed");
    return converter.getKeyPair((PEMKeyPair) pemKeyPairObject);
  }

  public Dict<String, StarlarkInt> RSA_public_key(Dict<String, StarlarkInt> finalRsaObj) {
    return null;
  }

  @StarlarkMethod(
      name = "decrypt",
      parameters = {
          @Param(name = "rsaObj", allowedTypes = {@ParamType(type = Dict.class)}),
          @Param(name = "ciphertext")
      },
      useStarlarkThread = true
  )
  public LarkyByteLike RSADecrypt(Dict<String, StarlarkInt> finalRsaObj, LarkyByteLike cT, StarlarkThread thread) throws EvalException {
    byte[] cipherText = cT.getBytes();
    byte[] bytes;
    try {
      bytes = RSA_decrypt(finalRsaObj, cipherText);
    } catch (InvalidCipherTextException e) {
      throw new EvalException(e);
    }
    return LarkyByte.builder(thread).setSequence(bytes).build();
  }

  @VisibleForTesting
  byte[] RSA_decrypt(Dict<String, StarlarkInt> finalRsaObj, byte[] cipherText) throws InvalidCipherTextException {
    RSAKeyParameters privParams = new RSAKeyParameters(
        true,
        finalRsaObj.get("n").toBigInteger(),
        finalRsaObj.get("d").toBigInteger());

    AsymmetricBlockCipher rsaEngine = new RSABlindedEngine();
    rsaEngine.init(false, privParams);
    byte[] bytes;
    bytes = rsaEngine.processBlock(cipherText, 0, cipherText.length);
    return bytes;
  }

  @StarlarkMethod(
      name = "encrypt",
      parameters = {
          @Param(name = "rsaObj", allowedTypes = {@ParamType(type = Dict.class)}),
          @Param(name = "plaintext")
      },
      useStarlarkThread = true
  )
  public LarkyByteLike RSAEncrypt(Dict<String, StarlarkInt> finalRsaObj, LarkyByteLike pT, StarlarkThread thread) throws EvalException {
    byte[] plainText = pT.getBytes();
    byte[] bytes;
    try {
      bytes = RSA_encrypt(finalRsaObj, plainText);
    } catch (InvalidCipherTextException e) {
      throw new EvalException(e);
    }
    return LarkyByte.builder(thread).setSequence(bytes).build();
  }

  @VisibleForTesting
  byte[] RSA_encrypt(Dict<String, StarlarkInt> finalRsaObj, byte[] plainText) throws InvalidCipherTextException {
    RSAKeyParameters pubParams = new RSAKeyParameters(
        false,
        finalRsaObj.get("n").toBigInteger(),
        finalRsaObj.get("e").toBigInteger());
    AsymmetricBlockCipher rsaEngine = new RSABlindedEngine();

    rsaEngine.init(true, pubParams);
    byte[] bytes;
    bytes = rsaEngine.processBlock(plainText, 0, plainText.length);
    return bytes;
  }
}