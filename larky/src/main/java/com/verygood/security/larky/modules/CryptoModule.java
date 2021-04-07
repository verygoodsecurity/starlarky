package com.verygood.security.larky.modules;

import com.verygood.security.larky.modules.crypto.CryptoCipherModule;
import com.verygood.security.larky.modules.crypto.CryptoHashModule;
import com.verygood.security.larky.modules.crypto.CryptoIOModule;
import com.verygood.security.larky.modules.crypto.CryptoMathModule;
import com.verygood.security.larky.modules.crypto.CryptoProtocolModule;
import com.verygood.security.larky.modules.crypto.CryptoPublicKeyModule;
import com.verygood.security.larky.modules.crypto.CryptoRandomModule;
import com.verygood.security.larky.modules.crypto.CryptoSignatureModule;
import com.verygood.security.larky.modules.crypto.CryptoUtilModule;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;


@StarlarkBuiltin(
    name = "jcrypto",
    category = "BUILTIN",
    doc = ""
)
public class CryptoModule implements StarlarkValue {

  static {
    /*
    Name: SUN Version: 11.0
    Name: SunRsaSign Version: 11.0
    Name: SunEC Version: 11.0
    Name: SunJSSE Version: 11.0
    Name: SunJCE Version: 11.0
    Name: SunJGSS Version: 11.0
    Name: SunSASL Version: 11.0
    Name: XMLDSig Version: 11.0
    Name: SunPCSC Version: 11.0
    Name: JdkLDAP Version: 11.0
    Name: JdkSASL Version: 11.0
    Name: Apple Version: 11.0
    Name: SunPKCS11 Version: 11.0
     */
    Security.addProvider(new BouncyCastleProvider());
    /* uncomment the below line for a post-quantum provider */
    //    Security.addProvider(new BouncyCastlePQCProvider());
  }

  public static final CryptoModule INSTANCE = new CryptoModule();

  @StarlarkMethod(name="Cipher", structField = true)
  public CryptoCipherModule Cipher() { return CryptoCipherModule.INSTANCE; }

  @StarlarkMethod(name="Hash", structField = true)
  public CryptoHashModule Hash()  {
    return CryptoHashModule.INSTANCE;
  }

  @StarlarkMethod(name="IO", structField = true)
  public CryptoIOModule IO()  { return CryptoIOModule.INSTANCE; }

  @StarlarkMethod(name="Math", structField = true)
  public CryptoMathModule Math() { return CryptoMathModule.INSTANCE; }

  @StarlarkMethod(name="Protocol", structField = true)
  public CryptoProtocolModule Protocol()  { return CryptoProtocolModule.INSTANCE; }

  @StarlarkMethod(name="PublicKey", structField = true)
  public CryptoPublicKeyModule PublicKey()  { return CryptoPublicKeyModule.INSTANCE; }

  @StarlarkMethod(name="Random", structField = true)
  public CryptoRandomModule Random()  {
    return CryptoRandomModule.INSTANCE;
  }

  @StarlarkMethod(name="Signature", structField = true)
  public CryptoSignatureModule Signature()  { return CryptoSignatureModule.INSTANCE; }

  @StarlarkMethod(name="Util", structField = true)
  public CryptoUtilModule Util()  { return CryptoUtilModule.INSTANCE; }

}
