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

package com.verygood.security.larky.modules;

import com.verygood.security.larky.modules.urllib.UrlParts;
import com.verygood.security.larky.modules.urllib.UrllibRequest;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

/** Native code behind {@code @stdlib//urllib/parse} and {@code @stdlib//urllib/request} ({@code @stdlib//jurllib}). */
@StarlarkBuiltin(name = "jurllib", documented = false, doc = "Native code for urllib.")
public final class UrllibModule implements StarlarkValue {

  public static final UrllibModule INSTANCE = new UrllibModule();

  private UrllibModule() {}

  @Override
  public boolean isImmutable() {
    return true;
  }

  @StarlarkMethod(
      name = "Request",
      documented = false,
      doc = "urllib.request.Request(url, data, headers, origin_req_host, unverifiable, method).",
      parameters = {
        @Param(name = "cls", doc = "The urllib.request.Request function, the request's __class__."),
        @Param(name = "unquote", doc = "urllib.parse.unquote."),
        @Param(name = "request_host", doc = "urllib.request's request_host(request)."),
        @Param(name = "copy_header_items", doc = "Adds the headers of a mapping that is not a dict."),
        @Param(name = "url"),
        @Param(name = "data"),
        @Param(name = "headers"),
        @Param(name = "origin_req_host"),
        @Param(name = "unverifiable"),
        @Param(name = "method")
      },
      useStarlarkThread = true)
  public UrllibRequest request(
      StarlarkCallable cls, StarlarkCallable unquote, StarlarkCallable requestHost,
      StarlarkCallable copyHeaderItems, Object url, Object data, Object headers, Object originReqHost,
      Object unverifiable, Object method, StarlarkThread thread) throws EvalException, InterruptedException {
    return UrllibRequest.create(cls, unquote, requestHost, copyHeaderItems, url, data, headers,
        originReqHost, unverifiable, method, thread);
  }

  @StarlarkMethod(
      name = "unwrap",
      documented = false,
      doc = "urllib.parse.unwrap(url), for a string.",
      parameters = {@Param(name = "url", allowedTypes = {@ParamType(type = String.class)})},
      useStarlarkThread = true)
  public String unwrap(String url, StarlarkThread thread) {
    return UrlParts.unwrap(url, thread);
  }

  @StarlarkMethod(
      name = "splittype",
      documented = false,
      doc = "urllib.parse._splittype(url).",
      parameters = {@Param(name = "url")},
      useStarlarkThread = true)
  public Tuple splittype(Object url, StarlarkThread thread) throws EvalException {
    return UrlParts.splittype(url, thread);
  }

  @StarlarkMethod(
      name = "splithost",
      documented = false,
      doc = "urllib.parse._splithost(url).",
      parameters = {@Param(name = "url")})
  public Tuple splithost(Object url) throws EvalException {
    return UrlParts.splithost(url);
  }

  @StarlarkMethod(
      name = "splittag",
      documented = false,
      doc = "urllib.parse._splittag(url).",
      parameters = {@Param(name = "url")})
  public Tuple splittag(Object url) throws EvalException {
    return UrlParts.splittag(url);
  }
}
