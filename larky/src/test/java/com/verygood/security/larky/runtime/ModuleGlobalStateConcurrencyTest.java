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

package com.verygood.security.larky.runtime;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.verygood.security.larky.parser.InMemMapBackedStarFile;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.ParsedStarFile;
import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.testing.TestingConsole;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * The cache-on gate for modules that keep Java state in their globals.
 *
 * <p>{@link ConcurrentEvaluationTest}'s script creates its ciphers, hashes and EC points inside a
 * function, so each evaluation gets its own. Here each fixture module ({@code
 * src/test/resources/vendor/_isolation_fixtures}) builds such an object at load time and keeps it
 * in a module global. With the module cache on, every evaluation shares the one loaded module and
 * so that object. Each case compares evaluations sharing one cache, run sequentially and at the
 * same time, with the result of an evaluation that loads the module itself.
 *
 * <p>Failure rates before the native owner check were measured by {@link #measure()}
 * ({@code -Dlarky.gate.measure=true -Dlarky.gate.out=FILE [-Dlarky.gate.runs=N]}).
 */
class ModuleGlobalStateConcurrencyTest {

  private static final int THREADS = 16;
  private static final int EVALUATIONS = 64;

  enum Case {
    HASH_COPIED("hash_copied", "digest", 20),
    HASH_MUTATED("hash_mutated", "digest", 20),
    HMAC_PRECOMPUTED("hmac_precomputed", "digest", 20),
    CIPHER_ECB("cipher_ecb", "digest", 20),
    CIPHER_CTR("cipher_ctr", "digest", 20),
    COUNTER_CONFIG("counter_config", "digest", 20),
    ECC_KEY("ecc_key", "digest", 3),
    ECC_KEY_LAZY("ecc_key_lazy", "digest", 1),
    TABLE_NATIVE("table_native", "digest", 20),
    PER_CALL("per_call", "digest", 20),
    ASN1_DECODE("asn1_sequence", "decode_shared", 1),
    CLASSMETHOD_SHARED("classmethod_shared", "class_name", 1),
    STARLARK_DICT("starlark_state", "write_dict", 1),
    STARLARK_LIST("starlark_state", "write_list", 1),
    STARLARK_STRUCT("starlark_state", "write_struct", 1);

    final String module;
    final String function;
    final int calls;

    Case(String module, String function, int calls) {
      this.module = module;
      this.function = function;
      this.calls = calls;
    }

    String label() {
      return "@vendor//_isolation_fixtures/" + module;
    }

    String script() {
      return String.join(
          "\n",
          "load(\"" + label() + "\", \"" + function + "\")",
          "",
          "def run():",
          "    out = []",
          "    for i in range(" + calls + "):",
          "        block = bytes(\"input-%04d-padding\" % i, \"utf-8\")[:16]",
          "        out.append(" + function + "(block))",
          "    return \",\".join(out)",
          "",
          "result = run()",
          "");
    }
  }

  private static String evaluate(Case c, ModuleCache cache) {
    try {
      ParsedStarFile result =
          new CachedTestScript(
                  ModuleSupplier.CORE_MODULES,
                  LarkyScript.StarlarkMode.STRICT,
                  Map.of("ctx", new HashMap<String, Object>()))
              .withModuleCache(cache)
              .evaluate(
                  InMemMapBackedStarFile.createStarFile("main.star", c.script()),
                  new TestingConsole());
      return Starlark.str(result.getGlobalEnvironmentVariable("result", Object.class), null);
    } catch (Throwable t) {
      return "FAILED: " + firstLine(t);
    }
  }

  private static String firstLine(Throwable t) {
    String s = t.toString();
    int nl = s.indexOf('\n');
    return nl < 0 ? s : s.substring(0, nl);
  }

  private static Map<String, Integer> concurrently(Callable<String> evaluation) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    try {
      CountDownLatch start = new CountDownLatch(1);
      List<Future<String>> futures = new ArrayList<>();
      for (int i = 0; i < EVALUATIONS; i++) {
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  return evaluation.call();
                }));
      }
      start.countDown();
      Map<String, Integer> outcomes = new TreeMap<>();
      for (Future<String> f : futures) {
        outcomes.merge(f.get(2, TimeUnit.MINUTES), 1, Integer::sum);
      }
      return outcomes;
    } finally {
      pool.shutdownNow();
    }
  }

  private static Map<String, Integer> sequentially(Callable<String> evaluation) throws Exception {
    Map<String, Integer> outcomes = new TreeMap<>();
    for (int i = 0; i < EVALUATIONS; i++) {
      outcomes.merge(evaluation.call(), 1, Integer::sum);
    }
    return outcomes;
  }

  /** The evaluations sharing one cache, sequentially and then concurrently, match a private load. */
  private static void assertSharingMatches(Case c) throws Exception {
    assertSharingMatches(c, true);
  }

  /**
   * As {@link #assertSharingMatches(Case)}; {@code cached} says whether the cache admits the
   * fixture module or declines it (each evaluation then loads its own).
   */
  private static void assertSharingMatches(Case c, boolean cached) throws Exception {
    String expected = evaluate(c, new ModuleCache(false));

    ModuleCache sequential = new ModuleCache(true);
    Map<String, Integer> seq = sequentially(() -> evaluate(c, sequential));
    assertWithMessage("%s: whether the fixture module was cached", c)
        .that(sequential.cachedPaths().stream().anyMatch(p -> p.contains(c.module)))
        .isEqualTo(cached);
    assertWithMessage("%s: %s sequential evaluations sharing one cache", c, EVALUATIONS)
        .that(seq)
        .containsExactly(expected, EVALUATIONS);

    ModuleCache concurrent = new ModuleCache(true);
    assertWithMessage("%s: %s concurrent evaluations sharing one cache", c, EVALUATIONS)
        .that(concurrently(() -> evaluate(c, concurrent)))
        .containsExactly(expected, EVALUATIONS);
  }

  /**
   * A native value in a module global that a call changes: the value belongs to the module's load,
   * whose Mutability is frozen once the module is loaded, so the call fails as appending to a
   * frozen list does. A private load fails the same way, and so does every evaluation sharing the
   * cached module, sequentially or at the same time: no evaluation sees another's state.
   */
  private static void assertOwnerErrorEverywhere(Case c) throws Exception {
    String expected = evaluate(c, new ModuleCache(false));
    assertWithMessage("%s: a private load", c).that(expected).startsWith("FAILED: ");
    assertWithMessage("%s: a private load", c).that(expected).contains("trying to mutate a frozen");
    assertSharingMatches(c);
  }

  // A hash object kept in a module global and only copied: the shared object is only read.
  @Test
  void hashCopiedFromAModuleGlobal() throws Exception {
    assertSharingMatches(Case.HASH_COPIED);
  }

  // The same object updated by every call. Freezing the module does not stop it: the write goes
  // to the Java digest inside the frozen value, so with the cache on each evaluation continues the
  // previous evaluations' hash.
  // Before the owner check (measured 2026-11-16 over 15 runs of 64): 945/960 sequential and
  // 771/960 concurrent evaluations differed from a private load.
  @Test
  void hashUpdatedThroughAModuleGlobalFailsWithTheOwnerError() throws Exception {
    assertOwnerErrorEverywhere(Case.HASH_MUTATED);
  }

  @Test
  void hmacKeyedHashesCopiedFromModuleGlobals() throws Exception {
    assertSharingMatches(Case.HMAC_PRECOMPUTED);
  }

  // ECB keeps no state between blocks, so sequential sharing gives the same results, but the Java
  // block cipher object is not thread-safe: concurrent encrypts corrupt each other's output or fail
  // (OutputLengthException).
  // Before the owner check (measured 2026-11-16 over 15 runs of 64): 0/960 sequential and
  // 495/960 concurrent evaluations differed from a private load. ECB encryption runs the Java
  // cipher, so it counts as a change even though no state survives the call.
  @Test
  void ecbCipherInAModuleGlobalFailsWithTheOwnerError() throws Exception {
    assertOwnerErrorEverywhere(Case.CIPHER_ECB);
  }

  // CTR keeps its counter in the cipher's Starlark struct, which is frozen with the module: the
  // first encrypt fails with "Attempting to update a frozen structure", with the cache on or off.
  @Test
  void ctrCipherInAModuleGlobalIsFrozen() throws Exception {
    assertSharingMatches(Case.CIPHER_CTR);
  }

  @Test
  void counterConfigurationInAModuleGlobal() throws Exception {
    assertSharingMatches(Case.COUNTER_CONFIG);
  }

  // EC keys whose public points were computed while the module loaded. The cache shares the
  // module: a BouncyCastle point fills only its precomputation tables after construction, under
  // its lock (BouncyCastleImmutabilityTest), and every evaluation matches a private load.
  @Test
  void eccKeyInAModuleGlobal() throws Exception {
    assertSharingMatches(Case.ECC_KEY);
  }

  // An EC key whose public point is first computed by a call: ECC caches it in the key's frozen
  // struct, so the call fails, with the cache on or off.
  @Test
  void eccKeyComputedLazilyIsFrozen() throws Exception {
    assertSharingMatches(Case.ECC_KEY_LAZY);
  }

  // A frozen dict does not freeze the Java object it holds.
  // Before the owner check (measured 2026-11-16 over 15 runs of 64): 945/960 sequential and
  // 809/960 concurrent evaluations differed from a private load.
  @Test
  void hashInAFrozenDictUpdatedFailsWithTheOwnerError() throws Exception {
    assertOwnerErrorEverywhere(Case.TABLE_NATIVE);
  }

  // decode() replaces a native ASN.1 object's contents, so on a frozen one it fails, as appending
  // to a frozen list does. Before the check, with the cache on, an evaluation saw the sequence an
  // earlier evaluation had decoded (1 element instead of 2).
  @Test
  void asn1DecodeIntoAModuleGlobalFailsWithTheOwnerError() throws Exception {
    assertOwnerErrorEverywhere(Case.ASN1_DECODE);
  }

  // A class binds a frozen classmethod by copying it. Before, the first evaluation bound the
  // shared classmethod to its own class, and the next evaluation's C().m() failed ("descriptor 'm'
  // for C objects doesn't apply to a C object": the other evaluation's C).
  @Test
  void classmethodFromAModuleGlobalBindsToEachEvaluationsClass() throws Exception {
    assertWithMessage("a private load")
        .that(evaluate(Case.CLASSMETHOD_SHARED, new ModuleCache(false)))
        .isEqualTo("C,<classmethod _name>");
    assertSharingMatches(Case.CLASSMETHOD_SHARED);
  }

  // Hashes, ciphers, counters and zlib streams created inside the function belong to the calling
  // evaluation and change freely, with the cache on or off.
  @Test
  void nativeObjectsCreatedPerCallStayMutable() throws Exception {
    String expected = evaluate(Case.PER_CALL, new ModuleCache(false));
    assertWithMessage("a private load").that(expected).doesNotContain("FAILED");
    assertSharingMatches(Case.PER_CALL);
  }

  // The root script's own native values behave like its lists: mutable while the script runs,
  // frozen once its evaluation is over, so a later host callback cannot change them.
  @Test
  void rootGlobalsAreMutableDuringTheEvaluationOnly() throws Exception {
    String script =
        String.join(
            "\n",
            "load(\"@vendor//Crypto/Hash/SHA256\", SHA256=\"SHA256\")",
            "_H = SHA256.new()",
            "_L = []",
            "def step(x):",
            "    _H.update(x)",
            "    return _H.hexdigest()",
            "def push(x):",
            "    _L.append(x)",
            "during = step(b\"a\")",
            "push(1)",
            "");
    ParsedStarFile result =
        new CachedTestScript(
                ModuleSupplier.CORE_MODULES,
                LarkyScript.StarlarkMode.STRICT,
                Map.of("ctx", new HashMap<String, Object>()))
            .withModuleCache(new ModuleCache(false))
            .evaluate(InMemMapBackedStarFile.createStarFile("main.star", script), new TestingConsole());
    assertThat(result.getGlobalEnvironmentVariable("during", String.class)).hasLength(64);

    Object step = result.getGlobalEnvironmentVariable("step", Object.class);
    Object push = result.getGlobalEnvironmentVariable("push", Object.class);
    EvalException hash =
        assertThrows(
            EvalException.class,
            () -> result.interop().call(step, StarlarkBytes.immutableOf(new byte[] {'b'})));
    EvalException list =
        assertThrows(
            EvalException.class, () -> result.interop().call(push, StarlarkInt.of(2)));
    assertThat(hash).hasMessageThat().contains("trying to mutate a frozen");
    assertThat(list).hasMessageThat().contains("trying to mutate a frozen list value");
  }


  // Plain Starlark values: freezing rejects the write in both modes.
  @Test
  void starlarkDictWriteIsRejected() throws Exception {
    assertSharingMatches(Case.STARLARK_DICT);
  }

  @Test
  void starlarkListWriteIsRejected() throws Exception {
    assertSharingMatches(Case.STARLARK_LIST);
  }

  @Test
  void starlarkMutableStructWriteIsRejected() throws Exception {
    assertSharingMatches(Case.STARLARK_STRUCT);
  }

  /**
   * Measures every case: per run, how many of the sequential and concurrent evaluations sharing a
   * cache differ from a private load, and whether the fixture module was cached at all. Writes one
   * CSV row per case and run.
   */
  @Test
  @EnabledIfSystemProperty(named = "larky.gate.measure", matches = "true")
  void measure() throws Exception {
    int runs = Integer.getInteger("larky.gate.runs", 5);
    Path out = Path.of(System.getProperty("larky.gate.out", "target/module-global-gate.csv"));
    try (PrintWriter w =
        new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
      w.println("case,run,mode,evaluations,mismatches,distinct_outcomes,module_cached,expected,sample");
      for (Case c : Case.values()) {
        String expected = evaluate(c, new ModuleCache(false));
        for (int run = 0; run < runs; run++) {
          ModuleCache seqCache = new ModuleCache(true);
          row(w, c, run, "sequential", expected, sequentially(() -> evaluate(c, seqCache)), seqCache);
          ModuleCache conCache = new ModuleCache(true);
          row(w, c, run, "concurrent", expected, concurrently(() -> evaluate(c, conCache)), conCache);
        }
        w.flush();
      }
    }
  }

  private static String clip(String s) {
    return s.replace("\"", "'").substring(0, Math.min(160, s.length()));
  }

  private static void row(
      PrintWriter w,
      Case c,
      int run,
      String mode,
      String expected,
      Map<String, Integer> outcomes,
      ModuleCache cache)
      throws IOException {
    int total = outcomes.values().stream().mapToInt(Integer::intValue).sum();
    int mismatches = total - outcomes.getOrDefault(expected, 0);
    String sample =
        outcomes.keySet().stream()
            .filter(k -> !k.equals(expected))
            .findFirst()
            .orElse(expected.startsWith("FAILED") ? expected : "");
    boolean cached = cache.cachedPaths().stream().anyMatch(p -> p.contains(c.module));
    w.printf(
        "%s,%d,%s,%d,%d,%d,%s,\"%s\",\"%s\"%n",
        c,
        run,
        mode,
        total,
        mismatches,
        outcomes.size(),
        cached,
        clip(expected),
        sample.replace("\"", "'").substring(0, Math.min(160, sample.length())));
  }
}
