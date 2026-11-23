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

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Structure;

/**
 * Constructors for {@code urllib.request.Request} ({@link HttpRequest}) and {@code VGSHttpRequest}
 * ({@link VgsHttpRequest}), for their Starlark facades.
 */
@StarlarkBuiltin(name = "jhttprequest", documented = false, doc = "HTTP request construction.")
public final class HttpRequestModule implements StarlarkValue {

  public static final HttpRequestModule INSTANCE = new HttpRequestModule();

  private HttpRequestModule() {}

  @Override
  public boolean isImmutable() {
    return true;
  }

  @StarlarkMethod(
      name = "Request", documented = false,
      parameters = {
        @Param(name = "constructor"), @Param(name = "parse"), @Param(name = "request_host"),
        @Param(name = "copy_header_items"), @Param(name = "url"), @Param(name = "data"),
        @Param(name = "headers"), @Param(name = "origin_req_host"), @Param(name = "unverifiable"),
        @Param(name = "method")
      },
      useStarlarkThread = true)
  public HttpRequest request(
      StarlarkCallable constructor, Structure parse, StarlarkCallable requestHost,
      StarlarkCallable copyHeaderItems, Object url, Object data, Object headers, Object originReqHost,
      Object unverifiable, Object method, StarlarkThread thread) throws EvalException, InterruptedException {
    return HttpRequest.create(constructor, parse, requestHost, copyHeaderItems, url, data, headers,
        originReqHost, unverifiable, method, thread);
  }

  @StarlarkMethod(
      name = "VGSHttpRequest", documented = false,
      parameters = {
        @Param(name = "constructor"), @Param(name = "parse"), @Param(name = "request_host"),
        @Param(name = "copy_header_items"), @Param(name = "header_factory"), @Param(name = "url_splitter"),
        @Param(name = "url"), @Param(name = "data"), @Param(name = "headers"), @Param(name = "method")
      },
      useStarlarkThread = true)
  public VgsHttpRequest vgsHttpRequest(
      StarlarkCallable constructor, Structure parse, StarlarkCallable requestHost,
      StarlarkCallable copyHeaderItems, StarlarkCallable headerFactory, StarlarkCallable urlSplitter,
      Object url, Object data, Object headers, Object method, StarlarkThread thread)
      throws EvalException, InterruptedException {
    return VgsHttpRequest.create(constructor, parse, requestHost, copyHeaderItems, headerFactory, urlSplitter,
        url, data, headers, method, thread);
  }
}
