package com.verygood.security.larky.modules.crypto.Util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.hash.HashCode;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.util.DigestFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateCrtKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.AbstractMap;
import java.util.Map;


/**
 * Utilities for ssh key pairs
 *
 * @see <a href=
 *      "http://stackoverflow.com/questions/3706177/how-to-generate-ssh-compatible-id-rsa-pub-from-java"
 *      />
 */
public final class CryptoUtils {

  private CryptoUtils() {
  }

   /**
    * Executes {@link #publicKeySpecFromOpenSSH(ByteSource)} on the string which was OpenSSH
    * Base64 Encoded {@code id_rsa.pub}
    *
    * @param idRsaPub
    *           formatted {@code ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAB...}
    */
   public static RSAPublicKeySpec publicKeySpecFromOpenSSH(String idRsaPub) {
     return publicKeySpecFromOpenSSH(ByteSource.wrap(idRsaPub.getBytes(StandardCharsets.UTF_8)));
   }

   /**
    * Returns {@link RSAPublicKeySpec} which was OpenSSH Base64 Encoded {@code id_rsa.pub}
    *
    * @param supplier
    *           the input stream factory, formatted {@code ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAB...}
    *
    * @return the {@link RSAPublicKeySpec} which was OpenSSH Base64 Encoded {@code id_rsa.pub}
    * @throws IOException
    *            if an I/O error occurs
    */
   public static RSAPublicKeySpec publicKeySpecFromOpenSSH(ByteSource supplier) {
     BigInteger publicExponent;
     BigInteger modulus;
     String s;
     try {
       try (InputStream stream = supplier.openStream()) {
         s = CharStreams.toString(new InputStreamReader(stream, StandardCharsets.UTF_8));
       }
       Iterable<String> parts = Splitter.on(' ').split(s.trim());
       checkArgument(
           Iterables.size(parts) >= 2 && "ssh-rsa".equals(Iterables.get(parts, 0)),
           "bad format, should be: ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAB...");
       try (InputStream stream =
                new ByteArrayInputStream(
                    BaseEncoding.base64().decode(Iterables.get(parts, 1)))) {
         String marker = new String(readLengthFirst(stream));
         checkArgument(
             "ssh-rsa".equals(marker),
             "looking for marker ssh-rsa but got %s", marker);
         publicExponent = new BigInteger(readLengthFirst(stream));
         modulus = new BigInteger(readLengthFirst(stream));
       }
     } catch (IOException e) {
       throw new RuntimeException(e);
     }
     return new RSAPublicKeySpec(modulus, publicExponent);
   }

   // http://www.ietf.org/rfc/rfc4253.txt
   private static byte[] readLengthFirst(InputStream in) throws IOException {
      int byte1 = in.read();
      int byte2 = in.read();
      int byte3 = in.read();
      int byte4 = in.read();
      int length = (byte1 << 24) + (byte2 << 16) + (byte3 << 8) + (byte4);
      byte[] val = new byte[length];
     //noinspection UnstableApiUsage
     ByteStreams.readFully(in, val);
      return val;
   }

   /**
    *
    * @param generator
    *           to generate RSA key pairs
    * @param rand
    *           for initializing {@code generator}
    * @return new 2048 bit keyPair
    */
   public static AsymmetricCipherKeyPair generateRsaKeyPair(
       AsymmetricCipherKeyPairGenerator generator,
       SecureRandom rand) {
     return generateRsaKeyPair(generator, rand, 2048);
   }

  public static AsymmetricCipherKeyPair generateRsaKeyPair(AsymmetricCipherKeyPairGenerator generator, SecureRandom rand, int bitsize) {
    BigInteger publicExponent = BigInteger.valueOf(65537);
    RSAKeyGenerationParameters rsaKeyGenerationParameters = new RSAKeyGenerationParameters(
        publicExponent, rand, bitsize, 100);
    generator.init(rsaKeyGenerationParameters);
    AsymmetricCipherKeyPair asymKeyPair = generator.generateKeyPair();
    //RSAKeyParameters pubKey = ((RSAKeyParameters) asymKeyPair.getPublic());
    //RSAPrivateCrtKeyParameters privateKey = ((RSAPrivateCrtKeyParameters) asymKeyPair.getPrivate());
    return asymKeyPair;
  }

