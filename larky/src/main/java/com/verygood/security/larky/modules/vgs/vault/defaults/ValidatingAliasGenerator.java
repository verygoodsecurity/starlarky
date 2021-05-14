package com.verygood.security.larky.modules.vgs.vault.defaults;

import net.starlark.java.eval.EvalException;

abstract class ValidatingAliasGenerator implements AliasGenerator {

    @Override
    public String generate(String value) throws EvalException {
        if (!isValid(value)) {
            return fallbackAliasGenerator().generate(value);
        }
        return internalGenerator(value);
    }

    protected abstract boolean isValid(String value);

    protected abstract String internalGenerator(String Value);

    protected abstract AliasGenerator fallbackAliasGenerator();

}
