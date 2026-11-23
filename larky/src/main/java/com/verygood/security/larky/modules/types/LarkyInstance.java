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

package com.verygood.security.larky.modules.types;

import com.google.common.collect.ImmutableList;
import com.verygood.security.larky.parser.CurrentEvaluation;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkEvalWrapper;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

/**
 * An instance of a Python class whose class is written in Java.
 *
 * <p>The type declares its attributes: properties (data descriptors, with a getter and usually a
 * setter) and plain attributes (slots). Its methods are {@code @StarlarkMethod}s. Any other
 * attribute a script sets goes into the instance's own dict, as on a Python object. Lookup
 * follows Python: declared attributes first, then the instance dict, which can shadow a method,
 * then the methods. A type whose methods are not {@code @StarlarkMethod}s returns them from
 * {@link #getMethod}.
 *
 * <p>An instance belongs to the {@link Mutability} of the evaluation that creates it and freezes
 * with it. It keeps no {@link StarlarkThread}; calls that need one use the evaluation active on
 * the current Java thread.
 */
public abstract class LarkyInstance implements LarkyObject, Mutability.Freezable {

  private final Object constructor;
  private final Mutability mutability;
  // Attributes the type does not declare, created on first use.
  @Nullable private Dict<String, Object> attributes;

  protected LarkyInstance(Object constructor, Mutability mutability) {
    this.constructor = constructor;
    this.mutability = mutability;
  }

  /** The class name scripts see, as {@code __name__} and {@code type(x)}. */
  @Override
  public abstract String typeName();

  /**
   * The value of the declared attribute {@code name}, or null if the type declares none of that
   * name or it is not set.
   */
  @Nullable
  protected abstract Object getDeclared(String name, StarlarkThread thread)
      throws EvalException, InterruptedException;

  /**
   * Sets the declared attribute {@code name}. Returns false if the type declares none of that
   * name, so the value goes into the instance dict. The instance is mutable when this is called.
   */
  protected abstract boolean setDeclared(String name, Object value, StarlarkThread thread)
      throws EvalException, InterruptedException;

  /**
   * Adds the instance's declared state (its plain attributes, not properties or methods), in
   * order, as Python's {@code __dict__} shows it.
   */
  protected abstract void addState(Map<String, Object> state);

  /** The names of the declared attributes that are set, for {@code dir()}. */
  protected abstract ImmutableList<String> declaredNames();

  /**
   * The method {@code name}, for a type whose methods are not {@code @StarlarkMethod}s, or null.
   * It is looked up after the instance dict, so an attribute of the same name overrides it.
   */
  @Nullable
  protected Object getMethod(String name) {
    return null;
  }

  /** The names {@link #getMethod} answers, for {@code dir()}. */
  protected ImmutableList<String> methodNames() {
    return ImmutableList.of();
  }

  /** The attribute {@code name} set in the instance dict, or null. */
  @Nullable
  protected final Object instanceAttribute(String name) {
    return attributes == null ? null : attributes.get(name);
  }

  @Override
  public final Mutability mutability() {
    return mutability;
  }

  /** Fails if the instance is frozen. Every change to its state goes through this. */
  protected final void checkMutable() throws EvalException {
    Starlark.checkMutable(this);
  }

  @Override
  public final boolean isImmutable() {
    return mutability.isFrozen();
  }

  @Override
  public final StarlarkThread getCurrentThread() {
    return CurrentEvaluation.currentOrNull();
  }

  @Nullable
  @Override
  public final Object getField(String name, @Nullable StarlarkThread thread) {
    switch (name) {
      case PyProtocols.__CLASS__:
        return constructor;
      case PyProtocols.__NAME__:
        return typeName();
      case PyProtocols.__DICT__:
        return dunderDict();
      default:
        break;
    }
    StarlarkThread active = thread != null ? thread : CurrentEvaluation.currentOrNull();
    try {
      Object value = getDeclared(name, active);
      if (value != null) {
        return value;
      }
    } catch (EvalException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new StarlarkEvalWrapper.Exc.RuntimeEvalException(e, active);
    }
    Object attribute = instanceAttribute(name);
    return attribute != null ? attribute : getMethod(name);
  }

