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

package com.verygood.security.larky.modules.urllib;

import com.verygood.security.larky.objects.LarkyNativeClass;
import com.verygood.security.larky.objects.LarkyNativeObject;
import java.util.Map;
import javax.annotation.Nullable;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkIterable;
import net.starlark.java.eval.StarlarkOperators;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkStrings;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

/**
 * {@code urllib.request.Request}, ported from CPython 3.10's {@code Lib/urllib/request.py}.
 *
 * <p>Its attributes are CPython's: the properties {@code full_url} and {@code data}, and the plain
 * attributes {@code _full_url}, {@code fragment}, {@code type}, {@code host}, {@code selector},
 * {@code headers}, {@code unredirected_hdrs}, {@code _data}, {@code _tunnel_host},
 * {@code origin_req_host}, {@code unverifiable} and, when it was given, {@code method}. Where
 * CPython calls a method on {@code self} ({@code self.add_header}, {@code self._parse},
 * {@code self.has_header}, ...), so does this class, so a method a script sets on the instance is
 * the one called. The URL is taken apart by {@link UrlParts}, which {@code urllib.parse} uses too.
 */
@StarlarkBuiltin(name = "Request", documented = false)
public class UrllibRequest extends LarkyNativeObject {

  /**
   * The class: {@code full_url} and {@code data} are properties, and the rest of a request's state
   * is in its {@code __dict__}, under CPython's names.
   */
  public static final LarkyNativeClass<UrllibRequest> CLASS =
      LarkyNativeClass.named("Request", UrllibRequest.class)
          .property("full_url", UrllibRequest::fullUrl, UrllibRequest::setFullUrl)
          .property("data", (self, thread) -> self.state("_data"), UrllibRequest::setData)
          .build();

  private final StarlarkCallable unquote;
  private final StarlarkCallable requestHost;
  private final StarlarkCallable copyHeaderItems;

  /**
   * An uninitialized request. {@code unquote} is {@code urllib.parse.unquote}; {@code requestHost}
   * and {@code copyHeaderItems} are {@code urllib.request}'s {@code request_host(request)} and the
   * loop that adds the headers of a mapping other than a dict.
   */
  private UrllibRequest(
      Object constructor, StarlarkCallable unquote, StarlarkCallable requestHost, StarlarkCallable copyHeaderItems,
      StarlarkThread thread) {
    super(CLASS, constructor, thread.mutability());
    this.unquote = unquote;
    this.requestHost = requestHost;
    this.copyHeaderItems = copyHeaderItems;
  }

  /**
   * A request of the class {@code cls}, a subclass of {@link #CLASS} such as {@code
   * VGSHttpRequest}'s, that takes over the state of {@code base}. {@code base} is a request that
   * {@code urllib.request.Request(...)} has just built and that nothing else refers to.
   */
  protected UrllibRequest(LarkyNativeClass<? extends UrllibRequest> cls, Object constructor, UrllibRequest base) {
    super(cls, constructor, base);
    this.unquote = base.unquote;
    this.requestHost = base.requestHost;
    this.copyHeaderItems = base.copyHeaderItems;
  }

  /** {@code Request(url, data, headers, origin_req_host, unverifiable, method)}, whose class is {@code constructor}. */
  public static UrllibRequest create(
      Object constructor, StarlarkCallable unquote, StarlarkCallable requestHost, StarlarkCallable copyHeaderItems,
      Object url, Object data, Object headers, Object originReqHost, Object unverifiable, Object method,
      StarlarkThread thread) throws EvalException, InterruptedException {
    UrllibRequest request = new UrllibRequest(constructor, unquote, requestHost, copyHeaderItems, thread);
    request.initialize(url, data, headers, originReqHost, unverifiable, method, thread);
    return request;
  }

  /** The value of {@code _data}, for hosts. */
  public final Object data() {
    Object data = state("_data");
    return data == null ? Starlark.NONE : data;
  }

  // Initialization

