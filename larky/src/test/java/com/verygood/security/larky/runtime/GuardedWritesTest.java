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

package com.verygood.security.larky.runtime;

import static com.google.common.truth.Truth.assertWithMessage;

import com.verygood.security.larky.annot.CacheableWhenFrozen;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

/**
 * Checks, in Larky's and libstarlark's bytecode, what {@link ModuleAdmission} trusts: a frozen
 * value, a frozen Larky class, or a value in {@link ModuleAdmission#GUARDING_LIBSTARLARK} keeps the
 * Java state it holds unchanged. The walk accepts a frozen {@link CacheableWhenFrozen} value
 * without looking inside it, and accepts the arrays, collections and non-final fields of the
 * others; a later evaluation sharing the module must not be able to change them.
 *
 * <p>For those classes, every write outside a constructor to
 *
 * <ul>
 *   <li>an instance field,
 *   <li>an element of an array held in a field, or
 *   <li>a {@code java.util} collection held in a field, or
 *   <li>a bytearray's {@code ByteList} (a call to a {@code ByteList} method that changes it)
 * </ul>
 *
 * must be in a method that checks the value's Mutability ({@code checkMutable}, {@code
 * Mutability.isFrozen}, {@code isFrozenType}, {@code ensureNotFrozen}, {@code
 * LarkyFunction.checkUnfrozen}), or in a
 * non-public method all of whose callers do (or are constructors). The rest are listed, each with
 * a reason, in {@code guarded-writes-allowlist.txt}. A new write fails the build, and so does a
 * listed one that no longer exists.
 *
 * <p>The check is per method, not per path: a method that checks on one branch and writes on
 * another passes. It follows values through locals and casts, not through calls or other fields.
 */
public class GuardedWritesTest {

  private static final String FREEZABLE = "net/starlark/java/eval/Mutability$Freezable";
  private static final String LARKY_TYPE = "com/verygood/security/larky/objects/type/LarkyType";
  private static final String FORWARDING_TYPE =
      "com/verygood/security/larky/objects/type/ForwardingLarkyType";
  // A bytearray's ByteList: ModuleAdmission trusts it inside a frozen bytearray, because only the
  // bytearray's methods change it, and they check its Mutability first.
  private static final String HELD_HOLDER = "net/starlark/java/eval/StarlarkBytes";
  private static final String HELD_FIELD = "delegate";
  private static final String HELD_CLASS = "net/starlark/java/ext/ByteList";

  private static final Path ALLOWLIST = Path.of("src/test/resources/guarded-writes-allowlist.txt");

  private static final Set<String> MUTATORS =
      Set.of(
          "put", "putAll", "putIfAbsent", "remove", "removeIf", "removeAll", "retainAll", "clear",
          "add", "addAll", "addFirst", "addLast", "set", "sort", "replaceAll", "compute",
          "computeIfAbsent", "computeIfPresent", "merge", "replace", "poll", "pollFirst",
          "pollLast", "push", "pop", "offer", "offerFirst", "offerLast", "removeFirst",
          "removeLast", "trimToSize", "ensureCapacity");

  /** Larky's and libstarlark's main classes, by internal name. */
  private static final Map<String, ClassNode> MAIN = new HashMap<>();

  @BeforeAll
  static void readClasses() throws Exception {
    readFrom(location(Starlark.class), MAIN);
    readFrom(location(ModuleAdmission.class), MAIN);
    assertWithMessage("classes read").that(MAIN).containsKey(LARKY_TYPE);
  }

  /** The classes this scan reads. */
  private Map<String, ClassNode> classes = MAIN;

