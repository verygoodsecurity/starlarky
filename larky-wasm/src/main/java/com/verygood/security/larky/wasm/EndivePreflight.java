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

import com.verygood.security.larky.wasm.WasmRuntime.WasmException;
import com.verygood.security.larky.wasm.WasmRuntime.WasmException.Kind;

/**
 * Checks length fields before handing untrusted bytes to Endive. Endive 1.1.0 allocates several
 * arrays and lists from unsigned LEB lengths before checking that their contents exist. This pass
 * reads only structural fields: it never allocates in proportion to a guest-supplied number.
 */
final class EndivePreflight {
  private static final int MAX_LOCALS_PER_FUNCTION = 50_000;
  private static final int MAX_TOTAL_LOCALS = 100_000;
  private static final int MAX_TYPE_ENTRIES = 100_000;

  private EndivePreflight() {}

  static void check(byte[] wasm) throws WasmException {
    Cursor r = new Cursor(wasm, 0, wasm.length);
    if (r.u8() != 0 || r.u8() != 'a' || r.u8() != 's' || r.u8() != 'm'
        || r.u8() != 1 || r.u8() != 0 || r.u8() != 0 || r.u8() != 0) {
      throw invalid("bad WebAssembly header");
    }
    Budget budget = new Budget();
    while (r.remaining() != 0) {
      int id = r.u8();
      int size = r.length();
      Cursor s = r.section(size);
      switch (id) {
        case 0 -> s.name();
        case 1 -> types(s, budget);
        case 2 -> imports(s);
        case 3 -> functions(s);
        case 7 -> exports(s);
        case 9 -> elements(s, budget);
        case 10 -> code(s, budget);
        case 11 -> data(s);
        default -> { /* Endive validates other sections after all dangerous lengths are checked. */ }
      }
      if (id != 0 && (id == 1 || id == 2 || id == 3 || id == 7 || id == 9
          || id == 10 || id == 11) && s.remaining() != 0) {
        throw invalid("section " + id + " has trailing bytes");
      }
    }
  }

  private static void types(Cursor s, Budget budget) throws WasmException {
    int count = s.count(1);
    if (count > WasmRuntime.MAX_FUNCTIONS) throw invalid("too many types");
    for (int i = 0; i < count; i++) {
      int form = s.length();
      if (form == 0x4e) {
        int subtypes = s.count(1);
        budget.typeEntries += subtypes;
        if (budget.typeEntries > MAX_TYPE_ENTRIES) throw invalid("too many type entries");
        for (int j = 0; j < subtypes; j++) subtype(s, s.length(), budget);
      } else {
        budget.typeEntries++;
        subtype(s, form, budget);
      }
    }
  }

  private static void subtype(Cursor s, int form, Budget budget) throws WasmException {
    if (form == 0x50 || form == 0x4f) {
      int parents = s.count(1);
      budget.typeEntries += parents;
      if (budget.typeEntries > MAX_TYPE_ENTRIES) throw invalid("too many type entries");
      for (int i = 0; i < parents; i++) s.u32();
      form = s.length();
    }
    switch (form) {
      case 0x60 -> {
        int params = s.count(1);
        if (params > 1_000) throw invalid("too many function parameters");
        budget.typeEntries += params;
        if (budget.typeEntries > MAX_TYPE_ENTRIES) throw invalid("too many type entries");
        for (int i = 0; i < params; i++) s.valueType();
        int results = s.count(1);
        if (results > 1_000) throw invalid("too many function results");
        budget.typeEntries += results;
        if (budget.typeEntries > MAX_TYPE_ENTRIES) throw invalid("too many type entries");
        for (int i = 0; i < results; i++) s.valueType();
      }
      case 0x5e -> s.fieldType();
      case 0x5f -> {
        int fields = s.count(2);
        budget.typeEntries += fields;
        if (budget.typeEntries > MAX_TYPE_ENTRIES) throw invalid("too many type entries");
        for (int i = 0; i < fields; i++) s.fieldType();
      }
      default -> throw invalid("unsupported type form " + form);
    }
  }

  private static void imports(Cursor s) throws WasmException {
    int count = s.count(3);
    for (int i = 0; i < count; i++) {
      s.name();
      s.name();
      // The WASI contract rejects all non-function imports. Reject them before Endive parses
      // their descriptors, so a malformed descriptor cannot reach another allocation path.
      if (s.u8() != 0) throw invalid("only function imports are allowed");
      s.u32();
    }
  }

  private static void functions(Cursor s) throws WasmException {
    int count = s.count(1);
    if (count > WasmRuntime.MAX_FUNCTIONS) throw invalid("too many functions");
    for (int i = 0; i < count; i++) s.u32();
  }

  private static void exports(Cursor s) throws WasmException {
    int count = s.count(3);
    for (int i = 0; i < count; i++) {
      s.name();
      s.u8();
      s.u32();
    }
  }

