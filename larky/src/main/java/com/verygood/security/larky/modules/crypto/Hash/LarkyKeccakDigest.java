package com.verygood.security.larky.modules.crypto.Hash;

import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.crypto.digests.KeccakDigest;

public class LarkyKeccakDigest extends LarkyDigest {

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
