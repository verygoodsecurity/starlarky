package com.verygood.security.larky.parser;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

public class InMemMapBackedStarFile implements StarFile {

  private final ImmutableMap<String, byte[]> starFiles;
  private final String current;
  // The file evaluation started from, and the cache namespace of the files it loads.
  private final String root;
  private final String cacheNamespace;

  public InMemMapBackedStarFile(ImmutableMap<String, byte[]> configFiles, String current) {
    this(configFiles, current, "");
  }

  /**
   * As above; programs compiled from the files {@code current} loads (not {@code current} itself)
   * are cached under {@code cacheNamespace}. A service can so separate its callers' code (e.g. by
   * tenant) while sharing its own entry script.
   */
  public InMemMapBackedStarFile(
      ImmutableMap<String, byte[]> configFiles, String current, String cacheNamespace) {
    this(configFiles, current, current, cacheNamespace);
  }

  private InMemMapBackedStarFile(
      ImmutableMap<String, byte[]> configFiles, String current, String root, String cacheNamespace) {
    this.starFiles = configFiles;
    this.current = current;
    this.root = root;
    this.cacheNamespace = cacheNamespace;
  }

  public static StarFile createStarFile(String filename, String starFileContent) {
      return new InMemMapBackedStarFile(
          new ImmutableMap.Builder<String, byte[]>()
              .put(filename, starFileContent.getBytes(UTF_8))
              .build(),
          filename);
  }

  @Override
  public final StarFile resolve(String path)  {
    String resolved = StarFile.isAbsolute(path)
        ? containsLabel(path.substring(2))
        : relativeToCurrentPath(path);
    if (!starFiles.containsKey(resolved)) {
      throw new RuntimeException(
          String.format("Cannot resolve '%s': '%s' does not exist.", path, resolved));
    }
    return new InMemMapBackedStarFile(starFiles, resolved, root, cacheNamespace);
  }

  @Override
  public String path() {
    return current;
  }

  @Override
  public String cacheNamespace() {
    return current.equals(root) ? "" : cacheNamespace;
  }

  @Override
  public String getIdentifier() {
    return path();
  }

  @Override
  public byte[] readContentBytes() {
    return starFiles.get(current);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("current", current)
        .add("starFiles", starFiles.keySet())
        .toString();
  }

  private String relativeToCurrentPath(String label) {
    int i = current.lastIndexOf("/");
    String resolved = i == -1 ? label : current.substring(0, i) + "/" + label;
    return containsLabel(resolved);
  }

  private String containsLabel(String resolved)  {
    if (!starFiles.containsKey(resolved)) {
      throw new RuntimeException(
          String.format("Cannot resolve '%s': does not exist.", resolved));
    }
    return resolved;
  }
}
