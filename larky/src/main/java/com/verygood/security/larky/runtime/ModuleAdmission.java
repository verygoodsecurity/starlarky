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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.verygood.security.larky.annot.HostService;
import com.verygood.security.larky.modules.types.NativeMutability;
import com.verygood.security.larky.objects.type.ForwardingLarkyType;
import com.verygood.security.larky.objects.type.LarkyType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.starlark.java.eval.BuiltinFunction;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.ext.ByteList;

/**
 * Decides whether a Larky module that has just finished loading may be shared through the {@link
 * ModuleCache}, by walking everything its globals (and the predeclared values it uses) reach.
 *
 * <p>The walk only reads fields and container contents: it never calls Starlark code ({@code
 * repr}, equality, hashing, iteration of script values) and never formats a value. A module it
 * declines still runs and is used by the evaluation that loaded it; it is just not shared. The
 * result describes what blocked sharing using global, field and class names and collection
 * positions.
 *
 * <p>Each reachable object is, in this order:
 *
 * <ol>
 *   <li>one of Larky's predeclared builtins, the same object in every evaluation: accepted;
 *   <li>a {@link StarlarkThread}: declined, {@link Reason#THREAD};
 *   <li>a native module marked {@link HostService}: declined, {@link Reason#HOST_SERVICE};
 *   <li>a {@link Mutability.Freezable}, {@link Mutability} or Larky class ({@link LarkyType})
 *       that is not frozen: declined, {@link
 *       Reason#OWNED_MUTABLE}. A frozen {@link NativeMutability.Guarded} native value is accepted
 *       without looking at its Java fields; other frozen values (dicts, lists, structs, objects)
 *       are accepted and their contents walked;
 *   <li>an object whose class hierarchy has no instance fields: accepted;
 *   <li>an object whose actual class is final or sealed and annotated Error Prone {@code
 *       Immutable} (which the build checks): accepted;
 *   <li>a libstarlark builtin function: accepted, its receiver walked, its method descriptor not;
 *   <li>one of {@link #KNOWN_IMMUTABLE}, strings, boxed numbers, Starlark ints, floats, bytes and
 *       {@code None}, classes, enums, and compiled syntax ({@link ProgramCache} shares it): accepted;
 *   <li>another module: accepted if the cache admitted it, walked if it is a native module wrapper,
 *       otherwise declined, {@link Reason#DEPENDS_ON};
 *   <li>an object of Larky's or libstarlark's own classes: its fields walked. Unless it is frozen
 *       (a frozen value or Larky class, or a libstarlark value), every instance field must be
 *       final: declined, {@link Reason#MUTABLE_FIELD}, otherwise;
 *   <li>an immutable Guava or JDK collection: its contents walked. A mutable JDK collection or an
 *       array is walked only inside a frozen value, a frozen Larky class or a libstarlark value,
 *       whose writes check that it is frozen;
 *   <li>anything else: declined, {@link Reason#UNKNOWN_CLASS}.
 * </ol>
 */
final class ModuleAdmission {

  /** Why a module was not shared. */
  enum Reason {
    /** It reaches a native module that keeps a host's provider ({@link HostService}). */
    HOST_SERVICE,
    /** It reaches a value whose owner's {@code Mutability} is not frozen. */
    OWNED_MUTABLE,
    /** It reaches an object of a class the walk cannot vouch for. */
    UNKNOWN_CLASS,
    /**
     * It reaches a Larky or libstarlark object with a field that can be reassigned (not final),
     * which is not frozen: nothing stops a later call from changing it.
     */
    MUTABLE_FIELD,
    /** It reaches a {@link StarlarkThread}. */
    THREAD,
    /** It loads, or reaches, a module that is not shared. */
    DEPENDS_ON,
    /** Its walk encounters more than {@link #MAX_NODES} references. */
    TOO_LARGE,
    /** It could not be analyzed (its predeclared names are unknown, or the walk failed). */
    NOT_ANALYZED
  }

  /**
   * Why a module was not shared: the reason, what blocked it (a class name, or the label of a
   * module it depends on), and the path to it from the module's globals, made of global and field
   * names and collection positions. Never a value.
   */
  record Decline(Reason reason, String subject, String path) {
    @Override
    public String toString() {
      return path.isEmpty() ? reason + " " + subject : reason + " " + subject + " at " + path;
    }
  }

