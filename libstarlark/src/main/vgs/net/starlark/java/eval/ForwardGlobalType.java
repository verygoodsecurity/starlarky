// Copyright 2026 Very Good Security Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package net.starlark.java.eval;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.starlark.java.syntax.StarlarkType;
import net.starlark.java.syntax.TypeConstructor;
import net.starlark.java.syntax.TypeContext;
import net.starlark.java.syntax.Types;

/**
 * A type named by a global of the file being type-tagged that has no value yet, such as a class
 * the file defines ({@code A = type('A', (), {})}; {@code def f(a: A)}). Type tagging precedes
 * execution, so the global is looked up whenever the type is compared, once it has a value that is
 * a type constructor. Until then (in particular during static checking) nothing is known about it:
 * it accepts only itself and {@code Any}, and is accepted wherever {@code Any} is.
 *
 * <p>Enabled per module by {@link Module#allowForwardTypeReferences}.
 */
final class ForwardGlobalType extends StarlarkType {

  private final Module module;
  private final String name;

  private ForwardGlobalType(Module module, String name) {
    this.module = module;
    this.name = name;
  }

  static TypeConstructor constructor(Module module, String name) {
    return Types.wrapType(name, new ForwardGlobalType(module, name));
  }

  /** The type the global denotes, or null if it has no value or its value is not a type. */
  @Nullable
  private StarlarkType resolve() {
    if (module.getGlobal(name) instanceof TypeConstructor constructor) {
      try {
        return constructor.createStarlarkType(ImmutableList.of());
      } catch (TypeConstructor.Failure e) {
        return null;
      }
    }
    return null;
  }

  @Override
  public String typeRepr() {
    return name;
  }

  @Override
  public boolean assignableFromHook(StarlarkType t, TypeContext context) {
    if (equals(t)) {
      return true;
    }
    StarlarkType resolved = resolve();
    return resolved != null && StarlarkType.assignableFrom(resolved, t, context);
  }

  @Override
  public List<StarlarkType> getSupertypes(TypeContext context) {
    StarlarkType resolved = resolve();
    return ImmutableList.of(resolved != null ? resolved : Types.ANY);
  }

  @Override
  public StarlarkType getField(String field, TypeContext context) {
    StarlarkType resolved = resolve();
    StarlarkType type = resolved != null ? resolved.getField(field, context) : null;
    return type != null ? type : Types.ANY;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ForwardGlobalType that
        && that.module == module
        && that.name.equals(name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(System.identityHashCode(module), name);
  }
}
