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

import java.util.Arrays;
import java.util.Random;

/** Summary statistics, absh's set (n, mean, std, se, min, max, median), plus a paired ratio CI. */
final class Stats {

  final int n;
  final double mean;
  final double std;
  final double se;
  final double min;
  final double max;
  final double median;

  private Stats(double[] values) {
    if (values.length == 0) {
      throw new IllegalArgumentException("no samples");
    }
    double[] sorted = values.clone();
    Arrays.sort(sorted);
    n = sorted.length;
    double sum = 0;
    for (double v : sorted) {
      sum += v;
    }
    mean = sum / n;
    double squares = 0;
    for (double v : sorted) {
      squares += (v - mean) * (v - mean);
    }
    std = n > 1 ? Math.sqrt(squares / (n - 1)) : 0;
    se = std / Math.sqrt(n);
    min = sorted[0];
    max = sorted[n - 1];
    median = quantile(sorted, 0.5);
  }

  static Stats of(double[] values) {
    return new Stats(values);
  }

  /** The {@code q} quantile of sorted values, interpolating linearly between neighbors. */
  static double quantile(double[] sorted, double q) {
    double pos = q * (sorted.length - 1);
    int lo = (int) Math.floor(pos);
    int hi = Math.min(sorted.length - 1, lo + 1);
    return sorted[lo] + (pos - lo) * (sorted[hi] - sorted[lo]);
  }

  static double median(double[] values) {
    double[] sorted = values.clone();
    Arrays.sort(sorted);
    return quantile(sorted, 0.5);
  }

  /**
   * mean(b) / mean(a), with a 95% interval from a bootstrap over the pairs (a[i], b[i]): samples
   * taken in the same block are resampled together, so drift that affects both cancels out. The
   * seed is fixed, so the same samples give the same interval.
   */
  static double[] pairedRatio(double[] a, double[] b, int resamples, long seed) {
    if (a.length != b.length || a.length == 0) {
      throw new IllegalArgumentException("need the same, nonzero number of paired samples");
    }
    int n = a.length;
    double ratio = sum(b) / sum(a);
    Random random = new Random(seed);
    double[] ratios = new double[resamples];
    for (int r = 0; r < resamples; r++) {
      double sa = 0;
      double sb = 0;
      for (int i = 0; i < n; i++) {
        int k = random.nextInt(n);
        sa += a[k];
        sb += b[k];
      }
      ratios[r] = sb / sa;
    }
    Arrays.sort(ratios);
    return new double[] {ratio, quantile(ratios, 0.025), quantile(ratios, 0.975)};
  }

  private static double sum(double[] values) {
    double s = 0;
    for (double v : values) {
      s += v;
    }
    return s;
  }

  private static final char[] BARS = "▁▂▃▄▅▆▇█".toCharArray();

  /**
   * A histogram of {@code values} over [lo, hi] in {@code width} bins, as absh draws it; values
   * above {@code hi} go in the last bin. Each variant is scaled to its own fullest bin, so the
   * shapes compare even when the sample counts differ.
   */
  static String sparkline(double[] values, double lo, double hi, int width) {
    int[] bins = new int[width];
    for (double v : values) {
      int i = hi > lo ? (int) ((v - lo) / (hi - lo) * width) : 0;
      bins[Math.max(0, Math.min(width - 1, i))]++;
    }
    int top = Arrays.stream(bins).max().orElse(0);
    StringBuilder sb = new StringBuilder(width);
    for (int count : bins) {
      sb.append(count == 0 ? ' ' : BARS[Math.min(BARS.length - 1, (count * BARS.length - 1) / top)]);
    }
    return sb.toString();
  }
}
