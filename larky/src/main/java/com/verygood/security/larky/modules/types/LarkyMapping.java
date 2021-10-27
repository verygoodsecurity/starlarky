package com.verygood.security.larky.modules.types;

import java.util.NavigableMap;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkMapping;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;


public interface LarkyMapping<K, V> extends StarlarkMapping<K, V>, LarkyIndexable {

  void freeze(); // this.mutability = Mutability.IMMUTABLE;
  boolean updateIteratorCount(int delta);
  NavigableMap<K, V> contents(); // deterministic

  @Override
  default Object getIndex(StarlarkThread thread, StarlarkSemantics semantics, Object key) throws EvalException {
    Object v = get(key);
    if (v == null) {
      throw Starlark.errorf("key %s not found in dictionary", Starlark.repr(key));
    }
    return v;
  }

  @Override
  default boolean containsKey(StarlarkThread thread, StarlarkSemantics semantics, Object key) throws EvalException {
    Starlark.checkHashable(key);
    return this.containsKey(key);
  }

}

