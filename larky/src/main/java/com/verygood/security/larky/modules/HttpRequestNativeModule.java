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

package com.verygood.security.larky.modules;

import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.verygood.security.larky.modules.types.structs.HttpRequest;
import com.verygood.security.larky.modules.types.structs.VgsHttpRequest;
import java.util.Map;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkIndexable;
import net.starlark.java.eval.StarlarkIterable;
import net.starlark.java.eval.StarlarkMembershipTestable;
import net.starlark.java.eval.StarlarkSetIndexable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Structure;
import net.starlark.java.eval.Tuple;

/** Shared request operations behind the urllib and VGS Starlark facades. */
@StarlarkBuiltin(name = "http_request_native", documented = false, doc = "Shared HTTP request construction and mutation.")
public final class HttpRequestNativeModule implements StarlarkValue {
  public static final HttpRequestNativeModule INSTANCE = new HttpRequestNativeModule();

  // Use the Starlark parser for unusual values and large strings. Native scans are bounded.
  private static final int MAX_FAST_STRING_LENGTH = 16 * 1024;
  private static final CharMatcher ASCII = CharMatcher.ascii();
  // StringModule strips these ASCII characters in both its Python and Bazel modes.
  private static final CharMatcher STRIP = CharMatcher.anyOf(
      "\t\n\u000b\f\r\u001c\u001d\u001e\u001f ");
  private static final CharMatcher HOST_END = CharMatcher.anyOf("/#?");

  private HttpRequestNativeModule() {}

  @Override
  public boolean isImmutable() {
    return true;
  }

  @StarlarkMethod(
      name = "create", documented = false,
      parameters = @Param(name = "urllib_constructor"),
      useStarlarkThread = true)
  public VgsHttpRequest create(StarlarkCallable urllibConstructor, StarlarkThread thread) {
    return VgsHttpRequest.create(urllibConstructor, thread);
  }

  @StarlarkMethod(
      name = "install_fields", documented = false,
      parameters = {
        @Param(name = "request"), @Param(name = "url"), @Param(name = "headers"),
        @Param(name = "url_splitter"), @Param(name = "header_factory")
      },
      useStarlarkThread = true)
  public void installFields(
      VgsHttpRequest request, Object url, Object headers, StarlarkCallable urlSplitter,
      StarlarkCallable headerFactory, StarlarkThread thread) throws EvalException, InterruptedException {
    request.installFields(url, headers, urlSplitter, headerFactory, thread);
  }

  @StarlarkMethod(name = "create_generic", documented = false,
      parameters = @Param(name = "constructor"), useStarlarkThread = true)
  public HttpRequest createGeneric(StarlarkCallable constructor, StarlarkThread thread) {
    return HttpRequest.create(constructor, thread);
  }

  @StarlarkMethod(name = "initialize", documented = false,
      parameters = {
        @Param(name = "request"), @Param(name = "url"), @Param(name = "data"),
        @Param(name = "headers"), @Param(name = "origin_req_host"), @Param(name = "unverifiable"),
        @Param(name = "method"), @Param(name = "request_host"), @Param(name = "copy_header_items")
      },
      useStarlarkThread = true)
  public Structure initialize(Structure request, Object url, Object data, Object headers,
      Object originReqHost, Object unverifiable, Object method, StarlarkCallable requestHost,
      StarlarkCallable copyHeaderItems, StarlarkThread thread) throws EvalException, InterruptedException {
    request.setField("full_url", url);
    request.setField("unredirected_hdrs", Dict.of(thread.mutability()));
    request.setField("headers", Dict.of(thread.mutability()));
    request.setField("_data", Starlark.NONE);
    request.setField("data", data);
    request.setField("_tunnel_host", Starlark.NONE);
    if (headers instanceof Dict<?, ?> dict) {
      checkWork(thread, dict.size());
      // Snapshot items just as the Starlark loop does. add_header can modify the source.
      for (Object item : dict.items(thread)) {
        checkWork(thread, 0);
        Tuple pair = (Tuple) item;
        callField(thread, request, "add_header", pair.get(0), pair.get(1));
      }
    } else {
      Starlark.positionalOnlyCall(thread, copyHeaderItems, request, headers);
    }
    request.setField("origin_req_host", Starlark.checkedEquals(originReqHost, Starlark.NONE)
        ? Starlark.positionalOnlyCall(thread, requestHost, request) : originReqHost);
    request.setField("unverifiable", unverifiable);
    if (Starlark.truth(method)) {
      request.setField("method", method);
    }
    return request;
  }

