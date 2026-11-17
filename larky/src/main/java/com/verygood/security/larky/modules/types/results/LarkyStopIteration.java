package com.verygood.security.larky.modules.types.results;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkValue;

/**
 * Python's {@code StopIteration} when it is <b>thrown</b> to end an iteration ({@code next()},
 * iterators). Every raise uses a new instance from {@link #newInstance()}.
 *
 * <p>A thrown StopIteration can never be shared. When an exception leaves a Starlark call, Starlark
 * records the call stack on it, once ({@code EvalException.ensureStack} keeps the first stack it is
 * given). One shared exception would therefore carry the first evaluation's file, line and function
 * names forever, and report them in the error of every later evaluation, of any tenant, running at
 * the same time or not. Errors are also kept after they are caught ({@code safe()}, a host's log),
 * so a shared one could not even be reset safely.
 *
 * <p>StopIteration as a <b>value</b> (what {@code StopIteration()} returns to scripts, and what
 * {@code LarkyIterator} compares returned values with) is {@link Sentinel}, which is not an
 * exception: {@code throw LarkyStopIteration.sentinel()} does not compile. The two are equal to each
 * other in Starlark, as every StopIteration was before.
 */
@StarlarkBuiltin(name = "StopIteration")
public final class LarkyStopIteration extends Error implements StarlarkValue {

  private static final String MESSAGE = "StopIteration";

  private static final Sentinel SENTINEL = new Sentinel();

  private LarkyStopIteration(EvalException exc) {
    super(exc);
  }

  /** A new StopIteration to throw, for exactly one raise. */
  public static LarkyStopIteration newInstance() {
    return new LarkyStopIteration(new EvalException(MESSAGE));
  }

  /** The shared StopIteration value, to return or compare by identity. It cannot be thrown. */
  public static Sentinel sentinel() {
    return SENTINEL;
  }

  /** Whether {@code o} is a StopIteration, thrown or the sentinel. */
  static boolean isStopIteration(Object o) {
    return o instanceof LarkyStopIteration || o instanceof Sentinel;
  }

  @Override
  public boolean equals(Object o) {
    return isStopIteration(o);
  }

  @Override
  public int hashCode() {
    return MESSAGE.hashCode();
  }

  @Override
  public boolean isImmutable() {
    return true;
  }

  @Override
  public Object getValue() {
    return this;
  }

  @Override
  public Error getError() {
    return this;
  }

  @Override
  public boolean isOk() {
    return false;
  }

  @Override
  public boolean isError() {
    return true;
  }

  /**
   * StopIteration as a value: the one shared object that {@code StopIteration()} returns. It is a
   * {@link Result} error like a caught StopIteration, but not an exception, so no call stack can
   * ever be recorded on it and it cannot be thrown. Code that needs to throw one asks it for {@link
   * #getError()}, which is a new {@link LarkyStopIteration}.
   */
  @StarlarkBuiltin(name = "StopIteration")
  public static final class Sentinel implements Result {

    private Sentinel() {}

    @Override
    public Object getValue() {
      return this;
    }

    /** A new exception for this StopIteration, so a caller that throws it raises a fresh one. */
    @Override
    public Error getError() {
      return newInstance();
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
      return isStopIteration(o);
    }

    @Override
    public int hashCode() {
      return MESSAGE.hashCode();
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
}
