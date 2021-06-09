package com.verygood.security.larky.modules.types;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Structure;

import javax.annotation.Nullable;

public interface LarkyObject extends Structure {

  List<Object> EMPTY_ARGS = Collections.emptyList();
  Map<String, Object> EMPTY_KWARGS = Collections.emptyMap();

  StarlarkThread getCurrentThread();

  /**
   * Returns the value of the field with the given name, or null if the field does not exist. The
   * interpreter (Starlark code) calls the Structure#getField method, which has access to
   * StarlarkSemantics.
   *
   * <p>The set of names for which {@code getField} returns non-null should match {@code
   * getFieldNames} if possible.
   *
   * @throws EvalException if a user-visible error occurs (other than non-existent field).
   */
  @Nullable
  default Object getField(String name) throws EvalException {
    return getValue(name);
  }

  default boolean hasStrField() throws EvalException {
    return getField(PyProtocols.__STR__) != null;
  }

  default boolean hasClassField() throws EvalException {
    return getField(PyProtocols.__CLASS__) != null;
  }

  /**
   * Returns the name of the type of a value as if by the Starlark expression {@code type(x)}.
   */
  default String type() {
    try {
      if (!hasClassField()) {
        return Starlark.type(this);
      }
      return (String) getField(PyProtocols.__CLASS__);
    } catch (EvalException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  default void str(Printer printer) {
    try {
      if (!hasStrField()) {
        repr(printer);
        return;
      }
      String result = (String) invoke(getField(PyProtocols.__STR__)) ;
      printer.append(result);
    } catch (EvalException e) {
      throw new RuntimeException(e);
    }

  }

  @Nullable
  @Override
  default String getErrorMessageForUnknownField(String field) {
    return String.format("'%s' object has no attribute '%s'",
        Starlark.type(this),
        field);
  }

  default Object invoke(Object function) throws EvalException {
    return invoke(function, EMPTY_ARGS, EMPTY_KWARGS);
  }

  default Object invoke(Object function, List<Object> args) throws EvalException {
    return invoke(function, args, EMPTY_KWARGS);
  }

  default Object invoke(Object function, Map<String, Object> kwargs) throws EvalException {
    return invoke(function, EMPTY_ARGS, kwargs);
  }

  default Object invoke(Object function, List<Object> args, Map<String, Object> kwargs) throws EvalException {
    try {
      return Starlark.call(getCurrentThread(), function, args, kwargs);
    } catch (InterruptedException e) {
      throw new EvalException(e.getMessage(), e.fillInStackTrace());
    }
  }

}
