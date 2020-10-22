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

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import javax.annotation.Nullable;

/**
 * A Starlark dependency resolver that resolves relative paths and absolute paths if
 * {@code rootPath} is defined.
 */
public class PathBasedStarFile implements StarFile {

  private final Path path;
  @Nullable private final Path rootPath;
  private final String identifierPrefix;

  public PathBasedStarFile(
      Path path, @Nullable Path rootPath, @Nullable String identifierPrefix) {
    /*logFileContent=*/
    Preconditions.checkArgument(path.isAbsolute());
    this.path = path;
    this.rootPath = rootPath;
    this.identifierPrefix = identifierPrefix;
    if (identifierPrefix != null) {
      // Check we don't generate weird identifiers like identifierPrefix + "/absolute/path"
      Preconditions.checkNotNull(rootPath, "identifierPrefix requires a non null root");
    }
  }

  @Override
  public StarFile resolve(String path) {
    Path resolved = StarFile.isAbsolute(path)
        ? relativeToRoot(path)
        : relativeToCurrentPath(path);

    if (!Files.exists(resolved)) {
      throw new RuntimeException(
          String.format("Cannot find '%s'. '%s' does not exist.", path, resolved));
    }
    if (!Files.isRegularFile(resolved)) {
      throw new RuntimeException(
          String.format("Cannot find '%s'. '%s' is not a file.", path, resolved));
    }
    return new PathBasedStarFile(resolved, rootPath, identifierPrefix);
  }

  @Override
  public String path() {
    return path.toString();
  }

  @Override
  public String getIdentifier() {
    if (rootPath == null) {
      return path();
    }

    return (Strings.isNullOrEmpty(identifierPrefix) ? "" : identifierPrefix + "/")
        + rootPath.relativize(path).toString();
  }

  private Path relativeToCurrentPath(String label) {
    return this.path.resolveSibling(label);
  }

  private Path relativeToRoot(String path)  {
    if (rootPath == null) {
      throw new RuntimeException("Absolute paths are not allowed because the root config path"
          + " couldn't be automatically detected. Use XXX DID I GET HERE?" );
    }
    return rootPath.resolve(path.substring(2));
  }

  @Override
  public byte[] readContentBytes() throws IOException {
    try {
      return Files.readAllBytes(path);
    } catch (NoSuchFileException e) {
      throw new RuntimeException("Cannot resolve " + path, e);
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("path", path)
        .add("rootPath", rootPath)
        .add("identifierPrefix", identifierPrefix)
        .toString();
  }
}
