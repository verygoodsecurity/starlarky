package com.verygood.security.larky.modules.x509;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.security.cert.CertificateEncodingException;
import java.util.Arrays;
import java.util.Objects;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.DHPublicKeyParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jcajce.provider.util.DigestFactory;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.DefaultAlgorithmNameFinder;

public class LarkyX509Certificate implements StarlarkValue {

  private static final DefaultAlgorithmNameFinder algFinder = new DefaultAlgorithmNameFinder();
  private final X509CertificateHolder cert;
  private transient int hash;   // lazy initialized
  private transient SubjectPublicKeyInfo certPublicKey;  // lazy initialized

  LarkyX509Certificate(final Certificate cert) {
    this(new X509CertificateHolder(cert));
  }

  LarkyX509Certificate(final X509CertificateHolder cert) {
    this.cert = cert;
    this.certPublicKey = null;
    this.hash = -1; // Default to -1

  }

  public static LarkyX509Certificate of(final X509CertificateHolder cert) throws EvalException {
    Objects.requireNonNull(cert);
    return new LarkyX509Certificate(cert);
  }

  public static LarkyX509Certificate of(final Certificate cert) throws EvalException {
    Objects.requireNonNull(cert);
    return new LarkyX509Certificate(cert);
  }


  public static LarkyX509Certificate of(final java.security.cert.X509Certificate cert) throws EvalException {
    try {
      return of(cert.getEncoded());
    } catch (CertificateEncodingException e) {
      throw new EvalException(e);
    }
  }

  public static LarkyX509Certificate of(final byte[] encoded) throws EvalException {
    return of(encoded, 0, encoded.length);
  }

  public static LarkyX509Certificate of(final byte[] encoded, final int offset, final int length) throws EvalException {
    Objects.requireNonNull(encoded);
    // short path
    try {
      final Certificate instance = Certificate.getInstance(encoded);
      return of(instance);
    }catch(IllegalArgumentException ignored) {
      // if we cannot just convert to an instance, let's continue to see
      // if it's a pem-encoded byte array
    }
    PEMParser parser;
    try(ByteArrayInputStream in = new ByteArrayInputStream(encoded, offset, length)) {
      parser = new PEMParser(new InputStreamReader(in));
      final Object object = parser.readObject();
      if (object instanceof X509CertificateHolder) {
        return of((X509CertificateHolder) object);
      }
      else if(object instanceof Certificate) {
        return of((Certificate) object);
      }
      throw Starlark.errorf("Could not parse PEM-encoded certificate");
    } catch (IOException e) {
      throw new EvalException(e);
    }
  }

  static byte[] digestOf(String hashFunction, byte[] input) {
    final Digest d = DigestFactory.getDigest(hashFunction);
    d.update(input, 0, input.length);
    byte[] result = new byte[d.getDigestSize()];
    d.doFinal(result, 0);
    return result;
  }

  /*
  Returns bytes using digest passed.
  */
  @StarlarkMethod(name = "fingerprint", parameters = {@Param(name = "hash_algorithm")}, useStarlarkThread = true)
  public StarlarkBytes fingerprint(String digestName, StarlarkThread thread) throws EvalException {
    byte[] der;
    try {
      //der = this.cert.toASN1Structure().getEncoded(ASN1Encoding.DER);
      der = this.certPublicKey.getEncoded(ASN1Encoding.DER);
    } catch (IOException e) {
      throw new EvalException(e);
    }
    byte[] digest = digestOf(digestName, der);
    return StarlarkBytes.of(thread.mutability(), digest);
      //.hex(":", StarlarkInt.of(0));
  }

  /*
    Returns certificate serial number
  */
  @StarlarkMethod(name = "serial_number")
  public StarlarkInt serial_number() {
    return StarlarkInt.of(this.cert.getSerialNumber());
  }


  /*
    Returns the certificate version
  */
  @StarlarkMethod(name = "version")
  public StarlarkInt version() {
    return StarlarkInt.of(this.cert.getVersionNumber());
  }


  /*
    Returns the public key as bytes
  */
  @StarlarkMethod(name = "public_key_as_bytes")
  public StarlarkBytes public_key_as_bytes() throws EvalException {
    try {
      return StarlarkBytes.immutableOf(this.getPublicKey().getEncoded());
    } catch (IOException e) {
      throw new EvalException(e);
    }
  }

  /*
    Returns the public key
  */
  @StarlarkMethod(name = "public_key")
  public LarkyKeyPair public_key() throws EvalException {
    final SubjectPublicKeyInfo publicKey = this.getPublicKey();
    try {
//      final AsymmetricKeyParameter key1 = PrivateKeyFactory.createKey(this.cert.getEncoded());
      final AsymmetricKeyParameter key = PublicKeyFactory.createKey(publicKey);
      if (key instanceof RSAKeyParameters) {
        return LarkyKeyPair.of(key, null, LarkyKeyPair.KEY_TYPE.RSA);
      } else if (key instanceof DSAPublicKeyParameters) {
        return LarkyKeyPair.of(key, null, LarkyKeyPair.KEY_TYPE.DSA);
      } else if (key instanceof ECPublicKeyParameters) {
        return LarkyKeyPair.of(key, null, LarkyKeyPair.KEY_TYPE.EC);
      }
      else if (key instanceof DHPublicKeyParameters) {
        return LarkyKeyPair.of(key, null, LarkyKeyPair.KEY_TYPE.DH);
      }
    } catch (IOException e) {
      throw new EvalException(e);

    }
    throw Starlark.errorf("Unrecognized public key");
  }


