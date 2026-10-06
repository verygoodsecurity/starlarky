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

package com.verygood.security.larky.wasm;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.SplittableRandom;

/**
 * Larky's WASI preview 1 functions for one run, the same for every runtime: each runtime only
 * adapts its guest memory ({@link GuestMemory}) and calls {@link #call}.
 *
 * <p>The module sees stdin (fd 0), stdout (1) and stderr (2), argv {@code ["module"]}, an empty
 * environment, no preopened directories, a clock that reads 0, and random bytes from a seed or
 * {@code SecureRandom}. {@link #RUNTIME_FUNCTIONS} lists the functions that do something; every
 * other WASI preview 1 function returns {@link #ERRNO_NOSYS}.
 *
 * <p>Every guest pointer and length is a u32 checked against the guest's memory before it is
 * used; a bad one traps ({@link Trap}). Nothing is allocated in proportion to a guest-chosen
 * length, and a call that loops over guest data stops at the run's deadline or an interrupt
 * ({@link Stop}).
 */
final class WasiHost {

  static final String MODULE = "wasi_snapshot_preview1";

  static final int ERRNO_SUCCESS = 0;
  static final int ERRNO_BADF = 8;
  static final int ERRNO_INVAL = 28;
  static final int ERRNO_NOSYS = 52;
  static final int ERRNO_SPIPE = 70;

  /** The WASI functions that do something; every other one returns NOSYS. */
  static final java.util.Set<String> RUNTIME_FUNCTIONS =
      java.util.Set.of(
          "args_get",
          "args_sizes_get",
          "clock_time_get",
          "environ_get",
          "environ_sizes_get",
          "fd_close",
          "fd_fdstat_get",
          "fd_prestat_dir_name",
          "fd_prestat_get",
          "fd_read",
          "fd_seek",
          "fd_write",
          "proc_exit",
          "random_get",
          "sched_yield");

  /**
   * Every WASI preview 1 function's signature: its parameter types, a colon, and its result types,
   * with {@code i} for i32 and {@code I} for i64.
   */
  static final java.util.Map<String, String> SIGNATURES =
      java.util.Map.ofEntries(
          java.util.Map.entry("args_get", "ii:i"),
          java.util.Map.entry("args_sizes_get", "ii:i"),
          java.util.Map.entry("clock_res_get", "ii:i"),
          java.util.Map.entry("clock_time_get", "iIi:i"),
          java.util.Map.entry("environ_get", "ii:i"),
          java.util.Map.entry("environ_sizes_get", "ii:i"),
          java.util.Map.entry("fd_advise", "iIIi:i"),
          java.util.Map.entry("fd_allocate", "iII:i"),
          java.util.Map.entry("fd_close", "i:i"),
          java.util.Map.entry("fd_datasync", "i:i"),
          java.util.Map.entry("fd_fdstat_get", "ii:i"),
          java.util.Map.entry("fd_fdstat_set_flags", "ii:i"),
          java.util.Map.entry("fd_fdstat_set_rights", "iII:i"),
          java.util.Map.entry("fd_filestat_get", "ii:i"),
          java.util.Map.entry("fd_filestat_set_size", "iI:i"),
          java.util.Map.entry("fd_filestat_set_times", "iIIi:i"),
          java.util.Map.entry("fd_pread", "iiiIi:i"),
          java.util.Map.entry("fd_prestat_dir_name", "iii:i"),
          java.util.Map.entry("fd_prestat_get", "ii:i"),
          java.util.Map.entry("fd_pwrite", "iiiIi:i"),
          java.util.Map.entry("fd_read", "iiii:i"),
          java.util.Map.entry("fd_readdir", "iiiIi:i"),
          java.util.Map.entry("fd_renumber", "ii:i"),
          java.util.Map.entry("fd_seek", "iIii:i"),
          java.util.Map.entry("fd_sync", "i:i"),
          java.util.Map.entry("fd_tell", "ii:i"),
          java.util.Map.entry("fd_write", "iiii:i"),
          java.util.Map.entry("path_create_directory", "iii:i"),
          java.util.Map.entry("path_filestat_get", "iiiii:i"),
          java.util.Map.entry("path_filestat_set_times", "iiiiIIi:i"),
          java.util.Map.entry("path_link", "iiiiiii:i"),
          java.util.Map.entry("path_open", "iiiiiIIii:i"),
          java.util.Map.entry("path_readlink", "iiiiii:i"),
          java.util.Map.entry("path_remove_directory", "iii:i"),
          java.util.Map.entry("path_rename", "iiiiii:i"),
          java.util.Map.entry("path_symlink", "iiiii:i"),
          java.util.Map.entry("path_unlink_file", "iii:i"),
          java.util.Map.entry("poll_oneoff", "iiii:i"),
          java.util.Map.entry("proc_exit", "i:"),
          java.util.Map.entry("proc_raise", "i:i"),
          java.util.Map.entry("random_get", "ii:i"),
          java.util.Map.entry("sched_yield", ":i"),
          java.util.Map.entry("sock_accept", "iii:i"),
          java.util.Map.entry("sock_recv", "iiiiii:i"),
          java.util.Map.entry("sock_send", "iiiii:i"),
          java.util.Map.entry("sock_shutdown", "ii:i"));

