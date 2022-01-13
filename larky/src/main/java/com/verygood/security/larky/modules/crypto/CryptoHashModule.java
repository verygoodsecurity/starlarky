package com.verygood.security.larky.modules.crypto;

import com.verygood.security.larky.modules.crypto.Hash.LarkyDigest;
import com.verygood.security.larky.modules.crypto.Hash.LarkyGeneralDigest;
import com.verygood.security.larky.modules.crypto.Hash.LarkyLongDigest;
import com.verygood.security.larky.modules.crypto.Hash.LarkyXofDigest;
import com.verygood.security.larky.modules.crypto.Hash.LarkyKeccakDigest;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.EvalException;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;

import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.crypto.digests.Blake2sDigest;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.crypto.digests.GeneralDigest;
import org.bouncycastle.crypto.digests.LongDigest;
import org.bouncycastle.crypto.digests.SHAKEDigest;
import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.util.DigestFactory;
import org.bouncycastle.crypto.digests.MD2Digest;
import org.bouncycastle.crypto.digests.MD4Digest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;

public class CryptoHashModule implements StarlarkValue {

  public static final CryptoHashModule INSTANCE = new CryptoHashModule();

  @StarlarkMethod(name = "MD2")
  public LarkyDigest MD2() {
    MD2Digest digest = new MD2Digest();
    
    return new LarkyDigest() {
      @Override
      public ExtendedDigest getDigest() {
        return digest;
      }
    };
  }

  @StarlarkMethod(name = "MD4")
  public LarkyDigest MD4() {
    MD4Digest digest = new MD4Digest();
    
    return new LarkyDigest() {
      @Override
      public ExtendedDigest getDigest() {
        return digest;
      }
    };
  }

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
  public LarkyLongDigest SHA384() {
    LongDigest digest = (LongDigest) DigestFactory.createSHA384();
    return new LarkyLongDigest(digest);
  }

  @StarlarkMethod(name = "SHA512", parameters = {
          @Param(name = "truncate", allowedTypes = {@ParamType(type = String.class)},
                  defaultValue = "512"),
  })
  public LarkyLongDigest SHA512(String truncate) throws EvalException {
    LongDigest digest;
    switch (truncate) {
      case "512":
        digest = (LongDigest) DigestFactory.createSHA512();
        break;
      case "224":
        digest = (LongDigest) DigestFactory.createSHA512_224();
        break;
      case "256":
        digest = (LongDigest) DigestFactory.createSHA512_256();
        break;
      default:
        throw Starlark.errorf("Incorrect truncation length. It must be 224, 256 or 512.");
    }
    return new LarkyLongDigest(digest);
  }

  @StarlarkMethod(name = "Keccak", parameters = {
          @Param(name = "digest_bits", allowedTypes = {@ParamType(type = StarlarkInt.class)})
  })
  public LarkyKeccakDigest Keccak(StarlarkInt digest_bits) {
    return new LarkyKeccakDigest(new KeccakDigest(digest_bits.toIntUnchecked()));
  }

  @StarlarkMethod(name = "SHA3_224")
  public LarkyKeccakDigest SHA3_224() {
    KeccakDigest digest = (KeccakDigest) DigestFactory.createSHA3_224();
    return new LarkyKeccakDigest(digest);
  }

  @StarlarkMethod(name = "SHA3_256")
  public LarkyKeccakDigest SHA3_256() {
    KeccakDigest digest = (KeccakDigest) DigestFactory.createSHA3_256();
    return new LarkyKeccakDigest(digest);
  }

  @StarlarkMethod(name = "SHA3_384")
  public LarkyKeccakDigest SHA3_384() {
    KeccakDigest digest = (KeccakDigest) DigestFactory.createSHA3_384();
    return new LarkyKeccakDigest(digest);
  }

  @StarlarkMethod(name = "SHA3_512")
  public LarkyKeccakDigest SHA3_512() {
    KeccakDigest digest = (KeccakDigest) DigestFactory.createSHA3_512();
    return new LarkyKeccakDigest(digest);
  }

  @StarlarkMethod(name = "SHAKE128", parameters = {
      @Param(name = "bit_length", allowedTypes = {@ParamType(type = StarlarkInt.class)},
          defaultValue = "128"),
  })
  public LarkyXofDigest<?> SHAKE(StarlarkInt bitLength) throws EvalException {
    int length = bitLength.toIntUnchecked();
    if (length != 128 && length != 256){
      throw Starlark.errorf("Incorrect bits length. It must be 128 or 256.");
    }
    SHAKEDigest digest = new SHAKEDigest(bitLength.toIntUnchecked());
    return new LarkyXofDigest<>(digest);
  }

  @StarlarkMethod(name = "BLAKE2S", parameters = {
      @Param(name = "digest_bytes",
          allowedTypes = {@ParamType(type = StarlarkInt.class)},
          defaultValue = "32"),
      @Param(name = "key",
          allowedTypes = {@ParamType(type = StarlarkBytes.class), @ParamType(type = NoneType.class)},
          defaultValue = "None"),
  })
  public LarkyDigest BLAKE2S(StarlarkInt digestBytes, Object keyO) {
    byte[] key = Starlark.isNullOrNone(keyO)
        ? null
        : ((StarlarkBytes) keyO).toByteArray();
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

  @StarlarkMethod(name = "BLAKE2B", parameters = {
    @Param(name = "digest_bytes",
        allowedTypes = {@ParamType(type = StarlarkInt.class)},
        defaultValue = "64"),
    @Param(name = "key",
        allowedTypes = {@ParamType(type = StarlarkBytes.class), @ParamType(type = NoneType.class)},
        defaultValue = "None"),
  })
  public LarkyDigest BLAKE2B(StarlarkInt digestBytes, Object keyO) {
    byte[] key = Starlark.isNullOrNone(keyO) ? null : ((StarlarkBytes) keyO).toByteArray();
    Blake2bDigest digest;
    if(key != null) {
      digest = new Blake2bDigest(key, digestBytes.toIntUnchecked(), null, null);
    }
    else {
      digest = new Blake2bDigest(digestBytes.toIntUnchecked() * Byte.SIZE);
    }

    return new LarkyDigest(){
      @Override
      public ExtendedDigest getDigest() {
        return digest;
      }
    };
  }

@StarlarkMethod(name = "RIPEMD160")
public LarkyDigest RIPEMD160(){
    RIPEMD160Digest digest = new RIPEMD160Digest();
      
    return new LarkyDigest(){
      @Override
      public ExtendedDigest getDigest() {
        return digest;
      }
    };
  }
  
}
