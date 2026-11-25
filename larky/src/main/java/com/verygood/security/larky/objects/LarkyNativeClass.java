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
 *       .attribute("x", self -> self.x, (self, value) -> self.x = self.checkMutable(value))
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
 *   <li>plain attributes, given here with {@link Builder#attribute}: each is a field of the Java
 *       class, as a member of a C struct is, so an instance allocates no dictionary for them;
 *   <li>any other attribute a script sets on an instance, which goes into a dictionary that the
 *       instance creates when the first one is set.
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

  /** Writes the field of a plain attribute of {@code self}; {@code value} is null to delete it. */
  @FunctionalInterface
  public interface FieldSetter<T> {
    void set(T self, @Nullable Object value) throws EvalException;
  }

  private final LarkyType type;
  // the plain attributes of the class and its bases, in order: an instance's state
  private final ImmutableList<LarkyNativeProperty<?>> fields;

  private LarkyNativeClass(LarkyType type, ImmutableList<LarkyNativeProperty<?>> fields) {
    this.type = type;
    this.fields = fields;
  }

  /** The plain attributes of the class and its bases, in the order {@code __dict__} lists them. */
  ImmutableList<LarkyNativeProperty<?>> fields() {
    return fields;
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

    /**
     * A plain attribute, held in a field of the Java class: {@code get} reads the field and {@code
     * set} writes it, after {@link LarkyNativeObject#checkMutable(Object)}. A null field means the
     * attribute is not set, and {@code set} is given null when a script deletes it. Plain
     * attributes are the instance's state: {@code __dict__} lists them, in the order they are
     * given here.
     */
    public Builder<T> attribute(String name, Function<T, Object> get, FieldSetter<T> set) {
      dict.put(name, LarkyNativeProperty.ofField(name, javaClass, get, set));
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
      ImmutableList.Builder<LarkyNativeProperty<?>> fields = ImmutableList.builder();
      if (base != null) {
        for (LarkyNativeProperty<?> inherited : base.fields) {
          // a name this class defines again is this class's
          if (!dict.containsKey(inherited.name())) {
            fields.add(inherited);
          }
        }
      }
      for (Object attribute : dict.values()) {
        if (attribute instanceof LarkyNativeProperty<?> property && property.isField()) {
          fields.add(property);
        }
      }
      return new LarkyNativeClass<>(
          LarkyTypeObject.createBuiltinType(name, base == null ? null : base.type, dict), fields.build());
    }
  }
}
