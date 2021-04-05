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
import net.starlark.java.eval.Tuple;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.DataLengthException;
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
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.DecoderException;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.util.AbstractMap;
import java.util.Map;

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



  @StarlarkMethod(
      name = "compute_factors", parameters = {
      @Param(name = "n", allowedTypes = {
          @ParamType(type = StarlarkInt.class),
      }),
      @Param(name = "e", allowedTypes = {
          @ParamType(type = StarlarkInt.class)
      }),
      @Param(name = "d", allowedTypes = {
          @ParamType(type = StarlarkInt.class)
      })
  }
  )
  public Tuple computePrimeFactors(StarlarkInt n, StarlarkInt e, StarlarkInt d) throws EvalException {
    Map.Entry<BigInteger, BigInteger> pq = probabilisticPrimeFactorization(
        n.toBigInteger(),
        e.toBigInteger(),
        d.toBigInteger());
    return Tuple.of(pq.getKey(), pq.getValue());
  }

  /**
   * C.1 Probabilistic Prime-Factor Recovery The following algorithm recovers the prime factors of a
   * modulus, given the public and private exponents. The algorithm is based on Fact 1 in [Boneh
   * 1999]
   *
   * https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-56Br2.pdf
   */
  public Map.Entry<BigInteger, BigInteger> probabilisticPrimeFactorization(BigInteger n, BigInteger e, BigInteger d) throws EvalException {
    BigInteger p, q;
    // Step 1: Let k = de – 1. If k is odd, then go to Step 4
    BigInteger k = d.multiply(e).subtract(BigIntegers.ONE);
    boolean isEven = !k.testBit(0);
    if (isEven) {
      // Step 2 (express k as (2^t)r, where r is the largest odd integer
      // dividing k and t >= 1)
      BigInteger r = k;
      BigInteger t = BigIntegers.ZERO;

      do {
        r = r.divide(BigIntegers.TWO);
        t = t.add(BigIntegers.ONE);
      } while (!r.testBit(0));

      // Step 3
      SecureRandom random = CryptoServicesRegistrar.getSecureRandom();
      boolean success = false;
      BigInteger y = null;

      step3loop:
      for (int i = 1; i <= 100; i++) {

        // 3a
        BigInteger g = BigIntegers.createRandomBigInteger(n.bitLength(), random);

        // 3b
        y = g.modPow(r, n);

        // 3c
        if (y.equals(BigIntegers.ONE) || y.equals(n.subtract(BigIntegers.ONE))) {
          // 3g
          continue;
        }

        // 3d
        for (BigInteger j = BigIntegers.ONE; j.compareTo(t) <= 0; j = j.add(BigIntegers.ONE)) {
          // 3d1
          BigInteger x = y.modPow(BigIntegers.TWO, n);

          // 3d2
          if (x.equals(BigIntegers.ONE)) {
            success = true;
            break step3loop;
          }

          // 3d3
          if (x.equals(n.subtract(BigIntegers.ONE))) {
            // 3g
            continue step3loop;
          }
          // 3d4
          y = x;
        }

        // 3e
        BigInteger x = y.modPow(BigIntegers.TWO, n);
        if (x.equals(BigIntegers.ONE)) {
          success = true;
          break;
        }

        // 3g
        // (loop again)
      }

      if (success) {
        // Step 5
        p = y.subtract(BigIntegers.ONE).gcd(n);
        q = n.divide(p);
        return new AbstractMap.SimpleEntry<>(p, q);
      }
    }
    // Step 4
    throw Starlark.errorf("ValueError: Unable to compute factors p and q from exponent d.");
  }

//  @StarlarkMethod(
//      name = "construct",
//      parameters = {
//          @Param(name="n", allowedTypes = {
//              @ParamType(type = StarlarkInt.class),
//          }),
//          @Param(name = "e", allowedTypes = {
//              @ParamType(type = StarlarkInt.class)
//          }),
//          @Param(name = "d", allowedTypes = {
//              @ParamType(type = StarlarkInt.class),
//              @ParamType(type = NoneType.class)
//          }, defaultValue = "None"),
//          @Param(name = "p", allowedTypes = {
//            @ParamType(type = StarlarkInt.class),
//              @ParamType(type = NoneType.class)
//          }, defaultValue = "None"),
//          @Param(name = "q", allowedTypes = {
//              @ParamType(type = StarlarkInt.class),
//              @ParamType(type = NoneType.class)
//          }, defaultValue = "None"),
//          @Param(name = "u", allowedTypes = {
//              @ParamType(type = StarlarkInt.class),
//              @ParamType(type = NoneType.class)
//          }, defaultValue = "None")
//  })
//  public Dict<String, StarlarkInt> RSAConstruct(StarlarkInt n, StarlarkInt e, Object d, Object p, Object q, Object crtCoefficient) throws EvalException {
//    if(!Starlark.isNullOrNone(p) && !Starlark.isNullOrNone(q)) {
//      throw Starlark.errorf("Both p & q must be present if one or the other is present");
//    }
//    boolean isPublic = Starlark.isNullOrNone(d);
//    if(isPublic) {
//
//    }
//
//    BigInteger pSub1 = p.subtract(BigInteger.ONE);
//    BigInteger qSub1 = q.subtract(BigInteger.ONE);
//    BigInteger dP = d.remainder(pSub1);
//    BigInteger dQ = d.remainder(qSub1);
//    BigInteger qInv = BigIntegers.modOddInverse(p, q);
//    new AsymmetricCipherKeyPair(
//        new RSAKeyParameters(false, n, e),
//        new RSAPrivateCrtKeyParameters(n, e, d, p, q, dP, dQ, qInv));
//  }

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
    } catch (DataLengthException | InvalidCipherTextException e) {
      throw new EvalException("ValueError: " + e.getMessage(), e);
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
    } catch (DataLengthException | InvalidCipherTextException e) {
      throw new EvalException("ValueError: " + e.getMessage(), e);
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