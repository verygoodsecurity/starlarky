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

import com.verygood.security.larky.modules.types.structs.VgsHttpResponse;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

/** Native construction for the public VGS HTTP response constructors. */
@StarlarkBuiltin(name = "http_response_native", documented = false, doc = "VGS HTTP response construction.")
public final class HttpResponseNativeModule implements StarlarkValue {
  public static final HttpResponseNativeModule INSTANCE = new HttpResponseNativeModule();

  private HttpResponseNativeModule() {}

  @Override
  public boolean isImmutable() {
    return true;
  }

  @StarlarkMethod(
      name = "create", documented = false,
      parameters = {
        @Param(name = "constructor"), @Param(name = "header_factory"),
        @Param(name = "body"), @Param(name = "headers"), @Param(name = "status_code"),
        @Param(name = "short_circuit")
      },
      useStarlarkThread = true)
  public Object create(
      StarlarkCallable constructor, StarlarkCallable headerFactory, Object body, Object headers,
      Object status, boolean shortCircuit, StarlarkThread thread)
      throws EvalException, InterruptedException {
    return VgsHttpResponse.create(constructor, headerFactory, body, headers, status, shortCircuit, thread);
  }
}
