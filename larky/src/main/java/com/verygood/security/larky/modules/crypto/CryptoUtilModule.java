package com.verygood.security.larky.modules.crypto;

import com.verygood.security.larky.modules.crypto.Util.ASN1.LarkyASN1Sequence;
import com.verygood.security.larky.modules.crypto.Util.ASN1.LarkyDerInteger;
import com.verygood.security.larky.modules.crypto.Util.Strxor;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkValue;


public class CryptoUtilModule implements StarlarkValue {

  public static final CryptoUtilModule INSTANCE = new CryptoUtilModule();

  @StarlarkMethod(name = "ASN1", structField = true)
  public CryptoUtilModule Util() {
    return INSTANCE;
  }

  @StarlarkMethod(name = "strxor", structField = true)
  public Strxor strxor() {
    return new Strxor();
  }

  @StarlarkMethod(name = "DerInteger", parameters = {
      @Param(name = "n")
  })
  public LarkyDerInteger DerInteger(StarlarkInt n) {
    LarkyDerInteger i = LarkyDerInteger.fromStarlarkInt(n);
    return i;
  }

  @StarlarkMethod(name = "DerSequence", parameters = {@Param(name = "obj")})
  public LarkyASN1Sequence DerSequence(StarlarkList<StarlarkValue> obj) throws EvalException {
    return LarkyASN1Sequence.fromList(obj);
  }


}