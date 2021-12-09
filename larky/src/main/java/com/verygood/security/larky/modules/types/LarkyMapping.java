package com.verygood.security.larky.modules.types;

import com.google.common.collect.ImmutableList;
import java.util.Map;

import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkMapping;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;


public interface LarkyMapping<K, V> extends StarlarkMapping<K, V>, LarkyIndexable {

  default Object get__setitem__() {
    try {
      return getField(PyProtocols.__SETITEM__);
    } catch (EvalException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Puts an entry into a dict, after validating that mutation is allowed.
   *
   * @param key the key of the added entry
   * @param value the value of the added entry
   * @throws EvalException if the key is invalid or the dict is frozen
   */
  @Override
  default void putEntry(K key, V value) throws EvalException {
    this.setIndex(getCurrentThread(), getCurrentThread().getSemantics(),key,value);
  }

  /**
   * Puts all the entries from a given map into the dict, after validating that mutation is allowed.
   *
   * @param map the map whose entries are added
   * @throws EvalException if some key is invalid or the dict is frozen
   */
  @Override
  default <K2 extends K, V2 extends V> void putEntries(Map<K2, V2> map) throws EvalException {
    Starlark.checkMutable(this);
    for (Map.Entry<K2, V2> e : map.entrySet()) {
      K2 k = e.getKey();
      Starlark.checkHashable(k);
      final Object __setitem__ = get__setitem__();
      if(StarlarkUtil.isCallable(__setitem__)) {
        this.invoke(getCurrentThread(), __setitem__, ImmutableList.of(k, e.getValue()), EMPTY_KWARGS);
        continue;
      }
      contents().put(k, e.getValue());
    }
  }

  default void setIndex(StarlarkThread thread, StarlarkSemantics semantics, K key, V value) throws EvalException {
    Starlark.checkMutable(this);
    Starlark.checkHashable(key);
    final Object __setitem__ = get__setitem__();
    if(__setitem__ != null && StarlarkUtil.isCallable(__setitem__)) {
      this.invoke(thread, __setitem__, ImmutableList.of(key, value), EMPTY_KWARGS);
      return;
    }
    contents().put(key, value);
  }

  @StarlarkMethod(
     name = "__setitem__",
     doc = "If not, insert <code>key</code> with a value of <code>value</code> "
           + "in the dictionary",
     parameters = {
       @Param(name = "key", doc = "The key."),
       @Param(name = "value", doc = "the value"),
     }, useStarlarkThread = true)
   default void setItem(K key, V value, StarlarkThread thread) throws EvalException {
     setIndex(thread,thread.getSemantics(),key,value);
   }

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

