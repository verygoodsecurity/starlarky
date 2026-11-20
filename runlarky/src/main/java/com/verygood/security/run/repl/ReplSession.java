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

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.console.LogConsole;
import com.verygood.security.larky.modules.utils.Reporter;
import com.verygood.security.larky.parser.LarkyEvaluator;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.LarkyScript.StarlarkMode;
import com.verygood.security.larky.parser.PathBasedStarFile;
import com.verygood.security.larky.parser.StarFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.InteractiveInput;
import net.starlark.java.syntax.Location;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.Program;
import net.starlark.java.syntax.StarlarkFile;
import net.starlark.java.syntax.SyntaxError;

/**
 * The state of one REPL session: a single {@link Module} with the predeclared environment a Larky
 * script runs in (the runner's script mode, {@link LarkyScript} in strict mode), into which every
 * input is executed, and the session's source so that error tracebacks show the right lines.
 *
 * <p>Inputs are numbered as one file, {@code <stdin>}: the first line of each input continues the
 * line numbers of the previous ones. Not thread-safe: the caller runs one evaluation (or
 * completion) at a time.
 */
public final class ReplSession {

  /** The file name of the code typed at the prompt. */
  public static final String STDIN = "<stdin>";

  private final PrintWriter out;
  private final PrintWriter err;
  private final Path workingDirectory;

  // The session's source: every line typed at the prompt, and each file run with :load.
  private final List<String> stdinLines = new ArrayList<>();
  private final Map<String, String[]> fileLines = new HashMap<>();

  private LarkyEvaluator evaluator;
  private StarlarkSemantics semantics;
  private FileOptions options;
  private Module module;
  private Mutability mutability;

  public ReplSession(PrintWriter out, PrintWriter err) {
    this(out, err, Path.of("").toAbsolutePath());
  }

  public ReplSession(PrintWriter out, PrintWriter err, Path workingDirectory) {
    this.out = out;
    this.err = err;
    this.workingDirectory = workingDirectory;
    reset();
  }

  /** Discards every binding and the session's source, starting over with a fresh module. */
  public void reset() {
    if (mutability != null) {
      mutability.close();
    }
    LarkyScript script = new LarkyScript(StarlarkMode.STRICT);
    evaluator = new LarkyEvaluator(script, LogConsole.writeOnlyConsole(System.err, false));
    semantics = evaluator.getLarkySemantics();
    options = LarkyScript.scriptFileOptions(StarlarkMode.STRICT, semantics);
    module = Module.withPredeclared(semantics, evaluator.getEnvironment());
    // One mutability for the whole session: values stay mutable across inputs, as in Python.
    mutability = Mutability.create("larky-repl");
    stdinLines.clear();
    fileLines.clear();
  }

  /** The options inputs are parsed with. */
  public FileOptions fileOptions() {
    return options;
  }

  /** Whether {@code text} is a complete input (see {@link InteractiveInput#isComplete}). */
  public boolean isComplete(String text) {
    return InteractiveInput.isComplete(text, options);
  }

  Module module() {
    return module;
  }

  StarlarkSemantics semantics() {
    return semantics;
  }

  Mutability mutability() {
    return mutability;
  }

  LarkyEvaluator evaluator() {
    return evaluator;
  }

  ImmutableMap<String, Object> environment() {
    return evaluator.getEnvironment();
  }

  /** The script file code typed at the prompt is: load() labels resolve against the cwd. */
  StarFile stdinFile() {
    return new StdinStarFile(workingDirectory);
  }

  /**
   * Executes one complete input typed at the prompt, printing the value of a final expression
   * statement (unless None) and any error. Returns whether it succeeded.
   */
  public boolean eval(String input) throws InterruptedException {
    int firstLine = stdinLines.size() + 1;
    stdinLines.addAll(Arrays.asList(input.split("\n", -1)));
    // Pad with newlines so that locations are lines of the whole session.
    ParserInput parserInput = ParserInput.fromString("\n".repeat(firstLine - 1) + input, STDIN);
    return exec(parserInput, stdinFile(), /* echo= */ true);
  }

