package com.verygood.security.larky.modules;

import com.verygood.security.larky.modules.xml.LarkyXMLNamespaceContext;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;


@StarlarkBuiltin(
    name = "jxml",
    category = "BUILTIN",
    doc = "java specific xml/xml-security/xmldsig/xml-crypto implementation")
public class XMLModule implements StarlarkValue {
  public static final XMLModule INSTANCE = new XMLModule();

  @StarlarkMethod(name="_namespace_map", useStarlarkThread = true)
  public LarkyXMLNamespaceContext namespaceMap(StarlarkThread thread) {
    return LarkyXMLNamespaceContext.withThread(thread);
  }
}
