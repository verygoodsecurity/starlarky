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

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkValue;

/** Native construction for {@code @vgs//proxy} ({@code @vgs//jproxy}). */
@StarlarkBuiltin(name = "jproxy", documented = false, doc = "VGS proxy control types.")
public final class ProxyModule implements StarlarkValue {
  public static final ProxyModule INSTANCE = new ProxyModule();

  private ProxyModule() {}

  @Override
  public boolean isImmutable() {
    return true;
  }

  /**
   * {@code proxy.ShortCircuitResponse(response)}: a {@link ShortCircuit} holding the protocol response
   * ({@link ProtocolResponse}, such as a {@code VGSHttpResponse}) to stop processing the message with.
   * Its class is {@code constructor}, the {@code proxy.ShortCircuitResponse} function that calls this.
   */
  @StarlarkMethod(
      name = "ShortCircuitResponse", documented = false,
      parameters = {@Param(name = "constructor"), @Param(name = "response")})
  public ShortCircuit shortCircuitResponse(Object constructor, Object response) throws EvalException {
    if (!(response instanceof ProtocolResponse protocolResponse)) {
      throw Starlark.errorf(
          "ShortCircuitResponse(response=...) expected `VGSHttpResponse`; got %s", Starlark.type(response));
    }
    return new ShortCircuitResponse(constructor, protocolResponse);
  }
}