  private static Path location(Class<?> c) throws Exception {
    return Path.of(c.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  private static void readFrom(Path p, Map<String, ClassNode> into) throws IOException {
    if (Files.isDirectory(p)) {
      try (Stream<Path> s = Files.walk(p)) {
        for (Path f : (Iterable<Path>) s::iterator) {
          if (f.toString().endsWith(".class")) {
            add(Files.readAllBytes(f), into);
          }
        }
      }
      return;
    }
    try (JarFile j = new JarFile(p.toFile())) {
      for (JarEntry e : java.util.Collections.list(j.entries())) {
        if (e.getName().endsWith(".class")) {
          try (InputStream in = j.getInputStream(e)) {
            add(in.readAllBytes(), into);
          }
        }
      }
    }
  }

  private static void add(byte[] b, Map<String, ClassNode> into) {
    ClassNode n = new ClassNode();
    new ClassReader(b).accept(n, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    into.put(n.name, n);
  }

  // --- which classes the walk trusts ---------------------------------------------------------

  private final Map<String, Boolean> trustedMemo = new HashMap<>();

  /** Instances of {@code name} are trusted by the admission walk to keep their state. */
  private boolean trusted(String name) {
    return trustedMemo.computeIfAbsent(
        name,
        n ->
            ModuleAdmission.GUARDING_LIBSTARLARK.contains(n.replace('/', '.'))
                || isA(n, FREEZABLE)
                || (isA(n, LARKY_TYPE) && !isA(n, FORWARDING_TYPE)));
  }

  private boolean isA(String name, String target) {
    if (name == null) {
      return false;
    }
    if (name.equals(target)) {
      return true;
    }
    ClassNode n = classes.get(name);
    if (n == null) {
      return false;
    }
    for (String i : n.interfaces) {
      if (isA(i, target)) {
        return true;
      }
    }
    return isA(n.superName, target);
  }

  private static boolean container(String desc) {
    return desc.startsWith("[")
        || (desc.startsWith("Ljava/util/")
            && !desc.startsWith("Ljava/util/Optional")
            && !desc.startsWith("Ljava/util/function/"));
  }

  private static boolean check(MethodInsnNode m) {
    return m.name.equals("checkMutable")
        || m.name.equals("checkUnfrozen")
        || m.name.equals("ensureNotFrozen")
        || m.name.equals("isFrozenType")
        || (m.name.equals("isFrozen") && m.owner.equals("net/starlark/java/eval/Mutability"));
  }

  // --- the scan ------------------------------------------------------------------------------

  private static String key(String owner, String name, String desc) {
    return owner + "." + name + desc;
  }

  private final Map<String, MethodNode> methods = new HashMap<>();
  private final Map<String, List<String>> callers = new HashMap<>();
  private final Set<String> checking = new HashSet<>();

  /** "Class.method\twhat it writes", for each unchecked write. */
  private TreeSet<String> unchecked() throws AnalyzerException {
    for (ClassNode c : classes.values()) {
      for (MethodNode m : c.methods) {
        String k = key(c.name, m.name, m.desc);
        methods.put(k, m);
        for (AbstractInsnNode i : m.instructions) {
          if (i instanceof MethodInsnNode mi) {
            if (check(mi)) {
              checking.add(k);
            }
            callers.computeIfAbsent(key(mi.owner, mi.name, mi.desc), x -> new ArrayList<>()).add(k);
          } else if (i instanceof InvokeDynamicInsnNode indy) {
            // a lambda's body runs as part of the method that creates it
            for (Object arg : indy.bsmArgs) {
              if (arg instanceof Handle h) {
                callers.computeIfAbsent(key(h.getOwner(), h.getName(), h.getDesc()), x -> new ArrayList<>()).add(k);
              }
            }
          }
        }
      }
    }
    Rules rules =
        new Rules(
            this::trusted,
            fi -> trusted(fi.owner) && container(fi.desc),
            heldMutators(),
            fi -> fi.name.equals(HELD_FIELD) && isA(fi.owner, HELD_HOLDER));
    TreeSet<String> found = new TreeSet<>();
    for (ClassNode c : classes.values()) {
      for (MethodNode m : c.methods) {
        if (m.instructions.size() == 0 || m.name.equals("<clinit>")) {
          continue;
        }
        for (String what : writes(c, m, rules)) {
          if (!covered(key(c.name, m.name, m.desc), new HashSet<>())) {
            found.add(c.name.replace('/', '.') + "." + m.name + "\t" + what);
          }
        }
      }
    }
    return found;
  }

  /**
   * The methods of {@link #HELD_CLASS} that change the object they are called on (or one passed
   * to them): they write its fields or array, or call another such method.
   */
  private Set<String> heldMutators() throws AnalyzerException {
    Set<String> mutators = new HashSet<>();
    List<ClassNode> held = new ArrayList<>();
    for (ClassNode c : classes.values()) {
      if (isA(c.name, HELD_CLASS)) {
        held.add(c);
      }
    }
    for (boolean changed = true; changed; ) {
      changed = false;
      Rules rules =
          new Rules(
              owner -> isA(owner, HELD_CLASS),
              fi -> isA(fi.owner, HELD_CLASS) && container(fi.desc),
              Set.copyOf(mutators),
              null);
      for (ClassNode c : held) {
        for (MethodNode m : c.methods) {
          String k = key(c.name, m.name, m.desc);
          if (m.instructions.size() == 0
              || m.name.equals("<init>")
              || mutators.contains(k)) {
            continue;
          }
          if (!writes(c, m, rules).isEmpty()) {
            mutators.add(k);
            changed = true;
          }
        }
      }
    }
    return mutators;
  }

  /** The method checks, or it is not public and every caller checks or is a constructor. */
  private boolean covered(String k, Set<String> visiting) {
    if (checking.contains(k) || k.contains(".<init>(")) {
      return true;
    }
    MethodNode m = methods.get(k);
    if (m == null || (m.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0) {
      return false;
    }
    List<String> cs = callers.getOrDefault(k, List.of());
    if (cs.isEmpty() || !visiting.add(k)) {
      return false;
    }
    for (String c : cs) {
      if (!covered(c, visiting)) {
        return false;
      }
    }
    return true;
  }

  /** What a scan counts as a write. */
  private record Rules(
      Predicate<String> fieldOwner, // PUTFIELD on these classes' fields
      Predicate<FieldInsnNode> container, // contents of what these fields hold
      Set<String> mutators, // calls (owner.name+desc) that change their receiver
      Predicate<FieldInsnNode> mutatorReceiver) {} // ... when it came from such a field; null: any

  private List<String> writes(ClassNode c, MethodNode m, Rules rules) throws AnalyzerException {
    List<String> out = new ArrayList<>();
    Frame<SourceValue>[] frames = new Analyzer<>(new SourceInterpreter()).analyze(c.name, m);
    for (int ix = 0; ix < m.instructions.size(); ix++) {
      AbstractInsnNode i = m.instructions.get(ix);
      Frame<SourceValue> f = frames[ix];
      if (f == null) {
        continue; // unreachable
      }
      String what = null;
      int op = i.getOpcode();
      if (op == Opcodes.PUTFIELD) {
        FieldInsnNode fi = (FieldInsnNode) i;
        if (rules.fieldOwner().test(fi.owner)
            && !fresh(f.getStack(f.getStackSize() - 2), m, frames, 0)) {
          what = "sets " + fi.owner.replace('/', '.') + "#" + fi.name;
        }
      } else if (op >= Opcodes.IASTORE && op <= Opcodes.SASTORE) {
        what = held("stores into ", f.getStack(f.getStackSize() - 3), m, frames, rules.container());
      } else if (i instanceof MethodInsnNode mi) {
        int args = Type.getArgumentTypes(mi.desc).length;
        if (op != Opcodes.INVOKESTATIC
            && mi.owner.startsWith("java/util/")
            && MUTATORS.contains(mi.name)) {
          SourceValue receiver = f.getStack(f.getStackSize() - 1 - args);
          what = held("calls " + mi.name + " on ", receiver, m, frames, rules.container());
        } else if (mi.owner.equals("java/util/Arrays")
            && (mi.name.equals("fill") || mi.name.equals("sort"))) {
          SourceValue array = f.getStack(f.getStackSize() - args);
          what = held("calls Arrays." + mi.name + " on ", array, m, frames, rules.container());
        } else if (mi.owner.equals("java/lang/System") && mi.name.equals("arraycopy")) {
          SourceValue dest = f.getStack(f.getStackSize() - 3);
          what = held("copies into ", dest, m, frames, rules.container());
        } else if (op != Opcodes.INVOKESTATIC
            && rules.mutators().contains(key(mi.owner, mi.name, mi.desc))) {
          SourceValue receiver = f.getStack(f.getStackSize() - 1 - args);
          String call = "calls " + mi.owner.replace('/', '.') + "." + mi.name + " on ";
          if (rules.mutatorReceiver() == null) {
            what = fresh(receiver, m, frames, 0) ? null : call + "it";
          } else {
            what = held(call, receiver, m, frames, rules.mutatorReceiver());
          }
        }
      }
      if (what != null) {
        out.add(what);
      }
    }
    return out;
  }

  private String held(
      String verb, SourceValue v, MethodNode m, Frame<SourceValue>[] frames, Predicate<FieldInsnNode> p) {
    String field = heldIn(v, m, frames, 0, p);
    return field == null ? null : verb + field;
  }

  private static Frame<SourceValue> frameAt(MethodNode m, Frame<SourceValue>[] frames, AbstractInsnNode i) {
    return frames[m.instructions.indexOf(i)];
  }

  private static SourceValue top(Frame<SourceValue> f) {
    return f.getStack(f.getStackSize() - 1);
  }

  /** The value is an object this method created, or {@code this} in a constructor. */
  private static boolean fresh(SourceValue v, MethodNode m, Frame<SourceValue>[] frames, int depth) {
    if (depth > 8 || v.insns.isEmpty()) {
      return false;
    }
    for (AbstractInsnNode s : v.insns) {
      int op = s.getOpcode();
      if (op == Opcodes.NEW) {
        continue;
      }
      if (op == Opcodes.ALOAD) {
        int var = ((VarInsnNode) s).var;
        if (var == 0 && m.name.equals("<init>")) {
          continue;
        }
        SourceValue local = frameAt(m, frames, s).getLocal(var);
        if (local.insns.isEmpty()) {
          return false; // a parameter
        }
        for (AbstractInsnNode store : local.insns) {
          Frame<SourceValue> sf = frameAt(m, frames, store);
          if (sf == null || !fresh(top(sf), m, frames, depth + 1)) {
            return false;
          }
        }
        continue;
      }
      if (op == Opcodes.DUP || op == Opcodes.CHECKCAST) {
        if (!fresh(top(frameAt(m, frames, s)), m, frames, depth + 1)) {
          return false;
        }
        continue;
      }
      return false;
    }
    return true;
  }

  /** "Class#field" if the value was read from a field {@code p} accepts, of an older object. */
  private String heldIn(
      SourceValue v, MethodNode m, Frame<SourceValue>[] frames, int depth, Predicate<FieldInsnNode> p) {
    if (depth > 8) {
      return null;
    }
    for (AbstractInsnNode s : v.insns) {
      Frame<SourceValue> sf = frameAt(m, frames, s);
      int op = s.getOpcode();
      String r = null;
      if (op == Opcodes.GETFIELD) {
        FieldInsnNode fi = (FieldInsnNode) s;
        if (p.test(fi) && !fresh(top(sf), m, frames, 0)) {
          return fi.owner.replace('/', '.') + "#" + fi.name;
        }
      } else if (op == Opcodes.ALOAD) {
        for (AbstractInsnNode store : sf.getLocal(((VarInsnNode) s).var).insns) {
          Frame<SourceValue> stf = frameAt(m, frames, store);
          if (stf != null && (r = heldIn(top(stf), m, frames, depth + 1, p)) != null) {
            return r;
          }
        }
      } else if (op == Opcodes.DUP || op == Opcodes.CHECKCAST) {
        if ((r = heldIn(top(sf), m, frames, depth + 1, p)) != null) {
          return r;
        }
      }
    }
    return null;
  }

  // --- the allowlist -------------------------------------------------------------------------

  /** "Class.method\twhat" to its reason. */
  private static Map<String, String> allowlist() throws IOException {
    Map<String, String> entries = new TreeMap<>();
    for (String line : Files.readAllLines(ALLOWLIST, StandardCharsets.UTF_8)) {
      if (line.isBlank() || line.startsWith("#")) {
        continue;
      }
      String[] parts = line.split("\t", 3);
      assertWithMessage("line needs a method, a write and a reason: %s", line)
          .that(parts.length == 3 && !parts[2].isBlank())
          .isTrue();
      entries.put(parts[0] + "\t" + parts[1], parts[2]);
    }
    return entries;
  }

  @Test
  void everyWriteToTrustedStateChecksTheMutabilityOrIsListed() throws Exception {
    TreeSet<String> found = unchecked();
    Map<String, String> allowed = allowlist();
    TreeSet<String> unlisted = new TreeSet<>(found);
    unlisted.removeAll(allowed.keySet());
    TreeSet<String> stale = new TreeSet<>(allowed.keySet());
    stale.removeAll(found);
    assertWithMessage(
            "writes to state the module cache shares, with no Mutability check: add a check, or"
                + " list them in %s with a reason",
            ALLOWLIST)
        .that(unlisted)
        .isEmpty();
    assertWithMessage("entries in %s that no longer occur: delete them", ALLOWLIST)
        .that(stale)
        .isEmpty();
  }

  /** A value the walk would trust, with checked and unchecked writes. */
  static final class Fixture implements Mutability.Freezable {
    private final List<Object> items = new ArrayList<>();
    private Object last;

    @Override
    public Mutability mutability() {
      return Mutability.IMMUTABLE;
    }

    void checked(Object x) throws EvalException {
      Starlark.checkMutable(this);
      items.add(x);
      last = x;
    }

    void unchecked(Object x) {
      List<Object> alias = items;
      alias.add(x);
      last = x;
    }

    private void helper(Object x) {
      last = x;
    }

    void callsHelperAfterChecking(Object x) throws EvalException {
      Starlark.checkMutable(this);
      helper(x);
    }
  }

  /** The scan finds unchecked writes, also through a local, and accepts checked ones. */
  @Test
  void findsUncheckedWritesOnly() throws Exception {
    try (InputStream in =
        Fixture.class.getResourceAsStream("/" + Fixture.class.getName().replace('.', '/') + ".class")) {
      classes = new HashMap<>();
      add(in.readAllBytes(), classes);
    }
    String f = Fixture.class.getName();
    assertWithMessage("unchecked writes in the fixture")
        .that(unchecked())
        .containsExactly(
            f + ".unchecked\tcalls add on " + f + "#items",
            f + ".unchecked\tsets " + f + "#last");
  }
}