  /**
    * return a "public" -> rsa public key, "private" -> its corresponding private key
    */
   public static Map<String, String> generate() {
     RSAKeyPairGenerator rsaKeyPairGenerator = new RSAKeyPairGenerator();
     return generate(rsaKeyPairGenerator, new SecureRandom());
   }


 public static Map<String, String> generate(AsymmetricCipherKeyPairGenerator generator, SecureRandom rand) {
    AsymmetricCipherKeyPair pair = generateRsaKeyPair(generator, rand);
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    builder.put("public", encodeAsOpenSSH((RSAKeyParameters) pair.getPublic()));
    builder.put("private", pem((RSAPrivateCrtKeyParameters) pair.getPrivate()));
    return builder.build();
 }

  public static String pem(RSAPrivateCrtKeyParameters aPrivate) {
    StringWriter sWrt = new StringWriter();
    try (JcaPEMWriter pemWriter = new JcaPEMWriter(sWrt)) {
      JcaMiscPEMGenerator gen = new JcaMiscPEMGenerator(
          PrivateKeyInfoFactory.createPrivateKeyInfo(aPrivate),
          null);
      PemObject pemObject = gen.generate();
      pemWriter.writeObject(pemObject);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return sWrt.toString();
  }

  public static String encodeAsOpenSSH(RSAKeyParameters key) {
      byte[] keyBlob = keyBlob(key.getExponent(), key.getModulus());
      return "ssh-rsa " + BaseEncoding.base64().encode(keyBlob);
   }


   /**
    * @param privateKeyPEM
    *           RSA private key in PEM format
    * @param publicKeyOpenSSH
    *           RSA public key in OpenSSH format
    * @return true if the keypairs match
    */
   public static boolean privateKeyMatchesPublicKey(String privateKeyPEM, String publicKeyOpenSSH) {
      KeySpec privateKeySpec = privateKeySpec(privateKeyPEM);

//     OpenSSHPublicKeyUtil.encodePublicKey()
      /*
           * OpenSSHPrivateKeySpec privSpec = new OpenSSHPrivateKeySpec(rawPriv);
     * <p>
     * KeyFactory kpf = KeyFactory.getInstance("RSA", "BC");
     * PrivateKey prk = kpf.generatePrivate(privSpec);
     * <p>
     * OpenSSHPrivateKeySpec rcPrivateSpec = kpf.getKeySpec(prk, OpenSSHPrivateKeySpec.class);
       */
      checkArgument(privateKeySpec instanceof RSAPrivateCrtKeySpec,
               "incorrect format expected RSAPrivateCrtKeySpec was %s", privateKeySpec);
     KeyFactory kf;
     PrivateKey prk;
     RSAPrivateCrtKeySpec keySpec;
     try {
       kf = KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
       prk = kf.generatePrivate(privateKeySpec);
       keySpec = kf.getKeySpec(prk, RSAPrivateCrtKeySpec.class);
     } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
       throw new RuntimeException(e);
     }
     return privateKeyMatchesPublicKey(keySpec, publicKeySpecFromOpenSSH(publicKeyOpenSSH));
   }
/*
 KeyFactory kf;
   PrivateKey prk;
   RSAPrivateCrtKeySpec keySpec;
   try {
     kf = KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
     prk = kf.generatePrivate(privateKeySpec);
     keySpec = kf.getKeySpec(prk, RSAPrivateCrtKeySpec.class);
   } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
     throw new RuntimeException(e);
   }
 */
  public static KeySpec privateKeySpec(String privateKeyPEM) {
    Object pemObj = extractPEMObject(privateKeyPEM.getBytes(StandardCharsets.UTF_8));
    KeyFactory kf;
    PrivateKey prk;
    RSAPrivateCrtKeySpec keySpec;
    try {
      BCRSAPrivateCrtKey pk = (BCRSAPrivateCrtKey) CryptoUtils.loadPrivateKey(pemObj, null);
      kf = KeyFactory.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
      prk = kf.generatePrivate(new PKCS8EncodedKeySpec(pk.getEncoded()));
      keySpec = kf.getKeySpec(prk, RSAPrivateCrtKeySpec.class);
    } catch (NoSuchAlgorithmException
        | NoSuchProviderException
        | InvalidKeySpecException
        | IOException e) {
      throw new RuntimeException(e);
    }
    return keySpec;

  }

