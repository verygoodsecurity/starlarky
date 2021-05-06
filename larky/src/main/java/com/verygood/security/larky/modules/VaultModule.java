package com.verygood.security.larky.modules;

import com.google.common.collect.Iterators;
import com.verygood.security.larky.modules.vgs.vault.DefaultVault;
import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import com.verygood.security.larky.modules.vgs.vault.NoopVault;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;

import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;


@StarlarkBuiltin(
        name = "vault",
        category = "BUILTIN",
        doc = "Overridable Vault API in Larky")
public class VaultModule implements LarkyVault {

    public static final VaultModule INSTANCE = new VaultModule();
    public static final String PROPERTY_NAME = "larky.modules.vgs.vault.enableInMemoryVault";

    private LarkyVault vault;

    public VaultModule() {

        ServiceLoader<LarkyVault> loader = ServiceLoader.load(LarkyVault.class);
        Iterator<LarkyVault> vaultIterator = loader.iterator();

        if (Boolean.getBoolean(PROPERTY_NAME)) {
            vault = new DefaultVault();
        } else if (!vaultIterator.hasNext()) {
            vault = new NoopVault();
        } else {
            vault = vaultIterator.next();
            if (vaultIterator.hasNext()) {
                throw new IllegalArgumentException(
                        "Expecting only 1 service provider of type LarkyVault, found "
                                + (1 + Iterators.size(vaultIterator))
                );
            }
        }

    }

    @StarlarkMethod(
            name = "put",
            doc = "tokenizes value",
            parameters = {
                    @Param(
                            name = "value",
                            doc = "value to tokenize",
                            allowedTypes = {
                                    @ParamType(type=String.class),
                                    @ParamType(type= List.class, generic1 = String.class)
                            }),
                    @Param(
                            name = "storage",
                            doc = "storage type ('persistent' or 'volatile')",
                            named = true,
                            defaultValue = "None",
                            allowedTypes = {
                                    @ParamType(type= NoneType.class),
                                    @ParamType(type=String.class),
                            }),
                    @Param(
                            name = "format",
                            doc = "standard VGS alias format",
                            named = true,
                            defaultValue = "None",
                            allowedTypes = {
                                    @ParamType(type= NoneType.class),
                                    @ParamType(type=String.class),
                            })
            })
    @Override
    public Object put(Object value, Object storage, Object format) throws EvalException {
        return vault.put(value, storage, format);
    }

    @StarlarkMethod(
            name = "get",
            doc = "reveals tokenized value",
            parameters = {
                    @Param(
                            name = "value",
                            doc = "token to reveal",
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
                                    @ParamType(type= NoneType.class),
                                    @ParamType(type=String.class),
                            })
            })
    @Override
    public Object get(Object value, Object storage) throws EvalException {
        return vault.get(value, storage);
    }

}