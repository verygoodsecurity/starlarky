package com.verygood.security.larky.modules.types;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

public class LarkyBoundedWhileTrue implements StarlarkValue {

  /**
   * The maximum number of iterations allowed for this instance.
   * <p>
   * This value is always of type {@code int}, as Java's {@code int} type enforces a
   * 32-bit signed integer constraint. This is intentional; the bound must always fit
   * within a signed 32-bit integer range. Iterators using this class will never permit
   * more iterations than can be represented by a 32-bit int.
   */
  private final int bound;
  private final String limitExceededMsg;

  private LarkyBoundedWhileTrue(int bound, String limitExceededMsg) {
    this.bound = bound;
    this.limitExceededMsg = limitExceededMsg;
  }

  public static LarkyIterator of(int maxIterations, String errorMessage,
                                 StarlarkThread thread) {
    LarkyIterator iterator =
        new LarkyBoundedWhileTrueIterator(errorMessage, maxIterations);
    iterator.setCurrentThread(thread);
    return iterator;
  }

  @Override
  public void repr(Printer p) {
    p.append("BoundedWhileTrue(bound=")
        .append(String.valueOf(bound))
        .append(", limit_exceed_msg=")
        .append(limitExceededMsg)
        .append(")");
  }
  
  private static class LarkyBoundedWhileTrueIterator extends LarkyIterator {

    private final String errorMessage;
    private int counter = 0;
    private final int maxIterations;

    private LarkyBoundedWhileTrueIterator(String errorMessage, int maxIterations) {
      this.errorMessage = errorMessage;
      this.maxIterations = maxIterations;
    }

    @Override
    public Object __next__() throws EvalException {
      if (counter >= maxIterations) {
        throw Starlark.errorf("%s", errorMessage);
      }
      counter++;
      return true; // Return True for each iteration
    }

    @Override
    public boolean hasNext() {
      try {
        return (boolean) __next__();
      } catch (EvalException e) {
        throw new RuntimeException(e);
      }
    }
    
    @Override
    public String typeName() {
      return "bounded_while_true_iterator";
    }

    @Override
    public void repr(Printer p) {
      p.append("bounded_while_true_iterator(")
          .append("limit_exceed_msg=")
          .append("\"")
          .append(errorMessage)
          .append("\", max_iterations=")
          .append(String.valueOf(maxIterations))
          .append(")");
    }
  }
}

