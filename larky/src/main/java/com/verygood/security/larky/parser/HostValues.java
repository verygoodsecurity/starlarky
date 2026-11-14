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

package com.verygood.security.larky.parser;

import com.google.common.collect.ImmutableList;
import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.modules.vgs.ModuleProviders;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;

/**
 * How embedding code (a service that evaluated a script and now reads its results) uses the
 * values of that script: converts them to strings, reads their fields, and calls them.
 *
 * <p>Script values are not plain data. A struct field may be a {@code larky.property} whose getter
 * is Starlark code, a struct may define {@code __getattr__}, {@code __str__} or {@code __repr__},
 * and a function value is Starlark code. Running any of that needs a {@link StarlarkThread}. The
 * thread that evaluated the script is finished: its {@link Mutability} is frozen and its step
 * count and deadline are spent, and values never keep it. Do not build a thread of your own with
 * {@code StarlarkSemantics.DEFAULT} and no step limit either: callbacks would then run with other
 * semantics than the script and without a bound on a script's loop.
 *
 * <p>Obtain an instance from the evaluation result: {@link ParsedStarFile#hostValues()}. It
 * carries that evaluation's
 *
 * <ul>
 *   <li>semantics ({@link LarkySemantics}),
 *   <li>step limit ({@link LarkyEvaluator#STEP_LIMIT}), applied to <em>each call</em> of this
 *       class: it bounds one access, not the sum of all accesses,
 *   <li>module providers ({@link LarkyEvaluator#MODULE_PROVIDERS}).
 * </ul>
 *
 * The expiration deadline ({@link LarkyEvaluator#EXPIRATION_MS}) is not inherited: it is an
 * absolute time that has typically passed by the time the host reads results. Bound host calls
 * with the step limit.
 *
 * <p>Every method creates a fresh {@link Mutability} and {@link StarlarkThread}, makes the thread
 * the {@link CurrentEvaluation} for the call (so thread-less callbacks such as {@code __eq__} in a
 * dict lookup run on it), and closes the mutability afterwards. Consequences:
 *
 * <ul>
 *   <li>Values created by a call (a list returned by a getter) are frozen when the call returns.
 *       They are safe to keep and read, and cannot be mutated by, or leak mutability into, a
 *       later call.
 *   <li>Nothing is shared between calls, so instances are thread-safe, and a getter that loops
 *       forever stops at the step limit with an {@link EvalException} instead of hanging.
 *   <li>{@code print()} output of the callbacks goes where it went before host code read values
 *       this way: an instance from {@link ParsedStarFile#hostValues()} sends it to the
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
 * HostValues host = parsed.hostValues();
 * Object result = parsed.getGlobalEnvironmentVariable("result", Object.class);
 * Object body = host.getField(result, "body");        // runs the property getter, fresh thread
 * Object headers = host.getField(result, "headers");  // frozen once the call returns
 * String text = host.str(body);
 * }</pre>
 *
 * <p>Do not call {@code struct.getField(name, null)}, {@code Starlark.str(value, semantics)} or
 * {@code Starlark.repr(value, semantics)} directly on script values after the evaluation: with no
 * evaluation active they fail with an {@link IllegalStateException}.
 */
public final class HostValues {

  private final StarlarkSemantics semantics;
  @Nullable private final Long maxSteps;
  @Nullable private final ModuleProviders providers;
  @Nullable private final StarlarkThread.PrintHandler printHandler;

  private HostValues(
      StarlarkSemantics semantics,
      @Nullable Long maxSteps,
      @Nullable ModuleProviders providers,
      @Nullable StarlarkThread.PrintHandler printHandler) {
    this.semantics = semantics;
    this.maxSteps = maxSteps;
    this.providers = providers;
    this.printHandler = printHandler;
  }

  /** Larky's default semantics, no step limit, no providers, Starlark's default print handler. */
  public static HostValues create() {
    return of(LarkySemantics.LARKY_SEMANTICS, null, null);
  }

  /**
   * @param maxSteps the step limit of each call, or null for none
   * @param providers the module providers visible to callbacks, or null
   */
  public static HostValues of(
      StarlarkSemantics semantics, @Nullable Long maxSteps, @Nullable ModuleProviders providers) {
    return new HostValues(semantics, maxSteps, providers, null);
  }

  /** The same, delivering {@code print()} output of callbacks to {@code handler}. */
  public HostValues withPrintHandler(StarlarkThread.PrintHandler handler) {
    return new HostValues(semantics, maxSteps, providers, handler);
  }

  /**
   * The same, leaving {@code print()} output of callbacks to Starlark's default handler (standard
   * error, prefixed with the caller's location), as on a {@link StarlarkThread} the host creates
   * itself and never gives a handler. For hosts that read values on their own thread today and must
   * keep printing where they did.
   */
  public HostValues withStarlarkDefaultPrintHandler() {
    return new HostValues(semantics, maxSteps, providers, null);
  }

  /** The semantics callbacks run with. */
  public StarlarkSemantics semantics() {
    return semantics;
  }

  /** The step limit of each call, or null if unlimited. */
  @Nullable
  public Long maxSteps() {
    return maxSteps;
  }

  /** A body that runs on a fresh thread. */
  @FunctionalInterface
  private interface Body<T> {
    T run(Mutability mu, StarlarkThread thread) throws EvalException, InterruptedException;
  }

  private <T> T run(String name, Body<T> body) throws EvalException, InterruptedException {
    try (Mutability mu = Mutability.create("HostValues." + name)) {
      StarlarkThread thread = StarlarkThread.createTransient(mu, semantics);
      if (printHandler != null) {
        thread.setPrintHandler(printHandler);
      }
      if (providers != null) {
        providers.installOn(thread);
      }
      if (maxSteps != null) {
        thread.setMaxExecutionSteps(maxSteps);
      }
      try (CurrentEvaluation ignored = CurrentEvaluation.enter(thread)) {
        return body.run(mu, thread);
      }
    }
  }

  /** As Starlark's {@code str(value)}. */
  public String str(Object value) throws EvalException, InterruptedException {
    return run("str", (mu, thread) -> Starlark.str(value, semantics));
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
