package com.verygood.security.larky.modules.types;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Structure;
import net.starlark.java.spelling.SpellChecker;

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

  default boolean hasReprField() throws EvalException {
    return getField(PyProtocols.__REPR__) != null;
  }

  default boolean hasClassField() throws EvalException {
    return getField(PyProtocols.__CLASS__) != null;
  }

  default boolean hasNameField() throws EvalException {
    return getField(PyProtocols.__NAME__) != null;
  }

  // TODO(mahmoudimus): should this move to a sizeable interface?
  default boolean hasLenField() throws EvalException {
    return getField(PyProtocols.__LEN__) != null;
  }

  default Object get__len__() throws EvalException {
    return getField(PyProtocols.__LEN__);
  }

  /**
   * Returns the name of the type of a value as if by the Starlark expression {@code type(x)}.
   */
  default String type() {
    return StarlarkUtil.richType(this);
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
    String starlarkType = Starlark.type(this);
    String larkyType = type();
    if(!larkyType.equals(starlarkType)) {
      starlarkType += String.format(" of class '%s'",larkyType);
    }

    return String.format(
      "%s has no field or method '%s'%s",
      starlarkType,
      field,
      SpellChecker.didYouMean(field,
        Starlark.dir(
          getCurrentThread() != null
            ? getCurrentThread().mutability()
            : null,
          getCurrentThread() != null
            ? getCurrentThread().getSemantics()
            : StarlarkSemantics.DEFAULT,
          this)));
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
    return invoke(getCurrentThread(), function, args, kwargs);
  }

  default Object invoke(StarlarkThread thread, Object function, List<Object> args, Map<String, Object> kwargs) throws EvalException {
    try {
      return Starlark.call(thread, function, args, kwargs);
    } catch (InterruptedException e) {
      throw new EvalException(e.getMessage(), e.fillInStackTrace());
    }
  }

}
