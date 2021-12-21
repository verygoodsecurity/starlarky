package com.verygood.security.run;

import com.verygood.security.mode.grpc.LarkyGrpcServer;
import com.verygood.security.mode.repl.ReplRunner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import lombok.SneakyThrows;

public class LarkyEntrypoint {

  private static final String MODE_FLAG = "m";
  private static final String DEBUG_FLAG = "d";
  public static final String DEFAULT_MODE = "REPL";

  enum Mode {
    REPL, GRPC
  }

  @SneakyThrows
  public static int run(String... args) {
    CommandLine commandLine = parseOptions(args);
    Mode mode = parseMode(commandLine.hasOption(MODE_FLAG) ?
        commandLine.getOptionValue(MODE_FLAG) : DEFAULT_MODE);
    if (mode == null) {
      printUsage();
      return 1;
    }
    switch (mode) {
      case REPL:
        ReplRunner runner = new ReplRunner();
        runner.readEvalPrintLoop(new String[]{});
        break;
      case GRPC:
        LarkyGrpcServer larkyServer = new LarkyGrpcServer(8080);
        larkyServer.serve();
        break;
    }
    return 0;
  }

  private static CommandLine parseOptions(String... args) throws ParseException {
    Options options = new Options();
    options.addOption(MODE_FLAG, "mode", true, "Executor mode");
    options.addOption(DEBUG_FLAG, "debug", false, "Run in debug mode");
    return new DefaultParser().parse(options, args);
  }

  private static void printUsage() {
    System.out.println("Usage: larky-runer -m [repl|grpc] [-d]\n" +
        "-m, --mode - executor mode, coulbe repl or grpc\n" +
        "-d, --debug - for debug mode");
  }

  private static Mode parseMode(String mode) {
    try {
      return Mode.valueOf(mode.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
