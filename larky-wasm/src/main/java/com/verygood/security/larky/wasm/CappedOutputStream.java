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

import java.io.OutputStream;
import java.util.Arrays;

/**
 * Collects a guest's stdout or stderr, refusing to grow past a limit. A write that would exceed it
 * throws {@link OutputLimitExceeded} out of the WASI call, which stops the guest.
 */
final class CappedOutputStream extends OutputStream {

  /** Unwinds the guest when it writes more than the limit. */
  static final class OutputLimitExceeded extends RuntimeException {
    OutputLimitExceeded(String message) {
      super(message, null, false, false);
    }
  }

  private final String name;
  private final int limit;
  private byte[] buf = new byte[256];
  private int count;
  private boolean exceeded;

  CappedOutputStream(String name, int limit) {
    this.name = name;
    this.limit = limit;
  }

  @Override
  public void write(int b) {
    if (count == limit) {
      throw exceed();
    }
    if (count == buf.length) {
      buf = Arrays.copyOf(buf, Math.min(limit, buf.length * 2));
    }
    buf[count++] = (byte) b;
  }

  @Override
  public void write(byte[] b, int off, int len) {
    if (len > limit - count) {
      throw exceed();
    }
    if (count + len > buf.length) {
      buf = Arrays.copyOf(buf, Math.min(limit, Math.max(count + len, buf.length * 2)));
    }
    System.arraycopy(b, off, buf, count, len);
    count += len;
  }

  private OutputLimitExceeded exceed() {
    exceeded = true;
    return new OutputLimitExceeded(name + " exceeded " + limit + " bytes");
  }

  boolean exceeded() {
    return exceeded;
  }

  byte[] toByteArray() {
    return Arrays.copyOf(buf, count);
  }
}
