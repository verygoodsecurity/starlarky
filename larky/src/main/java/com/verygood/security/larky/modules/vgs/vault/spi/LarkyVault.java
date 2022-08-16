package com.verygood.security.larky.modules.vgs.vault.spi;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkValue;

import java.util.List;

public interface LarkyVault extends StarlarkValue {

    Object redact(Object value, Object storage, Object format, List<Object> tags, Object decoratorConfig) throws EvalException;

    Object reveal(Object value, Object storage) throws EvalException;

}