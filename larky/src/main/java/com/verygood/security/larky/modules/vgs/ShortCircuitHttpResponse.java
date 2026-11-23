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

package com.verygood.security.larky.modules.vgs;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Structure;

/**
 * {@code proxy.ShortCircuitResponse(response)} for an HTTP response. It holds the response and
 * nothing else, and cannot be changed; the response it holds can.
 */
@StarlarkBuiltin(name = "ShortCircuitResponse", documented = false)
public final class ShortCircuitHttpResponse implements ShortCircuit, Structure {

  private static final ImmutableList<String> FIELDS = ImmutableList.of("__name__", "response");

  private final VgsHttpResponse response;

  ShortCircuitHttpResponse(VgsHttpResponse response) {
    this.response = response;
  }

  @Override
  public VgsHttpResponse response() {
    return response;
  }

  @Override
  public ShortCircuit shortCircuit(StarlarkThread thread) {
    return this;
  }

  @Nullable
  @Override
  public Object getValue(String name) {
    switch (name) {
      case "__name__":
        return "ShortCircuitResponse";
      case "response":
        return response;
      default:
        return null;
    }
  }

  @Override
  public ImmutableList<String> getFieldNames() {
    return FIELDS;
  }

  @Nullable
  @Override
  public String getErrorMessageForUnknownField(String field) {
    return null;
  }

  @Override
  public boolean isImmutable() {
    return response.isImmutable();
  }

  @Override
  public void repr(Printer printer, StarlarkSemantics semantics) {
    printer.append("ShortCircuitResponse(");
    response.repr(printer, semantics);
    printer.append(")");
  }
}
