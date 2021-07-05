package com.verygood.security.larky.modules;

import com.verygood.security.larky.modules.openssl.OpenSSL;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

@StarlarkBuiltin(
    name = "jopenssl",
    category = "BUILTIN",
    doc = "")
public class OpenSSLModule implements StarlarkValue {

  public static final OpenSSLModule INSTANCE =  new OpenSSLModule();

  @StarlarkMethod(name = "OpenSSL", doc = "openssl module", structField = true)
  public static OpenSSL openSSL() { return OpenSSL.INSTANCE; }

}