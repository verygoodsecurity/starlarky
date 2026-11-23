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
import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.modules.types.PyProtocols;
import com.verygood.security.larky.objects.descriptor.LarkyDataDescriptor;
import com.verygood.security.larky.objects.descriptor.LarkyNativeProperty;
import com.verygood.security.larky.objects.descriptor.LarkyNonDataDescriptor;
import com.verygood.security.larky.objects.type.LarkyType;
import com.verygood.security.larky.parser.CurrentEvaluation;
import java.util.LinkedHashMap;
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
import net.starlark.java.eval.StarlarkOperators;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

/**
 * An instance of a class that is written in Java. The class is defined with {@link
 * LarkyNativeClass}; a subclass of this one holds the Java code of its properties and methods and
 * needs none of the Python protocols, which this class implements for it.
 *
 * <p>It follows the object model of this package: it is a {@link PyObject} whose {@link LarkyType}
 * is built in Java rather than by {@code type(name, bases, dict)}. Its plain attributes are fields
 * of the Java class, as the members of a type written in C are fields of its struct, so an
 * instance has no dictionary until a script sets an attribute that the class does not define.
 * The class's dictionary holds the plain attributes and the properties (data descriptors) and is
 * searched along the MRO, so a class written in Java can extend another. Its methods are {@code
 * @StarlarkMethod}s, or non-data descriptors in the class's dictionary. Lookup is Python's: a data
 * descriptor of the class, then the attributes set on the instance, then the methods and the rest
 * of the class.
 *
 * <p>Larky's classes are functions: scripts see {@code __class__} as the function that built the
 * instance and {@code __name__} as its name, as on the structs these classes replace. The {@link
 * LarkyType} is what the instance's attributes are looked up on.
 *
 * <p>An instance belongs to the {@link Mutability} of the evaluation that creates it and freezes
 * with it.
 */
public abstract class LarkyNativeObject implements PyObject, Mutability.Freezable {

  private final LarkyNativeClass<?> cls;
  private final LarkyType type;
  private final Object constructor;
  private final Mutability mutability;
  // The attributes a script set that the class does not define, created with the first one.
  @Nullable private Dict<String, Object> attributes;

  /**
   * An instance of {@code cls}. {@code constructor} is the function that builds it, which scripts
   * see as its {@code __class__}.
   */
  protected LarkyNativeObject(LarkyNativeClass<?> cls, Object constructor, Mutability mutability) {
    this.cls = cls;
    this.type = cls.type();
    this.constructor = constructor;
    this.mutability = mutability;
  }

  /**
   * An instance of {@code cls} in place of {@code base}, an instance of a base class of {@code
   * cls}: it starts with a copy of the attributes a script set on it. The subclass's constructor
   * copies its fields.
   */
  protected LarkyNativeObject(LarkyNativeClass<?> cls, Object constructor, LarkyNativeObject base) {
    this.cls = cls;
    this.type = cls.type();
    this.constructor = constructor;
    this.mutability = base.mutability;
    this.attributes = base.attributes == null ? null : Dict.copyOf(base.mutability, base.attributes);
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

  /**
   * {@code value}, after failing if the instance is frozen: for the setter of a plain attribute,
   * {@code self.field = self.checkMutable(value)}.
   */
  protected final <V> V checkMutable(V value) throws EvalException {
    Starlark.checkMutable(this);
    return value;
  }

  @Override
  public final StarlarkThread getCurrentThread() {
    return CurrentEvaluation.currentOrNull();
  }

  // The instance's state

  /**
   * {@code __dict__}: the plain attributes that are set, in the class's order, then the attributes
   * a script added. It is a copy, in the instance's mutability, so a script may change it without
   * changing the instance.
   */
  @Override
  public final Dict<String, Object> __dict__() {
    Map<String, Object> state = new LinkedHashMap<>();
    for (LarkyNativeProperty<?> field : cls.fields()) {
      Object value = field.fieldValue(this);
      if (value != null) {
        state.put(field.name(), value);
      }
    }
    if (attributes != null) {
      state.putAll(attributes);
    }
    return Dict.copyOf(mutability, state);
  }

  @Override
  public final Map<String, Object> getInternalDictUnsafe() {
    return attributes == null ? ImmutableMap.of() : attributes;
  }

  @Override
  public final <K, V> void setItemUnsafe(K key, V value) throws EvalException {
    String name = (String) key;
    if (classAttribute(name) instanceof LarkyDataDescriptor) {
      // its value is a field or computed: an entry here would only shadow it in __dict__
      throw Starlark.errorf("%s attribute '%s' is set through the attribute, not the instance's dict", typeName(), name);
    }
    setState(name, value);
  }

  /** The attribute {@code name} that a script set on the instance and the class does not define, or null. */
  @Nullable
  protected final Object state(String name) {
    return attributes == null ? null : attributes.get(name);
  }

  /** Sets the attribute {@code name} on the instance. No descriptor is consulted. */
  protected final void setState(String name, Object value) throws EvalException {
    checkMutable();
    if (attributes == null) {
      attributes = Dict.of(mutability);
    }
    attributes.putEntry(name, value);
  }

  /**
   * {@code value}, the field behind the plain attribute {@code name}; fails as a missing attribute
   * if it is null, which is how a field is when a script deleted the attribute.
   */
  protected final Object required(String name, @Nullable Object value) throws EvalException {
    if (value == null) {
      throw Starlark.errorf("AttributeError: '%s' object has no attribute '%s'", typeName(), name);
    }
    return value;
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
        return __dict__();
      default:
        break;
    }
    StarlarkThread active = thread != null ? thread : CurrentEvaluation.currentOrNull();
    Object classAttribute = classAttribute(name);
    try {
      if (classAttribute instanceof LarkyDataDescriptor descriptor) {
        // null for a plain attribute that is not set: the instance has no attribute of that name
        return descriptor.__get__(this, type, active);
      }
      Object value = state(name);
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
      // fails, as reading the attribute does, if there is none
      return StarlarkOperators.getattr(thread, this, name, null);
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
    setState(name, value);
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
    if (attributes == null || !attributes.containsKey(name)) {
      throw Starlark.errorf("AttributeError: '%s' object has no attribute '%s'", typeName(), name);
    }
    attributes.pop(name, Starlark.NONE, thread);
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
      for (Map.Entry<String, Object> attribute : ((LarkyType) base).getInternalDictUnsafe().entrySet()) {
        // a plain attribute that is not set is not an attribute of this instance
        // the attribute as this class resolves the name, which a subclass may have redefined
        if (!(type.lookup(attribute.getKey()) instanceof LarkyNativeProperty<?> field)
            || !field.isField()
            || field.fieldValue(this) != null) {
          names.add(attribute.getKey());
        }
      }
    }
    if (attributes != null) {
      names.addAll(attributes.keySet());
    }
    return ImmutableList.copyOf(names);
  }

  // For the Java code of a class

  /** Sets the attribute {@code name} as a script does: {@code self.name = value}. */
  protected final void setAttr(StarlarkThread thread, String name, Object value) throws EvalException {
    __setattr__(name, value, thread);
  }

  /** The attribute {@code name} as a script reads it, or null if there is none. */
  protected final Object attr(StarlarkThread thread, String name) throws EvalException, InterruptedException {
    return StarlarkOperators.getattr(thread, this, name, null);
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
    return call(thread, StarlarkOperators.getattr(thread, owner, name, null), args);
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
