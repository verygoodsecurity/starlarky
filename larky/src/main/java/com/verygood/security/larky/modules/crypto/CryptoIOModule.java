package com.verygood.security.larky.modules.crypto;

import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

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
}
