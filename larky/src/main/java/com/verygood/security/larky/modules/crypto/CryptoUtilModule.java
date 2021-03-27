package com.verygood.security.larky.modules.crypto;

import com.verygood.security.larky.modules.types.LarkyByte;
import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERSequenceGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;


public class CryptoUtilModule implements StarlarkValue {

  public static final CryptoUtilModule INSTANCE = new CryptoUtilModule();

  @StarlarkMethod(name="asn1", structField = true)
  public CryptoUtilModule Util()  { return INSTANCE; }


  class LarkyDerInteger extends ASN1Integer implements StarlarkValue {

    public LarkyDerInteger(ASN1Integer asn1int) {
      super(asn1int.getValue());
    }

    @StarlarkMethod(name="encode", useStarlarkThread = true)
    public LarkyByte encode(StarlarkThread thread) throws EvalException {
      LarkyByte b;
      try {
         b = (LarkyByte) LarkyByte.builder(thread).setSequence(getEncoded()).build();
      } catch (IOException e) {
        throw Starlark.errorf(e.getMessage());
      }
      return b;
    }

    @StarlarkMethod(name="as_int")
    public StarlarkInt value() {
      return StarlarkInt.of(this.getValue());
    }

    @StarlarkMethod(name="decode", parameters = {
        @Param(name="barr"),
        @Param(name="strict", named = true, defaultValue = "False")
    })
    public LarkyDerInteger decode(LarkyByteLike barr, Boolean strict) throws EvalException {
      ASN1Integer intgr;
      try {
        intgr = (ASN1Integer) ASN1Primitive.fromByteArray(barr.getBytes());
      } catch (IOException e) {
        throw Starlark.errorf(e.getMessage());
      }
      //System.out.println(ASN1Dump.dumpAsString(intgr, true));
      return new LarkyDerInteger(intgr);
    }
  }

  @StarlarkMethod(name="DerInteger", parameters = {
      @Param(name = "n")
  })
  public LarkyDerInteger DerInteger(StarlarkInt n) throws EvalException {
    LarkyDerInteger i = new LarkyDerInteger(new ASN1Integer(n.toBigInteger()));;
    return i;
  }

  public static byte[] getEncoded(BigInteger[] sigs)
		throws IOException {
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	DERSequenceGenerator seq = new DERSequenceGenerator(bos);
	for(BigInteger i : sigs) {
          seq.addObject(new ASN1Integer(i));
        }
	seq.close();
	return bos.toByteArray();
}

  @StarlarkMethod(name="DerSequence", parameters = {@Param(name = "obj")})
  public LarkyByte x(StarlarkList<StarlarkInt> obj) throws IOException {
    BigInteger[] bi = new BigInteger[obj.size()];
    for(int i = 0; i < obj.size(); ++i) {
      bi[i] = obj.get(i).toBigInteger();
    }
    byte[] encoded = getEncoded(bi);
    return null;
    //return (LarkyDerSequence) LarkyDerSequence.getInstance(DERSequence.fromByteArray(encoded));
    //DERSequence seq = (DERSequence) DERSequence.fromByteArray(encoded);
    //return (LarkyDerSequence) seq;

  }

}
