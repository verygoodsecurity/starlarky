package com.verygood.security.larky.modules.crypto.Util;

import static com.verygood.security.larky.modules.crypto.Util.CryptoUtils.encodeAsOpenSSH;
import static com.verygood.security.larky.modules.crypto.Util.CryptoUtils.fingerprint;
import static com.verygood.security.larky.modules.crypto.Util.CryptoUtils.generate;
import static com.verygood.security.larky.modules.crypto.Util.CryptoUtils.privateKeyHasFingerprint;
import static com.verygood.security.larky.modules.crypto.Util.CryptoUtils.privateKeyHasSha1;
import static com.verygood.security.larky.modules.crypto.Util.CryptoUtils.privateKeyMatchesPublicKey;
import static com.verygood.security.larky.modules.crypto.Util.CryptoUtils.privateKeySpec;
import static com.verygood.security.larky.modules.crypto.Util.CryptoUtils.publicKeySpecFromOpenSSH;
import static org.junit.Assert.assertEquals;

import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Map;

public class CryptoUtilsTest {

  public static String expectedFingerprint = "2b:a9:62:95:5b:8b:1d:61:e0:92:f7:03:10:e9:db:d9";
  public static String expectedSha1 = "c8:01:34:c0:3c:8c:91:ac:e1:da:cf:72:15:d7:f2:e5:99:5b:28:d4";
  public static String pubKey =
      "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCcm8DjTHg4r72dVhNLQ" +
      "33XpUyMLr+ph78i4NR3LqF1bXDP0g4xNLcI/GUTQq6g07X8zs7vIWyjoitqBPFSQ2onaZQ6pXQF/QISRQgrN5hEZ" +
      "+nH5Aw+isdstBeOMWKdYrCJtm6/qWq2+rByyuNbtulazP3H7SqoozSjRSGNQyFNGpmhjGgTbNQftYDwlFq0T9tCS" +
      "O/+dYF8j79bNIOEmfsCMiqXQ13hD5vGiEgkRm7zIPDUfpOl3ubDzebpRgGTh5kfv2vd3Z665AxQoi6fItvDu80kn" +
      "yphMlC41giIm5YqfPOPG4lR+6aF06p+NKhvOeECNMtRsD9u1kKJD9NqxXhx";

  public static String privKey =
      "-----BEGIN RSA PRIVATE KEY-----\n" +
      "MIIEogIBAAKCAQEAnJvA40x4OK+9nVYTS0N916VMjC6/qYe/IuDUdy6hdW1wz9IO\n" +
      "MTS3CPxlE0KuoNO1/M7O7yFso6IragTxUkNqJ2mUOqV0Bf0CEkUIKzeYRGfpx+QM\n" +
      "PorHbLQXjjFinWKwibZuv6lqtvqwcsrjW7bpWsz9x+0qqKM0o0UhjUMhTRqZoYxo\n" +
      "E2zUH7WA8JRatE/bQkjv/nWBfI+/WzSDhJn7AjIql0Nd4Q+bxohIJEZu8yDw1H6T\n" +
      "pd7mw83m6UYBk4eZH79r3d2euuQMUKIunyLbw7vNJJ8qYTJQuNYIiJuWKnzzjxuJ\n" +
      "UfumhdOqfjSobznhAjTLUbA/btZCiQ/TasV4cQIDAQABAoIBAEeOn1b8ZN455qDS\n" +
      "aKR2JTT4cX6ICckznnEYW9xNMTcPl4FN0HBJTuzLLn/bcyFHOxtVf5YiJpqqCb46\n" +
      "ne1hokp54mHdoaLu1Rh19GKS139CH77XA4U8Mh0IOM8e35lcM5/o/LeUeI89Aoyh\n" +
      "CbupWvzDN543TsuZLv7/InKCXt/0dXhAQpq3UiBT63EITQbyom5fSPnMzqM3F8jD\n" +
      "E9ZqkX4JsnTPC7FQDIpPCaKjG9YCZqoljz+1ssli3mN66V/JKefcCiVoubalmmT2\n" +
      "dpvmRtFaKvhAmkWYakYybYg8aDi3YygAHSU1bzxlY4TNiQgPdnTTDAPyeqqVrE1D\n" +
      "Chi+18UCgYEAzlk7c+tFwxZ3ryycOe0loFudUNE5rviHhPgbOHoSTooXh0Hq1Vrb\n" +
      "2ic+4FbRpoPHLpcLM9LX+arezUdeFBZ8qunjUG6MbUhAeAm/3cfMk+nZg3Skpg8+\n" +
      "C1D3hxGX4qdhURHvc2QUH7VIUWbucvPgtL8pt1z5Su/EE1Cb2XVsvu8CgYEAwkqZ\n" +
      "4vTZxI0XqJo6BfUnKGJEDC8xeWr10ELPdXLTCuNDpLSYNedQAxZi9XzvbnbWZ/MF\n" +
      "Z7IWkzzyAjsX0gpI56cxxtas/chxUboBlUo6ZW8QcPDcU2sKJi318wzElqqvRMNM\n" +
      "InfLf8nuPC9hyhe49/lFBBSZJeIo396DuqnTPp8CgYBO4NVVLm5wcLo3gDoH+psT\n" +
      "fXHZXuFJ/T7wmVbuc9tjom30CkKWZDD+Z1olr4pcuKr/KEXj/YkJq0OX/Nv9mcr2\n" +
      "GooGSPvtGl1qhW+Oe728HPxEv+XghJsXAFBelV8WCR2uO8jotyzqIgYO9+XWk1sm\n" +
      "PJzZtvSkrJqrN3kb20NCiQKBgDDVP0hj8jgMnl2qJdtJesYTrLbDRdQWpiHqKOqE\n" +
      "Kbca1+2V1ov1z453GfhJpoRFKi6GTl15zWLEdq9I2vvXyesvgrtPSbufnZvE/JDh\n" +
      "TzwfZip832O4C5z9AExOcTrNO7A0xfYD1goQXuiRoCqDO+JXrJkR9EwpQ8zAyKsp\n" +
      "9AZRAoGAGq3TYpmlI5oucEURHKsHOrIBirHFD+RaXMynxzgwkRnt6Z5Mg10I7Ddr\n" +
      "LiGK8/IrF8bg1F7weLVmj93zjvhQTh5yvb1jwVdFGXM2rbR7/P7F6n2f7xM4+lmv\n" +
      "Tq7E9Sv8UVuraAwJihlKCuBtpZM1t2JhcuNjXAZngj7R9j5HIZg=\n" +
      "-----END RSA PRIVATE KEY-----";

