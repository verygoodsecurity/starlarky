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

/**
 * A response that stops processing the message: the host sends it instead of continuing (in the request phase,
 * without calling the upstream), and skips the remaining filters. Hosts test {@code instanceof ShortCircuit} and
 * encode {@link #response()} by its protocol.
 */
public interface ShortCircuit extends ProtocolResponse {

  /** The protocol response to stop processing the message with. */
  ProtocolResponse response();
}
