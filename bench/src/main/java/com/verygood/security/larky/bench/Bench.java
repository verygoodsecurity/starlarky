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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * A/B benchmark of two builds of the same workload, in the style of absh
 * (https://github.com/stepancheg/absh), run in process: each JVM fork loads both builds side by
 * side and alternates between them in blocks on one thread (see {@link Fork}). Reports, for wall
 * time, thread CPU time and thread allocation per call, absh's summary for each build and the B/A
 * ratio with a 95% interval; and per fork, the p50/p95 call latency, CPU and allocation of each.
 */
public final class Bench {

  private static final String USAGE = String.join("\n",
      "usage: java -jar larky-bench.jar -a CLASSPATH -b CLASSPATH -w WORKLOAD [options]",
      "",
      "  -a CLASSPATH          build A (baseline): Larky and libstarlark jars, ':'-separated",
      "  -b CLASSPATH          build B (candidate)",
      "  -w, --workload CLASS  workload class: static Runnable prepare(String spec)",
      "  -s, --spec SPEC       passed to the workload (default: empty)",
      "  --workload-src PATH   .java file or directory, compiled against each build (repeatable)",
      "  --extra-cp CLASSPATH  appended to both builds (e.g. a host application's jars)",
      "  -n, --forks N         JVM forks (default 3); even forks start each block with B",
      "  --blocks N            measured blocks per fork (default 200)",
      "  --warmup-blocks N     warmup blocks per fork (default: --blocks)",
      "  --chunk N             calls per block (default 60)",
      "  -r                    order A and B at random within each block instead of alternating",
      "  --jvm-arg ARG         fork JVM argument (repeatable; default -Xms512m -Xmx1024m)",
      "  --java PATH           java executable for the forks (default: this JVM's)",
      "  -o, --out DIR         results directory (default bench-results/<timestamp>)",
      "  --name-a / --name-b   labels for the report");

  private Bench() {}

  static final class Options {
    String a;
    String b;
    String workload;
    String spec = "";
    final List<Path> sources = new ArrayList<>();
    String extraCp = "";
    int forks = 3;
    int blocks = 200;
    int warmupBlocks = -1;
    int chunk = 60;
    boolean random;
    final List<String> jvmArgs = new ArrayList<>();
    String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    Path out;
    String nameA = "A";
    String nameB = "B";

    static Options parse(String[] args) {
      Options o = new Options();
      for (int i = 0; i < args.length; i++) {
        String arg = args[i];
        switch (arg) {
          case "-a" -> o.a = value(args, ++i, arg);
          case "-b" -> o.b = value(args, ++i, arg);
          case "-w", "--workload" -> o.workload = value(args, ++i, arg);
          case "-s", "--spec" -> o.spec = value(args, ++i, arg);
          case "--workload-src" -> o.sources.add(Path.of(value(args, ++i, arg)));
          case "--extra-cp" -> o.extraCp = value(args, ++i, arg);
          case "-n", "--forks" -> o.forks = Integer.parseInt(value(args, ++i, arg));
          case "--blocks" -> o.blocks = Integer.parseInt(value(args, ++i, arg));
          case "--warmup-blocks" -> o.warmupBlocks = Integer.parseInt(value(args, ++i, arg));
          case "--chunk" -> o.chunk = Integer.parseInt(value(args, ++i, arg));
          case "-r" -> o.random = true;
          case "--jvm-arg" -> o.jvmArgs.add(value(args, ++i, arg));
          case "--java" -> o.java = value(args, ++i, arg);
          case "-o", "--out" -> o.out = Path.of(value(args, ++i, arg));
          case "--name-a" -> o.nameA = value(args, ++i, arg);
          case "--name-b" -> o.nameB = value(args, ++i, arg);
          case "-h", "--help" -> throw new IllegalArgumentException(USAGE);
          default -> throw new IllegalArgumentException("unknown option " + arg + "\n\n" + USAGE);
        }
      }
      if (o.a == null || o.b == null || o.workload == null) {
        throw new IllegalArgumentException(USAGE);
      }
      if (o.forks < 1 || o.blocks < 1 || o.chunk < 1) {
        throw new IllegalArgumentException("--forks, --blocks and --chunk must be positive");
      }
      if (o.warmupBlocks < 0) {
        o.warmupBlocks = o.blocks;
      }
      if (o.jvmArgs.isEmpty()) {
        o.jvmArgs.addAll(List.of("-Xms512m", "-Xmx1024m"));
      }
      if (o.out == null) {
        o.out = Path.of("bench-results",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
      }
      return o;
    }

    private static String value(String[] args, int i, String option) {
      if (i >= args.length) {
        throw new IllegalArgumentException(option + " needs a value");
      }
      return args[i];
    }
  }

  /** What the forks reported. */
  static final class Results {
    /** variant -> per-block {fork, block, calls, wall, cpu, alloc}, in fork and block order. */
    final Map<String, List<long[]>> blocks = new LinkedHashMap<>();
    /** variant -> per-fork {fork, samples, p50, p95, p99} of per-call latency (ns). */
    final Map<String, List<long[]>> latencies = new LinkedHashMap<>();
    final List<String> checks = new ArrayList<>();

    Results() {
      for (String v : List.of("A", "B")) {
        blocks.put(v, new ArrayList<>());
        latencies.put(v, new ArrayList<>());
      }
    }
  }

  public static void main(String[] args) throws Exception {
    Options o;
    try {
      o = Options.parse(args);
    } catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
      System.exit(2);
      return;
    }
    Files.createDirectories(o.out.resolve("logs"));
    String cpA = withWorkloads(o, o.a, "A");
    String cpB = withWorkloads(o, o.b, "B");
    String self = Path.of(Bench.class.getProtectionDomain().getCodeSource().getLocation().toURI())
        .toString();
    Results results = new Results();
    for (int fork = 1; fork <= o.forks; fork++) {
      boolean swap = fork % 2 == 0;
      long seed = o.random ? 0x5EED_0000L + fork : 0;
      List<String> command = new ArrayList<>();
      command.add(o.java);
      command.addAll(o.jvmArgs);
      command.addAll(List.of("-cp", self, Fork.class.getName(), cpA, cpB, o.workload, o.spec,
          Boolean.toString(swap), Integer.toString(o.blocks), Integer.toString(o.warmupBlocks),
          Integer.toString(o.chunk), Long.toString(seed)));
      System.err.printf(Locale.ROOT, "fork %d/%d (%s first)...%n", fork, o.forks, swap ? "B" : "A");
      runFork(command, o.out.resolve("logs").resolve("fork-" + fork + ".log"), fork, results);
    }
    String report = report(o, results);
    System.out.print(report);
    writeCsv(o.out, results);
    Files.writeString(o.out.resolve("summary.md"), "```\n" + report + "```\n\n" + markdown(o, results));
    System.err.println("results: " + o.out.toAbsolutePath());
  }

  /** The build's classpath, behind the workload sources compiled against it, plus --extra-cp. */
  private static String withWorkloads(Options o, String build, String label) throws IOException {
    String classpath = o.extraCp.isEmpty() ? build : build + File.pathSeparator + o.extraCp;
    if (o.sources.isEmpty()) {
      return classpath;
    }
    List<String> files = new ArrayList<>();
    for (Path source : o.sources) {
      if (Files.isDirectory(source)) {
        try (Stream<Path> walk = Files.walk(source)) {
          walk.filter(p -> p.toString().endsWith(".java")).forEach(p -> files.add(p.toString()));
        }
      } else {
        files.add(source.toString());
      }
    }
    Path classes = o.out.resolve("workload-classes-" + label);
    Files.createDirectories(classes);
    JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
    List<String> args = new ArrayList<>(List.of(
        "-proc:none", "-nowarn", "-encoding", "UTF-8", "-cp", classpath, "-d", classes.toString()));
    args.addAll(files);
    int status = javac.run(null, null, null, args.toArray(String[]::new));
    if (status != 0) {
      throw new IllegalStateException("compiling the workload against build " + label + " failed");
    }
    return classes + File.pathSeparator + classpath;
  }

  private static void runFork(List<String> command, Path log, int fork, Results results)
      throws IOException, InterruptedException {
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    int parities = 0;
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        PrintWriter writer = new PrintWriter(Files.newBufferedWriter(log))) {
      String line;
      while ((line = reader.readLine()) != null) {
        writer.println(line);
        if (!line.startsWith("bench,")) {
          continue;
        }
        String[] f = line.split(",", -1);
        switch (f[1]) {
          case "block" -> results.blocks.get(f[2]).add(new long[] {fork, Long.parseLong(f[3]),
              Long.parseLong(f[4]), Long.parseLong(f[5]), Long.parseLong(f[6]), Long.parseLong(f[7])});
          case "latency" -> results.latencies.get(f[2]).add(new long[] {fork, Long.parseLong(f[3]),
              Long.parseLong(f[4]), Long.parseLong(f[5]), Long.parseLong(f[6])});
          case "parity" -> {
            parities++;
            results.checks.add(String.format(Locale.ROOT, "fork %d: outputs %s %s", fork,
                "none".equals(f[3]) ? "not compared" : "equal", f[2]));
          }
          case "describe" -> {
            if (fork == 1) {
              results.checks.add("build " + f[2] + ": " + f[3]);
            }
          }
          default -> {
          }
        }
      }
    }
    int status = process.waitFor();
    if (status != 0 || parities != 2) {
      List<String> lines = Files.readAllLines(log);
      List<String> tail = lines.subList(Math.max(0, lines.size() - 30), lines.size());
      throw new IllegalStateException("fork " + fork + " failed (exit " + status + "); see " + log
          + "\n" + String.join("\n", tail));
    }
  }

  /** Per-call value of each block for column {@code column} (3 wall, 4 cpu, 5 alloc), scaled. */
  static double[] perCall(List<long[]> blocks, int column, double scale) {
    double[] values = new double[blocks.size()];
    for (int i = 0; i < values.length; i++) {
      long[] b = blocks.get(i);
      values[i] = b[column] / (double) b[2] / scale;
    }
    return values;
  }

  static String report(Options o, Results r) {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format(Locale.ROOT, "A: %s%nB: %s%nworkload: %s %s%n", o.nameA, o.nameB,
        o.workload, o.spec));
    sb.append(String.format(Locale.ROOT,
        "%d forks x %d blocks x %d calls per build (%d warmup blocks per fork)%n%n",
        o.forks, o.blocks, o.chunk, o.warmupBlocks));
    String[][] metrics = {{"wall", "3", "1000", "us/call"}, {"cpu", "4", "1000", "us/call"},
        {"alloc", "5", "1024", "KiB/call"}};
    for (String[] m : metrics) {
      double[] a = perCall(r.blocks.get("A"), Integer.parseInt(m[1]), Double.parseDouble(m[2]));
      double[] b = perCall(r.blocks.get("B"), Integer.parseInt(m[1]), Double.parseDouble(m[2]));
      Stats sa = Stats.of(a);
      Stats sb2 = Stats.of(b);
      double[] both = new double[a.length + b.length];
      System.arraycopy(a, 0, both, 0, a.length);
      System.arraycopy(b, 0, both, a.length, b.length);
      java.util.Arrays.sort(both);
      double lo = both[0];
      double hi = Stats.quantile(both, 0.99);
      sb.append(m[0]).append(' ').append(m[3]).append(" (one sample per block)\n");
      sb.append(absh("A", sa)).append(absh("B", sb2));
      sb.append("A: ").append(Stats.sparkline(a, lo, hi, 30)).append('\n');
      sb.append("B: ").append(Stats.sparkline(b, lo, hi, 30)).append('\n');
      double[] ratio = Stats.pairedRatio(a, b, 2000, 1L);
      sb.append(String.format(Locale.ROOT, "B/A: %.3f %.3f..%.3f (95%% conf)%n%n",
          ratio[0], ratio[1], ratio[2]));
    }
    for (String check : r.checks) {
      sb.append(check).append('\n');
    }
    return sb.toString();
  }

  private static String absh(String label, Stats s) {
    return String.format(Locale.ROOT, "%s: n=%d mean=%.3f std=%.3f se=%.3f min=%.3f max=%.3f median=%.3f%n",
        label, s.n, s.mean, s.std, s.se, s.min, s.max, s.median);
  }

  /** Our table: per fork, p50/p95 latency, CPU and allocation per call for each build. */
  static String markdown(Options o, Results r) {
    StringBuilder sb = new StringBuilder();
    sb.append("| Fork | First | p50 µs A → B | p95 µs A → B | CPU µs/call A → B | KiB/call A → B |\n");
    sb.append("|---:|:---:|---:|---:|---:|---:|\n");
    int[] improved = new int[4];
    double[][] medians = new double[4][];
    List<double[]> rows = new ArrayList<>();
    for (int fork = 1; fork <= o.forks; fork++) {
      long[] la = latency(r, "A", fork);
      long[] lb = latency(r, "B", fork);
      double[] ca = forkTotals(r.blocks.get("A"), fork);
      double[] cb = forkTotals(r.blocks.get("B"), fork);
      double[] row = {la[2] / 1e3, lb[2] / 1e3, la[3] / 1e3, lb[3] / 1e3, ca[0] / 1e3, cb[0] / 1e3,
          ca[1] / 1024, cb[1] / 1024};
      rows.add(row);
      for (int k = 0; k < 4; k++) {
        if (row[2 * k + 1] < row[2 * k]) {
          improved[k]++;
        }
      }
      sb.append(String.format(Locale.ROOT,
          "| %d | %s | %.1f → %.1f | %.1f → %.1f | %.1f → %.1f | %.1f → %.1f |%n", fork,
          fork % 2 == 0 ? "B" : "A", row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7]));
    }
    for (int k = 0; k < 8; k++) {
      final int col = k;
      if (k % 2 == 0) {
        medians[k / 2] = new double[] {
            Stats.median(rows.stream().mapToDouble(x -> x[col]).toArray()),
            Stats.median(rows.stream().mapToDouble(x -> x[col + 1]).toArray())};
      }
    }
    sb.append(String.format(Locale.ROOT,
        "| median | | %.1f → %.1f (%.3f) | %.1f → %.1f (%.3f) | %.1f → %.1f (%.3f) | %.1f → %.1f (%.3f) |%n",
        medians[0][0], medians[0][1], medians[0][1] / medians[0][0],
        medians[1][0], medians[1][1], medians[1][1] / medians[1][0],
        medians[2][0], medians[2][1], medians[2][1] / medians[2][0],
        medians[3][0], medians[3][1], medians[3][1] / medians[3][0]));
    sb.append(String.format(Locale.ROOT,
        "%nB lower than A in p50 %d/%d, p95 %d/%d, CPU %d/%d, allocation %d/%d forks.%n",
        improved[0], o.forks, improved[1], o.forks, improved[2], o.forks, improved[3], o.forks));
    return sb.toString();
  }

  private static long[] latency(Results r, String variant, int fork) {
    return r.latencies.get(variant).stream().filter(l -> l[0] == fork).findFirst()
        .orElseThrow(() -> new IllegalStateException("no latency for " + variant + " fork " + fork));
  }

  /** {cpu ns per call, allocated bytes per call} over a fork's blocks. */
  private static double[] forkTotals(List<long[]> blocks, int fork) {
    long calls = 0;
    long cpu = 0;
    long alloc = 0;
    for (long[] b : blocks) {
      if (b[0] == fork) {
        calls += b[2];
        cpu += b[4];
        alloc += b[5];
      }
    }
    return new double[] {cpu / (double) calls, alloc / (double) calls};
  }

  private static void writeCsv(Path out, Results r) {
    try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out.resolve("blocks.csv")));
        PrintWriter l = new PrintWriter(Files.newBufferedWriter(out.resolve("latency.csv")))) {
      w.println("variant,fork,block,calls,wall_ns,cpu_ns,alloc_bytes");
      l.println("variant,fork,samples,p50_ns,p95_ns,p99_ns");
      for (String v : List.of("A", "B")) {
        for (long[] b : r.blocks.get(v)) {
          w.printf(Locale.ROOT, "%s,%d,%d,%d,%d,%d,%d%n", v, b[0], b[1], b[2], b[3], b[4], b[5]);
        }
        for (long[] x : r.latencies.get(v)) {
          l.printf(Locale.ROOT, "%s,%d,%d,%d,%d,%d%n", v, x[0], x[1], x[2], x[3], x[4]);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
