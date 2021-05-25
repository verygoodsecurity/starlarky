package com.verygood.security.larky.modules.vgs.vault.defaults;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DefaultVault implements LarkyVault {

    private final Map<String, Object> persistentVaultStorage = new HashMap<>();
    private final Map<String, Object> volatileVaultStorage = new HashMap<>();

    private final ImmutableMap<String, Map<String, Object>> storageConfig = ImmutableMap.of(
            "persistent", persistentVaultStorage,
            "volatile", volatileVaultStorage
    );

    private final ImmutableMap<String, AliasGenerator> formatTokenizer = ImmutableMap.<String, AliasGenerator>builder()
            .put("RAW_UUID", new RawAliasGenerator())
            .put("UUID", new UUIDAliasGenerator())
            .put("NUM_LENGTH_PRESERVING", new NumberLengthPreserving())
            // Placeholders for unsupported formats found in
            // https://www.verygoodsecurity.com/docs/terminology/nomenclature#alias-format
            .put("PFPT", new NoopAliasGenerator("PFPT"))
            .put("FPE_SIX_T_FOUR", new NoopAliasGenerator("FPE_SIX_T_FOUR"))
            .put("FPE_T_FOUR", new NoopAliasGenerator("FPE_T_FOUR"))
            .put("NON_LUHN_FPE_ALPHANUMERIC", new NoopAliasGenerator("NON_LUHN_FPE_ALPHANUMERIC"))
            .put("FPE_SSN_T_FOUR", new NoopAliasGenerator("FPE_SSN_T_FOUR"))
            .put("FPE_ACC_NUM_T_FOUR", new NoopAliasGenerator("FPE_ACC_NUM_T_FOUR"))
            .put("FPE_ALPHANUMERIC_ACC_NUM_T_FOUR", new NoopAliasGenerator("FPE_ALPHANUMERIC_ACC_NUM_T_FOUR"))
            .put("GENERIC_T_FOUR", new NoopAliasGenerator("GENERIC_T_FOUR"))
            .put("ALPHANUMERIC_SIX_T_FOUR", new NoopAliasGenerator("ALPHANUMERIC_SIX_T_FOUR"))
            .build();

    @Override
    public Object redact(Object value, Object storage, Object format, List<Object> tags, Dict<String, Object> context) throws EvalException {
        String sValue = getValue(value);
        String token = getTokenizer(format).tokenize(sValue);
        getStorage(storage).put(token, value);
        return token;
    }

    @Override
    public Object reveal(Object value, Object storage, Dict<String, Object> context) throws EvalException {
        String sValue = getValue(value);
        Object secret = getStorage(storage).get(sValue);
        return secret == null ? "token" : secret; // return 'token' if entry not found
    }

    private String getValue(Object value) throws EvalException {
        if (!(value instanceof String)) {
            throw Starlark.errorf(String.format(
                    "Value of type %s is not supported in DefaultVault, expecting String", value.getClass().getName()
            ));
        }

        return value.toString();
    }

    private Map<String, Object> getStorage(Object storage) throws EvalException {

        if (storage instanceof NoneType) { // Use 'persistent` storage by default
            return persistentVaultStorage;
        } else if (storage instanceof String) {
            if (!storageConfig.containsKey(storage)) {
                throw Starlark.errorf(String.format(
                        "Storage '%s' not found in supported storage types: %s",
                        storage,
                        storageConfig.keySet().toString()
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
            return formatTokenizer.get("UUID"); // default tokenizer
        } else if (format instanceof String) {
            if (formatTokenizer.containsKey(format)) {
                return formatTokenizer.get(format);
            } else {
                throw Starlark.errorf(String.format(
                        "Format '%s' not found in supported format types: %s",
                        format,
                        formatTokenizer.keySet().toString()
                ));
            }
        }

        throw Starlark.errorf(String.format(
                "Format of type %s is not supported in DefaultVault, expecting String",
                format.getClass().getName()
        ));
    }

}