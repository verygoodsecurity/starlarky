package com.verygood.security.run;

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
import net.starlark.java.syntax.Program;
import net.starlark.java.syntax.SyntaxError;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import lombok.SneakyThrows;

import static com.verygood.security.larky.ModuleSupplier.CORE_MODULES;
import static java.nio.charset.StandardCharsets.UTF_8;

@QuarkusMain
public class LarkyEntrypoint implements QuarkusApplication {

  //REPL
  private static final String START_PROMPT = ">> ";
  private static final String CONTINUATION_PROMPT = ".. ";
  private static final FileOptions OPTIONS = FileOptions.DEFAULT;
  private static final BufferedReader reader =
      new BufferedReader(new InputStreamReader(System.in, UTF_8));
  private static final StarlarkThread thread;

  private static ModuleSupplier.ModuleSet moduleSet;

  static {
    Module module = Module.create();
    moduleSet =
        new ModuleSupplier().modulesToVariableMap(false);
    moduleSet.getModules()
        .forEach(module::setGlobal);
    Mutability mu = Mutability.create("interpreter");
    thread = new StarlarkThread(mu, StarlarkSemantics.DEFAULT);
    thread.setPrintHandler((th, msg) -> System.out.println(msg));
    thread.setLoader((name) -> module);
  }
  //REPL end

  @SneakyThrows
  @Override
  public int run(String... args) {
    if (args.length == 0) {
      readEvalPrintLoop();
    } else {
      CommandLine commandLine = parseOptions(args);
      if (!commandLine.hasOption('s') || !commandLine.hasOption('o') || !commandLine.hasOption('l')) {
        System.out.println("Usage: larky-runer -s script_file -o output_file -l log_file -i input_param_file");
        return 1;
      }
      execute(commandLine);
    }

    return 0;
  }

  @SneakyThrows
  private static void execute(CommandLine commandLine) {
    String outputPath = commandLine.getOptionValue('o');
    String script = readFile(commandLine.getOptionValue('s'));

    String input = commandLine.hasOption('i') ?
        readFile(commandLine.getOptionValue('i'))
        : "";

    String logPath = commandLine.hasOption('l') ?
        commandLine.getOptionValue('l')
        : "";

    PrependMergedStarFile prependMergedStarFile = new PrependMergedStarFile(input, script);

    if (commandLine.hasOption("d")) {
      System.err.println("==================================");
      System.err.println(new String(prependMergedStarFile.readContentBytes()));
      System.err.println("==================================");
    }

    Console console = new FileConsole(CapturingConsole.captureAllConsole(
        LogConsole.writeOnlyConsole(System.out, true)), Path.of(logPath), Duration.ZERO);

    String output = new LarkyScript(StarlarkMode.STRICT)
        .executeSkylarkWithOutput(prependMergedStarFile,
            new ModuleSupplier().create(), console)
        .toString();

    Files.writeString(Path.of(outputPath), output, StandardOpenOption.CREATE);
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
        Object result = Starlark.execFile(input, OPTIONS, moduleSet.getModules(), thread);
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

  private static CommandLine parseOptions(String... args) throws ParseException {
    Options options = new Options();
    options.addOption("s", "script", true, "Starlark script");
    options.addOption("i", "input", true, "Input parameters");
    options.addOption("o", "output", true, "Output parameters");
    options.addOption("l", "log", true, "Log output");
    options.addOption("d", "debug", false, "Verbose merged script");
    CommandLineParser parser = new DefaultParser();
    return parser.parse(options, args);
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
