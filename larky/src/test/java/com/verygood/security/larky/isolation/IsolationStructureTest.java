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
package com.verygood.security.larky.isolation;

import static com.google.common.truth.Truth.assertWithMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Structural isolation check, run as a ratchet.
 *
 * <p>A value that is shared across evaluations (a process-wide static, or a module global of a
 * cached stdlib/vendor/vgs module) must be deeply immutable, or a frozen {@code Freezable}, and must
 * not store a {@code StarlarkThread} or an exception. This test scans the bytecode of every class in
 * larky's main output and of the VGS-owned libstarlark classes (those whose source file is under
 * {@code libstarlark/src/main/vgs}; upstream Bazel classes and the bytecode compiler in {@code
 * src/main/bc} are not scanned) and reports these violations:
 *
 * <ul>
 *   <li>{@code THREAD}: a field (instance or static) whose type mentions {@code StarlarkThread}
 *       anywhere: the field type, an array element, or a generic argument such as {@code
 *       Supplier<StarlarkThread>} or {@code List<StarlarkThread>}. Also a lambda (including a
 *       serializable one) that captures a {@code StarlarkThread} and whose functional interface is a
 *       {@code StarlarkValue}, so that the lambda can be reached as a value; the key counts such
 *       lambdas per method, so adding another one changes the key. Anonymous and inner classes that
 *       capture a thread show up as a synthetic {@code val$...} field.
 *   <li>{@code SHARED_THROWABLE}: a static field whose type mentions a {@code Throwable} anywhere
 *       (field type, array element, generic argument). A shared exception object keeps the call
 *       stack of the first evaluation that threw it ({@code EvalException.ensureStack} records it
 *       once), leaking one evaluation's locations and function names into another. A static of type
 *       {@code Object} or of an interface type cannot be checked this way; it is reported as {@code
 *       STATIC} instead.
 *   <li>{@code STATIC}: a static field that is not provably immutable: any non-final static, and any
 *       final static whose value is not of a {@linkplain #isShareable shareable} type. A value of
 *       interface type ({@code List}, {@code Map}, ...) is shareable only when every assignment
 *       stores the result of an immutable factory directly ({@code ImmutableList.of}, {@code
 *       List.of}, {@code Collections.unmodifiable*}, ...) and its generic arguments are shareable.
 *       Caches (Guava, {@code ConcurrentHashMap}), builtin type objects whose subclass registry is
 *       written at runtime, and enum constants with mutable fields are reported.
 *   <li>{@code NONFINAL}: a non-final, non-synthetic instance field of a class that implements
 *       {@code StarlarkValue} and is not freeze-guarded. Fields inherited from superclasses that are
 *       not themselves {@code StarlarkValue}s count, under the declaring class's name.
 *   <li>{@code MUTABLE}: a final instance field of such a class whose type is not shareable (an
 *       array, a {@code java.util} collection, {@code Object}, a stateful JDK type, ...).
 * </ul>
 *
 * <p>A type is <b>shareable</b> when it is a primitive, a known immutable JDK or Guava type, a class
 * annotated {@code @Immutable} (which {@link #immutableAnnotationsHold} checks independently of Error
 * Prone), an enum or final class whose instance fields are all final and shareable, or a
 * freeze-guarded {@code StarlarkValue}. A class is <b>freeze-guarded</b> when it (or a supertype)
 * implements {@code Mutability.Freezable}, or when it (or a superclass) has a {@code Mutability}
 * field and calls {@code checkMutable} somewhere ({@code isFrozen} alone is a read, not a write
 * guard). This is a proxy: it does not prove that every write is guarded.
 *
 * <p>The allowlist {@code isolation-allowlist.txt} (tab separated: {@code KIND}, key, one-line
 * reason; {@code #} starts a comment) records today's violations. The key is {@code
 * class#field:type}, so an entry stops matching when its field changes type. The test fails when a
 * violation is not allowlisted, when an allowlisted entry no longer exists, and when a reason is
 * missing or still says {@code TODO}.
 *
 * <p>Set {@code -Disolation.print=true} to print the current violations in allowlist format.
 */
public class IsolationStructureTest {
  private static final String CURRENT_EVALUATION = "com.verygood.security.larky.runtime.CurrentEvaluation";

  private static final String STARLARK_VALUE = "net/starlark/java/eval/StarlarkValue";
  private static final String FREEZABLE = "net/starlark/java/eval/Mutability$Freezable";
  private static final String THREAD = "net/starlark/java/eval/StarlarkThread";
  private static final String MUTABILITY_DESC = "Lnet/starlark/java/eval/Mutability;";
  private static final Set<String> IMMUTABLE_ANNOTATIONS =
      Set.of(
          "Lcom/google/errorprone/annotations/Immutable;",
          "Ljavax/annotation/concurrent/Immutable;");
  private static final Set<String> KINDS =
      Set.of("THREAD", "SHARED_THROWABLE", "STATIC", "NONFINAL", "MUTABLE");

  /** Classes that must be among the scanned ones; guards against scanning the wrong output. */
  private static final List<String> MUST_SCAN =
      List.of(
          "com/verygood/security/larky/objects/type/LarkyTypeObject",
          "com/verygood/security/larky/modules/types/structs/SimpleStruct",
          "com/verygood/security/larky/modules/re/RegexPattern",
          "net/starlark/java/eval/StarlarkBytes");

  /**
   * JDK and library types treated as immutable without looking inside them. Everything else under
   * {@code java/}, {@code javax/}, {@code jdk/}, {@code sun/} or {@code com/google/common/} is not
   * shareable unless listed here or matched by {@link #IMMUTABLE_LIBRARY_PREFIXES}.
   */
  private static final Set<String> IMMUTABLE_LIBRARY_TYPES =
      Set.of(
          "java/lang/String",
          "java/lang/Boolean",
          "java/lang/Byte",
          "java/lang/Character",
          "java/lang/Short",
          "java/lang/Integer",
          "java/lang/Long",
          "java/lang/Float",
          "java/lang/Double",
          "java/lang/Void",
          "java/lang/Class",
          "java/lang/invoke/MethodHandle",
          "java/math/BigInteger",
          "java/math/BigDecimal",
          "java/math/MathContext",
          "java/util/regex/Pattern",
          "java/nio/charset/Charset",
          "java/util/UUID",
          "java/util/Locale",
          "java/util/OptionalInt",
          "java/util/OptionalLong",
          "java/util/OptionalDouble",
          "com/google/common/base/CharMatcher",
          "com/google/common/base/Joiner",
          "com/google/common/base/Splitter",
          "com/google/common/io/BaseEncoding",
          "com/google/common/primitives/UnsignedInteger",
          "com/google/common/primitives/UnsignedLong",
          "com/google/re2j/Pattern",
          // Immutable Starlark values (a Tuple's elements are values frozen with it).
          "net/starlark/java/eval/Tuple",
          "net/starlark/java/eval/StarlarkInt",
          "net/starlark/java/eval/StarlarkFloat",
          "net/starlark/java/eval/NoneType",
          "net/starlark/java/eval/StarlarkSemantics",
          "net/starlark/java/syntax/Location",
          "net/starlark/java/syntax/FileOptions",
          // The freeze state itself: a guarded value's Mutability is what makes it safe to share.
          "net/starlark/java/eval/Mutability",
          "org/bouncycastle/asn1/ASN1ObjectIdentifier",
          // Process-wide loggers: thread-safe and never reachable from a script.
          "org/slf4j/Logger",
          "com/google/common/flogger/FluentLogger",
          "java/util/logging/Logger");

  private static final List<String> IMMUTABLE_LIBRARY_PREFIXES =
      List.of(
          "com/google/common/collect/Immutable",
          "com/google/common/primitives/Immutable",
          "java/time/format/DateTimeFormatter",
          "java/time/Duration",
          "java/time/Instant",
          "java/time/ZoneId",
          "java/time/ZoneOffset",
          "java/time/Local");

  /** Library packages whose classes are not analysed field by field. */
  private static final List<String> LIBRARY_PREFIXES =
      List.of("java/", "javax/", "jdk/", "sun/", "com/sun/", "com/google/", "org/");

  /** Interfaces whose value is immutable when it comes straight from an immutable factory. */
  private static final Set<String> FACTORY_INTERFACES =
      Set.of(
          "java/util/List",
          "java/util/Set",
          "java/util/Map",
          "java/util/Collection",
          "java/util/SortedSet",
          "java/util/SortedMap",
          "java/util/NavigableSet",
          "java/util/NavigableMap",
          "java/lang/Iterable",
          "java/util/Optional");

  private static final Pattern CLASS_REF = Pattern.compile("L([A-Za-z0-9_/$]+)[<;]");

  /** One scanned class. */
  private static final class ClassInfo {
    String name;
    int access;
    String source;
    String superName;
    List<String> interfaces = new ArrayList<>();
    List<FieldInfo> fields = new ArrayList<>();
    boolean callsFreezeCheck;
    boolean annotatedImmutable;
    /** static field name -> true while every <clinit> store is an immutable factory result. */
    Map<String, Boolean> immutableInit = new HashMap<>();
    /** static field name -> true while every <clinit> store is an empty immutable collection. */
    Map<String, Boolean> emptyInit = new HashMap<>();
    /** static field name -> exact classes stored by `new X(...)` immediately before the store. */
    Map<String, Set<String>> exactInit = new HashMap<>();
    /** static fields with at least one <clinit> store that is not `new X(...)`. */
    Set<String> exactUnknown = new HashSet<>();
    /** method name -> number of thread-capturing lambdas per functional interface. */
    Map<String, Map<String, Integer>> threadLambdas = new TreeMap<>();

    boolean isInterface() {
      return (access & Opcodes.ACC_INTERFACE) != 0;
    }

    boolean isFinal() {
      return (access & Opcodes.ACC_FINAL) != 0;
    }

    boolean isEnum() {
      return (access & Opcodes.ACC_ENUM) != 0;
    }
  }

  private record FieldInfo(String name, String desc, String signature, int access) {
    boolean isStatic() {
      return (access & Opcodes.ACC_STATIC) != 0;
    }

    boolean isFinal() {
      return (access & Opcodes.ACC_FINAL) != 0;
    }

    boolean isSynthetic() {
      return (access & Opcodes.ACC_SYNTHETIC) != 0;
    }

    /** Every class named in the field's type: the type itself, array elements, generic arguments. */
    List<String> referencedClasses() {
      List<String> out = new ArrayList<>();
      Matcher m = CLASS_REF.matcher(signature != null ? signature : desc);
      while (m.find()) {
        out.add(m.group(1));
      }
      return out;
    }

    String typeName() {
      return Type.getType(desc).getClassName();
    }
  }

  private record Violation(String kind, String key) implements Comparable<Violation> {
    @Override
    public int compareTo(Violation o) {
      int c = kind.compareTo(o.kind);
      return c != 0 ? c : key.compareTo(o.key);
    }

    @Override
    public String toString() {
      return kind + "\t" + key;
    }
  }

  private static final Map<String, ClassInfo> cache = new HashMap<>();
  private static final Map<String, Boolean> shareable = new HashMap<>();
  private static List<ClassInfo> scanned;

  @BeforeClass
  public static void scan() throws Exception {
    scanned = scannedClasses();
  }

  @Test
  public void scansTheExpectedClasses() {
    Set<String> names = new HashSet<>();
    scanned.forEach(c -> names.add(c.name));
    assertWithMessage("scanned classes").that(names).containsAtLeastElementsIn(MUST_SCAN);
    assertWithMessage("scanned class count").that(scanned.size()).isAtLeast(350);
    assertWithMessage("upstream libstarlark classes must not be scanned")
        .that(names)
        .doesNotContain("net/starlark/java/eval/Dict");
  }

  /**
   * Every class annotated {@code @Immutable} must pass this test's own analysis, so the guarantee
   * holds even if Error Prone stops running (it is a compiler plugin; nothing else would notice).
   */
  @Test
  public void immutableAnnotationsHold() {
    List<String> broken = new ArrayList<>();
    for (ClassInfo c : scanned) {
      if (!c.annotatedImmutable) {
        continue;
      }
      for (FieldInfo f : instanceFieldsWithInherited(c)) {
        if (!f.isFinal() || !isShareableField(f)) {
          broken.add(c.name.replace('/', '.') + "#" + f.name() + ":" + f.typeName());
        }
      }
    }
    assertWithMessage("fields of @Immutable classes that are not final and shareable")
        .that(broken)
        .isEmpty();
  }

  @Test
  public void isolationViolationsMatchTheAllowlist() throws Exception {
    TreeSet<Violation> found = new TreeSet<>();
    for (ClassInfo c : scanned) {
      collect(c, found);
    }

    if (Boolean.getBoolean("isolation.print")) {
      for (Violation v : found) {
        System.out.println("ISOLATION\t" + v + "\tTODO reason");
      }
    }

    Map<Violation, String> allowed = readAllowlist();
    TreeSet<Violation> fresh = new TreeSet<>(found);
    fresh.removeAll(allowed.keySet());
    TreeSet<Violation> stale = new TreeSet<>(allowed.keySet());
    stale.removeAll(found);

    StringBuilder msg = new StringBuilder();
    if (!fresh.isEmpty()) {
      msg.append("\nNew isolation violations (fix them; an allowlist line needs a reviewed reason):\n");
      fresh.forEach(v -> msg.append("  ").append(v).append('\n'));
    }
    if (!stale.isEmpty()) {
      msg.append(
          "\nAllowlisted entries that no longer exist (delete these lines from"
              + " isolation-allowlist.txt):\n");
      stale.forEach(v -> msg.append("  ").append(v).append('\n'));
    }
    if (msg.length() > 0) {
      org.junit.Assert.fail(msg.toString());
    }
  }

  private void collect(ClassInfo c, Set<Violation> out) {
    String cls = c.name.replace('/', '.');
    boolean isValue = isStarlarkValue(c.name);
    boolean guarded = isValue && isFreezeGuarded(c.name);
    for (FieldInfo f : c.fields) {
      String key = cls + "#" + f.name() + ":" + f.typeName();
      List<String> refs = f.referencedClasses();
      if (refs.contains(THREAD)) {
        out.add(new Violation("THREAD", key));
      }
      if (f.isStatic()) {
        if (refs.stream().anyMatch(this::isThrowable)) {
          out.add(new Violation("SHARED_THROWABLE", key));
        }
        if (isStaticMutable(c, f)) {
          out.add(new Violation("STATIC", key));
        }
      }
    }
    if (isValue && !guarded) {
      for (Map.Entry<String, FieldInfo> e : instanceFieldsByOwner(c).entrySet()) {
        FieldInfo f = e.getValue();
        String owner = e.getKey().substring(0, e.getKey().indexOf('#'));
        if (f.isSynthetic()) {
          continue;
        }
        // A field declared in a StarlarkValue superclass is reported for that class.
        if (!owner.equals(c.name) && isStarlarkValue(owner)) {
          continue;
        }
        String key = owner.replace('/', '.') + "#" + f.name() + ":" + f.typeName();
        if (!f.isFinal()) {
          out.add(new Violation("NONFINAL", key));
        } else if (!isShareableField(f)) {
          out.add(new Violation("MUTABLE", key));
        }
      }
    }
    for (Map.Entry<String, Map<String, Integer>> e : c.threadLambdas.entrySet()) {
      for (Map.Entry<String, Integer> sam : e.getValue().entrySet()) {
        if (isStarlarkValue(sam.getKey())) {
          out.add(
              new Violation(
                  "THREAD",
                  cls + "#lambda@" + e.getKey() + ":" + sam.getKey().replace('/', '.')
                      + " x" + sam.getValue()));
        }
      }
    }
  }

  private boolean isStaticMutable(ClassInfo c, FieldInfo f) {
    String n = f.name();
    if (n.equals("$VALUES") || n.equals("$assertionsDisabled") || n.startsWith("$SwitchMap$")) {
      // $VALUES is a private array that values() copies; the other two are compiler constants.
      return n.startsWith("$SwitchMap$") && !f.isFinal();
    }
    if (!f.isFinal()) {
      return true;
    }
    Type t = Type.getType(f.desc());
    if (t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) {
      return false;
    }
    if (t.getSort() == Type.ARRAY) {
      return true;
    }
    String type = t.getInternalName();
    Set<String> exact = c.exactInit.get(n);
    if (exact != null && !c.exactUnknown.contains(n)) {
      // Every store is `new X(...)`: judge X itself, whatever the declared type.
      return !exact.stream().allMatch(x -> isShareableClass(x, true));
    }
    if (c.emptyInit.getOrDefault(n, false)) {
      return false; // an empty immutable collection holds nothing, whatever its element type
    }
    if (FACTORY_INTERFACES.contains(type)) {
      return !(c.immutableInit.getOrDefault(n, false) && genericArgumentsShareable(f));
    }
    if (isShareable(type)) {
      return !(isStarlarkValue(type) || genericArgumentsShareable(f));
    }
    return true;
  }

  private boolean isShareableField(FieldInfo f) {
    Type t = Type.getType(f.desc());
    if (t.getSort() == Type.ARRAY) {
      return false;
    }
    if (t.getSort() != Type.OBJECT) {
      return true;
    }
    String type = t.getInternalName();
    if (!isShareable(type)) {
      return false;
    }
    // A Starlark container's elements are Starlark values, frozen along with it.
    return isStarlarkValue(type) || genericArgumentsShareable(f);
  }

  /** The generic arguments of an immutable container must be shareable too. */
  private boolean genericArgumentsShareable(FieldInfo f) {
    if (f.signature() == null) {
      return true;
    }
    // Class<?> names a class, which is shareable whatever its type argument.
    String sig = f.signature().replaceAll("Ljava/lang/Class<[^<>]*>;", "Ljava/lang/Class;");
    if (sig.matches(".*[<;]T[A-Za-z0-9_$]+;.*") || sig.matches(".*[*+-].*")) {
      return false; // a type variable or wildcard: unknown
    }
    List<String> refs = new ArrayList<>();
    Matcher m = CLASS_REF.matcher(sig);
    while (m.find()) {
      refs.add(m.group(1));
    }
    for (int i = 1; i < refs.size(); i++) {
      if (!isShareable(refs.get(i))) {
        return false;
      }
    }
    return true;
  }

  /** Whether a value whose static type is {@code type} is safe to share across evaluations. */
  private boolean isShareable(String type) {
    return isShareableClass(type, false);
  }

  /**
   * @param exact the value is known to be exactly this class (not a subclass), so a non-final class
   *     can be judged by its own fields
   */
  private boolean isShareableClass(String type, boolean exact) {
    String memo = type + (exact ? "!" : "");
    Boolean known = shareable.get(memo);
    if (known != null) {
      return known;
    }
    shareable.put(memo, true); // provisional, for recursive types
    boolean result = computeShareable(type, exact);
    shareable.put(memo, result);
    return result;
  }

  private boolean computeShareable(String type, boolean exact) {
    if (exact && type.equals("java/lang/Object")) {
      return true; // `new Object()`: an identity token with no state
    }
    if (IMMUTABLE_LIBRARY_TYPES.contains(type)
        || IMMUTABLE_LIBRARY_PREFIXES.stream().anyMatch(type::startsWith)) {
      return true;
    }
    if (LIBRARY_PREFIXES.stream().anyMatch(type::startsWith)) {
      return false;
    }
    ClassInfo ci = info(type);
    if (ci == null) {
      return false;
    }
    if (ci.annotatedImmutable || isAnnotatedImmutableUpwards(type)) {
      return true;
    }
    if (isStarlarkValue(type) && isFreezeGuarded(type)) {
      return true; // frozen with its module before it is shared
    }
    if (ci.isInterface() || (!exact && !ci.isFinal() && !ci.isEnum())) {
      return false; // an implementation or subclass may add state
    }
    for (FieldInfo f : instanceFieldsWithInherited(ci)) {
      if (!f.isFinal() || !isShareableField(f)) {
        return false;
      }
    }
    return true;
  }

  private boolean isAnnotatedImmutableUpwards(String type) {
    // @Immutable is @Inherited: a subclass of an annotated class is checked by Error Prone too.
    ClassInfo ci = info(type);
    while (ci != null && ci.superName != null) {
      ci = info(ci.superName);
      if (ci != null && ci.annotatedImmutable) {
        return true;
      }
    }
    return false;
  }

  /** Instance fields of the class and its superclasses, keyed {@code owner#field}. */
  private Map<String, FieldInfo> instanceFieldsByOwner(ClassInfo c) {
    Map<String, FieldInfo> out = new TreeMap<>();
    for (ClassInfo ci = c; ci != null; ci = ci.superName == null ? null : info(ci.superName)) {
      if (LIBRARY_PREFIXES.stream().anyMatch(ci.name::startsWith)) {
        break;
      }
      for (FieldInfo f : ci.fields) {
        if (!f.isStatic()) {
          out.put(ci.name + "#" + f.name(), f);
        }
      }
    }
    return out;
  }

  private List<FieldInfo> instanceFieldsWithInherited(ClassInfo c) {
    return new ArrayList<>(instanceFieldsByOwner(c).values());
  }

  private boolean isFreezeGuarded(String name) {
    Set<String> supers = allSupertypes(name);
    if (supers.contains(FREEZABLE)) {
      return true;
    }
    boolean mutabilityField = false;
    boolean check = false;
    for (String s : supers) {
      ClassInfo ci = info(s);
      if (ci == null) {
        continue;
      }
      check |= ci.callsFreezeCheck;
      for (FieldInfo f : ci.fields) {
        mutabilityField |= !f.isStatic() && f.desc().equals(MUTABILITY_DESC);
      }
    }
    return mutabilityField && check;
  }

  private boolean isThrowable(String name) {
    return allSupertypes(name).contains("java/lang/Throwable");
  }

  private boolean isStarlarkValue(String name) {
    return allSupertypes(name).contains(STARLARK_VALUE);
  }

  /** The class itself plus all superclasses and interfaces, transitively. */
  private Set<String> allSupertypes(String name) {
    Set<String> seen = new HashSet<>();
    Deque<String> todo = new ArrayDeque<>();
    todo.add(name);
    while (!todo.isEmpty()) {
      String n = todo.poll();
      if (!seen.add(n)) {
        continue;
      }
      ClassInfo ci = info(n);
      if (ci != null) {
        if (ci.superName != null) {
          todo.add(ci.superName);
        }
        todo.addAll(ci.interfaces);
      }
    }
    return seen;
  }

  private static ClassInfo info(String name) {
    if (cache.containsKey(name)) {
      return cache.get(name);
    }
    ClassInfo ci = null;
    String res = name + ".class";
    ClassLoader cl = IsolationStructureTest.class.getClassLoader();
    try (InputStream in = cl.getResourceAsStream(res)) {
      if (in != null) {
        ci = parse(in.readAllBytes());
      }
    } catch (IOException e) {
      throw new IllegalStateException("cannot read " + name, e);
    }
    cache.put(name, ci);
    return ci;
  }

  // ---- class discovery ----------------------------------------------------------------------

  private static List<ClassInfo> scannedClasses() throws Exception {
    List<ClassInfo> result = new ArrayList<>();
    for (byte[] b : classBytes(codeSource("com.verygood.security.larky.Larky"), "")) {
      ClassInfo ci = parse(b);
      cache.putIfAbsent(ci.name, ci);
      result.add(ci);
    }
    // from larky/ under Maven; from the repository root in some IDE runs
    Path vgs = basedir().resolve("../libstarlark/src/main/vgs").normalize();
    if (!Files.isDirectory(vgs)) {
      vgs = basedir().resolve("libstarlark/src/main/vgs").normalize();
    }
    assertWithMessage("VGS libstarlark source root " + vgs).that(Files.isDirectory(vgs)).isTrue();
    for (byte[] b : classBytes(codeSource("net.starlark.java.eval.StarlarkValue"), "net/starlark/")) {
      ClassInfo ci = parse(b);
      if (ci.source == null) {
        continue;
      }
      int slash = ci.name.lastIndexOf('/');
      String pkg = slash < 0 ? "" : ci.name.substring(0, slash + 1);
      if (Files.isRegularFile(vgs.resolve(pkg + ci.source))) {
        cache.putIfAbsent(ci.name, ci);
        result.add(ci);
      }
    }
    return result;
  }

  private static Path basedir() {
    return Paths.get(System.getProperty("basedir", System.getProperty("user.dir"))).toAbsolutePath();
  }

  private static Path codeSource(String className) throws ClassNotFoundException, URISyntaxException {
    Class<?> c = Class.forName(className, false, IsolationStructureTest.class.getClassLoader());
    return Paths.get(c.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  private static List<byte[]> classBytes(Path root, String prefix) throws IOException {
    List<byte[]> out = new ArrayList<>();
    if (Files.isDirectory(root)) {
      try (Stream<Path> s = Files.walk(root.resolve(prefix))) {
        List<Path> files = new ArrayList<>();
        s.filter(p -> p.toString().endsWith(".class")).forEach(files::add);
        Collections.sort(files);
        for (Path p : files) {
          out.add(Files.readAllBytes(p));
        }
      }
    } else {
      try (JarFile jar = new JarFile(root.toFile())) {
        List<JarEntry> entries = new ArrayList<>();
        jar.stream()
            .filter(e -> e.getName().endsWith(".class") && e.getName().startsWith(prefix))
            .filter(e -> !e.getName().startsWith("META-INF/"))
            .forEach(entries::add);
        for (JarEntry e : entries) {
          try (InputStream in = jar.getInputStream(e)) {
            out.add(in.readAllBytes());
          }
        }
      }
    }
    return out;
  }

  // ---- bytecode parsing ---------------------------------------------------------------------

  private static boolean isImmutableFactory(String owner, String name) {
    if (owner.startsWith("com/google/common/collect/Immutable")) {
      // ImmutableX.builder() returns a mutable builder; only its build methods are immutable.
      if (owner.endsWith("$Builder")) {
        return name.startsWith("build");
      }
      return !name.startsWith("builder");
    }
    if (owner.equals("java/util/Collections")) {
      return name.startsWith("unmodifiable")
          || name.startsWith("empty")
          || name.startsWith("singleton");
    }
    if (owner.equals("java/util/List") || owner.equals("java/util/Set") || owner.equals("java/util/Map")) {
      return name.equals("of") || name.equals("copyOf") || name.equals("ofEntries");
    }
    if (owner.equals("java/util/Optional")) {
      return name.equals("of") || name.equals("empty") || name.equals("ofNullable");
    }
    return owner.equals("java/util/stream/Stream") && name.equals("toList");
  }

  private static ClassInfo parse(byte[] bytes) {
    ClassReader cr = new ClassReader(bytes);
    ClassInfo ci = new ClassInfo();
    cr.accept(
        new ClassVisitor(Opcodes.ASM9) {
          @Override
          public void visit(int v, int acc, String name, String sig, String sup, String[] ifs) {
            ci.name = name;
            ci.access = acc;
            ci.superName = sup;
            if (ifs != null) {
              ci.interfaces.addAll(List.of(ifs));
            }
          }

          @Override
          public void visitSource(String s, String d) {
            ci.source = s;
          }

          @Override
          public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            ci.annotatedImmutable |= IMMUTABLE_ANNOTATIONS.contains(desc);
            return null;
          }

          @Override
          public FieldVisitor visitField(int acc, String name, String desc, String sig, Object val) {
            ci.fields.add(new FieldInfo(name, desc, sig, acc));
            return null;
          }

          @Override
          public MethodVisitor visitMethod(
              int acc, String mname, String mdesc, String sig, String[] ex) {
            return new StoreTracker(ci, mname, mname.equals("<clinit>"));
          }
        },
        ClassReader.SKIP_FRAMES);
    return ci;
  }

  /**
   * Records, for each static field stored in {@code <clinit>}, whether the stored value comes
   * straight from an immutable factory or a constructor (the call must be the instruction right
   * before the store, a {@code CHECKCAST} aside), plus freeze checks and thread-capturing lambdas.
   */
  private static final class StoreTracker extends MethodVisitor {
    private final ClassInfo ci;
    private final String method;
    private final boolean clinit;
    private boolean lastWasFactory;
    private boolean lastWasEmpty;
    private String lastConstructed;

    StoreTracker(ClassInfo ci, String method, boolean clinit) {
      super(Opcodes.ASM9);
      this.ci = ci;
      this.method = method;
      this.clinit = clinit;
    }

    private void other() {
      lastWasFactory = false;
      lastWasEmpty = false;
      lastConstructed = null;
    }

    @Override
    public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
      if (name.equals("checkMutable")) {
        ci.callsFreezeCheck = true;
      }
      other();
      lastWasFactory = isImmutableFactory(owner, name);
      lastWasEmpty =
          lastWasFactory
              && (name.startsWith("empty") || (name.equals("of") && desc.startsWith("()")));
      if (op == Opcodes.INVOKESPECIAL && name.equals("<init>")) {
        lastConstructed = owner;
      }
    }

    @Override
    public void visitFieldInsn(int op, String owner, String name, String desc) {
      if (clinit && op == Opcodes.PUTSTATIC && owner.equals(ci.name)) {
        ci.immutableInit.merge(name, lastWasFactory, Boolean::logicalAnd);
        ci.emptyInit.merge(name, lastWasEmpty, Boolean::logicalAnd);
        if (lastConstructed != null) {
          ci.exactInit.computeIfAbsent(name, k -> new TreeSet<>()).add(lastConstructed);
        } else {
          // A store that is not `new X(...)`: the exact class is unknown for this field.
          ci.exactUnknown.add(name);
        }
      }
      other();
    }

    @Override
    public void visitTypeInsn(int op, String type) {
      if (op != Opcodes.CHECKCAST) {
        other();
      }
    }

    @Override
    public void visitInsn(int op) {
      other();
    }

    @Override
    public void visitIntInsn(int op, int operand) {
      other();
    }

    @Override
    public void visitVarInsn(int op, int var) {
      other();
    }

    @Override
    public void visitJumpInsn(int op, Label label) {
      other();
    }

    @Override
    public void visitLabel(Label label) {
      // A jump target: the value on the stack may come from elsewhere.
      other();
    }

    @Override
    public void visitLdcInsn(Object value) {
      other();
    }

    @Override
    public void visitIincInsn(int var, int increment) {
      other();
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
      other();
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
      other();
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
      other();
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... args) {
      other();
      boolean lambda = bsm.getName().equals("metafactory") || bsm.getName().equals("altMetafactory");
      if (!lambda) {
        return;
      }
      for (Type t : Type.getArgumentTypes(desc)) {
        if (t.getSort() == Type.OBJECT && t.getInternalName().equals(THREAD)) {
          ci.threadLambdas
              .computeIfAbsent(method, k -> new TreeMap<>())
              .merge(Type.getReturnType(desc).getInternalName(), 1, Integer::sum);
          return;
        }
      }
    }
  }

  // ---- allowlist ----------------------------------------------------------------------------

  private static Map<Violation, String> readAllowlist() throws IOException {
    Map<Violation, String> out = new TreeMap<>();
    try (InputStream in = IsolationStructureTest.class.getResourceAsStream("/isolation-allowlist.txt")) {
      assertWithMessage("isolation-allowlist.txt on the test classpath").that(in).isNotNull();
      int lineNo = 0;
      for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList()) {
        lineNo++;
        if (line.isBlank() || line.startsWith("#")) {
          continue;
        }
        String[] p = line.split("\t", 3);
        assertWithMessage("allowlist line %s must be KIND<TAB>class#field:type<TAB>reason: %s", lineNo, line)
            .that(p.length == 3 && !p[2].isBlank())
            .isTrue();
        assertWithMessage("allowlist line %s still has a placeholder reason", lineNo)
            .that(p[2].trim().toUpperCase(java.util.Locale.ROOT).startsWith("TODO"))
            .isFalse();
        assertWithMessage("unknown kind on allowlist line %s", lineNo).that(KINDS).contains(p[0]);
        // A StarlarkThread may be held only by CurrentEvaluation, which sets and restores the
        // running thread around each evaluation. Anything else takes the thread as a parameter or
        // reads CurrentEvaluation; there is no reason that makes a stored thread acceptable.
        assertWithMessage(
                "allowlist line %s: only CurrentEvaluation may hold a StarlarkThread", lineNo)
            .that(!p[0].equals("THREAD") || p[1].startsWith(CURRENT_EVALUATION + "#"))
            .isTrue();
        Violation v = new Violation(p[0], p[1]);
        assertWithMessage("duplicate allowlist entry on line %s: %s", lineNo, v)
            .that(out.put(v, p[2]))
            .isNull();
      }
    }
    return out;
  }
}
