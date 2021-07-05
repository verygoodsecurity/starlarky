package com.verygood.security.larky.modules.crypto;

import static org.junit.Assert.assertArrayEquals;

import com.google.common.primitives.Bytes;
import java.nio.charset.StandardCharsets;
import java.security.Security;

import com.verygood.security.larky.modules.utils.ByteArrayUtil;

import net.starlark.java.eval.EvalException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.junit.BeforeClass;
import org.junit.Test;

public class CryptoProtocolModuleTest {

  @BeforeClass
    public static void beforeClass() {
      if(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
        Security.addProvider(new BouncyCastleProvider());
      }
    }

  public byte[] t2b(String t) {
    if(t == null) {
      return null;
    }
    String t2 = t.replace(" ", "").replace("\n", "");
    return Hex.decode(t2.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  public void PBKDF2() throws Exception {
    /**
      List of tuples with test data.
      Each tuple is made up by:
            Item #0: a pass phrase
            Item #1: salt (encoded in hex)
            Item #2: output key length
            Item #3: iterations to use
            Item #4: hash module
            Item #5: expected result (encoded in hex)
     */
    Object[][] _testData = new Object[][]{
      // From http://www.di-mgt.com.au/cryptoKDFs.html#examplespbkdf
      {"password","78578E5A5D63CB06", 24, 2048, "SHA1" ,"BFDE6BE94DF7E11DD409BCE20A0255EC327CB936FFE93643"},
      // From RFC 6050
      {"password", "73616c74", 20, 1, "SHA1" ,"0c60c80f961f0e71f3a9b524af6012062fe037a6"},
      {"password", "73616c74", 20, 2, "SHA1" ,"ea6c014dc72d6f8ccd1ed92ace1d41f0d8de8957"},
      {"password", "73616c74", 20, 4096, "SHA1", "4b007901b765489abead49d926f721d065a429c1"},
      {"passwordPASSWORDpassword","73616c7453414c5473616c7453414c5473616c7453414c5473616c7453414c5473616c74", 25, 4096, "SHA1" ,"3d2eec4fe41c849b80c8d83662c0e44a8b291a964cf2f07038"},
      {"pass\0word", "7361006c74", 16, 4096, "SHA1", "56fa6aa75548099dcc37d7f03425e0c3"},
      // From draft-josefsson-scrypt-kdf-01, Chapter 10
      {"passwd", "73616c74", 64, 1, "SHA256", "55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc49ca9cccf179b645991664b39d77ef317c71b845b1e30bd509112041d3a19783"},
      {"Password", "4e61436c", 64, 80000, "SHA256" ,"4ddcd8f60b98be21830cee5ef22701f9641a4418d04c0414aeff08876b34ab56a1d425a1225833549adb841b51c9b3176a272bdebba1d078478f62b397f33c8d"},
    };

    for (Object[] tcase : _testData) {
      byte[] bytes = CryptoProtocolModule.INSTANCE.PBKDF2(
        /*password*/ ((String) tcase[0]).toCharArray(),
        /*salt*/ t2b((String) tcase[1]),
        /*dkLen*/ (int) tcase[2],
        /*count*/ (int) tcase[3],
        /*prfO*/ null,
        /*hmacHashModuleO*/ tcase[4]
      );
      assertArrayEquals(bytes, t2b((String) tcase[5]));
    }
  }

  @Test
  public void PBKDF1() throws EvalException {
    /**
     *    # List of tuples with test data.
     *     # Each tuple is made up by:
     *     #       Item #0: a pass phrase
     *     #       Item #1: salt (8 bytes encoded in hex)
     *     #       Item #2: output key length
     *     #       Item #3: iterations to use
     *     #       Item #4: expected result (encoded in hex)
     *
     *     _testData = (
     *             # From http://www.di-mgt.com.au/cryptoKDFs.html#examplespbkdf
     *             ("password", "78578E5A5D63CB06", 16, 1000, "DC19847E05C64D2FAF10EBFB4A3D2A20"),
     *     )
     *
     *     def test1(self):
     *         v = self._testData[0]
     *         res = PBKDF1(v[0], t2b(v[1]), v[2], v[3], SHA1)
     *         self.assertEqual(res, t2b(v[4]))
     */
    byte[] bytes = CryptoProtocolModule.INSTANCE.PBKDF1(
        "password".toCharArray(),
        t2b("78578E5A5D63CB06"),
        16,
        1000,
        "SHA");
    assertArrayEquals(bytes, t2b("DC19847E05C64D2FAF10EBFB4A3D2A20"));
    byte[] key = CryptoProtocolModule.INSTANCE.PBKDF1(
        "test".toCharArray(),
        t2b("7876D1B9A17F7A0F"),
        16,
        1,
        "MD5"
    );
    assertArrayEquals(key, t2b("ef0e5f0b7de47c31cc54db84a329784c".toUpperCase()));
    byte[] joined = Bytes.concat(key, "test".getBytes(StandardCharsets.ISO_8859_1));
    assertArrayEquals(joined, t2b("ef0e5f0b7de47c31cc54db84a329784c74657374"));
    bytes = CryptoProtocolModule.INSTANCE.PBKDF1(
        ByteArrayUtil.bytesToChars(joined, StandardCharsets.ISO_8859_1),
        t2b("7876D1B9A17F7A0F"),
        8,
        1,
        "MD5"
    );
    assertArrayEquals(bytes, t2b("785bbf1440a606b6".toUpperCase()));
    key = Bytes.concat(key, bytes);
    assertArrayEquals(key, t2b("ef0e5f0b7de47c31cc54db84a329784c785bbf1440a606b6".toUpperCase()));
  }
}