package com.verygood.security.run;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Strings;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.SyntaxError;

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

  //REPL
  private static final String START_PROMPT = ">> ";
  private static final String CONTINUATION_PROMPT = ".. ";
  private static final FileOptions OPTIONS = FileOptions.DEFAULT;
  private static final BufferedReader reader =
      new BufferedReader(new InputStreamReader(System.in, UTF_8));
  private static final StarlarkThread thread;
  private static final Module module = Module.create();

  static {
    Mutability mu = Mutability.create("interpreter");
    thread = new StarlarkThread(mu, StarlarkSemantics.DEFAULT);
    thread.setPrintHandler((th, msg) -> System.out.println(msg));
  }
  //REPL end

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
      readEvalPrintLoop();
      Runtime.getRuntime().exit(0);
    }
    int exitCode = new CommandLine(new LarkyEntrypoint()).execute(args);
    Runtime.getRuntime().exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    if (Strings.isNullOrEmpty(filePath)
          || Strings.isNullOrEmpty(outputPath)
          || Strings.isNullOrEmpty(logPath)) {
      new CommandLine(new LarkyEntrypoint()).usage(System.out);
      return CommandLine.ExitCode.SOFTWARE;
      //System.out.println("Usage: larky-runer -s script_file -o output_file -l log_file -i input_param_file");
    }

    execute();
    return CommandLine.ExitCode.OK;
  }

  @SneakyThrows
  private void execute() {

    String script = readFile(filePath);
    String input = readFile(inputParams);

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
      e.printStackTrace(System.err);
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private static void readEvalPrintLoop() {
    System.err.println("Welcome to Starlark (java.starlark.net)");
    String line;

    // TODO(adonovan): parse a compound statement, like the Python and
    // go.starlark.net REPLs. This requires a new grammar production, and
    // integration with the lexer so that it consumes new
    // lines only until the parse is complete.

    while ((line = prompt()) != null) {
      ParserInput input = ParserInput.fromString(line, "<stdin>");
      try {
        Object result = Starlark.execFile(input, OPTIONS, module, thread);
        if (result != Starlark.NONE) {
          System.out.println(Starlark.repr(result));
        }
      } catch (SyntaxError.Exception ex) {
        for (SyntaxError error : ex.errors()) {
          System.err.println(error);
        }
      } catch (EvalException ex) {
        // TODO(adonovan): provide a SourceReader. Requires that we buffer the
        // entire history so that line numbers don't reset in each chunk.
        System.err.println(ex.getMessageWithStack());
      } catch (InterruptedException ex) {
        System.err.println("Interrupted");
      }
    }
  }

  private static String prompt() {
    StringBuilder input = new StringBuilder();
    System.out.print(START_PROMPT);
    try {
      String lineSeparator = "";
      while (true) {
        String line = reader.readLine();
        if (line == null) {
          return null;
        }
        if (line.isEmpty()) {
          return input.toString();
        }
        input.append(lineSeparator).append(line);
        lineSeparator = "\n";
        System.out.print(CONTINUATION_PROMPT);
      }
    } catch (IOException e) {
      System.err.format("Error reading line: %s\n", e);
      return null;
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
