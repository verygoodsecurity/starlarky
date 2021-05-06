package com.verygood.security.larky.modules.vgs.vault.spi;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkValue;

public interface LarkyVault extends StarlarkValue {

    Object put(Object value, Object storage, Object format) throws EvalException;

    Object get(Object value, Object storage) throws EvalException;

}