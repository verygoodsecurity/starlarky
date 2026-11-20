package com.verygood.security.larky.modules.vgs.vault.spi;

import java.util.List;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkValue;

public interface LarkyVault extends StarlarkValue {

    Object redact(Object value, Object storage, Object format, List<Object> tags, Object decoratorConfig) throws EvalException;

    /** Reveals an alias or batch and reports the individual values actually found. */
    RevealOutcome revealOutcome(Object value, Object storage) throws EvalException;

    default Object reveal(Object value, Object storage) throws EvalException {
        return revealOutcome(value, storage).value();
    }

    /**
     * Delete the token, errors if not found.
     * @param value Alias or Alias List
     * @param storage Persistent or Volatile (null defaults to Persistent)
     * @throws EvalException if any alias not found or bad input
     */
    void delete(Object value, Object storage) throws EvalException;

    Object sign(String keyId, String message, String algorithm) throws EvalException;

    Object verify(String keyId, String message, String signature, String algorithm) throws EvalException;
}