  /** {@code __init__}: CPython's {@code Request.__init__}. */
  protected final void initialize(
      Object url, Object data, Object headers, Object originReqHost, Object unverifiable, Object method,
      StarlarkThread thread) throws EvalException, InterruptedException {
    setAttr(thread, "full_url", url);
    // headers first: the order of __dict__
    setAttr(thread, "headers", Dict.of(thread.mutability()));
    setAttr(thread, "unredirected_hdrs", Dict.of(thread.mutability()));
    setAttr(thread, "_data", Starlark.NONE);
    setAttr(thread, "data", data);
    setAttr(thread, "_tunnel_host", Starlark.NONE);
    if (headers instanceof Dict<?, ?> dict) {
      chargeSteps(thread, dict.size());
      // Snapshot the items, as the Starlark loop does: add_header can change the source.
      for (Object item : dict.items(thread)) {
        chargeSteps(thread, 0);
        Tuple pair = (Tuple) item;
        callAttr(thread, "add_header", pair.get(0), pair.get(1));
      }
    } else {
      Starlark.positionalOnlyCall(thread, copyHeaderItems, this, headers);
    }
    setAttr(thread, "origin_req_host", Starlark.checkedEquals(originReqHost, Starlark.NONE)
        ? Starlark.positionalOnlyCall(thread, requestHost, this) : originReqHost);
    setAttr(thread, "unverifiable", unverifiable);
    if (Starlark.truth(method)) {
      setAttr(thread, "method", method);
    }
  }

  @StarlarkMethod(name = "__init__", documented = false,
      parameters = {
        @Param(name = "url", named = true),
        @Param(name = "data", defaultValue = "None", named = true),
        @Param(name = "headers", defaultValue = "{}", named = true),
        @Param(name = "origin_req_host", defaultValue = "None", named = true),
        @Param(name = "unverifiable", defaultValue = "False", named = true),
        @Param(name = "method", defaultValue = "None", named = true)
      },
      useStarlarkThread = true)
  public void init(Object url, Object data, Object headers, Object originReqHost, Object unverifiable,
      Object method, StarlarkThread thread) throws EvalException, InterruptedException {
    initialize(url, data, headers, originReqHost, unverifiable, method, thread);
  }

  // full_url and data

  /** {@code full_url}. Without an evaluation (a host reading it), it needs a string URL and fragment. */
  protected final Object fullUrl(@Nullable StarlarkThread thread) throws EvalException, InterruptedException {
    Object fullUrl = requireState("_full_url");
    Object fragment = requireState("fragment");
    if (!Starlark.truth(fragment)) {
      return fullUrl;
    }
    if (fullUrl instanceof String url && fragment instanceof String text) {
      return url + "#" + text;
    }
    if (thread == null) {
      throw new IllegalStateException("full_url of a non-string URL or fragment needs an active evaluation");
    }
    // Keep the original format operation for other values.
    return callField(thread, "{}#{}", "format", fullUrl, fragment);
  }

  private void setFullUrl(Object url, StarlarkThread thread) throws EvalException, InterruptedException {
    // unwrap('<URL:type://host/path>') --> 'type://host/path'
    String unwrapped = UrlParts.unwrap(
        url instanceof String text ? text : Starlark.str(url, thread.getSemantics()), thread);
    Tuple parts = UrlParts.splittag(unwrapped);
    setState("_full_url", parts.get(0));
    setState("fragment", parts.get(1));
    callAttr(thread, "_parse");
  }

  private void setData(Object value, StarlarkThread thread) throws EvalException, InterruptedException {
    checkMutable();
    Object current = state("_data");
    if (current == null || !Starlark.checkedEquals(value, current)) {
      setState("_data", value);
      // If the data changes, the Content-length header is most probably for the previous value (issue 16464).
      if (Starlark.truth(callAttr(thread, "has_header", "Content-length"))) {
        callAttr(thread, "remove_header", "Content-length");
      }
    }
  }