  @StarlarkMethod(name = "full_url", documented = false,
      parameters = @Param(name = "request"), useStarlarkThread = true)
  public Object fullUrl(Structure request, StarlarkThread thread)
      throws EvalException, InterruptedException {
    Object fragment = attr(thread, request, "fragment");
    if (!Starlark.truth(fragment)) {
      return attr(thread, request, "_full_url");
    }
    // Preserve the original format operation for script-provided values.
    return callField(thread, "{}#{}", "format",
        attr(thread, request, "_full_url"), attr(thread, request, "fragment"));
  }

  @StarlarkMethod(name = "set_full_url", documented = false,
      parameters = {@Param(name = "request"), @Param(name = "url"), @Param(name = "parse")},
      useStarlarkThread = true)
  public void setFullUrl(Structure request, Object url, Structure parse, StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (isFastString(url)) {
      String text = STRIP.trimFrom((String) url);
      if (text.startsWith("<") && text.endsWith(">")) {
        text = STRIP.trimFrom(text.substring(1, text.length() - 1));
      }
      url = text.startsWith("URL:") ? STRIP.trimFrom(text.substring(4)) : text;
    } else {
      url = callField(thread, parse, "unwrap", url);
    }
    request.setField("_full_url", url);
    Object unwrapped = attr(thread, request, "_full_url");
    if (isFastString(unwrapped)) {
      String text = (String) unwrapped;
      int hash = text.lastIndexOf('#');
      request.setField("_full_url", hash < 0 ? text : text.substring(0, hash));
      request.setField("fragment", hash < 0 ? Starlark.NONE : text.substring(hash + 1));
    } else {
      Sequence<?> parts = parsePair(thread, parse, "_splittag", unwrapped);
      request.setField("_full_url", parts.get(0));
      request.setField("fragment", parts.get(1));
    }
    callField(thread, request, "_parse");
  }

