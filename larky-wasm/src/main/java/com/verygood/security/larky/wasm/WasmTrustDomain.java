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

package com.verygood.security.larky.wasm;

import com.verygood.security.larky.wasm.WasmRuntime.WasmException;

/**
 * Host-supplied identity for an evaluation using the pooled isolate backend. The identity must
 * come from authenticated, host-owned request state, never a value exposed to customer scripts.
 * A scope is bound to the caller thread and must enclose both loading and running a module.
 */
public final class WasmTrustDomain {
  private static final String MAX_LENGTH_PROPERTY =
      "larky.wasm.isolate.pool.maxTrustDomainChars";
  private static final int DEFAULT_MAX_LENGTH = 256;
  private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

  private WasmTrustDomain() {}

  public static Scope enter(String identity) {
    int maxLength = maxLength();
    if (identity == null || identity.isBlank() || identity.length() > maxLength
        || identity.chars().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException("a short, nonblank WASM trust domain is required");
    }
    Scope previous = CURRENT.get();
    if (previous != null && !previous.identity.equals(identity)) {
      throw new IllegalStateException("cannot change WASM trust domain in an active evaluation");
    }
    Scope scope = new Scope(identity, previous);
    CURRENT.set(scope);
    return scope;
  }

  private static int maxLength() {
    String raw = System.getProperty(MAX_LENGTH_PROPERTY);
    if (raw == null) return DEFAULT_MAX_LENGTH;
    try {
      int value = Integer.parseInt(raw);
      if (value > 0) return value;
    } catch (NumberFormatException ignored) {
      // Report one consistent configuration error below.
    }
    throw new IllegalArgumentException(MAX_LENGTH_PROPERTY + " must be a positive integer");
  }

  static String require() throws WasmException {
    Scope scope = CURRENT.get();
    if (scope == null) {
      throw new WasmException(WasmException.Kind.INVALID_MODULE,
          "pooled WASM isolation requires a host-supplied trust domain");
    }
    return scope.identity;
  }

  public static final class Scope implements AutoCloseable {
    private final String identity;
    private final Scope previous;
    private boolean closed;

    private Scope(String identity, Scope previous) {
      this.identity = identity;
      this.previous = previous;
    }

    @Override
    public void close() {
      if (closed) return;
      if (CURRENT.get() != this) {
        throw new IllegalStateException("WASM trust-domain scopes must close in reverse order");
      }
      closed = true;
      if (previous == null) CURRENT.remove();
      else CURRENT.set(previous);
    }
  }
}
