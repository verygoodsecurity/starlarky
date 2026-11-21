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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a native module that gives scripts a service the host provides (a vault, a network-token
 * service, metrics, a key holder...): it keeps the host's provider, so a Larky module that reaches
 * it is not shared through the module cache (it still runs, once per evaluation).
 *
 * <p>The annotation sits on the class rather than in a list of names, so a new service module is
 * classified where it is written. IsolationStructureTest requires it on every Starlark value class
 * with a field whose type is a host provider interface (a {@code .spi.} package).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface HostService {}
