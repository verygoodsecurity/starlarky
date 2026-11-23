/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.larky.modules.types.structs;

import com.verygood.security.larky.modules.multidict.LarkyMultiDict;
import com.verygood.security.larky.modules.types.Property;
import com.verygood.security.larky.parser.CurrentEvaluation;
import java.util.Arrays;
import java.util.function.BiConsumer;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import org.jetbrains.annotations.Nullable;

/** Java backing for the response constructed by {@code @vgs//http/response}. */
public sealed class VgsHttpResponse extends MutableStruct permits ShortCircuitHttpResponse {

  private static final int HEADER_FACTORY = 0;
  private static final int HEADER_OWNER = 1;

  public static VgsHttpResponse create(
      StarlarkCallable constructor, StarlarkCallable headerFactory, Object body, Object headers,
      Object status, StarlarkThread thread) throws EvalException, InterruptedException {
    return create(constructor, headerFactory, body, headers, status, false, thread);
  }

  public static VgsHttpResponse create(
      StarlarkCallable constructor, StarlarkCallable headerFactory, Object body, Object headers,
      Object status, boolean shortCircuit, StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (shortCircuit && body instanceof VgsHttpResponse response) {
      // Larky 1.3 and earlier: proxy.ShortCircuitResponse(VGSHttpResponse(...)). The response's body, ordered
      // headers and status code make the short-circuit response; given more than the response, it is ambiguous.
      if (!(Starlark.isNullOrNone(headers) || headers instanceof Dict<?, ?> dict && dict.isEmpty())
          || !DEFAULT_STATUS.equals(status)) {
        throw Starlark.errorf(
            "ShortCircuitResponse takes either a VGSHttpResponse or body, headers and status_code, not both");
      }
      body = response.body(thread);
      headers = response.headerItems(thread);
      status = response.statusCode(thread);
    }
    SimpleStruct headerMap = buildHeaders(headerFactory, headers, thread);
    var fields = Dict.<String, Object>builder()
        .put("__name__", shortCircuit ? "ShortCircuitResponse" : "VGSHttpResponse")
        .put("__class__", constructor)
        .put("_body", body)
        .put("body", Starlark.NONE)
        .put("_headers", headerMap)
        .put("headers", headerProperty(headerFactory, headerMap))
        .put("status_code", status)
        .build(thread.mutability());
    return shortCircuit
        ? new ShortCircuitHttpResponse(fields, thread)
        : new VgsHttpResponse(fields, thread);
  }

  // the facade's status_code default (vgs/http/response.star)
  private static final StarlarkInt DEFAULT_STATUS = StarlarkInt.of(200);

  VgsHttpResponse(Dict<String, Object> fields, StarlarkThread thread) {
    super(fields, thread.mutability());
  }

  // The tuple captures the constructor and the original header access path. Scripts read the
  // header object through getField, which resolves this marker.
  private static Tuple headerProperty(StarlarkCallable factory, SimpleStruct headers) {
    return Tuple.of(factory, headers);
  }

  private static SimpleStruct buildHeaders(
      StarlarkCallable factory, Object source, StarlarkThread thread)
      throws EvalException, InterruptedException {
    Object value = Starlark.positionalOnlyCall(thread, factory, source);
    if (!(value instanceof SimpleStruct headers)) {
      throw Starlark.errorf("Expected a response header struct");
    }
    return headers;
  }

  @Override
  public Object getField(String name, @Nullable StarlarkThread thread) {
    if ("body".equals(name) && fields.get("body") == Starlark.NONE) {
      return super.getField("_body", thread);
    }
    if ("headers".equals(name) && fields.get("headers") instanceof Tuple) {
      return super.getField("_headers", thread);
    }
    return super.getField(name, thread);
  }

  @Override
  public void setField(String name, Object value) throws EvalException {
    if (!(value instanceof Property)
        && (("body".equals(name) && fields.get("body") == Starlark.NONE)
            || ("headers".equals(name) && fields.get("headers") instanceof Tuple))) {
      setResponseProperty(name, value);
    } else {
      super.setField(name, value);
    }
  }

  private void setResponseProperty(String name, Object value) throws EvalException {
    if (((Dict<?, ?>) fields).mutability().isFrozen()) {
      throw Starlark.errorf("Attempting to update a frozen structure");
    }
    StarlarkThread thread = CurrentEvaluation.requireThread();
    try {
      if ("headers".equals(name)) {
        var property = (Tuple) fields.get("headers");
        var headerFactory = (StarlarkCallable) property.get(HEADER_FACTORY);
        SimpleStruct headers = buildHeaders(headerFactory, value, thread);
        super.setField("_headers", headers);
        super.setField("headers", headerProperty(headerFactory, headers));
      } else if (!Starlark.checkedEquals(value, body(thread))) {
        super.setField("_body", value);
        // The stored length applies to the previous body (issue 16464).
        callHeaderMethod(thread, getField("headers", thread), "popall", "Content-Length",
            StarlarkList.newList(thread.mutability()));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new EvalException(e);
    }
  }

  public Object body(StarlarkThread thread) {
    return getField("body", thread);
  }

  public Object statusCode(StarlarkThread thread) {
    return getField("status_code", thread);
  }

  /**
   * Streams standard header pairs during the response's evaluation. The visitor must not
   * modify the response. Returns false without visiting any pairs when the caller needs to
   * use the script's {@code items()} path instead.
   */
  public boolean visitHeaderLines(BiConsumer<Object, Object> visitor) {
    if (!(fields.get("headers") instanceof Tuple property)
        || !(fields.get("_headers") instanceof LarkyMultiDict headers)
        || headers != property.get(HEADER_OWNER)) {
      return false;
    }
    return headers.visitItems(visitor);
  }

  /** Calls the current header container's script-visible {@code items()} method. */
  public Object headerItems(StarlarkThread thread) throws EvalException, InterruptedException {
    Object headers = getField("headers", thread);
    return Starlark.isNullOrNone(headers) ? Starlark.NONE : callHeaderMethod(thread, headers, "items");
  }

  private static Object callHeaderMethod(StarlarkThread thread, Object headers, String name, Object... args)
      throws EvalException, InterruptedException {
    Object method = Starlark.getattr(thread.mutability(), thread.getSemantics(), headers, name, null);
    return Starlark.call(thread, method, Arrays.asList(args), Dict.empty());
  }
}