  private static void elements(Cursor s, Budget budget) throws WasmException {
    int count = s.count(2);
    if (count > WasmRuntime.MAX_TABLE_ELEMENTS) throw invalid("too many element segments");
    for (int i = 0; i < count; i++) {
      long flags = s.u32();
      if (flags > 7) throw invalid("unsupported element flags");
      boolean active = (flags & 1) == 0;
      boolean explicitTable = active && (flags & 2) != 0;
      boolean expressions = (flags & 4) != 0;
      if (explicitTable) s.u32();
      if (active) s.expression();
      if (!active || explicitTable) {
        if (expressions) s.valueType();
        else s.u32(); // elemkind
      }
      int initializers = s.count(1);
      // Each live element occupies a table slot. Passive elements may be activated later, so
      // reject oversized segments even when the declared table minimum is small.
      if (initializers > WasmRuntime.MAX_TABLE_ELEMENTS) {
        throw new WasmException(Kind.MEMORY_LIMIT, "element segment exceeds "
            + WasmRuntime.MAX_TABLE_ELEMENTS + " entries");
      }
      budget.elementEntries += initializers;
      if (budget.elementEntries > WasmRuntime.MAX_TABLE_ELEMENTS) {
        throw new WasmException(Kind.MEMORY_LIMIT, "element segments exceed "
            + WasmRuntime.MAX_TABLE_ELEMENTS + " entries in total");
      }
      for (int j = 0; j < initializers; j++) {
        if (expressions) s.expression();
        else s.u32();
      }
    }
  }

  private static void code(Cursor s, Budget budget) throws WasmException {
    int count = s.count(2);
    if (count > WasmRuntime.MAX_FUNCTIONS) throw invalid("too many function bodies");
    for (int i = 0; i < count; i++) {
      Cursor body = s.section(s.length());
      int groups = body.count(2);
      long locals = 0;
      for (int j = 0; j < groups; j++) {
        locals += body.u32();
        if (locals > MAX_LOCALS_PER_FUNCTION) throw invalid("too many locals in function body");
        body.valueType();
      }
      budget.locals += locals;
      if (budget.locals > MAX_TOTAL_LOCALS) throw invalid("too many locals in module");
      if (body.remaining() == 0) throw invalid("empty function expression");
      // Instructions have no input-independent allocation in Endive. Endive parses and
      // validates their operands. The body length has already been bounded by the section.
    }
  }

  private static void data(Cursor s) throws WasmException {
    int count = s.count(2);
    for (int i = 0; i < count; i++) {
      long mode = s.u32();
      switch ((int) mode) {
        case 0 -> s.expression();
        case 1 -> { }
        case 2 -> { s.u32(); s.expression(); }
        default -> throw invalid("unsupported data segment mode " + mode);
      }
      s.skip(s.length());
    }
  }

  private static WasmException invalid(String why) {
    return new WasmException(Kind.INVALID_MODULE, "invalid WebAssembly module: " + why);
  }

  private static final class Budget {
    long typeEntries;
    long elementEntries;
    long locals;
  }

  private static final class Cursor {
    final byte[] bytes;
    int pos;
    final int end;

    Cursor(byte[] bytes, int pos, int end) {
      this.bytes = bytes;
      this.pos = pos;
      this.end = end;
    }

    int remaining() { return end - pos; }

    int u8() throws WasmException {
      if (remaining() < 1) throw invalid("unexpected end");
      return bytes[pos++] & 0xff;
    }

    long u32() throws WasmException {
      long value = 0;
      for (int i = 0; i < 5; i++) {
        int b = u8();
        if (i == 4 && (b & 0xf0) != 0) throw invalid("integer too large");
        value |= (long) (b & 0x7f) << (7 * i);
        if ((b & 0x80) == 0) return value;
      }
      throw invalid("integer representation too long");
    }

    int length() throws WasmException {
      long value = u32();
      if (value > Integer.MAX_VALUE) throw invalid("length too large");
      return (int) value;
    }

    int count(int minimumBytes) throws WasmException {
      int count = length();
      if (count > remaining() / minimumBytes) throw invalid("vector length exceeds section");
      return count;
    }

    void skip(int size) throws WasmException {
      if (size > remaining()) throw invalid("length exceeds section");
      pos += size;
    }

    Cursor section(int size) throws WasmException {
      if (size > remaining()) throw invalid("section length exceeds input");
      Cursor part = new Cursor(bytes, pos, pos + size);
      pos += size;
      return part;
    }

    void name() throws WasmException { skip(length()); }

    void valueType() throws WasmException {
      int type = length();
      if (type == 0x63 || type == 0x64) signedLeb();
    }

    void fieldType() throws WasmException {
      valueType();
      u8(); // mutability
    }

    void signedLeb() throws WasmException {
      for (int i = 0; i < 10; i++) if ((u8() & 0x80) == 0) return;
      throw invalid("signed integer representation too long");
    }

    void expression() throws WasmException {
      // Constant expressions accepted by WASI toolchains. Reject other forms before Endive
      // reaches a length field in the following segment.
      for (;;) {
        switch (u8()) {
          case 0x0b -> { return; }
          case 0x41, 0x42, 0x23, 0xd2, 0xd0 -> signedLeb();
          case 0x43 -> skip(4);
          case 0x44 -> skip(8);
          default -> throw invalid("unsupported constant expression");
        }
      }
    }
  }
}
