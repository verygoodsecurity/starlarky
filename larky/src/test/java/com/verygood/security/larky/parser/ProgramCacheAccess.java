package com.verygood.security.larky.parser;

/** Exposes ProgramCache's counters to tests in other packages. */
public final class ProgramCacheAccess {
  private ProgramCacheAccess() {}

  public static long scriptCount() {
    return ProgramCache.scriptCount();
  }
}
