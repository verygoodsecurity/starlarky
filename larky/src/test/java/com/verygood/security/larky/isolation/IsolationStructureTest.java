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
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Structural isolation check, run as a ratchet.
 *
 * <p>A value that is shared across evaluations (module globals of a cached stdlib/vendor/vgs
 * module) must be deeply immutable and must not store a {@code StarlarkThread}. This test scans
 * the bytecode of every class in larky's main output and of the VGS-owned libstarlark classes
 * (those whose source file is under {@code libstarlark/src/main/vgs}; upstream Bazel classes and
 * the bytecode compiler in {@code src/main/bc} are not scanned) and reports these violations:
 *
 * <ul>
 *   <li>{@code THREAD}: a field (instance or static, also generic arguments such as {@code
 *       Supplier<StarlarkThread>}) of type {@code StarlarkThread}, or a lambda that captures a
 *       {@code StarlarkThread} and whose functional interface is a {@code StarlarkValue} (so the
 *       lambda can be reached as a value). Anonymous and inner classes capturing a thread show up
 *       as a synthetic {@code val$...} field. Key: {@code class#field} or {@code
 *       class#lambda@method}.
 *   <li>{@code NONFINAL}: a non-final, non-synthetic instance field declared in a class that
 *       implements {@code StarlarkValue} and is not freeze-guarded. A class is freeze-guarded when
 *       it (or a superclass) implements {@code net.starlark.java.eval.Freezable}, or when it
 *       (or a superclass) has a {@code Mutability} field and calls {@code checkMutable}, {@code
 *       isFrozen} or {@code isImmutable} somewhere. This is a pragmatic proxy: it does not prove
 *       that every write is guarded. Key: {@code class#field}.
 *   <li>{@code STATIC}: a static field that holds mutable state: a non-final field, or a final
 *       field of array / {@code java.util} collection / stateful JDK or crypto type that is not
 *       initialised by an immutable factory ({@code Immutable*}, {@code Collections.unmodifiable*},
 *       {@code List.of}, ...). Key: {@code class#field}.
 *   <li>{@code SHARED_THROWABLE}: a static field (final or not) whose type is assignable to {@code
 *       Throwable}. A shared exception object keeps the call stack of the first evaluation that
 *       threw it ({@code EvalException.ensureStack} records it once), leaking one evaluation's
 *       locations and function names into another. Key: {@code class#field}.
 * </ul>
 *
 * <p>The allowlist {@code isolation-allowlist.txt} (tab separated: {@code KIND}, {@code key},
 * one-line reason; {@code #} starts a comment) records today's violations. The test fails when a
 * violation is not allowlisted AND when an allowlisted entry no longer exists, so a change that
 * fixes a violation must delete its line, and nothing new can be added without a reviewed reason.
 *
 * <p>Set {@code -Disolation.print=true} to print the current violations in allowlist format.
 */
public class IsolationStructureTest {

  private static final String STARLARK_VALUE = "net/starlark/java/eval/StarlarkValue";
  private static final String FREEZABLE = "net/starlark/java/eval/Freezable";
  private static final String THREAD_DESC = "Lnet/starlark/java/eval/StarlarkThread;";
  private static final String MUTABILITY_DESC = "Lnet/starlark/java/eval/Mutability;";

  private static final Pattern MUTABLE_COLLECTION =
      Pattern.compile(
          "Ljava/util/(concurrent/)?[A-Za-z]*(Map|List|Set|Collection|Deque|Queue|Iterator)[A-Za-z]*;"
              + "|Lcom/google/common/collect/(?!Immutable)[A-Za-z]*(Multimap|Multiset|Table|Map|List|Set)[A-Za-z]*;");
  private static final Pattern STATEFUL =
      Pattern.compile(
          "java/security/(MessageDigest|Signature|SecureRandom|KeyStore)|javax/crypto/"
              + "|java/util/concurrent/atomic/|java/lang/(StringBuilder|StringBuffer)|java/nio/[A-Za-z]*Buffer"
              + "|java/util/(Random|regex/Matcher)|java/io/[A-Za-z]*(Reader|Writer|Stream);"
              + "|Deflater|Inflater|java/lang/ThreadLocal|java/lang/ref/");

  /** One scanned class. */
  private static final class ClassInfo {
    String name;
    int access;
    String source;
    String superName;
    List<String> interfaces = new ArrayList<>();
    List<FieldInfo> fields = new ArrayList<>();
    boolean callsFreezeCheck;
    /** static field name -> true when <clinit> stores the result of an immutable factory. */
    Map<String, Boolean> immutableInit = new HashMap<>();
    /** methods containing a lambda that captures a thread: method name -> SAM interface. */
    Map<String, Set<String>> threadLambdas = new TreeMap<>();
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

  private final Map<String, ClassInfo> cache = new HashMap<>();

  @Test
  public void isolationViolationsMatchTheAllowlist() throws Exception {
    List<ClassInfo> scanned = scannedClasses();
    assertWithMessage("scanned class count").that(scanned.size()).isAtLeast(300);

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
      String key = cls + "#" + f.name();
      if (f.desc().equals(THREAD_DESC)
          || (f.signature() != null && f.signature().contains(THREAD_DESC))) {
        out.add(new Violation("THREAD", key));
      }
      if (f.isStatic()) {
        if (f.desc().startsWith("L") && isThrowable(f.desc().substring(1, f.desc().length() - 1))) {
          out.add(new Violation("SHARED_THROWABLE", key));
        }
        if (isStaticMutable(c, f)) {
          out.add(new Violation("STATIC", key));
        }
      } else if (isValue && !f.isFinal() && !f.isSynthetic() && !guarded) {
        out.add(new Violation("NONFINAL", key));
      }
    }
    for (Map.Entry<String, Set<String>> e : c.threadLambdas.entrySet()) {
      for (String sam : e.getValue()) {
        if (isStarlarkValue(sam)) {
          out.add(new Violation("THREAD", cls + "#lambda@" + e.getKey()));
        }
      }
    }
  }

  private boolean isStaticMutable(ClassInfo c, FieldInfo f) {
    String n = f.name();
    if (n.equals("$VALUES") || n.equals("$assertionsDisabled") || n.startsWith("$SwitchMap$")) {
      return false;
    }
    boolean isEnumConstant =
        (c.access & Opcodes.ACC_ENUM) != 0 && f.desc().equals("L" + c.name + ";") && f.isFinal();
    if (isEnumConstant) {
      return false;
    }
    if (!f.isFinal()) {
      return true;
    }
    String d = f.desc();
    boolean mutableType =
        d.startsWith("[") || MUTABLE_COLLECTION.matcher(d).matches() || STATEFUL.matcher(d).find();
    if (!mutableType) {
      return false;
    }
    if (d.startsWith("[")) {
      return true;
    }
    return !c.immutableInit.getOrDefault(n, false);
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

  private ClassInfo info(String name) {
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

  private List<ClassInfo> scannedClasses() throws Exception {
    List<ClassInfo> result = new ArrayList<>();
    for (byte[] b : classBytes(codeSource("com.verygood.security.larky.Larky"), "")) {
      ClassInfo ci = parse(b);
      cache.putIfAbsent(ci.name, ci);
      result.add(ci);
    }
    Path vgs = basedir().resolve("../libstarlark/src/main/vgs").normalize();
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
      return true;
    }
    if (owner.equals("java/util/Collections")) {
      return name.startsWith("unmodifiable")
          || name.startsWith("empty")
          || name.startsWith("singleton");
    }
    if (owner.equals("java/util/List") || owner.equals("java/util/Set") || owner.equals("java/util/Map")) {
      return name.equals("of") || name.equals("copyOf") || name.equals("ofEntries");
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
          public FieldVisitor visitField(int acc, String name, String desc, String sig, Object val) {
            ci.fields.add(new FieldInfo(name, desc, sig, acc));
            return null;
          }

          @Override
          public MethodVisitor visitMethod(
              int acc, String mname, String mdesc, String sig, String[] ex) {
            boolean clinit = mname.equals("<clinit>");
            return new MethodVisitor(Opcodes.ASM9) {
              String lastOwner = "";
              String lastName = "";

              @Override
              public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
                lastOwner = owner;
                lastName = name;
                if (name.equals("checkMutable") || name.equals("isFrozen") || name.equals("isImmutable")) {
                  ci.callsFreezeCheck = true;
                }
              }

              @Override
              public void visitFieldInsn(int op, String owner, String name, String desc) {
                if (clinit && op == Opcodes.PUTSTATIC && owner.equals(ci.name)) {
                  ci.immutableInit.put(name, isImmutableFactory(lastOwner, lastName));
                }
              }

              @Override
              public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... args) {
                boolean capturesThread = false;
                for (Type t : Type.getArgumentTypes(desc)) {
                  capturesThread |= t.getDescriptor().equals(THREAD_DESC);
                }
                if (capturesThread && bsm.getName().equals("metafactory")) {
                  ci.threadLambdas
                      .computeIfAbsent(mname, k -> new TreeSet<>())
                      .add(Type.getReturnType(desc).getInternalName());
                }
              }
            };
          }
        },
        0);
    return ci;
  }

  // ---- allowlist ----------------------------------------------------------------------------

  private static Map<Violation, String> readAllowlist() throws IOException {
    Map<Violation, String> out = new TreeMap<>();
    try (InputStream in = IsolationStructureTest.class.getResourceAsStream("/isolation-allowlist.txt")) {
      assertWithMessage("isolation-allowlist.txt on the test classpath").that(in).isNotNull();
      int lineNo = 0;
      for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
        lineNo++;
        if (line.isBlank() || line.startsWith("#")) {
          continue;
        }
        String[] p = line.split("\t", 3);
        assertWithMessage("allowlist line %s must be KIND<TAB>class#field<TAB>reason: %s", lineNo, line)
            .that(p.length == 3 && !p[2].isBlank())
            .isTrue();
        assertWithMessage("unknown kind on allowlist line %s", lineNo)
            .that(Set.of("THREAD", "NONFINAL", "STATIC", "SHARED_THROWABLE"))
            .contains(p[0]);
        Violation v = new Violation(p[0], p[1]);
        assertWithMessage("duplicate allowlist entry on line %s: %s", lineNo, v)
            .that(out.put(v, p[2]))
            .isNull();
      }
    }
    return out;
  }
}
