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

import com.verygood.security.larky.objects.type.LarkyType;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

/**
 * A method of a class written in Java whose methods are not {@code @StarlarkMethod}s: a non-data
 * descriptor in the class's dictionary, as a function is in a Python class. Reading it from an
 * instance binds it; an attribute of the same name in the instance's dictionary comes first.
 *
 * @param <T> the Java class of the instances
 */
public final class LarkyNativeMethod<T> implements LarkyNonDataDescriptor, StarlarkValue {

  private final String name;
  private final Class<T> owner;
  private final Function<T, StarlarkCallable> bind;

  private LarkyNativeMethod(String name, Class<T> owner, Function<T, StarlarkCallable> bind) {
    this.name = name;
    this.owner = owner;
    this.bind = bind;
  }

  /** The method {@code name} of the instances of {@code owner}; {@code bind} binds it to one. */
  public static <T> LarkyNativeMethod<T> of(String name, Class<T> owner, Function<T, StarlarkCallable> bind) {
    return new LarkyNativeMethod<>(name, owner, bind);
  }

  @Override
  public Object __get__(Object obj, LarkyType type, @Nullable StarlarkThread thread) {
    return obj == null ? this : bind.apply(owner.cast(obj));
  }

  @Override
  public boolean readonly() {
    return true;
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
    printer.append("<method '").append(name).append("'>");
  }
}
