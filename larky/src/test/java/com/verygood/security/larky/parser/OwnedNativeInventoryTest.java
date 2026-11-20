/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.larky.parser;

import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.testing.TestingConsole;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Measurement only ({@code -Dlarky.ownerInventory=true -Dlarky.ownerInventory.out=FILE}): loads
 * every stdlib, vendor and vgs module through one module cache, walks the objects reachable from
 * each cached module's globals, and writes how many of them carry an owner tag (a {@link
 * Mutability.Freezable}: frozen or not) and how many a cache-admission check could not classify.
 */
class OwnedNativeInventoryTest {

  private static final Pattern PUBLIC_NAME =
      Pattern.compile("^(?:def\\s+([A-Za-z]\\w*)|([A-Za-z]\\w*)\\s*=)", Pattern.MULTILINE);

  enum Kind {
    OWNED_FROZEN,
    OWNED_MUTABLE,
    IMMUTABLE_LEAF,
    STRUCTURAL,
    THREAD,
    UNKNOWN_STARLARK,
    FOREIGN_JAVA
  }

  @Test
  @EnabledIfSystemProperty(named = "larky.ownerInventory", matches = "true")
  void inventory() throws Exception {
    Path resources = Path.of("src/main/resources");
    List<String> labels = new ArrayList<>();
    for (String kind : List.of("stdlib", "vendor", "vgs")) {
      try (Stream<Path> files = Files.walk(resources.resolve(kind))) {
        files
            .filter(p -> p.toString().endsWith(".star"))
            .sorted()
            .forEach(
                p -> {
                  String rel = resources.resolve(kind).relativize(p).toString();
                  labels.add("@" + kind + "//" + rel.substring(0, rel.length() - ".star".length()));
                });
      }
    }
    ModuleCache cache = new ModuleCache(true);
    int loaded = 0;
    List<String> failures = new ArrayList<>();
    for (String label : labels) {
      String kind = label.substring(1, label.indexOf("//"));
      String rel = label.substring(label.indexOf("//") + 2);
      String src = Files.readString(resources.resolve(kind).resolve(rel + ".star"));
      Matcher m = PUBLIC_NAME.matcher(src);
      String sym = null;
      while (m.find()) {
        sym = m.group(1) != null ? m.group(1) : m.group(2);
        if (sym != null) {
          break;
        }
      }
      if (sym == null) {
        failures.add(label + "\tno public name");
        continue;
      }
      String script = "load(\"" + label + "\", \"" + sym + "\")\n";
      try {
        new LarkyScript(
                ModuleSupplier.CORE_MODULES,
                LarkyScript.StarlarkMode.STRICT,
                Map.of("ctx", new HashMap<String, Object>()))
            .withModuleCache(cache)
            .evaluate(InMemMapBackedStarFile.createStarFile("main.star", script), new TestingConsole());
        loaded++;
      } catch (Throwable t) {
        String msg = String.valueOf(t.getMessage());
        failures.add(label + "\t" + msg.substring(0, Math.min(120, msg.length())).replace('\n', ' '));
      }
    }

    Map<String, Module> modules = cachedModules(cache);
    Path out = Path.of(System.getProperty("larky.ownerInventory.out", "target/owner-inventory.csv"));
    try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
      w.println("# labels=" + labels.size() + " loaded=" + loaded + " cached_modules=" + modules.size());
      for (String f : failures) {
        w.println("# load_failure\t" + f);
      }
      w.println("module,kind,class,count");
      for (Map.Entry<String, Module> e : new TreeMap<>(modules).entrySet()) {
        Map<Kind, Map<String, Integer>> counts = walk(e.getValue());
        for (Map.Entry<Kind, Map<String, Integer>> k : counts.entrySet()) {
          for (Map.Entry<String, Integer> c : k.getValue().entrySet()) {
            w.printf("%s,%s,%s,%d%n", e.getKey(), k.getKey(), c.getKey(), c.getValue());
          }
        }
      }
    }
  }

  /**
   * Measurement only, aggregates only ({@code -Dlarky.ownerInventory.scripts=CSV
   * -Dlarky.ownerInventory.out=FILE}): runs the top level of each distinct script in the CSV's
   * {@code larky_script} column (without calling any of its functions) and counts the scripts whose
   * globals reach an owner-tagged native value. Such a value is frozen when the evaluation ends, so
   * a host that later calls a function changing it gets the owner error. Writes counts only.
   */
  @Test
  @EnabledIfSystemProperty(named = "larky.ownerInventory.scripts", matches = ".+")
  void hostScriptRootGlobals() throws Exception {
    Path csv = Path.of(System.getProperty("larky.ownerInventory.scripts"));
    List<String> scripts = new ArrayList<>(readScripts(csv));
    int ok = 0;
    int failed = 0;
    int withOwnedNative = 0;
    int withUnknown = 0;
    Map<String, Integer> ownedNativeClasses = new TreeMap<>();
    Map<String, Integer> failureKinds = new TreeMap<>();
    Field moduleField = ParsedStarFile.class.getDeclaredField("module");
    moduleField.setAccessible(true);
    for (String script : scripts) {
      ParsedStarFile result;
      try {
        result =
            new LarkyScript(
                    ModuleSupplier.CORE_MODULES,
                    LarkyScript.StarlarkMode.STRICT,
                    Map.of("ctx", new HashMap<String, Object>()))
                .withModuleCache(new ModuleCache(false))
                .evaluate(InMemMapBackedStarFile.createStarFile("main.star", script), new TestingConsole());
        ok++;
      } catch (Throwable t) {
        failed++;
        failureKinds.merge(t.getClass().getSimpleName(), 1, Integer::sum);
        continue;
      }
      Map<Kind, Map<String, Integer>> counts = walk((Module) moduleField.get(result));
      boolean owned = false;
      for (Kind k : List.of(Kind.OWNED_FROZEN, Kind.OWNED_MUTABLE)) {
        for (Map.Entry<String, Integer> c : counts.getOrDefault(k, Map.of()).entrySet()) {
          if (c.getKey().startsWith("com.verygood.security.larky.modules.")
              && !c.getKey().contains(".types.structs.")
              && !c.getKey().contains("LarkyMapping")) {
            owned = true;
            ownedNativeClasses.merge(c.getKey(), 1, Integer::sum);
          }
        }
      }
      withOwnedNative += owned ? 1 : 0;
      withUnknown += counts.containsKey(Kind.UNKNOWN_STARLARK) ? 1 : 0;
    }
    Path out = Path.of(System.getProperty("larky.ownerInventory.out", "target/owner-inventory-scripts.txt"));
    try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
      w.println("distinct_scripts=" + scripts.size());
      w.println("top_level_ok=" + ok);
      w.println("top_level_failed=" + failed + " " + failureKinds);
      w.println("ok_scripts_whose_globals_reach_an_owner_tagged_native=" + withOwnedNative);
      w.println("owner_tagged_native_classes(scripts)=" + ownedNativeClasses);
      w.println("ok_scripts_whose_globals_reach_an_unknown_value=" + withUnknown);
    }
  }

  private static Set<String> readScripts(Path csv) throws Exception {
    // RFC 4180: quoted fields with doubled quotes and embedded newlines.
    String text = Files.readString(csv, StandardCharsets.UTF_8);
    List<List<String>> rows = new ArrayList<>();
    List<String> row = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean quoted = false;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (quoted) {
        if (c == '"') {
          if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
            field.append('"');
            i++;
          } else {
            quoted = false;
          }
        } else {
          field.append(c);
        }
      } else if (c == '"') {
        quoted = true;
      } else if (c == ',') {
        row.add(field.toString());
        field.setLength(0);
      } else if (c == '\n') {
        row.add(field.toString());
        field.setLength(0);
        rows.add(row);
        row = new ArrayList<>();
      } else if (c != '\r') {
        field.append(c);
      }
    }
    if (field.length() > 0 || !row.isEmpty()) {
      row.add(field.toString());
      rows.add(row);
    }
    int col = rows.get(0).indexOf("larky_script");
    Set<String> scripts = new java.util.LinkedHashSet<>();
    for (List<String> r : rows.subList(1, rows.size())) {
      if (col < r.size() && !r.get(col).isBlank()) {
        scripts.add(r.get(col));
      }
    }
    return scripts;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Module> cachedModules(ModuleCache cache) throws Exception {
    Field f = ModuleCache.class.getDeclaredField("cache");
    f.setAccessible(true);
    Map<Object, Object> entries = (Map<Object, Object>) f.get(cache);
    Map<String, Module> out = new TreeMap<>();
    for (Map.Entry<Object, Object> e : entries.entrySet()) {
      String path = (String) e.getKey().getClass().getMethod("path").invoke(e.getKey());
      java.lang.reflect.Method mm = e.getValue().getClass().getDeclaredMethod("module");
      mm.setAccessible(true);
      out.put(path, (Module) mm.invoke(e.getValue()));
    }
    return out;
  }

  /** Objects reachable from the module's globals (only from them: other modules are walked on their own). */
  private static Map<Kind, Map<String, Integer>> walk(Module root) throws Exception {
    Map<Kind, Map<String, Integer>> counts = new TreeMap<>();
    Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    Deque<Object> todo = new ArrayDeque<>();
    root.getGlobals().values().stream().filter(java.util.Objects::nonNull).forEach(todo::push);
    Map<Object, Object> parent = new IdentityHashMap<>();
    while (!todo.isEmpty()) {
      Object o = todo.pop();
      if (o == null || !seen.add(o)) {
        continue;
      }
      if (o instanceof Module && o != root) {
        continue; // another module: walked as its own entry
      }
      Kind k = classify(o);
      counts.computeIfAbsent(k, x -> new TreeMap<>()).merge(o.getClass().getName(), 1, Integer::sum);
      if (k == Kind.THREAD || k == Kind.OWNED_MUTABLE) {
        // record what holds it, up to three levels
        StringBuilder via = new StringBuilder();
        Object p = parent.get(o);
        for (int i = 0; i < 3 && p != null; i++, p = parent.get(p)) {
          via.append(i == 0 ? "" : " <- ").append(p.getClass().getName());
        }
        counts.computeIfAbsent(Kind.valueOf(k.name()), x -> new TreeMap<>())
            .merge("via:" + via, 1, Integer::sum);
      }
      int before = todo.size();
      if (k == Kind.STRUCTURAL || k == Kind.UNKNOWN_STARLARK
          || (k == Kind.OWNED_FROZEN || k == Kind.OWNED_MUTABLE) && isStarlarkContainer(o)) {
        children(o, todo);
      }
      int added = todo.size() - before;
      java.util.Iterator<Object> it = todo.iterator();
      for (int i = 0; i < added && it.hasNext(); i++) {
        parent.putIfAbsent(it.next(), o);
      }
    }
    return counts;
  }

  private static boolean isStarlarkContainer(Object o) {
    // Starlark's own containers and Larky's structs: their contents are values to classify too. A
    // native wrapper's Java internals are covered by its owner tag and are not walked.
    String n = o.getClass().getName();
    return n.startsWith("net.starlark.java.eval.")
        || n.startsWith("com.verygood.security.larky.modules.types.structs.")
        || n.startsWith("com.verygood.security.larky.modules.types.LarkyMapping")
        || n.startsWith("com.verygood.security.larky.objects.");
  }

  private static Kind classify(Object o) {
    if (o instanceof StarlarkThread) {
      return Kind.THREAD;
    }
    if (o instanceof Mutability.Freezable f) {
      return f.mutability().isFrozen() ? Kind.OWNED_FROZEN : Kind.OWNED_MUTABLE;
    }
    if (o instanceof String || o instanceof Boolean || o instanceof Number || o instanceof Character
        || o instanceof Enum<?> || o instanceof Class<?>) {
      return Kind.IMMUTABLE_LEAF;
    }
    String n = o.getClass().getName();
    if (n.equals("net.starlark.java.eval.StarlarkInt") || n.startsWith("net.starlark.java.eval.StarlarkInt$")
        || n.equals("net.starlark.java.eval.StarlarkFloat") || n.equals("net.starlark.java.eval.NoneType")
        || n.equals("net.starlark.java.eval.StarlarkBytes") || n.startsWith("net.starlark.java.syntax.")
        || n.equals("net.starlark.java.eval.Mutability")) {
      return Kind.IMMUTABLE_LEAF;
    }
    if (o instanceof Module || n.startsWith("net.starlark.java.eval.")
        || n.startsWith("com.verygood.security.larky.modules.types.structs.")
        || n.startsWith("com.verygood.security.larky.objects.")
        || o instanceof Map || o instanceof Collection || o.getClass().isArray()) {
      return Kind.STRUCTURAL;
    }
    if (o instanceof StarlarkValue || n.startsWith("com.verygood.security.larky.")) {
      return Kind.UNKNOWN_STARLARK;
    }
    return Kind.FOREIGN_JAVA;
  }

  private static void children(Object o, Deque<Object> todo) throws IllegalAccessException {
    if (o instanceof Module m) {
      m.getGlobals().values().stream().filter(java.util.Objects::nonNull).forEach(todo::push);
      return;
    }
    if (o instanceof Map<?, ?> map) {
      for (Map.Entry<?, ?> e : map.entrySet()) {
        if (e.getKey() != null) {
          todo.push(e.getKey());
        }
        if (e.getValue() != null) {
          todo.push(e.getValue());
        }
      }
      return;
    }
    if (o instanceof Collection<?> c) {
      for (Object x : c) {
        if (x != null) {
          todo.push(x);
        }
      }
      return;
    }
    if (o.getClass().isArray()) {
      if (!o.getClass().getComponentType().isPrimitive()) {
        for (int i = 0; i < Array.getLength(o); i++) {
          Object x = Array.get(o, i);
          if (x != null) {
            todo.push(x);
          }
        }
      }
      return;
    }
    for (Class<?> c = o.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
      for (Field f : c.getDeclaredFields()) {
        if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) {
          continue;
        }
        try {
          f.setAccessible(true);
        } catch (RuntimeException inaccessible) {
          continue; // JDK internals
        }
        Object v = f.get(o);
        if (v != null) {
          todo.push(v);
        }
      }
    }
  }
}
