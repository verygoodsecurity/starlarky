package com.verygood.security.larky.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.SneakyThrows;

public class InMemoryStarFile implements StarFile {

  private List<String> PRELOADER_PREFIXES = List.of(" ", "#", "load");
  private String content;

  public InMemoryStarFile(String scriptFile) {
    this("", scriptFile);
  }

  @SneakyThrows
  public InMemoryStarFile(String inputFile, String scriptFile) {
    List<String> input = !inputFile.equals("") ? Files.readAllLines(Path.of(inputFile)) : Collections.EMPTY_LIST;
    List<String> script = Files.readAllLines(Path.of(scriptFile));

    List<String> contentLines = new ArrayList<>();
    boolean loadBlockEnded = false;
    for (String scriptLine : script) {
      if (!loadBlockEnded) {
        if (PRELOADER_PREFIXES.stream().noneMatch(scriptLine::startsWith)) {
          loadBlockEnded = true;
          contentLines.addAll(input);
        }
      }
      contentLines.add(scriptLine);
    }

    this.content = String.join("\n", contentLines);
  }

  @SneakyThrows
  @Override
  public StarFile resolve(String path) {
    Path resolved = StarFile.isAbsolute(path)
        ? Path.of(path)
        : Path.of(getClass().getClassLoader().getResource(path).toURI());

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
