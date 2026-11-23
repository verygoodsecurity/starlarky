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

package com.verygood.security.larky.modules.vgs;

import com.verygood.security.larky.modules.multidict.LarkyMultiDict;
import com.verygood.security.larky.modules.types.Property;
import com.verygood.security.larky.modules.types.structs.MutableStruct;
import com.verygood.security.larky.modules.types.structs.SimpleStruct;
import com.verygood.security.larky.parser.CurrentEvaluation;
import java.util.Arrays;
import java.util.function.BiConsumer;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;
import org.jetbrains.annotations.Nullable;

/** Java backing for the response constructed by {@code @vgs//http/response}. */
public sealed class VgsHttpResponse extends MutableStruct implements ProtocolResponse
    permits ShortCircuitHttpResponse {

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
    if (body instanceof ProtocolResponse response) {
      // a response is short-circuited by proxy.ShortCircuitResponse(response); as a body it would be sent as text
      throw Starlark.errorf(
          "%s body must not be a %s; to stop with that response, return proxy.ShortCircuitResponse(response)",
          shortCircuit ? "ShortCircuitResponse" : "VGSHttpResponse", scriptName(response, thread));
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

  // the name a script knows a response by (VGSHttpResponse, ShortCircuitResponse); Starlark.type gives the Java one
  private static Object scriptName(ProtocolResponse response, StarlarkThread thread) {
    return response instanceof VgsHttpResponse http ? http.getField("__name__", thread) : Starlark.type(response);
  }

  /** This response's fields, which a {@link ShortCircuitHttpResponse} decorating it shares. */
  @SuppressWarnings("unchecked")
  final Dict<String, Object> sharedFields() {
    return (Dict<String, Object>) fields;
  }

  @Override
  public ShortCircuit shortCircuit(StarlarkThread thread) {
    return ShortCircuitHttpResponse.decorating(this, thread);
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