  @BeforeClass
  public static void beforeClass() {
    if(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

   @Test
   public void testCanReadRsaAndCompareFingerprintOnPublicRSAKey() throws IOException {
      RSAPublicKeySpec key = publicKeySpecFromOpenSSH(pubKey);
      String fingerPrint = fingerprint(key.getPublicExponent(), key.getModulus());
      assertEquals(fingerPrint, expectedFingerprint);
   }

   @Test
   public void testCanReadRsaAndCompareFingerprintOnPrivateRSAKey() throws IOException {
      RSAPrivateCrtKeySpec key = (RSAPrivateCrtKeySpec) privateKeySpec(privKey);
      String fingerPrint = fingerprint(key.getPublicExponent(), key.getModulus());
      assertEquals(fingerPrint, expectedFingerprint);
   }

   @Test
   public void testPrivateKeyMatchesFingerprintTyped() throws IOException {
      RSAPrivateCrtKeySpec privateKey = (RSAPrivateCrtKeySpec) privateKeySpec(privKey);
      assert privateKeyHasFingerprint(privateKey, expectedFingerprint);
   }

   @Test
   public void testPrivateKeyMatchesFingerprintString() throws IOException {
      assert privateKeyHasFingerprint(privKey, expectedFingerprint);
   }

   @Test
   public void testPrivateKeyMatchesSha1Typed() throws IOException {
         RSAPrivateCrtKeySpec privateKey = (RSAPrivateCrtKeySpec) privateKeySpec(privKey);
      assert privateKeyHasSha1(privateKey, expectedSha1);
   }

   @Test
   public void testPrivateKeyMatchesSha1String() throws IOException {
      assert privateKeyHasSha1(privKey, expectedSha1);
   }

   @Test
   public void testPrivateKeyMatchesPublicKeyTyped() throws IOException {
      RSAPrivateCrtKeySpec privateKey = (RSAPrivateCrtKeySpec) privateKeySpec(privKey);
      RSAPublicKeySpec publicKey = publicKeySpecFromOpenSSH(pubKey);
      assert privateKeyMatchesPublicKey(privateKey, publicKey);
   }

   @Test
   public void testPrivateKeyMatchesPublicKeyString() throws IOException {
      assert privateKeyMatchesPublicKey(privKey, pubKey);
   }

   @Test
   public void testCanGenerate() {
      Map<String, String> map = generate();
      assert map.get("public").startsWith("ssh-rsa ") : map;
      assert map.get("private").startsWith("-----BEGIN RSA PRIVATE KEY-----") : map;
      assert privateKeyMatchesPublicKey(map.get("private"), map.get("public")) : map;

   }

   @Test
   public void testEncodeAsOpenSSH() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
     RSAPublicKeySpec rsaPublicKeySpec = publicKeySpecFromOpenSSH(pubKey);
     RSAPublicKey rsa = (RSAPublicKey) KeyFactory.getInstance("RSA")
         .generatePublic(rsaPublicKeySpec);
     String encoded = encodeAsOpenSSH(
         new RSAKeyParameters(false, rsa.getModulus(), rsa.getPublicExponent())
     );
      assertEquals(encoded, pubKey.trim());
  }
}