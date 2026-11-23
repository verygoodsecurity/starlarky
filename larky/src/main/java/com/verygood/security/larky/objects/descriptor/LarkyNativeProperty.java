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

package com.verygood.security.larky.objects.descriptor;

import com.verygood.security.larky.objects.LarkyNativeClass;
import com.verygood.security.larky.objects.LarkyNativeClass.Getter;
import com.verygood.security.larky.objects.LarkyNativeClass.Setter;
import com.verygood.security.larky.objects.type.LarkyType;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

/**
 * A property of a class written in Java ({@link com.verygood.security.larky.objects.LarkyNativeClass}):
 * a data descriptor in the class's dictionary, as {@code property(fget, fset)} is in a Python
 * class, whose getter and setter are Java code.
 *
 * @param <T> the Java class of the instances
 */
public final class LarkyNativeProperty<T> implements LarkyDataDescriptor, StarlarkValue {

  private final String name;
  private final Class<T> owner;
  private final Getter<T> getter;
  @Nullable private final Setter<T> setter;
  // a plain attribute held in a Java field: part of the instance's state, as __dict__ shows it
  @Nullable private final Function<T, Object> field;

  private LarkyNativeProperty(
      String name, Class<T> owner, Getter<T> getter, @Nullable Setter<T> setter,
      @Nullable Function<T, Object> field) {
    this.name = name;
    this.owner = owner;
    this.getter = getter;
    this.setter = setter;
    this.field = field;
  }

  /**
   * The plain attribute {@code name} of the instances of {@code owner}, held in a Java field that
   * {@code get} reads and {@code set} writes. A null value means the attribute is not set; {@code
   * set} is given null to delete it.
   */
  public static <T> LarkyNativeProperty<T> ofField(
      String name, Class<T> owner, Function<T, Object> get, LarkyNativeClass.FieldSetter<T> set) {
    return new LarkyNativeProperty<>(
        name, owner, (self, thread) -> get.apply(self), (self, value, thread) -> set.set(self, value), get);
  }

  /** Whether this is a plain attribute, part of the instance's state. */
  public boolean isField() {
    return field != null;
  }

  /** The value of this plain attribute on {@code obj}, or null if it is not set. */
  @Nullable
  public Object fieldValue(Object obj) {
    return field.apply(owner.cast(obj));
  }

  /** The attribute's name. */
  public String name() {
    return name;
  }

  /** The property {@code name} of the instances of {@code owner}; read-only if {@code setter} is null. */
  public static <T> LarkyNativeProperty<T> of(
      String name, Class<T> owner, Getter<T> getter, @Nullable Setter<T> setter) {
    return new LarkyNativeProperty<>(name, owner, getter, setter, null);
  }

  @Override
  public Object __get__(Object obj, LarkyType type, @Nullable StarlarkThread thread)
      throws EvalException, InterruptedException {
    // read from the class, not from an instance: the property itself, as in Python
    return obj == null ? this : getter.get(self(obj), thread);
  }

  @Override
  public void __set__(Object obj, Object value, @Nullable StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (setter == null) {
      throw Starlark.errorf("AttributeError: can't set attribute '%s'", name);
    }
    setter.set(self(obj), value, thread);
  }

  /** {@code obj} as an instance of the class: the property of a class written in Java works on no other object. */
  private T self(Object obj) throws EvalException {
    if (!owner.isInstance(obj)) {
      throw Starlark.errorf(
          "TypeError: property '%s' needs an instance of its class; got %s", name, Starlark.type(obj));
    }
    return owner.cast(obj);
  }

  @Override
  public void __delete__(Object obj, @Nullable StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (field == null) {
      throw Starlark.errorf("AttributeError: can't delete attribute '%s'", name);
    }
    T self = self(obj);
    if (field.apply(self) == null) {
      throw Starlark.errorf("AttributeError: '%s' object has no attribute '%s'", Starlark.type(obj), name);
    }
    setter.set(self, null, thread);
  }

  @Override
  public boolean readonly() {
    return setter == null;
  }

  @Override
  public boolean optional() {
    return false;
  }

  @Override
  public boolean isImmutable() {
    return true;
  }

  @Override
  public void repr(Printer printer, StarlarkSemantics semantics) {
    printer.append("<property '").append(name).append("'>");
  }
}
