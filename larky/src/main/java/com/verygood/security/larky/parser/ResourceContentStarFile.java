package com.verygood.security.larky.parser;

import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.starlark.java.eval.EvalException;

import javax.annotation.Nullable;
import lombok.SneakyThrows;

public class ResourceContentStarFile implements StarFile {
  /*
     Right now, it just has them all as built-ins, with namespaces
     __builtin__.struct() // struct()
     unittest // exists in the global namespace by default
     import unittest // unittest
     load('unitest', 'unitest') => it now is usable in global namespace, otherwise, unknown symbol is thrown
   */
  /**
   * load("//testlib/builtinz", "setz") # works, but root is not defined.
   * load("./testlib/builtinz", "setz") # works load("testlib/builtinz", "setz", "collections")
   * load("/testlib/builtinz", "setz")  # does not work
   */
  private static final String STDLIB = "@stdlib";
  private static final String VENDOR = "@vendor//";
  private static final String VGS = "@vgs//";

  private String resourcePath;
  private byte[] content;

  private ResourceContentStarFile(String resourcePath, byte[] content) {
    this.resourcePath = resourcePath;
    this.content = content;
  }

  public static ResourceContentStarFile buildStarFile(String resourcePath, InputStream inputStream) throws IOException {
    return new ResourceContentStarFile(resourcePath,
      String.join(
        "\n",
          CharStreams.toString(new InputStreamReader(inputStream, StandardCharsets.UTF_8)))
        .getBytes());
  }

  public static ResourceContentStarFile buildStarFile(String resourcePath) throws EvalException {
    String resourceName = resolveResourceName(resourcePath);
    InputStream resourceStream = ResourceContentStarFile.class.getClassLoader().getResourceAsStream(resourceName);
    if(resourceStream == null) {
      // If we cannot find our package, try to see if it's a module (i.e. Module/__init__.star)
      @SuppressWarnings("UnstableApiUsage")
      String baseName = Files.getNameWithoutExtension(resourceName);
      String errorMsg = "Unable to find resource: " + resourceName;
      if(!baseName.equals("__init__")) {
        resourceName = resourceName.replace(
            baseName + ".star",
            baseName + "/__init__.star");
        resourceStream = ResourceContentStarFile.class.getClassLoader().getResourceAsStream(resourceName);
        errorMsg += " and additionally there was no module for " + resourceName + " found";
      }
      // resourceStream still null? ok, let's throw the exception..
      if(resourceStream == null) {
        throw new EvalException(errorMsg);
      }
    }
    try {
      return buildStarFile(resourceName, resourceStream);
    } catch (IOException e) {
      throw new EvalException(e);
    }
  }

  public static boolean startsWithPrefix(String moduleToLoad) {
    return moduleToLoad.startsWith(STDLIB) || moduleToLoad.startsWith(VENDOR) || moduleToLoad.startsWith(VGS);
  }

  /**
   * Splits a load label {@code @namespace//path} (or {@code @namespace/path}) into namespace and
   * path, or returns null if it has neither form.
   *
   * <p>Matches exactly what the regex {@code @(\w+)/?/(.+)} found (leftmost match, {@code \w} is
   * ASCII word characters, {@code .} stops at a newline), without a regex engine on every load;
   * ResourceContentStarFileTest compares the two.
   */
  @Nullable
  static String[] splitLabel(String label) {
    int n = label.length();
    for (int at = label.indexOf('@'); at >= 0; at = label.indexOf('@', at + 1)) {
      int end = at + 1;
      while (end < n && isWordChar(label.charAt(end))) {
        end++;
      }
      if (end == at + 1 || end >= n || label.charAt(end) != '/') {
        continue; // no namespace, or not followed by a slash
      }
      // "/?/" takes two slashes if a path character follows them, else one.
      int path = end + 1;
      if (path < n && label.charAt(path) == '/' && path + 1 < n && label.charAt(path + 1) != '\n') {
        path++;
      }
      if (path >= n || label.charAt(path) == '\n') {
        continue; // empty path
      }
      int newline = label.indexOf('\n', path);
      return new String[] {
        label.substring(at + 1, end), label.substring(path, newline < 0 ? n : newline)
      };
    }
    return null;
  }

  private static boolean isWordChar(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
  }

  public static String getModulePath(String moduleToLoad) {
    String[] label = splitLabel(moduleToLoad);
    if (label == null) {
      throw new RuntimeException("Could not find match for module: " + moduleToLoad);
    }
    return label[1]; // namespace/path <-- the path
  }

  public static String resolveResourceName(String moduleToLoad) {
    String[] label = splitLabel(moduleToLoad);
    String prefix;
    String modulePath;
    if (label == null) {
      // Could not find a module match or is incorrectly constructed
      // We default to a stdlib directory (unless we do not want this behavior?)
      prefix = STDLIB.replace("@", "");
      modulePath = moduleToLoad;
    } else {
      prefix = label[0];
      modulePath = label[1];
    }

    return String.format("%s/%s%s",
        prefix,
        modulePath,
        modulePath.endsWith(LarkyScript.STAR_EXTENSION) ? "" : LarkyScript.STAR_EXTENSION);
  }


  @Override
  public StarFile resolve(String path) {
    try {
      return buildStarFile(path);
    } catch (EvalException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String path() {
    return resourcePath;
  }

  @Override
  public byte[] readContentBytes() {
    return content;
  }

  @Override
  public String getIdentifier() {
    return resourcePath.replace(LarkyScript.STAR_EXTENSION, "");
  }


  @SneakyThrows
  @Nullable
  private Path getStdlibPath() {
    URL resourceUrl = this.getClass().getClassLoader()
        .getResource(STDLIB.replace("@", ""));
    assert resourceUrl != null;
    URI resourceAsURI;
    try {
      resourceAsURI = resourceUrl.toURI();
    } catch (URISyntaxException e) {
      return null;
    }

    return Paths.get(resourceAsURI);
  }

  @SuppressWarnings("UnstableApiUsage")
  private String withExtension(String moduleToLoad) {
    String nameWithoutExtension = Files.getNameWithoutExtension(moduleToLoad);
    String fname = Files.simplifyPath(nameWithoutExtension + LarkyScript.STAR_EXTENSION);
    return StarFile.ABSOLUTE_PREFIX + moduleToLoad.replace(nameWithoutExtension, fname);
  }

}
