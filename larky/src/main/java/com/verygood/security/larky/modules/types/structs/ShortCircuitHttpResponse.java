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

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.StarlarkThread;

/** An HTTP response that the host sends to the client immediately. */
public final class ShortCircuitHttpResponse extends VgsHttpResponse {
  ShortCircuitHttpResponse(Dict<String, Object> fields, StarlarkThread thread) {
    super(fields, thread);
  }
}
