package com.verygood.security.larky.modules.vgs.vault.defaults;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;

class NoopAliasGenerator implements AliasGenerator {

    private String aliasFormatName;

    public NoopAliasGenerator(String name) {
        this.aliasFormatName = name;
    }

    @Override
    public String generate(String value) throws EvalException {
        throw Starlark.errorf(String.format(
                "Format '%s' is not supported", aliasFormatName
        ));
    }
}
