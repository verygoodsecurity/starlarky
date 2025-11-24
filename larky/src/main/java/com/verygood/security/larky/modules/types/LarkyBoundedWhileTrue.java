package com.verygood.security.larky.modules.types;

import com.google.common.collect.ImmutableList;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkEvalWrapper;
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
        new LarkyBoundedWhileTrueIterator(errorMessage, maxIterations, thread);
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
      private final ImmutableList<StarlarkThread.CallStackEntry> callStack;
      private LarkyBoundedWhileTrueIterator(String errorMessage,
                                            int maxIterations,
                                            StarlarkThread thread) {
        this.setCurrentThread(thread);
        this.errorMessage = errorMessage;
        this.maxIterations = maxIterations;
        this.callStack = captureLoopCallStack(thread);
      }

      private ImmutableList<StarlarkThread.CallStackEntry> captureLoopCallStack(StarlarkThread thread) {
        int depth = StarlarkEvalWrapper.CallStack.depth(thread); 
        if (depth < 2) {
          return thread.getCallStack();
        }
        // depth - 2 trims `larky.star:_while_true()` and the internal `_WhileTrue` class
        return thread.getCallStack()
            // subList() toIndex is inclusive, so the check above gives us the correct
            // index to trim to
            .subList(0, depth - 2);
      }
      
      private boolean withinLoopBound() {
        return counter < maxIterations;
      }
      
      private StarlarkEvalWrapper.Exc.WrappedUncheckedEvalException limitExceededException() {
        return StarlarkEvalWrapper.Exc.WrappedUncheckedEvalException.of(
            errorMessage, 
            this.getCurrentThread(), 
            this.callStack
          );
      }
      
      @Override
      public Object __next__() throws EvalException {
        if (!withinLoopBound()) {
          throw limitExceededException();
        }
        counter++;
        return true;
      }

      @Override
      public boolean hasNext() {
        // if (!withinLoopBound()) {
        //   throw limitExceededException();
        // }
        return true;
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
