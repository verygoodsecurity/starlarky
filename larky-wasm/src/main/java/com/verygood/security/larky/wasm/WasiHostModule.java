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

import static com.verygood.security.larky.wasm.WasiHost.*;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.SplittableRandom;

/** Complete annotated WASI surface and mutable state, created afresh for each run. */
final class WasiHostModule {
  /** The last of WASI's clock ids: realtime (0), monotonic (1), process (2) and thread (3). */
  private static final long CLOCK_THREAD_CPUTIME_ID = 3;

  /**
   * Copies of guest data go through a buffer this size, whatever length the guest asks for, and
   * check for a stop between chunks.
   */
  private static final int CHUNK = 65536;

  private static final byte[] ARGV0 = "module\0".getBytes(StandardCharsets.US_ASCII);
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final byte[] stdin;
  private int stdinPosition;
  private final CappedOutputStream stdout;
  private final CappedOutputStream stderr;
  private final boolean[] open = {true, true, true};
  private final SplittableRandom seeded;
  private long word;
  private int wordBytesLeft;
  private final long deadlineEpochMs;

  WasiHostModule(
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

  @WasiHostFunction(name = "args_sizes_get", signature = "ii:i")
  int argsSizesGet(GuestMemory memory, int argc, int size) {
    writeInt(memory, argc, 1);
    writeInt(memory, size, ARGV0.length);
    return ERRNO_SUCCESS;
  }

  @WasiHostFunction(name = "args_get", signature = "ii:i")
  int argsGet(GuestMemory memory, int argv, int buffer) {
    writeInt(memory, argv, buffer);
    write(memory, buffer, ARGV0, ARGV0.length);
    return ERRNO_SUCCESS;
  }

  @WasiHostFunction(name = "environ_sizes_get", signature = "ii:i")
  int environSizesGet(GuestMemory memory, int count, int size) {
    writeInt(memory, count, 0);
    writeInt(memory, size, 0);
    return ERRNO_SUCCESS;
  }

  @WasiHostFunction(name = "environ_get", signature = "ii:i")
  int environGet(GuestMemory memory, int environ, int buffer) {
    return ERRNO_SUCCESS;
  }

  @WasiHostFunction(name = "clock_time_get", signature = "iIi:i")
  int clockTimeGet(GuestMemory memory, int clock, long precision, int address) {
    if (u32(clock) > CLOCK_THREAD_CPUTIME_ID) {
      return ERRNO_INVAL;
    }
    writeLong(memory, address, 0L);
    return ERRNO_SUCCESS;
  }

  @WasiHostFunction(name = "random_get", signature = "ii:i")
  int randomGet(GuestMemory memory, int address, int length) {
    return randomGetChecked(memory, u32(address), u32(length));
  }

  @WasiHostFunction(name = "fd_read", signature = "iiii:i")
  int fdRead(GuestMemory memory, int fd, int iovs, int count, int nread) {
    return fdReadChecked(memory, fd, u32(iovs), u32(count), u32(nread));
  }

  @WasiHostFunction(name = "fd_write", signature = "iiii:i")
  int fdWrite(GuestMemory memory, int fd, int iovs, int count, int nwritten) {
    return fdWriteChecked(memory, fd, u32(iovs), u32(count), u32(nwritten));
  }

  @WasiHostFunction(name = "fd_close", signature = "i:i")
  int fdClose(GuestMemory memory, int fd) {
    if (!isOpen(fd)) {
      return ERRNO_BADF;
    }
    open[fd] = false;
    return ERRNO_SUCCESS;
  }

  @WasiHostFunction(name = "fd_fdstat_get", signature = "ii:i")
  int fdFdstatGet(GuestMemory memory, int fd, int address) {
    return fdFdstatGetChecked(memory, fd, u32(address));
  }

  @WasiHostFunction(name = "fd_seek", signature = "iIii:i")
  int fdSeek(GuestMemory memory, int fd, long offset, int whence, int address) {
    return isOpen(fd) ? ERRNO_SPIPE : ERRNO_BADF;
  }

  @WasiHostFunction(name = "fd_prestat_get", signature = "ii:i")
  int fdPrestatGet(GuestMemory memory, int fd, int address) {
    return ERRNO_BADF; // no preopened directories
  }

  @WasiHostFunction(name = "fd_prestat_dir_name", signature = "iii:i")
  int fdPrestatDirName(GuestMemory memory, int fd, int address, int length) {
    return ERRNO_BADF; // no preopened directories
  }

  @WasiHostFunction(name = "sched_yield", signature = ":i")
  int schedYield(GuestMemory memory) {
    return ERRNO_SUCCESS;
  }

  @WasiHostFunction(name = "proc_exit", signature = "i:")
  void procExit(GuestMemory memory, int code) {
    throw new ProcExit(code);
  }

  // These declarations recognize the standard ABI without granting files, sockets or other effects.
  @WasiHostFunction(name = "clock_res_get", signature = "ii:i", implemented = false)
  int clockResGet(GuestMemory memory, int clock, int address) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "fd_advise", signature = "iIIi:i", implemented = false)
  int fdAdvise(GuestMemory memory, int fd, long offset, long length, int advice) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "fd_allocate", signature = "iII:i", implemented = false)
  int fdAllocate(GuestMemory memory, int fd, long offset, long length) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "fd_datasync", signature = "i:i", implemented = false)
  int fdDatasync(GuestMemory memory, int fd) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "fd_fdstat_set_flags", signature = "ii:i", implemented = false)
  int fdFdstatSetFlags(GuestMemory memory, int fd, int flags) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "fd_fdstat_set_rights", signature = "iII:i", implemented = false)
  int fdFdstatSetRights(GuestMemory memory, int fd, long rightsBase, long rightsInheriting) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "fd_filestat_get", signature = "ii:i", implemented = false)
  int fdFilestatGet(GuestMemory memory, int fd, int address) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "fd_filestat_set_size", signature = "iI:i", implemented = false)
  int fdFilestatSetSize(GuestMemory memory, int fd, long size) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "fd_filestat_set_times", signature = "iIIi:i", implemented = false)
  int fdFilestatSetTimes(GuestMemory memory, int fd, long atim, long mtim, int flags) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "fd_pread", signature = "iiiIi:i", implemented = false)
  int fdPread(GuestMemory memory, int fd, int iovs, int count, long offset, int nread) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "fd_pwrite", signature = "iiiIi:i", implemented = false)
  int fdPwrite(GuestMemory memory, int fd, int iovs, int count, long offset, int nwritten) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "fd_readdir", signature = "iiiIi:i", implemented = false)
  int fdReaddir(GuestMemory memory, int fd, int buffer, int length, long cookie, int used) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "fd_renumber", signature = "ii:i", implemented = false)
  int fdRenumber(GuestMemory memory, int from, int to) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "fd_sync", signature = "i:i", implemented = false)
  int fdSync(GuestMemory memory, int fd) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "fd_tell", signature = "ii:i", implemented = false)
  int fdTell(GuestMemory memory, int fd, int address) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "path_create_directory", signature = "iii:i", implemented = false)
  int pathCreateDirectory(GuestMemory memory, int fd, int path, int length) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "path_filestat_get", signature = "iiiii:i", implemented = false)
  int pathFilestatGet(GuestMemory memory, int fd, int flags, int path, int length, int address) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "path_filestat_set_times", signature = "iiiiIIi:i", implemented = false)
  int pathFilestatSetTimes(
      GuestMemory memory, int fd, int lookupFlags, int path, int length, long atim, long mtim,
      int flags) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "path_link", signature = "iiiiiii:i", implemented = false)
  int pathLink(
      GuestMemory memory, int oldFd, int oldFlags, int oldPath, int oldLength, int newFd,
      int newPath, int newLength) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "path_open", signature = "iiiiiIIii:i", implemented = false)
  int pathOpen(
      GuestMemory memory, int fd, int lookupFlags, int path, int length, int openFlags,
      long rightsBase, long rightsInheriting, int fdFlags, int openedFd) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "path_readlink", signature = "iiiiii:i", implemented = false)
  int pathReadlink(
      GuestMemory memory, int fd, int path, int pathLength, int buffer, int bufferLength,
      int used) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "path_remove_directory", signature = "iii:i", implemented = false)
  int pathRemoveDirectory(GuestMemory memory, int fd, int path, int length) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "path_rename", signature = "iiiiii:i", implemented = false)
  int pathRename(
      GuestMemory memory, int oldFd, int oldPath, int oldLength, int newFd, int newPath,
      int newLength) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "path_symlink", signature = "iiiii:i", implemented = false)
  int pathSymlink(GuestMemory memory, int oldPath, int oldLength, int fd, int newPath, int newLength) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "path_unlink_file", signature = "iii:i", implemented = false)
  int pathUnlinkFile(GuestMemory memory, int fd, int path, int length) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "poll_oneoff", signature = "iiii:i", implemented = false)
  int pollOneoff(GuestMemory memory, int input, int output, int count, int nevents) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "proc_raise", signature = "i:i", implemented = false)
  int procRaise(GuestMemory memory, int signal) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "sock_accept", signature = "iii:i", implemented = false)
  int sockAccept(GuestMemory memory, int fd, int flags, int openedFd) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "sock_recv", signature = "iiiiii:i", implemented = false)
  int sockRecv(GuestMemory memory, int fd, int iovs, int count, int flags, int nread, int resultFlags) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "sock_send", signature = "iiiii:i", implemented = false)
  int sockSend(GuestMemory memory, int fd, int iovs, int count, int flags, int nwritten) {
    return ERRNO_NOSYS;
  }

  @WasiHostFunction(name = "sock_shutdown", signature = "ii:i", implemented = false)
  int sockShutdown(GuestMemory memory, int fd, int how) {
    return ERRNO_NOSYS;
  }

  private boolean isOpen(int fd) {
    return fd >= 0 && fd < open.length && open[fd];
  }

  private int fdFdstatGetChecked(GuestMemory memory, int fd, long address) {
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

  private int fdReadChecked(GuestMemory memory, int fd, long iovs, long count, long nreadAddress) {
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

  private int fdWriteChecked(GuestMemory memory, int fd, long iovs, long count, long nwrittenAddress) {
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

  private int randomGetChecked(GuestMemory memory, long address, long length) {
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
