package com.verygood.security.larky.objects.type;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.verygood.security.larky.objects.PyObject;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import net.starlark.java.syntax.TokenKind;

import jakarta.annotation.Nullable;


public interface ForwardingLarkyType extends LarkyType {

  LarkyType delegate();

  @Override
  default StarlarkThread getCurrentThread() {
    return delegate().getCurrentThread();
  }

  @Override
  default Set<LarkyType> getAllSubclasses() {
    return delegate().getAllSubclasses();
  }

  @Override
  default void setBaseClasses(LarkyType[] parentClasses) {
    delegate().setBaseClasses(parentClasses);
  }

  @Override
  default PyObject __new__(Tuple args, Dict<String, Object> kwargs, StarlarkThread thread) {
    return delegate().__new__(args, kwargs, thread);
  }

  @Override
  default Map<String, Object> getInternalDictUnsafe() {
    return delegate().getInternalDictUnsafe();
  }

  @Override
  default Origin getOrigin() {
    return delegate().getOrigin();
  }

  @Override
  default Object getBase() {
    return delegate().getBase();
  }

  @Override
  default Tuple getBases() {
    return delegate().getBases();
  }

  @Override
  default Tuple getMRO() {
    return delegate().getMRO();
  }

  @Override
  default void setMRO(List<LarkyType> mro) {
    delegate().setMRO(mro);
  }

  @Override
  default LarkyType typeClass() {
    return delegate().typeClass();
  }

  @Override
  default LarkyType __class__() {
    return delegate().__class__();
  } // TODO: remove this from this interface


  @Override
  default String __name__() {
    return delegate().__name__();
  } // TODO: remove this from this interface


  @Override
  default ImmutableCollection<String> getFieldNames() {
    return delegate().getFieldNames();
  }

  @Nullable
  @Override
  default Object binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException {
    return delegate().binaryOp(op, that, thisLeft);
  }

  @Override
  default ImmutableSet<SpecialMethod> getSpecialMethods() {
    return delegate().getSpecialMethods();
  }
}
