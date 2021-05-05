package com.verygood.security.larky.modules.vgs;

import net.starlark.java.eval.StarlarkValue;

public interface LarkyVGSOverridable extends StarlarkValue {
    void addOverride(Object o) throws IllegalArgumentException;
}
