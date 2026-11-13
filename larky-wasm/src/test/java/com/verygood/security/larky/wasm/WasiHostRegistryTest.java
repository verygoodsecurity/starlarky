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

import com.verygood.security.larky.wasm.WasmRuntime.WasiHostPolicy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.junit.Test;

public final class WasiHostRegistryTest {
  private static final class Memory implements WasiHost.GuestMemory {
    final byte[] bytes = new byte[64];
    int accesses;

    @Override
    public long size() {
      accesses++;
      return bytes.length;
    }

    @Override
    public void read(long address, byte[] into, int offset, int length) {
      accesses++;
      System.arraycopy(bytes, (int) address, into, offset, length);
    }
    @Override
    public void write(long address, byte[] from, int offset, int length) {
      accesses++;
      System.arraycopy(from, offset, bytes, (int) address, length);
    }
  }

  private static WasiHost host(WasiHostPolicy policy, CappedOutputStream stdout) {
    return new WasiHost(new byte[] {42}, stdout, new CappedOutputStream("stderr", 64), 0L, 0, policy);
  }

  @Test
  public void disabledFunctionsDoNotTouchMemoryOutputOrDescriptorState() {
    Memory memory = new Memory();
    CappedOutputStream stdout = new CappedOutputStream("stdout", 64);
    WasiHost wasi = host(new WasiHostPolicy(Set.of("fd_write")), stdout);
    // Invalid pointers must not be inspected by a disabled function.
    assertThat(wasi.bind("random_get").call(new long[] {-1, -1}, memory)).isEqualTo(52);
    assertThat(wasi.bind("fd_close").call(new long[] {1}, memory)).isEqualTo(52);
    assertThat(memory.accesses).isEqualTo(0);
    ByteBuffer.wrap(memory.bytes).order(ByteOrder.LITTLE_ENDIAN)
        .putInt(0, 32).putInt(4, 1);
    memory.bytes[32] = 42;
    assertThat(wasi.bind("fd_write").call(new long[] {1, 0, 1, 8}, memory)).isEqualTo(0);
    assertThat(stdout.toByteArray()).isEqualTo(new byte[] {42});
  }

  @Test
  public void knownUnsupportedFunctionBindsToNosysWithoutMemoryAccess() {
    Memory memory = new Memory();
    assertThat(host(WasiHostPolicy.defaults(), new CappedOutputStream("stdout", 0))
        .bind("poll_oneoff").call(new long[] {-1, -1, -1, -1}, memory)).isEqualTo(52);
    assertThat(memory.accesses).isEqualTo(0);
  }

  @Test
  public void unknownImportsAreRejectedInsteadOfBoundToNosys() {
    assertThrows(IllegalArgumentException.class,
        () -> host(WasiHostPolicy.defaults(), new CappedOutputStream("stdout", 0)).bind("typo"));
  }

  @Test
  public void disabledProcExitTrapsBecauseItCannotReturnAnErrno() {
    WasiHost wasi = host(WasiHostPolicy.none(), new CappedOutputStream("stdout", 0));
    assertThrows(WasiHost.Trap.class, () -> wasi.bind("proc_exit").call(new long[] {3}, new Memory()));
  }

  @Test
  public void boundProcExitPreservesTheExitSignal() {
    WasiHost wasi = host(WasiHostPolicy.defaults(), new CappedOutputStream("stdout", 0));
    WasiHost.ProcExit exit = assertThrows(WasiHost.ProcExit.class,
        () -> wasi.bind("proc_exit").call(new long[] {3}, new Memory()));
    assertThat(exit.code).isEqualTo(3);
  }

  @Test
  public void policyCopiesItsAllowlistAndRejectsUnknownOrUnimplementedGrants() {
    Set<String> grants = new HashSet<>(Set.of("random_get"));
    WasiHostPolicy policy = new WasiHostPolicy(grants);
    grants.add("fd_write");
    assertThat(policy.enabledFunctions()).containsExactly("random_get");
    assertThrows(UnsupportedOperationException.class, () -> policy.enabledFunctions().add("fd_write"));
    assertThrows(IllegalArgumentException.class, () -> new WasiHostPolicy(Set.of("typo")));
    assertThrows(IllegalArgumentException.class, () -> new WasiHostPolicy(Set.of("path_open")));
  }

  @Test
  public void annotationsCoverTheCompletePreview1Abi() throws Exception {
    Properties abi = new Properties();
    try (var stream = getClass().getResourceAsStream("/wasm/wasi-preview1-signatures.properties")) {
      abi.load(stream);
    }
    var annotated = new HashMap<String, String>();
    for (var method : WasiHostModule.class.getDeclaredMethods()) {
      var function = method.getAnnotation(WasiHostFunction.class);
      if (function != null) {
        assertThat(annotated.put(function.name(), function.signature())).isNull();
      }
    }
    assertThat(annotated).containsExactlyEntriesIn(abi);
    assertThat(WasiHost.SIGNATURES).containsExactlyEntriesIn(abi);
  }

