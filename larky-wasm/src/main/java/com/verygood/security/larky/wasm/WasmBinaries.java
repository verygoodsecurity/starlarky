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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import com.verygood.security.larky.wasm.WasmRuntime.WasmException;

/**
 * Small rewrites of WebAssembly binaries, for runtimes that cannot enforce a limit or redirect an
 * import any other way. Only the type, import, function, table, memory and export sections are decoded; every other
 * section is copied byte for byte.
 */
final class WasmBinaries {

  /** Export and import kinds. */
  public static final int KIND_FUNC = 0;
  public static final int KIND_TABLE = 1;
  public static final int KIND_MEMORY = 2;
  public static final int KIND_GLOBAL = 3;
  public static final int KIND_TAG = 4;

  private static final int SECTION_TYPE = 1;
  private static final int SECTION_IMPORT = 2;
  private static final int SECTION_FUNCTION = 3;
  private static final int SECTION_TABLE = 4;
  private static final int SECTION_MEMORY = 5;
  private static final int SECTION_EXPORT = 7;
  private static final int SECTION_START = 8;

  /** The most pages a 32-bit memory can declare. */
  private static final long MAX_PAGES_32 = 65536;

  private WasmBinaries() {}


  /**
   * Checks that {@code wasm} is a WASI preview 1 command as {@link WasmRuntime} defines it: it
   * imports only functions of {@code wasi_snapshot_preview1}, exports {@code _start} of type
   * {@code [] -> []}, and exports memory 0 as {@code memory}.
   *
   * @throws WasmException of kind {@link WasmException.Kind#INVALID_MODULE} if it is not
   */
  static java.util.Set<String> checkWasiCommand(byte[] wasm) throws WasmException {
    return checkWasiCommand(wasm, WasmHostLimits.configured());
  }

  static java.util.Set<String> checkWasiCommand(byte[] wasm, WasmHostLimits limits)
      throws WasmException {
    Reader r = new Reader(wasm);
    r.header();
    java.util.List<String> types = new java.util.ArrayList<>(); // in WasiHost.SIGNATURES notation
    java.util.List<Long> functionTypes = new java.util.ArrayList<>();
    java.util.Set<String> imports = new java.util.LinkedHashSet<>();
    long start = -1;
    long memory = -1;
    while (!r.atEnd()) {
      int id = r.u8();
      int size = (int) r.u32();
      int end = r.pos + size;
      r.checkEnd(end);
      switch (id) {
        case SECTION_TYPE -> {
          long count = r.u32();
          for (long i = 0; i < count; i++) {
            if (r.u8() != 0x60) {
              throw invalid("unsupported type form");
            }
            StringBuilder signature = new StringBuilder();
            long params = r.u32();
            for (long j = 0; j < params; j++) {
              signature.append(code(r.valueType()));
            }
            signature.append(':');
            long results = r.u32();
            for (long j = 0; j < results; j++) {
              signature.append(code(r.valueType()));
            }
            types.add(signature.toString());
          }
        }
        case SECTION_IMPORT -> {
          long count = r.u32();
          for (long i = 0; i < count; i++) {
            String module = r.name();
            String name = r.name();
            int kind = r.u8();
            if (kind != KIND_FUNC || !module.equals(WasiHost.MODULE)) {
              throw invalid("import " + module + "." + name + " is not a " + WasiHost.MODULE + " function");
            }
            // Each import is checked, so a module that imports a function twice cannot get a
            // wrong signature past the check with a right one.
            long type = r.u32();
            String expected = WasiHost.SIGNATURES.get(name);
            if (expected == null) {
              throw invalid("import " + WasiHost.MODULE + "." + name + " is not a WASI preview 1 function");
            }
            if (type >= types.size() || !expected.equals(types.get((int) type))) {
              throw invalid("import " + WasiHost.MODULE + "." + name + " has the wrong signature");
            }
            functionTypes.add(type);
            imports.add(name);
          }
        }
        case SECTION_FUNCTION -> {
          long count = r.u32();
          if (count > limits.maxFunctions()) {
            throw invalid(
                "module defines " + count + " functions; the limit is " + limits.maxFunctions());
          }
          for (long i = 0; i < count; i++) {
            functionTypes.add(r.u32());
          }
        }
        case SECTION_MEMORY -> {
          // The run limit is for the instance, not a separate allowance for every memory.
          if (r.u32() != 1) {
            throw invalid("module must define exactly one memory");
          }
        }
        case SECTION_EXPORT -> {
          long count = r.u32();
          for (long i = 0; i < count; i++) {
            String name = r.name();
            int kind = r.u8();
            long index = r.u32();
            if (kind == KIND_FUNC && name.equals("_start")) {
              start = index;
            } else if (kind == KIND_MEMORY && name.equals("memory")) {
              memory = index;
            }
          }
        }
        case SECTION_START ->
            // It would run while the module is instantiated, before the host can see its memory.
            throw invalid("module has a start section; a WASI command starts at _start");
        default -> {}
      }
      r.pos = end;
    }
    if (start < 0) {
      throw invalid("module does not export _start");
    }
    if (start >= functionTypes.size() || functionTypes.get((int) start) >= types.size()) {
      throw invalid("_start is not a function of the module");
    }
    if (!types.get((int) (long) functionTypes.get((int) start)).equals(":")) {
      throw invalid("_start must have type [] -> []");
    }
    if (memory != 0) {
      throw invalid(memory < 0 ? "module does not export memory" : "export memory is not memory 0");
    }
    return imports;
  }

