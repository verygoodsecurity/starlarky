/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.larky.runtime;

import com.google.common.collect.ImmutableList;
import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.modules.vgs.ModuleProviders;
import com.verygood.security.larky.parser.ParsedStarFile;
import com.verygood.security.larky.parser.StarlarkUtil;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;

/**
 * Java access to Starlark values after a script finishes.
 *
 * <p>Script values are not plain data. A struct field may be a {@code larky.property} whose getter
 * is Starlark code, a struct may define {@code __getattr__}, {@code __str__} or {@code __repr__},
 * and a function value is Starlark code. Running any of that needs a {@link StarlarkThread}. The
 * thread that evaluated the script is finished (its {@link Mutability} is frozen), and callbacks
 * must not run on it after the evaluation. Do not build a thread of your own with {@code
 * StarlarkSemantics.DEFAULT} and no step limit either: callbacks would then run with other
 * semantics than the script and without the script's bounds.
 *
 * <p>Obtain an instance from the evaluation result: {@link ParsedStarFile#interop()}. It
 * carries that evaluation's
 *
 * <ul>
 *   <li>semantics ({@link LarkySemantics}),
 *   <li>remaining step budget: the step limit ({@link LarkyEvaluator#STEP_LIMIT}) minus the steps
 *       the evaluation used. All calls through the instance (and through instances derived from it
 *       with {@code with...}) draw on that one budget, as callbacks run on the evaluation's own
 *       thread did; once it is spent, every call fails with "too many steps". Calls running at the
 *       same time each start with the budget left when they start, so together they can exceed it
 *       by the steps of the calls that overlap,
 *   <li>expiration date ({@link LarkyEvaluator#EXPIRATION_MS}): the same absolute time; a call
 *       past it fails with "past expiration date", as on the evaluation's own thread,
 *   <li>module providers ({@link LarkyEvaluator#MODULE_PROVIDERS}).
 * </ul>
 *
 * <p>Every method creates a fresh {@link Mutability} and {@link StarlarkThread}, makes the thread
 * the {@link CurrentEvaluation} for the call (so thread-less callbacks such as {@code __eq__} in a
 * dict lookup run on it), and closes the mutability afterwards. Consequences:
 *
 * <ul>
 *   <li>Values created by a call (a list returned by a getter) are frozen when the call returns.
 *       They are safe to keep and read, and cannot be mutated by, or leak mutability into, a
 *       later call.
 *   <li>Calls share only the step budget, so instances are thread-safe, and a getter that loops
 *       forever stops at the step budget or the expiration date with an {@link EvalException}
 *       instead of hanging.
 *   <li>Every failure of a callback is an {@link EvalException}, also an unchecked exception
 *       thrown by Java code it calls (the {@code EvalException}'s cause).
 *   <li>{@code print()} output of the callbacks goes where it went before host code read values
 *       this way: an instance from {@link ParsedStarFile#interop()} sends it to the
 *       evaluation's reporter, as a callback run on the evaluation's own thread did; an instance
 *       without one ({@link #create()}, {@link #of}) leaves Starlark's default handler (standard
 *       error), as a thread the host created itself did. Use {@link #withPrintHandler} to send it
 *       elsewhere.
 * </ul>
 *
 * <p>Example, for a service that reads the response of a script whose result is a struct with a
 * computed {@code body} ({@code larky.property}) and a {@code headers} dict:
 *
 * <pre>{@code
 * ParsedStarFile parsed = interpreter.evaluate(script, console);
 * StarlarkInterop interop = parsed.interop();
 * Object result = parsed.getGlobalEnvironmentVariable("result", Object.class);
 * Object body = interop.getField(result, "body");        // runs the property getter, fresh thread
 * Object headers = interop.getField(result, "headers");  // frozen once the call returns
 * String text = interop.str(body);
 * }</pre>
 *
 * <p>Do not call {@code struct.getField(name, null)}, {@code Starlark.str(value, semantics)} or
 * {@code Starlark.repr(value, semantics)} directly on script values after the evaluation: with no
 * evaluation active they fail with an {@link IllegalStateException}.
 */
public final class StarlarkInterop {

  /** What all calls through an instance and the instances derived from it may use, together. */
  private static final class Budget {
    /** Steps left for all calls, or null for no limit. */
    @Nullable final AtomicLong remainingSteps;
    /** Absolute expiration date in ms since 1970, or Long.MAX_VALUE for none. */
    final long expirationMs;

