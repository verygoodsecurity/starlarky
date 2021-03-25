package com.verygood.security.larky.parser;

import com.google.common.io.Files;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;

import net.starlark.java.eval.EvalException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
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
  private static final Pattern NAMESPACE_PREFIX = Pattern.compile("@(\\w+)/?/(.+)");

  private String resourcePath;
  private byte[] content;

  private ResourceContentStarFile(String resourcePath, byte[] content) {
    this.resourcePath = resourcePath;
    this.content = content;
  }

  public static ResourceContentStarFile buildStarFile(String resourcePath, InputStream inputStream) throws IOException {
    return new ResourceContentStarFile(resourcePath,
        String.join("\n", IOUtils.readLines(inputStream, Charset.defaultCharset())).getBytes());
  }

  public static ResourceContentStarFile buildStarFile(String resourcePath) throws EvalException {
    String resourceName = resolveResourceName(resourcePath);
    InputStream resourceStream = ResourceContentStarFile.class.getClassLoader().getResourceAsStream(resourceName);
    if(resourceStream == null) {
      // If we cannot find our package, try to see if it's a module (i.e. Module/__init__.star)
      String baseName = FilenameUtils.getBaseName(resourceName);
      String errorMsg = "Unable to find resource: " + resourceName;
      if(!baseName.equals("__init__")) {
        String newRn = resourceName.replace(
            baseName + ".star",
            baseName + "/__init__.star");
        resourceStream = ResourceContentStarFile.class.getClassLoader().getResourceAsStream(newRn);
        errorMsg += " and additionally there was no module for " + newRn + " found";
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
    return moduleToLoad.startsWith(STDLIB) || moduleToLoad.startsWith(VENDOR);
  }

  public static String getModulePath(String moduleToLoad) {
    Matcher m = NAMESPACE_PREFIX.matcher(moduleToLoad);
    if(!m.find()) {
      throw new RuntimeException("Could not find match for module: " + moduleToLoad);
    }
    assert m.groupCount() == 2;
    return m.group(2); // this is 1 --> namespace/path <-- this is 2
  }

  public static String resolveResourceName(String moduleToLoad) {
    Matcher m = NAMESPACE_PREFIX.matcher(moduleToLoad);
    String prefix;
    String modulePath;
    if(!m.find() || m.groupCount() != 2) {
      // Could not find a module match or is incorrectly constructed
      // We default to a stdlib directory (unless we do not want this behavior?)
      prefix = STDLIB.replace("@", "");
      modulePath = moduleToLoad;
      // throw new RuntimeException("Could not find match for module: " + moduleToLoad);
    } else {
      prefix = m.group(1);
      modulePath = m.group(2);
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
