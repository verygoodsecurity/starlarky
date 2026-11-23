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

package com.verygood.security.larky.modules.types.structs;

import javax.annotation.Nullable;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.StarlarkThread;

/** An HTTP response that the host sends to the client immediately. */
public final class ShortCircuitHttpResponse extends VgsHttpResponse {
  ShortCircuitHttpResponse(Dict<String, Object> fields, StarlarkThread thread) {
    super(fields, thread);
  }

  /**
   * A short-circuit response that decorates {@code response}: it shares the response's state (body, headers, status
   * code and any other field), so a change made through either is a change to the one response. Only its type and
   * its {@code __name__} say that the message stops here.
   */
  @SuppressWarnings("unchecked")
  static ShortCircuitHttpResponse decorating(VgsHttpResponse response, StarlarkThread thread) {
    if (response instanceof ShortCircuitHttpResponse shortCircuit) {
      return shortCircuit;
    }
    return new ShortCircuitHttpResponse((Dict<String, Object>) response.fields, thread);
  }

  @Override
  public Object getField(String name, @Nullable StarlarkThread thread) {
    // the decorated response's fields name it VGSHttpResponse
    return "__name__".equals(name) ? "ShortCircuitResponse" : super.getField(name, thread);
  }
}
