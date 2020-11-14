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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import net.starlark.java.eval.Module;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;


/**
 * A Parsed Larky script file.
 *
 * <p> Objects of this class represent a parsed Larky script.
 */
@EqualsAndHashCode
@ToString
public final class ParsedStarFile implements StarFile {
  private final WeakReference<StarFile> starFile;
  private final String location;
  @Getter
  private final Map<String, Object> globals;

  @Getter
  private final Module module;

  public ParsedStarFile(StarFile content, String location, Map<String, Object> globals, Module module) {
    this.starFile = new WeakReference<>(content);
    this.location = Preconditions.checkNotNull(location);
    this.globals = ImmutableMap.copyOf(Preconditions.checkNotNull(globals));
    this.module = module;
  }

  /**
   * Location of the top-level config file. An arbitrary string meant to be used
   * for logging/debugging. It shouldn't be parsed, as the format might change.
   */
  public String getLocation() {
    return location;
  }

  /**
   * Reads values from the global frame of the skylark environment, i.e. global variables.
   */
  public <T> T getGlobalEnvironmentVariable(String name, Class<T> clazz) {
    return clazz.cast(globals.get(name));
  }

  @Override
  public StarFile resolve(String path) {
    // we will not resolve a parsed star file, all calls to resolving it will just be the file itself
    return this;
  }

  @Override
  public String path() {
    return getLocation();
  }

  @Override
  public byte[] readContentBytes() throws IOException {
    StarFile f = this.starFile.get();
    return f != null ? f.readContentBytes() : null;
  }

  @Override
  public String getIdentifier() {
    StarFile f = this.starFile.get();
    return f != null ? f.getIdentifier() : "";
  }
}