  @Override
  public final void setField(String name, Object value) throws EvalException {
    checkMutable();
    if (PyProtocols.__CLASS__.equals(name) || PyProtocols.__NAME__.equals(name)
        || PyProtocols.__DICT__.equals(name)) {
      throw Starlark.errorf("%s attribute '%s' is read-only", typeName(), name);
    }
    StarlarkThread thread = CurrentEvaluation.requireThread();
    try {
      if (setDeclared(name, value, thread)) {
        return;
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new EvalException(e);
    }
    if (attributes == null) {
      attributes = Dict.of(mutability);
    }
    attributes.putEntry(name, value);
  }

  @Override
  public final ImmutableList<String> getFieldNames() {
    ImmutableList.Builder<String> names = ImmutableList.builder();
    names.add(PyProtocols.__CLASS__, PyProtocols.__NAME__, PyProtocols.__DICT__);
    names.addAll(declaredNames());
    if (attributes != null) {
      names.addAll(attributes.keySet());
    }
    for (String method : methodNames()) {
      if (instanceAttribute(method) == null) {
        names.add(method);
      }
    }
    return names.build();
  }

  /**
   * The instance's state: its declared plain attributes, then those a script added. A copy, in the
   * instance's mutability (as a struct's {@code __dict__} was), so a script may change it.
   */
  public final Dict<String, Object> dunderDict() {
    Map<String, Object> state = new LinkedHashMap<>();
    addState(state);
    if (attributes != null) {
      state.putAll(attributes);
    }
    return Dict.copyOf(mutability, state);
  }

  /** The attribute {@code name} as a script reads it, including a value set in the instance dict. */
  protected final Object attr(StarlarkThread thread, String name) throws EvalException, InterruptedException {
    return Starlark.getattr(thread.mutability(), thread.getSemantics(), this, name, null);
  }

  /** Calls the attribute {@code name}, so an override in the instance dict is called instead of the method. */
  protected final Object callAttr(StarlarkThread thread, String name, Object... args)
      throws EvalException, InterruptedException {
    return call(thread, attr(thread, name), args);
  }

  /** Calls {@code callable} with positional arguments. */
  public static Object call(StarlarkThread thread, Object callable, Object... args)
      throws EvalException, InterruptedException {
    return callable instanceof StarlarkCallable function
        ? Starlark.positionalOnlyCall(thread, function, args)
        : Starlark.call(thread, callable, Tuple.of(args), Dict.empty());
  }

  /** Calls the attribute {@code name} of {@code owner} with positional arguments. */
  public static Object callField(StarlarkThread thread, Object owner, String name, Object... args)
      throws EvalException, InterruptedException {
    return call(thread, Starlark.getattr(thread.mutability(), thread.getSemantics(), owner, name, null), args);
  }

  /**
   * Charges {@code steps} of native work to the thread's step budget, after checking for an
   * interrupt and the deadline. When the budget cannot cover them, it spends the rest of it before
   * failing, as the evaluator's {@code ++steps >= limit} does, so the failure is reported as the
   * step limit.
   */
  public static void chargeSteps(StarlarkThread thread, long steps) throws EvalException, InterruptedException {
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedException();
    }
    if (thread.getExpirationMs() != Long.MAX_VALUE && thread.isExpired()) {
      throw Starlark.errorf("Starlark computation cancelled: past expiration date");
    }
    long remaining = thread.getMaxExecutionSteps() - thread.getExecutedSteps();
    if (steps >= remaining) {
      thread.incrementExecutedSteps(Math.max(remaining, 0));
      throw Starlark.errorf("Starlark computation cancelled: too many steps");
    }
    thread.incrementExecutedSteps(steps);
  }
}
