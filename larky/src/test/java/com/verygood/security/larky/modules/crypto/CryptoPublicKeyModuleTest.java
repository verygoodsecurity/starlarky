package com.verygood.security.larky.modules.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.truth.Truth;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;

import com.verygood.security.larky.modules.openssl.SSLUtils;
import com.verygood.security.larky.modules.utils.NumOpsUtils;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.SyntaxError;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CryptoPublicKeyModuleTest {

  private static final String plaintext =
      "   eb 7a 19 ac e9 e3 00 63 50 e3 29 50 4b 45 e2\n" +
      "ca 82 31 0b 26 dc d8 7d 5c 68 f1 ee a8 f5 52 67\n" +
      "c3 1b 2e 8b b4 25 1f 84 d7 e0 b2 c0 46 26 f5 af\n" +
      "f9 3e dc fb 25 c9 c2 b3 ff 8a e1 0e 83 9a 2d db\n" +
      "4c dc fe 4f f4 77 28 b4 a1 b7 c1 36 2b aa d2 9a\n" +
      "b4 8d 28 69 d5 02 41 21 43 58 11 59 1b e3 92 f9\n" +
      "82 fb 3e 87 d0 95 ae b4 04 48 db 97 2f 3a c1 4f\n" +
      "7b c2 75 19 52 81 ce 32 d2 f1 b7 6d 4d 35 3e 2d";

  private static final String ciphertext =
      "12 53 e0 4d c0 a5 39 7b b4 4a 7a b8 7e 9b f2 a0\n" +
      "39 a3 3d 1e 99 6f c8 2a 94 cc d3 00 74 c9 5d f7\n" +
      "63 72 20 17 06 9e 52 68 da 5d 1c 0b 4f 87 2c f6\n" +
      "53 c1 1d f8 23 14 a6 79 68 df ea e2 8d ef 04 bb\n" +
      "6d 84 b1 c3 1d 65 4a 19 70 e5 78 3b d6 eb 96 a0\n" +
      "24 c2 ca 2f 4a 90 fe 9f 2e f5 c9 c1 40 e5 bb 48\n" +
      "da 95 36 ad 87 00 c8 4f c9 13 0a de a7 4e 55 8d\n" +
      "51 a7 4d df 85 d8 b5 0d e9 68 38 d6 06 3e 09 55";

  private static final String MODULUS =
      "bb f8 2f 09 06 82 ce 9c 23 38 ac 2b 9d a8 71 f7\n" +
      "36 8d 07 ee d4 10 43 a4 40 d6 b6 f0 74 54 f5 1f\n" +
      "b8 df ba af 03 5c 02 ab 61 ea 48 ce eb 6f cd 48\n" +
      "76 ed 52 0d 60 e1 ec 46 19 71 9d 8a 5b 8b 80 7f\n" +
      "af b8 e0 a3 df c7 37 72 3e e6 b4 b7 d9 3a 25 84\n" +
      "ee 6a 64 9d 06 09 53 74 88 34 b2 45 45 98 39 4e\n" +
      "e0 aa b1 2d 7b 61 a5 1f 52 7a 9a 41 f6 c1 68 7f\n" +
      "e2 53 72 98 ca 2a 8f 59 46 f8 e5 fd 09 1d bd cb";

  private static final int E = 0x11; // public exponent

  private static final String PRIME_FACTOR =
      "c9 7f b1 f0 27 f4 53 f6 34 12 33 ea aa d1 d9 35\n" +
      "3f 6c 42 d0 88 66 b1 d0 5a 0f 20 35 02 8b 9d 86\n" +
      "98 40 b4 16 66 b4 2e 92 ea 0d a3 b4 32 04 b5 cf\n" +
      "ce 33 52 52 4d 04 16 a5 a4 41 e7 00 af 46 15 03";

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  @Before
  public void setUp() throws Exception {

  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testModule() throws SyntaxError.Exception, EvalException, InterruptedException {
    Module module = Module.create();
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = new StarlarkThread(mu, StarlarkSemantics.DEFAULT);
      Starlark.execFile(ParserInput.fromLines("True = 123"), FileOptions.DEFAULT, module, thread);
    }
    Truth.assertThat(module.getGlobal("True")).isEqualTo(StarlarkInt.of(123));
  }

  @Test
  public void testMutability() {
    Mutability mutability = Mutability.create("test");
    StarlarkList<String> list = StarlarkList.newList(mutability);
  }

  @Test
  public void RSA_generate() throws EvalException, InvalidCipherTextException {
    Dict<String, StarlarkInt> rsaObj = null, finalRsaObj;
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread thread = new StarlarkThread(mu, StarlarkSemantics.DEFAULT);
      rsaObj = CryptoPublicKeyModule.INSTANCE.RSA_generate(StarlarkInt.of(1024), null, thread);
    } catch (EvalException e) {
      fail(e.getMessageWithStack());
    }
    assertNotNull(rsaObj);
    finalRsaObj = rsaObj;
    "nedpqu".chars().forEachOrdered(i -> {
      assertTrue(finalRsaObj.containsKey(String.valueOf((char) i)));
    });

    BigInteger p = rsaObj.get("p").toBigInteger();
    BigInteger q = rsaObj.get("q").toBigInteger();
    BigInteger n = rsaObj.get("n").toBigInteger();
    BigInteger d = rsaObj.get("d").toBigInteger();
    BigInteger e = rsaObj.get("e").toBigInteger();
    BigInteger u = rsaObj.get("u").toBigInteger();
    assertEquals(n, p.multiply(q)); // Sanity check key data
//    self.assertEqual(1, rsaObj.p > 1)   # p > 1
    assertEquals(p.compareTo(BigInteger.ONE), 1);
//    self.assertEqual(1, rsaObj.q > 1)   # q > 1
    assertEquals(q.compareTo(BigInteger.ONE), 1);
//    self.assertEqual(1, rsaObj.e > 1)   # e > 1
    assertEquals(e.compareTo(BigInteger.ONE), 1);
//    self.assertEqual(1, rsaObj.d > 1)   # d > 1
    assertEquals(d.compareTo(BigInteger.ONE), 1);
//    lcm = int(Integer(rsaObj.p-1).lcm(rsaObj.q-1))
//    self.assertEqual(1, rsaObj.d * rsaObj.e % lcm) # ed = 1 (mod LCM(p-1, q-1))
    BigInteger lcm = NumOpsUtils.lcm(p.subtract(BigInteger.ONE), q.subtract(BigInteger.ONE));
    assertEquals(d.multiply(e).mod(lcm), BigInteger.ONE);
//    self.assertEqual(1, rsaObj.p * rsaObj.u % rsaObj.q) # pu = 1 (mod q)
    assertEquals(p.multiply(u).mod(q), BigInteger.ONE);
//    self._exercise_primitive(rsaObj)
/*
        # Since we're using a randomly-generated key, we can't check the test
        # vector, but we can make sure encryption and decryption are inverse
        # operations.
        ciphertext = bytes_to_long(a2b_hex(self.ciphertext))
 */
    byte[] cipherInt = Hex.decode(ciphertext.replaceAll("\\s+", ""));
    //byte[] cipherInt = NumOpsUtils.bytes2bigint(s, true, false).toByteArray();
    /*
      Test decryption
            plaintext = rsaObj._decrypt(ciphertext)
     */
    byte[] plainTextInt = CryptoPublicKeyModule.INSTANCE.RSA_decrypt(finalRsaObj, cipherInt);
/*
        # Test encryption (2 arguments)
        new_ciphertext2 = rsaObj._encrypt(plaintext)
 */
    byte[] cipherInt2 = CryptoPublicKeyModule.INSTANCE.RSA_encrypt(finalRsaObj, plainTextInt);
    assertArrayEquals(cipherInt, cipherInt2);

//    pub = rsaObj.public_key()
    BigInteger pubKey = finalRsaObj.get("e").toBigInteger();
//    self._check_public_key(pub)
    Dict<String, StarlarkInt> pubRsaObj = Dict.<String, StarlarkInt>builder()
        .put("e", StarlarkInt.of(pubKey))
        .put("n", StarlarkInt.of(n))
        .buildImmutable();
    cipherInt2 = CryptoPublicKeyModule.INSTANCE.RSA_encrypt(pubRsaObj, plainTextInt);
//    self._exercise_public_primitive(rsaObj)
    assertArrayEquals(cipherInt, cipherInt2);
  }


  public static byte[] toPKCS8Format(byte[] encoded) throws IOException {
    final PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(encoded);
    final ASN1Encodable asn1Encodable = privateKeyInfo.parsePrivateKey();
    final ASN1Primitive asn1Primitive = asn1Encodable.toASN1Primitive();
    final byte[] privateKeyPKCS8Formatted = asn1Primitive.getEncoded(ASN1Encoding.DER);
    return privateKeyPKCS8Formatted;
  }

