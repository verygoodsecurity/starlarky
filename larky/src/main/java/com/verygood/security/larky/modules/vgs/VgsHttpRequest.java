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

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.modules.multidict.LarkyMultiDict;
import com.verygood.security.larky.modules.urllib.UrllibRequest;
import com.verygood.security.larky.objects.descriptor.LarkyNativeProperty;
import com.verygood.security.larky.objects.type.LarkyType;
import com.verygood.security.larky.objects.type.LarkyTypeObject;
import java.util.function.BiConsumer;
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

  /**
   * The class, a subclass of {@code Request}: it adds the properties {@code url}, {@code body} and
   * {@code headers}. The header container is {@code _headers} in the request's {@code __dict__}.
   */
  public static final LarkyType TYPE = LarkyTypeObject.createBuiltinType(
      "VGSHttpRequest", UrllibRequest.TYPE, ImmutableMap.of(
          // full_url is a property: an instance attribute cannot replace it
          "url", LarkyNativeProperty.of("url", VgsHttpRequest.class, VgsHttpRequest::fullUrl, VgsHttpRequest::setUrl),
          "body", LarkyNativeProperty.of(
              "body", VgsHttpRequest.class,
              (self, thread) -> self.data(), (self, value, thread) -> self.__setattr__("data", value, thread)),
          "headers", LarkyNativeProperty.of(
              "headers", VgsHttpRequest.class,
              (self, thread) -> self.state("_headers"), VgsHttpRequest::setHeaders)));

  private static final String HEADERS = "_headers";

  private final StarlarkCallable headerFactory;
  private final StarlarkCallable urlSplitter;

  private VgsHttpRequest(
      Object constructor, UrllibRequest base, StarlarkCallable headerFactory, StarlarkCallable urlSplitter) {
    super(TYPE, constructor, base);
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
    VgsHttpRequest request = new VgsHttpRequest(constructor, base, headerFactory, urlSplitter);
    // urllib's plain headers attribute gives way to the property over _headers
    request.__dict__().pop("headers", Starlark.NONE, thread);
    request.setState(HEADERS, call(thread, headerFactory, headers));
    request.updatePath(url, thread);
    return request;
  }

  private void setUrl(Object value, StarlarkThread thread) throws EvalException, InterruptedException {
    __setattr__("full_url", value, thread);
    updatePath(value, thread);
  }

  private void setHeaders(Object value, StarlarkThread thread) throws EvalException, InterruptedException {
    checkMutable();
    setState(HEADERS, call(thread, headerFactory, value));
  }

  private void updatePath(Object url, StarlarkThread thread) throws EvalException, InterruptedException {
    checkMutable();
    Object parsed = call(thread, urlSplitter, url);
    setState("path", Starlark.getattr(thread.mutability(), thread.getSemantics(), parsed, "path", null));
    setState("query_string", Starlark.getattr(thread.mutability(), thread.getSemantics(), parsed, "query", null));
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
    return state(HEADERS) instanceof LarkyMultiDict multiDict && multiDict.visitItems(visitor);
  }

  /** The header container's {@code items()}, or None when the headers are None. */
  public Object headerItems(StarlarkThread thread) throws EvalException, InterruptedException {
    Object headers = state(HEADERS);
    return Starlark.isNullOrNone(headers) ? Starlark.NONE : callField(thread, headers, "items");
  }
}
