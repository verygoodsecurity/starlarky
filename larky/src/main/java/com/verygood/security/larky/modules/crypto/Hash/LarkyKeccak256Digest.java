package com.verygood.security.larky.modules.crypto.Hash;

import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.jcajce.provider.digest.Keccak;

public class LarkyKeccak256Digest extends LarkyDigest {

  private final Keccak.Digest256 digest;

  public LarkyKeccak256Digest(Keccak.Digest256 digest) {
    this.digest = digest;
  }

  @Override
  public ExtendedDigest getDigest() {
    return (ExtendedDigest) this.digest;
  }

}
