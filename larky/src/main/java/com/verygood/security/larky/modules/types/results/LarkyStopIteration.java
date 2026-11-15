package com.verygood.security.larky.modules.types.results;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkValue;

/**
 * Python's {@code StopIteration}, in two roles that must not be mixed up.
 *
 * <ul>
 *   <li><b>Thrown</b>, to end an iteration ({@code next()}, iterators): use {@link #newInstance()},
 *       a new exception for every raise. When an exception leaves a Starlark call, Starlark records
 *       the call stack on it, once ({@code EvalException.ensureStack} keeps the first stack it is
 *       given). One shared exception would therefore carry the first evaluation's file, line and
 *       function names forever, and report them in the error of every later evaluation, of any
 *       tenant, running at the same time or not. Errors are also kept after they are caught
 *       ({@code safe()}, a host's log), so a shared one could not even be reset safely.
 *   <li><b>Returned</b>, as a marker value: {@link #sentinel()}, the one shared object, which
 *       {@code StopIteration()} returns to scripts and {@code LarkyIterator} compares by identity.
 *       It is never thrown, so no stack is ever recorded on it.
 * </ul>
 */
@StarlarkBuiltin(name="StopIteration")
public final class LarkyStopIteration extends Error implements StarlarkValue {

  private static final String MESSAGE = "StopIteration";

  /** The shared marker value. Never throw it: see the class documentation. */
  private static final LarkyStopIteration SENTINEL =
      new LarkyStopIteration(new EvalException(MESSAGE));

  private LarkyStopIteration(EvalException exc) {
    super(exc);
  }

  /** A new StopIteration to throw, for exactly one raise. */
  public static LarkyStopIteration newInstance() {
    return new LarkyStopIteration(new EvalException(MESSAGE));
  }

  /**
   * The shared StopIteration value, to return or compare by identity. Never throw it: use {@link
   * #newInstance()}.
   */
  public static LarkyStopIteration sentinel() {
    return SENTINEL;
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

}
