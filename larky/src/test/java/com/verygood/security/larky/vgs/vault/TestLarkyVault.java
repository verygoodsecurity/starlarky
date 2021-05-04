package com.verygood.security.larky.vgs.vault;

import com.verygood.security.larky.modules.vgs.vault.LarkyVault;
import net.starlark.java.eval.EvalException;

import java.util.HashMap;
import java.util.Map;

public class TestLarkyVault implements LarkyVault {

    Map<Object,Object> vault_map = new HashMap<>();

    public Object _redact(Object value) {
        return "tok_123";
    }

    @Override
    public Object put(Object value, Object storage, Object format) throws EvalException {
        Object token = _redact(value);
        vault_map.put(token,value);
        return token;
    }

    @Override
    public Object get(Object value, Object storage) throws EvalException {
        return vault_map.get(value);
    }
}
