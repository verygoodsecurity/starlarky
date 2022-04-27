package com.verygood.security.larky.objects.type;

import com.google.common.collect.ImmutableCollection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.verygood.security.larky.objects.PyObject;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;


public interface ForwardingLarkyType extends LarkyType {

  LarkyType delegate();

  default StarlarkThread getCurrentThread() {
    return delegate().getCurrentThread();
  }

  default Set<LarkyType> getAllSubclasses() {
    return delegate().getAllSubclasses();
  }

  default void setMRO(List<LarkyType> mro) {
    delegate().setMRO(mro);
  }

  default void setBaseClasses(LarkyType[] parentClasses) {
    delegate().setBaseClasses(parentClasses);
  }

  default PyObject __new__(Tuple args, Dict<String, Object> kwargs, StarlarkThread thread) {
    return delegate().__new__(args, kwargs, thread);
  }

  default Map<String, Object> getInternalDictUnsafe() {
    return delegate().getInternalDictUnsafe();
  }


  default Origin getOrigin() {
    return delegate().getOrigin();
  }


  default Object getBase() {
    return delegate().getBase();
  }

  default Tuple getBases() {
    return delegate().getBases();
  }

  default Tuple getMRO() {
    return delegate().getMRO();
  }

  default LarkyType typeClass() {
    return delegate().typeClass();
  }
  default LarkyType __class__() {
    return delegate().__class__();
  } // TODO: remove this from this interface


  default String __name__() {
    return delegate().__name__();
  } // TODO: remove this from this interface


  default ImmutableCollection<String> getFieldNames() {
    return delegate().getFieldNames();
  }


}
