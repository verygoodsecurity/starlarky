package com.verygood.security.larky.parser;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.SneakyThrows;

public class PrependMergedStarFile implements StarFile {

  private List<String> PRELOADER_PREFIXES = Lists.newArrayList(" ", "#", "load");
  private String content;

  public PrependMergedStarFile(String scriptFile) {
    this("", scriptFile);
  }

  @SneakyThrows
  public PrependMergedStarFile(String input, String script) {
    StringBuilder finalScript = new StringBuilder();

    boolean loadBlockEnded = false;

    for (String scriptLine : script.split("\n")) {
      if (!loadBlockEnded) {
        if (scriptLine.trim().length() == 0) continue;
        if (PRELOADER_PREFIXES.stream().noneMatch(scriptLine::startsWith)) {
          loadBlockEnded = true;
          finalScript.append(input).append("\n");
        }
      }
      finalScript.append(scriptLine).append("\n");
    }

    this.content = String.join("\n", finalScript.toString());
  }

  @SneakyThrows
  @Override
  public StarFile resolve(String path) {
    Path resolved = StarFile.isAbsolute(path)
        ? Paths.get(path)
        : Paths.get(getClass().getClassLoader().getResource(path).toURI());

    return new PathBasedStarFile(resolved, null, null);
  }

  @Override
  public String path() {
    return toString();
  }

  @Override
  public byte[] readContentBytes() throws IOException {
    return content.getBytes();
  }

  @Override
  public String getIdentifier() {
    return toString();
  }
}
