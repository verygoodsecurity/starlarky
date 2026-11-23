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

import com.verygood.security.larky.objects.descriptor.LarkyNativeMethod;
import com.verygood.security.larky.objects.descriptor.LarkyNativeProperty;
import com.verygood.security.larky.objects.type.LarkyType;
import com.verygood.security.larky.objects.type.LarkyTypeObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;

/**
 * The definition of a Larky class that is written in Java, as a type written in C is defined for
 * CPython: its name, its base, its properties and its methods. Instances are {@link
 * LarkyNativeObject}s of the Java class {@code T}.
 *
 * <p>The Java code states what the class has; the Python protocols follow from it. Attribute
 * lookup, the descriptors, the MRO, {@code __dict__}, {@code dir()} and freezing are those of the
 * object model in this package, so scripts use an instance like any Python object.
 *
 * <pre>{@code
 * public final class Point extends LarkyNativeObject {
 *   public static final LarkyNativeClass<Point> CLASS = LarkyNativeClass.named("Point", Point.class)
 *       .property("norm", (self, thread) -> self.norm())        // read-only
 *       .property("x", (self, thread) -> self.state("_x"), Point::setX)
 *       .build();
 *
 *   @StarlarkMethod(name = "scale", parameters = {@Param(name = "by")})
 *   public void scale(StarlarkInt by) throws EvalException { ... }  // a method
 * }
 * }</pre>
 *
 * <p>A class has:
 *
 * <ul>
 *   <li>properties, given here with a getter and an optional setter;
 *   <li>methods, which are the {@code @StarlarkMethod}s of {@code T} (or, for a class that
 *       dispatches its methods itself, given here with {@link Builder#method});
 *   <li>a base class, given with {@link Builder#extending}, whose properties and methods it
 *       inherits;
 *   <li>plain attributes, which need no definition: they are the entries of an instance's {@code
 *       __dict__}, read and written from Java with {@link LarkyNativeObject#state} and {@link
 *       LarkyNativeObject#setState}.
 * </ul>
 *
 * @param <T> the Java class of the instances
 */
public final class LarkyNativeClass<T extends LarkyNativeObject> {

  /** Reads a property of {@code self}. */
  @FunctionalInterface
  public interface Getter<T> {
    Object get(T self, StarlarkThread thread) throws EvalException, InterruptedException;
  }

  /** Sets a property of {@code self}. */
  @FunctionalInterface
  public interface Setter<T> {
    void set(T self, Object value, StarlarkThread thread) throws EvalException, InterruptedException;
  }

  private final LarkyType type;

  private LarkyNativeClass(LarkyType type) {
    this.type = type;
  }

  /** Starts the definition of the class {@code name}, whose instances are of the Java class {@code javaClass}. */
  public static <T extends LarkyNativeObject> Builder<T> named(String name, Class<T> javaClass) {
    return new Builder<>(name, javaClass);
  }

  /** The class as the object model sees it. */
  public LarkyType type() {
    return type;
  }

  /** The class's name. */
  public String name() {
    return type.__name__();
  }

  /** Collects the definition of a class. */
  public static final class Builder<T extends LarkyNativeObject> {
    private final String name;
    private final Class<T> javaClass;
    private final Map<String, Object> dict = new LinkedHashMap<>();
    @Nullable private LarkyNativeClass<? super T> base;

    private Builder(String name, Class<T> javaClass) {
      this.name = name;
      this.javaClass = javaClass;
    }

    /** The base class: this class inherits its properties and methods. Without one, the base is {@code object}. */
    public Builder<T> extending(LarkyNativeClass<? super T> base) {
      this.base = base;
      return this;
    }

    /** A property that scripts can read and set. */
    public Builder<T> property(String name, Getter<T> getter, Setter<T> setter) {
      dict.put(name, LarkyNativeProperty.of(name, javaClass, getter, setter));
      return this;
    }

    /** A property that scripts can read; setting it fails. */
    public Builder<T> property(String name, Getter<T> getter) {
      dict.put(name, LarkyNativeProperty.of(name, javaClass, getter, null));
      return this;
    }

    /**
     * A method that is not a {@code @StarlarkMethod} of the Java class: {@code bind} gives the
     * callable for an instance.
     */
    public Builder<T> method(String name, Function<T, StarlarkCallable> bind) {
      dict.put(name, LarkyNativeMethod.of(name, javaClass, bind));
      return this;
    }

    public LarkyNativeClass<T> build() {
      return new LarkyNativeClass<>(
          LarkyTypeObject.createBuiltinType(name, base == null ? null : base.type, dict));
    }
  }
}
