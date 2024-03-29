package com.verygood.security.larky.modules.crypto;

import static org.junit.Assert.assertArrayEquals;

import com.google.common.base.Strings;

import com.verygood.security.larky.modules.crypto.Cipher.Engine;
import com.verygood.security.larky.modules.crypto.Cipher.LarkyBlockCipher;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkBytes.StarlarkByteArray;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;

import org.bouncycastle.util.encoders.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CryptoCipherModuleTest {

  private static Mutability mutability;
  private static StarlarkThread thread;


  @Before
  public void setUp() throws Exception {
    mutability = Mutability.create("CryptoCipherModuleTest");
    thread = new StarlarkThread(mutability, StarlarkSemantics.DEFAULT);
  }

  @After
  public void tearDown() throws Exception {
    mutability.close();
  }

  @Test
  public void testDesCbc() throws EvalException {
    byte[] key = Hex.decode("0102030405060708");
    byte[] icv = new byte[8];
    byte[] src = Hex.decode("01020304050607080102030405060708");
    byte[] encryptResult = Hex.decode("77A7D6BCF57962B9DE153505D3821AFC");

//
//    StarlarkBytes bKey = StarlarkBytes.builder(thread).setSequence(key).build();
//    StarlarkBytes bIV = StarlarkBytes.builder(thread).setSequence(icv).build();
//    StarlarkBytes bEncResult = StarlarkBytes.builder(thread).setSequence(encryptResult).build();

    StarlarkBytes bKey = StarlarkBytes.of(thread.mutability(), key);
    StarlarkBytes bIV = StarlarkBytes.of(thread.mutability(), icv);
    StarlarkBytes bEncResult = StarlarkBytes.of(thread.mutability(), encryptResult);
    StarlarkByteArray out = StarlarkByteArray.of(thread.mutability());
//    StarlarkBytes out = (StarlarkBytes) StarlarkBytes.builder(thread)
//        .setSequence(new byte[src.length])
//        .build();
    Engine des = CryptoCipherModule.INSTANCE.DES(bKey);
    LarkyBlockCipher larkyBlockCipher = CryptoCipherModule.INSTANCE.CBCMode(des, bIV);

    larkyBlockCipher.decrypt(bEncResult, out, thread);
    assertArrayEquals(src, out.toByteArray());
  }


  @Test
  public void testDESVectors() throws EvalException {
    // # This is a list of (plaintext, ciphertext, key, description) tuples.
    byte[] icv = new byte[8];
    StarlarkBytes bIV = StarlarkBytes.of(thread.mutability(), icv);
//    StarlarkBytes bIV = StarlarkBytes.builder(thread).setSequence(icv).build();
    String SP800_17_B1_KEY = Strings.repeat("01", 8);
    String SP800_17_B2_PT = Strings.repeat("00", 8);

    String[][] test_data = {
        /*
          Test vectors from Appendix A of NIST SP 800-17
          "Modes of Operation Validation System (MOVS): Requirements and Procedures"
          http://csrc.nist.gov/publications/nistpubs/800-17/800-17.pdf

          Appendix A - "Sample Round Outputs for the DES"
         */
        {"0000000000000000", "82dcbafbdeab6602", "10316e028c8f3b4a", "NIST SP800-17 A"},

        // Table B.1 - Variable Plaintext Known Answer Test
        {"8000000000000000", "95f8a5e5dd31d900", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #0"},
        {"4000000000000000", "dd7f121ca5015619", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #1"},
        {"2000000000000000", "2e8653104f3834ea", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #2"},
        {"1000000000000000", "4bd388ff6cd81d4f", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #3"},
        {"0800000000000000", "20b9e767b2fb1456", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #4"},
        {"0400000000000000", "55579380d77138ef", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #5"},
        {"0200000000000000", "6cc5defaaf04512f", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #6"},
        {"0100000000000000", "0d9f279ba5d87260", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #7"},
        {"0080000000000000", "d9031b0271bd5a0a", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #8"},
        {"0040000000000000", "424250b37c3dd951", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #9"},
        {"0020000000000000", "b8061b7ecd9a21e5", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #10"},
        {"0010000000000000", "f15d0f286b65bd28", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #11"},
        {"0008000000000000", "add0cc8d6e5deba1", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #12"},
        {"0004000000000000", "e6d5f82752ad63d1", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #13"},
        {"0002000000000000", "ecbfe3bd3f591a5e", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #14"},
        {"0001000000000000", "f356834379d165cd", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #15"},
        {"0000800000000000", "2b9f982f20037fa9", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #16"},
        {"0000400000000000", "889de068a16f0be6", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #17"},
        {"0000200000000000", "e19e275d846a1298", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #18"},
        {"0000100000000000", "329a8ed523d71aec", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #19"},
        {"0000080000000000", "e7fce22557d23c97", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #20"},
        {"0000040000000000", "12a9f5817ff2d65d", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #21"},
        {"0000020000000000", "a484c3ad38dc9c19", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #22"},
        {"0000010000000000", "fbe00a8a1ef8ad72", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #23"},
        {"0000008000000000", "750d079407521363", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #24"},
        {"0000004000000000", "64feed9c724c2faf", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #25"},
        {"0000002000000000", "f02b263b328e2b60", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #26"},
        {"0000001000000000", "9d64555a9a10b852", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #27"},
        {"0000000800000000", "d106ff0bed5255d7", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #28"},
        {"0000000400000000", "e1652c6b138c64a5", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #29"},
        {"0000000200000000", "e428581186ec8f46", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #30"},
        {"0000000100000000", "aeb5f5ede22d1a36", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #31"},
        {"0000000080000000", "e943d7568aec0c5c", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #32"},
        {"0000000040000000", "df98c8276f54b04b", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #33"},
        {"0000000020000000", "b160e4680f6c696f", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #34"},
        {"0000000010000000", "fa0752b07d9c4ab8", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #35"},
        {"0000000008000000", "ca3a2b036dbc8502", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #36"},
        {"0000000004000000", "5e0905517bb59bcf", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #37"},
        {"0000000002000000", "814eeb3b91d90726", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #38"},
        {"0000000001000000", "4d49db1532919c9f", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #39"},
        {"0000000000800000", "25eb5fc3f8cf0621", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #40"},
        {"0000000000400000", "ab6a20c0620d1c6f", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #41"},
        {"0000000000200000", "79e90dbc98f92cca", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #42"},
        {"0000000000100000", "866ecedd8072bb0e", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #43"},
        {"0000000000080000", "8b54536f2f3e64a8", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #44"},
        {"0000000000040000", "ea51d3975595b86b", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #45"},
        {"0000000000020000", "caffc6ac4542de31", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #46"},
        {"0000000000010000", "8dd45a2ddf90796c", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #47"},
        {"0000000000008000", "1029d55e880ec2d0", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #48"},
        {"0000000000004000", "5d86cb23639dbea9", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #49"},
        {"0000000000002000", "1d1ca853ae7c0c5f", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #50"},
        {"0000000000001000", "ce332329248f3228", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #51"},
        {"0000000000000800", "8405d1abe24fb942", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #52"},
        {"0000000000000400", "e643d78090ca4207", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #53"},
        {"0000000000000200", "48221b9937748a23", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #54"},
        {"0000000000000100", "dd7c0bbd61fafd54", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #55"},
        {"0000000000000080", "2fbc291a570db5c4", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #56"},
        {"0000000000000040", "e07c30d7e4e26e12", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #57"},
        {"0000000000000020", "0953e2258e8e90a1", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #58"},
        {"0000000000000010", "5b711bc4ceebf2ee", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #59"},
        {"0000000000000008", "cc083f1e6d9e85f6", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #60"},
        {"0000000000000004", "d2fd8867d50d2dfe", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #61"},
        {"0000000000000002", "06e7ea22ce92708f", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #62"},
        {"0000000000000001", "166b40b44aba4bd6", SP800_17_B1_KEY,
            "NIST SP800-17 B.1 #63"},

        // Table B.2 - Variable Key Known Answer Test
        {SP800_17_B2_PT, "95a8d72813daa94d", "8001010101010101",
            "NIST SP800-17 B.2 #0"},
        {SP800_17_B2_PT, "0eec1487dd8c26d5", "4001010101010101",
            "NIST SP800-17 B.2 #1"},
        {SP800_17_B2_PT, "7ad16ffb79c45926", "2001010101010101",
            "NIST SP800-17 B.2 #2"},
        {SP800_17_B2_PT, "d3746294ca6a6cf3", "1001010101010101",
            "NIST SP800-17 B.2 #3"},
        {SP800_17_B2_PT, "809f5f873c1fd761", "0801010101010101",
            "NIST SP800-17 B.2 #4"},
        {SP800_17_B2_PT, "c02faffec989d1fc", "0401010101010101",
            "NIST SP800-17 B.2 #5"},
        {SP800_17_B2_PT, "4615aa1d33e72f10", "0201010101010101",
            "NIST SP800-17 B.2 #6"},
        {SP800_17_B2_PT, "2055123350c00858", "0180010101010101",
            "NIST SP800-17 B.2 #7"},
        {SP800_17_B2_PT, "df3b99d6577397c8", "0140010101010101",
            "NIST SP800-17 B.2 #8"},
        {SP800_17_B2_PT, "31fe17369b5288c9", "0120010101010101",
            "NIST SP800-17 B.2 #9"},
        {SP800_17_B2_PT, "dfdd3cc64dae1642", "0110010101010101",
            "NIST SP800-17 B.2 #10"},
        {SP800_17_B2_PT, "178c83ce2b399d94", "0108010101010101",
            "NIST SP800-17 B.2 #11"},
        {SP800_17_B2_PT, "50f636324a9b7f80", "0104010101010101",
            "NIST SP800-17 B.2 #12"},
        {SP800_17_B2_PT, "a8468ee3bc18f06d", "0102010101010101",
            "NIST SP800-17 B.2 #13"},
        {SP800_17_B2_PT, "a2dc9e92fd3cde92", "0101800101010101",
            "NIST SP800-17 B.2 #14"},
        {SP800_17_B2_PT, "cac09f797d031287", "0101400101010101",
            "NIST SP800-17 B.2 #15"},
        {SP800_17_B2_PT, "90ba680b22aeb525", "0101200101010101",
            "NIST SP800-17 B.2 #16"},
        {SP800_17_B2_PT, "ce7a24f350e280b6", "0101100101010101",
            "NIST SP800-17 B.2 #17"},
        {SP800_17_B2_PT, "882bff0aa01a0b87", "0101080101010101",
            "NIST SP800-17 B.2 #18"},
        {SP800_17_B2_PT, "25610288924511c2", "0101040101010101",
            "NIST SP800-17 B.2 #19"},
        {SP800_17_B2_PT, "c71516c29c75d170", "0101020101010101",
            "NIST SP800-17 B.2 #20"},
        {SP800_17_B2_PT, "5199c29a52c9f059", "0101018001010101",
            "NIST SP800-17 B.2 #21"},
        {SP800_17_B2_PT, "c22f0a294a71f29f", "0101014001010101",
            "NIST SP800-17 B.2 #22"},
        {SP800_17_B2_PT, "ee371483714c02ea", "0101012001010101",
            "NIST SP800-17 B.2 #23"},
        {SP800_17_B2_PT, "a81fbd448f9e522f", "0101011001010101",
            "NIST SP800-17 B.2 #24"},
        {SP800_17_B2_PT, "4f644c92e192dfed", "0101010801010101",
            "NIST SP800-17 B.2 #25"},
        {SP800_17_B2_PT, "1afa9a66a6df92ae", "0101010401010101",
            "NIST SP800-17 B.2 #26"},
        {SP800_17_B2_PT, "b3c1cc715cb879d8", "0101010201010101",
            "NIST SP800-17 B.2 #27"},
        {SP800_17_B2_PT, "19d032e64ab0bd8b", "0101010180010101",
            "NIST SP800-17 B.2 #28"},
        {SP800_17_B2_PT, "3cfaa7a7dc8720dc", "0101010140010101",
            "NIST SP800-17 B.2 #29"},
        {SP800_17_B2_PT, "b7265f7f447ac6f3", "0101010120010101",
            "NIST SP800-17 B.2 #30"},
        {SP800_17_B2_PT, "9db73b3c0d163f54", "0101010110010101",
            "NIST SP800-17 B.2 #31"},
        {SP800_17_B2_PT, "8181b65babf4a975", "0101010108010101",
            "NIST SP800-17 B.2 #32"},
        {SP800_17_B2_PT, "93c9b64042eaa240", "0101010104010101",
            "NIST SP800-17 B.2 #33"},
        {SP800_17_B2_PT, "5570530829705592", "0101010102010101",
            "NIST SP800-17 B.2 #34"},
        {SP800_17_B2_PT, "8638809e878787a0", "0101010101800101",
            "NIST SP800-17 B.2 #35"},
        {SP800_17_B2_PT, "41b9a79af79ac208", "0101010101400101",
            "NIST SP800-17 B.2 #36"},
        {SP800_17_B2_PT, "7a9be42f2009a892", "0101010101200101",
            "NIST SP800-17 B.2 #37"},
        {SP800_17_B2_PT, "29038d56ba6d2745", "0101010101100101",
            "NIST SP800-17 B.2 #38"},
        {SP800_17_B2_PT, "5495c6abf1e5df51", "0101010101080101",
            "NIST SP800-17 B.2 #39"},
        {SP800_17_B2_PT, "ae13dbd561488933", "0101010101040101",
            "NIST SP800-17 B.2 #40"},
        {SP800_17_B2_PT, "024d1ffa8904e389", "0101010101020101",
            "NIST SP800-17 B.2 #41"},
        {SP800_17_B2_PT, "d1399712f99bf02e", "0101010101018001",
            "NIST SP800-17 B.2 #42"},
        {SP800_17_B2_PT, "14c1d7c1cffec79e", "0101010101014001",
            "NIST SP800-17 B.2 #43"},
        {SP800_17_B2_PT, "1de5279dae3bed6f", "0101010101012001",
            "NIST SP800-17 B.2 #44"},
        {SP800_17_B2_PT, "e941a33f85501303", "0101010101011001",
            "NIST SP800-17 B.2 #45"},
        {SP800_17_B2_PT, "da99dbbc9a03f379", "0101010101010801",
            "NIST SP800-17 B.2 #46"},
        {SP800_17_B2_PT, "b7fc92f91d8e92e9", "0101010101010401",
            "NIST SP800-17 B.2 #47"},
        {SP800_17_B2_PT, "ae8e5caa3ca04e85", "0101010101010201",
            "NIST SP800-17 B.2 #48"},
        {SP800_17_B2_PT, "9cc62df43b6eed74", "0101010101010180",
            "NIST SP800-17 B.2 #49"},
        {SP800_17_B2_PT, "d863dbb5c59a91a0", "0101010101010140",
            "NIST SP800-17 B.2 #50"},
        {SP800_17_B2_PT, "a1ab2190545b91d7", "0101010101010120",
            "NIST SP800-17 B.2 #51"},
        {SP800_17_B2_PT, "0875041e64c570f7", "0101010101010110",
            "NIST SP800-17 B.2 #52"},
        {SP800_17_B2_PT, "5a594528bebef1cc", "0101010101010108",
            "NIST SP800-17 B.2 #53"},
        {SP800_17_B2_PT, "fcdb3291de21f0c0", "0101010101010104",
            "NIST SP800-17 B.2 #54"},
        {SP800_17_B2_PT, "869efd7f9f265a09", "0101010101010102",
            "NIST SP800-17 B.2 #55"}
    };

    for(String[] tc : test_data) {
//      StarlarkBytes bEncResult = StarlarkBytes.builder(thread).setSequence(Hex.decode(tc[1])).build();
//      StarlarkBytes bKey = StarlarkBytes.builder(thread).setSequence(Hex.decode(tc[2])).build();
      StarlarkBytes bEncResult = StarlarkBytes.of(thread.mutability(), Hex.decode(tc[1]));
      StarlarkBytes bKey = StarlarkBytes.of(thread.mutability(), Hex.decode(tc[2]));
//      StarlarkBytes out = StarlarkBytes.of(thread.mutability(), new byte[bEncResult.size()]);
//      out.clear();
//      StarlarkBytes out = (StarlarkBytes) StarlarkBytes.builder(thread)
      StarlarkByteArray out = StarlarkByteArray.of(thread.mutability());

      Engine des = CryptoCipherModule.INSTANCE.DES(bKey);
      LarkyBlockCipher larkyBlockCipher = CryptoCipherModule.INSTANCE.CBCMode(des, bIV);
      larkyBlockCipher.decrypt(bEncResult, out, thread);
      assertArrayEquals(Hex.decode(tc[0]), out.toByteArray());
    }
  }
}