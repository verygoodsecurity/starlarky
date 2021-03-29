package com.verygood.security.larky.modules.crypto.Util;

import com.verygood.security.larky.modules.types.LarkyByteLike;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;

public class RFC1751 {

  @StarlarkMethod(name = "english_to_key", parameters = {
      @Param(name = "s")
  })
  public void english_to_key(String s) {
//Transform a string into a corresponding key.
}
  @StarlarkMethod(name = "key_to_english", parameters = {
      @Param(name = "n")
  })
  public void key_to_english(LarkyByteLike key) {
//Transform an arbitrary key into a string containing English words.
}
}
