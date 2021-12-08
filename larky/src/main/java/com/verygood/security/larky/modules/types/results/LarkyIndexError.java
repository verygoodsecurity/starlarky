package com.verygood.security.larky.modules.types.results;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkValue;

@StarlarkBuiltin(name="IndexError")
public final class LarkyIndexError extends Error implements StarlarkValue {

  private LarkyIndexError(EvalException exc) {
    super(exc);
  }

  private static final EvalException INDEX_ERROR = new EvalException("IndexError");

  public static final LarkyIndexError INSTANCE = new LarkyIndexError(INDEX_ERROR);

  public static LarkyIndexError getInstance() {
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
