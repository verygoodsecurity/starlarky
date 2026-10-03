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

package com.verygood.security.larky.wasm.graal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Keeps at most {@code limit} bytes; a write past that fails and marks the stream overflowed. */
final class CappedOutputStream extends OutputStream {
  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
  private final int limit;
  private volatile boolean overflowed;

  CappedOutputStream(int limit) {
    this.limit = limit;
  }

  boolean overflowed() {
    return overflowed;
  }

  synchronized byte[] toByteArray() {
    return buffer.toByteArray();
  }

  @Override
  public void write(int b) throws IOException {
    write(new byte[] {(byte) b}, 0, 1);
  }

  @Override
  public synchronized void write(byte[] b, int off, int len) throws IOException {
    int room = limit - buffer.size();
    if (len > room) {
      buffer.write(b, off, Math.max(room, 0));
      overflowed = true;
      throw new IOException("output limit of " + limit + " bytes exceeded");
    }
    buffer.write(b, off, len);
  }
}
