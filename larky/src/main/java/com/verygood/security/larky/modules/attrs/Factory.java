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
package com.verygood.security.larky.modules.attrs;

import com.google.common.collect.ImmutableList;

import java.util.Objects;

import javax.annotation.Nullable;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.Structure;

/** attrs' {@code Factory}: a default computed by calling {@code factory} for each instance. */
@StarlarkBuiltin(name = "Factory", documented = false)
public final class Factory implements Structure {
  final Object factory;
  final boolean takesSelf;

  Factory(Object factory, boolean takesSelf) {
    this.factory = factory;
    this.takesSelf = takesSelf;
  }

  @Nullable
  @Override
  public Object getValue(String name) {
    return switch (name) {
      case "factory" -> factory;
      case "takes_self" -> takesSelf;
      default -> null;
    };
  }

  @Override
  public ImmutableList<String> getFieldNames() {
    return ImmutableList.of("factory", "takes_self");
  }

  @Nullable
  @Override
  public String getErrorMessageForUnknownField(String field) {
    return "'Factory' object has no attribute '" + field + "'";
  }

  @Override
  public void repr(Printer printer, StarlarkSemantics semantics) {
    printer.append("Factory(factory=").repr(factory, semantics)
        .append(", takes_self=").repr(takesSelf, semantics).append(")");
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Factory f && f.takesSelf == takesSelf && Objects.equals(f.factory, factory);
  }

  @Override
  public int hashCode() {
    return Objects.hash(factory, takesSelf);
  }

  @Override
  public boolean isImmutable() {
    return Starlark.isImmutable(factory);
  }
}