  /** The last of WASI's clock ids: realtime (0), monotonic (1), process (2) and thread (3). */
  private static final long CLOCK_THREAD_CPUTIME_ID = 3;

  /**
   * Copies of guest data go through a buffer this size, whatever length the guest asks for, and
   * check for a stop between chunks.
   */
  private static final int CHUNK = 65536;

  private static final byte[] ARGV0 = "module\0".getBytes(StandardCharsets.US_ASCII);
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  /** A guest's linear memory, as each runtime exposes it. Addresses are within {@link #size}. */
  interface GuestMemory {
    long size();

    void read(long address, byte[] into, int offset, int length);

    void write(long address, byte[] from, int offset, int length);
  }

  /** Thrown by {@code proc_exit} to unwind the guest; the run returns {@link #code}. */
  static final class ProcExit extends RuntimeException {
    final int code;

    ProcExit(int code) {
      super(null, null, false, false);
      this.code = code;
    }
  }

  /** A pointer or length outside the guest's memory: the run traps, as a guest access would. */
  static final class Trap extends RuntimeException {
    Trap(String message) {
      super(message, null, false, false);
    }
  }

  /** Thrown when the run's deadline passes or its caller is interrupted inside a call. */
  static final class Stop extends RuntimeException {
    Stop() {
      super(null, null, false, false);
    }
  }

  private final byte[] stdin;
  private int stdinPosition;
  private final CappedOutputStream stdout;
  private final CappedOutputStream stderr;
  private final boolean[] open = {true, true, true};
  private final SplittableRandom seeded;
  private long word;
  private int wordBytesLeft;
  private final long deadlineEpochMs;

  WasiHost(
      byte[] stdin,
      CappedOutputStream stdout,
      CappedOutputStream stderr,
      Long randomSeed,
      long deadlineEpochMs) {
    this.stdin = stdin;
    this.stdout = stdout;
    this.stderr = stderr;
    this.seeded = randomSeed != null ? new SplittableRandom(randomSeed) : null;
    this.deadlineEpochMs = deadlineEpochMs;
  }

  /**
   * Runs WASI function {@code name} with the guest's i32/i64 arguments (each in a long) and returns
   * its errno; {@code proc_exit} throws {@link ProcExit} instead.
   */
  int call(String name, long[] args, GuestMemory memory) {
    switch (name) {
      case "args_sizes_get":
        writeInt(memory, args[0], 1);
        writeInt(memory, args[1], ARGV0.length);
        return ERRNO_SUCCESS;
      case "args_get":
        writeInt(memory, args[0], (int) args[1]);
        write(memory, args[1], ARGV0, ARGV0.length);
        return ERRNO_SUCCESS;
      case "environ_sizes_get":
        writeInt(memory, args[0], 0);
        writeInt(memory, args[1], 0);
        return ERRNO_SUCCESS;
      case "environ_get":
        return ERRNO_SUCCESS;
      case "clock_time_get":
        if (u32(args[0]) > CLOCK_THREAD_CPUTIME_ID) {
          return ERRNO_INVAL; // not one of the four clocks WASI defines
        }
        writeLong(memory, args[2], 0L);
        return ERRNO_SUCCESS;
      case "random_get":
        return randomGet(memory, u32(args[0]), u32(args[1]));
      case "fd_read":
        return fdRead(memory, (int) args[0], u32(args[1]), u32(args[2]), u32(args[3]));
      case "fd_write":
        return fdWrite(memory, (int) args[0], u32(args[1]), u32(args[2]), u32(args[3]));
      case "fd_close":
        if (!isOpen((int) args[0])) {
          return ERRNO_BADF;
        }
        open[(int) args[0]] = false;
        return ERRNO_SUCCESS;
      case "fd_fdstat_get":
        return fdFdstatGet(memory, (int) args[0], u32(args[1]));
      case "fd_seek":
        return isOpen((int) args[0]) ? ERRNO_SPIPE : ERRNO_BADF;
      case "fd_prestat_get":
      case "fd_prestat_dir_name":
        return ERRNO_BADF; // no preopened directories
      case "sched_yield":
        return ERRNO_SUCCESS;
      case "proc_exit":
        throw new ProcExit((int) args[0]);
      default:
        return ERRNO_NOSYS;
    }
  }

  private boolean isOpen(int fd) {
    return fd >= 0 && fd < open.length && open[fd];
  }

  private int fdFdstatGet(GuestMemory memory, int fd, long address) {
    if (!isOpen(fd)) {
      return ERRNO_BADF;
    }
    // filetype unknown (a stream, not a terminal), no flags, rights fd_read (0x2) for stdin and
    // fd_write (0x40) for stdout and stderr, nothing inheritable.
    byte[] stat = new byte[24];
    stat[8] = (byte) (fd == 0 ? 0x2 : 0x40);
    write(memory, address, stat, stat.length);
    return ERRNO_SUCCESS;
  }

