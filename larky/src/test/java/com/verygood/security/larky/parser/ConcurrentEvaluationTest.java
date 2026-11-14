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

import static com.google.common.truth.Truth.assertWithMessage;

import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import net.starlark.java.eval.Starlark;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Evaluations running at the same time must not affect each other: scripts that load the same
 * stdlib and vendor modules, run concurrently, must all succeed with the same result as when run
 * alone. Shared loaded modules (ModuleCache) broke this: an elliptic-curve key exchange failed
 * intermittently under concurrent load.
 */
public class ConcurrentEvaluationTest {

  // Uses modules with mutable internal state (elliptic-curve points, ciphers, hashes, counters),
  // with fixed keys and IV so every evaluation has the same result.
  private static final String SCRIPT =
      String.join(
          "\n",
          "load(\"@stdlib//base64\", \"base64\")",
          "load(\"@stdlib//binascii\", \"binascii\")",
          "load(\"@stdlib//json\", \"json\")",
          "load(\"@stdlib//re\", \"re\")",
          "load(\"@vendor//Crypto/PublicKey/ECC\", \"ECC\")",
          "load(\"@vendor//Crypto/Hash\", \"SHA256\")",
          "load(\"@vendor//Crypto/Cipher/AES\", \"AES\")",
          "load(\"@vendor//Crypto/Util/Counter\", \"Counter\")",
          "load(\"@vendor//cryptography/hazmat/primitives\", \"serialization\")",
          "load(\"@vendor//larky_ecdh\", \"LarkyECDH\")",
          "",
          "def der_b64(key):",
          "    return base64.b64encode(key.export_key(format=serialization.Encoding.DER))",
          "",
          "def run():",
          "    client = ECC.construct(curve=\"P-256\", d=1)",
          "    server = ECC.construct(curve=\"P-256\", d=2)",
          "    ecdh = LarkyECDH()",
          "    ecdh.set_public_key(der_b64(server.public_key()), \"X509\")",
          "    ecdh.set_private_key(der_b64(client), \"PKCS8\")",
          "    h = SHA256.new()",
          "    h.update(ecdh.exchange())",
          "    iv = bytes([i for i in range(16)])",
          "    ctr = Counter.new(128, initial_value=int(str(binascii.hexlify(iv)), 16))",
          "    cipher = AES.new(h.digest()[:16], AES.MODE_CTR, counter=ctr)",
          "    body = json.loads('{\"pin\": \"1234\"}')",
          "    data = base64.b64encode(cipher.encrypt(bytes(body[\"pin\"], \"utf-8\"))).decode(\"utf-8\")",
          "    pub = der_b64(client.public_key()).decode(\"utf-8\")",
          "    return re.sub(r\"\\s+\", \"\", json.dumps({\"data\": data, \"pub\": pub}))",
          "",
          "result = run()",
          "");

  private static final int THREADS = 16;
  private static final int EVALUATIONS = 64;

  private boolean wasEnabled;

  @Before
  public void saveCacheSetting() {
    wasEnabled = ModuleCache.isEnabled();
    ModuleCache.clear();
  }

  @After
  public void restoreCacheSetting() {
    ModuleCache.clear();
    ModuleCache.setEnabled(wasEnabled);
  }

  // Through the JSR-223 engine with per-evaluation bindings, as services embedding Larky run it.
  private static String evaluate() throws Exception {
    Bindings bindings = new SimpleBindings();
    bindings.put("ctx", new HashMap<String, Object>());
    ParsedStarFile result = (ParsedStarFile) new LarkyScriptEngine().eval(SCRIPT, bindings);
    return Starlark.str(result.getGlobalEnvironmentVariable("result", Object.class), null);
  }

  /** Runs the script concurrently; returns each distinct outcome and how often it occurred. */
  private static Map<String, Integer> evaluateConcurrently() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    try {
      CountDownLatch start = new CountDownLatch(1);
      List<Future<String>> futures = new ArrayList<>();
      for (int i = 0; i < EVALUATIONS; i++) {
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  try {
                    return evaluate();
                  } catch (Throwable t) {
                    return "FAILED: " + t;
                  }
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

  private static void assertAllMatchASingleEvaluation() throws Exception {
    String expected = evaluate();
    ModuleCache.clear();
    assertWithMessage("outcomes of %s concurrent evaluations", EVALUATIONS)
        .that(evaluateConcurrently())
        .containsExactly(expected, EVALUATIONS);
  }

  /** With the default settings: fails if the module cache is turned on by default while unsafe. */
  @Test
  public void concurrentEvaluationsDoNotInterfere() throws Exception {
    assertAllMatchASingleEvaluation();
  }

  /**
   * The gate for turning the module cache back on: it fails while loaded modules that keep mutable
   * state or their creating thread can be shared.
   */
  @Test
  @Ignore("shared loaded modules are not yet safe under concurrent evaluation")
  public void concurrentEvaluationsDoNotInterfereWithTheModuleCache() throws Exception {
    ModuleCache.setEnabled(true);
    assertAllMatchASingleEvaluation();
  }
}
