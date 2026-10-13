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

import org.junit.Test;
import run.endive.wabt.Wat2Wasm;

public final class GraalWasmTableLimitTest {
  private static final WasmRuntime RUNTIME = new GraalWasmRuntime();

  private static byte[] module(String tables, String body) {
    return Wat2Wasm.parse("(module (memory (export \"memory\") 1) " + tables
        + " (func (export \"_start\") " + body + "))");
  }

  @Test
  public void rejectsLargeAndCombinedInitialTablesBeforeInstantiation() {
    for (String tables : new String[] {
        "(table 10000000 funcref)",
        "(table 32769 funcref) (table 32768 funcref)"
    }) {
      byte[] wasm = module(tables, "");
      if (tables.equals("(table 10000000 funcref)")) {
        assertThat(wasm).hasLength(59);
      }
      WasmRuntime.WasmException error = assertThrows(WasmRuntime.WasmException.class,
          () -> RUNTIME.compile(wasm));
      assertThat(error.kind()).isEqualTo(WasmRuntime.WasmException.Kind.MEMORY_LIMIT);
      assertThat(error).hasMessageThat().contains("table");
    }
  }

  @Test
  public void rejectsTooManyEmptyTables() {
    WasmRuntime.WasmException error = assertThrows(WasmRuntime.WasmException.class,
        () -> RUNTIME.compile(module("(table 0 funcref) ".repeat(65), "")));
    assertThat(error.kind()).isEqualTo(WasmRuntime.WasmException.Kind.MEMORY_LIMIT);
  }

  @Test
  public void capsGrowthForUnboundedAndExplicitMaximumTables() throws Exception {
    for (String table : new String[] {"(table 0 funcref)", "(table 0 10000000 funcref)"}) {
      WasmRuntime.Program program = RUNTIME.compile(module(table,
          "(if (i32.ne (table.grow 0 (ref.null func) (i32.const 1)) (i32.const 0))"
              + " (then unreachable))"
              + "(if (i32.ne (table.grow 0 (ref.null func) (i32.const 65536)) (i32.const -1))"
              + " (then unreachable))"
              + "(if (i32.ne (table.size 0) (i32.const 1)) (then unreachable))"));
      assertThat(program.run(new byte[0], WasmRuntime.Limits.defaults()).exitCode()).isEqualTo(0);
    }
  }

  @Test
  public void combinedTableMaximaStayWithinTheProgramBudget() throws Exception {
    WasmRuntime.Program program = RUNTIME.compile(module(
        "(table 32768 funcref) (table 32768 funcref)",
        "(if (i32.ne (table.grow 0 (ref.null func) (i32.const 1)) (i32.const -1))"
            + " (then unreachable))"
            + "(if (i32.ne (table.grow 1 (ref.null func) (i32.const 1)) (i32.const -1))"
            + " (then unreachable))"));
    assertThat(program.run(new byte[0], WasmRuntime.Limits.defaults()).exitCode()).isEqualTo(0);
  }
  @Test
  public void smallerDeclaredMaximumLeavesBudgetForTheNextTable() throws Exception {
    WasmRuntime.Program program = RUNTIME.compile(module(
        "(table 0 1 funcref) (table 0 funcref)",
        "(if (i32.ne (table.grow 0 (ref.null func) (i32.const 1)) (i32.const 0))"
            + " (then unreachable))"
            + "(if (i32.ne (table.grow 1 (ref.null func) (i32.const 65535)) (i32.const 0))"
            + " (then unreachable))"
            + "(if (i32.ne (table.grow 1 (ref.null func) (i32.const 1)) (i32.const -1))"
            + " (then unreachable))"));
    assertThat(program.run(new byte[0], WasmRuntime.Limits.defaults()).exitCode()).isEqualTo(0);
  }

}
