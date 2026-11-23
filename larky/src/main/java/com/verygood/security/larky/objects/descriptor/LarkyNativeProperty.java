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

import com.verygood.security.larky.objects.LarkyNativeClass.Getter;
import com.verygood.security.larky.objects.LarkyNativeClass.Setter;
import com.verygood.security.larky.objects.type.LarkyType;
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

  private LarkyNativeProperty(String name, Class<T> owner, Getter<T> getter, @Nullable Setter<T> setter) {
    this.name = name;
    this.owner = owner;
    this.getter = getter;
    this.setter = setter;
  }

  /** The property {@code name} of the instances of {@code owner}; read-only if {@code setter} is null. */
  public static <T> LarkyNativeProperty<T> of(
      String name, Class<T> owner, Getter<T> getter, @Nullable Setter<T> setter) {
    return new LarkyNativeProperty<>(name, owner, getter, setter);
  }

  @Override
  public Object __get__(Object obj, LarkyType type, @Nullable StarlarkThread thread)
      throws EvalException, InterruptedException {
    // read from the class, not from an instance: the property itself, as in Python
    return obj == null ? this : getter.get(owner.cast(obj), thread);
  }

  @Override
  public void __set__(Object obj, Object value, @Nullable StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (setter == null) {
      throw Starlark.errorf("AttributeError: can't set attribute '%s'", name);
    }
    setter.set(owner.cast(obj), value, thread);
  }

  @Override
  public void __delete__(Object obj, @Nullable StarlarkThread thread) throws EvalException {
    throw Starlark.errorf("AttributeError: can't delete attribute '%s'", name);
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
