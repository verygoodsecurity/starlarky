/*
 * Copyright (C) 2016 Google Inc.
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

package com.verygood.security.larky.parser;


import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;

/**`
 * An object representing a configuration file and that it can be used to resolve
 * other config files relative to this one.
 */
public interface StarFile {

  String ABSOLUTE_PREFIX = "//";

  /**
   * Check if the path is absolute and validates that the path is normalized
   * @throws RuntimeException if the path is not normalized
   */
  static boolean isAbsolute(String path)  {
    boolean isAbsolute = path.startsWith(ABSOLUTE_PREFIX);
    // Remove '//' for absolute paths
    String withoutPrefix = isAbsolute ? path.substring(2) : path;
    try {
      return isAbsolute;
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(String.format("Invalid path '%s': %s",
          withoutPrefix, e.getMessage()));
    }
  }

  /**
   * Resolve {@code path} relative to the current config file.
   *
   * @throws RuntimeException if the path cannot be resolved to a content
   */
  StarFile resolve(String path) ;

  /**
   * Resolved, non-relative name of the config file.
   */
  String path();

  /**
   * Separates cached compiled programs: a script is only reused from the cache for files with the
   * same namespace (e.g. the same tenant), even when path and source are identical. Empty by default.
   */
  default String cacheNamespace() {
    return "";
  }

  /**
   * Get the contents of the file.
   *
   * <p>Implementations of this interface should prefer to not eagerly load the content of this
   * method is call in order to allow the callers to check its own cache if they already have
   * {@link #path()} path.
   */
  byte[] readContentBytes() throws IOException;

  /**
   * Utility function to read the content of the config file as String.
   */
  default String readContent() throws IOException {
    return new String(readContentBytes(), UTF_8);
  }

  /**
   * Return a {@code String} representing a stable identifier that works between different
   * {@link StarFile} implementations. Note that this is best effort based on several heuristics.
   *
   * <p>If root is not defined or cannot be computed, it will return the absolute path.
   *
   * <p>Users of this method should not try to parse the string, since it is subject to change.
   */
  String getIdentifier();

  /**
   * The bytes of the file named {@code name} that was shipped with this evaluation (e.g. a
   * WebAssembly module that {@code wasm.module(name)} reads), or null if there is none. Files on
   * the class path or on disk are never shipped files.
   */
  default byte[] readShippedFile(String name) {
    return null;
  }
}
