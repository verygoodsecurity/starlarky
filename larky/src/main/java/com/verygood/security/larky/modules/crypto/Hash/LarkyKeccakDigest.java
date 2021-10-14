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

}
