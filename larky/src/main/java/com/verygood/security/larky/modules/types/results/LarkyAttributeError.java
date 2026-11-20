package com.verygood.security.larky.modules.types.results;

import java.util.Objects;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.StarlarkSemantics;

/**
 * The {@code AttributeError()} value: one shared object, returned to scripts and compared by identity (see
 * {@link #sentinel()}). It is a {@link Result} error but not an exception, so it cannot be thrown:
 * {@code throw LarkyAttributeError.sentinel()} does not compile.
 *
 * <p>It must not be one. When an exception leaves a Starlark call, Starlark records the call stack
 * on it, once ({@code EvalException.ensureStack} keeps the first stack it is given). A shared
 * exception would carry the first evaluation's file, line and function names forever and report
 * them in every later evaluation's error. So code that throws this error ({@code unwrap()}) asks
 * for {@link #getError()}, which is a new exception each time.
 */
@StarlarkBuiltin(name = "AttributeError")
public final class LarkyAttributeError implements Result {

  private static final String MESSAGE = "AttributeError";

  private static final LarkyAttributeError SENTINEL = new LarkyAttributeError();

  private LarkyAttributeError() {}

  /** The shared {@code AttributeError()} value. It cannot be thrown. */
  public static LarkyAttributeError sentinel() {
    return SENTINEL;
  }

  @Override
  public Object getValue() {
    return this;
  }

  /**
   * A new {@link Error} holding this value, with a new exception, so a caller that throws it
   * raises a fresh one.
   */
  @Override
  public Error getError() {
    return Error.of(this);
  }

  @Override
  public boolean isOk() {
    return false;
  }

  @Override
  public boolean isError() {
    return true;
  }

  @Override
  public boolean isImmutable() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    return o == this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClass(), false, MESSAGE);
  }

  @Override
  public String toString() {
    return "Error{" + MESSAGE + '}';
  }

  @Override
  public void repr(Printer printer, StarlarkSemantics semantics) {
    printer.append(toString());
  }

  @Override
  public void str(Printer printer, StarlarkSemantics semantics) {
    printer.append(MESSAGE);
  }
}
