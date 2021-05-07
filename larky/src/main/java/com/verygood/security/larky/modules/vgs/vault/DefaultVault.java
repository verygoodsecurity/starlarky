package com.verygood.security.larky.modules.vgs.vault;

import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultVault implements LarkyVault {

    private final Map<String,Map<String,Object>> config = new HashMap<String,Map<String,Object>>() {{
        put("persistent", persistentVaultStorage);
        put("volatile", volatileVaultStorage);
    }};

    private Map<String,Object> persistentVaultStorage = new HashMap<>();
    private Map<String,Object> volatileVaultStorage = new HashMap<>();

    private String tokenize(Object value) {
        return "tok_" + value.hashCode();
    }

    @Override
    public Object redact(Object value, Object storage, Object format, List<Object> tags) throws EvalException {
        String token = tokenize(value);
        getStorage(storage).put(token, value);
        return token;
    }

    @Override
    public Object reveal(Object value, Object storage) throws EvalException {
        Object secret = getStorage(storage).get(value);
        return secret == null ? "token" : secret; // return 'token' if entry not found
    }

    private Map<String,Object> getStorage(Object storage) throws EvalException {

        if (storage instanceof NoneType) { // Use 'persistent` storage by default
            return persistentVaultStorage;
        } else if (storage instanceof String) {
            if (!config.keySet().contains(storage)) {
                throw Starlark.errorf(
                        String.format(
                                "Cannot find storage type %s. Only 'persistent' and 'volatile'.", storage
                        )
                );
            }

            return config.get(storage);
        }

        throw Starlark.errorf(String.format("Storage of type %s is not supported in DefaultVault, expecting String", storage.getClass().getName()));

    }

}