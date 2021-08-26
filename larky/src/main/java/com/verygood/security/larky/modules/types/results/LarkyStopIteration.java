package com.verygood.security.larky.modules.types.results;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkValue;

@StarlarkBuiltin(name="StopIteration")
public final class LarkyStopIteration extends Error implements StarlarkValue {

  private LarkyStopIteration(EvalException exc) {
    super(exc);
  }

  private static final EvalException STOP_ITERATION = new EvalException("StopIteration");

  public static final LarkyStopIteration INSTANCE = new LarkyStopIteration(STOP_ITERATION);

  public static LarkyStopIteration getInstance() {
    return INSTANCE;
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
