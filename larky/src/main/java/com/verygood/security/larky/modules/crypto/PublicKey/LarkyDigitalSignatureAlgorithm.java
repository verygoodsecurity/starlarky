package com.verygood.security.larky.modules.crypto.PublicKey;

import java.security.SecureRandom;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.DSAKeyPairGenerator;
import org.bouncycastle.crypto.generators.DSAParametersGenerator;
import org.bouncycastle.crypto.params.DSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.DSAParameterGenerationParameters;
import org.bouncycastle.crypto.params.DSAParameters;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.util.PrimeCertaintyCalculator;

public class LarkyDigitalSignatureAlgorithm implements StarlarkValue {
  public static LarkyDigitalSignatureAlgorithm INSTANCE = new LarkyDigitalSignatureAlgorithm();

  @StarlarkMethod(
    name = "generate",
    parameters = {
      @Param(name = "bits"),
      @Param(name = "randfunc", defaultValue = "None"),
      @Param(name = "domain", allowedTypes = {
        @ParamType(type= Tuple.class),
        @ParamType(type= NoneType.class)
      }, defaultValue = "None"),
    }, useStarlarkThread = true)
  public Dict<String, StarlarkInt> DSA_generate(StarlarkInt bits_, Object randomFuncO, Object domainO, StarlarkThread thread) throws EvalException {
    int bits = bits_.toIntUnchecked();
    if (bits != 1024 && bits != 2048 && bits != 3072) {
      throw Starlark.errorf("ValueError: Invalid modulus length (%d). Must be one of: 1024, 2048, or 3072.", bits);
    }
    SecureRandom secureRandom = CryptoServicesRegistrar.getSecureRandom();
    DSAKeyPairGenerator dsaKeyPairGenerator = new DSAKeyPairGenerator();
    DSAKeyGenerationParameters dsaKeyGenerationParameters;
    if(!Starlark.isNullOrNone(domainO)) {
      Tuple domain = (Tuple) domainO;
      dsaKeyGenerationParameters = new DSAKeyGenerationParameters(
        secureRandom,
        new DSAParameters(
          ((StarlarkInt) domain.get(0)).toBigInteger(),
          ((StarlarkInt) domain.get(1)).toBigInteger(),
          ((StarlarkInt) domain.get(2)).toBigInteger()
        )
      );
    } else {
      final DSAParametersGenerator dsaParametersGenerator = new DSAParametersGenerator(new SHA256Digest());
      final int N;
      switch(bits) {
        case 1024:
          N = 160;
          break;
        case 2048:
          N = 224;
          break;
        case 3072:
          N = 256;
          break;
        default:
          throw Starlark.errorf("Should absolutely never get here!");
      }
      DSAParameterGenerationParameters params = new DSAParameterGenerationParameters(bits, N, PrimeCertaintyCalculator.getDefaultCertainty(bits), secureRandom);
      dsaParametersGenerator.init(params);
      dsaKeyGenerationParameters =  new DSAKeyGenerationParameters(
        secureRandom,
        dsaParametersGenerator.generateParameters()
      );
    }
    dsaKeyPairGenerator.init(dsaKeyGenerationParameters);
    AsymmetricCipherKeyPair asymKeyPair;
    try {
      asymKeyPair = dsaKeyPairGenerator.generateKeyPair();
    }catch(IllegalArgumentException ex) {
      throw Starlark.errorf("ValueError: Invalid DSA domain parameters (%s)", ex.getMessage());
    }
    /*
      y : integer
        Public key.
      g : integer
        Generator
      p : integer
        DSA modulus
      q : integer
        Order of the subgroup
      x : integer
        Private key.
     */
    DSAPublicKeyParameters pubKey = ((DSAPublicKeyParameters) asymKeyPair.getPublic());
    DSAPrivateKeyParameters privateKey = ((DSAPrivateKeyParameters) asymKeyPair.getPrivate());
    return Dict.<String, StarlarkInt>builder()
        .put("y", StarlarkInt.of(pubKey.getY()))
        .put("g", StarlarkInt.of(pubKey.getParameters().getG()))
        .put("p", StarlarkInt.of(pubKey.getParameters().getP()))
        .put("q", StarlarkInt.of(pubKey.getParameters().getQ()))
        .put("x", StarlarkInt.of(privateKey.getX()))
        .build(thread.mutability());
  }
}
