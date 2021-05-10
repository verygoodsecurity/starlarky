package com.verygood.security.larky.modules.vgs.vault;

import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DefaultVault implements LarkyVault {

    private final Map<String,Object> persistentVaultStorage = new HashMap<>();
    private final Map<String,Object> volatileVaultStorage = new HashMap<>();

    private final Map<String,Map<String,Object>> storageConfig = new HashMap<String,Map<String,Object>>() {{
        put("persistent", persistentVaultStorage);
        put("volatile", volatileVaultStorage);
    }};

    private final Map<String, AliasGenerator> formatTokenizer = new HashMap<String,AliasGenerator>() {{
        put("default", new UUIDAliasGenerator());
    }};

    @Override
    public Object redact(Object value, Object storage, Object format, List<Object> tags) throws EvalException {

        String sValue = getValue(value);
        String token = getTokenizer(format).tokenize(sValue);
        getStorage(storage).put(token, value);
        return token;
    }

    @Override
    public Object reveal(Object value, Object storage) throws EvalException {
        String sValue = getValue(value);
        Object secret = getStorage(storage).get(sValue);
        return secret == null ? "token" : secret; // return 'token' if entry not found
    }

    private String getValue(Object value) throws EvalException {
        if ( !(value instanceof String) ) {
            throw Starlark.errorf(String.format(
                    "Value of type %s is not supported in DefaultVault, expecting String", value.getClass().getName()
            ));
        }

        return value.toString();
    }

    private Map<String,Object> getStorage(Object storage) throws EvalException {

        if (storage instanceof NoneType) { // Use 'persistent` storage by default
            return persistentVaultStorage;
        } else if (storage instanceof String) {
            if (!storageConfig.containsKey(storage)) {
                throw Starlark.errorf(String.format(
                                "Storage '%s' not found in available storage list [persistent, volatile]", storage
                ));
            }

            return storageConfig.get(storage);
        }

        throw Starlark.errorf(String.format(
                "Storage of type %s is not supported in DefaultVault, expecting String",
                storage.getClass().getName()
        ));
    }

    private AliasGenerator getTokenizer(Object format) throws EvalException {

        if (format instanceof NoneType) {
            return formatTokenizer.get("default");
        } else if (format instanceof String) {
            if (formatTokenizer.containsKey(format)) {
                return formatTokenizer.get(format);
            } else {
                throw Starlark.errorf(String.format(
                        "Format '%s' not found", format
                ));
            }
        }

        throw Starlark.errorf(String.format(
                "Format of type %s is not supported in DefaultVault, expecting String",
                format.getClass().getName()
        ));
    }

    private abstract class ValidatingAliasGenerator implements AliasGenerator {

        private String validFormat;
        private AliasGenerator fallbackAliasGenerator;

        public boolean isValid(String value) {
            return value.matches(validFormat);
        }

        @Override
        public String tokenize(String value) {
            if(!isValid(value)) {
                return fallbackAliasGenerator.tokenize(value);
            }
            return internalTokenize(value);
        }

        abstract String internalTokenize(String Value);
    }

    private class UUIDAliasGenerator extends RawAliasGenerator {
        @Override
        public String tokenize(String value) {
            return String.format("tok_%s", super.tokenize(value)).substring(0,30);
        }
    }

    private class RawAliasGenerator implements AliasGenerator {
        @Override
        public String tokenize(String value) {
            return new String(Base64.getEncoder().encode(UUID.randomUUID().toString().getBytes()));
        }
    }

    private interface AliasGenerator {
        String tokenize(String value);
    }

}