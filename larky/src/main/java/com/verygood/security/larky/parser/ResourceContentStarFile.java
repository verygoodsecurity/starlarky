package com.verygood.security.larky.parser;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.Charset;

import lombok.SneakyThrows;

import static com.verygood.security.larky.parser.LarkyEvaluator.LarkyLoader.STDLIB;

public class ResourceContentStarFile implements StarFile {

  private String resourcePath;
  private byte[] content;

  private ResourceContentStarFile(String resourcePath, byte[] content) {
    this.resourcePath = resourcePath;
    this.content = content;
  }

  @SneakyThrows
  public static ResourceContentStarFile buildStarFile(String resourcePath, InputStream inputStream) {
    return new ResourceContentStarFile(resourcePath,
        String.join("\n", IOUtils.readLines(inputStream, Charset.defaultCharset())).getBytes());
  }

  @SneakyThrows
  public static ResourceContentStarFile buildStarFile(String resourcePath) {
    String resourceName = resolveResourceName(resourcePath);
    InputStream resourceStream = ResourceContentStarFile.class.getClassLoader().getResourceAsStream(resourceName);
    return buildStarFile(resourceName, resourceStream);
  }

  @Override
  public StarFile resolve(String path) {
    return buildStarFile(path);
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

  public static String resolveResourceName(String moduleName) {
    return String.format("%s/%s%s",
        STDLIB.replace("@", ""),
        moduleName,
        moduleName.endsWith(LarkyScript.STAR_EXTENSION) ? "" : LarkyScript.STAR_EXTENSION);
  }
}
