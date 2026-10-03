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

/**
 * Small rewrites of WebAssembly binaries, for runtimes that cannot enforce a limit or redirect an
 * import any other way. Only the import, memory and export sections are decoded; every other
 * section is copied byte for byte.
 */
public final class WasmBinaries {

  /** Export and import kinds. */
  public static final int KIND_FUNC = 0;
  public static final int KIND_TABLE = 1;
  public static final int KIND_MEMORY = 2;
  public static final int KIND_GLOBAL = 3;
  public static final int KIND_TAG = 4;

  private static final int SECTION_TYPE = 1;
  private static final int SECTION_IMPORT = 2;
  private static final int SECTION_FUNCTION = 3;
  private static final int SECTION_MEMORY = 5;
  private static final int SECTION_EXPORT = 7;

  /** The most pages a 32-bit memory can declare. */
  private static final long MAX_PAGES_32 = 65536;

  private WasmBinaries() {}

  /** Whether {@code wasm} exports {@code name} as {@code kind} (e.g. {@link #KIND_FUNC}). */
  public static boolean hasExport(byte[] wasm, String name, int kind) throws WasmException {
    Reader r = new Reader(wasm);
    r.header();
    while (!r.atEnd()) {
      int id = r.u8();
      int size = (int) r.u32();
      int end = r.pos + size;
      r.checkEnd(end);
      if (id == SECTION_EXPORT) {
        long count = r.u32();
        for (long i = 0; i < count; i++) {
          String exportName = r.name();
          int exportKind = r.u8();
          r.u32();
          if (exportKind == kind && exportName.equals(name)) {
            return true;
          }
        }
      }
      r.pos = end;
    }
    return false;
  }

  /**
   * Checks that {@code wasm} is a WASI preview 1 command as {@link WasmRuntime} defines it: it
   * imports only functions of {@code wasi_snapshot_preview1}, exports {@code _start} of type
   * {@code [] -> []}, and exports memory 0 as {@code memory}.
   *
   * @throws WasmException of kind {@link WasmException.Kind#INVALID_MODULE} if it is not
   */
  public static void checkWasiCommand(byte[] wasm) throws WasmException {
    Reader r = new Reader(wasm);
    r.header();
    java.util.List<long[]> types = new java.util.ArrayList<>(); // {params, results}
    java.util.List<Long> functionTypes = new java.util.ArrayList<>();
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
            long params = r.u32();
            for (long j = 0; j < params; j++) {
              r.valueType();
            }
            long results = r.u32();
            for (long j = 0; j < results; j++) {
              r.valueType();
            }
            types.add(new long[] {params, results});
          }
        }
        case SECTION_IMPORT -> {
          long count = r.u32();
          for (long i = 0; i < count; i++) {
            String module = r.name();
            String name = r.name();
            int kind = r.u8();
            if (kind != KIND_FUNC || !module.equals("wasi_snapshot_preview1")) {
              throw invalid(
                  "import " + module + "." + name + " is not a wasi_snapshot_preview1 function");
            }
            functionTypes.add(r.u32());
          }
        }
        case SECTION_FUNCTION -> {
          long count = r.u32();
          for (long i = 0; i < count; i++) {
            functionTypes.add(r.u32());
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
    long[] type = types.get((int) (long) functionTypes.get((int) start));
    if (type[0] != 0 || type[1] != 0) {
      throw invalid("_start must have type [] -> []");
    }
    if (memory != 0) {
      throw invalid(memory < 0 ? "module does not export memory" : "export memory is not memory 0");
    }
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
      long v = u64();
      if (v > 0xffffffffL || v < 0) {
        throw invalid("integer too large");
      }
      return v;
    }

    long u64() throws WasmException {
      long result = 0;
      for (int shift = 0; shift < 70; shift += 7) {
        int x = u8();
        result |= (long) (x & 0x7f) << shift;
        if ((x & 0x80) == 0) {
          return result;
        }
      }
      throw invalid("integer too long");
    }

    void valueType() throws WasmException {
      int t = u8();
      if (t == 0x63 || t == 0x64) {
        u64(); // heap type (s33); its LEB length is all that matters here
      }
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
