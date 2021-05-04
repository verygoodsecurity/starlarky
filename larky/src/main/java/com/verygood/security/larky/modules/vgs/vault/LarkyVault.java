package com.verygood.security.larky.modules.vgs.vault;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.StarlarkValue;

import java.util.List;

public interface LarkyVault extends StarlarkValue {

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
                                    @ParamType(type=NoneType.class),
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
    Object put(Object value, Object storage, Object format) throws EvalException;

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
    Object get(Object value, Object storage) throws EvalException;

}
