package com.verygood.security.larky.modules.vgs.vault.defaults;

import net.starlark.java.eval.EvalException;

interface AliasGenerator {
    String tokenize(String value) throws EvalException;
}
