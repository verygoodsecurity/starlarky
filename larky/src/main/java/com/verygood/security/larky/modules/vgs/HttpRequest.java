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

import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.verygood.security.larky.modules.types.LarkyInstance;
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
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Structure;
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
 * the one called. Strings up to 16 KiB of ASCII are parsed natively; other values go through the
 * Starlark {@code urllib.parse} functions, as before.
 */
@StarlarkBuiltin(name = "Request", documented = false)
public sealed class HttpRequest extends LarkyInstance permits VgsHttpRequest {

  // Use the Starlark parser for unusual values and large strings. Native scans are bounded.
  private static final int MAX_FAST_STRING_LENGTH = 16 * 1024;
  private static final CharMatcher ASCII = CharMatcher.ascii();
  // StringModule strips these ASCII characters in both its Python and Bazel modes.
  private static final CharMatcher STRIP = CharMatcher.anyOf("\t\n\u000b\f\r\u001c\u001d\u001e\u001f ");
  private static final CharMatcher HOST_END = CharMatcher.anyOf("/#?");

  private final Structure parse;
  private final StarlarkCallable requestHost;
  private final StarlarkCallable copyHeaderItems;

  private Object fullUrl = Starlark.NONE;
  private Object fragment = Starlark.NONE;
  @Nullable private Object type;
  @Nullable private Object host;
  @Nullable private Object selector;
  private Object headers = Starlark.NONE;
  private Object unredirectedHdrs = Starlark.NONE;
  private Object data = Starlark.NONE;
  private Object tunnelHost = Starlark.NONE;
  private Object originReqHost = Starlark.NONE;
  private Object unverifiable = Boolean.FALSE;
  @Nullable private Object method;

  /**
   * An uninitialized request. {@code parse} is the {@code urllib.parse} module; {@code requestHost}
   * and {@code copyHeaderItems} are {@code urllib.request}'s {@code request_host(request)} and the
   * loop that adds the headers of a mapping other than a dict.
   */
  HttpRequest(
      Object constructor, Structure parse, StarlarkCallable requestHost, StarlarkCallable copyHeaderItems,
      StarlarkThread thread) {
    super(constructor, thread.mutability());
    this.parse = parse;
    this.requestHost = requestHost;
    this.copyHeaderItems = copyHeaderItems;
  }

  /** {@code Request(url, data, headers, origin_req_host, unverifiable, method)}, whose class is {@code constructor}. */
  public static HttpRequest create(
      Object constructor, Structure parse, StarlarkCallable requestHost, StarlarkCallable copyHeaderItems,
      Object url, Object data, Object headers, Object originReqHost, Object unverifiable, Object method,
      StarlarkThread thread) throws EvalException, InterruptedException {
    HttpRequest request = new HttpRequest(constructor, parse, requestHost, copyHeaderItems, thread);
    request.initialize(url, data, headers, originReqHost, unverifiable, method, thread);
    return request;
  }

  @Override
  public String typeName() {
    return "Request";
  }

  // Attributes

  @Nullable
  @Override
  protected Object getDeclared(String name, StarlarkThread thread) throws EvalException, InterruptedException {
    switch (name) {
      case "full_url":
        return fullUrl(thread);
      case "data":
      case "_data":
        return data;
      case "_full_url":
        return fullUrl;
      case "fragment":
        return fragment;
      case "type":
        return type;
      case "host":
        return host;
      case "selector":
        return selector;
      case "headers":
        return headers;
      case "unredirected_hdrs":
        return unredirectedHdrs;
      case "_tunnel_host":
        return tunnelHost;
      case "origin_req_host":
        return originReqHost;
      case "unverifiable":
        return unverifiable;
      case "method":
        return method;
      default:
        return null;
    }
  }

