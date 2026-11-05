package com.verygood.security.larky.modules.vgs.vault.spi;

import java.util.List;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkValue;

public interface LarkyVault extends StarlarkValue {

    Object redact(Object value, Object storage, Object format, List<Object> tags, Object decoratorConfig) throws EvalException;

    Object reveal(Object value, Object storage) throws EvalException;

    /**
     * Reveal an alias that is scoped to a record, as produced by formats whose aliases are only
     * unique within a record.
     *
     * <p>The default implementation keeps existing implementations source and binary compatible: it
     * delegates to {@link #reveal(Object, Object)} when no record is given and rejects a record
     * otherwise. Vaults that support record-scoped aliases override it.
     *
     * @param value Alias or Alias List
     * @param storage Persistent or Volatile (null defaults to Persistent)
     * @param record the record the alias belongs to, or {@code None} for an unscoped alias
     * @throws EvalException if a record is given and this vault does not support it, or on bad input
     */
    default Object reveal(Object value, Object storage, Object record) throws EvalException {
        if (record == null || record instanceof NoneType) {
            return reveal(value, storage);
        }
        throw Starlark.errorf("this vault does not support record-scoped reveal");
    }

    /**
     * Delete the token, errors if not found.
     * @param value Alias or Alias List
     * @param storage Persistent or Volatile (null defaults to Persistent)
     * @throws EvalException if any alias not found or bad input
     */
    void delete(Object value, Object storage) throws EvalException;

    /**
     * Delete a token that is scoped to a record.
     *
     * <p>The default implementation delegates to {@link #delete(Object, Object)} when no record is
     * given and rejects a record otherwise, so existing implementations are unaffected.
     *
     * @param value Alias or Alias List
     * @param storage Persistent or Volatile (null defaults to Persistent)
     * @param record the record the alias belongs to, or {@code None} for an unscoped alias
     * @throws EvalException if a record is given and this vault does not support it, if any alias is
     *     not found, or on bad input
     */
    default void delete(Object value, Object storage, Object record) throws EvalException {
        if (record == null || record instanceof NoneType) {
            delete(value, storage);
            return;
        }
        throw Starlark.errorf("this vault does not support record-scoped delete");
    }

    Object sign(String keyId, String message, String algorithm) throws EvalException;

    Object verify(String keyId, String message, String signature, String algorithm) throws EvalException;
}