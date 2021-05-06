package com.verygood.security.larky.modules.vgs.vault;

import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;

public class NoopVault implements LarkyVault {
    @Override
    public Object put(Object value, Object storage, Object format) throws EvalException {
        throw Starlark.errorf("vault.put operation must be overridden");
    }

    @Override
    public Object get(Object value, Object storage) throws EvalException {
        throw Starlark.errorf("vault.get operation must be overridden");
    }
}