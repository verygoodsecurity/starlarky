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

import com.google.common.collect.ImmutableList;
import com.verygood.security.larky.ModuleSupplier;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;

/**
 * Tab completion for the REPL: global names (the session's globals, the predeclared environment
 * and Starlark's universe), the members ({@code dir()}) of a dotted name such as {@code json.du},
 * the labels of Larky's modules in {@code load("...")} and the names a module exports in {@code
 * load("@stdlib//json", "...")}.
 */
final class ReplCompleter {

  /** Where the completed word starts in the buffer, and its candidate replacements. */
  record Result(int start, List<String> candidates) {}

  private static final Pattern LOAD_LABEL =
      Pattern.compile("(?:^|[^\\w.])load\\s*\\(\\s*[\"']([^\"'\\n]*)$");

  private static final Pattern LOAD_SYMBOL =
      Pattern.compile(
          "(?:^|[^\\w.])load\\s*\\(\\s*[\"']([^\"'\\n]+)[\"']\\s*,"
              + "(?:\\s*(?:\\w+\\s*=\\s*)?[\"'][^\"'\\n]*[\"']\\s*,)*"
              + "\\s*(?:\\w+\\s*=\\s*)?[\"']([^\"'\\n]*)$");

  static final ImmutableList<String> KEYWORDS =
      ImmutableList.of(
          "False", "None", "True", "and", "break", "continue", "def", "elif", "else", "for", "if",
          "in", "lambda", "load", "not", "or", "pass", "return");

  // Steps a completion may spend evaluating getattr()/dir(), which may run Larky code.
  private static final long MAX_STEPS = 100_000;

  private static volatile List<String> resourceLabels;

  private ReplCompleter() {}

  /** Where the word being completed at {@code cursor} starts. */
  static int wordStart(String buffer, int cursor) {
    String before = buffer.substring(0, cursor);
    Matcher m = LOAD_SYMBOL.matcher(before);
    if (m.find()) {
      return cursor - m.group(2).length();
    }
    m = LOAD_LABEL.matcher(before);
    if (m.find()) {
      return cursor - m.group(1).length();
    }
    int i = cursor;
    while (i > 0 && isNameChar(before.charAt(i - 1))) {
      i--;
    }
    return i;
  }

  static Result complete(ReplSession session, String buffer, int cursor) {
    String before = buffer.substring(0, cursor);
    Matcher m = LOAD_SYMBOL.matcher(before);
    if (m.find()) {
      return result(cursor, m.group(2), exportedNames(session, m.group(1)));
    }
    m = LOAD_LABEL.matcher(before);
    if (m.find()) {
      return result(cursor, m.group(1), loadLabels(session));
    }
    int start = wordStart(buffer, cursor);
    String word = before.substring(start);
    if (!word.isEmpty() && !Character.isJavaIdentifierStart(word.charAt(0))) {
      return new Result(start, List.of()); // e.g. a number
    }
    int dot = word.lastIndexOf('.');
    if (dot < 0) {
      return result(cursor, word, globalNames(session));
    }
    Object base = resolve(session, word.substring(0, dot));
    if (base == null) {
      return new Result(start, List.of());
    }
    String qualifier = word.substring(0, dot + 1);
    List<String> candidates = new ArrayList<>();
    for (String name : filter(members(session, base), word.substring(dot + 1))) {
      candidates.add(qualifier + name);
    }
    return new Result(start, candidates);
  }

  private static Result result(int cursor, String prefix, Collection<String> names) {
    return new Result(cursor - prefix.length(), filter(names, prefix));
  }

  // The names starting with prefix, sorted; private (_) names only for a prefix starting with _.
  private static List<String> filter(Collection<String> names, String prefix) {
    TreeSet<String> matches = new TreeSet<>();
    for (String name : names) {
      if (name.startsWith(prefix) && (prefix.startsWith("_") || !name.startsWith("_"))) {
        matches.add(name);
      }
    }
    return new ArrayList<>(matches);
  }

  private static boolean isNameChar(char c) {
    return Character.isLetterOrDigit(c) || c == '_' || c == '.';
  }

  private static Set<String> globalNames(ReplSession session) {
    Set<String> names = new TreeSet<>(KEYWORDS);
    names.addAll(Starlark.UNIVERSE.keySet());
    names.addAll(session.module().getPredeclaredBindings().keySet());
    names.addAll(session.module().getGlobals().keySet());
    return names;
  }

