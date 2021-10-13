package com.verygood.security.larky.modules.crypto;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.verygood.security.larky.modules.crypto.Protocol.KDF.BCryptKDF;
import com.verygood.security.larky.modules.crypto.Util.CryptoUtils;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.RSABlindedEngine;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateCrtKey;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyFactorySpi;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.RSAUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.custom.sec.SecP256R1Curve;
import org.bouncycastle.math.ec.custom.sec.SecP384R1Curve;
import org.bouncycastle.math.ec.custom.sec.SecP521R1Curve;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;


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
    Map.Entry<BigInteger, BigInteger> pq = CryptoUtils.probabilisticPrimeFactorization(
        n.toBigInteger(),
        e.toBigInteger(),
        d.toBigInteger());
    return Tuple.of(StarlarkInt.of(pq.getKey()), StarlarkInt.of(pq.getValue()));
  }

  @StarlarkMethod(
      name = "import_keyDER",
      parameters = {
          @Param(name = "externKey", allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
          @Param(name = "passPhrase", allowedTypes = {
              @ParamType(type = StarlarkBytes.class),
              @ParamType(type = NoneType.class)
          })
      }, useStarlarkThread = true)
  public StarlarkList<StarlarkInt> importKeyDER(StarlarkBytes externKey, Object passPhraseO, StarlarkThread thread) throws EvalException {
    List<BigInteger> components;
    char[] passphrase = null;
    if (!Starlark.isNullOrNone(passPhraseO)) {
      passphrase = ((StarlarkBytes) passPhraseO).toCharArray(StandardCharsets.ISO_8859_1);
    }

    try {
      components = decodeDERKey(externKey.toByteArray(), passphrase);
    } catch (SignatureException | NoSuchAlgorithmException | IOException e) {
      throw new EvalException("ValueError: " + e.getMessage(), e);
    }
    // TODO(mahmoudimus): hack until I get this module to return key pairs
    //  basically, pycrypto needs p mod (q^-1) and discards the crt co-efficient
    //  but we need to be able to re-construct a PEM encoded key, so we will
    //  have to just lop this thing off if it's a private key. ew.
    if (components.size() > 2) {
      components.remove(components.size() - 1); // remove the last item..
    }
    return StarlarkList.copyOf(
        thread.mutability(),
        components.stream().map(StarlarkInt::of).collect(Collectors.toList()));
  }

  public List<BigInteger> decodeDERKey(byte[] externKey, char[] passPhrase) throws SignatureException, EvalException, NoSuchAlgorithmException, IOException {
    List<Throwable> failures = new ArrayList<>();
    List<BigInteger> r;

    if (passPhrase != null) {
      r = _import_pkcs8_encrypted(externKey, passPhrase, failures);
      if (!r.isEmpty()) return r;
    }
    // see pycryptodome/RSA.py:_import_keyDER
    r = _importPKCS1Private(externKey, passPhrase, failures);
    if (!r.isEmpty()) return r;
    r = _import_pkcs1_public(externKey, passPhrase, failures);
    if (!r.isEmpty()) return r;
    r = _import_subjectPublicKeyInfo(externKey, passPhrase, failures);
    if (!r.isEmpty()) return r;
    r = _import_x509_cert(externKey, passPhrase, failures);
    if (!r.isEmpty()) return r;
    r = _import_pkcs8(externKey, passPhrase, failures);
    if (!r.isEmpty()) return r;


    StringBuilder sb = new StringBuilder();
    Throwable e = null;
    sb.append(failures.size());
    sb.append(" failures detected!");
    sb.append(System.lineSeparator());
    for (Throwable z : failures) {
      sb.append(z.getMessage());
      sb.append(System.lineSeparator());
      e = z;
    }
    throw new EvalException(sb.toString(), e);
  }

  @NotNull
  private List<BigInteger> _import_pkcs8_encrypted(byte[] externKey, char[] passPhrase, List<Throwable> failures) {
    List<BigInteger> r = new ArrayList<>();
    // TODO(mahmoudimus): explore if this can be done via PEMParser or MiscPEMGenerator
    PKCS8EncryptedPrivateKeyInfo x;
    try {
      x = new PKCS8EncryptedPrivateKeyInfo(externKey);
      RSAPrivateCrtKey privParams = (RSAPrivateCrtKey) CryptoUtils.loadPrivateKey(x, passPhrase);
      r.addAll(ImmutableList.of(
          privParams.getModulus(),
          privParams.getPublicExponent(),
          privParams.getPrivateExponent(),
          privParams.getPrimeP(),
          privParams.getPrimeQ(),
          privParams.getPrimeP().modInverse(privParams.getPrimeQ()),
          privParams.getCrtCoefficient()));
    } catch (IOException e) {
      failures.add(e);
    }
    return r;
  }

  @NotNull
  private List<BigInteger> _import_pkcs8(byte[] externKey, char[] passPhrase, List<Throwable> failures) throws IOException {
    List<BigInteger> r = new ArrayList<>();
    RSAPrivateCrtKeyParameters crtPrivParams;
    try {
      // is this a possible PKCS#8-encoded key?
      crtPrivParams = (RSAPrivateCrtKeyParameters) PrivateKeyFactory.createKey(externKey);
      r.addAll(ImmutableList.of(
          crtPrivParams.getModulus(),
          crtPrivParams.getPublicExponent(),
          crtPrivParams.getExponent(),
          crtPrivParams.getP(),
          crtPrivParams.getQ(),
          crtPrivParams.getP().modInverse(crtPrivParams.getQ()),
          crtPrivParams.getQInv()));
    } catch (IllegalArgumentException e) {
      failures.add(e);
    }
    return r;
  }

  @NotNull
  private List<BigInteger> _import_subjectPublicKeyInfo(byte[] externKey, char[] passPhrase, List<Throwable> failures) throws IOException {
    List<BigInteger> r = new ArrayList<>();
    RSAKeyParameters pubKey;
    try {
      pubKey = (RSAKeyParameters) PublicKeyFactory.createKey(externKey);
    } catch (IOException | IllegalArgumentException e) {
      failures.add(e); // ok so it's not a subject public key info.
      return r;
    }
    r = ImmutableList.of(pubKey.getModulus(), pubKey.getExponent());
    return r;
  }

  @NotNull
  private List<BigInteger> _import_x509_cert(byte[] externKey, char[] passPhrase, List<Throwable> failures) throws IOException {
    List<BigInteger> r = new ArrayList<>();
    RSAPublicKey pubKey;
    Certificate instance;
    //X509EncodedKeySpec spec = new X509EncodedKeySpec(externKey);
    //ASN1InputStream bIn = new ASN1InputStream(new ByteArrayInputStream(spec.getEncoded()));
    try {
      instance = Certificate.getInstance(new ASN1InputStream(externKey).readObject());
    } catch (IOException | IllegalArgumentException e) {
      failures.add(e);
      return r;
    }
    SubjectPublicKeyInfo pki = instance.getSubjectPublicKeyInfo();
    //SubjectPublicKeyInfo pki = SubjectPublicKeyInfo.getInstance(bIn.readObject());
    ASN1ObjectIdentifier algOid = pki.getAlgorithm().getAlgorithm();
    if (!RSAUtil.isRsaOid(algOid)) {
      failures.add(new EvalException(String.format(
          "Unknown algorithm id: %s, supporting only RSA here", algOid.getId())));
    }
    pubKey = (RSAPublicKey) new KeyFactorySpi().generatePublic(pki);
    r.addAll(ImmutableList.of(pubKey.getModulus(), pubKey.getPublicExponent()));
    return r;
  }

  @NotNull
  private List<BigInteger> _import_pkcs1_public(byte[] externKey, char[] passPhrase, List<Throwable> failures) {
    List<BigInteger> r = new ArrayList<>();
    try {
      DLSequence sequence = (DLSequence) ASN1Sequence.getInstance(externKey);
      org.bouncycastle.asn1.pkcs.RSAPublicKey pubKey = org.bouncycastle.asn1.pkcs.RSAPublicKey.getInstance(sequence);
      //ASN1Integer modulus = ASN1Integer.getInstance(sequence.getObjectAt(0));
      //ASN1Integer exponent = ASN1Integer.getInstance(sequence.getObjectAt(1));
      // TODO(mahmoudimus): do some checks here to see if this is even close
      //  if e <= 1 || e >= modulus, we have something wonky.
      r.addAll(ImmutableList.of(pubKey.getModulus(), pubKey.getPublicExponent()));
    } catch (IllegalArgumentException e) {
      failures.add(e);
    }
    return r;
  }

  @NotNull
  private List<BigInteger> _importPKCS1Private(byte[] externKey, char[] passPhrase, List<Throwable> failures) {
    List<BigInteger> r = new ArrayList<>();
    ASN1Integer integer;
    DLSequence sequence;
    Enumeration<?> rator;

    // is it is not, so let's try to see if it is a PKCS#1 encoded key.
    // ok now we try some black magic
    // https://stackoverflow.com/questions/48958304/pkcs1-and-pkcs8-format-for-rsa-private-key great question + answer
    try {
      sequence = (DLSequence) ASN1Sequence.getInstance(externKey);
      // if next element is 0 or 1, it's probably a private key
      rator = sequence.getObjects();
      integer = ASN1Integer.getInstance(rator.nextElement());
    } catch (IllegalArgumentException e) {
      failures.add(e);
      return r;
    }

    if (!integer.getValue().equals(BigInteger.ONE)
        && !integer.getValue().equals(BigInteger.ZERO)) {
      return r;
    }
    // ok this is a private key
    // ok, RFC says next thing should _NOT_ be a sequence if it is a PKCS#1 encoded
    // key, so let's check
    if (rator.nextElement() instanceof ASN1Sequence) {
      // it is! abort.
      //AlgorithmIdentifier.getInstance(rator.nextElement());
      return r;
    }
    RSAPrivateKey privParams = RSAPrivateKey.getInstance(sequence);
    r.addAll(ImmutableList.of(
        privParams.getModulus(),
        privParams.getPublicExponent(),
        privParams.getPrivateExponent(),
        privParams.getPrime1(),
        privParams.getPrime2(),
        privParams.getPrime1().modInverse(privParams.getPrime2()),
        privParams.getCoefficient()));
    return r;
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
      name = "PKCS8_unwrap", parameters = {
      @Param(name = "binary_key", allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
      @Param(name = "oid", allowedTypes = {@ParamType(type = String.class)}),
      @Param(name = "passphrase", allowedTypes = {
          @ParamType(type = StarlarkBytes.class),
          @ParamType(type = NoneType.class)
      }),
      @Param(name = "protection", allowedTypes = {@ParamType(type = String.class)})
  }, useStarlarkThread = true)
  public Tuple PKCS8_unwrap(StarlarkBytes binaryKey, String oid, Object passphraseO, String protection, StarlarkThread thread) throws EvalException {
    InputStreamReader r = new InputStreamReader(new ByteArrayInputStream(binaryKey.toByteArray()));
    String passphrase = Starlark.isNullOrNone(passphraseO) ? null : (String) passphraseO;
    KeyPair kp;
    try (PEMParser pemParser = new PEMParser(r)) {
      JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
      Object pemKeyPairObject = pemParser.readObject();
      kp = extractKeyPair(pemKeyPairObject, converter, passphrase);
    } catch (IOException | SignatureException e) {
      throw new EvalException(e.getMessage(), e);
    }
    String algorithm = kp.getPrivate().getAlgorithm();
    return Tuple.of(
        algorithm,
        StarlarkBytes.of(thread.mutability(),kp.getPrivate().getEncoded())
        //StarlarkBytes.builder(thread).setSequence(kp.getPrivate().getEncoded()).build()
    );
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
              @ParamType(type = StarlarkBytes.class), @ParamType(type = NoneType.class)}),
      }, useStarlarkThread = true)
  public StarlarkList<?> PEM_decode(String decodable, Object passphrase, StarlarkThread thread) throws EvalException {

    Map<String, byte[]> keyParts;
    try {
      Object pemObj = CryptoUtils.extractPEMObject(decodable.getBytes(StandardCharsets.UTF_8));
      if (pemObj == null) throw Starlark.errorf("Could not extract PEM encoded object!");
      char[] passChars = Starlark.isNullOrNone(passphrase)
          ? "".toCharArray()
          : ((StarlarkBytes) passphrase).toCharArray();
      keyParts = PEM_parse(pemObj, passChars);
    } catch (IOException | CryptoException e) {
      throw new EvalException(e.getMessage(), e);
    }

    Dict.Builder<String, Object> rval = Dict.<String, Object>builder()
        .put("n", StarlarkInt.of(new BigInteger(keyParts.get("n"))))
        .put("e", StarlarkInt.of(new BigInteger(keyParts.get("e"))));

    if (keyParts.containsKey("d")) {
      rval.put("d", StarlarkInt.of(new BigInteger(keyParts.get("d"))))
          .put("p", StarlarkInt.of(new BigInteger(keyParts.get("p"))))
          .put("q", StarlarkInt.of(new BigInteger(keyParts.get("q"))))
          .put("u", StarlarkInt.of(new BigInteger(keyParts.get("u"))));
    }
    return StarlarkList.copyOf(
        thread.mutability(),
        rval.build(thread.mutability()).values());
  }

  private Map<String, byte[]> PEM_parse(Object obj, char[] passChars) throws EvalException, IOException, CryptoException {
    JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
    Map<String, byte[]> returnVal = new HashMap<>();
    // if not encrypted
    if (obj instanceof PEMKeyPair) {
      PEMKeyPair obj_ = (PEMKeyPair) obj;
      KeyPair keyPair = converter.getKeyPair(obj_);
      // TODO what happens if keyPair algorithm is not RSA?
      if (keyPair.getPublic().getAlgorithm().equals("RSA")) {
        RSAPublicKey rsaPublicKey = convertPublicKey(obj_.getPublicKeyInfo());
        BCRSAPrivateCrtKey bcKey = (BCRSAPrivateCrtKey) converter.getPrivateKey(obj_.getPrivateKeyInfo());
        buildPublicParameters(rsaPublicKey, returnVal);
        buildPrivateKeyParameters(bcKey, returnVal);
        return returnVal;
      }
      throw Starlark.errorf("Unknown conversion algorithm for algo: %s", keyPair.getPublic().getAlgorithm());
    } else if (obj instanceof SubjectPublicKeyInfo) {
      RSAPublicKey rsaPublicKey = convertPublicKey((SubjectPublicKeyInfo) obj);
      buildPublicParameters(rsaPublicKey, returnVal);
      return returnVal;
    } else if (obj instanceof X509CertificateHolder) {
      RSAPublicKey rsaPublicKey = convertPublicKey(((X509CertificateHolder) obj).getSubjectPublicKeyInfo());
      buildPublicParameters(rsaPublicKey, returnVal);
      return returnVal;
    }

    // if we are here we are probably looking at a private key
    BCRSAPrivateCrtKey pk = (BCRSAPrivateCrtKey) CryptoUtils.loadPrivateKey(obj, passChars);
    RSAPublicKey publicKey;
    try {
      publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
          new RSAPublicKeySpec(pk.getModulus(), pk.getPublicExponent()));
    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      throw new EvalException(e.getMessage(), e);
    }
    buildPublicParameters(publicKey, returnVal);
    buildPrivateKeyParameters(pk, returnVal);
    return returnVal;
  }

  private void buildPrivateKeyParameters(BCRSAPrivateCrtKey bcKey, Map<String, byte[]> returnVal) {
    returnVal.put("p", bcKey.getPrimeP().toByteArray());
    returnVal.put("q", bcKey.getPrimeQ().toByteArray());
    returnVal.put("u", bcKey.getPrimeP().modInverse(bcKey.getPrimeQ()).toByteArray());
    returnVal.put("crt", bcKey.getCrtCoefficient().toByteArray());
    returnVal.put("d", bcKey.getPrivateExponent().toByteArray());
  }

  private void buildPublicParameters(RSAPublicKey rsaPublicKey, Map<String, byte[]> returnVal) throws PEMException {
    returnVal.put("n", rsaPublicKey.getModulus().toByteArray());
    returnVal.put("e", rsaPublicKey.getPublicExponent().toByteArray());

  }

  private RSAPublicKey convertPublicKey(SubjectPublicKeyInfo pk) throws CryptoException, IOException {
    ASN1ObjectIdentifier algOid = pk.getAlgorithm().getAlgorithm();
    try {
      KeyFactory kf = KeyFactory.getInstance(algOid.getId());

      if (RSAUtil.isRsaOid(algOid)) {
        org.bouncycastle.asn1.pkcs.RSAPublicKey rsa = org.bouncycastle.asn1.pkcs.RSAPublicKey.getInstance(pk.parsePublicKey());
        //RSAPublicKeySpec pubSpec = new RSAPublicKeySpec(rsa.getModulus(), rsa.getPublicExponent());
        BCRSAPublicKey publicKey = (BCRSAPublicKey) new KeyFactorySpi().generatePublic(pk);
        return publicKey;
      }
    } catch (GeneralSecurityException gse) {
      throw new CryptoException(gse.getMessage(), gse);
    }
    throw new CryptoException(
        String.format("Unknown algorithm id: %s, supporting only RSA here", algOid.getId()));
  }


  @StarlarkMethod(
      name = "decrypt",
      parameters = {
          @Param(name = "rsaObj", allowedTypes = {@ParamType(type = Dict.class)}),
          @Param(name = "ciphertext")
      },
      useStarlarkThread = true
  )
  public StarlarkBytes RSADecrypt(Dict<String, StarlarkInt> finalRsaObj, StarlarkBytes cT, StarlarkThread thread) throws EvalException {
    byte[] cipherText = cT.toByteArray();
    byte[] bytes;
    try {
      bytes = RSA_decrypt(finalRsaObj, cipherText);
    } catch (DataLengthException | InvalidCipherTextException e) {
      throw new EvalException("ValueError: " + e.getMessage(), e);
    }
    return StarlarkBytes.of(thread.mutability(), bytes);
//    return StarlarkBytes.builder(thread).setSequence(bytes).build();
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
  public StarlarkBytes RSAEncrypt(Dict<String, StarlarkInt> finalRsaObj, StarlarkBytes pT, StarlarkThread thread) throws EvalException {
    byte[] plainText = pT.toByteArray();
    byte[] bytes;
    try {
      bytes = RSA_encrypt(finalRsaObj, plainText);
    } catch (DataLengthException | InvalidCipherTextException e) {
      throw new EvalException("ValueError: " + e.getMessage(), e);
    }
    return StarlarkBytes.of(thread.mutability(), bytes);
//    return StarlarkBytes.builder(thread).setSequence(bytes).build();
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


  @StarlarkMethod(
      name = "decrypt_openssh_key", parameters = {
      @Param(name = "encrypted", allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
      @Param(name = "password", allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
      @Param(name = "salt", allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
  }, useStarlarkThread = true)
  public StarlarkBytes decryptOpenSSHKey(
      StarlarkBytes encrypted, StarlarkBytes password, StarlarkBytes salt, StarlarkThread thread) throws EvalException {
    if (password.size() == 0) {
      throw Starlark.errorf("Password cannot have size 0 when decrypting an SSH key!");
    }
    byte[] passInBytes = password.toByteArray();
    // We need 32+16 = 48 bytes, therefore 2 bcrypt outputs are sufficient
    byte[] key;
    try {
      // 16 rounds
      key = BCryptKDF.bcrypt_pbkdf(passInBytes, salt.toByteArray(), 48, 16);
    } catch (NoSuchAlgorithmException e) {
      throw new EvalException(e.getMessage(), e);
    }
    if (key.length != 48) {
      throw Starlark.errorf("Unexpected key size - was expecting 48, but got: %d", key.length);
    }
    CipherParameters keyIv = new ParametersWithIV(new KeyParameter(key, 0, 32), key, 32, 48 - 32);
    //AES256 CTR is SICBlockCipher
    BufferedBlockCipher cipher = new BufferedBlockCipher(
        new SICBlockCipher(
            new AESEngine()));
    cipher.init(false, keyIv);

    byte[] result = new byte[cipher.getOutputSize(encrypted.size())];
    int length = cipher.processBytes(
      encrypted.toByteArray(), 0, result.length, result, 0);
    try {
      length += cipher.doFinal(result, length);
    } catch (InvalidCipherTextException e) {
      throw new EvalException(e.getMessage(), e);
    }
    return StarlarkBytes.of(thread.mutability(), result);
//    return StarlarkBytes.builder(thread).setSequence(result).build();
  }

  public static class LarkyECPoint implements StarlarkValue {
    private ECPoint point;

    public LarkyECPoint(ECPoint point) {
      this.point = point;
    }

    @StarlarkMethod(name = "negate")
    public LarkyECPoint negate() {
      this.point = this.point.normalize().negate();
      return this;
      //return new LarkyECPoint(this.point.normalize().negate());
    }

    @StarlarkMethod(name = "is_infinity")
    public boolean isInfinity() {
      return this.point.isInfinity();
    }

    @StarlarkMethod(name = "as_tuple")
    public Tuple asTuple() {
      if (this.point.isInfinity()) {
        return Tuple.of(StarlarkInt.of(0), StarlarkInt.of(0));
      }
      return Tuple.of(
        StarlarkInt.of(this.point.getXCoord().toBigInteger()),
        StarlarkInt.of(this.point.getYCoord().toBigInteger())
      );
    }
    @StarlarkMethod(name="twice")
    public LarkyECPoint twice() {
      this.point = this.point.twice().normalize();
      return this;
    }

    @StarlarkMethod(name="add", parameters = {@Param(name="point", allowedTypes = {@ParamType(type=LarkyECPoint.class)})})
    public LarkyECPoint add(LarkyECPoint other) throws EvalException {
      if (!(this.point.getCurve().equals(other.point.getCurve()))) {
        throw Starlark.errorf("ValueError: EC points are not on the same curve");
      }
      this.point = this.point.add(other.point).normalize();
      return this;
    }

    @StarlarkMethod(name = "multiply", parameters = {@Param(name = "point", allowedTypes = {@ParamType(type = StarlarkInt.class)})})
    public LarkyECPoint multiply(StarlarkInt scale) {
      ////ECPoint point2 = this.point.multiply(scale.toBigInteger());
      //ECPoint point2 = new FixedPointCombMultiplier().multiply(this.point, scale.toBigInteger()).normalize();
      //point2.getXCoord().toBigInteger();
      this.point = this.point.multiply(scale.toBigInteger()).normalize();
      return this;
    }

    @Override
    public boolean equals(final Object that) {
      return that instanceof LarkyECPoint
               && this.point.equals(((LarkyECPoint) that).point);
    }

    @Override
    public int hashCode() {
      return this.point.normalize().hashCode();
    }
  }


  public static class LarkyEllipticCurveCrypto implements StarlarkValue {
    public static LarkyEllipticCurveCrypto INSTANCE = new LarkyEllipticCurveCrypto();


    public static class LarkyECCurve implements StarlarkValue {
      final private ECCurve curve;

      public LarkyECCurve(ECCurve curve) {
        this.curve = curve;
      }

      @StarlarkMethod(name = "point", parameters = {
        @Param(name = "xb", allowedTypes = {@ParamType(type = StarlarkInt.class)}),
        @Param(name = "yb", allowedTypes = {@ParamType(type = StarlarkInt.class)}),
      })
      public LarkyECPoint point(StarlarkInt xb, StarlarkInt yb) throws EvalException {
        BigInteger x = xb.toBigInteger();
        BigInteger y = yb.toBigInteger();
        ECPoint point = this.curve.createPoint(x, y);
        if (!point.isValid()) {
          throw Starlark.errorf("ValueError: The EC point does not belong to the curve");
        }
        return new LarkyECPoint(point);
      }

      @StarlarkMethod(name = "infinity")
      public LarkyECPoint pointAtInfinity() {
        return new LarkyECPoint(this.curve.getInfinity());
      }
    }

    @StarlarkMethod(name="P256R1Curve")
    public LarkyECCurve P256R1Curve() {
      return new LarkyECCurve(new SecP256R1Curve());
    }

    @StarlarkMethod(name="P384R1Curve")
    public LarkyECCurve P384R1Curve() {
      return new LarkyECCurve(new SecP384R1Curve());
    }

    @StarlarkMethod(name="P521R1Curve")
    public LarkyECCurve P521R1Curve() {
      return new LarkyECCurve(new SecP521R1Curve());
    }
  }

  @StarlarkMethod(
    name = "ECC", structField = true
  )
  public LarkyEllipticCurveCrypto ECC() throws EvalException {
    return LarkyEllipticCurveCrypto.INSTANCE;
  }
//  @StarlarkMethod(
//          name = "ECPoint", parameters = {
//          @Param(name = "x", allowedTypes = {@ParamType(type = StarlarkInt.class)}),
//          @Param(name = "y", allowedTypes = {@ParamType(type = StarlarkInt.class)}),
//          @Param(name = "curve", allowedTypes = {@ParamType(type = Dict.class)}),
//  }, useStarlarkThread = true)
//  public StarlarkBytes ECPoint(StarlarkInt x, StarlarkInt y, Dict<String, Object> curve, StarlarkThread thread) throws EvalException {
//
//    ECPoint point = new ECPoint(x.toBigInteger(), y.toBigInteger());
//    return point;
//
}