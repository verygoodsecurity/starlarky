package com.verygood.security.larky.modules.crypto.Hash;

import com.verygood.security.larky.runtime.CurrentEvaluation;
import com.verygood.security.larky.annot.CacheableWhenFrozen;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.ext.ByteList;

import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.util.Memoable;

@CacheableWhenFrozen
public abstract class LarkyDigest implements StarlarkValue, Mutability.Freezable {

  // The evaluation that created this hash; update() fails once it is frozen.
  private final Mutability mutability = CurrentEvaluation.mutabilityForNewValue();

  @Override
  public Mutability mutability() {
    return mutability;
  }

  public abstract ExtendedDigest getDigest();

  @StarlarkMethod(
      name = "update",
      doc = "Update the hash object with the bytes in data. Repeated calls\n" +
          "are equivalent to a single call with the concatenation of all\n" +
          "the arguments.",
      parameters = {@Param(name = "data", allowedTypes = {
          @ParamType(type = StarlarkBytes.class)
      })}
  )
  public void update(StarlarkBytes data) throws EvalException {
    Starlark.checkMutable(this);
    byte[] input = data.toByteArray();
    this.getDigest().update(input, 0, input.length);
  }

  @StarlarkMethod(
      name = "digest",
      doc = "Return the digest of the bytes passed to the update() method\n" +
          "so far as a bytes object.",
      useStarlarkThread = true
  )
  public StarlarkBytes digest(StarlarkThread thread) throws EvalException {
    return StarlarkBytes.of(thread.mutability(), reuseDigestIfPossible());
  }

  protected byte[] reuseDigestIfPossible() throws EvalException {
    if (!(this.getDigest() instanceof Memoable)) {
      // doFinal on the digest itself resets it, which changes its state
      Starlark.checkMutable(this);
    }
    byte[] resBuf = new byte[this.getDigest().getDigestSize()];
    final ExtendedDigest copy;
    if(this.getDigest() instanceof Memoable) {
      copy = (ExtendedDigest) ((Memoable)this.getDigest()).copy();
    } else {
      copy = this.getDigest();
    }
    copy.doFinal(resBuf, 0);
    return resBuf;
  }

  @StarlarkMethod(
      name = "hexdigest",
      doc = "Like digest() except the digest is returned as a string\n" +
          "of double length, containing only hexadecimal digits."
  )
  public String hexDigest() throws EvalException {
    return ByteList.wrap(reuseDigestIfPossible()).hex();
  }
}