//  @Test
//  public void testSubjectPublicKeyInfo() throws IOException {
//
//    String rsaKeyDER =
//      "3082013b020100024100bf1e27900aa08b23511a5c1281ae6d93312c3efe" +
//      "913f932ebed492f12d16b4610c328cb6e208ab5f45acbe2950833298f312" +
//      "2c19f78492dedf40f0e3c190338502030100010240094483129f114dedf6" +
//      "7edabc2301bc5a88e5e6601dd7016220ead9fd4bfc6fdeb75893898ae41c" +
//      "54ddbdbf1539f8ccbd18f67b440de1ac30440281d40cfac839022100f20f" +
//      "2f3e1da61883f62980922bd8df545ce407c726241103b5e2c53723124a23" +
//      "022100ca1fe924792cfcc96bfab74f344a68b418df578338064806000fe2" +
//      "a5c99a023702210087be1c3029504bcf34ec713d877947447813288975ca" +
//      "240080af7b094091b12102206ab469fa6d5648a57531c8b031a4ce9db53b" +
//      "c3116cf433f5a6f6bbea5601ce05022100bd9f40a764227a21962a4add07" +
//      "e4defe43ed91a3ae27bb057f39241f33ab01c1"
//          .replace(" ", "");
//    System.out.println(Arrays.toString(toPKCS8Format(rsaKeyDER.getBytes(StandardCharsets.UTF_8))));
//  }

  @Test
  public void testImportTest2() throws IOException {

    String rsaKeyPEM = "-----BEGIN RSA PRIVATE KEY-----\n" +
        "MIIBOwIBAAJBAL8eJ5AKoIsjURpcEoGubZMxLD7+kT+TLr7UkvEtFrRhDDKMtuII\n" +
        "q19FrL4pUIMymPMSLBn3hJLe30Dw48GQM4UCAwEAAQJACUSDEp8RTe32ftq8IwG8\n" +
        "Wojl5mAd1wFiIOrZ/Uv8b963WJOJiuQcVN29vxU5+My9GPZ7RA3hrDBEAoHUDPrI\n" +
        "OQIhAPIPLz4dphiD9imAkivY31Rc5AfHJiQRA7XixTcjEkojAiEAyh/pJHks/Mlr\n" +
        "+rdPNEpotBjfV4M4BkgGAA/ipcmaAjcCIQCHvhwwKVBLzzTscT2HeUdEeBMoiXXK\n" +
        "JACAr3sJQJGxIQIgarRp+m1WSKV1MciwMaTOnbU7wxFs9DP1pva76lYBzgUCIQC9\n" +
        "n0CnZCJ6IZYqSt0H5N7+Q+2Ro64nuwV/OSQfM6sBwQ==\n" +
        "-----END RSA PRIVATE KEY-----";
    StarlarkList<?> rsaObj = null;
    try (Mutability mu = Mutability.create("test")) {
          StarlarkThread thread = new StarlarkThread(mu, StarlarkSemantics.DEFAULT);
      rsaObj= CryptoPublicKeyModule.INSTANCE.PEM_decode(rsaKeyPEM, null, thread);
    } catch (EvalException e) {
      fail(e.getMessageWithStack());
    }

    assertNotNull(rsaObj);
  }

  @Test
  public void RSA_import_key() {
    // # PEM encryption
    //# With DES and passphrase 'test'
    String[][] fixture = {
        {
            "test",
            "-----BEGIN RSA PRIVATE KEY-----\n" +
            "Proc-Type: 4,ENCRYPTED\n" +
            "DEK-Info: DES-CBC,AF8F9A40BD2FA2FC\n" +
            "\n" +
            "Ckl9ex1kaVEWhYC2QBmfaF+YPiR4NFkRXA7nj3dcnuFEzBnY5XULupqQpQI3qbfA\n" +
            "u8GYS7+b3toWWiHZivHbAAUBPDIZG9hKDyB9Sq2VMARGsX1yW1zhNvZLIiVJzUHs\n" +
            "C6NxQ1IJWOXzTew/xM2I26kPwHIvadq+/VaT8gLQdjdH0jOiVNaevjWnLgrn1mLP\n" +
            "BCNRMdcexozWtAFNNqSzfW58MJL2OdMi21ED184EFytIc1BlB+FZiGZduwKGuaKy\n" +
            "9bMbdb/1PSvsSzPsqW7KSSrTw6MgJAFJg6lzIYvR5F4poTVBxwBX3+EyEmShiaNY\n" +
            "IRX3TgQI0IjrVuLmvlZKbGWP18FXj7I7k9tSsNOOzllTTdq3ny5vgM3A+ynfAaxp\n" +
            "dysKznQ6P+IoqML1WxAID4aGRMWka+uArOJ148Rbj9s=\n" +
            "-----END RSA PRIVATE KEY-----"
        },
        {
            // PKCS8 encryption
            "winter",
            "-----BEGIN ENCRYPTED PRIVATE KEY-----\n" +
            "MIIBpjBABgkqhkiG9w0BBQ0wMzAbBgkqhkiG9w0BBQwwDgQIeZIsbW3O+JcCAggA\n" +
            "MBQGCCqGSIb3DQMHBAgSM2p0D8FilgSCAWBhFyP2tiGKVpGj3mO8qIBzinU60ApR\n" +
            "3unvP+N6j7LVgnV2lFGaXbJ6a1PbQXe+2D6DUyBLo8EMXrKKVLqOMGkFMHc0UaV6\n" +
            "R6MmrsRDrbOqdpTuVRW+NVd5J9kQQh4xnfU/QrcPPt7vpJvSf4GzG0n666Ki50OV\n" +
            "M/feuVlIiyGXY6UWdVDpcOV72cq02eNUs/1JWdh2uEBvA9fCL0c07RnMrdT+CbJQ\n" +
            "NjJ7f8ULtp7xvR9O3Al/yJ4Wv3i4VxF1f3MCXzhlUD4I0ONlr0kJWgeQ80q/cWhw\n" +
            "ntvgJwnCn2XR1h6LA8Wp+0ghDTsL2NhJpWd78zClGhyU4r3hqu1XDjoXa7YCXCix\n" +
            "jCV15+ViDJzlNCwg+W6lRg18sSLkCT7alviIE0U5tHc6UPbbHwT5QqAxAABaP+nZ\n" +
            "CGqJGyiwBzrKebjgSm/KRd4C91XqcsysyH2kKPfT51MLAoD4xelOURBP\n" +
            "-----END ENCRYPTED PRIVATE KEY-----"
        }
    };
    SSLUtils sslUtils = new SSLUtils();
    KeyPair keyPair;

    keyPair = sslUtils.decodePrivKey(fixture[0][1], fixture[0][0]);
    assertNotNull(keyPair);
    //TODO: skip
    //keyPair = sslUtils.decodePrivKey(fixture[1][1], fixture[1][0]);
    //assertNotNull(keyPair);
  }
}
