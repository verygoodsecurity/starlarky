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
import com.verygood.security.larky.parser.EvaluationMetadata;
import com.verygood.security.larky.parser.EvaluationSettings;
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

  @CommandLine.Option(
      names = {"--steps"},
      description = "Print the run's steps, outcome and engine to stderr, as a host reads them")
  private boolean steps;

  @CommandLine.Option(
      names = {"--step-limit"},
      arity = "1",
      description = "Stop the run after this many steps, as a host's step limit does")
  private Long stepLimit;

  @CommandLine.Option(
      names = {"--no-charge-library-init"},
      description = "Do not count initializing Larky's own modules against --step-limit")
  private boolean noChargeLibraryInit;


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

    EvaluationSettings.Builder settings =
        EvaluationSettings.builder().chargeLibraryInit(!noChargeLibraryInit);
    if (stepLimit != null) {
      settings.stepLimit(stepLimit);
    }
    LarkyScript larkyScript =
        new LarkyScript(
            ModuleSupplier.CORE_MODULES,
            StarlarkMode.STRICT,
            java.util.Map.of(),
            settings.build());

    // through the evaluation's HostValues: a struct's toString runs its __repr__ (Starlark code)
    String output =
      larkyScript
        .executeSkylarkWithOutputAsString(
          prependMergedStarFile,
          new ModuleSupplier().create(),
          console,
          metadata -> {
            if (steps) {
              System.err.println(describe(metadata));
              describeUncached(metadata).forEach(System.err::println);
            }
          }
        );

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

  // One line, for people and scripts: the same numbers a host reads from EvaluationMetadata.
  static String describe(EvaluationMetadata m) {
    return "larky: outcome=" + m.outcome()
        + " steps=" + m.executedSteps()
        + " library_init_steps=" + m.libraryInitSteps()
        + " engine=" + m.engine()
        + " elapsed_ms=" + m.elapsedNanos() / 1_000_000;
  }

  // With the module cache on (the default), one line per Larky module the run
  // loaded that the cache did not share, and why: names only, as EvaluationMetadata gives them.
  static java.util.List<String> describeUncached(EvaluationMetadata m) {
    java.util.List<String> lines = new java.util.ArrayList<>();
    m.uncachedModules().forEach((label, why) -> lines.add("larky: uncached " + label + " " + why));
    return lines;
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