  /**
    * @return true if the keypairs match
    */
   public static boolean privateKeyMatchesPublicKey(RSAPrivateCrtKeySpec privateKey, RSAPublicKeySpec publicKey) {
      return privateKey.getPublicExponent().equals(publicKey.getPublicExponent())
               && privateKey.getModulus().equals(publicKey.getModulus());
   }

   /**
    * @return true if the keypair has the same fingerprint as supplied
    */
   public static boolean privateKeyHasFingerprint(RSAPrivateCrtKeySpec privateKey, String fingerprint) {
      return fingerprint(privateKey.getPublicExponent(), privateKey.getModulus()).equals(fingerprint);
   }

   /**
    * @param privateKeyPEM
    *           RSA private key in PEM format
    * @param fingerprint
    *           ex. {@code 2b:a9:62:95:5b:8b:1d:61:e0:92:f7:03:10:e9:db:d9}
    * @return true if the keypair has the same fingerprint as supplied
    */
   public static boolean privateKeyHasFingerprint(String privateKeyPEM, String fingerprint) {
      KeySpec privateKeySpec = privateKeySpec(privateKeyPEM);
      checkArgument(privateKeySpec instanceof RSAPrivateCrtKeySpec,
               "incorrect format expected RSAPrivateCrtKeySpec was %s", privateKeySpec);
      return privateKeyHasFingerprint((RSAPrivateCrtKeySpec) privateKeySpec, fingerprint);
   }

   /**
    * @param privateKeyPEM
    *           RSA private key in PEM format
    * @return fingerprint ex. {@code 2b:a9:62:95:5b:8b:1d:61:e0:92:f7:03:10:e9:db:d9}
    */
   public static String fingerprintPrivateKey(String privateKeyPEM) {
      KeySpec privateKeySpec = privateKeySpec(privateKeyPEM);
      checkArgument(privateKeySpec instanceof RSAPrivateCrtKeySpec,
               "incorrect format expected RSAPrivateCrtKeySpec was %s", privateKeySpec);
      RSAPrivateCrtKeySpec certKeySpec = (RSAPrivateCrtKeySpec) privateKeySpec;
      return fingerprint(certKeySpec.getPublicExponent(), certKeySpec.getModulus());
   }

   /**
    * @param publicKeyOpenSSH
    *           RSA public key in OpenSSH format
    * @return fingerprint ex. {@code 2b:a9:62:95:5b:8b:1d:61:e0:92:f7:03:10:e9:db:d9}
    */
   public static String fingerprintPublicKey(String publicKeyOpenSSH) {
      RSAPublicKeySpec publicKeySpec = publicKeySpecFromOpenSSH(publicKeyOpenSSH);
      return fingerprint(publicKeySpec.getPublicExponent(), publicKeySpec.getModulus());
   }

   /**
    * @return true if the keypair has the same SHA1 fingerprint as supplied
    */
   public static boolean privateKeyHasSha1(RSAPrivateCrtKeySpec privateKey, String fingerprint) {
      return sha1(privateKey).equals(fingerprint);
   }