  @Override
  protected boolean setDeclared(String name, Object value, StarlarkThread thread)
      throws EvalException, InterruptedException {
    checkMutable();
    switch (name) {
      case "full_url":
        setFullUrl(value, thread);
        return true;
      case "data":
        setData(value, thread);
        return true;
      case "_data":
        data = value;
        return true;
      case "_full_url":
        fullUrl = value;
        return true;
      case "fragment":
        fragment = value;
        return true;
      case "type":
        type = value;
        return true;
      case "host":
        host = value;
        return true;
      case "selector":
        selector = value;
        return true;
      case "headers":
        headers = value;
        return true;
      case "unredirected_hdrs":
        unredirectedHdrs = value;
        return true;
      case "_tunnel_host":
        tunnelHost = value;
        return true;
      case "origin_req_host":
        originReqHost = value;
        return true;
      case "unverifiable":
        unverifiable = value;
        return true;
      case "method":
        method = value;
        return true;
      default:
        return false;
    }
  }

  /** The name under which {@code __dict__} shows the header container. */
  protected String headersStateName() {
    return "headers";
  }

  @Override
  protected void addState(Map<String, Object> state) {
    state.put("_full_url", fullUrl);
    state.put("fragment", fragment);
    putIfSet(state, "type", type);
    putIfSet(state, "host", host);
    putIfSet(state, "selector", selector);
    state.put(headersStateName(), headers);
    state.put("unredirected_hdrs", unredirectedHdrs);
    state.put("_data", data);
    state.put("_tunnel_host", tunnelHost);
    state.put("origin_req_host", originReqHost);
    state.put("unverifiable", unverifiable);
    putIfSet(state, "method", method);
  }

  private static void putIfSet(Map<String, Object> state, String name, @Nullable Object value) {
    if (value != null) {
      state.put(name, value);
    }
  }

  @Override
  protected ImmutableList<String> declaredNames() {
    ImmutableList.Builder<String> names = ImmutableList.builder();
    names.add("full_url", "data", "_full_url", "fragment");
    for (String name : new String[] {"type", "host", "selector"}) {
      if (getDeclaredQuietly(name) != null) {
        names.add(name);
      }
    }
    names.add("headers", "unredirected_hdrs", "_data", "_tunnel_host", "origin_req_host", "unverifiable");
    if (method != null) {
      names.add("method");
    }
    return names.build();
  }

  @Nullable
  private Object getDeclaredQuietly(String name) {
    return switch (name) {
      case "type" -> type;
      case "host" -> host;
      case "selector" -> selector;
      default -> null;
    };
  }

  /** The value of {@code _data}, for hosts. */
  public final Object data() {
    return data;
  }

  /** The header container, for hosts and subclasses. */
  protected final Object headersValue() {
    return headers;
  }

  protected final void setHeadersValue(Object value) throws EvalException {
    checkMutable();
    headers = value;
  }

  // Initialization

