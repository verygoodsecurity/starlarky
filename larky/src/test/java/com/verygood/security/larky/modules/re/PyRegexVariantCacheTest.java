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
package com.verygood.security.larky.modules.re;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import com.google.re2j.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Test;

/** A compiled pattern is shared by concurrent evaluations once its module is cached. */
public class PyRegexVariantCacheTest {

  /** asciiMode x 3 dollar modes x prefixed x full. */
  private static final int VARIANTS = 2 * 3 * 2 * 2;
  private static final int ROUNDS = 200;

  private static Pattern variant(PyRegex regex, int i) throws Exception {
    return regex.variant(i / 12 == 1, (i / 4) % 3, (i & 2) != 0, (i & 1) != 0);
  }

  @Test
  public void concurrentMissesOnDifferentVariantsKeepEveryVariant() throws Exception {
    final ExecutorService pool = Executors.newFixedThreadPool(VARIANTS);
    try {
      for (int round = 0; round < ROUNDS; round++) {
        final PyRegex regex = new PyRegex("a+$", 0);
        final CyclicBarrier start = new CyclicBarrier(VARIANTS);
        final List<Future<Pattern>> compiled = new ArrayList<>();
        for (int i = 0; i < VARIANTS; i++) {
          final int key = i;
          compiled.add(pool.submit(() -> {
            start.await();
            return variant(regex, key);
          }));
        }
        for (Future<Pattern> f : compiled) {
          f.get();
        }
        assertEquals("round " + round, VARIANTS, regex.cachedVariantCount());
        for (int i = 0; i < VARIANTS; i++) {
          assertSame("variant " + i, compiled.get(i).get(), variant(regex, i));
        }
      }
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  public void concurrentMissesOnOneVariantReturnOnePattern() throws Exception {
    final int threads = 16;
    final ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      for (int round = 0; round < ROUNDS; round++) {
        final PyRegex regex = new PyRegex("a+$", 0);
        final CyclicBarrier start = new CyclicBarrier(threads);
        final List<Future<Pattern>> compiled = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
          compiled.add(pool.submit(() -> {
            start.await();
            return variant(regex, VARIANTS - 1);
          }));
        }
        final Pattern first = compiled.get(0).get();
        for (Future<Pattern> f : compiled) {
          assertSame("round " + round, first, f.get());
        }
      }
    } finally {
      pool.shutdownNow();
    }
  }
}