   /**
    * @param privateKeyPEM
    *           RSA private key in PEM format
    * @param sha1HexColonDelimited
    *           ex. {@code 2b:a9:62:95:5b:8b:1d:61:e0:92:f7:03:10:e9:db:d9}
    * @return true if the keypair has the same fingerprint as supplied
    */
   public static boolean privateKeyHasSha1(String privateKeyPEM, String sha1HexColonDelimited) {
      KeySpec privateKeySpec = privateKeySpec(privateKeyPEM);
      checkArgument(privateKeySpec instanceof RSAPrivateCrtKeySpec,
               "incorrect format expected RSAPrivateCrtKeySpec was %s", privateKeySpec);
      return privateKeyHasSha1(RSAPrivateCrtKeySpec.class.cast(privateKeySpec), sha1HexColonDelimited);
   }

   /**
    * @param privateKeyPEM
    *           RSA private key in PEM format
    * @return sha1HexColonDelimited ex. {@code 2b:a9:62:95:5b:8b:1d:61:e0:92:f7:03:10:e9:db:d9}
    */
   public static String sha1PrivateKey(String privateKeyPEM) {
      KeySpec privateKeySpec = privateKeySpec(privateKeyPEM);
      checkArgument(privateKeySpec instanceof RSAPrivateCrtKeySpec,
               "incorrect format expected RSAPrivateCrtKeySpec was %s", privateKeySpec);
      RSAPrivateCrtKeySpec certKeySpec = (RSAPrivateCrtKeySpec) privateKeySpec;
      return sha1(certKeySpec);
   }

   /**
    * Create a SHA-1 digest of the DER encoded private key.
    *
    * @param privateKey
    *
    * @return hex sha1HexColonDelimited ex. {@code 2b:a9:62:95:5b:8b:1d:61:e0:92:f7:03:10:e9:db:d9}
    */
   public static String sha1(RSAPrivateCrtKeySpec privateKey) {
    try {
      byte[] encodedKey = KeyFactory.getInstance("RSA")
           .generatePrivate(privateKey)
           .getEncoded();
      Digest sha1 = DigestFactory.createSHA1();
      sha1.update(encodedKey, 0, encodedKey.length);
      byte[] bytes = new byte[sha1.getDigestSize()];
      sha1.doFinal(bytes, 0);
      //noinspection UnstableApiUsage
      return hexColonDelimited(HashCode.fromBytes(bytes));
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
       throw new RuntimeException(e);
    }
   }

   /**
    * @return true if the keypair has the same fingerprint as supplied
    */
   public static boolean publicKeyHasFingerprint(RSAPublicKeySpec publicKey, String fingerprint) {
     return fingerprint(publicKey.getPublicExponent(), publicKey.getModulus()).equals(fingerprint);
   }

   /**
    * @param publicKeyOpenSSH
    *           RSA public key in OpenSSH format
    * @param fingerprint
    *           ex. {@code 2b:a9:62:95:5b:8b:1d:61:e0:92:f7:03:10:e9:db:d9}
    * @return true if the keypair has the same fingerprint as supplied
    */
   public static boolean publicKeyHasFingerprint(String publicKeyOpenSSH, String fingerprint) {
      return publicKeyHasFingerprint(publicKeySpecFromOpenSSH(publicKeyOpenSSH), fingerprint);
   }

   /**
    * Create a fingerprint per the following <a
    * href="http://tools.ietf.org/html/draft-friedl-secsh-fingerprint-00" >spec</a>
    *
    * @param publicExponent
    * @param modulus
    *
    * @return hex fingerprint ex. {@code 2b:a9:62:95:5b:8b:1d:61:e0:92:f7:03:10:e9:db:d9}
    */
   public static String fingerprint(BigInteger publicExponent, BigInteger modulus) {
     byte[] keyBlob = keyBlob(publicExponent, modulus);
     Digest md5 = DigestFactory.createMD5();
     md5.update(keyBlob, 0, keyBlob.length);
     byte[] bytes = new byte[md5.getDigestSize()];
     md5.doFinal(bytes, 0);
     //noinspection UnstableApiUsage
     return hexColonDelimited(HashCode.fromBytes(bytes));
   }

