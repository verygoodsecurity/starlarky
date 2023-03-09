package com.verygood.security.larky.modules.jks;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

@StarlarkBuiltin(
        name = "jks",
        category = "BUILTIN",
        doc = "")
public class JKSModule implements StarlarkValue {

    public static final JKSModule INSTANCE =  new JKSModule();

    @StarlarkMethod(name = "JKS", doc = "Java KeyStore module", structField = true)
    public static JKS jks() { return JKS.INSTANCE; }

}