  /*
   Not before time (represented as UTC datetime)
  */
  @StarlarkMethod(name = "not_valid_before")
  public StarlarkInt not_valid_before() {
    return StarlarkInt.of(this.cert.getNotBefore().getTime());
  }


  /*
   Not after time (represented as UTC datetime)
  */
  @StarlarkMethod(name = "not_valid_after")
  public StarlarkInt not_valid_after() {
    return StarlarkInt.of(this.cert.getNotAfter().getTime());

  }


  /*
    Returns the issuer name object.
  */
  @StarlarkMethod(name = "issuer")
  public String issuer() {
    return this.cert.getIssuer().toString();
  }


  /*
  Returns the subject name object.
  */
  @StarlarkMethod(name = "subject")
  public String subject() {
    return this.cert.getSubject().toString();
  }


  /*
   Returns a HashAlgorithm corresponding to the type of the digest signed
   in the certificate.
   */
  @StarlarkMethod(name = "signature_hash_algorithm")
  public String signature_hash_algorithm() throws EvalException {
    ASN1ObjectIdentifier aid = this.cert.getSignatureAlgorithm().getAlgorithm();

    String algorithm;
    if (PKCSObjectIdentifiers.rsaEncryption.equals(aid)) {
      algorithm = "RSA";
    } else if (X9ObjectIdentifiers.id_dsa.equals(aid)) {
      algorithm = "DSA";
    } else if (X9ObjectIdentifiers.id_ecPublicKey.equals(aid)) {
      algorithm = "EC";
    } else {
      algorithm = algFinder.getAlgorithmName(this.getPublicKey().getAlgorithm().getAlgorithm());
    }

    if (algorithm == null) {
      throw new EvalException("unsupported key algorithm: " + aid);
    }
    return algorithm;
  }

  private SubjectPublicKeyInfo getPublicKey() {
    if (certPublicKey == null) {
      this.certPublicKey = this.cert.getSubjectPublicKeyInfo();
    }
    return this.certPublicKey;
  }


  /*
          Returns the ObjectIdentifier of the signature algorithm.
          */
  @StarlarkMethod(name = "signature_algorithm_oid")
  public ASN1ObjectIdentifier signature_algorithm_oid() {
    return this.cert.getSignatureAlgorithm().getAlgorithm();
  }


  /*
    Returns an Extensions object.
  */
  @StarlarkMethod(name = "extensions", useStarlarkThread = true)
  public StarlarkList<?> extensions(StarlarkThread thread) {
    final ASN1ObjectIdentifier[] oids = this.cert.getExtensions().getExtensionOIDs();
    return StarlarkEvalWrapper.zeroCopyList(thread.mutability(), oids);
  }


  /*
  Returns the signature bytes.
  */
  @StarlarkMethod(name = "signature", useStarlarkThread = true)
  public StarlarkBytes signature(StarlarkThread thread) {
    return StarlarkBytes.of(thread.mutability(), this.cert.getSignature());
  }


  /*
     Returns the tbsCertificate payload bytes as defined in RFC 5280.
     */
  @StarlarkMethod(name = "tbs_certificate_bytes", useStarlarkThread = true)
  public StarlarkBytes tbs_certificate_bytes(StarlarkThread thread) throws EvalException {
    try {
      return StarlarkBytes.of(thread.mutability(), this.cert.toASN1Structure().getTBSCertificate().getEncoded());
    } catch (IOException e) {
      throw new EvalException(e);
    }
  }


    /*
    Checks equality.
    */

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (this == other) {
      return true;
    }
    if (!(other instanceof LarkyX509Certificate)) {
      return false;
    }
    try {
      byte[] thisCert = this.cert.getEncoded();
      byte[] otherCert = ((LarkyX509Certificate) other).cert.getEncoded();

      return Arrays.equals(thisCert, otherCert);
    } catch (IOException e) {
      return false;
    }
  }

  /*
       Computes a hash.
       */
  @Override
  @StarlarkMethod(name = "hashCode")
  public int hashCode() {
    int h = hash;
    if (h == -1) {
      try {
        h = Arrays.hashCode(this.cert.getEncoded());
      } catch (IOException e) {
        h = 0;
      }
      hash = h;
    }
    return h;
  }

  /*
  Serializes the certificate to PEM or DER format.
  */
  @StarlarkMethod(name = "public_bytes", parameters = {@Param(name = "encoding")}, useStarlarkThread = true)
  public StarlarkBytes public_bytes(String encoding, StarlarkThread thread) throws EvalException {
    switch (encoding.toUpperCase()) {
      case "DER":
        try {
          return StarlarkBytes.of(thread.mutability(), this.getPublicKey().getEncoded(ASN1Encoding.DER));
        } catch (IOException e) {
          throw new EvalException(e);
        }
      case "PEM":
        return StarlarkBytes.immutableOf(to_pem().toCharArray());
      case "OpenSSH":
      case "Raw":
      case "ANSI X9.62":
      case "S/MIME":
      default:
        throw Starlark.errorf("Unsupported encoding: %s", encoding);
    }

  }

  @StarlarkMethod(name = "to_der", useStarlarkThread = true)
  public String to_der(StarlarkThread thread) throws EvalException {
    try {
      return StarlarkBytes
        .of(thread.mutability(), this.cert.getEncoded())
        .decode("utf-8", "report");
    } catch (IOException e) {
      throw new EvalException(e);
    }
  }

  @StarlarkMethod(name = "to_pem")
  public String to_pem() throws EvalException {
    final StringWriter wo = new StringWriter();
    try (JcaPEMWriter pWrt = new JcaPEMWriter(wo)) {
      pWrt.writeObject(this.cert);
    } catch (IOException e) {
      throw new EvalException(e);
    }
    wo.flush();
    return wo.toString();
  }

}