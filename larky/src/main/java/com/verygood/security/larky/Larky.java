package com.verygood.security.larky;


import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.annotations.VisibleForTesting;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.LarkyParserInputUtils;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.SyntaxError;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

public class Larky {

  private static final String START_PROMPT = ">> ";
  private static final String CONTINUATION_PROMPT = ".. ";

  private static final BufferedReader reader =
      new BufferedReader(new InputStreamReader(System.in, UTF_8));
  private static final StarlarkThread thread;
  private static final Module module = Module.create();

  // TODO(adonovan): set load-binds-globally option when we support load,
  // so that loads bound in one REPL chunk are visible in the next.
  private static final FileOptions OPTIONS = FileOptions.DEFAULT;

  static {
    Mutability mu = Mutability.create("interpreter");
    thread = new StarlarkThread(mu, StarlarkSemantics.DEFAULT);
    thread.setPrintHandler((th, msg) -> System.out.println(msg));
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

  /**
   * Provide a REPL evaluating Starlark code.
   */
  @SuppressWarnings("CatchAndPrintStackTrace")
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


  static int execute(ParserInput input, String inputFile, String outputFile) {
    try {
      if (!inputFile.equals("")) {
        input = LarkyParserInputUtils.preAppend(
            ParserInput.readFile(inputFile),
            input);
      }
      Object returnValue = Starlark.execFile(input, OPTIONS, module, thread);
      if (!outputFile.equals("") && !(returnValue instanceof NoneType)) {
        writeOutput(outputFile, (StarlarkValue) returnValue);
      }
      return 0;
    } catch (SyntaxError.Exception ex) {
      for (SyntaxError error : ex.errors()) {
        System.err.println(error);
      }
      return 1;
    } catch (EvalException ex) {
      System.err.println(ex.getMessageWithStack());
      return 1;
    } catch (InterruptedException ex) {
      System.err.println("Interrupted");
      return 1;
    } catch (IOException ex) {
      System.err.println(ex.toString());
      return 1;
    }
  }

  /**
   * Execute a Starlark file.
   */
  @VisibleForTesting
  static int execute(ParserInput input) {
    return execute(input, "", "");
  }

  static void writeOutput(String outputFile, StarlarkValue returnValue) throws IOException {
    try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(outputFile),
                                                     Charset.defaultCharset(),
                                                     StandardOpenOption.CREATE)) {
      bw.write(returnValue.toString());
    }
  }

  public static void main(String[] args) throws Exception {
    String file = null;
    String cmd = null;
    String cpuprofile = null;
    String inputFile = "";
    String outputFile = "";

    // TODO: do a normal argument parsing
    // parse flags
    int i;
    for (i = 0; i < args.length; i++) {
      if (!args[i].startsWith("-")) {
        break;
      }
      if (args[i].equals("--")) {
        i++;
        break;
      }
      if (args[i].equals("-c")) {
        if (i + 1 == args.length) {
          throw new IOException("-c <cmd> flag needs an argument");
        }
        cmd = args[++i];
      } else if (args[i].equals("-cpuprofile")) {
        if (i + 1 == args.length) {
          throw new IOException("-cpuprofile <file> flag needs an argument");
        }
        cpuprofile = args[++i];
      } else if (args[i].equals("-input")) {
        if (i + 1 == args.length) {
          throw new IOException("-input <file> flag needs an argument");
        }
        inputFile = args[++i];
      } else if (args[i].equals("-output")) {
        if (i + 1 == args.length) {
          throw new IOException("-output <file> flag needs an argument");
        }
        outputFile = args[++i];
      } else {
        throw new IOException("unknown flag: " + args[i]);
      }
    }
    // positional arguments
    if (i < args.length) {
      if (i + 1 < args.length) {
        throw new IOException("too many positional arguments");
      }
      file = args[i];
    }

    if (cpuprofile != null) {
      FileOutputStream out = new FileOutputStream(cpuprofile);
      Starlark.startCpuProfile(out, Duration.ofMillis(10));
    }

    int exit;
    if (file == null) {
      if (cmd != null) {
        exit = execute(ParserInput.fromString(cmd, "<command-line>"));
      } else {
        readEvalPrintLoop();
        exit = 0;
      }
    } else if (cmd == null) {
      try {
        exit = execute(ParserInput.readFile(file), inputFile, outputFile);
      } catch (IOException e) {
        // This results in such lame error messages as:
        // "Error reading a.star: java.nio.file.NoSuchFileException: a.star"
        System.err.format("Error reading %s: %s\n", file, e);
        exit = 1;
      }
    } else {
      System.err.println("usage: Starlark [-cpuprofile file] [-c cmd | file]");
      exit = 1;
    }

    if (cpuprofile != null) {
      Starlark.stopCpuProfile();
    }

    System.exit(exit);
  }
}
