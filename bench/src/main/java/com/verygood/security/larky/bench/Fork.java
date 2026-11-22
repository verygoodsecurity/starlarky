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

import java.io.File;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/**
 * One JVM fork: loads the workload once per variant, each in its own class loader (whose parent is
 * the platform loader, so the variants share nothing but the JDK), and runs both on this thread in
 * alternating blocks. Prints {@code bench,...} lines for {@link Bench} to parse; anything else the
 * workload prints goes to the fork's log.
 *
 * <p>Arguments: {@code A-classpath B-classpath workload-class spec swap blocks warmup-blocks chunk
 * seed}, where {@code swap} starts the first block with B, and a {@code seed} other than 0 orders
 * the two variants within each block at random instead of alternating.
 */
public final class Fork {

  private static final com.sun.management.ThreadMXBean THREADS =
      (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

  private static final PrintStream OUT = System.out;

  private Fork() {}

  /** One variant: its class loader, its workload's task and hooks, and its measurements. */
  static final class Variant {
    final String label;
    final URLClassLoader loader;
    final Runnable task;
    final Method fingerprint;
    final String spec;
    final long[] latencies;

    Variant(String label, String classpath, String workload, String spec, int samples)
        throws Exception {
      this.label = label;
      this.loader = isolate(classpath);
      this.spec = spec;
      this.latencies = new long[samples];
      activate();
      Class<?> type = loader.loadClass(workload);
      Object task = type.getMethod("prepare", String.class).invoke(null, spec);
      if (!(task instanceof Runnable runnable)) {
        throw new IllegalStateException(workload + ".prepare must return a Runnable, got " + task);
      }
      this.task = runnable;
      this.fingerprint = optional(type, String.class, "fingerprint", "inspect");
      Method describe = optional(type, null, "describe", "sourceJar");
      if (describe != null) {
        line("describe", label, String.valueOf(describe.invoke(null)));
      }
    }

    void activate() {
      Thread.currentThread().setContextClassLoader(loader);
    }

    /** Runs {@code calls} calls; if {@code record}, returns {wall, cpu, allocated} and keeps latencies. */
    long[] run(int calls, boolean record, int offset) {
      activate();
      long thread = Thread.currentThread().threadId();
      long alloc0 = THREADS.getThreadAllocatedBytes(thread);
      long cpu0 = THREADS.getCurrentThreadCpuTime();
      long wall0 = System.nanoTime();
      for (int i = 0; i < calls; i++) {
        long t0 = System.nanoTime();
        task.run();
        if (record) {
          latencies[offset + i] = System.nanoTime() - t0;
        }
      }
      long wall = System.nanoTime() - wall0;
      long cpu = THREADS.getCurrentThreadCpuTime() - cpu0;
      long alloc = THREADS.getThreadAllocatedBytes(thread) - alloc0;
      return new long[] {wall, cpu, alloc};
    }

    String fingerprint() throws Exception {
      if (fingerprint == null) {
        return null;
      }
      activate();
      return (String) fingerprint.invoke(null, spec);
    }
  }

  private static Method optional(Class<?> type, Class<?> parameter, String... names) {
    for (String name : names) {
      try {
        return parameter == null ? type.getMethod(name) : type.getMethod(name, parameter);
      } catch (NoSuchMethodException e) {
        // try the next name
      }
    }
    return null;
  }

  static URLClassLoader isolate(String classpath) throws Exception {
    String[] parts = classpath.split(File.pathSeparator);
    URL[] urls = new URL[parts.length];
    for (int i = 0; i < parts.length; i++) {
      urls[i] = Path.of(parts[i]).toUri().toURL();
    }
    return new URLClassLoader(urls, ClassLoader.getPlatformClassLoader());
  }

  static void line(Object... fields) {
    StringBuilder sb = new StringBuilder("bench");
    for (Object f : fields) {
      sb.append(',').append(String.valueOf(f).replace('\n', ' ').replace(',', ';'));
    }
    OUT.println(sb);
  }

  private static void parity(Variant a, Variant b, String point) throws Exception {
    String fa = a.fingerprint();
    String fb = b.fingerprint();
    if (fa == null && fb == null) {
      line("parity", point, "none");
      return;
    }
    if (fa == null || !fa.equals(fb)) {
      throw new AssertionError("outputs differ " + point + ": A=" + fa + " B=" + fb);
    }
    line("parity", point, fa);
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 9) {
      throw new IllegalArgumentException(
          "usage: Fork A-cp B-cp workload spec swap blocks warmup-blocks chunk seed");
    }
    String workload = args[2];
    String spec = args[3];
    boolean swap = Boolean.parseBoolean(args[4]);
    int blocks = Integer.parseInt(args[5]);
    int warmupBlocks = Integer.parseInt(args[6]);
    int chunk = Integer.parseInt(args[7]);
    long seed = Long.parseLong(args[8]);
    THREADS.setThreadAllocatedMemoryEnabled(true);
    THREADS.setThreadCpuTimeEnabled(true);
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    try {
      Variant a = new Variant("A", args[0], workload, spec, blocks * chunk);
      Variant b = new Variant("B", args[1], workload, spec, blocks * chunk);
      Random random = seed == 0 ? null : new Random(seed);
      for (int block = 0; block < warmupBlocks; block++) {
        for (Variant v : order(a, b, block, swap, random)) {
          v.run(chunk, false, 0);
        }
      }
      parity(a, b, "after-warmup");
      for (int block = 0; block < blocks; block++) {
        for (Variant v : order(a, b, block, swap, random)) {
          long[] m = v.run(chunk, true, block * chunk);
          line("block", v.label, block, chunk, m[0], m[1], m[2]);
        }
      }
      parity(a, b, "after-measurement");
      for (Variant v : new Variant[] {a, b}) {
        long[] sorted = v.latencies.clone();
        Arrays.sort(sorted);
        line("latency", v.label, sorted.length, at(sorted, 0.50), at(sorted, 0.95), at(sorted, 0.99));
      }
      line("done", String.format(Locale.ROOT, "warmup_calls=%d", warmupBlocks * chunk));
    } catch (InvocationTargetException e) {
      throw new IllegalStateException("workload failed", e.getCause());
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }

  private static long at(long[] sorted, double q) {
    return sorted[Math.min(sorted.length - 1, (int) Math.floor(q * sorted.length))];
  }

  /** Alternates which variant runs first (B first on odd blocks, flipped when swapped), or random. */
  static Variant[] order(Variant a, Variant b, int block, boolean swap, Random random) {
    boolean bFirst = random != null ? random.nextBoolean() : ((block & 1) == 1) != swap;
    return bFirst ? new Variant[] {b, a} : new Variant[] {a, b};
  }
}
