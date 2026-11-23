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

import javax.annotation.Nullable;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.StarlarkThread;

/** An HTTP response that the host sends to the client immediately. */
public final class ShortCircuitHttpResponse extends VgsHttpResponse implements ShortCircuit {

  // the response this decorates: a script's VgsHttpResponse, or this one when built from fields
  private final VgsHttpResponse decorated;

  ShortCircuitHttpResponse(Dict<String, Object> fields, StarlarkThread thread) {
    super(fields, thread);
    this.decorated = this;
  }

  private ShortCircuitHttpResponse(VgsHttpResponse decorated, StarlarkThread thread) {
    super(decorated.sharedFields(), thread);
    this.decorated = decorated;
  }

  /**
   * A short-circuit response that decorates {@code response}: it shares the response's state (body, headers, status
   * code and any other field), so a change made through either is a change to the one response. Only its type and
   * its {@code __name__} say that the message stops here.
   */
  static ShortCircuitHttpResponse decorating(VgsHttpResponse response, StarlarkThread thread) {
    return response instanceof ShortCircuitHttpResponse shortCircuit
        ? shortCircuit
        : new ShortCircuitHttpResponse(response, thread);
  }

  @Override
  public ProtocolResponse response() {
    return decorated;
  }

  @Override
  public ShortCircuit shortCircuit(StarlarkThread thread) {
    return this;
  }

  @Override
  public Object getField(String name, @Nullable StarlarkThread thread) {
    // the decorated response's fields name it VGSHttpResponse
    return "__name__".equals(name) ? "ShortCircuitResponse" : super.getField(name, thread);
  }
}