   @SuppressWarnings("UnstableApiUsage")
   private static String hexColonDelimited(HashCode hc) {
      return Joiner.on(':').join(
          Splitter.fixedLength(2)
              .split(BaseEncoding.base16()
                  .lowerCase()
                  .encode(hc.asBytes())));
   }

   private static byte[] keyBlob(BigInteger publicExponent, BigInteger modulus) {
      try {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         writeLengthFirst("ssh-rsa".getBytes(), out);
         writeLengthFirst(publicExponent.toByteArray(), out);
         writeLengthFirst(modulus.toByteArray(), out);
         return out.toByteArray();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   // http://www.ietf.org/rfc/rfc4253.txt
   private static void writeLengthFirst(byte[] array, ByteArrayOutputStream out) throws IOException {
      out.write((array.length >>> 24) & 0xFF);
      out.write((array.length >>> 16) & 0xFF);
      out.write((array.length >>> 8) & 0xFF);
      out.write((array.length) & 0xFF);
      if (array.length == 1 && array[0] == (byte) 0x00)
         out.write(new byte[0]);
      else
         out.write(array);
   }

  public static Object extractPEMObject(byte[] decodable) {
    InputStreamReader sr = new InputStreamReader(new ByteArrayInputStream(decodable));
    try (PEMParser parser = new PEMParser(sr)) {
      Object obj = parser.readObject();
      return obj;

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static PrivateKey loadPrivateKey(Object pemObject, char[] passChars) throws IOException {

    final JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);

    if (pemObject instanceof PEMEncryptedKeyPair) { // PKCS#1 -----BEGIN RSA/DSA/EC PRIVATE KEY----- Proc-Type: 4,ENCRYPTED
      final PEMEncryptedKeyPair ckp = (PEMEncryptedKeyPair) pemObject;
      final PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(passChars);
      return converter.getKeyPair(ckp.decryptKeyPair(decProv)).getPrivate();
    } else if (pemObject instanceof PKCS8EncryptedPrivateKeyInfo) { // PKCS#8 -----BEGIN ENCRYPTED PRIVATE KEY-----
      try {
        final PKCS8EncryptedPrivateKeyInfo encryptedInfo = (PKCS8EncryptedPrivateKeyInfo) pemObject;
        final InputDecryptorProvider provider = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(passChars);
        final PrivateKeyInfo privateKeyInfo = encryptedInfo.decryptPrivateKeyInfo(provider);
        return converter.getPrivateKey(privateKeyInfo);
      } catch (PKCSException | OperatorCreationException e) {
        throw new IOException("Unable to decrypt private key.", e);
      }
    } else if (pemObject instanceof PrivateKeyInfo) { // PKCS#8 -----BEGIN PRIVATE KEY-----
      return converter.getPrivateKey((PrivateKeyInfo) pemObject);
    } else if (pemObject instanceof PEMKeyPair) { // PKCS#1 -----BEGIN RSA/DSA/EC PRIVATE KEY-----
      return converter.getKeyPair((PEMKeyPair) pemObject).getPrivate();
    }
    throw new IOException("Unrecognized private key format.");
  }

  /**
   * C.1 Probabilistic Prime-Factor Recovery The following algorithm recovers the prime factors of a
   * modulus, given the public and private exponents. The algorithm is based on Fact 1 in [Boneh
   * 1999]
   *
   * https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-56Br2.pdf
   */
  public static Map.Entry<BigInteger, BigInteger> probabilisticPrimeFactorization(BigInteger n, BigInteger e, BigInteger d) throws EvalException {
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
}