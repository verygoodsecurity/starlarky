// Copyright 2026 Very Good Security Authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.verygood.security.larky.bench;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class StatsTest {

  @Test
  public void summaryMatchesHandComputedValues() {
    Stats s = Stats.of(new double[] {4, 1, 3, 2});
    assertEquals(4, s.n);
    assertEquals(2.5, s.mean, 1e-12);
    assertEquals(Math.sqrt(5.0 / 3), s.std, 1e-12);
    assertEquals(s.std / 2, s.se, 1e-12);
    assertEquals(1, s.min, 0);
    assertEquals(4, s.max, 0);
    assertEquals(2.5, s.median, 1e-12);
  }

  @Test
  public void quantilesInterpolate() {
    double[] sorted = {10, 20, 30, 40, 50};
    assertEquals(10, Stats.quantile(sorted, 0), 0);
    assertEquals(30, Stats.quantile(sorted, 0.5), 0);
    assertEquals(48, Stats.quantile(sorted, 0.95), 1e-9);
    assertEquals(50, Stats.quantile(sorted, 1), 0);
  }

  @Test
  public void pairedRatioIsTheRatioOfMeansWithADeterministicInterval() {
    double[] a = new double[200];
    double[] b = new double[200];
    for (int i = 0; i < a.length; i++) {
      // shared drift per pair, B 20% faster with some noise
      double drift = 100 + (i % 17);
      a[i] = drift + (i % 5);
      b[i] = 0.8 * drift + (i % 3);
    }
    double[] first = Stats.pairedRatio(a, b, 2000, 1L);
    double[] again = Stats.pairedRatio(a, b, 2000, 1L);
    assertArrayEquals(first, again, 0);
    double sumA = 0;
    double sumB = 0;
    for (int i = 0; i < a.length; i++) {
      sumA += a[i];
      sumB += b[i];
    }
    assertEquals(sumB / sumA, first[0], 1e-12);
    assertTrue(first[1] <= first[0] && first[0] <= first[2]);
    assertTrue("interval " + first[1] + ".." + first[2], first[2] - first[1] < 0.02);
  }

  @Test
  public void pairedRatioNeedsPairs() {
    assertThrows(IllegalArgumentException.class,
        () -> Stats.pairedRatio(new double[] {1}, new double[] {1, 2}, 10, 1));
  }

  @Test
  public void sparklineScalesEachVariantToItsFullestBin() {
    assertEquals("█  ▄", Stats.sparkline(new double[] {0, 0, 3}, 0, 4, 4));
    assertEquals("    ", Stats.sparkline(new double[] {}, 0, 4, 4).replace('\0', ' '));
    // values above the range go in the last bin
    assertEquals("   █", Stats.sparkline(new double[] {99}, 0, 4, 4));
  }

  @Test
  public void optionsDefaultAndRefuseIncompleteCommands() {
    Bench.Options o = Bench.Options.parse(new String[] {"-a", "x.jar", "-b", "y.jar", "-w", "W"});
    assertEquals(3, o.forks);
    assertEquals(200, o.blocks);
    assertEquals(200, o.warmupBlocks);
    assertEquals(60, o.chunk);
    assertEquals(List.of("-Xms512m", "-Xmx1024m"), o.jvmArgs);
    assertThrows(IllegalArgumentException.class,
        () -> Bench.Options.parse(new String[] {"-a", "x.jar", "-w", "W"}));
    assertThrows(IllegalArgumentException.class,
        () -> Bench.Options.parse(new String[] {"-a", "x.jar", "-b", "y.jar", "-w", "W", "--bogus"}));
  }
}