  /**
   * Bounds table allocations for the whole instance, including table.grow. All minima are
   * reserved first; the remaining allowance is shared in declaration order across the tables
   * still to visit, respecting any smaller declared maximum. Imported tables are rejected by
   * checkWasiCommand before this rewrite is used.
   */
  static byte[] capTables(byte[] wasm) throws WasmException {
    return capTables(wasm, WasmHostLimits.configured());
  }

  static byte[] capTables(byte[] wasm, WasmHostLimits limits) throws WasmException {
    Reader r = new Reader(wasm);
    r.header();
    int sectionStart = -1;
    int sectionEnd = -1;
    java.util.List<TableDefinition> tables = new java.util.ArrayList<>();
    long totalMinimum = 0;
    while (!r.atEnd()) {
      int start = r.pos;
      int id = r.u8();
      int size = (int) r.u32();
      int end = r.pos + size;
      r.checkEnd(end);
      if (id == SECTION_TABLE) {
        if (sectionStart >= 0) {
          throw invalid("duplicate table section");
        }
        sectionStart = start;
        sectionEnd = end;
        // Keep every read inside this section, including variable-length reference types.
        Reader tableReader = new Reader(Arrays.copyOfRange(wasm, r.pos, end));
        long count = tableReader.u32();
        if (count > limits.maxTables()) {
          throw new WasmException(WasmException.Kind.MEMORY_LIMIT,
              "module defines " + count + " tables; the limit is " + limits.maxTables());
        }
        for (int i = 0; i < count; i++) {
          int typeStart = tableReader.pos;
          int type = tableReader.valueType();
          if (type == 0x40) {
            throw invalid("explicit table initializer expressions are not supported");
          }
          byte[] referenceType = Arrays.copyOfRange(tableReader.b, typeStart, tableReader.pos);
          int flags = tableReader.u8();
          if ((flags & ~1) != 0) {
            throw invalid("unsupported table limits flags " + flags);
          }
          long minimum = tableReader.u32();
          long maximum = (flags & 1) != 0 ? tableReader.u32() : 0xffff_ffffL;
          if (maximum < minimum) {
            throw invalid("table maximum is smaller than its minimum");
          }
          totalMinimum += minimum;
          if (totalMinimum > limits.maxTableElements()) {
            throw new WasmException(WasmException.Kind.MEMORY_LIMIT,
                "initial tables exceed the combined limit of " + limits.maxTableElements()
                    + " elements");
          }
          tables.add(new TableDefinition(referenceType, minimum, maximum));
        }
        if (!tableReader.atEnd()) {
          throw invalid("table section size mismatch");
        }
      }
      r.pos = end;
    }
    if (sectionStart < 0) {
      return wasm;
    }
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    writeU(body, tables.size());
    long remaining = limits.maxTableElements() - totalMinimum;
    for (int i = 0; i < tables.size(); i++) {
      TableDefinition table = tables.get(i);
      long maximum = Math.min(table.maximum(),
          table.minimum() + remaining / (tables.size() - i));
      remaining -= maximum - table.minimum();
      body.writeBytes(table.referenceType());
      body.write(1); // Every table has a maximum, even if the guest omitted it.
      writeU(body, table.minimum());
      writeU(body, maximum);
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream(wasm.length + tables.size() * 6);
    out.write(wasm, 0, sectionStart);
    out.write(SECTION_TABLE);
    writeU(out, body.size());
    out.writeBytes(body.toByteArray());
    out.write(wasm, sectionEnd, wasm.length - sectionEnd);
    byte[] capped = out.toByteArray();
    return Arrays.equals(capped, wasm) ? wasm : capped;
  }

  private record TableDefinition(byte[] referenceType, long minimum, long maximum) {}

  /** A value type in {@link WasiHost#SIGNATURES}' notation: i32 {@code i}, i64 {@code I}, else {@code ?}. */
  private static char code(int valueType) {
    return valueType == 0x7f ? 'i' : valueType == 0x7e ? 'I' : '?';
  }

  /**
   * Returns {@code wasm} with the maximum of every memory it defines or imports set to at most
   * {@code maxPages} (a memory without a maximum gets {@code maxPages}), so {@code memory.grow}
   * past it returns -1. Returns {@code wasm} itself if nothing changes.
   *
   * @throws WasmException of kind {@link WasmException.Kind#MEMORY_LIMIT} if a memory's initial
   *     size is over {@code maxPages}, or {@link WasmException.Kind#INVALID_MODULE} if the binary
   *     is malformed
   */
  public static byte[] capMemory(byte[] wasm, long maxPages) throws WasmException {
    return rewrite(wasm, maxPages, null, null, null);
  }

  /**
   * Returns {@code wasm} with each import of {@code module}.{@code name} for {@code name} in
   * {@code names} renamed to {@code newModule}.{@code name}, so the embedder can supply those
   * functions itself. Returns {@code wasm} itself if nothing changes.
   */
  public static byte[] renameImports(byte[] wasm, String module, Set<String> names, String newModule)
      throws WasmException {
    return rewrite(wasm, -1, module, names, newModule);
  }

  private static byte[] rewrite(
      byte[] wasm, long maxPages, String module, Set<String> names, String newModule)
      throws WasmException {
    Reader r = new Reader(wasm);
    r.header();
    ByteArrayOutputStream out = new ByteArrayOutputStream(wasm.length + 16);
    out.write(wasm, 0, 8);
    boolean changed = false;
    while (!r.atEnd()) {
      int start = r.pos;
      int id = r.u8();
      int size = (int) r.u32();
      int bodyStart = r.pos;
      int end = bodyStart + size;
      r.checkEnd(end);
      byte[] body = null;
      if (id == SECTION_IMPORT) {
        body = rewriteImports(r, end, maxPages, module, names, newModule);
      } else if (id == SECTION_MEMORY && maxPages >= 0) {
        body = rewriteMemories(r, end, maxPages);
      }
      if (body != null && !Arrays.equals(wasm, bodyStart, end, body, 0, body.length)) {
        changed = true;
        out.write(id);
        writeU(out, body.length);
        out.write(body, 0, body.length);
      } else {
        out.write(wasm, start, end - start);
      }
      r.pos = end;
    }
    return changed ? out.toByteArray() : wasm;
  }

  private static byte[] rewriteImports(
      Reader r, int end, long maxPages, String module, Set<String> names, String newModule)
      throws WasmException {
    ByteArrayOutputStream out = new ByteArrayOutputStream(end - r.pos + 16);
    long count = r.u32();
    writeU(out, count);
    for (long i = 0; i < count; i++) {
      String importModule = r.name();
      String importName = r.name();
      int kind = r.u8();
      if (module != null && kind == KIND_FUNC && importModule.equals(module)
          && names.contains(importName)) {
        importModule = newModule;
      }
      writeName(out, importModule);
      writeName(out, importName);
      out.write(kind);
      switch (kind) {
        case KIND_FUNC -> writeU(out, r.u32());
        case KIND_TABLE -> {
          out.write(r.u8());
          copyLimits(r, out);
        }
        case KIND_MEMORY -> {
          if (maxPages >= 0) {
            capLimits(r, out, maxPages);
          } else {
            copyLimits(r, out);
          }
        }
        case KIND_GLOBAL -> {
          out.write(r.u8());
          out.write(r.u8());
        }
        case KIND_TAG -> {
          out.write(r.u8());
          writeU(out, r.u32());
        }
        default -> throw invalid("unknown import kind " + kind);
      }
    }
    if (r.pos != end) {
      throw invalid("import section size mismatch");
    }
    return out.toByteArray();
  }

  private static byte[] rewriteMemories(Reader r, int end, long maxPages) throws WasmException {
    ByteArrayOutputStream out = new ByteArrayOutputStream(end - r.pos + 16);
    long count = r.u32();
    writeU(out, count);
    for (long i = 0; i < count; i++) {
      capLimits(r, out, maxPages);
    }
    if (r.pos != end) {
      throw invalid("memory section size mismatch");
    }
    return out.toByteArray();
  }

  private static void copyLimits(Reader r, ByteArrayOutputStream out) throws WasmException {
    int flags = r.u8();
    out.write(flags);
    writeU(out, r.u64());
    if ((flags & 1) != 0) {
      writeU(out, r.u64());
    }
    if ((flags & 8) != 0) {
      writeU(out, r.u32());
    }
  }

  private static void capLimits(Reader r, ByteArrayOutputStream out, long maxPages)
      throws WasmException {
    int flags = r.u8();
    if ((flags & ~0x7) != 0) {
      throw invalid("unsupported memory limits flags " + flags);
    }
    long cap = (flags & 4) != 0 ? maxPages : Math.min(maxPages, MAX_PAGES_32);
    long min = r.u64();
    long max = (flags & 1) != 0 ? r.u64() : Long.MAX_VALUE;
    if (Long.compareUnsigned(min, cap) > 0) {
      throw new WasmException(
          WasmException.Kind.MEMORY_LIMIT,
          "the module's initial memory (" + min + " pages) is over the limit of " + cap + " pages");
    }
    out.write(flags | 1);
    writeU(out, min);
    writeU(out, Long.compareUnsigned(max, cap) < 0 ? max : cap);
  }

  private static void writeU(ByteArrayOutputStream out, long value) {
    do {
      int b = (int) (value & 0x7f);
      value >>>= 7;
      out.write(value != 0 ? b | 0x80 : b);
    } while (value != 0);
  }

  private static void writeName(ByteArrayOutputStream out, String name) {
    byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
    writeU(out, bytes.length);
    out.write(bytes, 0, bytes.length);
  }

  private static WasmException invalid(String message) {
    return new WasmException(WasmException.Kind.INVALID_MODULE, "invalid module: " + message);
  }

  private static final class Reader {
    final byte[] b;
    int pos;

    Reader(byte[] b) {
      this.b = b;
    }

    void header() throws WasmException {
      if (b.length < 8 || b[0] != 0 || b[1] != 'a' || b[2] != 's' || b[3] != 'm') {
        throw invalid("bad magic number");
      }
      if (b[4] != 1 || b[5] != 0 || b[6] != 0 || b[7] != 0) {
        throw invalid("unsupported version");
      }
      pos = 8;
    }

    boolean atEnd() {
      return pos >= b.length;
    }

    void checkEnd(int end) throws WasmException {
      if (end < pos || end > b.length) {
        throw invalid("section runs past the end of the module");
      }
    }

    int u8() throws WasmException {
      if (pos >= b.length) {
        throw invalid("unexpected end of module");
      }
      return b[pos++] & 0xff;
    }

    long u32() throws WasmException {
      return unsigned(32);
    }

    long u64() throws WasmException {
      return unsigned(64);
    }

    /**
     * An unsigned LEB128 integer of at most {@code bits} bits, in at most {@code ceil(bits / 7)}
     * bytes, as the specification requires: a longer encoding, or bits set beyond {@code bits}
     * in the last byte, is malformed (and would otherwise be rewritten into a valid module).
     */
    private long unsigned(int bits) throws WasmException {
      long result = 0;
      for (int shift = 0; ; shift += 7) {
        int x = u8();
        int payload = x & 0x7f;
        if (bits - shift < 7 && payload >>> (bits - shift) != 0) {
          throw invalid("integer too large");
        }
        result |= (long) payload << shift;
        if ((x & 0x80) == 0) {
          return result;
        }
        if (shift + 7 >= bits) {
          throw invalid("integer representation too long");
        }
      }
    }

    /** Skips a signed LEB128 integer of at most {@code bits} bits, checking its encoding. */
    private void skipSigned(int bits) throws WasmException {
      for (int shift = 0; ; shift += 7) {
        int x = u8();
        if (bits - shift < 7) {
          // The last byte: its bits from the value's sign bit up must all be equal.
          int high = (x & 0x7f) >> (bits - shift - 1);
          if (high != 0 && high != 0x7f >> (bits - shift - 1)) {
            throw invalid("integer too large");
          }
        }
        if ((x & 0x80) == 0) {
          return;
        }
        if (shift + 7 >= bits) {
          throw invalid("integer representation too long");
        }
      }
    }

    int valueType() throws WasmException {
      int t = u8();
      if (t == 0x63 || t == 0x64) {
        skipSigned(33); // heap type
      }
      return t;
    }

    String name() throws WasmException {
      int len = (int) u32();
      checkEnd(pos + len);
      String s = new String(b, pos, len, StandardCharsets.UTF_8);
      pos += len;
      return s;
    }
  }
}
