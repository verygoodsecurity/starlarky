package com.verygood.security.larky.modules.vgs.vault.defaults;

import net.starlark.java.eval.EvalException;

@FunctionalInterface
public interface TokenizeFunction {

  String tokenize(String value) throws EvalException;
}
