package com.verygood.security.larky.modules.crypto;

import com.google.common.flogger.FluentLogger;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
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
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateCrtKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMEncryptor;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.io.pem.PemObject;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

//import com.verygood.security.larky.modules.crypto.Util.PEMExportUtils;

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
    return Tuple.of(StarlarkInt.of(pq.getKey()), StarlarkInt.of(pq.getValue()));
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

  @StarlarkMethod(
      name = "import_key",
      parameters = {
          @Param(name = "externKey", allowedTypes = {
              @ParamType(type = StarlarkInt.class),
          }),
          @Param(name = "passPhrase", allowedTypes = {
              @ParamType(type = StarlarkInt.class)
          })
      }, useStarlarkThread = true)
  public Dict<String, StarlarkInt> RSAPEMDecode(String externKey, String passPhrase, StarlarkThread thread) throws EvalException {
    KeyPair rsaKey;
    try {
      rsaKey = RSA_ImportKey(externKey, passPhrase);
    } catch (SignatureException e) {
      throw new EvalException("ValueError: " + e.getMessage(), e);
    }
    RSAPrivateKey privateKey = (RSAPrivateKey) rsaKey.getPrivate();
    RSAPublicKey publicKey = (RSAPublicKey) rsaKey.getPublic();
    return Dict.<String, StarlarkInt>builder()
        .put("n", StarlarkInt.of(publicKey.getModulus()))
        .put("e", StarlarkInt.of(publicKey.getPublicExponent()))
        .put("d", StarlarkInt.of(privateKey.getPrivateExponent()))
        .build(thread.mutability());

  }

  public KeyPair RSA_ImportKey(String externKey, String passPhrase) throws SignatureException {
    StringReader stringReader = new StringReader(externKey);
    try (PEMParser pemParser = new PEMParser(stringReader)) {
      JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
      Object pemKeyPairObject = pemParser.readObject();
      return extractKeyPair(pemKeyPairObject, converter, passPhrase);
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

  @StarlarkMethod(
      name = "PKCS8_wrap", parameters = {
      @Param(name = "binary_key", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
      @Param(name = "oid", allowedTypes = {@ParamType(type = String.class)}),
      @Param(name = "passphrase", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
      @Param(name = "protection", allowedTypes = {@ParamType(type = String.class)})
  }, useStarlarkThread = true)
  public LarkyByteLike PKCS8_wrap(LarkyByteLike binaryKey, String oid, LarkyByteLike passphrase, String protection, StarlarkThread thread) throws EvalException {
    byte[] hello_world = new byte[]{
        (byte) 0x68, (byte) 0x65, (byte) 0x6c, (byte) 0x6c, (byte) 0x6f, //hello
        (byte) 0x20,
        (byte) 0x77, (byte) 0x6f, (byte) 0x72, (byte) 0x6c, (byte) 0x64 //world
    };
    return LarkyByte.builder(thread).setSequence(hello_world).build();
  }

  @StarlarkMethod(
      name = "PKCS8_unwrap", parameters = {
      @Param(name = "binary_key", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
      @Param(name = "oid", allowedTypes = {@ParamType(type = String.class)}),
      @Param(name = "passphrase", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
      @Param(name = "protection", allowedTypes = {@ParamType(type = String.class)})
  }, useStarlarkThread = true)
  public LarkyByteLike PKCS8_unwrap(LarkyByteLike binaryKey, String oid, LarkyByteLike passphrase, String protection, StarlarkThread thread) throws EvalException {
    byte[] hello_world = new byte[]{
        (byte) 0x68, (byte) 0x65, (byte) 0x6c, (byte) 0x6c, (byte) 0x6f, //hello
        (byte) 0x20,
        (byte) 0x77, (byte) 0x6f, (byte) 0x72, (byte) 0x6c, (byte) 0x64 //world
    };
    return LarkyByte.builder(thread).setSequence(hello_world).build();
  }

  @StarlarkMethod(
      name = "PEM_encode",
      doc = "Encode a piece of binary data into PEM format." +
          "\n" +
          "- data (byte string):\n" +
          "  The piece of binary data to encode.\n" +
          "\n" +
          "- marker (string):\n" +
          "  The marker for the PEM block (e.g. \"PUBLIC KEY\").\n" +
          "  Note that there is no official master list for all allowed markers.\n" +
          "  Still, you can refer to the OpenSSL_ source code.\n" +
          "\n" +
          "- passphrase (byte string):\n" +
          "  If given, the PEM block will be encrypted. The key is derived from\n" +
          "  the passphrase.\n" +
          "\n" +
          "- randfunc (callable):\n" +
          "  Random number generation function; it accepts an integer N and returns\n" +
          "  a byte string of random data, N bytes long. If not given, a new one is\n" +
          "  instantiated.",
      parameters = {
          @Param(name = "exportable", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
          @Param(name = "marker", allowedTypes = {@ParamType(type = String.class)}),
          @Param(name = "passphrase", allowedTypes = {
              @ParamType(type = LarkyByteLike.class), @ParamType(type = NoneType.class)}),
          @Param(name = "randfunc"),
      }, useStarlarkThread = true)
  public String PEM_encode(LarkyByteLike exportable, String marker, Object passphrase, Object randfunc, StarlarkThread thread) throws EvalException {

    /**
     * Note this PyCrypto comment:
     * - only supports 3DES for PEM encoding encryption (DES-EDE3-CBC)
     * - Encrypt with PKCS#7 padding
     */

    SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();

    X509EncodedKeySpec spec = new X509EncodedKeySpec(exportable.getBytes());
    ASN1InputStream bIn = new ASN1InputStream(new ByteArrayInputStream(spec.getEncoded()));
    KeyFactory kf;
    PublicKey publicKey;
    try {
      SubjectPublicKeyInfo pki = SubjectPublicKeyInfo.getInstance(bIn.readObject());
      String algOid = pki.getAlgorithm().getAlgorithm().getId();
      kf = KeyFactory.getInstance(algOid);
      publicKey = kf.generatePublic(spec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
      throw new EvalException(e.getMessage(), e);
    }

    StringWriter sWrt = new StringWriter();
    try (JcaPEMWriter pemWriter = new JcaPEMWriter(sWrt)) {
      PEMEncryptor encryptor = Starlark.isNullOrNone(passphrase)
        ? null
        : new JcePEMEncryptorBuilder(PKCS8Generator.PBE_SHA1_3DES.toString())
                  .setSecureRandom(secureRandom)
                  .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                  .build(((String)passphrase).toCharArray());
      JcaMiscPEMGenerator gen = new JcaMiscPEMGenerator(publicKey, encryptor);
      PemObject pemObject = gen.generate();
      pemWriter.writeObject(pemObject);
    } catch (IOException e) {
      throw new EvalException(e.getMessage(), e);
    }
    return sWrt.toString();
  }

  private AsymmetricCipherKeyPair keyPairFromKeyParts(Map<String, byte[]> keyParts) {
    BigInteger n = new BigInteger(keyParts.get("n"));
    BigInteger e = new BigInteger(keyParts.get("e"));
    BigInteger p = new BigInteger(keyParts.get("p"));
    BigInteger q = new BigInteger(keyParts.get("q"));
    BigInteger d = new BigInteger(keyParts.get("d"));

    return new AsymmetricCipherKeyPair(
        new RSAKeyParameters(false, n, e),
        new RSAPrivateCrtKeyParameters(
            n, e, d, p, q,
            d.remainder(p.subtract(BigInteger.ONE)),
            d.remainder(q.subtract(BigInteger.ONE)),
            BigIntegers.modOddInverse(p, q)));
  }


  @StarlarkMethod(
      name = "PEM_decode",
      doc = "Decode a PEM block into binary." +
          "\n" +
          "- pem_data (string):\n" +
          "  The PEM block.\n" +
          "\n" +
          "- passphrase (byte string):\n" +
          "  If given and the PEM block is encrypted,\n" +
          "  the key will be derived from the passphrase.\n",
      parameters = {
          @Param(name = "decodeable", allowedTypes = {@ParamType(type = String.class)}),
          @Param(name = "passphrase", allowedTypes = {
              @ParamType(type = LarkyByteLike.class), @ParamType(type = NoneType.class)}),
      }, useStarlarkThread = true)
  public Dict<String, Object> PEM_decode(String decodable, Object passphrase, StarlarkThread thread) throws EvalException {

    Map<String, byte[]> keyParts;
    try {
      Object pemObj = extractPEMObject(decodable.getBytes(StandardCharsets.UTF_8));
      keyParts = PEM_parse(pemObj, passphrase);
    } catch (PEMException e) {
      throw new EvalException(e.getMessage(), e);
    }
    /* We are going to return the actual imported key to shortcircuit a lot of the pycrypto
    * work */
//    Tuple.of(key.getAlgorithm(), key.getFormat(), )
     return Dict.<String, Object>builder()
         .put("n", StarlarkInt.of(new BigInteger(keyParts.get("n"))))
         .put("e", StarlarkInt.of(new BigInteger(keyParts.get("e"))))
         .put("algo", new String(keyParts.get("algo")))
         .put("d", StarlarkInt.of(new BigInteger(keyParts.get("d"))))
         .put("p", StarlarkInt.of(new BigInteger(keyParts.get("p"))))
         .put("q", StarlarkInt.of(new BigInteger(keyParts.get("q"))))
         .put("u", StarlarkInt.of(new BigInteger(keyParts.get("u"))))
         .build(thread.mutability());
  }

  private Map<String, byte[]> PEM_parse(Object obj, Object passphrase) throws EvalException, PEMException {
    JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
        .setProvider(BouncyCastleProvider.PROVIDER_NAME);
    Map<String, byte[]> returnVal = new HashMap<>();
    //
    // if not encrypted
      if (obj instanceof PEMKeyPair) {
        PEMKeyPair obj_ = (PEMKeyPair) obj;
        KeyPair keyPair = converter.getKeyPair(obj_);
        // TODO what happens if keyPair algorithm is not RSA?
        if(keyPair.getPublic().getAlgorithm().equals("RSA")) {
          RSAPublicKey rsaPublicKey = (RSAPublicKey) converter.getPublicKey(obj_.getPublicKeyInfo());
          returnVal.put("n",rsaPublicKey.getModulus().toByteArray());
          returnVal.put("e",rsaPublicKey.getPublicExponent().toByteArray());
          returnVal.put("algo", keyPair.getPublic().getAlgorithm().getBytes(StandardCharsets.UTF_8));
          BCRSAPrivateCrtKey bcKey = (BCRSAPrivateCrtKey) converter.getPrivateKey(obj_.getPrivateKeyInfo());
          returnVal.put("p", bcKey.getPrimeP().toByteArray());
          returnVal.put("q", bcKey.getPrimeQ().toByteArray());
          returnVal.put("u", bcKey.getPrimeP().modInverse(bcKey.getPrimeQ()).toByteArray());
          returnVal.put("crt", bcKey.getCrtCoefficient().toByteArray());
          returnVal.put("d", bcKey.getPrivateExponent().toByteArray());
          return returnVal;
        }
        throw Starlark.errorf("Unknown conversion algorithm for algo: %s", keyPair.getPublic().getAlgorithm());
      }
      else if(obj instanceof SubjectPublicKeyInfo) {

      }
//      else if (obj instanceof PrivateKeyInfo) {
//        key = converter.getPrivateKey((PrivateKeyInfo) obj);
//      }
//    // if encrypted
//    char[] passChars = Starlark.isNullOrNone(passphrase)
//                 ? null
//                 : ((String) passphrase).toCharArray();
//    if (obj instanceof PEMEncryptedKeyPair) {
//      PEMDecryptorProvider decryptor =
//          new JcePEMDecryptorProviderBuilder().build(passChars);
//      PEMKeyPair keypair = ((PEMEncryptedKeyPair) obj).decryptKeyPair(decryptor);
//      key = converter.getKeyPair(keypair).getPrivate();
//    } else if (obj instanceof PKCS8EncryptedPrivateKeyInfo) {
//      InputDecryptorProvider decryptor =
//          new JceOpenSSLPKCS8DecryptorProviderBuilder().build(passChars);
//      key = converter.getPrivateKey(((PKCS8EncryptedPrivateKeyInfo) obj).decryptPrivateKeyInfo(decryptor));
//    }


    return null;
  }

  private Object extractPEMObject(byte[] decodable) throws EvalException {
    InputStreamReader sr = new InputStreamReader(new ByteArrayInputStream(decodable));
    try (PEMParser parser = new PEMParser(sr)) {
      Object obj = parser.readObject();
      if(obj == null) {
        throw Starlark.errorf("Could not extract PEM encoded object!");
      }
      return obj;

    } catch (IOException e) {
      throw new EvalException(e.getMessage(), e);
    }
  }

  public static PrivateKey loadPrivateKey(Path privateKeyFile, String passPhrase) throws IOException {
    if (passPhrase == null) {
      passPhrase = "";
    }
    try (PEMParser r = new PEMParser(Files.newBufferedReader(privateKeyFile))) {
      Object pemObject = r.readObject();
      final JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);

      if (pemObject instanceof PEMEncryptedKeyPair) // PKCS#1 -----BEGIN RSA/DSA/EC PRIVATE KEY----- Proc-Type: 4,ENCRYPTED
      {
        final PEMEncryptedKeyPair ckp = (PEMEncryptedKeyPair) pemObject;
        final PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(passPhrase.toCharArray());
        return converter.getKeyPair(ckp.decryptKeyPair(decProv)).getPrivate();
      } else if (pemObject instanceof PKCS8EncryptedPrivateKeyInfo) // PKCS#8 -----BEGIN ENCRYPTED PRIVATE KEY-----
      {
        try {
          final PKCS8EncryptedPrivateKeyInfo encryptedInfo = (PKCS8EncryptedPrivateKeyInfo) pemObject;
          final InputDecryptorProvider provider = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(passPhrase.toCharArray());
          final PrivateKeyInfo privateKeyInfo = encryptedInfo.decryptPrivateKeyInfo(provider);
          return converter.getPrivateKey(privateKeyInfo);
        } catch (PKCSException | OperatorCreationException e) {
          throw new IOException("Unable to decrypt private key.", e);
        }
      } else if (pemObject instanceof PrivateKeyInfo) // PKCS#8 -----BEGIN PRIVATE KEY-----
      {
        return converter.getPrivateKey((PrivateKeyInfo) pemObject);
      } else if (pemObject instanceof PEMKeyPair) //// PKCS#1 -----BEGIN RSA/DSA/EC PRIVATE KEY-----
      {
        return converter.getKeyPair((PEMKeyPair) pemObject).getPrivate();
      } else {
        throw new IOException("Unrecognized private key format.");
      }
    }
  }

  @StarlarkMethod(
      name = "export_key",
      doc = "Export an RSA key." +
          "" +
          "format (string):\n" +
          "  The format to use for wrapping the key:\n" +
          "\n" +
          "  - *'PEM'*. (*Default*) Text encoding, done according to `RFC1421`_/`RFC1423`_.\n" +
          "  - *'DER'*. Binary encoding.\n" +
          "  - *'OpenSSH'*. Textual encoding, done according to OpenSSH specification.\n" +
          "    Only suitable for public keys (not private keys).\n" +
          "\n" +
          "passphrase (string):\n" +
          "  (*For private keys only*) The pass phrase used for protecting the output.\n" +
          "\n" +
          "pkcs (integer):\n" +
          "  (*For private keys only*) The ASN.1 structure to use for\n" +
          "  serializing the key. Note that even in case of PEM\n" +
          "  encoding, there is an inner ASN.1 DER structure.\n" +
          "\n" +
          "  With ``pkcs=1`` (*default*), the private key is encoded in a\n" +
          "  simple `PKCS#1`_ structure (``RSAPrivateKey``).\n" +
          "\n" +
          "  With ``pkcs=8``, the private key is encoded in a `PKCS#8`_ structure\n" +
          "  (``PrivateKeyInfo``).\n" +
          "\n" +
          "  .. note::\n" +
          "      This parameter is ignored for a public key.\n" +
          "      For DER and PEM, an ASN.1 DER ``SubjectPublicKeyInfo``\n" +
          "      structure is always used.\n" +
          "\n" +
          "protection (string):\n" +
          "  (*For private keys only*)\n" +
          "  The encryption scheme to use for protecting the private key.\n" +
          "\n" +
          "  If ``None`` (default), the behavior depends on :attr:`format`:\n" +
          "\n" +
          "  - For *'DER'*, the *PBKDF2WithHMAC-SHA1AndDES-EDE3-CBC*\n" +
          "    scheme is used. The following operations are performed:\n" +
          "\n" +
          "      1. A 16 byte Triple DES key is derived from the passphrase\n" +
          "         using :func:`Crypto.Protocol.KDF.PBKDF2` with 8 bytes salt,\n" +
          "         and 1 000 iterations of :mod:`Crypto.Hash.HMAC`.\n" +
          "      2. The private key is encrypted using CBC.\n" +
          "      3. The encrypted key is encoded according to PKCS#8.\n" +
          "\n" +
          "  - For *'PEM'*, the obsolete PEM encryption scheme is used.\n" +
          "    It is based on MD5 for key derivation, and Triple DES for encryption.\n" +
          "\n" +
          "  Specifying a value for :attr:`protection` is only meaningful for PKCS#8\n" +
          "  (that is, ``pkcs=8``) and only if a pass phrase is present too.\n" +
          "\n" +
          "  The supported schemes for PKCS#8 are listed in the\n" +
          "  :mod:`Crypto.IO.PKCS8` module (see :attr:`wrap_algo` parameter).\n" +
          "\n" +
          "randfunc (callable):\n" +
          "  A function that provides random bytes. Only used for PEM encoding.\n" +
          "  The default is :func:`Crypto.Random.get_random_bytes`.",
      parameters = {
          @Param(name = "exportable", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
          @Param(name = "format", allowedTypes = {@ParamType(type = String.class)}),
          @Param(name = "passPhrase", allowedTypes = {
              @ParamType(type = String.class), @ParamType(type = NoneType.class)}),
          @Param(name = "pkcs1", allowedTypes = {@ParamType(type = StarlarkInt.class)},
              defaultValue = "1"),
          @Param(name = "randfunc", defaultValue = "None"),
      }, useStarlarkThread = true)
  public String RSAExportKey(LarkyByteLike exportable, String format, String passPhrase, String pkcs1, Object randfunc, StarlarkThread thread) throws EvalException {

//    SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(PublicKeyFactory.createKey(keyPair.getPublic()
//           .getEncoded()));
    return "";
  }

//    public KeyPair RSA_ExportKey(PrivateKey privateKey, String passPhrase) throws SignatureException {
//    StringWriter outputStream = new StringWriter();
//
//
//      try (JcaPEMWriter pemWriter = new JcaPEMWriter(outputStream)) {
//        new JcePEMEncryptorBuilder("AES-256-CBC").setProvider("BCFIPS").build(passwd));
//        JcaPKCS8Generator gen = new JcaPKCS8Generator(privateKey, null);
//        pemWriter.writeObject(privateKey,
//
//             pemWriter.close();
//
//             return sWrt.toString();
//        return keyPair;
//      } catch (IOException e) {
//        throw new SignatureException("Unable to parse RSA private key", e);
//      } catch (IllegalArgumentException | NullPointerException | DecoderException e) {
//        throw new SignatureException("Unable to parse RSA private key. Input is malformed", e);
//      }
//    }

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
