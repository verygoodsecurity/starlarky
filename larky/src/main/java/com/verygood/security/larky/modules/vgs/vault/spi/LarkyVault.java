package com.verygood.security.larky.modules.vgs.vault.spi;

import java.util.List;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkValue;

public interface LarkyVault extends StarlarkValue {

    Object redact(Object value, Object storage, Object format, List<Object> tags, Object decoratorConfig) throws EvalException;

    Object reveal(Object value, Object storage) throws EvalException;

    /**
     * Returns the current raw value (acts like a reveal and delete).
     * @param value Alias or Alias List
     * @param storage Persistent or Volatile (null defaults to Persistent)
     * @return Value of alias(es) or throws if not found
     * @throws EvalException if any alias not found or bad input
     */
    Object delete(Object value, Object storage) throws EvalException;

}