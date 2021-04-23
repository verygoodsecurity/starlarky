package com.verygood.security.larky.modules.iso8583;

import com.verygood.security.larky.modules.types.LarkyByteLike;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.packager.GenericPackager;

public class ISO8583ParseModule implements StarlarkValue {

  public static final ISO8583ParseModule INSTANCE = new ISO8583ParseModule();

  @StarlarkMethod(name = "decode", parameters = {
      @Param(name = "bytes", allowedTypes = {@ParamType(type = LarkyByteLike.class)}),
      @Param(name = "packagerString", allowedTypes = {@ParamType(type = String.class)})
  }, useStarlarkThread = true)
  public Dict<String, String> decode(LarkyByteLike bytes, String packagerStr, StarlarkThread thrd) throws EvalException, ISOException {

    byte[] packagerBytes = Base64.getDecoder().decode(packagerStr.getBytes(StandardCharsets.UTF_8));
    ISOPackager packager = new GenericPackager(new ByteArrayInputStream(packagerBytes));

    ISOMsg isoMsg = new ISOMsg();
    isoMsg.setPackager(packager);
    isoMsg.unpack(bytes.getBytes());
    // remove it
    isoMsg.dump(System.out, "parse:");
    Mutability mu = Mutability.create("");
    Dict<String, String> result = Dict.of(mu);
    for (int i = 0; i < isoMsg.getMaxField(); i++) {
      result.putEntry(String.valueOf(i), isoMsg.getString(i));
    }
    return  result;
  }
}
