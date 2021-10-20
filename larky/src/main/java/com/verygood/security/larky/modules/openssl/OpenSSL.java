package com.verygood.security.larky.modules.openssl;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.Date;

import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.lib.json.Json;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

public class OpenSSL implements StarlarkValue {
  public static final OpenSSL INSTANCE = new OpenSSL();

  @StarlarkMethod(name = "load_privatekey", parameters = {
    @Param(name = "buffer", allowedTypes = {
        @ParamType(type = StarlarkBytes.class)
    }),
    @Param(name = "passphrase", allowedTypes = {
        @ParamType(type = StarlarkBytes.class),
        @ParamType(type = NoneType.class),
    })
  })
  public LarkyLoadedKey loadPrivateKey(StarlarkBytes buffer, Object passPhraseO) throws EvalException {
    final String passphrase = Starlark.isNullOrNone(passPhraseO)
      ? ""
      : new String(((StarlarkBytes) passPhraseO).toByteArray(), StandardCharsets.UTF_8);
    KeyPair keyPair = new SSLUtils()
        .decodePrivKey(
            new String(buffer.toByteArray(), StandardCharsets.UTF_8),
            passphrase);

    return new LarkyLoadedKey(keyPair);
  }

  @StarlarkMethod(
    name="X509_sign",
    parameters = {
      @Param(name = "encodedKey", allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
      @Param(name = "jsonEncodedCert", allowedTypes = {@ParamType(type = String.class)}),
      @Param(name = "digestName", allowedTypes = {@ParamType(type = String.class)}),
    },
    useStarlarkThread = true
  )
  public Object X509Sign(StarlarkBytes encodedKey, String jsonEncodedCert, String digestName, StarlarkThread thread)
    throws EvalException, IOException, OperatorCreationException, CertificateException {

    RSAPrivateCrtKeyParameters key = (RSAPrivateCrtKeyParameters) PrivateKeyFactory.createKey(encodedKey.toByteArray());
    //TODO(mahmoudimus): Eww. This should be a class that we pass to a POJO, but
    // I do not want to add a jackson / mapstruct dependency
    Dict<String, Object> serde =
      Dict.cast(
        StarlarkUtil.valueToStarlark(
          Json.INSTANCE.decode(jsonEncodedCert, thread),
          thread.mutability()),
        String.class, Object.class, "Cannot convert JSON payload");

    LarkyX509Cert payload = LarkyX509Cert.builder()
                        .gmtime_adj_notAfter(((StarlarkInt) serde.get("gmtime_adj_notAfter")).toBigInteger())
                        .gmtime_adj_notBefore(((StarlarkInt) serde.get("gmtime_adj_notBefore")).toBigInteger())
                        .issuer_name((String) serde.get("issuer_name"))
                        .subject_name((String) serde.get("subject_name"))
                        .build();

    Instant now = Instant.now();
    Date validityBeginDate = Date.from(
      now.plusSeconds(
        payload.getGmtime_adj_notBefore().longValue()));
    Date validityEndDate = Date.from(
      now.plusSeconds(
        payload.gmtime_adj_notAfter.longValue()));
    //DigestFactory.getDigest(digestName);
    //new X509CertificateHolder()
    //return new X509v3CertificateBuilder();

    // https://github.com/bcgit/bc-java/blob/master/prov/src/test/java/org/bouncycastle/pqc/jcajce/provider/test/KeyStoreTest.java
    // https://stackoverflow.com/questions/29852290/self-signed-x509-certificate-with-bouncy-castle-in-java

    //
    // set up our certs
    //
    //KeyPairGenerator kpg  = KeyPairGenerator.getInstance("RSA", "BC");

    //kpg.initialize(1024, new SecureRandom());

    //
    // cert that issued the signing certificate
    //
    //signDN = "O=Bouncy Castle, C=AU";
    //signKP = kpg.generateKeyPair();
    String realDigestName;
    switch (digestName.toUpperCase()) {
      case "SHA256":
        realDigestName = digestName + "with" + "RSA";
        break;
      default:
        throw Starlark.errorf("Unknown digest name: " + digestName);
    }
    SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();
    BigInteger rootSerialNum = new BigInteger(Long.toString(secureRandom.nextLong()));
    // Issued By and Issued To same for root certificate
    X500Name rootCertIssuer = new X500Name(payload.getIssuer_name());
    X500Name rootCertSubject = rootCertIssuer;
    ContentSigner rootCertContentSigner =
      new JcaContentSignerBuilder(realDigestName)
        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
        .build(
          BouncyCastleProvider.getPrivateKey(
            PrivateKeyInfoFactory.createPrivateKeyInfo(key)));

    X509v3CertificateBuilder rootCertBuilder =
      new JcaX509v3CertificateBuilder(
        rootCertIssuer, rootSerialNum,
        validityBeginDate, validityEndDate, rootCertSubject,
        BouncyCastleProvider.getPublicKey(
          SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(key)
        ));
    X509CertificateHolder certificateHolder = rootCertBuilder.build(rootCertContentSigner);
//    X509Certificate caCertificate =
//      new JcaX509CertificateConverter()
//        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
//        .getCertificate(certificateHolder);
    return StarlarkBytes.of(thread.mutability(),certificateHolder.getEncoded());
//    return StarlarkBytes.builder(thread).setSequence(certificateHolder.getEncoded()).build();
  }

  @StarlarkMethod(name = "dump_certificate",
    parameters = {
      @Param(name = "cert", allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
      @Param(name = "type", allowedTypes = {@ParamType(type = String.class)}),
  }, useStarlarkThread = true)
  public StarlarkBytes dumpCertificate(StarlarkBytes cert, String type, StarlarkThread thread) throws EvalException {
    /*
      > pemWriter.close();
      > return stringWriter.toString();
      > Files.write(new File(RESULT_FOLDER, "CA.crt").toPath(), signCert.getEncoded());

      cert we sign against

      > origDN = "CN=Eric H. Echidna, E=eric@bouncycastle.org, O=Bouncy Castle, C=AU";
      > origKP = kpg.generateKeyPair();
      > origCert = makeCertificate(origKP, origDN, signKP, signDN);
      > Files.write(
      ... new File(RESULT_FOLDER, "User.crt").toPath(),
      ... origCert.getEncoded());
     */
    StringWriter stringWriter = new StringWriter();
    switch(type) {
      case "PEM":
        try(PemWriter pemWriter = new PemWriter(stringWriter)) {
          X509CertificateHolder certificateHolder = new X509CertificateHolder(cert.toByteArray());
          PemObject obj = new PemObject("CERTIFICATE", certificateHolder.toASN1Structure().getEncoded());
          pemWriter.writeObject(obj);
        } catch (IOException e) {
          throw new EvalException(e.getMessage(), e);
        }
        break;
      case "ASN1":
      case "TEXT":
      default:
        throw Starlark.errorf("certificate type %s not supported!", type);
    }
    return StarlarkBytes.of(thread.mutability(),stringWriter.toString().getBytes(StandardCharsets.UTF_8));
  }


}