    Budget(@Nullable Long maxSteps, long expirationMs) {
      this.remainingSteps = maxSteps == null ? null : new AtomicLong(Math.max(0, maxSteps));
      this.expirationMs = expirationMs;
    }
  }

  private final StarlarkSemantics semantics;
  @Nullable private final ModuleProviders providers;
  @Nullable private final StarlarkThread.PrintHandler printHandler;
  private final Budget budget;

  private StarlarkInterop(
      StarlarkSemantics semantics,
      @Nullable ModuleProviders providers,
      @Nullable StarlarkThread.PrintHandler printHandler,
      Budget budget) {
    this.semantics = java.util.Objects.requireNonNull(semantics, "semantics");
    this.providers = providers;
    this.printHandler = printHandler;
    this.budget = java.util.Objects.requireNonNull(budget, "budget");
  }

  /** Larky's default semantics, no step limit, no providers, Starlark's default print handler. */
  public static StarlarkInterop create() {
    return of(LarkySemantics.LARKY_SEMANTICS, null, null);
  }

  /**
   * @param maxSteps the steps all calls through the instance may use together, or null for no
   *     limit
   * @param providers the module providers visible to callbacks, or null
   */
  public static StarlarkInterop of(
      StarlarkSemantics semantics, @Nullable Long maxSteps, @Nullable ModuleProviders providers) {
    return of(semantics, maxSteps, Long.MAX_VALUE, providers);
  }

  /**
   * @param maxSteps the steps all calls through the instance may use together, or null for no
   *     limit
   * @param expirationMs the absolute time (ms since 1970) after which calls fail, or {@code
   *     Long.MAX_VALUE} for none
   * @param providers the module providers visible to callbacks, or null
   */
  public static StarlarkInterop of(
      StarlarkSemantics semantics,
      @Nullable Long maxSteps,
      long expirationMs,
      @Nullable ModuleProviders providers) {
    return new StarlarkInterop(semantics, providers, null, new Budget(maxSteps, expirationMs));
  }

  /**
   * The same (sharing this instance's step budget), delivering {@code print()} output of
   * callbacks to {@code handler}.
   */
  public StarlarkInterop withPrintHandler(StarlarkThread.PrintHandler handler) {
    return new StarlarkInterop(semantics, providers, handler, budget);
  }

  /**
   * The same, leaving {@code print()} output of callbacks to Starlark's default handler (standard
   * error, prefixed with the caller's location), as on a {@link StarlarkThread} the host creates
   * itself and never gives a handler. For hosts that read values on their own thread today and must
   * keep printing where they did.
   */
  public StarlarkInterop withStarlarkDefaultPrintHandler() {
    return new StarlarkInterop(semantics, providers, null, budget);
  }

  /** The semantics callbacks run with. */
  public StarlarkSemantics semantics() {
    return semantics;
  }

  /** The steps left for all calls together, or null if unlimited. */
  @Nullable
  public Long remainingSteps() {
    return budget.remainingSteps == null ? null : budget.remainingSteps.get();
  }

  /** The absolute expiration date of calls (ms since 1970), or {@code Long.MAX_VALUE} if none. */
  public long expirationMs() {
    return budget.expirationMs;
  }

  /** A body that runs on a fresh thread. */
  @FunctionalInterface
  private interface Body<T> {
    T run(Mutability mu, StarlarkThread thread) throws EvalException, InterruptedException;
  }

  private <T> T run(String name, Body<T> body) throws EvalException, InterruptedException {
    try {
      return runUnchecked(name, body);
    } catch (Starlark.UncheckedEvalException e) {
      // a RuntimeException thrown by Java code, decorated with the Starlark stack
      throw new EvalException(internalErrorMessage(name, e.getCause()), e);
    } catch (RuntimeException e) {
      for (Throwable c = e.getCause(); c != null; c = c.getCause()) {
        if (c instanceof EvalException) {
          throw (EvalException) c; // a Starlark error that Java code wrapped to pass it through
        }
      }
      throw new EvalException(internalErrorMessage(name, e), e);
    }
  }

  private static String internalErrorMessage(String name, @Nullable Throwable e) {
    return "internal error in StarlarkInterop." + name + ": "
        + (e == null ? "unknown" : e.getClass().getSimpleName()
            + (e.getMessage() == null ? "" : ": " + e.getMessage()));
  }

