package com.verygood.security.larky.modules;

import com.verygood.security.larky.modules.vgs.vault.LarkyVault;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;


@StarlarkBuiltin(
        name = "vault",
        category = "BUILTIN",
        doc = "")
public class VaultModule implements LarkyVault {

    public static final VaultModule INSTANCE = new VaultModule();

    private LarkyVault _vault = null;

    @Override
    public Object put(Object value, Object storage, Object format) throws EvalException {
        if ( _vault == null ) {
            throw Starlark.errorf("vault.put operation must be overridden");
        }

        return _vault.put(value,storage,format);
    }

    @Override
    public Object get(Object value, Object storage) throws EvalException {
        if ( _vault == null ) {
            throw Starlark.errorf("vault.get operation must be overridden");
        }

        return _vault.get(value,storage);
    }

    public void addOverride(LarkyVault vault) {
        _vault = vault;
    }

}
