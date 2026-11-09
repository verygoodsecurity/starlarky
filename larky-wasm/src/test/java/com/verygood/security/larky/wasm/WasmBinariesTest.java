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

import java.io.ByteArrayOutputStream;
import java.util.HexFormat;
import org.junit.Test;

public final class WasmBinariesTest {
  private static byte[] memory64(byte[] min, byte[] max) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.writeBytes(HexFormat.of().parseHex("0061736d01000000"));
    out.write(5);
    out.write(2 + min.length + max.length);
    out.write(1);
    out.write(5); // memory64 with a maximum
    out.writeBytes(min);
    out.writeBytes(max);
    return out.toByteArray();
  }

  @Test
  public void memory64AcceptsTenByteEncodingsAndCapsUnsignedMaximum() throws Exception {
    byte[] min = HexFormat.of().parseHex("81808080808080808000");
    byte[] max = HexFormat.of().parseHex("ffffffffffffffffff01");
    assertThat(WasmBinaries.capMemory(memory64(min, max), 16))
        .isEqualTo(memory64(new byte[] {1}, new byte[] {16}));
  }

  @Test
  public void memory64RejectsOverflowAndOverlongEncodingsBeforeRewriting() {
    for (String bad : new String[] {
        "80808080808080808002", "8080808080808080808000", "80808080808080808080"
    }) {
      byte[] encoded = HexFormat.of().parseHex(bad);
      assertThat(assertThrows(WasmRuntime.WasmException.class,
          () -> WasmBinaries.capMemory(memory64(new byte[] {1}, encoded), 16)).kind())
          .isEqualTo(WasmRuntime.WasmException.Kind.INVALID_MODULE);
    }
  }
  @Test
  public void tableRewriteRejectsMalformedAndUnsupportedDeclarations() {
    // Truncated max, max < min, table64, and an explicit table initializer prefix.
    for (String body : new String[] {"01700101", "0170010201", "01700400", "014000700000"}) {
      byte[] table = HexFormat.of().parseHex(body);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      out.writeBytes(HexFormat.of().parseHex("0061736d01000000"));
      out.write(4);
      out.write(table.length);
      out.writeBytes(table);
      assertThat(assertThrows(WasmRuntime.WasmException.class,
          () -> WasmBinaries.capTables(out.toByteArray())).kind())
          .isEqualTo(WasmRuntime.WasmException.Kind.INVALID_MODULE);
    }
  }

  @Test
  public void tableRewritePreservesAnAlreadyBoundedTable() throws Exception {
    byte[] wasm = HexFormat.of().parseHex("0061736d0100000004050170010102");
    assertThat(WasmBinaries.capTables(wasm)).isSameInstanceAs(wasm);
  }

}
