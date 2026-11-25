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

package com.verygood.security.larky.annot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.starlark.java.eval.Mutability;

/**
 * Treats a Larky {@link Mutability.Freezable} as one value during cache admission once its
 * mutability is frozen.
 *
 * <p>The wrapper owns its mutable Java state. Every Starlark operation that writes to that state
 * checks the wrapper's mutability first. Host-supplied providers and other values that require
 * their own admission check belong outside the wrapper. Larky subclasses, including the concrete
 * digest implementations, inherit these requirements.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CacheableWhenFrozen {}
