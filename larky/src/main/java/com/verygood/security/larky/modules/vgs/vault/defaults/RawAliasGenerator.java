package com.verygood.security.larky.modules.vgs.vault.defaults;

import net.starlark.java.eval.EvalException;

import java.util.Base64;
import java.util.UUID;

sealed class RawAliasGenerator implements AliasGenerator permits UUIDAliasGenerator {
    @Override
    public String generate(String value) throws EvalException {
        return new String(Base64.getEncoder().encode(UUID.randomUUID().toString().getBytes()));
    }
}
