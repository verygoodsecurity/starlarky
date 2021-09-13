package com.verygood.security.larky.modules.types;

import java.util.concurrent.atomic.AtomicLong;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;


public interface LarkyCounter extends StarlarkValue {

  /**
   * Adds the given delta to the counters current value
   *
   * @param delta the delta to add
   * @return the counters updated value
   */
  @StarlarkMethod(name = "add_and_get", parameters = {@Param(name="delta", defaultValue = "1")})
  default StarlarkInt Counter__addAndGet(StarlarkInt delta) throws EvalException {
    return StarlarkInt.of(addAndGet(delta.toLong("not a long!")));
  }

  /**
   * Adds the given delta to the counters current value
   *
   * @param delta the delta to add
   * @return the counters updated value
   */
  long addAndGet(long delta);

  /**
   * Returns the counters current value
   *
   * @return the counters current value
   */
  @StarlarkMethod(name = "get")
  default StarlarkInt Counter__get() {
    return StarlarkInt.of(get());
  }

  /**
   * Returns the counters current value
   *
   * @return the counters current value
   */
  long get();

  /** Returns a new counter. The returned counter is not thread-safe. */
  static LarkyCounter newCounter() {
    return new NotThreadSafeLarkyCounter();
  }

  /** Returns a new counter. The returned counter <i>is</i> thread-safe. */
  static LarkyCounter threadSafeCounter()  {
    return new AtomicLarkyCounter();
  }


  final class NotThreadSafeLarkyCounter implements LarkyCounter {
    private long count = 0;

    @Override
    public long addAndGet(long delta) {
      return count += delta;
    }

    @Override
    public long get() {
      return count;
    }
    ;
  }

  final class AtomicLarkyCounter implements LarkyCounter {
    private final AtomicLong count = new AtomicLong();

    @Override
    public long addAndGet(long delta) {
      return count.addAndGet(delta);
    }

    @Override
    public long get() {
      return count.get();
    }
  }
}