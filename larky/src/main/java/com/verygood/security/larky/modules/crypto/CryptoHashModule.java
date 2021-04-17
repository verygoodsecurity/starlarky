package com.verygood.security.larky.modules.crypto;

import com.verygood.security.larky.modules.crypto.Hash.LarkyDigest;
import com.verygood.security.larky.modules.crypto.Hash.LarkyGeneralDigest;
import com.verygood.security.larky.modules.crypto.Hash.LarkyLongDigest;
import com.verygood.security.larky.modules.crypto.Hash.LarkyXofDigest;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.crypto.digests.Blake2sDigest;
import org.bouncycastle.crypto.digests.GeneralDigest;
import org.bouncycastle.crypto.digests.LongDigest;
import org.bouncycastle.crypto.digests.SHAKEDigest;
import org.bouncycastle.crypto.util.DigestFactory;

public class CryptoHashModule implements StarlarkValue {

  public static final CryptoHashModule INSTANCE = new CryptoHashModule();

  @StarlarkMethod(name = "MD5")
  public LarkyGeneralDigest MD5() {
    GeneralDigest digest = (GeneralDigest) DigestFactory.createMD5();
    return new LarkyGeneralDigest(digest);
  }

  @StarlarkMethod(name = "SHA1")
  public LarkyGeneralDigest SHA1() {
    GeneralDigest digest = (GeneralDigest) DigestFactory.createSHA1();
    return new LarkyGeneralDigest(digest);
  }

  @StarlarkMethod(name = "SHA224")
  public LarkyGeneralDigest SHA224() {
    GeneralDigest digest = (GeneralDigest) DigestFactory.createSHA224();
    return new LarkyGeneralDigest(digest);
  }

  @StarlarkMethod(name = "SHA256")
  public LarkyGeneralDigest SHA256() {
    GeneralDigest digest = (GeneralDigest) DigestFactory.createSHA256();
    return new LarkyGeneralDigest(digest);
  }

  @StarlarkMethod(name = "SHA384")
  public LarkyGeneralDigest SHA384() {
    GeneralDigest digest = (GeneralDigest) DigestFactory.createSHA384();
    return new LarkyGeneralDigest(digest);
  }

  @StarlarkMethod(name = "SHA512")
  public LarkyLongDigest SHA512() {
    LongDigest digest = (LongDigest) DigestFactory.createSHA512();
    return new LarkyLongDigest(digest);
  }

  @StarlarkMethod(name = "SHAKE128", parameters = {
      @Param(name = "bit_length", allowedTypes = {@ParamType(type = StarlarkInt.class)},
          defaultValue = "128"),
  })
  public LarkyXofDigest<?> SHAKE128(StarlarkInt bitLength) {
    SHAKEDigest digest = new SHAKEDigest(bitLength.toIntUnchecked());
    return new LarkyXofDigest<>(digest);
  }

  @StarlarkMethod(name = "BLAKE2S", parameters = {
      @Param(name = "digest_bytes",
          allowedTypes = {@ParamType(type = StarlarkInt.class)},
          defaultValue = "32"),
      @Param(name = "key",
          allowedTypes = {@ParamType(type = LarkyByteLike.class), @ParamType(type = NoneType.class)},
          defaultValue = "None"),
  })
  public LarkyDigest BLAKE2S(StarlarkInt digestBytes, Object keyO) {
    byte[] key = Starlark.isNullOrNone(keyO)
        ? null
        : ((LarkyByteLike) keyO).getBytes();
    Blake2sDigest digest;
    if(key != null) {
      digest = new Blake2sDigest(key, digestBytes.toIntUnchecked(), null, null);
    }
    else {
      digest = new Blake2sDigest(digestBytes.toIntUnchecked() * Byte.SIZE);
    }

    return new LarkyDigest() {
      @Override
      public ExtendedDigest getDigest() {
        return digest;
      }
    };
  }
}
