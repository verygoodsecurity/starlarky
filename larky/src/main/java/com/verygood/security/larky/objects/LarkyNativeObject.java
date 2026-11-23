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

package com.verygood.security.larky.objects;

import com.google.common.collect.ImmutableList;
import com.verygood.security.larky.modules.types.PyProtocols;
import com.verygood.security.larky.objects.descriptor.LarkyDataDescriptor;
import com.verygood.security.larky.objects.descriptor.LarkyNonDataDescriptor;
import com.verygood.security.larky.objects.type.LarkyType;
import com.verygood.security.larky.parser.CurrentEvaluation;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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
 * An instance of a class that is written in Java: a {@link PyObject} whose {@link LarkyType} is
 * built in Java rather than by {@code type(name, bases, dict)}.
 *
 * <p>It follows the object model of this package. Its state is its {@code __dict__}. Its class's
 * dictionary holds the properties (data descriptors, {@link
 * com.verygood.security.larky.objects.descriptor.LarkyNativeProperty}) and is searched along the
 * MRO, so a class written in Java can extend another. Its methods are {@code @StarlarkMethod}s, or
 * non-data descriptors in the class's dictionary. Lookup is Python's: a data descriptor of the
 * class, then the instance's dictionary, then the methods and the rest of the class.
 *
 * <p>Larky's classes are functions: scripts see {@code __class__} as the function that built the
 * instance and {@code __name__} as its name, as on the structs these classes replace. The {@link
 * LarkyType} is what the instance's attributes are looked up on.
 *
 * <p>An instance belongs to the {@link Mutability} of the evaluation that creates it and freezes
 * with it.
 */
public abstract class LarkyNativeObject implements PyObject, Mutability.Freezable {

  private final LarkyType type;
  private final Object constructor;
  private final Mutability mutability;
  private final Dict<String, Object> dict;

  /** An instance of {@code type} with an empty dictionary, built by the function {@code constructor}. */
  protected LarkyNativeObject(LarkyType type, Object constructor, Mutability mutability) {
    this.type = type;
    this.constructor = constructor;
    this.mutability = mutability;
    this.dict = Dict.of(mutability);
  }

  /**
   * An instance of {@code type} that takes over the dictionary of {@code base}, an instance of a
   * base class of {@code type} that nothing else refers to.
   */
  protected LarkyNativeObject(LarkyType type, Object constructor, LarkyNativeObject base) {
    this.type = type;
    this.constructor = constructor;
    this.mutability = base.mutability;
    this.dict = base.dict;
  }

  // The class

  @Override
  public final LarkyType typeClass() {
    return type;
  }

  @Override
  public final LarkyType __class__() {
    return type;
  }

  /** The class name scripts see, as {@code __name__} and {@code type(x)}. */
  @Override
  public String typeName() {
    return type.__name__();
  }

  /** The function that built this instance: its class, to a script. */
  public final Object constructor() {
    return constructor;
  }

  @Override
  public final LarkyType.Origin getOrigin() {
    return LarkyType.Origin.BUILTIN;
  }

  @Override
  public final PyObject __new__(Tuple args, Dict<String, Object> kwargs, StarlarkThread thread) {
    throw new UnsupportedOperationException(typeName() + " is built by its constructor function");
  }

  @Override
  public final void __init__(Tuple args, Dict<String, ?> keywords) {}

  @Override
  public String __repr__() {
    return Starlark.repr(this, net.starlark.java.eval.StarlarkSemantics.DEFAULT);
  }

  @Override
  public String __str__() {
    return __repr__();
  }

  // Freezing

  @Override
  public final Mutability mutability() {
    return mutability;
  }

  @Override
  public final boolean isImmutable() {
    return mutability.isFrozen();
  }

  /** Fails if the instance is frozen. Every change to its state goes through this. */
  protected final void checkMutable() throws EvalException {
    Starlark.checkMutable(this);
  }

  @Override
  public final StarlarkThread getCurrentThread() {
    return CurrentEvaluation.currentOrNull();
  }

  // The instance's dictionary

  @Override
  public final Dict<String, Object> __dict__() {
    return dict;
  }

  @Override
  public final Map<String, Object> getInternalDictUnsafe() {
    return dict;
  }

  @Override
  public final <K, V> void setItemUnsafe(K key, V value) throws EvalException {
    setState((String) key, value);
  }

