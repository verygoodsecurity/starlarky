package com.verygood.security.larky.modules.crypto.Hash;

import com.google.common.base.Preconditions;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;

import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.crypto.Xof;
import org.bouncycastle.crypto.digests.KeccakDigest;

public class LarkyXofDigest<T extends KeccakDigest & Xof> extends LarkyKeccakDigest {

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
          @Param(name = "length", allowedTypes = {@ParamType(type = StarlarkInt.class)})
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
