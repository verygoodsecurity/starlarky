package com.verygood.security.larky.modules.crypto.Hash;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.util.encoders.Hex;

public abstract class LarkyDigest implements StarlarkValue {

  public abstract ExtendedDigest getDigest();

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
