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
package com.verygood.security.larky.parser;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.console.testing.TestingConsole;
import com.verygood.security.larky.modules.crypto.PublicKey.ECC.LarkyECPoint;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.StarlarkInt;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.junit.Test;

/** Values shared between evaluations must not be changed by using them. */
public class ImmutableSharedValuesTest {

  // One module cache for every evaluation in this class, so @vendor//Crypto/PublicKey/ECC (and its
  // curves' generator points) is loaded once and shared, as with the cache turned on in a host.
  private static final ModuleCache CACHE = new ModuleCache(true);

  private static LarkyEvaluator newEvaluator() {
    return new LarkyEvaluator(
        new LarkyScript(LarkyScript.StarlarkMode.STRICT).withModuleCache(CACHE),
        new TestingConsole());
  }

  private static Module eval(String main, String shared) throws Exception {
    ImmutableMap<String, byte[]> files =
        ImmutableMap.of(
            "shared.star", shared.getBytes(StandardCharsets.UTF_8),
            "main.star", main.getBytes(StandardCharsets.UTF_8));
    return newEvaluator().eval(new InMemMapBackedStarFile(files, "main.star")).module();
  }

  // ---- LarkyECPoint -------------------------------------------------------------------------

  private static LarkyECPoint generator() throws Exception {
    X9ECParameters params = CustomNamedCurves.getByName("secp256k1");
    return LarkyECPoint.create(
        params.getCurve(),
        params.getG().normalize().getAffineXCoord().toBigInteger(),
        params.getG().normalize().getAffineYCoord().toBigInteger());
  }

  @Test
  public void ecPointOperationsReturnNewPointsAndLeaveTheReceiverUnchanged() throws Exception {
    LarkyECPoint g = generator();
    Object before = g.asTuple();

    LarkyECPoint twice = g.twice();
    LarkyECPoint negated = g.negate();
    LarkyECPoint sum = g.add(g);
    LarkyECPoint product = g.multiply(StarlarkInt.of(5));

    assertThat(twice).isNotSameInstanceAs(g);
    assertThat(negated).isNotSameInstanceAs(g);
    assertThat(sum).isNotSameInstanceAs(g);
    assertThat(product).isNotSameInstanceAs(g);
    assertThat((Object) g.asTuple()).isEqualTo(before);
    assertThat(twice).isEqualTo(sum);
    assertThat(twice).isNotEqualTo(g);
    assertThat(negated).isNotEqualTo(g);
    assertThat(g.multiply(StarlarkInt.of(2))).isEqualTo(twice);
    assertThat((Object) g.asTuple()).isEqualTo(before);
  }

