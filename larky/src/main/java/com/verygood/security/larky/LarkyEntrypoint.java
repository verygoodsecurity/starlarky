package com.verygood.security.larky;

import com.google.common.collect.ImmutableSet;
import com.google.devtools.common.options.OptionsParser;

import com.verygood.security.larky.console.CapturingConsole;
import com.verygood.security.larky.console.Console;
import com.verygood.security.larky.console.FileConsole;
import com.verygood.security.larky.console.LogConsole;
import com.verygood.security.larky.nativelib.LarkyGlobals;
import com.verygood.security.larky.parser.CompilerOptions;
import com.verygood.security.larky.parser.InMemoryStarFile;
import com.verygood.security.larky.parser.LarkyParser;
import com.verygood.security.larky.parser.StarFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

import lombok.SneakyThrows;

public class LarkyEntrypoint {

  private static final ModuleSet moduleSet = new ModuleSet(new ModuleSupplier().create().getModules());

  @SneakyThrows
  public static void main(String[] args) {
    CompilerOptions compilerOptions = parseOptions(args);

    if (compilerOptions.input.isBlank()
        || compilerOptions.output.isBlank()
        || compilerOptions.script.isBlank()
        || compilerOptions.log.isBlank())
      return;

    LarkyParser parser = new LarkyParser(
        ImmutableSet.of(LarkyGlobals.class),
        LarkyParser.StarlarkMode.STRICT);

    StarFile starFile = new InMemoryStarFile(compilerOptions.input, compilerOptions.script);

    Console console = new FileConsole(CapturingConsole.captureAllConsole(
        LogConsole.writeOnlyConsole(System.out, true)), Path.of(compilerOptions.log), Duration.ZERO);

    Files.writeString(Path.of(compilerOptions.output),
        (String) parser.executeSkylarkWithOutput(starFile, moduleSet, console),
        StandardOpenOption.CREATE_NEW);
  }

  private static CompilerOptions parseOptions(String... args) {
    OptionsParser parser = OptionsParser.newOptionsParser(CompilerOptions.class);
    parser.parseAndExitUponError(args);
    return parser.getOptions(CompilerOptions.class);
  }

}
