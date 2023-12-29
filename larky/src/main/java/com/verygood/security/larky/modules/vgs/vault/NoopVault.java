package com.verygood.security.larky.modules.vgs.vault;

import com.google.protobuf.ByteString;
import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import java.util.List;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;

public class NoopVault implements LarkyVault {
    @Override
    public Object redact(Object value, Object storage, Object format, List<Object> tags, Object decoratorConfig) throws EvalException {
        throw Starlark.errorf("vault.redact operation must be overridden");
    }

    @Override
    public Object reveal(Object value, Object storage) throws EvalException {
        throw Starlark.errorf("vault.reveal operation must be overridden");
    }

    @Override
    public void delete(Object value, Object storage) throws EvalException {
        throw Starlark.errorf("vault.delete operation must be overridden");
    }

    @Override
    public Object sign(String keyId, String message, String algorithm) throws EvalException {
        throw Starlark.errorf("vault.sign operation must be overridden");
    }

    @Override
    public Object verify(String keyId, String message, ByteString signature, String algorithm) throws EvalException {
        throw Starlark.errorf("vault.verify operation must be overridden");
    }
}