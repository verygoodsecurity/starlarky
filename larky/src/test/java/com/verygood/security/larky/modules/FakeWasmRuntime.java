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

package com.verygood.security.larky.modules;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.verygood.security.larky.wasm.WasmRuntime.WasmException;
import com.verygood.security.larky.wasm.WasmRuntime.WasmException.Kind;
import com.verygood.security.larky.wasm.WasmRuntime;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link WasmRuntime} for Larky's tests: a "module" is any bytes starting with {@code FAKE}
 * (compiling one that starts with {@code FAKEslow} takes 200 ms). It
 * echoes stdin to stdout, except for the inputs below, which exit, fail or wait; its own error
 * messages are deliberately unlike Larky's.
 *
 * <ul>
 *   <li>{@code exit:N:text}: exits with code N, writing text to stderr
 *   <li>{@code bigerr}: exits with code 3, writing 2000 bytes to stderr
 *   <li>{@code trap}, {@code oom}, {@code flood}: throw TRAP, MEMORY_LIMIT, OUTPUT_LIMIT
 *   <li>{@code sleep}: waits for the deadline, then throws TIMEOUT
 *   <li>{@code limits}: writes the limits it was given
 *   <li>{@code interrupt}: throws InterruptedException
 * </ul>
 */
public final class FakeWasmRuntime implements WasmRuntime {

  public static final String NAME = "fake";
  static final AtomicInteger COMPILES = new AtomicInteger();

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public WasmRuntime.Program compile(byte[] wasm) throws WasmException {
    if (wasm.length < 4 || !new String(wasm, 0, 4, UTF_8).equals("FAKE")) {
      throw new WasmException(Kind.INVALID_MODULE, "fake: bad magic");
    }
    COMPILES.incrementAndGet();
    if (new String(wasm, UTF_8).startsWith("FAKEslow")) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return FakeWasmRuntime::run;
  }

  private static WasmRuntime.Result run(byte[] stdin, WasmRuntime.Limits limits)
      throws WasmException, InterruptedException {
    String in = new String(stdin, UTF_8);
    if (in.startsWith("exit:")) {
      String[] parts = in.split(":", 3);
      return new WasmRuntime.Result(Integer.parseInt(parts[1]), new byte[0], parts[2].getBytes(UTF_8));
    }
    switch (in) {
      case "bigerr":
        byte[] err = new byte[2000];
        Arrays.fill(err, (byte) 'e');
        return new WasmRuntime.Result(3, new byte[0], err);
      case "trap":
        throw new WasmException(Kind.TRAP, "fake: unreachable executed");
      case "oom":
        throw new WasmException(Kind.MEMORY_LIMIT, "fake: memory");
      case "flood":
        throw new WasmException(Kind.OUTPUT_LIMIT, "fake: output");
      case "sleep":
        if (limits.deadlineEpochMs() == 0) {
          throw new IllegalStateException("no deadline");
        }
        while (System.currentTimeMillis() < limits.deadlineEpochMs()) {
          Thread.sleep(5);
        }
        throw new WasmException(Kind.TIMEOUT, "fake: deadline");
      case "interrupt":
        throw new InterruptedException("fake");
      case "limits":
        return new WasmRuntime.Result(
            0,
            (limits.maxMemoryBytes() + " " + limits.maxOutputBytes() + " " + limits.randomSeed())
                .getBytes(UTF_8),
            new byte[0]);
      default:
        return new WasmRuntime.Result(0, stdin, new byte[0]);
    }
  }
}