  @Test
  public void ecPointIsAFinalValueClass() throws Exception {
    for (java.lang.reflect.Field f : LarkyECPoint.class.getDeclaredFields()) {
      if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
        assertWithMessage(f.getName()).that(java.lang.reflect.Modifier.isFinal(f.getModifiers()))
            .isTrue();
      }
    }
  }

  // ---- ECC.star / concurrency -----------------------------------------------------------------

  // Only method calls are made on the shared generator: an operator on a struct of a shared module
  // still dispatches on the thread the struct captured when its module loaded (a separate issue,
  // not related to the point's own state).
  private static final String ECC_SCRIPT =
      String.join(
          "\n",
          "load('@vendor//Crypto/PublicKey/ECC', _curves='curves')",
          "G = _curves['p256'].G",
          "gx0, gy0 = G.xy",
          "d1 = 0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
          "d2 = 0x0fedcba0987654321fedcba0987654321fedcba0987654321fedcba098765432",
          "A = G.copy()",
          "A.__imul__(d1)",
          "B = G.copy()",
          "B.__imul__(d2)",
          "ab = B.copy()",
          "ab.__imul__(d1)",
          "ba = A.copy()",
          "ba.__imul__(d2)",
          "s = G.copy()",
          "s.__iadd__(G)",
          "s.__iadd__(G)",
          "t = G.copy().double()",
          "n = G.__neg__()",
          "# set() used to alias the underlying point, so doubling c doubled G as well",
          "c = G.copy()",
          "c.set(G)",
          "c.double()",
          "gx1, gy1 = G.xy",
          "result = [int(ab.x), int(ab.y), int(ba.x), int(ba.y), int(s.x), int(t.x), int(n.y),",
          "          int(gx0), int(gy0), int(gx1), int(gy1), int(c.x)]",
          "");

  private static List<Object> runEcc() throws Exception {
    Module m = eval(ECC_SCRIPT, "");
    Object r = m.getGlobal("result");
    List<Object> out = new ArrayList<>();
    for (Object o : (Iterable<?>) r) {
      out.add(o instanceof StarlarkInt ? ((StarlarkInt) o).toBigInteger() : o);
    }
    return out;
  }

  @Test
  public void pointOperationsDoNotChangeTheSharedGenerator() throws Exception {
    List<Object> r = runEcc();
    assertThat(r.get(0)).isEqualTo(r.get(2));
    assertThat(r.get(1)).isEqualTo(r.get(3));
    // The generator before and after the operations is the same.
    assertThat(r.get(7)).isEqualTo(r.get(9));
    assertThat(r.get(8)).isEqualTo(r.get(10));
    assertThat(r.get(4)).isNotEqualTo(r.get(5));
    // c = G doubled; t = G doubled
    assertThat(r.get(11)).isEqualTo(r.get(5));
    assertThat(r.get(11)).isNotEqualTo(r.get(9));
  }

  @Test
  public void aSharedEcPointGivesTheSameResultsToConcurrentThreads() throws Exception {
    final int threads = 16;
    final LarkyECPoint g = generator();
    final Object expectedTwice = g.twice().asTuple();
    final Object expectedSum = g.add(g.twice()).asTuple();
    final Object expectedProduct = g.multiply(StarlarkInt.of(123456789)).asTuple();
    final Object expectedNeg = g.negate().asTuple();
    final Object expectedG = g.asTuple();
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      CountDownLatch start = new CountDownLatch(1);
      List<Future<Boolean>> futures = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  for (int j = 0; j < 200; j++) {
                    assertThat((Object) g.twice().asTuple()).isEqualTo(expectedTwice);
                    assertThat((Object) g.add(g.twice()).asTuple()).isEqualTo(expectedSum);
                    assertThat((Object) g.multiply(StarlarkInt.of(123456789)).asTuple())
                        .isEqualTo(expectedProduct);
                    assertThat((Object) g.negate().asTuple()).isEqualTo(expectedNeg);
                    assertThat((Object) g.asTuple()).isEqualTo(expectedG);
                  }
                  return true;
                }));
      }
      start.countDown();
      for (Future<Boolean> f : futures) {
        assertThat(f.get()).isTrue();
      }
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  public void eccAgreesAcrossConcurrentEvaluationsSharingTheLoadedModule() throws Exception {
    final int threads = 16;
    List<Object> expected = runEcc();
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      CountDownLatch start = new CountDownLatch(1);
      List<Future<List<Object>>> futures = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        futures.add(
            pool.submit(
                (Callable<List<Object>>)
                    () -> {
                      start.await();
                      List<Object> last = null;
                      for (int j = 0; j < 5; j++) {
                        last = runEcc();
                        assertThat(last).isEqualTo(expected);
                      }
                      return last;
                    }));
      }
      start.countDown();
      for (Future<List<Object>> f : futures) {
        assertThat(f.get()).isEqualTo(expected);
      }
    } finally {
      pool.shutdownNow();
    }
    assertWithMessage("ECC is shared through the module cache")
        .that(CACHE.cachedPaths().stream().map(String::valueOf).anyMatch(p -> p.contains("Crypto/PublicKey/ECC")))
        .isTrue();
  }

  // ---- multidict ------------------------------------------------------------------------------

  private static String evalError(String main, String shared) {
    try {
      eval(main, shared);
      return null;
    } catch (Throwable t) {
      return String.valueOf(t.getMessage());
    }
  }

  private static final String MD_SHARED =
      String.join(
          "\n",
          "load('@vendor//multidict', _multidict='multidict')",
          "md = _multidict.MultiDict()",
          "md.add('a', '1')",
          "v0 = _multidict.getversion(md)",
          "");

  @Test
  public void aFrozenMultiDictRejectsEveryMutationIncludingTheVersionCounter() {
    for (String mutation :
        new String[] {
          "md.add('b', '2')",
          "md.extend({'c': '3'})",
          "md.popone('a')",
          "md.clear()",
          "md['a'] = '9'",
        }) {
      String error = evalError("load('shared', 'md')\n" + mutation + "\n", MD_SHARED);
      assertWithMessage(mutation).that(error).isNotNull();
    }
  }

  @Test
  public void multiDictVersionIsPerInstanceAndAdvancesOnMutation() throws Exception {
    Module m =
        eval(
            String.join(
                "\n",
                "load('@vendor//multidict', _multidict='multidict')",
                "a = _multidict.MultiDict()",
                "b = _multidict.MultiDict()",
                "v_b0 = _multidict.getversion(b)",
                "v_a0 = _multidict.getversion(a)",
                "a.add('k', 'v')",
                "a.add('k', 'w')",
                "v_a1 = _multidict.getversion(a)",
                "v_b1 = _multidict.getversion(b)",
                ""),
            "");
    assertThat(((StarlarkInt) m.getGlobal("v_a1")).toBigInteger())
        .isEqualTo(((StarlarkInt) m.getGlobal("v_a0")).toBigInteger().add(BigInteger.TWO));
    assertThat(m.getGlobal("v_b1")).isEqualTo(m.getGlobal("v_b0"));
  }
}