  /**
   * Library types accepted as deeply immutable, with Error Prone's {@code
   * -XepOpt:Immutable:KnownImmutable} in larky/pom.xml and the reasons in
   * larky/src/main/config/known-immutable-types.txt (IsolationStructureTest checks the three
   * match).
   */
  /**
   * libstarlark values, other than frozen ones, that the walk trusts to keep the Java arrays and
   * fields it finds in them unchanged. {@code GuardedWritesTest} checks every write to them.
   *
   * <ul>
   *   <li>Tuples: their element arrays are written only while they are built.
   *   <li>A function: its {@code token} is set when a module's top level assigns it, before the
   *       module is frozen; its defaults and free variables are fixed when it is created.
   *   <li>A closure cell: written only by the function call that created it. Starlark has no
   *       {@code nonlocal}, so nothing writes it once that call returns.
   *   <li>A local symbol: its hash code is computed lazily, always to the same value.
   * </ul>
   */
  static final ImmutableSet<String> GUARDING_LIBSTARLARK =
      ImmutableSet.of(
          "net.starlark.java.eval.RegularTuple",
          "net.starlark.java.eval.SingletonTuple",
          "net.starlark.java.eval.StarlarkFunction",
          "net.starlark.java.eval.StarlarkFunction$Cell",
          "net.starlark.java.eval.SymbolGenerator$LocalSymbol");

  static final ImmutableSet<String> KNOWN_IMMUTABLE =
      ImmutableSet.of(
          "com.google.re2j.Pattern",
          "java.util.regex.Pattern",
          "org.bouncycastle.math.ec.custom.sec.SecP256K1Curve",
          "org.bouncycastle.math.ec.custom.sec.SecP256R1Curve",
          "org.bouncycastle.math.ec.custom.sec.SecP384R1Curve",
          "org.bouncycastle.math.ec.custom.sec.SecP521R1Curve",
          "org.bouncycastle.math.ec.custom.sec.SecP256K1Point",
          "org.bouncycastle.math.ec.custom.sec.SecP256R1Point",
          "org.bouncycastle.math.ec.custom.sec.SecP384R1Point",
          "org.bouncycastle.math.ec.custom.sec.SecP521R1Point");

  /** The maximum number of references one module's walk can inspect, including repeated ones. */
  static final int MAX_NODES = 2_000_000;

  private static final boolean NATIVE_IMAGE =
      System.getProperty("org.graalvm.nativeimage.imagecode") != null;

