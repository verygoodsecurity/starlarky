/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.run.repl;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import javax.annotation.Nullable;
import org.jline.terminal.Terminal;

/**
 * Larky's interactive interpreter. Each input runs in one persistent module with the environment
 * of a Larky script (see {@link ReplSession}); an input is complete when it parses, or once a
 * block is closed by a blank line (see {@link net.starlark.java.syntax.InteractiveInput}).
 *
 * <p>Each input runs on its own thread, so that Ctrl-C ({@link #interruptEvaluation}) interrupts
 * it without stopping the REPL.
 */
public final class LarkyRepl {

  private static final String HISTORY_FILE = ".larky_history";

  // Evaluation threads get a large stack: deeply nested data (e.g. json) recurses in Java.
  private static final long EVAL_STACK_SIZE = 64L << 20;

  static final String HELP =
      String.join(
          "\n",
          "Type Larky (Starlark) code to run it; the value of an expression is printed.",
          "A block (def, if, for) ends at a blank line. Larky's modules load as in a script,",
          "e.g. load(\"@stdlib//json\", \"json\").",
          "",
          "Commands:",
          "  :help          show this help",
          "  :load <file>   run a .star file in this session",
          "  :reset         forget every binding and start over",
          "  :quit          exit (or Ctrl-D)",
          "",
          "Tab completes names, attributes (json.<Tab>) and load() labels.",
          "Ctrl-C cancels the input being typed, or interrupts a running evaluation.");

  private final ReplSession session;
  private final ReplInput input;
  private final PrintWriter out;
  private final PrintWriter err;
  private final boolean interactive;

  private volatile Thread running;
  private boolean failed;

  public LarkyRepl(
      ReplSession session, ReplInput input, PrintWriter out, PrintWriter err, boolean interactive) {
    this.session = session;
    this.input = input;
    this.out = out;
    this.err = err;
    this.interactive = interactive;
  }

  /**
   * Runs the REPL on this process's standard streams: with a line editor when standard input is a
   * terminal, otherwise reading plain lines, without prompts when not on a console (e.g. {@code
   * echo 'print(1)' | larky-runner}). Returns the exit status: non-zero if a non-interactive
   * input failed.
   */
  public static int runConsole() {
    Terminal terminal = JLineInput.systemTerminal();
    PrintWriter err = new PrintWriter(new OutputStreamWriter(System.err, UTF_8), true);
    if (terminal == null) {
      // No line editor: prompts only on an interactive console (e.g. TERM=dumb).
      boolean console = System.console() != null;
      PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, UTF_8), true);
      ReplSession session = new ReplSession(out, err);
      ReplInput input =
          new ReplInput.Plain(
              new BufferedReader(new InputStreamReader(System.in, UTF_8)),
              console ? out : null,
              session::isComplete);
      return new LarkyRepl(session, input, out, err, console).run();
    }
    PrintWriter out = terminal.writer();
    ReplSession session = new ReplSession(out, err);
    LarkyRepl repl =
        new LarkyRepl(session, new JLineInput(terminal, session, historyFile()), out, err, true);
    // While an input runs (JLine handles Ctrl-C itself while reading), Ctrl-C interrupts it.
    terminal.handle(Terminal.Signal.INT, signal -> repl.interruptEvaluation());
    return repl.run();
  }

  @Nullable
  private static Path historyFile() {
    try {
      String home = System.getProperty("user.home");
      return home == null || home.isEmpty() ? null : Path.of(home, HISTORY_FILE);
    } catch (RuntimeException e) {
      return null;
    }
  }

  /** Reads and runs inputs until the end of the input or :quit. Returns the exit status. */
  public int run() {
    if (interactive) {
      out.println("Larky interactive interpreter. Type :help for help, :quit or Ctrl-D to exit.");
      out.flush();
    }
    try (input) {
      while (true) {
        String text;
        try {
          text = input.read();
        } catch (ReplInput.CancelledException e) {
          err.println("KeyboardInterrupt");
          err.flush();
          continue;
        } catch (IOException e) {
          err.println("error reading input: " + e.getMessage());
          err.flush();
          return 1;
        }
        if (text == null) {
          break;
        }
        if (!handle(text)) {
          break;
        }
      }
    }
    if (interactive) {
      out.println();
      out.flush();
    }
    return failed && !interactive ? 1 : 0;
  }

  /** Runs one input; returns false to exit. */
  boolean handle(String text) {
    String trimmed = text.strip();
    if (trimmed.isEmpty()) {
      return true;
    }
    if (trimmed.startsWith(":") && !trimmed.contains("\n")) {
      return command(trimmed);
    }
    record(evaluate(() -> session.eval(text)));
    return true;
  }

  private boolean command(String line) {
    int space = line.indexOf(' ');
    String name = space < 0 ? line : line.substring(0, space);
    String arg = space < 0 ? "" : line.substring(space + 1).strip();
    switch (name) {
      case ":help", ":h", ":?" -> out.println(HELP);
      case ":quit", ":q", ":exit" -> {
        return false;
      }
      case ":reset" -> {
        session.reset();
        out.println("Session reset.");
      }
      case ":load", ":l" -> {
        if (arg.isEmpty()) {
          err.println("usage: :load <file>");
          failed = true;
        } else {
          record(evaluate(() -> session.execFile(Path.of(arg))));
        }
      }
      default -> {
        err.println("Unknown command " + name + "; type :help for the commands.");
        failed = true;
      }
    }
    out.flush();
    err.flush();
    return true;
  }

  private void record(boolean ok) {
    failed |= !ok;
  }

  /** Something to run on an evaluation thread. */
  interface Evaluation {
    boolean run() throws InterruptedException;
  }

  /**
   * Runs {@code evaluation} on its own thread and waits for it; {@link #interruptEvaluation}
   * interrupts it. Returns whether it succeeded.
   */
  boolean evaluate(Evaluation evaluation) {
    boolean[] ok = {false};
    Thread thread =
        new Thread(
            null,
            () -> {
              try {
                ok[0] = evaluation.run();
              } catch (InterruptedException e) {
                err.println("KeyboardInterrupt");
              } catch (StackOverflowError e) {
                err.println("Error: maximum recursion depth exceeded");
              } catch (RuntimeException | Error e) {
                // A bug in Larky rather than in the code typed: report it without a Java trace.
                err.println("Internal error: " + e);
              } finally {
                err.flush();
                out.flush();
              }
            },
            "larky-repl-eval",
            EVAL_STACK_SIZE);
    running = thread;
    thread.start();
    // An interrupt of this thread is passed on to the evaluation, then restored once it ends.
    boolean interrupted = false;
    try {
      while (true) {
        try {
          thread.join();
          break;
        } catch (InterruptedException e) {
          interrupted = true;
          thread.interrupt();
        }
      }
    } finally {
      running = null;
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
    return ok[0];
  }

  /** Interrupts the running evaluation, if any (Ctrl-C). */
  public void interruptEvaluation() {
    Thread thread = running;
    if (thread != null) {
      thread.interrupt();
    }
  }

  /** Whether an evaluation is running. */
  boolean isEvaluating() {
    return running != null;
  }
}