  private int fdRead(GuestMemory memory, int fd, long iovs, long count, long nreadAddress) {
    if (fd != 0 || !isOpen(fd)) {
      return ERRNO_BADF;
    }
    check(memory, iovs, count * 8);
    check(memory, nreadAddress, 4);
    long total = 0;
    for (long i = 0; i < count && stdinPosition < stdin.length; i++) {
      checkStop(i);
      long buffer = readU32(memory, iovs + 8 * i);
      long length = readU32(memory, iovs + 8 * i + 4);
      check(memory, buffer, length);
      long n = Math.min(length, stdin.length - stdinPosition);
      for (long done = 0; done < n; done += CHUNK) {
        checkStop(0);
        int part = (int) Math.min(CHUNK, n - done);
        memory.write(buffer + done, stdin, stdinPosition, part);
        stdinPosition += part;
      }
      total += n;
    }
    writeInt(memory, nreadAddress, (int) total);
    return ERRNO_SUCCESS;
  }

  private int fdWrite(GuestMemory memory, int fd, long iovs, long count, long nwrittenAddress) {
    if ((fd != 1 && fd != 2) || !isOpen(fd)) {
      return ERRNO_BADF;
    }
    CappedOutputStream out = fd == 1 ? stdout : stderr;
    check(memory, iovs, count * 8);
    check(memory, nwrittenAddress, 4);
    byte[] chunk = null;
    long total = 0;
    for (long i = 0; i < count; i++) {
      checkStop(i);
      long buffer = readU32(memory, iovs + 8 * i);
      long length = readU32(memory, iovs + 8 * i + 4);
      check(memory, buffer, length);
      for (long done = 0; done < length; done += CHUNK) {
        checkStop(0);
        int n = (int) Math.min(CHUNK, length - done);
        if (chunk == null) {
          chunk = new byte[CHUNK];
        }
        memory.read(buffer + done, chunk, 0, n);
        out.write(chunk, 0, n); // throws OutputLimitExceeded past the limit, stopping the guest
      }
      total += length;
    }
    writeInt(memory, nwrittenAddress, (int) total);
    return ERRNO_SUCCESS;
  }

  private int randomGet(GuestMemory memory, long address, long length) {
    check(memory, address, length);
    byte[] chunk = new byte[(int) Math.min(length, CHUNK)];
    for (long done = 0; done < length; done += chunk.length) {
      checkStop(0);
      if (length - done < chunk.length) {
        chunk = new byte[(int) (length - done)];
      }
      fill(chunk);
      memory.write(address + done, chunk, 0, chunk.length);
    }
    return ERRNO_SUCCESS;
  }

  /**
   * The next random bytes: with a seed, {@code new SplittableRandom(seed).nextLong()}s, least
   * significant byte first, as one stream across calls; otherwise {@code SecureRandom}.
   */
  private void fill(byte[] bytes) {
    if (seeded == null) {
      SECURE_RANDOM.nextBytes(bytes);
      return;
    }
    for (int i = 0; i < bytes.length; i++) {
      if (wordBytesLeft == 0) {
        word = seeded.nextLong();
        wordBytesLeft = 8;
      }
      bytes[i] = (byte) word;
      word >>>= 8;
      wordBytesLeft--;
    }
  }

  /** Stops a call that loops over guest data when the deadline passes or the caller is interrupted. */
  private void checkStop(long iteration) {
    if ((iteration & 1023) == 0
        && (Thread.currentThread().isInterrupted()
            || (deadlineEpochMs != 0 && System.currentTimeMillis() >= deadlineEpochMs))) {
      throw new Stop();
    }
  }

  private static long u32(long value) {
    return value & 0xffff_ffffL;
  }

  private static void check(GuestMemory memory, long address, long length) {
    if (address + length > memory.size()) {
      throw new Trap("out of bounds memory access");
    }
  }

  private static long readU32(GuestMemory memory, long address) {
    byte[] b = new byte[4];
    check(memory, address, 4);
    memory.read(address, b, 0, 4);
    return (b[0] & 0xffL) | (b[1] & 0xffL) << 8 | (b[2] & 0xffL) << 16 | (b[3] & 0xffL) << 24;
  }

  private static void writeInt(GuestMemory memory, long address, int value) {
    byte[] b = {(byte) value, (byte) (value >>> 8), (byte) (value >>> 16), (byte) (value >>> 24)};
    write(memory, u32(address), b, 4);
  }

  private static void writeLong(GuestMemory memory, long address, long value) {
    byte[] b = new byte[8];
    for (int i = 0; i < 8; i++) {
      b[i] = (byte) (value >>> (8 * i));
    }
    write(memory, u32(address), b, 8);
  }

  private static void write(GuestMemory memory, long address, byte[] bytes, int length) {
    check(memory, u32(address), length);
    memory.write(u32(address), bytes, 0, length);
  }
}
