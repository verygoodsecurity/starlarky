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
import com.verygood.security.larky.modules.multidict.LarkyMultiDict;
import com.verygood.security.larky.modules.types.LarkyInstance;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkOperators;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;

/**
 * {@code VGSHttpResponse} from {@code @vgs//http/response}: an HTTP response with ordered,
 * case-insensitive headers.
 *
 * <p>Its attributes are those of the Starlark class it replaces: the properties {@code body} and
 * {@code headers} over the plain attributes {@code _body} and {@code _headers}, and the plain
 * attribute {@code status_code}. Setting {@code body} to a different value drops
 * {@code Content-Length}, which applied to the previous body (issue 16464). Setting
 * {@code headers} builds a new header container from the value.
 */
@StarlarkBuiltin(name = "VGSHttpResponse", documented = false)
public final class VgsHttpResponse extends LarkyInstance implements ProtocolResponse {

  private static final ImmutableList<String> DECLARED =
      ImmutableList.of("body", "_body", "headers", "_headers", "status_code");

  private final StarlarkCallable headerFactory;
  private Object body;
  private Object headers;
  private Object statusCode;

  private VgsHttpResponse(
      StarlarkCallable constructor, StarlarkCallable headerFactory, Object body, Object headers,
      Object statusCode, StarlarkThread thread) {
    super(constructor, thread.mutability());
    this.headerFactory = headerFactory;
    this.body = body;
    this.headers = headers;
    this.statusCode = statusCode;
  }

  /**
   * Builds the response {@code VGSHttpResponse(body, headers, status_code)}, whose class is
   * {@code constructor} and whose headers {@code headerFactory} builds from {@code headers}.
   */
  public static VgsHttpResponse create(
      StarlarkCallable constructor, StarlarkCallable headerFactory, Object body, Object headers,
      Object statusCode, StarlarkThread thread) throws EvalException, InterruptedException {
    Object headerContainer = call(thread, headerFactory, headers);
    return new VgsHttpResponse(constructor, headerFactory, body, headerContainer, statusCode, thread);
  }

  @Override
  public String typeName() {
    return "VGSHttpResponse";
  }

  @Nullable
  @Override
  protected Object getDeclared(String name, StarlarkThread thread) {
    switch (name) {
      case "body":
      case "_body":
        return body;
      case "headers":
      case "_headers":
        return headers;
      case "status_code":
        return statusCode;
      default:
        return null;
    }
  }

  @Override
  protected boolean setDeclared(String name, Object value, StarlarkThread thread)
      throws EvalException, InterruptedException {
    checkMutable();
    switch (name) {
      case "body":
        if (!Starlark.checkedEquals(value, body)) {
          body = value;
          callField(thread, headers, "popall", "Content-Length", StarlarkList.newList(thread.mutability()));
        }
        return true;
      case "_body":
        body = value;
        return true;
      case "headers":
        headers = call(thread, headerFactory, value);
        return true;
      case "_headers":
        headers = value;
        return true;
      case "status_code":
        statusCode = value;
        return true;
      default:
        return false;
    }
  }

  @Override
  protected void addState(Map<String, Object> state) {
    state.put("_body", body);
    state.put("_headers", headers);
    state.put("status_code", statusCode);
  }

  @Override
  protected ImmutableList<String> declaredNames() {
    return DECLARED;
  }

  @StarlarkMethod(name = "add_header", documented = false,
      parameters = {@Param(name = "key", named = true), @Param(name = "val", named = true)}, useStarlarkThread = true)
  public void addHeader(Object key, Object value, StarlarkThread thread)
      throws EvalException, InterruptedException {
    // self.headers[key] = val
    StarlarkOperators.setIndex(thread, headers, key, value);
  }

  @StarlarkMethod(name = "append_header", documented = false,
      parameters = {@Param(name = "key", named = true), @Param(name = "val", named = true)}, useStarlarkThread = true)
  public void appendHeader(Object key, Object value, StarlarkThread thread)
      throws EvalException, InterruptedException {
    callField(thread, headers, "add", key, value);
  }

  @StarlarkMethod(name = "remove_header", documented = false,
      parameters = {@Param(name = "key", named = true)}, useStarlarkThread = true)
  public void removeHeader(Object key, StarlarkThread thread) throws EvalException, InterruptedException {
    callField(thread, headers, "popall", key, StarlarkList.newList(thread.mutability()));
  }

  @StarlarkMethod(name = "get_all_headers", documented = false,
      parameters = {@Param(name = "key", named = true)}, useStarlarkThread = true)
  public Object getAllHeaders(Object key, StarlarkThread thread) throws EvalException, InterruptedException {
    return callField(thread, headers, "getall", key, StarlarkList.newList(thread.mutability()));
  }

  public Object body(StarlarkThread thread) {
    return body;
  }

  public Object statusCode(StarlarkThread thread) {
    return statusCode;
  }

  /**
   * Streams the header lines of a native header container without allocating an items list. The
   * visitor must not modify the response. Returns false without visiting any pairs when the
   * caller must call the container's {@code items()} instead: it is not a native container, or a
   * script replaced its {@code items}.
   */
  public boolean visitHeaderLines(BiConsumer<Object, Object> visitor) {
    return headers instanceof LarkyMultiDict multiDict && multiDict.visitItems(visitor);
  }

  /** The header container's {@code items()}, or None when the headers are None. */
  public Object headerItems(StarlarkThread thread) throws EvalException, InterruptedException {
    return Starlark.isNullOrNone(headers) ? Starlark.NONE : callField(thread, headers, "items");
  }

  @Override
  public void repr(Printer printer, StarlarkSemantics semantics) {
    printer.append("<VGSHttpResponse [");
    printer.repr(statusCode, semantics);
    printer.append("]>");
  }
}
