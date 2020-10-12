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

package com.verygood.security.larky.config;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

/** A Config file implementation that uses a map for storing the internal data structure.
 *
 * <p>Assumes all paths to be absolute.
 */
public class MapConfigFile implements ConfigFile {

  private final ImmutableMap<String, byte[]> configFiles;
  private final String current;

  public MapConfigFile(ImmutableMap<String, byte[]> configFiles, String current) {
    this.configFiles = configFiles;
    this.current = current;
  }

  @Override
  public final ConfigFile resolve(String path) {
    String resolved = ConfigFile.isAbsolute(path)
        ? containsLabel(path.substring(2))
        : relativeToCurrentPath(path);
    if (!configFiles.containsKey(resolved)) {
      throw new RuntimeException(
          String.format("Cannot resolve '%s': '%s' does not exist.", path, resolved));
    }
    return new MapConfigFile(configFiles, resolved);
  }

  @Override
  public String path() {
    return current;
  }

  @Override
  public String getIdentifier() {
    return path();
  }

  @Override
  public byte[] readContentBytes() {
    return configFiles.get(current);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("current", current)
        .add("configFiles", configFiles.keySet())
        .toString();
  }

  private String relativeToCurrentPath(String label)  {
    int i = current.lastIndexOf("/");
    String resolved = i == -1 ? label : current.substring(0, i) + "/" + label;
    return containsLabel(resolved);
  }

  private String containsLabel(String resolved)  {
    if (!configFiles.containsKey(resolved)) {
      throw new RuntimeException(
          String.format("Cannot resolve '%s': does not exist.", resolved));
    }
    return resolved;
  }
}