  @Test
  public void everyUnsupportedDeclarationReturnsNosysAndCannotBeGranted() {
    var descriptors = WasiHostRegistry.discover(WasiHostModule.class);
    var module = new WasiHostModule(new byte[0], new CappedOutputStream("stdout", 0),
        new CappedOutputStream("stderr", 0), 0L, 0);
    Memory memory = new Memory();
    for (String name : WasiHost.SIGNATURES.keySet()) {
      if (descriptors.get(name).implemented()) {
        continue;
      }
      assertThat(descriptors).containsKey(name);
      long[] args = new long[WasiHost.SIGNATURES.get(name).indexOf(':')];
      java.util.Arrays.fill(args, -1L);
      assertThat(descriptors.get(name).bind(module).call(args, memory)).isEqualTo(52);
      assertThat(host(WasiHostPolicy.defaults(), new CappedOutputStream("stdout", 0))
          .bind(name).call(args, memory)).isEqualTo(52);
      assertThrows(name, IllegalArgumentException.class, () -> new WasiHostPolicy(Set.of(name)));
    }
    assertThat(memory.accesses).isEqualTo(0);
  }

  @Test
  public void discoveryPreservesTheExplicitDefaultGrants() {
    assertThat(WasiHostPolicy.defaults().enabledFunctions()).containsExactly(
        "args_get", "args_sizes_get", "environ_get", "environ_sizes_get", "clock_time_get",
        "random_get", "fd_read", "fd_write", "fd_close", "fd_fdstat_get", "fd_seek",
        "fd_prestat_get", "fd_prestat_dir_name", "sched_yield", "proc_exit");
  }

  static final class Duplicate {
    @WasiHostFunction(name = "sched_yield", signature = ":i")
    int first(WasiHost.GuestMemory memory) { return 0; }
    @WasiHostFunction(name = "sched_yield", signature = ":i")
    int second(WasiHost.GuestMemory memory) { return 0; }
  }

  static final class BadSignature {
    @WasiHostFunction(name = "random_get", signature = "ix:i")
    int wrong(WasiHost.GuestMemory memory, int address) { return 0; }
  }

  static final class BadJavaType {
    @WasiHostFunction(name = "random_get", signature = "ii:i")
    int wrong(WasiHost.GuestMemory memory, long address, int length) { return 0; }
  }

  static final class BadReturn {
    @WasiHostFunction(name = "proc_exit", signature = "i:")
    int wrong(WasiHost.GuestMemory memory, int code) { return 0; }
  }

  static final class StaticFunction {
    @WasiHostFunction(name = "sched_yield", signature = ":i")
    static int wrong(WasiHost.GuestMemory memory) { return 0; }
  }

  static final class InvalidName {
    @WasiHostFunction(name = "bad-name", signature = ":i")
    int wrong(WasiHost.GuestMemory memory) { return 0; }
  }

  @Test
  public void registryRejectsDuplicateNamesAndIncompatibleDeclarations() {
    for (Class<?> invalid : new Class<?>[] {Duplicate.class, BadSignature.class, BadJavaType.class,
        BadReturn.class, StaticFunction.class, InvalidName.class}) {
      assertThrows(invalid.getName(), IllegalArgumentException.class,
          () -> WasiHostRegistry.discover(invalid));
    }
  }

  static final class AdditionalFunction {
    @WasiHostFunction(name = "test_extension", signature = "i:i")
    int extra(WasiHost.GuestMemory memory, int value) { return value; }
  }

  @Test
  public void explicitlyDiscoveredDeclarationsDoNotChangeTheHostCatalogOrPolicy() {
    var function = WasiHostRegistry.discover(AdditionalFunction.class).get("test_extension");
    assertThat(function.bind(new AdditionalFunction()).call(new long[] {28}, new Memory()))
        .isEqualTo(28);
    assertThat(WasiHost.SIGNATURES).doesNotContainKey("test_extension");
    assertThrows(IllegalArgumentException.class,
        () -> new WasiHostPolicy(Set.of("test_extension")));
  }

  static final class NumericArguments {
    long precision;
    int id;
    int address;
    @WasiHostFunction(name = "clock_time_get", signature = "iIi:i")
    int clock(WasiHost.GuestMemory memory, int id, long precision, int address) {
      this.id = id;
      this.precision = precision;
      this.address = address;
      return 28;
    }
  }

  @Test
  public void bindingPreservesI64AndI32BitPatterns() {
    NumericArguments module = new NumericArguments();
    var function = WasiHostRegistry.discover(NumericArguments.class).get("clock_time_get").bind(module);
    assertThat(function.call(new long[] {0xffff_ffffL, Long.MIN_VALUE, 0x8000_0000L}, new Memory()))
        .isEqualTo(28);
    assertThat(module.id).isEqualTo(-1);
    assertThat(module.precision).isEqualTo(Long.MIN_VALUE);
    assertThat(module.address).isEqualTo(Integer.MIN_VALUE);
  }
}
