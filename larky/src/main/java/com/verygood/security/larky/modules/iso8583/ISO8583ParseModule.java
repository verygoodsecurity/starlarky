package com.verygood.security.larky.modules.iso8583;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

public class ISO8583ParseModule implements StarlarkValue {

  public static final ISO8583ParseModule INSTANCE = new ISO8583ParseModule();

  @StarlarkMethod(name = "decode", parameters = {
      @Param(name = "n", allowedTypes = {@ParamType(type = StarlarkInt.class)}),
      @Param(name = "m", allowedTypes = {@ParamType(type = StarlarkInt.class)})
  }, useStarlarkThread = true)
  public Dict decode(StarlarkInt n,StarlarkInt m, StarlarkThread thrd) throws EvalException {

    return Dict.builder()
        .put("a", n)
        .put("b", m)
        .buildImmutable();
  }
}
