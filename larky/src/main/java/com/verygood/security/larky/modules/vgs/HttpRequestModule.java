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

import com.verygood.security.larky.modules.urllib.UrllibRequest;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

/** The constructor of {@code VGSHttpRequest} ({@link VgsHttpRequest}), for its Starlark facade. */
@StarlarkBuiltin(name = "jhttprequest", documented = false, doc = "VGS HTTP request construction.")
public final class HttpRequestModule implements StarlarkValue {

  public static final HttpRequestModule INSTANCE = new HttpRequestModule();

  private HttpRequestModule() {}

  @Override
  public boolean isImmutable() {
    return true;
  }

  @StarlarkMethod(
      name = "VGSHttpRequest",
      documented = false,
      doc = "VGSHttpRequest(url, data, headers, method), from the urllib request of the same url, data and method.",
      parameters = {
        @Param(name = "cls", doc = "The VGSHttpRequest function, the request's __class__."),
        @Param(
            name = "request",
            doc = "urllib.request.Request(url, data=data, method=method). It is not usable afterwards.",
            allowedTypes = {@ParamType(type = UrllibRequest.class)}),
        @Param(name = "header_factory", doc = "Builds the header container: VGSCIMultiDict."),
        @Param(name = "url_splitter", doc = "urllib.parse.urlsplit."),
        @Param(name = "url"),
        @Param(name = "headers")
      },
      useStarlarkThread = true)
  public VgsHttpRequest vgsHttpRequest(
      StarlarkCallable cls, UrllibRequest request, StarlarkCallable headerFactory, StarlarkCallable urlSplitter,
      Object url, Object headers, StarlarkThread thread)
      throws EvalException, InterruptedException {
    return VgsHttpRequest.create(cls, request, headerFactory, urlSplitter, url, headers, thread);
  }
}
