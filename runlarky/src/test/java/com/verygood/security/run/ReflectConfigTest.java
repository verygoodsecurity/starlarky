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

package com.verygood.security.run;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.ClassPath;
import com.verygood.security.larky.ModuleSupplier;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.starlark.java.annot.StarlarkAnnotations;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;
import org.junit.Test;

/**
 * Checks that the native image's reflect-config.json lets Starlark see the methods of every class
 * a script can reach through {@link ModuleSupplier}.
 *
 * <p>Starlark finds a value's methods with {@code Class.getMethods()} and their annotations with
 * {@code getDeclaredMethods()} on the class that declares them ({@code CallUtils},
 * {@code StarlarkAnnotations}). In a native image both return only what reflect-config.json
 * registers, so a missing class makes its methods vanish: {@code type(1)} fails with "'type'
 * object is not callable" (issue #397).
 */
public class ReflectConfigTest {

  private static final String CONFIG = "META-INF/native-image/reflect-config.json";
  private static final List<String> SCANNED_PACKAGES =
      List.of("com.verygood.security.larky.", "net.starlark.java.");

  /** A reflect-config.json entry: which methods of one class are registered. */
  private record Entry(boolean allPublic, boolean allDeclared, Set<String> methods) {}

  @Test
  public void everyReachableStarlarkClassIsRegistered() throws Exception {
    Map<String, Entry> config = readConfig();

    // class name -> what is missing for it
    Map<String, Set<String>> missing = new TreeMap<>();
    for (Class<?> clazz : reachableClasses()) {
      // Values are instances of concrete classes; an abstract one matters only as a declarer.
      boolean concrete = !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers());
      for (Method method : clazz.getMethods()) {
        StarlarkMethod annotation = StarlarkAnnotations.getStarlarkMethod(method);
        if (annotation == null) {
          continue;
        }
        // getMethods() on the value's class lists it, from the methods registered on the class
        // that declares it ...
        if (concrete) {
          require(config, missing, clazz, method, Access.PUBLIC);
        }
        require(config, missing, method.getDeclaringClass(), method, Access.EITHER);
        // ... and StarlarkAnnotations finds its annotation with getDeclaredMethods() on the class
        // that carries it. (Classes searched before that one may return nothing.)
        Class<?> owner = annotationOwner(method.getDeclaringClass(), method);
        if (owner == null) {
          fail("StarlarkAnnotations found @StarlarkMethod " + method.getName() + " on "
              + clazz.getName() + ", but no class above " + method.getDeclaringClass().getName()
              + " declares it; annotationOwner no longer matches StarlarkAnnotations");
        }
        require(config, missing, owner, method, Access.DECLARED);
      }
    }

