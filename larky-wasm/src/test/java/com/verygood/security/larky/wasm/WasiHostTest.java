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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class WasiHostTest {

  private static final int CHUNK = 65536;

  /** A guest memory that can interrupt the calling thread when data (not an iovec) is copied. */
  private static final class Memory implements WasiHost.GuestMemory {
    final byte[] bytes = new byte[1 << 20];
    final boolean interrupts;
    int dataCopies;

    Memory(boolean interrupts) {
      this.interrupts = interrupts;
    }

    @Override
    public long size() {
      return bytes.length;
    }

    @Override
    public void read(long address, byte[] into, int offset, int length) {
      System.arraycopy(bytes, (int) address, into, offset, length);
      if (length > 4) { // not an iovec field
        copied();
      }
    }

    @Override
    public void write(long address, byte[] from, int offset, int length) {
      System.arraycopy(from, offset, bytes, (int) address, length);
      if (length > 8) { // not an errno, count or timestamp
        copied();
      }
    }

    private void copied() {
      dataCopies++;
      if (interrupts) {
        Thread.currentThread().interrupt();
      }
    }

    /** One iovec at address 0 pointing at {@code length} bytes from 1024. */
    void iovec(int length) {
      ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(0, 1024).putInt(4, length);
    }
  }

  @After
  public void clearInterrupt() {
    Thread.interrupted();
  }

  private static WasiHost host(byte[] stdin, CappedOutputStream stdout) {
    return new WasiHost(stdin, stdout, new CappedOutputStream("stderr", 0), 0L, 0);
  }

  @Test
  public void fdWriteStopsBetweenChunksOfOneIovec() {
    Memory memory = new Memory(true);
    memory.iovec(8 * CHUNK);
    CappedOutputStream stdout = new CappedOutputStream("stdout", 1 << 20);
    WasiHost wasi = host(new byte[0], stdout);
    assertThrows(WasiHost.Stop.class, () -> wasi.call("fd_write", new long[] {1, 0, 1, 16}, memory));
    assertThat(memory.dataCopies).isEqualTo(1);
    assertThat(stdout.toByteArray()).hasLength(CHUNK);
  }

  @Test
  public void fdReadStopsBetweenChunksOfOneIovec() {
    Memory memory = new Memory(true);
    memory.iovec(8 * CHUNK);
    WasiHost wasi = host(new byte[8 * CHUNK], new CappedOutputStream("stdout", 0));
    assertThrows(WasiHost.Stop.class, () -> wasi.call("fd_read", new long[] {0, 0, 1, 16}, memory));
    assertThat(memory.dataCopies).isEqualTo(1);
  }

  @Test
  public void fdReadCopiesAllOfALargeIovec() {
    Memory memory = new Memory(false);
    memory.iovec(3 * CHUNK + 5);
    byte[] stdin = new byte[3 * CHUNK + 5];
    for (int i = 0; i < stdin.length; i++) {
      stdin[i] = (byte) (i * 7 + i / 251);
    }
    WasiHost wasi = host(stdin, new CappedOutputStream("stdout", 0));
    assertThat(wasi.call("fd_read", new long[] {0, 0, 1, 16}, memory)).isEqualTo(0);
    ByteBuffer view = ByteBuffer.wrap(memory.bytes).order(ByteOrder.LITTLE_ENDIAN);
    assertThat(view.getInt(16)).isEqualTo(stdin.length);
    byte[] copied = new byte[stdin.length];
    System.arraycopy(memory.bytes, 1024, copied, 0, copied.length);
    assertThat(copied).isEqualTo(stdin);
  }

  @Test
  public void clockTimeGetRejectsClocksWasiDoesNotDefine() {
    Memory memory = new Memory(false);
    ByteBuffer view = ByteBuffer.wrap(memory.bytes).order(ByteOrder.LITTLE_ENDIAN);
    WasiHost wasi = host(new byte[0], new CappedOutputStream("stdout", 0));
    for (long id : new long[] {4, 0x7fff_ffffL, 0xffff_ffffL, -1}) {
      view.putLong(512, 12345);
      assertThat(wasi.call("clock_time_get", new long[] {id, 1, 512}, memory))
          .isEqualTo(WasiHost.ERRNO_INVAL);
      assertThat(view.getLong(512)).isEqualTo(12345);
    }
    for (long id = 0; id <= 3; id++) {
      view.putLong(512, 12345);
      assertThat(wasi.call("clock_time_get", new long[] {id, 1, 512}, memory)).isEqualTo(0);
      assertThat(view.getLong(512)).isEqualTo(0);
    }
  }
}
