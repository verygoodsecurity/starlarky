package com.verygood.security.larky.modules.types.results;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkValue;

@StarlarkBuiltin(name="StopIteration")
public final class LarkyStopIteration extends Error implements StarlarkValue {

  private LarkyStopIteration(EvalException exc) {
    super(exc);
  }

  /**
   * Shared value for scripts that <em>return</em> StopIteration (compared by identity). Never throw
   * it: an exception records its call stack once, so a shared one would report the first
   * evaluation's location and function names to every later evaluation. Use {@link #create()}.
   */
  public static final LarkyStopIteration INSTANCE =
      new LarkyStopIteration(new EvalException("StopIteration"));

  public static LarkyStopIteration getInstance() {
    return INSTANCE;
  }

  /** Returns a new exception, to be thrown by exactly one raise. */
  public static LarkyStopIteration create() {
    return new LarkyStopIteration(new EvalException("StopIteration"));
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