    if (!missing.isEmpty()) {
      StringBuilder message =
          new StringBuilder(missing.size() + " classes need entries in " + CONFIG + ":\n");
      missing.forEach((name, what) -> message.append("  ").append(name).append(' ')
          .append(what).append('\n'));
      fail(message.toString());
    }
  }

  @Test
  public void everyLarkyOrStarlarkEntryNamesAClass() throws Exception {
    List<String> unknown = new ArrayList<>();
    for (String name : readConfig().keySet()) {
      if (SCANNED_PACKAGES.stream().noneMatch(name::startsWith)) {
        continue;
      }
      try {
        Class.forName(name, false, getClass().getClassLoader());
      } catch (ClassNotFoundException e) {
        unknown.add(name);
      }
    }
    assertTrue("entries naming no class: " + unknown, unknown.isEmpty());
  }

  private enum Access { PUBLIC, DECLARED, EITHER }

  /** Records what {@code clazz}'s entry lacks for {@code method} to be found with {@code access}. */
  private static void require(
      Map<String, Entry> config,
      Map<String, Set<String>> missing,
      Class<?> clazz,
      Method method,
      Access access) {
    Entry entry = config.get(clazz.getName());
    if (entry == null) {
      missing.computeIfAbsent(clazz.getName(), k -> new TreeSet<>()).add("(no entry)");
      return;
    }
    boolean covered = switch (access) {
      case PUBLIC -> entry.allPublic();
      case DECLARED -> entry.allDeclared();
      case EITHER -> entry.allPublic() || entry.allDeclared();
    };
    if (!covered && !entry.methods().contains(method.getName())) {
      String flag = access == Access.DECLARED ? "allDeclaredMethods" : "allPublicMethods";
      missing.computeIfAbsent(clazz.getName(), k -> new TreeSet<>())
          .add(flag + " or method " + method.getName());
    }
  }

  /** The class whose {@code @StarlarkMethod} StarlarkAnnotations.getStarlarkMethod returns. */
  private static Class<?> annotationOwner(Class<?> clazz, Method method) {
    for (Method declared : clazz.getDeclaredMethods()) {
      // Matched by name, as StarlarkAnnotations does.
      if (declared.getName().equals(method.getName())
          && declared.isAnnotationPresent(StarlarkMethod.class)) {
        return clazz;
      }
    }
    if (clazz.getSuperclass() != null) {
      Class<?> owner = annotationOwner(clazz.getSuperclass(), method);
      if (owner != null) {
        return owner;
      }
    }
    for (Class<?> iface : clazz.getInterfaces()) {
      Class<?> owner = annotationOwner(iface, method);
      if (owner != null) {
        return owner;
      }
    }
    return null;
  }

  /**
   * Everything {@link ModuleSupplier} exposes, plus, transitively, the return types of their
   * {@code @StarlarkMethod}s. An abstract return type stands for each of its concrete subclasses
   * in Larky and Starlark. Methods returning {@code Object} or {@code StarlarkValue} cannot be
   * followed (e.g. {@code type(1)}, {@code set()}), so every concrete {@code StarlarkValue} class in
   * Larky and Starlark is included as well.
   */
  private static Set<Class<?>> reachableClasses() throws Exception {
    List<Class<?>> scanned = scannedClasses();
    Set<Class<?>> seen = new LinkedHashSet<>();
    ArrayDeque<Class<?>> work = new ArrayDeque<>();

    work.addAll(ModuleSupplier.CORE_MODULES);
    ModuleSupplier.CORE_ENVIRONMENT.values().forEach(v -> work.add(v.getClass()));
    ModuleSupplier.STD_MODULES.forEach(v -> work.add(v.getClass()));
    ModuleSupplier.VGS_MODULES.forEach(v -> work.add(v.getClass()));
    ModuleSupplier.TEST_MODULES.forEach(v -> work.add(v.getClass()));

    while (!work.isEmpty()) {
      Class<?> clazz = work.pop();
      if (!seen.add(clazz)) {
        continue;
      }
      for (Method method : clazz.getMethods()) {
        if (StarlarkAnnotations.getStarlarkMethod(method) == null) {
          continue;
        }
        Class<?> returned = method.getReturnType();
        if (returned == Object.class
            || returned == StarlarkValue.class
            || SCANNED_PACKAGES.stream().noneMatch(returned.getName()::startsWith)) {
          continue;
        }
        work.add(returned);
        if (returned.isInterface() || Modifier.isAbstract(returned.getModifiers())) {
          for (Class<?> candidate : scanned) {
            if (returned.isAssignableFrom(candidate)) {
              work.add(candidate);
            }
          }
        }
      }
    }
    for (Class<?> clazz : scanned) {
      if (StarlarkValue.class.isAssignableFrom(clazz)) {
        seen.add(clazz);
      }
    }
    return seen;
  }

  /** Concrete classes in the Larky and Starlark packages. */
  private static List<Class<?>> scannedClasses() throws Exception {
    ClassLoader loader = ReflectConfigTest.class.getClassLoader();
    List<Class<?>> classes = new ArrayList<>();
    for (ClassPath.ClassInfo info : ClassPath.from(loader).getAllClasses()) {
      if (SCANNED_PACKAGES.stream().noneMatch(info.getName()::startsWith)) {
        continue;
      }
      Class<?> clazz;
      try {
        clazz = Class.forName(info.getName(), false, loader);
      } catch (Throwable e) {
        continue; // optional dependencies missing from this classpath
      }
      if (!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
        classes.add(clazz);
      }
    }
    return classes;
  }

  private static Map<String, Entry> readConfig() throws Exception {
    Map<String, Entry> config = new HashMap<>();
    try (InputStream in = ReflectConfigTest.class.getClassLoader().getResourceAsStream(CONFIG)) {
      if (in == null) {
        fail(CONFIG + " not found on the classpath");
      }
      Set<String> duplicates = new HashSet<>();
      for (JsonNode node : new ObjectMapper().readTree(in)) {
        String name = node.get("name").asText();
        Set<String> methods = new HashSet<>();
        if (node.has("methods")) {
          node.get("methods").forEach(m -> methods.add(m.get("name").asText()));
        }
        Entry entry = new Entry(
            node.path("allPublicMethods").asBoolean(false),
            node.path("allDeclaredMethods").asBoolean(false),
            methods);
        if (config.put(name, entry) != null) {
          duplicates.add(name);
        }
      }
      assertTrue("duplicate entries: " + duplicates, duplicates.isEmpty());
    }
    return config;
  }
}
