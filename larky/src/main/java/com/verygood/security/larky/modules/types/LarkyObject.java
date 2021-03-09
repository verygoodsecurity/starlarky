package com.verygood.security.larky.modules.types;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Structure;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public abstract class LarkyObject implements Structure {

  List<Object> EMPTY_ARGS = Collections.emptyList();
  Map<String, Object> EMPTY_KWARGS = Collections.emptyMap();
  final StarlarkThread currentThread;

  public LarkyObject(StarlarkThread thread) {
    this.currentThread = thread;
  }
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
  public Object getField(String name) throws EvalException {
    return getValue(name);
  }

  public boolean hasStrField() throws EvalException {
    return getField(PyProtocols.__STR__) != null;
  }

  public boolean hasClassField() throws EvalException {
    return getField(PyProtocols.__CLASS__) != null;
  }

  /**
   * Returns the name of the type of a value as if by the Starlark expression {@code type(x)}.
   */
  public String type() {
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
  public void str(Printer printer) {
    try {
      if (!hasStrField()) {
        repr(printer);
        return;
      }
      String result = (String) Starlark.call(
          this.currentThread,
          getField(PyProtocols.__STR__),
          EMPTY_ARGS,
          EMPTY_KWARGS);
      printer.append(result);
    } catch (InterruptedException | EvalException e) {
      throw new RuntimeException(e);
    }
  }

}
