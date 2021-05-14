package com.verygood.security.larky.modules.vgs.vault.defaults;

import net.starlark.java.eval.EvalException;

class UUIDAliasGenerator extends RawAliasGenerator {
    @Override
    public String tokenize(String value) throws EvalException {
        return String.format("tok_%s", super.tokenize(value)).substring(0, 30);
    }
}
