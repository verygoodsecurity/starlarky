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

package com.verygood.security.larky.runtime;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Logs, at DEBUG and once per module and reason, why the module cache did not share a module. The
 * message holds the load label and the {@link ModuleAdmission.Decline} (class, global, field and
 * key names), never a value.
 */
@Slf4j
final class UncachedModuleLog {

  // label + reason: bounded by the number of Larky modules times the reasons
  private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

  private UncachedModuleLog() {}

  static void once(String label, ModuleAdmission.Decline decline) {
    if (log.isDebugEnabled() && LOGGED.add(label + " " + decline.reason())) {
      log.debug("module cache: not sharing {}: {}", label, decline);
    }
  }
}