  private static final int MAX_PATH = 240;
  private static final Pattern NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,63}");

  /** How the walk sees the modules a module reaches, other than itself. */
  interface Modules {
    /** Whether {@code module} was admitted to the cache (so is already known to be shareable). */
    boolean admitted(Module module);

    /** Whether {@code module} is a native module wrapper, shared by every evaluation. */
    boolean nativeWrapper(Module module);

    /** The load label of {@code module}, for a {@link Reason#DEPENDS_ON} decline. */
    String label(Module module);
  }

  private ModuleAdmission() {}

  /**
   * Returns why {@code module} must not be shared, or null if it may be.
   *
   * @param predeclared the predeclared values the module's file uses, by name
   * @param sharedBuiltins builtins every evaluation in the process shares whatever the cache does
   *     (Larky's predeclared builtins): accepted wherever they are met, without walking them
   */
  @Nullable
  static Decline check(
      Module module,
      Map<String, Object> predeclared,
      Collection<Object> sharedBuiltins,
      Modules modules) {
    return check(module, predeclared, sharedBuiltins, modules, MAX_NODES);
  }

  /** As above, with a limit on the references inspected (for tests). */
  @Nullable
  static Decline check(
      Module module,
      Map<String, Object> predeclared,
      Collection<Object> sharedBuiltins,
      Modules modules,
      int maxReferences) {
    if (NATIVE_IMAGE) {
      // A native image reflects only the fields registered for it: an unregistered class would
      // look field-less and be accepted. Share nothing rather than guess.
      return new Decline(Reason.NOT_ANALYZED, "native image", "");
    }
    try {
      return new Walk(module, modules, sharedBuiltins, maxReferences).run(predeclared);
    } catch (WalkLimitExceeded e) {
      return new Decline(Reason.TOO_LARGE, maxReferences + " references", e.path);
    } catch (RuntimeException | LinkageError e) {
      // a fault in the walk itself: fail closed, and say so
      return new Decline(Reason.NOT_ANALYZED, e.getClass().getName(), "");
    }
  }

  // ------------------------------------------------------------------------------------------

  private record Node(Object value, String path, @Nullable Object holder) {}

  /** Unwinds the walk at the first over-budget reference, even inside a collection. */
  private static final class WalkLimitExceeded extends RuntimeException {
    private final String path;

    WalkLimitExceeded(String path) {
      super(null, null, false, false);
      this.path = path;
    }
  }

  private static final class Walk {
    private final Module root;
    private final Modules modules;
    private final IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
    private final ArrayDeque<Node> todo = new ArrayDeque<>();
    // Objects found inside a frozen value whose writes go only through that value (see heldBy).
    private final IdentityHashMap<Object, Boolean> guardedHeld = new IdentityHashMap<>();
    private int remainingReferences;

    Walk(Module root, Modules modules, Collection<Object> sharedBuiltins, int maxReferences) {
      this.root = root;
      this.modules = modules;
      this.remainingReferences = maxReferences;
      for (Object b : sharedBuiltins) {
        seen.put(b, Boolean.TRUE); // never queued, so never walked
      }
    }

    @Nullable
    Decline run(Map<String, Object> predeclared) throws RuntimeException {
      seen.put(root, Boolean.TRUE);
      for (String global : root.getExports()) {
        add(root.getGlobal(global), name(global), null);
      }
      for (Map.Entry<String, Object> p : predeclared.entrySet()) {
        add(p.getValue(), "<predeclared " + name(p.getKey()) + ">", null);
      }
      while (!todo.isEmpty()) {
        Node n = todo.poll();
        Decline d = visit(n.value(), n.path(), n.holder());
        if (d != null) {
          return d;
        }
      }
      return null;
    }

    private void add(@Nullable Object value, String path, @Nullable Object holder) {
      // Charge every edge before deduplication so large collections of aliases are bounded too.
      if (remainingReferences <= 0) {
        throw new WalkLimitExceeded(path.length() > MAX_PATH ? shorten(path) : path);
      }
      remainingReferences--;
      if (value != null && !seen.containsKey(value)) {
        seen.put(value, Boolean.TRUE);
        todo.add(new Node(value, path.length() > MAX_PATH ? shorten(path) : path, holder));
      }
    }

    @Nullable
    private Decline visit(Object o, String path, @Nullable Object holder) {
      Class<?> c = o.getClass();
      ClassInfo info = INFO.get(c);
      if (o instanceof StarlarkThread) {
        return new Decline(Reason.THREAD, c.getName(), path);
      }
      if (info.hostService) {
        return new Decline(Reason.HOST_SERVICE, c.getName(), path);
      }
      if (larkyClass(o) && !((LarkyType) o).isFrozenType()) {
        return new Decline(Reason.OWNED_MUTABLE, c.getName(), path);
      }
      if (o instanceof Mutability mu) {
        return mu.isFrozen() ? null : new Decline(Reason.OWNED_MUTABLE, c.getName(), path);
      }
      if (o instanceof Mutability.Freezable f) {
        if (!f.mutability().isFrozen()) {
          return new Decline(Reason.OWNED_MUTABLE, c.getName(), path);
        }
        if (o instanceof NativeMutability.Guarded) {
          return null; // its Java state changes only through methods that check its owner
        }
        // a frozen dict, list, struct or object: its contents are values to check too
      } else if (info.noInstanceFields || info.provenImmutable || info.leaf) {
        return null;
      }
      if (o instanceof Module m) {
        return module(m, path);
      }
      if (o instanceof BuiltinFunction) {
        // the method descriptor is libstarlark's dispatch data; the receiver is a value
        try {
          add(BuiltinReceiver.FIELD.get(o), path + ".<receiver>", o);
        } catch (IllegalAccessException e) {
          return new Decline(Reason.NOT_ANALYZED, BuiltinFunction.class.getName(), path);
        }
        return null;
      }
      if (info.trusted) {
        if (heldBy(o, holder)) {
          guardedHeld.put(o, Boolean.TRUE);
        }
        return fields(o, info, path);
      }
      if (o instanceof ImmutableCollection<?>
          || o instanceof ImmutableMap<?, ?>
          || o instanceof ImmutableMultimap<?, ?>
          || info.jdkImmutableCollection) {
        contents(o, path, holder);
        return null;
      }
      if (o instanceof Optional<?> opt) {
        opt.ifPresent(v -> add(v, path + ".get()", holder));
        return null;
      }
      boolean jdkMutable =
          c.isArray()
              || ((o instanceof Collection<?> || o instanceof Map<?, ?>)
                  && c.getName().startsWith("java.util."));
      if (jdkMutable && guards(holder)) {
        contents(o, path, holder);
        return null;
      }
      return new Decline(Reason.UNKNOWN_CLASS, c.isArray() ? c.getTypeName() : c.getName(), path);
    }

    @Nullable
    private Decline module(Module m, String path) {
      if (modules.admitted(m)) {
        return null; // already checked when it was cached
      }
      if (modules.nativeWrapper(m)) {
        for (String global : m.getExports()) {
          add(m.getGlobal(global), path + "." + name(global), m);
        }
        return null;
      }
      return new Decline(Reason.DEPENDS_ON, modules.label(m), path);
    }

    @Nullable
    private Decline fields(Object o, ClassInfo info, String path) {
      if (info.foreignStateClass != null) {
        // a superclass from outside Larky and libstarlark keeps state of its own
        return new Decline(Reason.UNKNOWN_CLASS, info.foreignStateClass, path);
      }
      if (info.nonFinalField != null && !guards(o)) {
        // a field that can be reassigned, in a value nothing freezes
        return new Decline(Reason.MUTABLE_FIELD, info.nonFinalField, path);
      }
      for (Field f : info.fields) {
        Object v;
        try {
          v = f.get(o);
        } catch (IllegalAccessException e) {
          return new Decline(Reason.UNKNOWN_CLASS, o.getClass().getName(), path + "." + f.getName());
        }
        add(v, path + "." + f.getName(), o);
      }
      return null;
    }

    private void contents(Object o, String path, @Nullable Object holder) {
      if (o.getClass().isArray()) {
        if (o.getClass().getComponentType().isPrimitive()) {
          return;
        }
        int n = Array.getLength(o);
        for (int i = 0; i < n; i++) {
          add(Array.get(o, i), path + "[" + i + "]", holder);
        }
        return;
      }
      // Iterating a JDK or Guava collection runs no Starlark code: their iterators do not call
      // the elements' equals/hashCode.
      if (o instanceof Map<?, ?> map) {
        entries(map.entrySet(), path, holder);
      } else if (o instanceof ImmutableMultimap<?, ?> mm) {
        entries(mm.entries(), path, holder);
      } else if (o instanceof Collection<?> coll) {
        int i = 0;
        for (Object e : coll) {
          add(e, path + "[" + i++ + "]", holder);
        }
      }
    }

    private void entries(
        Iterable<? extends Map.Entry<?, ?>> entries, String path, @Nullable Object holder) {
      int i = 0;
      for (Map.Entry<?, ?> entry : entries) {
        String entryPath = path + "{" + i++ + "}";
        add(entry.getKey(), entryPath + "<key>", holder);
        add(entry.getValue(), entryPath, holder);
      }
    }

    // A mutable JDK collection or array, or a non-final field, is accepted only where its holder
    // controls every write: a frozen value or Larky class (whose writes check that it is frozen),
    // or one of the libstarlark values in GUARDING_LIBSTARLARK. GuardedWritesTest checks those
    // writes in the bytecode.
    private boolean guards(@Nullable Object holder) {
      if (holder == null) {
        return false;
      }
      if (guardedHeld.containsKey(holder)) {
        return true;
      }
      if (holder instanceof Mutability.Freezable f) {
        return f.mutability().isFrozen();
      }
      if (larkyClass(holder)) {
        // a class: its attributes change only through __setattr__/__delattr__, which refuse once
        // the type is frozen; its MRO and bases are set when it is created
        return ((LarkyType) holder).isFrozenType();
      }
      return GUARDING_LIBSTARLARK.contains(holder.getClass().getName());
    }
  }

  // ------------------------------------------------------------------------------------------

  /**
   * {@code o} is the private state of a frozen {@code holder}, written only through the holder's
   * methods, which check its Mutability first: a bytearray's {@link ByteList}. {@code
   * GuardedWritesTest} checks those methods.
   */
  private static boolean heldBy(Object o, @Nullable Object holder) {
    return o instanceof ByteList
        && holder instanceof StarlarkBytes b
        && b instanceof Mutability.Freezable f
        && f.mutability().isFrozen();
  }

  /**
   * A Larky class. A function, method or {@code super} object is a {@link ForwardingLarkyType}
   * too, but it answers for its builtin type ({@code function}, always frozen), not for itself.
   */
  private static boolean larkyClass(Object o) {
    return o instanceof LarkyType && !(o instanceof ForwardingLarkyType);
  }

  private static String name(String s) {
    return NAME.matcher(s).matches() ? s : "<name>";
  }

  private static String shorten(String path) {
    return path.substring(0, MAX_PATH / 2) + "…" + path.substring(path.length() - MAX_PATH / 2);
  }

  // Lookup is lazy so the native-image guard can decline before reflection is needed.
  private static final class BuiltinReceiver {
    private static final Field FIELD = find();

    private static Field find() {
      try {
        Field f = BuiltinFunction.class.getDeclaredField("obj");
        f.setAccessible(true);
        return f;
      } catch (ReflectiveOperationException | RuntimeException e) {
        throw new IllegalStateException("libstarlark's BuiltinFunction has no receiver field", e);
      }
    }
  }

  /** What the walk needs to know about a class, computed once per class. */
  private static final class ClassInfo {
    boolean hostService;
    boolean noInstanceFields;
    boolean provenImmutable;
    boolean leaf;
    boolean trusted;
    boolean jdkImmutableCollection;
    @Nullable String foreignStateClass;
    @Nullable String nonFinalField; // Class#field, the first non-final instance field
    List<Field> fields = ImmutableList.of();
  }

  private static final ClassValue<ClassInfo> INFO =
      new ClassValue<>() {
        @Override
        protected ClassInfo computeValue(Class<?> c) {
          return describe(c);
        }
      };

  private static boolean trustedPackage(Class<?> c) {
    String n = c.getName();
    return n.startsWith("com.verygood.security.larky.") || n.startsWith("net.starlark.java.");
  }

  private static ClassInfo describe(Class<?> c) {
    ClassInfo info = new ClassInfo();
    info.hostService = c.isAnnotationPresent(HostService.class);
    info.noInstanceFields = !c.isArray();
    for (Class<?> k = c; k != null && info.noInstanceFields; k = k.getSuperclass()) {
      for (Field f : k.getDeclaredFields()) {
        if (!Modifier.isStatic(f.getModifiers())) {
          info.noInstanceFields = false;
          break;
        }
      }
    }
    info.provenImmutable =
        c.isAnnotationPresent(com.google.errorprone.annotations.Immutable.class)
            && (Modifier.isFinal(c.getModifiers()) || c.isSealed());
    String n = c.getName();
    info.leaf =
        KNOWN_IMMUTABLE.contains(n)
            || c == String.class
            || c == Boolean.class
            || c == Character.class
            || c == Integer.class
            || c == Long.class
            || c == Short.class
            || c == Byte.class
            || c == Double.class
            || c == Float.class
            || c == BigInteger.class
            || c == BigDecimal.class
            || c == Class.class
            || c.isEnum()
            || (c.getSuperclass() != null && c.getSuperclass().isEnum())
            || StarlarkInt.class.isAssignableFrom(c)
            || c == StarlarkFloat.class
            || c == NoneType.class
            || c == StarlarkBytes.class // bytes; bytearray is a Freezable subclass
            || n.startsWith("net.starlark.java.syntax.")
            || n.equals("net.starlark.java.eval.MethodDescriptor")
            || n.equals("net.starlark.java.eval.ParamDescriptor");
    info.trusted = trustedPackage(c) && !c.isArray();
    info.jdkImmutableCollection =
        n.startsWith("java.util.ImmutableCollections$")
            || n.startsWith("java.util.Collections$Empty")
            || n.startsWith("java.util.Collections$Singleton");
    if (info.trusted) {
      List<Field> fields = new ArrayList<>();
      for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
        boolean ours = trustedPackage(k);
        for (Field f : k.getDeclaredFields()) {
          if (Modifier.isStatic(f.getModifiers())) {
            continue; // not part of this object
          }
          if (ours && info.nonFinalField == null && !Modifier.isFinal(f.getModifiers())) {
            info.nonFinalField = k.getName() + "#" + f.getName();
          }
          if (f.getType().isPrimitive()) {
            continue; // a value
          }
          if (!ours) {
            info.foreignStateClass = k.getName();
            continue;
          }
          try {
            f.setAccessible(true);
          } catch (RuntimeException inaccessible) {
            info.foreignStateClass = k.getName() + "#" + f.getName();
            continue;
          }
          fields.add(f);
        }
      }
      info.fields = ImmutableList.copyOf(fields);
    }
    return info;
  }
}
