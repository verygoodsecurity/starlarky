package com.verygood.security.larky.modules.crypto.Hash;

import com.verygood.security.larky.modules.crypto.CryptoHashModule;

import net.starlark.java.annot.StarlarkMethod;

import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.crypto.digests.GeneralDigest;

public class LarkyGeneralDigest extends LarkyDigest {
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
