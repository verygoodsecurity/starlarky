package com.verygood.security.larky.modules.crypto;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.util.AlgorithmIdentifierFactory;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptor;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMEncryptorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.util.io.pem.PemObjectGenerator;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import lombok.Getter;

public class CryptoIOModule implements StarlarkValue {

  public static final CryptoIOModule INSTANCE = new CryptoIOModule();

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
          @Param(name = "data", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
          @Param(name = "marker", allowedTypes = {@ParamType(type = String.class)}),
          @Param(name = "passphrase", allowedTypes = {
              @ParamType(type = LarkyByteLike.class), @ParamType(type = NoneType.class)}),
          @Param(name = "randfunc"),
      }, useStarlarkThread = true)
  public String PEM_encode(LarkyByteLike exportable, String marker, Object passPhraseO, Object randfunc, StarlarkThread thread) throws EvalException {
    return null;
//    /**
//     * Note this PyCrypto comment:
//     * - only supports 3DES for PEM encoding encryption (DES-EDE3-CBC)
//     * - Encrypt with PKCS#7 padding
//     */
//    char[] passphrase = null;
//    if(!Starlark.isNullOrNone(passPhraseO)) {
//      byte[] bytes = ((LarkyByteLike) passPhraseO).getBytes();
//      CharBuffer decoded = StandardCharsets.ISO_8859_1.decode(ByteBuffer.wrap(bytes));
//      passphrase = Arrays.copyOf(decoded.array(), decoded.limit());
//    }
//    SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();
//
//    StringWriter sWrt = new StringWriter();
//    try (JcaPEMWriter pemWriter = new JcaPEMWriter(sWrt)) {
//      PEMEncryptor encryptor = Starlark.isNullOrNone(passphrase)
//          ? null
//          : new JcePEMEncryptorBuilder(PKCS8Generator.PBE_SHA1_3DES.toString())
//          .setSecureRandom(secureRandom)
//          .setProvider(BouncyCastleProvider.PROVIDER_NAME)
//          .build(passphrase);
//      JcaMiscPEMGenerator gen = new JcaMiscPEMGenerator(publicKey, encryptor);
//      PemObject pemObject = gen.generate();
//      pemWriter.writeObject(pemObject);
//    } catch (IOException e) {
//      throw new EvalException(e.getMessage(), e);
//    }
//    return sWrt.toString();
  }

  @StarlarkMethod(name = "PKCS8", structField = true)
  public PKCS8 PKCS8() {
    return PKCS8_INSTANCE;
  }
  public static final PKCS8 PKCS8_INSTANCE = new PKCS8();

  public static class PKCS8 implements StarlarkValue {

    @StarlarkMethod(
        name = "wrap", parameters = {
        @Param(name = "binary_key", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
        @Param(name = "oid", allowedTypes = {@ParamType(type = String.class)}),
        @Param(name = "passphrase", allowedTypes = {
            @ParamType(type = LarkyByteLike.class),
            @ParamType(type = NoneType.class)
        }),
        @Param(name = "protection", allowedTypes = {@ParamType(type = String.class), @ParamType(type = NoneType.class)}, defaultValue = "None"),
//      TODO uncomment these parameters and implement this later
//        @Param(name = "prot_params", allowedTypes = {@ParamType(type = Dict.class), @ParamType(type = NoneType.class)}),
//        @Param(name = "key_params", allowedTypes = {@ParamType(type = LarkyByteLike.class), @ParamType(type = NoneType.class)}),
//        @Param(name = "randfunc", defaultValue = "None"),
    }, useStarlarkThread = true)
    public LarkyByteLike wrap(LarkyByteLike binaryKey, String oid, Object passphraseO, Object protectionO, StarlarkThread thread) throws EvalException {
     /*
        oid = 1.2.840.113549.1.1.1
        oid = 1.2.840.113549.1.1.1
        PBKDF2WithHMACSHA1AndDES-EDE3-CBC
        MIIBOwIBAAJBAL8eJ5AKoIsjURpcEoGubZMxLD7+kT+TLr7UkvEtFrRhDDKMtuIIq19FrL4pUIMymPMSLBn3hJLe30Dw48GQM4UCAwEAAQJACUSDEp8RTe32ftq8IwG8Wojl5mAd1wFiIOrZ/Uv8b963WJOJiuQcVN29vxU5+My9GPZ7RA3hrDBEAoHUDPrIOQIhAPIPLz4dphiD9imAkivY31Rc5AfHJiQRA7XixTcjEkojAiEAyh/pJHks/Mlr+rdPNEpotBjfV4M4BkgGAA/ipcmaAjcCIQCHvhwwKVBLzzTscT2HeUdEeBMoiXXKJACAr3sJQJGxIQIgarRp+m1WSKV1MciwMaTOnbU7wxFs9DP1pva76lYBzgUCIQC9n0CnZCJ6IZYqSt0H5N7+Q+2Ro64nuwV/OSQfM6sBwQ==
      */

      SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();

      wtf algoId = null;
      String passphrase = Starlark.isNullOrNone(passphraseO) ? null : (String) passphraseO;
      String protection = Starlark.isNullOrNone(protectionO) ? null : (String) protectionO;
      final KeyFactory keyFactory;
      final PrivateKey privateKey;
      RSAPrivateKey pk;
      PKCS8EncodedKeySpec pkcs8EncodedKeySpec;
      OutputEncryptor encryptor = null;

      try {
        if(passphrase != null && protection != null) {
          algoId = extractProtection(protection);
        }
        keyFactory = KeyFactory.getInstance("RSA");

      } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
         throw new EvalException(e.getMessage(), e);
      }

      if(passphrase != null) {
        try {
          encryptor = new JceOpenSSLPKCS8EncryptorBuilder(PKCSObjectIdentifiers.des_EDE3_CBC)
              .setRandom(secureRandom)
              .setProvider(BouncyCastleProvider.PROVIDER_NAME)
              .setPasssword(passphrase.toCharArray())
              .build(); // TODO(fixme): possible char[] (16) vs (8)
        } catch (OperatorCreationException e) {
          throw new EvalException(e.getMessage(), e);
        }
      }

      try {
        pk = RSAPrivateKey.getInstance(new ASN1InputStream(binaryKey.getBytes()).readObject());
        pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(pk.getEncoded());
        privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
        PKCS8Generator gen = new JcaPKCS8Generator(privateKey, encryptor);
        return LarkyByte.builder(thread).setSequence(gen.generate().getContent()).build();
      } catch (IOException | InvalidKeySpecException e) {
        throw new EvalException(e.getMessage(), e);
      }

//      StringWriter outputStream = new StringWriter();
//      try (JcaPEMWriter pemWriter = new JcaPEMWriter(outputStream)) {
//        PKCS8Generator gen = new JcaPKCS8Generator(privateKey, encryptor);
////        JcaPKCS8Generator gen = new JcaPKCS8Generator(privateKey, build);
//        pemWriter.writeObject(gen.generate());
//      } catch (IOException e) {
//        throw new EvalException(e.getMessage(), e);
//      }
//      return LarkyByte.builder(thread).setSequence(outputStream.toString().getBytes(StandardCharsets.UTF_8)).build();
//----- ignore me -----//
      //      AlgorithmIdentifier algorithmIdentifier;
      //      algorithmIdentifier = AlgorithmIdentifierFactory.generateEncryptionAlgID(
      //          PKCSObjectIdentifiers.des_EDE3_CBC,
      //          -1,
      //          secureRandom);
      //
      //      Cipher kf;
      //      try {
      //        kf = Cipher.getInstance(oid);
      //        //KeyFactory kf = KeyFactory.getInstance(oid);
      //      } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      //        throw new EvalException(e.getMessage(), e);
      //      }
    }

    class wtf {
      @Getter
      private ASN1ObjectIdentifier asn1PKCS8EncryptionAlgorithm;
      @Getter
      private AlgorithmIdentifier algoId;

      public wtf(ASN1ObjectIdentifier asn1PKCS8EncryptionAlgorithm, AlgorithmIdentifier algoId) {
        this.asn1PKCS8EncryptionAlgorithm = asn1PKCS8EncryptionAlgorithm;
        this.algoId = algoId;
      }
    }

    private wtf extractProtection(String protection) throws NoSuchAlgorithmException, NoSuchProviderException {
      SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();

      if(protection.equals("PBKDF2WithHMAC-SHA1AndDES-EDE3-CBC")) {
        SecretKeyFactory kf = SecretKeyFactory.getInstance(
            "PBKDF2WithHMACSHA1",
            BouncyCastleProvider.PROVIDER_NAME);
        ASN1ObjectIdentifier asn1PKCS8EncryptionAlgorithm = PKCSObjectIdentifiers.des_EDE3_CBC;
        AlgorithmIdentifier algoId = AlgorithmIdentifierFactory.generateEncryptionAlgID(
            PKCSObjectIdentifiers.des_EDE3_CBC,
            -1,
            secureRandom);
        return new wtf(asn1PKCS8EncryptionAlgorithm, algoId);

      }
      else if(protection.equals("PBKDF2WithHMAC-SHA1AndAES128-CBC")
          || protection.equals("scryptAndAES128-CBC")) {
      }
      else if(protection.equals("PBKDF2WithHMAC-SHA1AndAES192-CBC")
          || protection.equals("scryptAndAES192-CBC")) {
      }
      else if(protection.equals("PBKDF2WithHMAC-SHA1AndAES256-CBC")
          || protection.equals("scryptAndAES256-CBC")) {
      }
      throw new NoSuchAlgorithmException(protection);
//      if protection == 'PBKDF2WithHMAC-SHA1AndDES-EDE3-CBC':
//                  key_size = 24
//                  module = DES3
//                  cipher_mode = DES3.MODE_CBC
//                  enc_oid = _OID_DES_EDE3_CBC
//              elif protection in ('PBKDF2WithHMAC-SHA1AndAES128-CBC',
//                      'scryptAndAES128-CBC'):
//                  key_size = 16
//                  module = AES
//                  cipher_mode = AES.MODE_CBC
//                  enc_oid = _OID_AES128_CBC
//              elif protection in ('PBKDF2WithHMAC-SHA1AndAES192-CBC',
//                      'scryptAndAES192-CBC'):
//                  key_size = 24
//                  module = AES
//                  cipher_mode = AES.MODE_CBC
//                  enc_oid = _OID_AES192_CBC
//              elif protection in ('PBKDF2WithHMAC-SHA1AndAES256-CBC',
//                      'scryptAndAES256-CBC'):
//                  key_size = 32
//                  module = AES
//                  cipher_mode = AES.MODE_CBC
//                  enc_oid = _OID_AES256_CBC
//              else:
//                  raise ValueError("Unknown PBES2 mode")
    }
  }


  @StarlarkMethod(name = "PEM", structField = true)
  public PEM PEM() {
    return PEM_INSTANCE;
  }
  private static final PEM PEM_INSTANCE = new PEM();

  public static class PEM implements StarlarkValue {

    @StarlarkMethod(
        name = "encode",
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
            @Param(name = "data", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
            @Param(name = "marker", allowedTypes = {@ParamType(type = String.class)}),
            @Param(name = "passphrase", allowedTypes = {
                @ParamType(type = LarkyByteLike.class), @ParamType(type = NoneType.class)}),
            @Param(name = "randfunc"),
        }, useStarlarkThread = true)
    public String encode(LarkyByteLike exportable, String marker, Object passPhraseO, Object randfunc, StarlarkThread thread) throws EvalException {
      char[] passphrase = null;
       if (!Starlark.isNullOrNone(passPhraseO)) {
         byte[] bytes = ((LarkyByteLike) passPhraseO).getBytes();
         CharBuffer decoded = StandardCharsets.ISO_8859_1.decode(ByteBuffer.wrap(bytes));
         passphrase = Arrays.copyOf(decoded.array(), decoded.limit());
       }
      try {
        return doEncode(exportable.getBytes(), marker, passphrase);
      } catch (IOException e) {
        throw new EvalException(e.getMessage(), e);
      }
      finally {
        if(passphrase != null) {
          Arrays.fill(passphrase, (char) 0);
        }
      }
    }

    private String doEncode(byte[] exportable, String marker, char[] passphrase) throws EvalException, IOException {
      /**
         * Note this PyCrypto comment:
         * - only supports 3DES for PEM encoding encryption (DES-EDE3-CBC)
         * - Encrypt with PKCS#7 padding
         */

      SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();
      OutputEncryptor pkcs8encryptor = null;
      PEMEncryptor pemEncryptor = null;
      PrivateKey privateKey = null;
      PemObjectGenerator gen = null;
      PrivateKeyInfo privateKeyInfo = null;
      AsymmetricKeyParameter key = null;
/*
  encryptor = (passphrase == null)
                ? null
                : new JceOpenSSLPKCS8EncryptorBuilder(PKCSObjectIdentifiers.des_EDE3_CBC)
                                .setRandom(secureRandom)
                                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                                .setPasssword(passphrase)
                                .build() ; // TODO(fixme): possible char[] (16) vs (8)
try {
        privateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(new PKCS8EncodedKeySpec(exportable));
      } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
        throw new EvalException(e.getMessage(), e);
      }

 */
      if (passphrase != null) {
        pemEncryptor = new JcePEMEncryptorBuilder("DES-EDE3-CBC")
              .setProvider(BouncyCastleProvider.PROVIDER_NAME)
              .setSecureRandom(secureRandom)
              .build(passphrase);
        try {
          pkcs8encryptor = new JceOpenSSLPKCS8EncryptorBuilder(PKCSObjectIdentifiers.des_EDE3_CBC)
              .setRandom(secureRandom)
              .setProvider(BouncyCastleProvider.PROVIDER_NAME)
              .setPasssword(passphrase)
              .build(); // TODO(fixme): possible char[] (16) vs (8)
        } catch (OperatorCreationException e) {
          throw new EvalException(e.getMessage(), e);
        }
      }
      switch (marker) {
        case "PUBLIC KEY":
          SubjectPublicKeyInfo pkinfo = SubjectPublicKeyInfoFactory
              .createSubjectPublicKeyInfo(
                  PublicKeyFactory.createKey(exportable));
          gen = new JcaMiscPEMGenerator(pkinfo, pemEncryptor);
          break;
        case "RSA PRIVATE KEY":
          if(passphrase != null) {
            RSAPrivateKey rsa = RSAPrivateKey.getInstance(exportable);
            gen = new JcaMiscPEMGenerator(
                PrivateKeyInfoFactory.createPrivateKeyInfo(
                    new RSAPrivateCrtKeyParameters(
                        rsa.getModulus(),
                        rsa.getPublicExponent(),
                        rsa.getPrivateExponent(),
                        rsa.getPrime1(),
                        rsa.getPrime2(),
                        rsa.getExponent1(),
                        rsa.getExponent2(),
                        rsa.getCoefficient()
                    )
                ),
                pemEncryptor);
            break;
          }
          // PrivateKeyInfo && PKCSObjectIdentifiers.rsaEncryption
          key = PrivateKeyFactory.createKey(exportable);
          privateKeyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(key);
          /*
            PrivateKeyInfo info = (PrivateKeyInfo)o;
            ASN1ObjectIdentifier algOID = info.getPrivateKeyAlgorithm().getAlgorithm();
           */
          gen = new JcaMiscPEMGenerator(privateKeyInfo, pemEncryptor);
          break;
        case "PRIVATE KEY":
          PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(exportable);
          key = PrivateKeyFactory.createKey(exportable);
          privateKeyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(key);
//          RSAPrivateCrtKeyParameters priv = (RSAPrivateCrtKeyParameters) PrivateKeyFactory.createKey(exportable);
//          AlgorithmIdentifier algID = AlgorithmIdentifierFactory.generateEncryptionAlgID(
//              PKCSObjectIdentifiers.des_EDE3_CBC,
//              -1,
//              secureRandom);
//          privateKeyInfo = new PrivateKeyInfo(algID, new RSAPrivateKey(
//              priv.getModulus(), priv.getPublicExponent(),
//              priv.getExponent(),
//              priv.getP(),
//              priv.getQ(),
//              priv.getDP(),
//              priv.getDQ(),
//              priv.getQInv()),
//              null);
          gen = new PKCS8Generator(privateKeyInfo, pkcs8encryptor);
          //new MiscPEMGenerator(KeyFactory.getInstance("RSA").generatePrivate(spec)).generate();
          //new JcaMiscPEMGenerator(PrivateKeyFactory.createKey(pkinfo)).generate();
          //gen = new JcaMiscPEMGenerator(privateKeyInfo, pemEncryptor);
          // // PrivateKeyInfo && No algorithm
          break;
        case "ENCRYPTED PRIVATE KEY":
//          PKCS8EncryptedPrivateKeyInfoBuilder pkcs8EncryptedPrivateKeyInfoBuilder =
//                              new JcaPKCS8EncryptedPrivateKeyInfoBuilder(privateKey);
//          PKCS8EncryptedPrivateKeyInfo pkcs8EncryptedPrivateKeyInfo = pkcs8EncryptedPrivateKeyInfoBuilder
//                  .build(new JcePKCSPBEOutputEncryptorBuilder(PKCSObjectIdentifiers.pbeWithSHA1AndDES_CBC)
//                          .setProvider(new BouncyCastleProvider())
//                          .build(passphrase);
          PKCS8EncryptedPrivateKeyInfo pkcs8EncryptedPrivateKeyInfo = new PKCS8EncryptedPrivateKeyInfo(exportable);
          gen = new JcaMiscPEMGenerator(pkcs8EncryptedPrivateKeyInfo, pemEncryptor);
          break;


      }


      //PrivateKey privateKey;
      StringWriter outputStream = new StringWriter();
      //PemObjectGenerator gen;
      try (JcaPEMWriter pemWriter = new JcaPEMWriter(outputStream)) {
//        try {
//          AsymmetricKeyParameter key = PrivateKeyFactory.createKey(exportable.getBytes());
//          PrivateKeyInfo privateKeyInfo = PrivateKeyInfoFactory.createPrivateKeyInfo(key);
//          gen = new PKCS8Generator(privateKeyInfo, encryptor);
//        } catch(IllegalArgumentException e ) {
//          privateKey = KeyFactory.getInstance("RSA")
//              .generatePrivate(new PKCS8EncodedKeySpec(exportable.getBytes()));
//
//          gen = new JcaMiscPEMGenerator(
//              privateKey,
//              new JcePEMEncryptorBuilder("DES-EDE3-CBC")
//                  .setProvider(BouncyCastleProvider.PROVIDER_NAME)
//                  .setSecureRandom(secureRandom)
//                  .build(passphrase));
//        }
        pemWriter.writeObject(gen.generate());
      } catch (IOException e) {
        throw new EvalException(e.getMessage(), e);
      }
      return outputStream.toString();
    }
  }
}
