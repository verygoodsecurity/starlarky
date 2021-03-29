package com.verygood.security.larky.modules.crypto.Util;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkInt;

public class Padding {

  @StarlarkMethod(name = "pad", parameters = {
      @Param(name = "n")
  })
  public void pad(StarlarkInt n) {
    //pad(data_to_pad, block_size, style='pkcs7')
  }
  @StarlarkMethod(name = "pad", parameters = {
      @Param(name = "n")
  })
  public void unpad(StarlarkInt n) {
    //pad(data_to_pad, block_size, style='pkcs7')
  }
}
