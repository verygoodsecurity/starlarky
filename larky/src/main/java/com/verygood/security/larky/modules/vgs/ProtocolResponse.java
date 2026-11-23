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

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

/**
 * A protocol's own response that a script returns, such as an HTTP response ({@link VgsHttpResponse}). A script can
 * return it as is, or wrapped by {@code proxy.ShortCircuitResponse(response)} to stop processing the message with
 * it. A new protocol's response type implements this, and short-circuits the same way.
 */
public interface ProtocolResponse extends StarlarkValue {

  /** This response as a {@link ShortCircuit}: the same response, decorated to stop processing the message. */
  ShortCircuit shortCircuit(StarlarkThread thread) throws EvalException;
}
