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
     * Reveal an alias using the optional lookup context it was redacted with.
     *
     * <p>{@code entityKey} and {@code kind} are each optional, and an omitted one ({@code None} or
     * {@code null}) adds no lookup scope. An implementation must look up exactly the context given,
     * the same one used at redact, and must never search other scopes: an alias stored with a kind
     * is not found by a reveal that omits it.
     *
     * <p>The default implementation keeps existing implementations source and binary compatible: it
     * delegates to {@link #reveal(Object, Object)} when neither field is given and rejects a call
     * that gives either. Vaults that support a lookup context override it.
     *
     * @param value Alias or Alias List
     * @param storage Persistent or Volatile (null defaults to Persistent)
     * @param entityKey the entity key the alias was redacted with, or {@code None} for no entity scope
     * @param kind the kind the alias was redacted with as lookup scope, or {@code None} for no kind scope
     * @throws EvalException if a context field is given and this vault does not support it, or on bad
     *     input
     */
    default Object reveal(Object value, Object storage, Object entityKey, Object kind) throws EvalException {
        if (isAbsent(entityKey) && isAbsent(kind)) {
            return reveal(value, storage);
        }
        throw Starlark.errorf("this vault does not support reveal with entity_key or kind");
    }

    /**
     * Delete the token, errors if not found.
     * @param value Alias or Alias List
     * @param storage Persistent or Volatile (null defaults to Persistent)
     * @throws EvalException if any alias not found or bad input
     */
    void delete(Object value, Object storage) throws EvalException;

    /**
     * Delete a token using the optional lookup context it was redacted with.
     *
     * <p>As for {@link #reveal(Object, Object, Object, Object)}: each omitted field adds no lookup
     * scope, and only the entry stored with exactly this context is deleted. The default
     * implementation delegates to {@link #delete(Object, Object)} when neither field is given and
     * rejects a call that gives either, so existing implementations are unaffected.
     *
     * @param value Alias or Alias List
     * @param storage Persistent or Volatile (null defaults to Persistent)
     * @param entityKey the entity key the alias was redacted with, or {@code None} for no entity scope
     * @param kind the kind the alias was redacted with as lookup scope, or {@code None} for no kind scope
     * @throws EvalException if a context field is given and this vault does not support it, if any
     *     alias is not found, or on bad input
     */
    default void delete(Object value, Object storage, Object entityKey, Object kind) throws EvalException {
        if (isAbsent(entityKey) && isAbsent(kind)) {
            delete(value, storage);
            return;
        }
        throw Starlark.errorf("this vault does not support delete with entity_key or kind");
    }

    private static boolean isAbsent(Object contextField) {
        return contextField == null || contextField instanceof NoneType;
    }

    Object sign(String keyId, String message, String algorithm) throws EvalException;

    Object verify(String keyId, String message, String signature, String algorithm) throws EvalException;
}