  /** As {@link #run}, but unchecked exceptions of the body propagate unchanged. */
  private <T> T runUnchecked(String name, Body<T> body) throws EvalException, InterruptedException {
    long startSteps = Long.MAX_VALUE;
    if (budget.remainingSteps != null) {
      startSteps = budget.remainingSteps.get();
      if (startSteps <= 0) {
        throw new EvalException("Starlark computation cancelled: too many steps");
      }
    }
    if (budget.expirationMs != Long.MAX_VALUE
        && System.currentTimeMillis() > budget.expirationMs) {
      throw new EvalException("Starlark computation cancelled: past expiration date");
    }
    try (Mutability mu = Mutability.create("StarlarkInterop." + name)) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, semantics);
      if (printHandler != null) {
        thread.setPrintHandler(printHandler);
      }
      if (providers != null) {
        providers.installOn(thread);
      }
      if (budget.remainingSteps != null) {
        thread.setMaxExecutionSteps(startSteps);
      }
      if (budget.expirationMs != Long.MAX_VALUE) {
        thread.setExpirationMs(budget.expirationMs);
      }
      try (CurrentEvaluation ignored = CurrentEvaluation.enter(thread)) {
        return body.run(mu, thread);
      } finally {
        if (budget.remainingSteps != null) {
          budget.remainingSteps.addAndGet(-thread.getExecutedSteps());
        }
      }
    }
  }

  /** As Starlark's {@code str(value)}. */
  public String str(Object value) throws EvalException, InterruptedException {
    return run("str", (mu, thread) -> Starlark.str(value, semantics));
  }

  /**
   * As {@code String.valueOf(value)}, Java's {@code toString}, for hosts that printed results that
   * way. A struct's {@code toString} runs its {@code __repr__}, so it needs an evaluation too.
   */
  public String javaString(Object value) throws EvalException, InterruptedException {
    return run("javaString", (mu, thread) -> String.valueOf(value));
  }

  /**
   * As {@link StarlarkUtil#valueToStarlark(Object)}: converts a host value (possibly a value of an
   * earlier evaluation, such as a binding a JSR-223 engine kept) to Starlark form. Copying a dict
   * hashes its keys, which runs a struct's {@code __hash__} and {@code __eq__}.
   *
   * @throws IllegalArgumentException for a value that has no Starlark form, as valueToStarlark
   */
  public Object toStarlark(Object value) throws EvalException, InterruptedException {
    return runUnchecked("toStarlark", (mu, thread) -> StarlarkUtil.valueToStarlark(value));
  }

  /** As Starlark's {@code repr(value)}. */
  public String repr(Object value) throws EvalException, InterruptedException {
    return run("repr", (mu, thread) -> Starlark.repr(value, semantics));
  }

  /**
   * The field or method {@code name} of {@code value}, as {@code getattr(value, name)}: runs a
   * {@code larky.property} getter or a {@code __getattr__}.
   *
   * @throws EvalException if there is no such attribute, or the callback fails
   */
  public Object getField(Object value, String name) throws EvalException, InterruptedException {
    return run("getField", (mu, thread) -> Starlark.getattr(mu, semantics, value, name, null));
  }

  /** As {@link #getField(Object, String)}, but {@code defaultValue} (non-null) if there is none. */
  public Object getField(Object value, String name, Object defaultValue)
      throws EvalException, InterruptedException {
    return run(
        "getField", (mu, thread) -> Starlark.getattr(mu, semantics, value, name, defaultValue));
  }

  /** As {@code hasattr(value, name)}. */
  public boolean hasField(Object value, String name) throws EvalException, InterruptedException {
    return run("hasField", (mu, thread) -> Starlark.hasattr(semantics, value, name));
  }

  /** Calls {@code callable} as {@code callable(*args)}. */
  public Object call(Object callable, Object... args) throws EvalException, InterruptedException {
    return call(callable, ImmutableList.copyOf(args), Dict.empty());
  }

  /** Calls {@code callable} as {@code callable(*args, **kwargs)}. */
  public Object call(Object callable, List<Object> args, Map<String, Object> kwargs)
      throws EvalException, InterruptedException {
    return run("call", (mu, thread) -> Starlark.call(thread, callable, args, kwargs));
  }

  /** Calls the method {@code method} of {@code value}, as {@code value.method(*args)}. */
  public Object callMethod(Object value, String method, Object... args)
      throws EvalException, InterruptedException {
    return run(
        "callMethod",
        (mu, thread) ->
            Starlark.call(
                thread,
                Starlark.getattr(mu, semantics, value, method, null),
                ImmutableList.copyOf(args),
                Dict.empty()));
  }
}
