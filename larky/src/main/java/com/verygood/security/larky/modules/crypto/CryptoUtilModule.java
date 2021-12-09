package com.verygood.security.larky.modules.crypto;

import static com.verygood.security.larky.modules.crypto.Util.ASN1.LarkyDerBitString;
import static com.verygood.security.larky.modules.crypto.Util.ASN1.LarkyDerObjectId;
import static com.verygood.security.larky.modules.crypto.Util.ASN1.LarkyOctetString;
import static com.verygood.security.larky.modules.crypto.Util.ASN1.LarkySetOf;

import com.verygood.security.larky.modules.crypto.Util.ASN1.LarkyASN1Sequence;
import com.verygood.security.larky.modules.crypto.Util.ASN1.LarkyDerInteger;
import com.verygood.security.larky.modules.crypto.Util.Strxor;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEROctetString;


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
  public LarkyDerInteger DerInteger(Object nO) {
    StarlarkInt n = ((StarlarkInt) nO); // we want to throw a failure if it's not an Int
    return LarkyDerInteger.fromStarlarkInt(n);
  }

  @StarlarkMethod(name = "DerObjectId", parameters = {
      @Param(name = "objectstr")
  })
  public LarkyDerObjectId DerObjectId(String objectstr) {
    return new LarkyDerObjectId(new ASN1ObjectIdentifier(objectstr));
  }

  @StarlarkMethod(name = "DerOctetString", parameters = {
      @Param(name = "bytearr")
  })
  public LarkyOctetString DerOctetString(StarlarkBytes bytearr) {
    return new LarkyOctetString(new DEROctetString(bytearr.toByteArray()));
  }

  @StarlarkMethod(name = "DerBitString", parameters = {
      @Param(name = "bytearr")
  })
  public LarkyDerBitString DerBitString(StarlarkBytes bytearr) {
    return new LarkyDerBitString(new DERBitString(bytearr.toByteArray()));
  }

  @StarlarkMethod(name = "DerSetOf", parameters = {@Param(name = "obj")})
  public LarkySetOf DerSetOf(StarlarkList<?> obj) throws EvalException {
    return LarkySetOf.fromList(obj);
  }

  @StarlarkMethod(name = "DerSequence", parameters = {@Param(name = "obj")})
  public LarkyASN1Sequence DerSequence(StarlarkList<?> obj) throws EvalException {
    return LarkyASN1Sequence.fromList(obj);
  }


}