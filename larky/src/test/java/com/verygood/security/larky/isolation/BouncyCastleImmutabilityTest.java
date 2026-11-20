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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Checks, in BouncyCastle's own bytecode, why its EC points and curves are listed in
 * known-immutable-types.txt: nothing writes their fields after construction, except two caches.
 *
 * <ul>
 *   <li>{@code ECCurve.precompute} stores a point's precomputation tables in {@code
 *       ECPoint.preCompTable} while holding the point's lock. The tables are derived only from the
 *       point, so every evaluation computes the same ones.
 *   <li>{@code ECCurve.getMultiplier} creates the curve's multiplier on first use. Two threads may
 *       each create one; either is correct.
 * </ul>
 *
 * <p>Any other write, in any BouncyCastle class, fails this test. A BouncyCastle upgrade that adds
 * one must be reviewed before the types stay listed.
 */
public class BouncyCastleImmutabilityTest {

  private static final String POINT = "org/bouncycastle/math/ec/ECPoint";
  private static final String CURVE = "org/bouncycastle/math/ec/ECCurve";

  /** Writes allowed outside a constructor: "class.method field". */
  private static final Set<String> CACHES =
      Set.of(CURVE + ".precompute preCompTable", CURVE + ".getMultiplier multiplier");

  /**
   * Methods that set up a curve they have just created (cloneCurve) before returning it, so no
   * other thread can see it yet.
   */
  private static final Set<String> BUILDERS = Set.of(CURVE + "$Config.create");

  /** The listed types and their superclasses: the classes whose fields matter. */
  private static final Set<String> covered = new java.util.HashSet<>();

  private static final Map<String, byte[]> classes = new HashMap<>();
  private static final Map<String, String> superOf = new HashMap<>();

