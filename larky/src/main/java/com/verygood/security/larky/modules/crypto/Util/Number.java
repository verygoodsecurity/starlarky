package com.verygood.security.larky.modules.crypto.Util;

import net.starlark.java.eval.StarlarkBytes;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;

public class Number {

    @StarlarkMethod(name = "english_to_key", parameters = {
        @Param(name = "s")
    })
    public void english_to_key(String s) {
  //Transform a string into a corresponding key.
  }
    @StarlarkMethod(name = "key_to_english", parameters = {
        @Param(name = "n")
    })
    public void key_to_english(StarlarkBytes key) {
  //Transform an arbitrary key into a string containing English words.
  }
}
