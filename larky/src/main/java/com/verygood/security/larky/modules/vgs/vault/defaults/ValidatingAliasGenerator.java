package com.verygood.security.larky.modules.vgs.vault.defaults;

import net.starlark.java.eval.EvalException;

abstract class ValidatingAliasGenerator implements AliasGenerator {

    @Override
    public String tokenize(String value) throws EvalException {
        if (!isValid(value)) {
            return fallbackAliasGenerator().tokenize(value);
        }
        return internalTokenize(value);
    }

    protected abstract boolean isValid(String value);

    protected abstract String internalTokenize(String Value);

    protected abstract AliasGenerator fallbackAliasGenerator();

}
