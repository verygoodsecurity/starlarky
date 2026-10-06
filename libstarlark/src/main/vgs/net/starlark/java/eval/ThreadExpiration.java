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

package net.starlark.java.eval;

/** Lets built-ins that wait on their own deadline fail as evaluation does at expiration. */
public final class ThreadExpiration {

  private ThreadExpiration() {}

  /**
   * Throws the error evaluation raises once {@code thread} is past its expiration date
   * ({@link StarlarkThread#setExpirationMs}), reading the clock now rather than on the periodic
   * check. Returns normally if the thread has no expiration date or has not reached it.
   */
  public static void check(StarlarkThread thread) throws EvalException {
    long expirationMs = thread.getExpirationMs();
    if (expirationMs == Long.MAX_VALUE) {
      return;
    }
    thread.setExpirationMs(expirationMs); // re-arms the check, so it reads the clock
    thread.checkExpired();
  }
}
