package com.verygood.security.larky.modules;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.StarlarkValue;

@StarlarkBuiltin(
    name = "jx509",
    category = "BUILTIN",
    doc = "A module that helps expose x509 helpers")
public class X509Module implements StarlarkValue {

  public static final X509Module INSTANCE =  new X509Module();

}