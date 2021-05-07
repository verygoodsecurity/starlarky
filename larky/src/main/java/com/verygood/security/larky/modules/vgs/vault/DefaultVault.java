package com.verygood.security.larky.modules.vgs.vault;

import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import net.starlark.java.eval.EvalException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultVault implements LarkyVault {

    Map<Object,Object> vault_map = new HashMap<>();

    private Object redact(Object value) {
        return "tok_" + value.hashCode();
    }

    @Override
    public Object redact(Object value, Object storage, Object format, List<Object> tags) throws EvalException {
        Object token = redact(value);
        vault_map.put(token, value);
        return token;
    }

    @Override
    public Object reveal(Object value, Object storage) throws EvalException {
        return vault_map.get(value);
    }

}