  // The value of a dotted name, or null. Attributes are read with the script's getattr().
  private static Object resolve(ReplSession session, String dotted) {
    String[] parts = dotted.split("\\.", -1);
    Object value = session.module().getGlobals().get(parts[0]);
    if (value == null) {
      value = session.builtin(parts[0]);
    }
    if (value == null || parts.length == 1) {
      return value;
    }
    Object getattr = session.builtin("getattr");
    StarlarkThread thread = boundedThread(session);
    try {
      for (int i = 1; i < parts.length && value != null; i++) {
        Object attr = Starlark.call(thread, getattr, List.of(value, parts[i], Starlark.NONE), Map.of());
        value = attr == Starlark.NONE ? null : attr;
      }
      return value;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  // The names dir(value) lists.
  private static List<String> members(ReplSession session, Object value) {
    List<String> names = new ArrayList<>();
    try {
      Object dir = Starlark.call(boundedThread(session), session.builtin("dir"), List.of(value), Map.of());
      for (Object name : Starlark.toIterable(dir)) {
        if (name instanceof String s) {
          names.add(s);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      // no members
    }
    return names;
  }

  private static StarlarkThread boundedThread(ReplSession session) {
    StarlarkThread thread = StarlarkThread.createTransient(session.mutability(), session.semantics());
    thread.setMaxExecutionSteps(MAX_STEPS);
    thread.setPrintHandler((th, msg) -> {});
    return thread;
  }

  // The global names of the module a load() label names (loading it, as the load would).
  private static Set<String> exportedNames(ReplSession session, String label) {
    try {
      Module module =
          session.evaluator().processLoads(session.stdinFile(), List.of(label)).get(label);
      return module == null ? Set.of() : module.getGlobals().keySet();
    } catch (Exception e) {
      return Set.of();
    }
  }

  /** The labels of Larky's native modules and of its .star modules (@stdlib, @vendor, @vgs). */
  static Set<String> loadLabels(ReplSession session) {
    Set<String> labels = new TreeSet<>();
    for (Map.Entry<String, Object> e :
        session.evaluator().getModuleSet().getModules().entrySet()) {
      labels.add("@" + ModuleSupplier.namespaceOf(e.getValue()) + "//" + e.getKey());
    }
    labels.addAll(resourceLabels());
    return labels;
  }

  private static List<String> resourceLabels() {
    List<String> labels = resourceLabels;
    if (labels == null) {
      labels = findResourceLabels();
      resourceLabels = labels;
    }
    return labels;
  }

  // Best effort: lists the .star resources under stdlib/, vendor/ and vgs/, whether they are in a
  // directory, a jar, or a native image's resources.
  private static List<String> findResourceLabels() {
    TreeSet<String> labels = new TreeSet<>();
    ClassLoader loader = ReplCompleter.class.getClassLoader();
    if (loader == null) {
      loader = ClassLoader.getSystemClassLoader();
    }
    for (String root : List.of("stdlib", "vendor", "vgs")) {
      List<String> names = new ArrayList<>();
      try {
        Enumeration<URL> urls = loader.getResources(root);
        while (urls.hasMoreElements()) {
          listResources(urls.nextElement(), root, names);
        }
      } catch (IOException | RuntimeException e) {
        // skip this root
      }
      for (String name : names) {
        String label = toLabel(root, name);
        if (label != null) {
          labels.add(label);
        }
      }
    }
    return List.copyOf(labels);
  }

  private static void listResources(URL url, String root, List<String> names) {
    try {
      switch (url.getProtocol()) {
        case "file" -> {
          Path dir = Path.of(url.toURI());
          try (Stream<Path> files = Files.walk(dir)) {
            files.forEach(p -> names.add(root + "/" + dir.relativize(p).toString().replace('\\', '/')));
          }
        }
        case "jar" -> {
          JarURLConnection connection = (JarURLConnection) url.openConnection();
          connection.setUseCaches(false);
          try (JarFile jar = connection.getJarFile()) {
            for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
              String name = e.nextElement().getName();
              if (name.startsWith(root + "/")) {
                names.add(name);
              }
            }
          }
        }
        default -> {
          // A native image's resources ("resource:" URLs) form a file system.
          URI uri = URI.create("resource:/");
          FileSystem fs;
          try {
            fs = FileSystems.newFileSystem(uri, Map.of());
          } catch (FileSystemAlreadyExistsException e) {
            fs = FileSystems.getFileSystem(uri);
          }
          Path dir = fs.getPath(root);
          try (Stream<Path> files = Files.walk(dir)) {
            files.forEach(p -> names.add(root + "/" + dir.relativize(p)));
          }
        }
      }
    } catch (Exception e) {
      // best effort
    }
  }

  // stdlib/foo/bar.star -> @stdlib//foo/bar; stdlib/foo/__init__.star -> @stdlib//foo
  private static String toLabel(String root, String resource) {
    if (!resource.endsWith(".star")) {
      return null;
    }
    String path = resource.substring(root.length() + 1, resource.length() - ".star".length());
    if (path.endsWith("/__init__")) {
      path = path.substring(0, path.length() - "/__init__".length());
    }
    if (path.isEmpty() || path.equals("__init__")) {
      return null;
    }
    return "@" + root + "//" + path;
  }
}