  /** The entry {@code name} of the instance's dictionary, or null. No descriptor is consulted. */
  @Nullable
  protected final Object state(String name) {
    return dict.get(name);
  }

  /** Sets the entry {@code name} of the instance's dictionary. No descriptor is consulted. */
  protected final void setState(String name, Object value) throws EvalException {
    checkMutable();
    dict.putEntry(name, value);
  }

  // Attribute access

  /** The class's attribute {@code name}, found along the MRO, or null. */
  @Nullable
  private Object classAttribute(String name) {
    switch (name) {
      // LarkyType.lookup answers these for the class itself
      case PyProtocols.__CLASS__:
      case PyProtocols.__DICT__:
      case "__bases__":
      case "__base__":
      case "__mro__":
        return null;
      default:
        return type.lookup(name);
    }
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
        return dict;
      default:
        break;
    }
    StarlarkThread active = thread != null ? thread : CurrentEvaluation.currentOrNull();
    Object classAttribute = classAttribute(name);
    try {
      if (classAttribute instanceof LarkyDataDescriptor descriptor) {
        return descriptor.__get__(this, type, active);
      }
      Object value = dict.get(name);
      if (value != null) {
        return value;
      }
      // null: the interpreter looks for a @StarlarkMethod of this name next
      return classAttribute instanceof LarkyNonDataDescriptor descriptor
          ? descriptor.__get__(this, type, active)
          : classAttribute;
    } catch (EvalException | InterruptedException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new StarlarkEvalWrapper.Exc.RuntimeEvalException(e, active);
    }
  }

  @Override
  public final Object __getattribute__(String name, StarlarkThread thread) throws EvalException {
    try {
      Object value = Starlark.getattr(thread.mutability(), thread.getSemantics(), this, name, null);
      if (value == null) {
        throw Starlark.errorf("AttributeError: '%s' object has no attribute '%s'", typeName(), name);
      }
      return value;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new EvalException(e);
    }
  }

  @Override
  public final void setField(String name, Object value) throws EvalException {
    // a frozen instance fails as one, with or without an evaluation
    checkMutable();
    __setattr__(name, value, CurrentEvaluation.requireThread());
  }

  @Override
  public final void setField(String name, Object value, StarlarkThread thread) throws EvalException {
    __setattr__(name, value, thread);
  }

  @Override
  public final void __setattr__(String name, Object value, StarlarkThread thread) throws EvalException {
    checkMutable();
    checkNotReserved(name);
    if (classAttribute(name) instanceof LarkyDataDescriptor descriptor) {
      try {
        descriptor.__set__(this, value, thread);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new EvalException(e);
      }
      return;
    }
    dict.putEntry(name, value);
  }

  @Override
  public final void __delattr__(String name, StarlarkThread thread) throws EvalException {
    checkMutable();
    checkNotReserved(name);
    if (classAttribute(name) instanceof LarkyDataDescriptor descriptor) {
      try {
        descriptor.__delete__(this, thread);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new EvalException(e);
      }
      return;
    }
    if (!dict.containsKey(name)) {
      throw Starlark.errorf("AttributeError: '%s' object has no attribute '%s'", typeName(), name);
    }
    dict.pop(name, Starlark.NONE, thread);
  }

  private void checkNotReserved(String name) throws EvalException {
    if (PyProtocols.__CLASS__.equals(name) || PyProtocols.__NAME__.equals(name)
        || PyProtocols.__DICT__.equals(name)) {
      throw Starlark.errorf("%s attribute '%s' is read-only", typeName(), name);
    }
  }

  @Override
  public final ImmutableList<String> getFieldNames() {
    Set<String> names = new LinkedHashSet<>();
    names.add(PyProtocols.__CLASS__);
    names.add(PyProtocols.__NAME__);
    names.add(PyProtocols.__DICT__);
    for (Object base : type.getMRO()) {
      names.addAll(((LarkyType) base).getInternalDictUnsafe().keySet());
    }
    names.addAll(dict.keySet());
    return ImmutableList.copyOf(names);
  }

  // For the Java code of a class

  /** The attribute {@code name} as a script reads it. */
  protected final Object attr(StarlarkThread thread, String name) throws EvalException, InterruptedException {
    return Starlark.getattr(thread.mutability(), thread.getSemantics(), this, name, null);
  }

  /** Calls the attribute {@code name}, so an override in the instance's dictionary is called instead of the method. */
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
