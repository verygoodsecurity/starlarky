package com.verygood.security.larky.taint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.starlark.java.annot.Declassifies;
import net.starlark.java.annot.SecretSource;
import net.starlark.java.annot.StarlarkAnnotations;
import net.starlark.java.annot.StarlarkMethod;
import org.junit.jupiter.api.Test;

/**
 * Every {@code @StarlarkMethod} must have a recorded taint classification, so that a new built-in
 * cannot arrive unreviewed. Methods propagate by default; the classification file is what makes
 * that a reviewed decision rather than an accident.
 *
 * <p>After adding a built-in, classify it and regenerate with {@code
 * -Dlarky.taint.updateClassification=true}.
 */
class SecretTaintCoverageTest {

  private static final Path CLASSIFICATION =
      Paths.get("src", "test", "resources", "taint", "starlark_method_classification.txt");

  /** Methods whose implementation checks taint itself: "class#name". */
  private static final Set<String> SINKS =
      ImmutableSet.of("net.starlark.java.eval.MethodLibrary#print");

  // Names that imply a method hands out a secret, or sends text somewhere outside the script.
  private static final Pattern SOURCE_LIKE = Pattern.compile("reveal|detokenize");
  private static final Pattern SINK_LIKE = Pattern.compile("print|record|emit|publish");

  private static TreeMap<String, String> classify() throws Exception {
    TreeMap<String, String> found = new TreeMap<>();
    for (ClassPath.ClassInfo info :
        ClassPath.from(SecretTaintCoverageTest.class.getClassLoader()).getAllClasses()) {
      String name = info.getName();
      if (!(name.startsWith("net.starlark.java.") || name.startsWith("com.verygood.security.larky."))
          || name.contains(".processor.")) {
        continue;
      }
      Class<?> cls;
      try {
        cls = Class.forName(name, false, SecretTaintCoverageTest.class.getClassLoader());
      } catch (ClassNotFoundException | LinkageError e) {
        continue; // optional dependency not on the test classpath
      }
      if (name.contains("Test") && name.contains("$")) {
        continue;
      }
      Method[] methods;
      try {
        methods = cls.getDeclaredMethods();
      } catch (LinkageError e) {
        continue;
      }
      for (Method method : methods) {
        StarlarkMethod annotation = method.getAnnotation(StarlarkMethod.class);
        if (annotation != null) {
          found.put(name + "#" + annotation.name(), kind(cls, method, annotation));
        }
      }
    }
    return found;
  }

  private static String kind(Class<?> cls, Method method, StarlarkMethod annotation) {
    if (StarlarkAnnotations.hasAnnotationInHierarchy(method, SecretSource.class)) {
      assertTrue(
          !annotation.structField(),
          () -> "@SecretSource requires a callable method: " + cls.getName() + "#" + annotation.name());
      return "source";
    }
    if (SINKS.contains(cls.getName() + "#" + annotation.name())) {
      return "sink";
    }
    if (StarlarkAnnotations.hasAnnotationInHierarchy(method, Declassifies.class)) {
      return "declassify";
    }
    return "propagate";
  }

  private static List<String> render(TreeMap<String, String> found) {
    return found.entrySet().stream()
        .map(e -> e.getKey() + " " + e.getValue())
        .collect(Collectors.toList());
  }

  @Test
  void everyMethodIsClassified() throws Exception {
    List<String> actual = render(classify());
    if (Boolean.getBoolean("larky.taint.updateClassification")) {
      Files.createDirectories(CLASSIFICATION.getParent());
      Files.write(CLASSIFICATION, actual, StandardCharsets.UTF_8);
    }
    assertTrue(actual.size() > 500, "scan found too few methods: " + actual.size());
    List<String> recorded = Files.readAllLines(CLASSIFICATION, StandardCharsets.UTF_8);
    List<String> missing = new ArrayList<>(actual);
    missing.removeAll(recorded);
    List<String> stale = new ArrayList<>(recorded);
    stale.removeAll(actual);
    assertEquals(
        List.of(),
        missing,
        "unclassified or changed @StarlarkMethods; review and update "
            + CLASSIFICATION
            + " (stale entries: "
            + stale
            + ")");
    assertEquals(List.of(), stale, "classified methods that no longer exist");
  }

  @Test
  void methodsNamedLikeSourcesAreSources() throws Exception {
    List<String> wrong = new ArrayList<>();
    classify()
        .forEach(
            (method, kind) -> {
              String name = method.substring(method.indexOf('#') + 1);
              if (SOURCE_LIKE.matcher(name).matches() && !kind.equals("source")) {
                wrong.add(method + " is " + kind);
              }
            });
    assertEquals(List.of(), wrong);
  }

  @Test
  void methodsNamedLikeSinksAreSinks() throws Exception {
    List<String> wrong = new ArrayList<>();
    classify()
        .forEach(
            (method, kind) -> {
              String name = method.substring(method.indexOf('#') + 1);
              if (SINK_LIKE.matcher(name).matches() && !kind.equals("sink")) {
                wrong.add(method + " is " + kind);
              }
            });
    assertEquals(List.of(), wrong);
  }
}
