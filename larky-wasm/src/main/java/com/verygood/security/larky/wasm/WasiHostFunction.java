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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a WASI import; the host's allowlist separately authorizes its use.
 * Methods are instance methods with GuestMemory first, then int for i32 and long for i64.
 * The result is int for errno, or void for proc_exit. Signatures use i for i32, I for i64,
 * and a colon before the result.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface WasiHostFunction {
  String name();
  String signature();

  /** False for explicit NOSYS stubs, which cannot be granted by a host policy. */
  boolean implemented() default true;
}
