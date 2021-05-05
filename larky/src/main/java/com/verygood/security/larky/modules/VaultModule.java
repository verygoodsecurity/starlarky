package com.verygood.security.larky.modules;

import com.verygood.security.larky.modules.vgs.LarkyVGSOverridable;
import com.verygood.security.larky.modules.vgs.vault.LarkyVault;
import com.verygood.security.larky.modules.vgs.vault.NoopVault;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;


@StarlarkBuiltin(
        name = "vault",
        category = "BUILTIN",
        doc = "Overridable Vault API in Larky")
public class VaultModule implements LarkyVault, LarkyVGSOverridable {

    public static final VaultModule INSTANCE = new VaultModule();

    private LarkyVault vault = new NoopVault();

    @Override
    public Object put(Object value, Object storage, Object format) throws EvalException {
        return vault.put(value, storage, format);
    }

    @Override
    public Object get(Object value, Object storage) throws EvalException {
        return vault.get(value, storage);
    }

    @Override
    public void addOverride(Object vault) throws IllegalArgumentException {
        if ( !(vault instanceof LarkyVault) ) {
            throw new IllegalArgumentException(
                    "VaultModule override must be of type LarkyVault, found "
                            + vault.getClass()
            );
        }
        this.vault = (LarkyVault) vault;
    }

}
