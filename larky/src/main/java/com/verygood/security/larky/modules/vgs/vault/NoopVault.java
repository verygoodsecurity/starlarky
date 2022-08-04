package com.verygood.security.larky.modules.vgs.vault;

import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import java.util.Map;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;

import java.util.List;

public class NoopVault implements LarkyVault {
    @Override
    public Object redact(Object value, Object storage, Object format, List<Object> tags, Object decoratorConfig) throws EvalException {
        throw Starlark.errorf("vault.redact operation must be overridden");
    }

    @Override
    public Object reveal(Object value, Object storage) throws EvalException {
        throw Starlark.errorf("vault.reveal operation must be overridden");
    }
}