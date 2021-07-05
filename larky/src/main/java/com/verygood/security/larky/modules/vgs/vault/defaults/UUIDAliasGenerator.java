package com.verygood.security.larky.modules.vgs.vault.defaults;

import net.starlark.java.eval.EvalException;

class UUIDAliasGenerator extends RawAliasGenerator {
    @Override
    public String generate(String value) throws EvalException {
        return String.format("tok_%s", super.generate(value)).substring(0, 30);
    }
}
