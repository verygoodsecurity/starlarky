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

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;

import java.io.IOException;


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
  public LarkyASN1Sequence x(StarlarkList<StarlarkValue> obj) throws IOException, EvalException {
    ASN1Encodable[] encodables = new ASN1Encodable[obj.size()];
    for (int i = 0; i < obj.size(); ++i) {
      encodables[i] = LarkyASN1Sequence.asASN1Encodable(obj.get(i));
    }
    DERSequence asn1Encodables = new DERSequence(encodables);
    System.out.println(asn1Encodables);
    return new LarkyASN1Sequence(asn1Encodables);
    //return new LarkyASN1Sequence(new DERSequence(encodables));
    //return (LarkyASN1Sequence) LarkyASN1Sequence.getInstance(DERSequence.fromByteArray(encoded));
    // DERSequence seq = (DERSequence) DERSequence.fromByteArray(encoded);
    //return (LarkyASN1Sequence) seq

  }


}