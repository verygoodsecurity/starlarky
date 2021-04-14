package com.verygood.security.larky.modules.crypto;

import com.google.common.base.Preconditions;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.crypto.Xof;
import org.bouncycastle.crypto.digests.GeneralDigest;
import org.bouncycastle.crypto.digests.KeccakDigest;
import org.bouncycastle.crypto.digests.LongDigest;
import org.bouncycastle.crypto.digests.SHAKEDigest;
import org.bouncycastle.crypto.util.DigestFactory;
import org.bouncycastle.util.encoders.Hex;

public class CryptoHashModule implements StarlarkValue {

  public static final CryptoHashModule INSTANCE = new CryptoHashModule();

  abstract class LarkyDigest implements StarlarkValue {

    public abstract  ExtendedDigest getDigest();

    @StarlarkMethod(
        name = "update",
        doc = "Update the hash object with the bytes in data. Repeated calls\n" +
            "are equivalent to a single call with the concatenation of all\n" +
            "the arguments.",
        parameters = {@Param(name = "data", allowedTypes = {
            @ParamType(type = LarkyByteLike.class)
        })}
    )
    public void update(LarkyByteLike data) {
      byte[] input = data.getBytes();
      this.getDigest().update(input, 0, input.length);
    }

    @StarlarkMethod(
        name = "digest",
        doc = "Return the digest of the bytes passed to the update() method\n" +
            "so far as a bytes object.",
        useStarlarkThread = true
    )
    public LarkyByteLike digest(StarlarkThread thread) throws EvalException {
      byte[] resBuf = new byte[this.getDigest().getDigestSize()];
      this.getDigest().doFinal(resBuf, 0);
      return LarkyByte.builder(thread).setSequence(resBuf).build();
    }

    @StarlarkMethod(
        name = "hexdigest",
        doc = "Like digest() except the digest is returned as a string\n" +
            "of double length, containing only hexadecimal digits."
    )
    public String hexdigest() {
      byte[] resBuf = new byte[this.getDigest().getDigestSize()];
      this.getDigest().doFinal(resBuf, 0);
      return Hex.toHexString(resBuf);
    }
  }

  class LarkyGeneralDigest extends LarkyDigest {
    private GeneralDigest digest;
    public LarkyGeneralDigest(GeneralDigest digest) {
      this.digest = digest;
    }

    @Override
    public ExtendedDigest getDigest() {
      return this.digest;
    }

    @StarlarkMethod(
    name = "copy",
    doc = "Return a copy (“clone”) of the hash object. This can be used to efficiently " +
        "compute the digests of data sharing a common initial substring."
    )
    public LarkyGeneralDigest copy() {
      return new LarkyGeneralDigest((GeneralDigest) this.digest.copy());
    }
  }

  class LarkyLongDigest extends LarkyDigest {
    private final LongDigest digest;

    public LarkyLongDigest(LongDigest digest) {
      this.digest = digest;
    }

    @Override
    public ExtendedDigest getDigest() {
      return this.digest;
    }

    @StarlarkMethod(
    name = "copy",
    doc = "Return a copy (“clone”) of the hash object. This can be used to efficiently " +
        "compute the digests of data sharing a common initial substring."
    )
    public LarkyLongDigest copy() {
      return new LarkyLongDigest((LongDigest) this.digest.copy());
    }
  }

  class LarkyKeccakDigest extends LarkyDigest {

    private final KeccakDigest digest;

    public LarkyKeccakDigest(KeccakDigest digest) {
      this.digest = digest;
    }

    @Override
    public ExtendedDigest getDigest() {
      return this.digest;
    }

//    @StarlarkMethod(
//      name = "read",
//      doc = "" +
//          "Compute the next piece of XOF output.\n" +
//          "  .. note::\n" +
//          "      You cannot use update anymore after the first call to\n" +
//          "      :meth:`read`.\n" +
//          "  Args:\n" +
//          "   length (integer): the amount of bytes this method must return"
//        ,useStarlarkThread = true)
//    public LarkyByteLike read(StarlarkThread thread) throws EvalException {
//      byte[] bytes = new byte[this.digest.getDigestSize()];
//      this.digest.doFinal(bytes, 0);
//      return LarkyByte.builder(thread).setSequence(bytes).build();
//    }
  }

  class LarkyXofDigest<T extends KeccakDigest & Xof> extends LarkyKeccakDigest {

    private static final int MAX_READ_LENGTH = 2048;
    private final T digest;

    public LarkyXofDigest(T digest) {
      super(digest);
      this.digest = digest;
    }

    @Override
    public ExtendedDigest getDigest() {
      return this.digest;
    }

    @StarlarkMethod(
      name = "read",
      doc = "" +
          "Compute the next piece of XOF output.\n" +
          "  .. note::\n" +
          "      You cannot use update anymore after the first call to\n" +
          "      :meth:`read`.\n" +
          "  Args:\n" +
          "   length (integer): the amount of bytes this method must return",
        parameters = {
          @Param(name = "length", allowedTypes = {@ParamType(type=StarlarkInt.class)})
        }, useStarlarkThread = true)
    public LarkyByteLike read(StarlarkInt length, StarlarkThread thread) throws EvalException {
      int length_ = length.toIntUnchecked();
      Preconditions.checkArgument(
          length_ < MAX_READ_LENGTH,
          "Expected length %s to be less than %s", length_, MAX_READ_LENGTH);
      byte[] bytes = new byte[length_];
      this.digest.doFinal(bytes, 0, length.toIntUnchecked());
      return LarkyByte.builder(thread).setSequence(bytes).build();
    }
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
      @Param(name = "bit_length", allowedTypes = {@ParamType(type = StarlarkInt.class)}, defaultValue = "128"),
  })
  public LarkyXofDigest<?> SHAKE128(StarlarkInt bitLength) {
    SHAKEDigest digest = new SHAKEDigest(bitLength.toIntUnchecked());
    return new LarkyXofDigest<>(digest);
  }
}