  /** Executes the file {@code path} into the session's module (the :load command). */
  public boolean execFile(Path path) throws InterruptedException {
    Path file = workingDirectory.resolve(path).toAbsolutePath().normalize();
    String text;
    try {
      text = Files.readString(file, UTF_8);
    } catch (IOException e) {
      err.println("cannot read " + file + ": " + e.getMessage());
      err.flush();
      return false;
    }
    fileLines.put(file.toString(), text.split("\n", -1));
    return exec(
        ParserInput.fromString(text, file.toString()),
        new PathBasedStarFile(file, null, null),
        /* echo= */ false);
  }

  private boolean exec(ParserInput input, StarFile content, boolean echo)
      throws InterruptedException {
    try {
      Program prog;
      try {
        prog = Program.compileFile(StarlarkFile.parse(input, options), module);
      } catch (SyntaxError.Exception e) {
        for (SyntaxError error : e.errors()) {
          err.println(error);
        }
        return false;
      }

      Map<String, Module> loaded;
      try {
        loaded = evaluator.processLoads(content, prog.getLoads());
      } catch (EvalException e) {
        err.println(e.getMessageWithStack(this::readSourceLine));
        return false;
      } catch (RuntimeException e) {
        // LarkyLoader reports a module that fails to load as a RuntimeException with the reason.
        err.println(e.getMessage());
        return false;
      }

      StarlarkThread thread = newThread();
      thread.setLoader(loaded::get);
      try {
        Object result = Starlark.execFileProgram(prog, module, thread);
        if (echo && result != Starlark.NONE) {
          out.println(repr(thread, result));
        }
        return true;
      } catch (EvalException e) {
        err.println(e.getMessageWithStack(this::readSourceLine));
        return false;
      }
    } finally {
      out.flush();
      err.flush();
    }
  }

  StarlarkThread newThread() {
    StarlarkThread thread = StarlarkThread.createTransient(mutability, semantics);
    thread.setPrintHandler(
        (th, msg) -> {
          out.println(msg);
          out.flush();
        });
    thread.setThreadLocal(Reporter.class, new Reporter(LogConsole.writeOnlyConsole(System.err, false)));
    return thread;
  }

  /** The value as the script's own repr() shows it. */
  private String repr(StarlarkThread thread, Object value) throws InterruptedException {
    Object repr = builtin("repr");
    if (repr != null) {
      try {
        Object s = Starlark.call(thread, repr, List.of(value), Map.of());
        if (s instanceof String str) {
          return str;
        }
      } catch (EvalException e) {
        // fall back to Starlark's own repr
      }
    }
    return Starlark.repr(value, semantics);
  }

  /** A predeclared or universal builtin, as a script sees it. */
  Object builtin(String name) {
    Object value = module.getPredeclared(name);
    return value != null ? value : Starlark.UNIVERSE.get(name);
  }

  /** The source line at {@code loc}: of the session, a :loaded file or one of Larky's modules. */
  String readSourceLine(Location loc) {
    int i = loc.line() - 1;
    if (STDIN.equals(loc.file())) {
      return i >= 0 && i < stdinLines.size() ? stdinLines.get(i) : null;
    }
    String[] lines = fileLines.computeIfAbsent(loc.file(), ReplSession::readResourceLines);
    return lines != null && i >= 0 && i < lines.length ? lines[i] : null;
  }

  // Larky's own modules are resources (e.g. stdlib/json.star); nothing else is read from disk.
  private static String[] readResourceLines(String name) {
    try (InputStream in = ReplSession.class.getResourceAsStream("/" + name)) {
      return in == null ? null : new String(in.readAllBytes(), UTF_8).split("\n", -1);
    } catch (IOException e) {
      return null;
    }
  }

  /** The script file of the prompt; relative load() labels resolve against the cwd. */
  private static final class StdinStarFile implements StarFile {
    private final Path workingDirectory;

    StdinStarFile(Path workingDirectory) {
      this.workingDirectory = workingDirectory;
    }

    @Override
    public StarFile resolve(String path) {
      return new PathBasedStarFile(workingDirectory.resolve(STDIN), null, null).resolve(path);
    }

    @Override
    public String path() {
      return STDIN;
    }

    @Override
    public byte[] readContentBytes() {
      return new byte[0];
    }

    @Override
    public String getIdentifier() {
      return STDIN;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof StdinStarFile s && Objects.equals(s.workingDirectory, workingDirectory);
    }

    @Override
    public int hashCode() {
      return workingDirectory.hashCode();
    }
  }
}
