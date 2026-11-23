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

import com.verygood.security.larky.modules.urllib.UrllibRequest;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Structure;

/** Native code behind {@code @stdlib//urllib/request} ({@code @stdlib//jurllib}). */
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
        @Param(name = "parse", doc = "The urllib.parse module."),
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
      StarlarkCallable cls, Structure parse, StarlarkCallable requestHost,
      StarlarkCallable copyHeaderItems, Object url, Object data, Object headers, Object originReqHost,
      Object unverifiable, Object method, StarlarkThread thread) throws EvalException, InterruptedException {
    return UrllibRequest.create(cls, parse, requestHost, copyHeaderItems, url, data, headers,
        originReqHost, unverifiable, method, thread);
  }
}
