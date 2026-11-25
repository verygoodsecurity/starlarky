package com.verygood.security.larky.modules.vgs.vault.defaults;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.modules.vgs.vault.DecoratorConfig;
import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;


public class DefaultVault implements LarkyVault {

    /**
     * Entries are keyed on the lookup context and the alias. A context field is null when the
     * redact gave none, so it adds no scope; a lookup matches only exactly the same context.
     */
    private record StorageKey(String entityKey, String kind, String alias) {}

    /** The lookup context a redact stores an alias under. */
    private record RedactContext(String entityKey, String kind) {}

    private final Map<StorageKey, Object> persistentVaultStorage = new HashMap<>();
    private final Map<StorageKey, Object> volatileVaultStorage = new HashMap<>();

    private final ImmutableMap<String, Map<StorageKey, Object>> storageConfig = ImmutableMap.of(
            "persistent", persistentVaultStorage,
            "volatile", volatileVaultStorage
    );

    private final ImmutableMap<String, AliasGenerator> formatToAliasGenerator = ImmutableMap.<String, AliasGenerator>builder()
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
            .put("SYNTHETIC", new NoopAliasGenerator("SYNTHETIC"))
            .build();

    @Override
    public Object redact(Object value, Object storage, Object format, List<Object> tags, Object decoratorConfig) throws EvalException {
        String sValue = getValue(value);
        RedactContext context = getRedactContext(decoratorConfig);
        AliasGenerator generator = getAliasGenerator(format);
        Optional<AliasDecorator> aliasDecorator = resolveAliasDecorator(decoratorConfig, generator);

        String alias = aliasDecorator.isPresent()
            ? aliasDecorator.get().tokenize(sValue)
            : generator.generate(sValue);
        getStorage(storage).put(new StorageKey(context.entityKey(), context.kind(), alias), value);
        return alias;
    }

    private Optional<AliasDecorator> resolveAliasDecorator(Object decoratorConfig, AliasGenerator generator) {
        DecoratorConfig config = DecoratorConfig.fromObject(decoratorConfig);
        if (config != null && config.getSearchPattern() != null && config.getReplacePattern() != null) {
            AliasDecorator decorator = new AliasDecorator(
                generator::generate,
                config.getSearchPattern(),
                config.getReplacePattern()
            );
            return Optional.of(decorator);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Object reveal(Object value, Object storage) throws EvalException {
        return reveal(value, storage, Starlark.NONE, Starlark.NONE);
    }

    @Override
    public Object reveal(Object value, Object storage, Object entityKey, Object kind) throws EvalException {
        String sValue = getValue(value);
        Object secret = getStorage(storage).get(lookupKey(entityKey, kind, sValue));
        return secret == null ? sValue : secret; // return value of alias if secret not found
    }

    @Override
    public void delete(Object value, Object storage) throws EvalException {
        delete(value, storage, Starlark.NONE, Starlark.NONE);
    }

    @Override
    public void delete(Object value, Object storage, Object entityKey, Object kind) throws EvalException {
        String sValue = getValue(value);
        getStorage(storage).remove(lookupKey(entityKey, kind, sValue));
    }

    // These don't make a ton of sense in memory, we could hash and return if we really wanted to.
    public Object sign(String keyId, String message, String algorithm) throws EvalException {
        return message;
    }

    public Object verify(String keyId, String message, String signature, String algorithm) throws EvalException {
        return false;
    }

    private String getValue(Object value) throws EvalException {
        if (!(value instanceof String)) {
            throw Starlark.errorf(String.format(
                    "Value of type %s is not supported in DefaultVault, expecting String", value.getClass().getName()
            ));
        }

        return value.toString();
    }

    private StorageKey lookupKey(Object entityKey, Object kind, String alias) throws EvalException {
        return new StorageKey(getContextField("entity_key", entityKey), getContextField("kind", kind), alias);
    }

    /**
     * The lookup context of a redact: decorator_config's "entity_key", and its
     * "kind" only when "scope_kind" is True. Otherwise kind is only a generation input and adds no
     * scope.
     */
    private RedactContext getRedactContext(Object decoratorConfig) throws EvalException {
        if (!(decoratorConfig instanceof Map<?, ?> config)) {
            return new RedactContext(null, null);
        }
        String entityKey = getContextField("entity_key", config.get("entity_key"));
        String kind = isScopeKind(config.get("scope_kind")) ? getContextField("kind", config.get("kind")) : null;
        return new RedactContext(entityKey, kind);
    }

    private boolean isScopeKind(Object scopeKind) throws EvalException {
        if (scopeKind == null || scopeKind instanceof NoneType) {
            return false;
        }
        if (!(scopeKind instanceof Boolean flag)) {
            throw Starlark.errorf("scope_kind must be a bool, not %s", Starlark.type(scopeKind));
        }
        return flag;
    }

    /** Validates a lookup context field: null for None, otherwise a non-blank String. */
    private String getContextField(String field, Object value) throws EvalException {
        if (value == null || value instanceof NoneType) {
            return null;
        }
        if (!(value instanceof String name)) {
            throw Starlark.errorf(String.format(
                    "%s of type %s is not supported in DefaultVault, expecting String",
                    field, value.getClass().getName()
            ));
        }
        if (name.isBlank()) {
            throw Starlark.errorf("%s must not be empty or blank", field);
        }
        return name;
    }

    private Map<StorageKey, Object> getStorage(Object storage) throws EvalException {

        if (storage instanceof NoneType) { // Use persistent storage by default
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

    private AliasGenerator getAliasGenerator(Object format) throws EvalException {

        if (format instanceof NoneType) {
            return formatToAliasGenerator.get("UUID"); // default generator
        } else if (format instanceof String) {
            if (formatToAliasGenerator.containsKey(format)) {
                return formatToAliasGenerator.get(format);
            } else {
                throw Starlark.errorf(String.format(
                        "Format '%s' not found in supported format types: %s",
                        format,
                        formatToAliasGenerator.keySet().toString()
                ));
            }
        }

        throw Starlark.errorf(String.format(
                "Format of type %s is not supported in DefaultVault, expecting String",
                format.getClass().getName()
        ));
    }

}