package com.verygood.security.run;

import com.google.common.base.Strings;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.Callable;

import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.CapturingConsole;
import com.verygood.security.larky.console.Console;
import com.verygood.security.larky.console.FileConsole;
import com.verygood.security.larky.console.LogConsole;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.LarkyScript.StarlarkMode;
import com.verygood.security.larky.parser.PrependMergedStarFile;
import com.verygood.security.run.repl.LarkyRepl;

import lombok.SneakyThrows;
import picocli.CommandLine;


@CommandLine.Command(
  name = "larky-runner",
  description = "Larky CLI Runner Application",
  aliases = {"larky"},
  header = "Larky Runner",
  footer = "(c) Very Good Security",
  mixinStandardHelpOptions = true,
  subcommands = {}
)
public class LarkyEntrypoint implements Callable<Integer> {

  @CommandLine.Option(names = {"-s", "--script"}, arity = "1", description = "Starlark script")
  private String filePath; // String script = readFile(commandLine.getOptionValue('s'));

  @CommandLine.Option(names = {"-i", "--input"}, arity = "1", description = "Input parameters")
  private String inputParams;
  // String input = commandLine.hasOption('i')
  // ? readFile(commandLine.getOptionValue('i'))
  // : "";

  @CommandLine.Option(names = {"-o", "--output"}, arity = "1", description = "Output parameters")
  private String outputPath; //String outputPath = commandLine.getOptionValue('o');

  @CommandLine.Option(names = {"-l", "--log"}, arity = "1", description = "Log output")
  private String logPath;
  //  String logPath = commandLine.hasOption('l') ?
  //      commandLine.getOptionValue('l')
  //      : "";
  @CommandLine.Option(names = {"-d", "--debug"}, description="Verbose merged script")
  private boolean debug; //  boolean debug = commandLine.hasOption("d");


  public static void main(String[] args) {
    if(args.length == 0) {
      Runtime.getRuntime().exit(LarkyRepl.runConsole());
    }
    int exitCode = new CommandLine(new LarkyEntrypoint()).execute(args);
    Runtime.getRuntime().exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    execute();
    return CommandLine.ExitCode.OK;
  }

  @SneakyThrows
  private void execute() {

    Path tempdir =  Paths.get(System.getProperty("java.io.tmpdir"), "larky-runner");
    String timestamp = String.valueOf(System.currentTimeMillis());
    String script = Strings.isNullOrEmpty(filePath) ? "" : readFile(filePath);
    String input = Strings.isNullOrEmpty(inputParams) ? "" : readFile(inputParams);

    if (!Files.exists(tempdir)) {
      Files.createDirectory(tempdir);
    }

    if (Strings.isNullOrEmpty(logPath)) {
      logPath = Files.createTempFile(tempdir, timestamp, ".log").toString();
    }

    if (Strings.isNullOrEmpty(outputPath)) {
      outputPath = Files.createTempFile(tempdir, timestamp, ".out").toString();
    }

    PrependMergedStarFile prependMergedStarFile = new PrependMergedStarFile(input, script);

    if(debug) {
      System.err.println("==================================");
      System.err.println(new String(prependMergedStarFile.readContentBytes()));
      System.err.println("==================================");
    }

    Console console = new FileConsole(CapturingConsole.captureAllConsole(
        LogConsole.writeOnlyConsole(System.out, true)), Path.of(logPath), Duration.ZERO);

    String output =
      new LarkyScript(StarlarkMode.STRICT)
        .executeSkylarkWithOutput(
          prependMergedStarFile,
          new ModuleSupplier().create(),
          console
        ).toString();

    if (debug) {
      System.err.println(output);
    }

    try(FileWriter writer = new FileWriter(Path.of(outputPath).toFile())) {
        writer.write(output);
    }
    catch(IOException e){
      // nosemgrep: java.lang.security.audit.active-debug-code-printstacktrace.active-debug-code-printstacktrace
      e.printStackTrace(System.err);
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private static String readFile(String filePath) {
    try {
      return filePath.trim().isEmpty() ? "" : Files.readString(Paths.get(filePath));
    } catch (IOException e) {
      System.err.println("Input file path is incorrect!");
      return "";
    }
  }
}