  @StarlarkMethod(name = "parse_url", documented = false,
      parameters = {@Param(name = "request"), @Param(name = "parse")}, useStarlarkThread = true)
  public void parseUrl(Structure request, Structure parse, StarlarkThread thread)
      throws EvalException, InterruptedException {
    Object fullUrl = attr(thread, request, "_full_url");
    Object type;
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
      Sequence<?> parts = parsePair(thread, parse, "_splittype", fullUrl);
      type = parts.get(0);
      rest = parts.get(1);
    }
    request.setField("type", type);
    if (Starlark.checkedEquals(attr(thread, request, "type"), Starlark.NONE)) {
      throw Starlark.errorf("ValueError: unknown url type: %s",
          Starlark.repr(attr(thread, request, "full_url"), thread.getSemantics()));
    }
    if (nativeParts) {
      String text = (String) rest;
      if (text.startsWith("//")) {
        int end = HOST_END.indexIn(text, 2);
        String selector = end < 0 ? "" : text.substring(end);
        request.setField("host", text.substring(2, end < 0 ? text.length() : end));
        request.setField("selector",
            selector.isEmpty() || selector.startsWith("/") ? selector : "/" + selector);
      } else {
        request.setField("host", Starlark.NONE);
        request.setField("selector", text);
      }
    } else {
      Sequence<?> parts = parsePair(thread, parse, "_splithost", rest);
      request.setField("host", parts.get(0));
      request.setField("selector", parts.get(1));
    }
    Object host = attr(thread, request, "host");
    if (Starlark.truth(host)) {
      Object current = attr(thread, request, "host");
      if (!(current instanceof String name) || name.length() > MAX_FAST_STRING_LENGTH
          || name.indexOf('%') >= 0) {
        current = callField(thread, parse, "unquote", current);
      }
      request.setField("host", current);
    }
  }

  private static Sequence<?> parsePair(StarlarkThread thread, Structure parse, String method, Object value)
      throws EvalException, InterruptedException {
    Object result = callField(thread, parse, method, value);
    if (result instanceof Sequence<?> pair && pair.size() == 2) {
      return pair;
    }
    throw Starlark.errorf("urllib parser %s must return two values", method);
  }

  @StarlarkMethod(name = "set_data", documented = false,
      parameters = {@Param(name = "request"), @Param(name = "data")},
      useStarlarkThread = true)
  public void setData(Structure request, Object data, StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (!Starlark.checkedEquals(data, attr(thread, request, "_data"))) {
      request.setField("_data", data);
      if (Starlark.truth(callField(thread, request, "has_header", "Content-length"))) {
        callField(thread, request, "remove_header", "Content-length");
      }
    }
  }

  @StarlarkMethod(name = "get_method", documented = false,
      parameters = {@Param(name = "request"), @Param(name = "getattr")}, useStarlarkThread = true)
  public Object getMethod(Structure request, StarlarkCallable getattr, StarlarkThread thread)
      throws EvalException, InterruptedException {
    String fallback = Starlark.checkedEquals(attr(thread, request, "data"), Starlark.NONE)
        ? "GET" : "POST";
    return Starlark.positionalOnlyCall(thread, getattr, request, "method", fallback);
  }

  @StarlarkMethod(name = "set_proxy", documented = false,
      parameters = {@Param(name = "request"), @Param(name = "host"), @Param(name = "type")},
      useStarlarkThread = true)
  public void setProxy(Structure request, Object host, Object type, StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (Starlark.checkedEquals(attr(thread, request, "type"), "https")
        && !Starlark.truth(attr(thread, request, "_tunnel_host"))) {
      request.setField("_tunnel_host", attr(thread, request, "host"));
    } else {
      request.setField("type", type);
      request.setField("selector", attr(thread, request, "full_url"));
    }
    request.setField("host", host);
  }

  @StarlarkMethod(name = "has_proxy", documented = false,
      parameters = @Param(name = "request"), useStarlarkThread = true)
  public boolean hasProxy(Structure request, StarlarkThread thread)
      throws EvalException, InterruptedException {
    return Starlark.checkedEquals(attr(thread, request, "selector"), attr(thread, request, "full_url"));
  }

  @StarlarkMethod(name = "add_header", documented = false,
      parameters = {
        @Param(name = "request"), @Param(name = "field"), @Param(name = "key"), @Param(name = "value"),
        @Param(name = "capitalize"), @Param(name = "set_item")
      },
      useStarlarkThread = true)
  public void addHeader(Structure request, String field, Object key, Object value,
      boolean capitalize, StarlarkCallable setItem, StarlarkThread thread)
      throws EvalException, InterruptedException {
    Object mapping = attr(thread, request, field);
    if (capitalize) {
      if (isFastString(key)) {
        String text = (String) key;
        key = text.isEmpty() ? text : Ascii.toUpperCase(text.charAt(0)) + Ascii.toLowerCase(text.substring(1));
      } else {
        key = callField(thread, key, "capitalize");
      }
    }
    if (mapping instanceof Dict<?, ?> dict) {
      putHeader(dict, key, value);
    } else if (mapping instanceof StarlarkSetIndexable indexable) {
      indexable.setIndex(thread.getSemantics(), key, value);
    } else if (mapping instanceof StarlarkSetIndexable.Threaded indexable) {
      indexable.setIndex(thread, thread.getSemantics(), key, value);
    } else {
      Starlark.positionalOnlyCall(thread, setItem, mapping, key, value);
    }
  }

  @SuppressWarnings("unchecked")
  private static void putHeader(Dict<?, ?> dict, Object key, Object value) throws EvalException {
    ((Dict<Object, Object>) dict).putEntry(key, value);
  }

  @StarlarkMethod(name = "has_header", documented = false,
      parameters = {@Param(name = "request"), @Param(name = "key"), @Param(name = "contains")},
      useStarlarkThread = true)
  public Object hasHeader(Structure request, Object key, StarlarkCallable contains, StarlarkThread thread)
      throws EvalException, InterruptedException {
    Object redirected = contains(attr(thread, request, "headers"), key, contains, thread);
    return Starlark.truth(redirected) ? redirected
        : contains(attr(thread, request, "unredirected_hdrs"), key, contains, thread);
  }

  private static Object contains(Object mapping, Object key, StarlarkCallable fallback,
      StarlarkThread thread) throws EvalException, InterruptedException {
    if (mapping instanceof StarlarkMembershipTestable testable) {
      return testable.containsKey(thread.getSemantics(), key);
    }
    if (mapping instanceof StarlarkIndexable.Threaded indexable) {
      return indexable.containsKey(thread, thread.getSemantics(), key);
    }
    return Starlark.positionalOnlyCall(thread, fallback, mapping, key);
  }

  @StarlarkMethod(name = "get_header", documented = false,
      parameters = {@Param(name = "request"), @Param(name = "key"), @Param(name = "default")},
      useStarlarkThread = true)
  public Object getHeader(Structure request, Object key, Object fallback, StarlarkThread thread)
      throws EvalException, InterruptedException {
    Object primary = attr(thread, request, "headers");
    // Capture the primary method before evaluating the eager default, as the script did.
    Object primaryGet = primary instanceof Dict<?, ?> ? null : attr(thread, primary, "get");
    Object secondary = attr(thread, request, "unredirected_hdrs");
    Object defaultValue = secondary instanceof Dict<?, ?> dict ? dict.get2(key, fallback, thread)
        : callField(thread, secondary, "get", key, fallback);
    return primary instanceof Dict<?, ?> dict ? dict.get2(key, defaultValue, thread)
        : call(thread, primaryGet, key, defaultValue);
  }

  @StarlarkMethod(name = "remove_header", documented = false,
      parameters = {@Param(name = "request"), @Param(name = "key")}, useStarlarkThread = true)
  public void removeHeader(Structure request, Object key, StarlarkThread thread)
      throws EvalException, InterruptedException {
    pop(attr(thread, request, "headers"), key, thread);
    pop(attr(thread, request, "unredirected_hdrs"), key, thread);
  }

  private static void pop(Object mapping, Object key, StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (mapping instanceof Dict<?, ?> dict) {
      dict.pop(key, Starlark.NONE, thread);
    } else {
      callField(thread, mapping, "pop", key, Starlark.NONE);
    }
  }

  @StarlarkMethod(name = "header_items", documented = false,
      parameters = @Param(name = "request"), useStarlarkThread = true)
  public Object headerItems(Structure request, StarlarkThread thread)
      throws EvalException, InterruptedException {
    Dict<Object, Object> merged = Dict.of(thread.mutability());
    mergeHeaders(merged, attr(thread, request, "unredirected_hdrs"), thread);
    mergeHeaders(merged, attr(thread, request, "headers"), thread);
    return merged.items(thread);
  }

  private static void mergeHeaders(Dict<Object, Object> merged, Object source, StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (source instanceof Map<?, ?> map) {
      checkWork(thread, map.size());
      merged.update(source, Dict.empty(), thread);
      return;
    }
    if (!(source instanceof StarlarkIterable<?> pairs)) {
      throw Starlark.errorf("in update, got %s, want iterable", Starlark.type(source));
    }
    int position = 0;
    for (Object item : pairs) {
      checkWork(thread, 1);
      // Dict.update accepts any Starlark iterable of two-element iterables.
      if (item instanceof Sequence<?> pair) {
        checkPairLength(position, pair.size());
        merged.putEntry(pair.get(0), pair.get(1));
      } else if (item instanceof StarlarkIterable<?> pair) {
        Object key = null;
        Object value = null;
        int length = 0;
        for (Object element : pair) {
          checkWork(thread, 1);
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
    return value instanceof String text && text.length() <= MAX_FAST_STRING_LENGTH
        && ASCII.matchesAllOf(text);
  }

  private static Object attr(StarlarkThread thread, Object owner, String name)
      throws EvalException, InterruptedException {
    return Starlark.getattr(thread.mutability(), thread.getSemantics(), owner, name, null);
  }

  private static Object callField(StarlarkThread thread, Object owner, String name, Object... args)
      throws EvalException, InterruptedException {
    return call(thread, attr(thread, owner, name), args);
  }

  private static Object call(StarlarkThread thread, Object callable, Object... args)
      throws EvalException, InterruptedException {
    return callable instanceof StarlarkCallable function
        ? Starlark.positionalOnlyCall(thread, function, args)
        : Starlark.call(thread, callable, Tuple.of(args), Dict.empty());
  }

  private static void checkWork(StarlarkThread thread, int steps)
      throws EvalException, InterruptedException {
    if (Thread.currentThread().isInterrupted()) {
      throw new InterruptedException();
    }
    if (thread.getExpirationMs() != Long.MAX_VALUE && thread.isExpired()) {
      throw Starlark.errorf("Starlark computation cancelled: past expiration date");
    }
    long remaining = thread.getMaxExecutionSteps() - thread.getExecutedSteps();
    if (steps >= remaining) {
      // Spend the rest of the budget before failing, as the evaluator's ++steps >= limit does, so the
      // failure is reported as the step limit (LarkyEvaluator.failureOf) rather than a script error.
      thread.incrementExecutedSteps(Math.max(remaining, 0));
      throw Starlark.errorf("Starlark computation cancelled: too many steps");
    }
    thread.incrementExecutedSteps(steps);
  }
}
