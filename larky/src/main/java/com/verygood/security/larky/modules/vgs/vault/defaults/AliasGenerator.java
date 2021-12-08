package com.verygood.security.larky.modules.vgs.vault.defaults;

import net.starlark.java.eval.EvalException;

interface AliasGenerator {
    String generate(String value) throws EvalException;
}
