package com.verygood.security.larky.modules.crypto;

import com.verygood.security.larky.modules.crypto.Cipher.Engine;
import com.verygood.security.larky.modules.crypto.Cipher.GHash;
import com.verygood.security.larky.modules.crypto.Cipher.LarkyBlockCipher;
import com.verygood.security.larky.modules.crypto.Cipher.LarkyPKCS1;
import com.verygood.security.larky.modules.crypto.Cipher.LarkyStreamCipher;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.DESEngine;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.modes.CTRModeCipher;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.params.DESedeParameters;
import org.bouncycastle.crypto.params.KeyParameter;

public class CryptoCipherModule implements StarlarkValue {

  public static final CryptoCipherModule INSTANCE = new CryptoCipherModule();

  @StarlarkMethod(
      name="GHASH",
      parameters = {
          @Param(name = "data", allowedTypes = {@ParamType(type = StarlarkBytes.class)})
  })
  public GHash createGHASH(StarlarkBytes data) {
    return new GHash(data.toByteArray());
  }

  @StarlarkMethod(name = "CTRMode", parameters = {
      @Param(name = "engine", allowedTypes = {@ParamType(type = Engine.class)}),
      @Param(name = "iv", allowedTypes = {@ParamType(type = StarlarkBytes.class)})
  })
  public LarkyStreamCipher<SICBlockCipher> CTRMode(Engine engine, StarlarkBytes iv) {
    // SIC = Segmented Integer Counter
    // This mode is also known as CTR mode.
    return new LarkyStreamCipher<>((SICBlockCipher) SICBlockCipher.newInstance(engine.getEngine()), engine, iv.toByteArray());
  }

  @StarlarkMethod(name = "CBCMode", parameters = {
      @Param(name = "engine", allowedTypes = {@ParamType(type = Engine.class)}),
      @Param(name = "iv", allowedTypes = {@ParamType(type = StarlarkBytes.class)})
  })
  public LarkyBlockCipher CBCMode(Engine engine, StarlarkBytes iv) {
    return new LarkyBlockCipher(CBCBlockCipher.newInstance(engine.getEngine()), engine, iv.toByteArray());
  }

  @StarlarkMethod(name = "ECBMode", parameters = {
      @Param(name = "engine", allowedTypes = {@ParamType(type = Engine.class)})
  })
  public LarkyBlockCipher ECBMode(Engine engine) {
    return new LarkyBlockCipher(engine.getEngine(), engine, null);
  }

  @StarlarkMethod(name = "CFBMode", parameters = {
      @Param(name = "engine", allowedTypes = {@ParamType(type = Engine.class)}),
      @Param(name = "iv", allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
      @Param(name = "segment_size", allowedTypes = {@ParamType(type = StarlarkInt.class)})
  })
  public LarkyBlockCipher CFBMode(Engine engine, StarlarkBytes iv, StarlarkInt segmentSize) {
    return new LarkyBlockCipher(CFBBlockCipher.newInstance(engine.getEngine(), segmentSize.toIntUnchecked()), engine, iv.toByteArray());
  }

  @StarlarkMethod(name = "DES3", parameters = {
      @Param(name = "key", allowedTypes = {@ParamType(type = StarlarkBytes.class)})
  })
  public Engine DES3(StarlarkBytes key) throws EvalException {
    DESedeEngine deSede = new DESedeEngine();
    try {
      DESedeParameters params = new DESedeParameters(key.toByteArray());
      return new Engine(deSede, params);
    } catch(IllegalArgumentException e) {
      throw new EvalException(e.getMessage(), e);
    }
  }

  @StarlarkMethod(name = "DES", parameters = {
      @Param(name = "key", allowedTypes = {@ParamType(type = StarlarkBytes.class)})
    })
    public Engine DES(StarlarkBytes key) throws EvalException {
    DESEngine desEngine = new DESEngine();
      try {
        KeyParameter params = new KeyParameter(key.toByteArray());
        return new Engine(desEngine, params);
      } catch(IllegalArgumentException e) {
        throw new EvalException(e.getMessage(), e);
      }
    }

  @StarlarkMethod(name = "AES", parameters = {
      @Param(name = "key", allowedTypes = {@ParamType(type = StarlarkBytes.class)})
  })
  public Engine AES(StarlarkBytes key) throws EvalException {
    AESEngine aesEngine = (AESEngine) AESEngine.newInstance();
    try {
      KeyParameter params = new KeyParameter(key.toByteArray());
      return new Engine(aesEngine, params);
    } catch(IllegalArgumentException e) {
      throw new EvalException(e.getMessage(), e);
    }
  }

  @StarlarkMethod(name = "PKCS1", structField = true)
  public LarkyPKCS1 PKCS1() throws EvalException {
    return LarkyPKCS1.INSTANCE;
  }


}
