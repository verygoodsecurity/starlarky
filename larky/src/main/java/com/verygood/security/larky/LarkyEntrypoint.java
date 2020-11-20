package com.verygood.security.larky;

import com.verygood.security.larky.jsr223.LarkyScriptEngineFactory;
import com.verygood.security.larky.parser.PrependMergedStarFile;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.script.ScriptEngine;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import lombok.SneakyThrows;

//@QuarkusMain
//public class LarkyEntrypoint implements QuarkusApplication {
public class LarkyEntrypoint {

  private static final LarkyScriptEngineFactory larkyFactory = new LarkyScriptEngineFactory();

  @SneakyThrows
//  @Override
  public static void main(String[] args) {
//  public int run(String... args) {
    CommandLine commandLine = parseOptions(args);

    if (!commandLine.hasOption('s') || !commandLine.hasOption('o')) {
      System.out.println("Usage: larky-runer -s script_file -o output_file -l log_file -i input_param_file");
//      return 1;
    }

    String outputPath = commandLine.getOptionValue('o');
    String script = readFile(commandLine.getOptionValue('s'));

    String input = commandLine.hasOption('i') ?
        readFile(commandLine.getOptionValue('i'))
        : "";

    String log = commandLine.hasOption('l') ?
        readFile(commandLine.getOptionValue('l'))
        : "";

    final ScriptEngine scriptEngine = larkyFactory.getScriptEngine();
    final PrependMergedStarFile prependMergedStarFile = new PrependMergedStarFile(input, script);

    final Object eval = scriptEngine.eval(prependMergedStarFile.readContent());

    System.out.println(eval);
//    Console console = new FileConsole(CapturingConsole.captureAllConsole(
//        LogConsole.writeOnlyConsole(System.out, true)), Path.of(commandLine.getOptionValue('l')), Duration.ZERO);
//
//    Files.writeString(Path.of(commandLine.getOptionValue('o')),
//        (String) parser.executeSkylarkWithOutput(starFile, moduleSet, console),
//        StandardOpenOption.CREATE);

//    return 0;
  }

  private static CommandLine parseOptions(String... args) throws ParseException {
    Options options = new Options();
    options.addOption("s", "script", true, "Starlark script");
    options.addOption("i", "input", true, "Input parameters");
    options.addOption("o", "output", true, "Output parameters");
    options.addOption("l", "log", true, "Log output");
    CommandLineParser parser = new DefaultParser();
    return parser.parse(options, args);
  }

  private static String readFile(String filePath) {
    try {
      return filePath.trim().isEmpty()? "" : Files.readString(Paths.get(filePath));
    } catch (IOException e) {
      System.out.println("Input file path is incorrect!");
      return "";
    }
  }
}
