package com.verygood.security.larky.parser;

import com.verygood.security.larky.console.LogConsole;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.starlark.java.eval.CompiledModule;
import net.starlark.java.eval.Module;
import net.starlark.java.syntax.FileOptions;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.Program;
import net.starlark.java.syntax.StarlarkFile;

/**
 * Precompiles Larky's own modules at build time: for every {@code .star} file under {@code
 * stdlib/}, {@code vendor/} and {@code vgs/} in a classes directory, writes a {@link
 * CompiledModule} next to it ({@code .starc}). {@link ProgramCache} loads those instead of parsing
 * and resolving the source when bytecode is enabled.
 *
 * <p>Modules are resolved in the environment of a default {@link LarkyEvaluator}; at run time a
 * precompiled module is used only if it resolves the same way in the actual environment. A module
 * that fails to compile is skipped (its source is compiled at run time), and reported.
 *
 * <p>Usage: {@code LarkyPrecompiler <classes directory>}.
 */
public final class LarkyPrecompiler {

  static final String SOURCE_SUFFIX = ".star";
  static final String COMPILED_SUFFIX = ".starc";
  private static final String[] ROOTS = {"stdlib", "vendor", "vgs"};

  private LarkyPrecompiler() {}

  /** The resource name of the precompiled form of {@code sourceResource} ({@code x.star}). */
  static String compiledName(String sourceResource) {
    return sourceResource.substring(0, sourceResource.length() - SOURCE_SUFFIX.length())
        + COMPILED_SUFFIX;
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("usage: LarkyPrecompiler <classes directory>");
      System.exit(2);
    }
    Path classes = Path.of(args[0]);
    LarkyEvaluator evaluator =
        new LarkyEvaluator(
            new LarkyScript(LarkyScript.StarlarkMode.STRICT),
            LogConsole.writeOnlyConsole(System.err, false));
    FileOptions options = evaluator.getStarlarkValidationOptions();

    int written = 0;
    List<String> failed = new ArrayList<>();
    for (String root : ROOTS) {
      Path dir = classes.resolve(root);
      if (!Files.isDirectory(dir)) {
        continue;
      }
      List<Path> sources;
      try (Stream<Path> walk = Files.walk(dir)) {
        sources = walk.filter(p -> p.toString().endsWith(SOURCE_SUFFIX)).sorted().toList();
      }
      for (Path source : sources) {
        String resource = classes.relativize(source).toString().replace('\\', '/');
        try {
          byte[] bytes = Files.readAllBytes(source);
          Module env =
              Module.withPredeclared(evaluator.getLarkySemantics(), evaluator.getEnvironment());
          StarlarkFile file = StarlarkFile.parse(ParserInput.fromUTF8(bytes, resource), options);
          Program program = Program.compileFile(file, env, /* enableBytecode= */ true);
          CompiledModule compiled = CompiledModule.of(program, file, sha256(bytes));
          try (OutputStream out = Files.newOutputStream(source.resolveSibling(
              compiledName(source.getFileName().toString())))) {
            compiled.write(out);
          }
          written++;
        } catch (Exception | StackOverflowError e) {
          failed.add(resource + ": " + e.getMessage());
        }
      }
    }
    System.out.printf(
        "LarkyPrecompiler: wrote %d compiled modules, skipped %d%n", written, failed.size());
    for (String failure : failed) {
      System.out.println("  skipped " + failure);
    }
  }

  static byte[] sha256(byte[] bytes) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