  @StarlarkMethod(name = "_parse", documented = false, useStarlarkThread = true)
  public void parseUrl(StarlarkThread thread) throws EvalException, InterruptedException {
    Tuple typeAndRest = UrlParts.splittype(requireState("_full_url"), thread);
    setState("type", typeAndRest.get(0));
    if (typeAndRest.get(0) == Starlark.NONE) {
      throw Starlark.errorf("ValueError: unknown url type: %s",
          Starlark.repr(attr(thread, "full_url"), thread.getSemantics()));
    }
    Tuple hostAndSelector = UrlParts.splithost(typeAndRest.get(1));
    setState("host", hostAndSelector.get(0));
    setState("selector", hostAndSelector.get(1));
    // unquote returns a string without a '%' as it is
    if (hostAndSelector.get(0) instanceof String name && name.indexOf('%') >= 0) {
      setState("host", Starlark.positionalOnlyCall(thread, unquote, name));
    }
  }

  // Methods

  @StarlarkMethod(name = "get_method", documented = false, useStarlarkThread = true)
  public Object getMethod(StarlarkThread thread) throws EvalException, InterruptedException {
    // getattr(self, 'method', default_method): method is set only when given
    Object method = state("method");
    if (method != null) {
      return method;
    }
    return Starlark.checkedEquals(attr(thread, "data"), Starlark.NONE) ? "GET" : "POST";
  }

  @StarlarkMethod(name = "get_full_url", documented = false, useStarlarkThread = true)
  public Object getFullUrl(StarlarkThread thread) throws EvalException, InterruptedException {
    return attr(thread, "full_url");
  }

  @StarlarkMethod(name = "set_proxy", documented = false,
      parameters = {@Param(name = "host", named = true), @Param(name = "type", named = true)}, useStarlarkThread = true)
  public void setProxy(Object proxyHost, Object proxyType, StarlarkThread thread)
      throws EvalException, InterruptedException {
    if ("https".equals(state("type")) && !Starlark.truth(state("_tunnel_host"))) {
      setState("_tunnel_host", requireState("host"));
    } else {
      setState("type", proxyType);
      setState("selector", attr(thread, "full_url"));
    }
    setState("host", proxyHost);
  }

  @StarlarkMethod(name = "has_proxy", documented = false, useStarlarkThread = true)
  public boolean hasProxy(StarlarkThread thread) throws EvalException, InterruptedException {
    return Starlark.checkedEquals(requireState("selector"), attr(thread, "full_url"));
  }

  @StarlarkMethod(name = "add_header", documented = false,
      parameters = {@Param(name = "key", named = true), @Param(name = "val", named = true)}, useStarlarkThread = true)
  public void addHeader(Object key, Object value, StarlarkThread thread) throws EvalException, InterruptedException {
    // Useful for something like authentication
    StarlarkOperators.setIndex(thread, attr(thread, "headers"), capitalize(key, thread), value);
  }

  @StarlarkMethod(name = "add_unredirected_header", documented = false,
      parameters = {@Param(name = "key", named = true), @Param(name = "val", named = true)}, useStarlarkThread = true)
  public void addUnredirectedHeader(Object key, Object value, StarlarkThread thread)
      throws EvalException, InterruptedException {
    // will not be added to a redirected request
    StarlarkOperators.setIndex(thread, attr(thread, "unredirected_hdrs"), capitalize(key, thread), value);
  }

  private static Object capitalize(Object key, StarlarkThread thread) throws EvalException, InterruptedException {
    return key instanceof String text
        ? StarlarkStrings.capitalize(text, thread)
        : callField(thread, key, "capitalize");
  }

  @StarlarkMethod(name = "has_header", documented = false,
      parameters = {@Param(name = "header_name", named = true)}, useStarlarkThread = true)
  public boolean hasHeader(Object name, StarlarkThread thread) throws EvalException, InterruptedException {
    return StarlarkOperators.in(thread, name, attr(thread, "headers"))
        || StarlarkOperators.in(thread, name, attr(thread, "unredirected_hdrs"));
  }

