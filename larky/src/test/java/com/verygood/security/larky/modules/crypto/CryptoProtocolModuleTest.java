package com.verygood.security.larky.modules.crypto;

import static org.junit.Assert.assertArrayEquals;

import net.starlark.java.eval.EvalException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.Security;

public class CryptoProtocolModuleTest {

  @BeforeClass
    public static void beforeClass() {
      if(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
        Security.addProvider(new BouncyCastleProvider());
      }
    }

/*
  def t2b(t):
      if t is None:
          return None
      t2 = t.replace(" ", "").replace("\n", "")
      return unhexlify(b(t2))
  */
  public byte[] t2b(String t) {
    if(t == null) {
      return null;
    }
    String t2 = t.replace(" ", "").replace("\n", "");
    return Hex.decode(t2.getBytes(StandardCharsets.UTF_8));
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
        "password",
        t2b("78578E5A5D63CB06"),
        16,
        1000,
        "SHA");
    assertArrayEquals(bytes, t2b("DC19847E05C64D2FAF10EBFB4A3D2A20"));
  }
}