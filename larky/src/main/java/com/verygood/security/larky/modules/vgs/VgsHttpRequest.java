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
import com.verygood.security.larky.modules.urllib.UrllibRequest;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkOperators;
import net.starlark.java.eval.StarlarkThread;

/**
 * {@code VGSHttpRequest} from {@code @vgs//http/request}: a {@code urllib.request.Request} with
 * ordered, case-insensitive headers.
 *
 * <p>On top of urllib's attributes it has the properties {@code url} (the full URL; setting it
 * also sets {@code path} and {@code query_string}), {@code body} (the data) and {@code headers}
 * (over the plain attribute {@code _headers}; setting it builds a new header container from the
 * value), and the plain attributes {@code path} and {@code query_string}. Its
 * {@code add_header} and {@code add_unredirected_header} keep the key as given, where urllib's
 * capitalize it.
 */
@StarlarkBuiltin(name = "VGSHttpRequest", documented = false)
public final class VgsHttpRequest extends UrllibRequest {

  private final StarlarkCallable headerFactory;
  private final StarlarkCallable urlSplitter;
  private Object path = Starlark.NONE;
  private Object queryString = Starlark.NONE;

  private VgsHttpRequest(
      Object constructor, UrllibRequest base, StarlarkCallable headerFactory, StarlarkCallable urlSplitter,
      StarlarkThread thread) {
    super(constructor, base, thread);
    this.headerFactory = headerFactory;
    this.urlSplitter = urlSplitter;
  }

  /**
   * {@code VGSHttpRequest(url, data, headers, method)}, whose class is {@code constructor}. It takes
   * over {@code base}, the {@code urllib.request.Request(url, data=data, method=method)} that the
   * facade has just built; {@code headerFactory} builds its headers, and {@code urlSplitter}
   * ({@code urllib.parse.urlsplit}) reads its path and query.
   */
  public static VgsHttpRequest create(
      Object constructor, UrllibRequest base, StarlarkCallable headerFactory, StarlarkCallable urlSplitter,
      Object url, Object headers, StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (base.getClass() != UrllibRequest.class) {
      throw Starlark.errorf(
          "VGSHttpRequest(request=...) expected `Request`; got %s", Starlark.type(base));
    }
    VgsHttpRequest request = new VgsHttpRequest(constructor, base, headerFactory, urlSplitter, thread);
    request.setHeadersValue(call(thread, headerFactory, headers));
    request.updatePath(url, thread);
    return request;
  }

  @Override
  public String typeName() {
    return "VGSHttpRequest";
  }

  @Nullable
  @Override
  protected Object getDeclared(String name, StarlarkThread thread) throws EvalException, InterruptedException {
    switch (name) {
      case "url":
        // full_url is a property: an instance attribute cannot replace it
        return fullUrl(thread);
      case "body":
        return data();
      case "headers":
      case "_headers":
        return headersValue();
      case "path":
        return path;
      case "query_string":
        return queryString;
      default:
        break;
    }
    return super.getDeclared(name, thread);
  }

  @Override
  protected boolean setDeclared(String name, Object value, StarlarkThread thread)
      throws EvalException, InterruptedException {
    checkMutable();
    switch (name) {
      case "url":
        setField("full_url", value);
        updatePath(value, thread);
        return true;
      case "body":
        setField("data", value);
        return true;
      case "headers":
        setHeadersValue(call(thread, headerFactory, value));
        return true;
      case "_headers":
        setHeadersValue(value);
        return true;
      case "path":
        path = value;
        return true;
      case "query_string":
        queryString = value;
        return true;
      default:
        break;
    }
    return super.setDeclared(name, value, thread);
  }

  private void updatePath(Object url, StarlarkThread thread) throws EvalException, InterruptedException {
    checkMutable();
    Object parsed = call(thread, urlSplitter, url);
    path = Starlark.getattr(thread.mutability(), thread.getSemantics(), parsed, "path", null);
    queryString = Starlark.getattr(thread.mutability(), thread.getSemantics(), parsed, "query", null);
  }

  @Override
  protected String headersStateName() {
    return "_headers";
  }

  @Override
  protected void addState(Map<String, Object> state) {
    super.addState(state);
    state.put("path", path);
    state.put("query_string", queryString);
  }

  @Override
  protected ImmutableList<String> declaredNames() {
    return ImmutableList.<String>builder()
        .addAll(super.declaredNames()).add("url", "body", "_headers", "path", "query_string").build();
  }

  // Unlike urllib's, these keep the key as given.

  @Override
  @StarlarkMethod(name = "add_header", documented = false,
      parameters = {@Param(name = "key", named = true), @Param(name = "val", named = true)}, useStarlarkThread = true)
  public void addHeader(Object key, Object value, StarlarkThread thread) throws EvalException, InterruptedException {
    StarlarkOperators.setIndex(thread, attr(thread, "headers"), key, value);
  }

  @Override
  @StarlarkMethod(name = "add_unredirected_header", documented = false,
      parameters = {@Param(name = "key", named = true), @Param(name = "val", named = true)}, useStarlarkThread = true)
  public void addUnredirectedHeader(Object key, Object value, StarlarkThread thread)
      throws EvalException, InterruptedException {
    // will not be added to a redirected request
    StarlarkOperators.setIndex(thread, attr(thread, "unredirected_hdrs"), key, value);
  }

  // For hosts

  public Object body(StarlarkThread thread) {
    return data();
  }

  /**
   * Streams the header lines of a native header container without allocating an items list. The
   * visitor must not modify the request. Returns false without visiting any pairs when the host
   * must call the container's {@code items()} instead: it is not a native container, or a script
   * replaced its {@code items}.
   */
  public boolean visitHeaderLines(BiConsumer<Object, Object> visitor) {
    return headersValue() instanceof LarkyMultiDict multiDict && multiDict.visitItems(visitor);
  }

  /** The header container's {@code items()}, or None when the headers are None. */
  public Object headerItems(StarlarkThread thread) throws EvalException, InterruptedException {
    Object headers = headersValue();
    return Starlark.isNullOrNone(headers) ? Starlark.NONE : callField(thread, headers, "items");
  }
}
