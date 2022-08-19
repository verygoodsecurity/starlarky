package com.verygood.security.larky.modules;

import com.google.common.collect.ImmutableList;
import com.verygood.security.larky.modules.vgs.vault.DecoratorConfig;
import com.verygood.security.larky.modules.vgs.vault.DecoratorConfig.InvalidDecoratorConfigException;
import com.verygood.security.larky.modules.vgs.vault.NoopVault;
import com.verygood.security.larky.modules.vgs.vault.defaults.DefaultVault;
import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;


@StarlarkBuiltin(
        name = "vault",
        category = "BUILTIN",
        doc = "Overridable Vault API in Larky")
public class VaultModule implements LarkyVault {

    public static final VaultModule INSTANCE = new VaultModule();
    public static final String ENABLE_INMEMORY_PROPERTY = "larky.modules.vgs.vault.enableInMemoryVault";

    private LarkyVault vault;

    private final ImmutableList<String> supportedStorage = ImmutableList.of(
            "persistent",
            "volatile"
    );

    private final ImmutableList<String> supportedFormat = ImmutableList.of(
            "RAW_UUID",
            "UUID",
            "NUM_LENGTH_PRESERVING",
            "PFPT",
            "FPE_SIX_T_FOUR",
            "FPE_T_FOUR",
            "NON_LUHN_FPE_ALPHANUMERIC",
            "FPE_SSN_T_FOUR",
            "FPE_ACC_NUM_T_FOUR",
            "FPE_ALPHANUMERIC_ACC_NUM_T_FOUR",
            "GENERIC_T_FOUR",
            "ALPHANUMERIC_SIX_T_FOUR",
            "VGS_FIXED_LEN_GENERIC"
    );

    public VaultModule() {

        ServiceLoader<LarkyVault> loader = ServiceLoader.load(LarkyVault.class);
        List<LarkyVault> vaultProviders = ImmutableList.copyOf(loader.iterator());

        if (Boolean.getBoolean(ENABLE_INMEMORY_PROPERTY)) {
            vault = new DefaultVault();
        } else if (vaultProviders.isEmpty()) {
            vault = new NoopVault();
        } else {
            if (vaultProviders.size() != 1) {
                throw new IllegalArgumentException(String.format(
                        "VaultModule expecting only 1 vault provider of type LarkyVault, found %d",
                        vaultProviders.size()
                ));
            }

            vault = vaultProviders.get(0);
        }

    }

    @StarlarkMethod(
            name = "redact",
            doc = "generates an alias for value",
            parameters = {
                    @Param(
                            name = "value",
                            doc = "value to alias",
                            allowedTypes = {
                                    @ParamType(type = String.class),
                                    @ParamType(type = List.class, generic1 = String.class)
                            }),
                    @Param(
                            name = "storage",
                            doc = "storage type ('persistent' or 'volatile')",
                            named = true,
                            defaultValue = "None",
                            allowedTypes = {
                                    @ParamType(type = NoneType.class),
                                    @ParamType(type = String.class),
                            }),
                    @Param(
                            name = "format",
                            doc = "standard VGS alias format",
                            named = true,
                            defaultValue = "None",
                            allowedTypes = {
                                    @ParamType(type = NoneType.class),
                                    @ParamType(type = String.class),
                            }),
                    @Param(
                            name = "tags",
                            doc = "list of tags to classify data",
                            named = true,
                            defaultValue = "[]",
                            allowedTypes = {
                                    @ParamType(type = List.class)
                            }),
                    @Param(
                            name = "decorator_config",
                            doc = "alias decorator config",
                            named = true,
                            defaultValue = "None",
                            allowedTypes = {
                                @ParamType(type = NoneType.class),
                                @ParamType(type = Map.class),
                        }),
            })
    @Override
    public Object redact(Object value, Object storage, Object format, List<Object> tags, Object decoratorConfig) throws EvalException {
        validateStorage(storage);
        validateFormat(format);
        validateDecoratorConfig(decoratorConfig);

        return vault.redact(value, storage, format, tags, decoratorConfig);
    }

    @StarlarkMethod(
            name = "reveal",
            doc = "reveals aliased value",
            parameters = {
                    @Param(
                            name = "value",
                            doc = "alias to reveal",
                            allowedTypes = {
                                    @ParamType(type = String.class),
                                    @ParamType(type = List.class, generic1 = String.class)
                            }),
                    @Param(
                            name = "storage",
                            doc = "storage type ('persistent' or 'volatile')",
                            named = true,
                            defaultValue = "None",
                            allowedTypes = {
                                    @ParamType(type = NoneType.class),
                                    @ParamType(type = String.class),
                            })
            })
    @Override
    public Object reveal(Object value, Object storage) throws EvalException {
        validateStorage(storage);

        return vault.reveal(value, storage);
    }

    private void validateStorage(Object storage) throws EvalException {
        if (!(storage instanceof NoneType) && !(storage instanceof String)) {
            throw Starlark.errorf(String.format(
                    "Storage of type %s is not supported in VAULT, expecting String",
                    storage.getClass().getName()
            ));
        } else if ((storage instanceof String) && !supportedStorage.contains(storage.toString())) {
            throw Starlark.errorf(String.format(
                    "Storage '%s' not found in supported storage types: %s",
                    storage,
                    supportedStorage.toString()
            ));
        }
    }

    private void validateFormat(Object format) throws EvalException {
        if (!(format instanceof NoneType) && !(format instanceof String)) {
            throw Starlark.errorf(String.format(
                    "Format of type %s is not supported in VAULT, expecting String",
                    format.getClass().getName()
            ));
        } else if ((format instanceof String) && !supportedFormat.contains(format.toString())) {
            throw Starlark.errorf(String.format(
                    "Format '%s' not found in supported format types: %s",
                    format,
                    supportedFormat.toString()
            ));
        }
    }

    private void validateDecoratorConfig(Object decoratorConfig) throws EvalException {
        if (decoratorConfig != null && !(decoratorConfig instanceof NoneType) && !(decoratorConfig instanceof Map)) {
            throw Starlark.errorf(String.format(
                "Decorator config of type %s is not supported in VAULT, expecting Map",
                decoratorConfig.getClass().getName()
            ));
        } else {
            try {
                DecoratorConfig.fromObject(decoratorConfig);
            } catch (InvalidDecoratorConfigException e) {
                throw Starlark.errorf(String.format(
                    "Decorator config '%s' is invalid. %s",
                    decoratorConfig, e.getMessage()
                ));
            }
        }
    }


}
