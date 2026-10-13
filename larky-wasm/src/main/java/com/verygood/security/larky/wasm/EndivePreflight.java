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
  private static final byte[] WASM_HEADER = {0, 'a', 's', 'm', 1, 0, 0, 0};
  private static final int MAX_LOCALS_PER_FUNCTION = 50_000;
  private static final int MAX_TOTAL_LOCALS = 100_000;
  private static final int MAX_TYPE_ENTRIES = 100_000;
  private static final int MAX_FUNCTION_ARITY = 1_000;
  private static final int MIN_SINGLE_BYTE_ENTRY = 1;
  private static final int MIN_TWO_BYTE_ENTRY = 2;
  private static final int MIN_THREE_BYTE_ENTRY = 3;
  private static final int F32_BYTES = 4;
  private static final int F64_BYTES = 8;
  private static final int FUNCTION_IMPORT_KIND = 0;
  private static final int UNSIGNED_BYTE_MASK = 0xff;
  private static final int MAX_U32_LEB_BYTES = 5;
  private static final int MAX_SIGNED_LEB_BYTES = 10;
  private static final int LEB_PAYLOAD_BITS = 7;
  private static final int LEB_PAYLOAD_MASK = 0x7f;
  private static final int LEB_CONTINUATION_BIT = 0x80;
  private static final int U32_FINAL_BYTE_OVERFLOW_BITS = 0xf0;
  private static final int REF_TYPE = 0x64;
  private static final int REF_NULL_TYPE = 0x63;
  private static final int ELEMENT_FLAG_PASSIVE = 1;
  private static final int ELEMENT_FLAG_EXPLICIT_TABLE = 2;
  private static final int ELEMENT_FLAG_EXPRESSIONS = 4;
  private static final int MAX_ELEMENT_FLAGS = ELEMENT_FLAG_PASSIVE | ELEMENT_FLAG_EXPLICIT_TABLE
      | ELEMENT_FLAG_EXPRESSIONS;

  private enum SectionId {
    CUSTOM(0, false), TYPE(1, true), IMPORT(2, true), FUNCTION(3, true),
    TABLE(4, false), MEMORY(5, false), GLOBAL(6, false), EXPORT(7, true),
    START(8, false), ELEMENT(9, true), CODE(10, true), DATA(11, true),
    DATA_COUNT(12, false), OTHER(-1, false);

    private static final SectionId[] BY_WIRE_ID = new SectionId[DATA_COUNT.wireId + 1];

    static {
      for (SectionId section : values()) {
        if (section.wireId >= 0) BY_WIRE_ID[section.wireId] = section;
      }
    }

    final int wireId;
    final boolean fullyParsed;

    SectionId(int wireId, boolean fullyParsed) {
      this.wireId = wireId;
      this.fullyParsed = fullyParsed;
    }

    static SectionId fromWireId(int wireId) {
      return wireId < BY_WIRE_ID.length ? BY_WIRE_ID[wireId] : OTHER;
    }
  }

  private enum TypeForm {
    REC(0x4e), SUB_FINAL(0x4f), SUB(0x50), ARRAY(0x5e), STRUCT(0x5f), FUNCTION(0x60),
    OTHER(-1);

    private static final TypeForm[] BY_WIRE_ID = new TypeForm[FUNCTION.wireId + 1];

    static {
      for (TypeForm form : values()) {
        if (form.wireId >= 0) BY_WIRE_ID[form.wireId] = form;
      }
    }

    final int wireId;

    TypeForm(int wireId) {
      this.wireId = wireId;
    }

    static TypeForm fromWireId(int wireId) {
      if (wireId >= BY_WIRE_ID.length) return OTHER;
      TypeForm form = BY_WIRE_ID[wireId];
      return form != null ? form : OTHER;
    }
  }

  private enum DataMode {
    ACTIVE(0), PASSIVE(1), ACTIVE_EXPLICIT_MEMORY(2), OTHER(-1);

    final int wireId;

    DataMode(int wireId) {
      this.wireId = wireId;
    }

    static DataMode fromWireId(long wireId) {
      if (wireId == ACTIVE.wireId) return ACTIVE;
      if (wireId == PASSIVE.wireId) return PASSIVE;
      if (wireId == ACTIVE_EXPLICIT_MEMORY.wireId) return ACTIVE_EXPLICIT_MEMORY;
      return OTHER;
    }
  }

  private static final class ConstOpcode {
    static final int END = 0x0b;
    static final int I32_CONST = 0x41;
    static final int I64_CONST = 0x42;
    static final int F32_CONST = 0x43;
    static final int F64_CONST = 0x44;
    static final int GLOBAL_GET = 0x23;
    static final int REF_NULL = 0xd0;
    static final int REF_FUNC = 0xd2;

    private ConstOpcode() {}
  }

  private EndivePreflight() {}

  static void check(byte[] wasm) throws WasmException {
    Cursor r = new Cursor(wasm, 0, wasm.length);
    for (byte expected : WASM_HEADER) {
      if (r.u8() != (expected & UNSIGNED_BYTE_MASK)) throw invalid("bad WebAssembly header");
    }
    Budget budget = new Budget();
    while (r.remaining() != 0) {
      int wireId = r.u8();
      SectionId id = SectionId.fromWireId(wireId);
      int size = r.length();
      Cursor s = r.section(size);
      switch (id) {
        case CUSTOM -> s.name();
        case TYPE -> types(s, budget);
        case IMPORT -> imports(s);
        case FUNCTION -> functions(s);
        case EXPORT -> exports(s);
        case ELEMENT -> elements(s, budget);
        case CODE -> code(s, budget);
        case DATA -> data(s);
        default -> { /* Endive validates other sections after all dangerous lengths are checked. */ }
      }
      if (id.fullyParsed && s.remaining() != 0) {
        throw invalid("section " + wireId + " has trailing bytes");
      }
    }
  }

  private static void types(Cursor s, Budget budget) throws WasmException {
    int count = s.count(MIN_SINGLE_BYTE_ENTRY);
    if (count > WasmRuntime.MAX_FUNCTIONS) throw invalid("too many types");
    for (int i = 0; i < count; i++) {
      int wireForm = s.length();
      if (TypeForm.fromWireId(wireForm) == TypeForm.REC) {
        int subtypes = s.count(MIN_SINGLE_BYTE_ENTRY);
        budget.typeEntries += subtypes;
        if (budget.typeEntries > MAX_TYPE_ENTRIES) throw invalid("too many type entries");
        for (int j = 0; j < subtypes; j++) subtype(s, s.length(), budget);
      } else {
        budget.typeEntries++;
        subtype(s, wireForm, budget);
      }
    }
  }

  private static void subtype(Cursor s, int form, Budget budget) throws WasmException {
    TypeForm typeForm = TypeForm.fromWireId(form);
    if (typeForm == TypeForm.SUB || typeForm == TypeForm.SUB_FINAL) {
      int parents = s.count(MIN_SINGLE_BYTE_ENTRY);
      budget.typeEntries += parents;
      if (budget.typeEntries > MAX_TYPE_ENTRIES) throw invalid("too many type entries");
      for (int i = 0; i < parents; i++) s.u32();
      form = s.length();
      typeForm = TypeForm.fromWireId(form);
    }
    switch (typeForm) {
      case FUNCTION -> {
        int params = s.count(MIN_SINGLE_BYTE_ENTRY);
        if (params > MAX_FUNCTION_ARITY) throw invalid("too many function parameters");
        budget.typeEntries += params;
        if (budget.typeEntries > MAX_TYPE_ENTRIES) throw invalid("too many type entries");
        for (int i = 0; i < params; i++) s.valueType();
        int results = s.count(MIN_SINGLE_BYTE_ENTRY);
        if (results > MAX_FUNCTION_ARITY) throw invalid("too many function results");
        budget.typeEntries += results;
        if (budget.typeEntries > MAX_TYPE_ENTRIES) throw invalid("too many type entries");
        for (int i = 0; i < results; i++) s.valueType();
      }
      case ARRAY -> s.fieldType();
      case STRUCT -> {
        int fields = s.count(MIN_TWO_BYTE_ENTRY);
        budget.typeEntries += fields;
        if (budget.typeEntries > MAX_TYPE_ENTRIES) throw invalid("too many type entries");
        for (int i = 0; i < fields; i++) s.fieldType();
      }
      default -> throw invalid("unsupported type form " + form);
    }
  }

  private static void imports(Cursor s) throws WasmException {
    int count = s.count(MIN_THREE_BYTE_ENTRY);
    for (int i = 0; i < count; i++) {
      s.name();
      s.name();
      // The WASI contract rejects all non-function imports. Reject them before Endive parses
      // their descriptors, so a malformed descriptor cannot reach another allocation path.
      if (s.u8() != FUNCTION_IMPORT_KIND) throw invalid("only function imports are allowed");
      s.u32();
    }
  }

  private static void functions(Cursor s) throws WasmException {
    int count = s.count(MIN_SINGLE_BYTE_ENTRY);
    if (count > WasmRuntime.MAX_FUNCTIONS) throw invalid("too many functions");
    for (int i = 0; i < count; i++) s.u32();
  }

  private static void exports(Cursor s) throws WasmException {
    int count = s.count(MIN_THREE_BYTE_ENTRY);
    for (int i = 0; i < count; i++) {
      s.name();
      s.u8();
      s.u32();
    }
  }

  private static void elements(Cursor s, Budget budget) throws WasmException {
    int count = s.count(MIN_TWO_BYTE_ENTRY);
    if (count > WasmRuntime.MAX_TABLE_ELEMENTS) throw invalid("too many element segments");
    for (int i = 0; i < count; i++) {
      long flags = s.u32();
      if (flags > MAX_ELEMENT_FLAGS) throw invalid("unsupported element flags");
      boolean active = (flags & ELEMENT_FLAG_PASSIVE) == 0;
      boolean explicitTable = active && (flags & ELEMENT_FLAG_EXPLICIT_TABLE) != 0;
      boolean expressions = (flags & ELEMENT_FLAG_EXPRESSIONS) != 0;
      if (explicitTable) s.u32();
      if (active) s.expression();
      if (!active || explicitTable) {
        if (expressions) s.valueType();
        else s.u32(); // elemkind
      }
      int initializers = s.count(MIN_SINGLE_BYTE_ENTRY);
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
    int count = s.count(MIN_TWO_BYTE_ENTRY);
    if (count > WasmRuntime.MAX_FUNCTIONS) throw invalid("too many function bodies");
    for (int i = 0; i < count; i++) {
      Cursor body = s.section(s.length());
      int groups = body.count(MIN_TWO_BYTE_ENTRY);
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
    int count = s.count(MIN_TWO_BYTE_ENTRY);
    for (int i = 0; i < count; i++) {
      long mode = s.u32();
      switch (DataMode.fromWireId(mode)) {
        case ACTIVE -> s.expression();
        case PASSIVE -> { }
        case ACTIVE_EXPLICIT_MEMORY -> { s.u32(); s.expression(); }
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
      return bytes[pos++] & UNSIGNED_BYTE_MASK;
    }

    long u32() throws WasmException {
      long value = 0;
      for (int i = 0; i < MAX_U32_LEB_BYTES; i++) {
        int b = u8();
        if (i == MAX_U32_LEB_BYTES - 1 && (b & U32_FINAL_BYTE_OVERFLOW_BITS) != 0) {
          throw invalid("integer too large");
        }
        value |= (long) (b & LEB_PAYLOAD_MASK) << (LEB_PAYLOAD_BITS * i);
        if ((b & LEB_CONTINUATION_BIT) == 0) return value;
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
      if (type == REF_NULL_TYPE || type == REF_TYPE) signedLeb();
    }

    void fieldType() throws WasmException {
      valueType();
      u8(); // mutability
    }

    void signedLeb() throws WasmException {
      for (int i = 0; i < MAX_SIGNED_LEB_BYTES; i++) {
        if ((u8() & LEB_CONTINUATION_BIT) == 0) return;
      }
      throw invalid("signed integer representation too long");
    }

    void expression() throws WasmException {
      // Constant expressions accepted by WASI toolchains. Reject other forms before Endive
      // reaches a length field in the following segment.
      for (;;) {
        switch (u8()) {
          case ConstOpcode.END -> { return; }
          case ConstOpcode.I32_CONST, ConstOpcode.I64_CONST, ConstOpcode.GLOBAL_GET,
              ConstOpcode.REF_FUNC, ConstOpcode.REF_NULL -> signedLeb();
          case ConstOpcode.F32_CONST -> skip(F32_BYTES);
          case ConstOpcode.F64_CONST -> skip(F64_BYTES);
          default -> throw invalid("unsupported constant expression");
        }
      }
    }
  }
}
