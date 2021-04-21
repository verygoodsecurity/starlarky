package com.verygood.security.larky.modules.crypto.Hash;

import net.starlark.java.annot.StarlarkMethod;

import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.crypto.digests.LongDigest;

public class LarkyLongDigest extends LarkyDigest {
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
