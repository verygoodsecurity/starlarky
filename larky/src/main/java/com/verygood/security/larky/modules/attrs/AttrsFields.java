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

import java.util.List;

import javax.annotation.Nullable;

import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.NamedTuple;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Structure;

/**
 * The tuple returned by {@code fields(C)}: a tuple of {@link Attribute}s that also allows access
 * by attribute name ({@code fields(C).x}), like attrs' generated {@code CAttributes} class.
 */
@StarlarkBuiltin(name = "tuple", documented = false)
public final class AttrsFields extends NamedTuple implements Structure {
  private final ImmutableList<String> names;

  AttrsFields(List<Attribute> attrs) {
    super(attrs);
    ImmutableList.Builder<String> b = ImmutableList.builder();
    for (Attribute a : attrs) {
      b.add(a.name());
    }
    this.names = b.build();
  }

  @SuppressWarnings("unchecked")
  List<Attribute> attributes() {
    return (List<Attribute>) (List<?>) elements();
  }

  @Nullable
  @Override
  public Object getValue(String name) {
    int i = names.indexOf(name);
    return i < 0 ? null : get(i);
  }

  @Override
  public ImmutableList<String> getFieldNames() {
    return names;
  }

  /** Prints as a plain tuple, like attrs' tuple subclass. */
  @Override
  public void repr(Printer printer, StarlarkSemantics semantics) {
    printer.append("(");
    for (int i = 0; i < size(); i++) {
      if (i > 0) {
        printer.append(", ");
      }
      printer.repr(get(i), semantics);
    }
    printer.append(size() == 1 ? ",)" : ")");
  }

  @Override
  public void str(Printer printer, StarlarkSemantics semantics) {
    repr(printer, semantics);
  }

  @Override
  public void debugPrint(Printer printer, StarlarkThread thread) {
    repr(printer, thread.getSemantics());
  }

  @Nullable
  @Override
  public String getErrorMessageForUnknownField(String field) {
    return "AttributeError: 'tuple' object has no attribute '" + field + "'";
  }
}
