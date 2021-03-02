package com.verygood.security.larky.parser;

import static com.verygood.security.larky.parser.LarkyEvaluator.LarkyLoader.STDLIB;

import net.starlark.java.eval.EvalException;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class ResourceContentStarFile implements StarFile {

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
      throw new EvalException("Unable to find resource: " + resourceName);
    }
    try {
      return buildStarFile(resourceName, resourceStream);
    } catch (IOException e) {
      throw new EvalException(e);
    }
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

  public static String resolveResourceName(String moduleName) {
    return String.format("%s/%s%s",
        STDLIB.replace("@", ""),
        moduleName,
        moduleName.endsWith(LarkyScript.STAR_EXTENSION) ? "" : LarkyScript.STAR_EXTENSION);
  }
}
