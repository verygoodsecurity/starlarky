package com.verygood.security.larky.modules;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.StarlarkValue;


@StarlarkBuiltin(
    name = "jxmlsec",
    category = "BUILTIN",
    doc = "java specific xml-security/xmldsig/xml-crypto implementation")
public class XMLSecModule implements StarlarkValue {
  public static final XMLSecModule INSTANCE = new XMLSecModule();
}