  @BeforeAll
  static void readJar() throws Exception {
    Path jar = Path.of(ECPoint.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    try (JarFile f = new JarFile(jar.toFile())) {
      for (JarEntry e : java.util.Collections.list(f.entries())) {
        String n = e.getName();
        if (!n.endsWith(".class") || !n.startsWith("org/bouncycastle/")) {
          continue;
        }
        try (InputStream in = f.getInputStream(e)) {
          byte[] b = in.readAllBytes();
          String name = n.substring(0, n.length() - ".class".length());
          classes.put(name, b);
          superOf.put(name, new ClassReader(b).getSuperName());
        }
      }
    }
    assertWithMessage("ECCurve found in %s", jar).that(classes).containsKey(CURVE);
    for (String type : listed()) {
      for (String c = type.replace('.', '/'); c != null; c = superOf.get(c)) {
        covered.add(c);
      }
    }
  }

  /** The BouncyCastle types in known-immutable-types.txt. */
  private static List<String> listed() throws IOException {
    List<String> names = new ArrayList<>();
    for (String line :
        java.nio.file.Files.readAllLines(Path.of("src/main/config/known-immutable-types.txt"))) {
      if (line.startsWith("org.bouncycastle.")) {
        names.add(line.split("\t", 2)[0]);
      }
    }
    return names;
  }

  private static boolean extendsOrIs(String name, String base) {
    for (String c = name; c != null; c = superOf.get(c)) {
      if (c.equals(base)) {
        return true;
      }
    }
    return false;
  }

  private static boolean pointOrCurve(String owner) {
    return covered.contains(owner);
  }

  /** One finding per write to a point's or curve's state outside a constructor. */
  private static List<String> writes() {
    List<String> found = new ArrayList<>();
    for (Map.Entry<String, byte[]> c : classes.entrySet()) {
      String cls = c.getKey();
      new ClassReader(c.getValue())
          .accept(
              new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(
                    int access, String method, String desc, String sig, String[] ex) {
                  boolean ctor = method.equals("<init>");
                  boolean synced = (access & Opcodes.ACC_SYNCHRONIZED) != 0;
                  return new MethodVisitor(Opcodes.ASM9) {
                    boolean locked = synced;
                    String lastField; // the field the last GETFIELD read, if it was a point's

                    @Override
                    public void visitInsn(int op) {
                      if (op == Opcodes.MONITORENTER) {
                        locked = true;
                      }
                      // an element written into an array read from a point's or curve's field
                      if (op >= Opcodes.IASTORE && op <= Opcodes.SASTORE && lastField != null && !ctor) {
                        found.add(cls + "." + method + " writes an element of " + lastField);
                      }
                      if (op >= Opcodes.IALOAD && op <= Opcodes.SALOAD) {
                        lastField = null; // the element read replaces the array on the stack
                      }
                    }

                    @Override
                    public void visitFieldInsn(int op, String owner, String name, String d) {
                      boolean ours = pointOrCurve(owner);
                      if (op == Opcodes.GETFIELD) {
                        lastField = ours && d.startsWith("[") ? owner + "." + name : null;
                        if (ours && d.equals("Ljava/util/Hashtable;")) {
                          lastTable = owner + "." + name;
                        }
                        return;
                      }
                      if (op != Opcodes.PUTFIELD || !ours) {
                        return;
                      }
                      if (ctor && extendsOrIs(cls, owner)) {
                        return; // a constructor setting its own object's fields
                      }
                      String key = cls + "." + method + " " + name;
                      if (BUILDERS.contains(cls + "." + method)) {
                        return;
                      }
                      if (CACHES.contains(key)
                          && (!name.equals("preCompTable") || locked)) {
                        return;
                      }
                      found.add(cls + "." + method + " writes " + owner + "." + name);
                    }

                    String lastTable;

                    @Override
                    public void visitMethodInsn(
                        int op, String owner, String name, String d, boolean itf) {
                      // changing a point's precomputation table needs the point's lock
                      if (lastTable != null
                          && owner.equals("java/util/Hashtable")
                          && Set.of("put", "remove", "clear", "putAll").contains(name)
                          && !locked) {
                        found.add(cls + "." + method + " changes " + lastTable + " without a lock");
                      }
                    }

                    @Override
                    public void visitTypeInsn(int op, String type) {
                      if (op == Opcodes.ANEWARRAY) {
                        lastField = null; // a new array, not the field's
                      }
                    }

                    @Override
                    public void visitIntInsn(int op, int operand) {
                      if (op == Opcodes.NEWARRAY) {
                        lastField = null;
                      }
                    }

                    @Override
                    public void visitVarInsn(int op, int var) {
                      if (op != Opcodes.ALOAD) {
                        lastField = null;
                      }
                    }
                  };
                }
              },
              ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }
    return found;
  }

  @Test
  void pointsAndCurvesAreWrittenOnlyByTheirConstructorsAndTheTwoCaches() {
    assertWithMessage("writes to BouncyCastle EC point or curve state outside a constructor")
        .that(new TreeSet<>(writes()))
        .isEmpty();
  }

  /** The cache writes the test allows still exist, so the allowance is not stale. */
  @Test
  void theAllowedCachesExist() throws IOException {
    TreeSet<String> seen = new TreeSet<>();
    new ClassReader(classes.get(CURVE))
        .accept(
            new ClassVisitor(Opcodes.ASM9) {
              @Override
              public MethodVisitor visitMethod(
                  int access, String method, String desc, String sig, String[] ex) {
                return new MethodVisitor(Opcodes.ASM9) {
                  @Override
                  public void visitFieldInsn(int op, String owner, String name, String d) {
                    if (op == Opcodes.PUTFIELD) {
                      seen.add(CURVE + "." + method + " " + name);
                    }
                  }
                };
              }
            },
            0);
    assertWithMessage("cache writes in ECCurve").that(seen).containsAtLeastElementsIn(CACHES);
  }

  /** The listed point and curve classes are the ones this test covers. */
  @Test
  void theListedTypesArePointsAndCurves() throws Exception {
    for (String name : listed()) {
      {
        Class<?> c = Class.forName(name);
        assertWithMessage("%s is an EC point or curve", name)
            .that(ECPoint.class.isAssignableFrom(c) || ECCurve.class.isAssignableFrom(c))
            .isTrue();
      }
    }
  }
}