  @StarlarkMethod(name = "get_header", documented = false,
      parameters = {@Param(name = "header_name", named = true), @Param(name = "default", defaultValue = "None", named = true)},
      useStarlarkThread = true)
  public Object getHeader(Object name, Object fallback, StarlarkThread thread)
      throws EvalException, InterruptedException {
    Object primary = attr(thread, "headers");
    // Look up the primary get method before evaluating its (eager) default, as Python does.
    Object primaryGet = primary instanceof Dict<?, ?> ? null : Starlark.getattr(
        thread.mutability(), thread.getSemantics(), primary, "get", null);
    Object secondary = attr(thread, "unredirected_hdrs");
    Object defaultValue = secondary instanceof Dict<?, ?> dict ? dict.get2(name, fallback, thread)
        : callField(thread, secondary, "get", name, fallback);
    return primary instanceof Dict<?, ?> dict ? dict.get2(name, defaultValue, thread)
        : call(thread, primaryGet, name, defaultValue);
  }

  @StarlarkMethod(name = "remove_header", documented = false,
      parameters = {@Param(name = "header_name", named = true)}, useStarlarkThread = true)
  public void removeHeader(Object name, StarlarkThread thread) throws EvalException, InterruptedException {
    pop(attr(thread, "headers"), name, thread);
    pop(attr(thread, "unredirected_hdrs"), name, thread);
  }

  private static void pop(Object mapping, Object key, StarlarkThread thread) throws EvalException, InterruptedException {
    if (mapping instanceof Dict<?, ?> dict) {
      dict.pop(key, Starlark.NONE, thread);
    } else {
      callField(thread, mapping, "pop", key, Starlark.NONE);
    }
  }

  @StarlarkMethod(name = "header_items", documented = false, useStarlarkThread = true)
  public Object mergedHeaderItems(StarlarkThread thread) throws EvalException, InterruptedException {
    Dict<Object, Object> merged = Dict.of(thread.mutability());
    mergeHeaders(merged, attr(thread, "unredirected_hdrs"), thread);
    mergeHeaders(merged, attr(thread, "headers"), thread);
    return merged.items(thread);
  }

  private static void mergeHeaders(Dict<Object, Object> merged, Object source, StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (source instanceof Map<?, ?> map) {
      chargeSteps(thread, map.size());
      merged.update(source, Dict.empty(), thread);
      return;
    }
    if (!(source instanceof StarlarkIterable<?> pairs)) {
      throw Starlark.errorf("in update, got %s, want iterable", Starlark.type(source));
    }
    int position = 0;
    for (Object item : pairs) {
      chargeSteps(thread, 1);
      // Dict.update accepts any Starlark iterable of two-element iterables.
      if (item instanceof Sequence<?> pair) {
        checkPairLength(position, pair.size());
        merged.putEntry(pair.get(0), pair.get(1));
      } else if (item instanceof StarlarkIterable<?> pair) {
        Object key = null;
        Object value = null;
        int length = 0;
        for (Object element : pair) {
          chargeSteps(thread, 1);
          if (length == 0) {
            key = element;
          } else if (length == 1) {
            value = element;
          }
          length++;
        }
        checkPairLength(position, length);
        merged.putEntry(key, value);
      } else {
        throw Starlark.errorf("in update, dictionary update sequence element #%d is not iterable (%s)",
            position, Starlark.type(item));
      }
      position++;
    }
  }

  private static void checkPairLength(int position, int length) throws EvalException {
    if (length != 2) {
      throw Starlark.errorf("in update, item #%d has length %d, but exactly two elements are required",
          position, length);
    }
  }

  /** The method a script would see, without calling a script's override: {@code method}, else GET or POST. */
  protected final Object methodForDisplay() {
    Object method = state("method");
    if (method != null) {
      return method;
    }
    return state("_data") == Starlark.NONE ? "GET" : "POST";
  }

  @Override
  public void repr(Printer printer, StarlarkSemantics semantics) {
    // Neither the URL nor the body: either can carry sensitive values.
    printer.append("<").append(typeName()).append(" [");
    Object shown = methodForDisplay();
    if (shown instanceof String text) {
      printer.append(text);
    } else {
      printer.repr(shown, semantics);
    }
    printer.append("]>");
  }
}
