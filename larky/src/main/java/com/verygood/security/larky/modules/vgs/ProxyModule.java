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
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkCallable;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

/** Native construction for {@code @vgs//proxy} ({@code @vgs//jproxy}). */
@StarlarkBuiltin(name = "jproxy", documented = false, doc = "VGS proxy control types.")
public final class ProxyModule implements StarlarkValue {
  public static final ProxyModule INSTANCE = new ProxyModule();

  private ProxyModule() {}

  @Override
  public boolean isImmutable() {
    return true;
  }

  @StarlarkMethod(
      name = "short_circuit_response", documented = false,
      parameters = {@Param(name = "protocol_short_circuit")})
  public StarlarkCallable shortCircuitResponse(StarlarkCallable protocolShortCircuit) {
    return new ShortCircuitResponse(protocolShortCircuit);
  }

  /**
   * {@code proxy.ShortCircuitResponse}: given a protocol response ({@link ProtocolResponse}, such as a
   * {@code VGSHttpResponse}), that response decorated to stop processing the message; given only {@code body},
   * {@code headers} and {@code status_code}, the protocol's own short circuit built from them (for HTTP,
   * {@code @vgs//http/response}'s ShortCircuitResponse).
   */
  static final class ShortCircuitResponse implements StarlarkCallable {
    private final StarlarkCallable protocolShortCircuit;

    ShortCircuitResponse(StarlarkCallable protocolShortCircuit) {
      this.protocolShortCircuit = protocolShortCircuit;
    }

    @Override
    public String getName() {
      return "ShortCircuitResponse";
    }

    @Override
    public boolean isImmutable() {
      return true;
    }

    @Override
    public Object call(StarlarkThread thread, Tuple args, Dict<String, Object> kwargs)
        throws EvalException, InterruptedException {
      if (args.isEmpty()) {
        return Starlark.call(thread, protocolShortCircuit, args, kwargs);
      }
      if (args.size() > 1 || !kwargs.isEmpty()) {
        throw Starlark.errorf(
            "ShortCircuitResponse takes either a protocol response or body, headers and status_code, not both");
      }
      if (!(args.get(0) instanceof ProtocolResponse response)) {
        throw Starlark.errorf(
            "ShortCircuitResponse takes a protocol response, one of [VGSHttpResponse], got %s",
            Starlark.type(args.get(0)));
      }
      return response.shortCircuit(thread);
    }
  }
}