  /** {@code __init__}: CPython's {@code Request.__init__}. */
  protected final void initialize(
      Object url, Object data, Object headers, Object originReqHost, Object unverifiable, Object method,
      StarlarkThread thread) throws EvalException, InterruptedException {
    setField("full_url", url);
    setField("unredirected_hdrs", Dict.of(thread.mutability()));
    setField("headers", Dict.of(thread.mutability()));
    setField("_data", Starlark.NONE);
    setField("data", data);
    setField("_tunnel_host", Starlark.NONE);
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
    setField("origin_req_host", Starlark.checkedEquals(originReqHost, Starlark.NONE)
        ? Starlark.positionalOnlyCall(thread, requestHost, this) : originReqHost);
    setField("unverifiable", unverifiable);
    if (Starlark.truth(method)) {
      setField("method", method);
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
    checkMutable();
    if (isFastString(url)) {
      String text = STRIP.trimFrom((String) url);
      if (text.startsWith("<") && text.endsWith(">")) {
        text = STRIP.trimFrom(text.substring(1, text.length() - 1));
      }
      url = text.startsWith("URL:") ? STRIP.trimFrom(text.substring(4)) : text;
    } else {
      url = callField(thread, parse, "unwrap", url);
    }
    if (isFastString(url)) {
      String text = (String) url;
      int hash = text.lastIndexOf('#');
      fullUrl = hash < 0 ? text : text.substring(0, hash);
      fragment = hash < 0 ? Starlark.NONE : text.substring(hash + 1);
    } else {
      Sequence<?> parts = parsePair(thread, "_splittag", url);
      fullUrl = parts.get(0);
      fragment = parts.get(1);
    }
    callAttr(thread, "_parse");
  }

  private void setData(Object value, StarlarkThread thread) throws EvalException, InterruptedException {
    checkMutable();
    if (!Starlark.checkedEquals(value, data)) {
      data = value;
      // If the data changes, the Content-length header is most probably for the previous value (issue 16464).
      if (Starlark.truth(callAttr(thread, "has_header", "Content-length"))) {
        callAttr(thread, "remove_header", "Content-length");
      }
    }
  }

  @StarlarkMethod(name = "_parse", documented = false, useStarlarkThread = true)
  public void parseUrl(StarlarkThread thread) throws EvalException, InterruptedException {
    checkMutable();
    Object rest;
    boolean nativeParts = isFastString(fullUrl);
    if (nativeParts) {
      String text = (String) fullUrl;
      int colon = text.indexOf(':');
      int slash = text.indexOf('/');
      boolean hasType = colon > 0 && (slash < 0 || colon < slash);
      type = hasType ? Ascii.toLowerCase(text.substring(0, colon)) : Starlark.NONE;
      rest = hasType ? text.substring(colon + 1) : text;
    } else {
      Sequence<?> parts = parsePair(thread, "_splittype", fullUrl);
      type = parts.get(0);
      rest = parts.get(1);
    }
    if (Starlark.checkedEquals(type, Starlark.NONE)) {
      throw Starlark.errorf("ValueError: unknown url type: %s",
          Starlark.repr(attr(thread, "full_url"), thread.getSemantics()));
    }
    if (nativeParts) {
      String text = (String) rest;
      if (text.startsWith("//")) {
        int end = HOST_END.indexIn(text, 2);
        String path = end < 0 ? "" : text.substring(end);
        host = text.substring(2, end < 0 ? text.length() : end);
        selector = path.isEmpty() || path.startsWith("/") ? path : "/" + path;
      } else {
        host = Starlark.NONE;
        selector = text;
      }
    } else {
      Sequence<?> parts = parsePair(thread, "_splithost", rest);
      host = parts.get(0);
      selector = parts.get(1);
    }
    if (Starlark.truth(host)) {
      if (!(host instanceof String name) || name.length() > MAX_FAST_STRING_LENGTH || name.indexOf('%') >= 0) {
        host = callField(thread, parse, "unquote", host);
      }
    }
  }

  private Sequence<?> parsePair(StarlarkThread thread, String function, Object value)
      throws EvalException, InterruptedException {
    Object result = callField(thread, parse, function, value);
    if (result instanceof Sequence<?> pair && pair.size() == 2) {
      return pair;
    }
    String got = result instanceof Sequence<?> sequence
        ? Starlark.type(result) + " of length " + sequence.size() : Starlark.type(result);
    throw Starlark.errorf("urllib.parse.%s(url=...) expected `tuple` of length 2; got %s", function, got);
  }

  // Methods

  @StarlarkMethod(name = "get_method", documented = false, useStarlarkThread = true)
  public Object getMethod(StarlarkThread thread) throws EvalException, InterruptedException {
    // getattr(self, 'method', default_method): method is set only when given
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
    checkMutable();
    if (Starlark.checkedEquals(type, "https") && !Starlark.truth(tunnelHost)) {
      tunnelHost = host;
    } else {
      type = proxyType;
      selector = attr(thread, "full_url");
    }
    host = proxyHost;
  }

  @StarlarkMethod(name = "has_proxy", documented = false, useStarlarkThread = true)
  public boolean hasProxy(StarlarkThread thread) throws EvalException, InterruptedException {
    return Starlark.checkedEquals(selector, attr(thread, "full_url"));
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
    if (isFastString(key)) {
      String text = (String) key;
      return text.isEmpty() ? text : Ascii.toUpperCase(text.charAt(0)) + Ascii.toLowerCase(text.substring(1));
    }
    return callField(thread, key, "capitalize");
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

  private static boolean isFastString(Object value) {
    return value instanceof String text && text.length() <= MAX_FAST_STRING_LENGTH && ASCII.matchesAllOf(text);
  }

  /** The method a script would see, without calling a script's override: {@code method}, else GET or POST. */
  protected final Object methodForDisplay() {
    if (method != null) {
      return method;
    }
    return data == Starlark.NONE ? "GET" : "POST";
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
