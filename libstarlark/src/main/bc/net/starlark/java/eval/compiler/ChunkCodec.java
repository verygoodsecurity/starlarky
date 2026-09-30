// Copyright 2025 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package net.starlark.java.eval.compiler;

import com.google.common.collect.ImmutableList;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.starlark.java.eval.BytecodeFunction;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.syntax.Location;

/**
 * Binary form of a {@link BytecodeChunk}, including nested function chunks. Used by {@link
 * CompiledModule}; see there for the file header and compatibility checks.
 */
public final class ChunkCodec {

  private ChunkCodec() {}

  private static final byte STRING = 0;
  private static final byte INT = 1;
  private static final byte BIGINT = 2;
  private static final byte FLOAT = 3;
  private static final byte BYTES = 4;
  private static final byte MANDATORY = 5;
  private static final byte FUNCTION = 6;

  // ---- writing ----

  public static void write(DataOutputStream out, BytecodeChunk chunk) throws IOException {
    writeString(out, chunk.getName());
    out.writeInt(chunk.getLocalCount());
    out.writeInt(chunk.getParameterCount());
    writeStrings(out, chunk.getParameterNames());
    writeStrings(out, chunk.getLocalNames());

    List<ComprehensionScope> scopes = chunk.getLocalScopes();
    out.writeInt(scopes.size());
    for (ComprehensionScope scope : scopes) {
      out.writeBoolean(scope != null);
      if (scope != null) {
        writeString(out, scope.start().file());
        writePosition(out, scope.firstIterableStart());
        writePosition(out, scope.firstIterableEnd());
        writePosition(out, scope.start());
        writePosition(out, scope.end());
      }
    }

    writeInts(out, chunk.getLineNumbers());
    writeInts(out, chunk.getColumnNumbers());

    List<Object> constants = chunk.getConstantPool().getConstants();
    out.writeInt(constants.size());
    for (Object constant : constants) {
      writeConstant(out, constant);
    }

    List<Instruction> code = chunk.getInstructions();
    out.writeInt(code.size());
    for (Instruction in : code) {
      Opcode op = in.getOpcode();
      out.writeShort(op.ordinal());
      if (op.getOperandCount() >= 1) {
        out.writeInt(in.getOperand1());
      }
      if (op.getOperandCount() >= 2) {
        out.writeInt(in.getOperand2());
      }
    }
  }

  private static void writeConstant(DataOutputStream out, Object c) throws IOException {
    if (c instanceof String s) {
      out.writeByte(STRING);
      writeString(out, s);
    } else if (c instanceof StarlarkInt i) {
      BigInteger big = i.toBigInteger();
      if (big.bitLength() < 64) {
        out.writeByte(INT);
        out.writeLong(big.longValue());
      } else {
        byte[] bytes = big.toByteArray();
        out.writeByte(BIGINT);
        out.writeInt(bytes.length);
        out.write(bytes);
      }
    } else if (c instanceof StarlarkFloat f) {
      out.writeByte(FLOAT);
      out.writeLong(Double.doubleToRawLongBits(f.toDouble()));
    } else if (c instanceof byte[] b) {
      out.writeByte(BYTES);
      out.writeInt(b.length);
      out.write(b);
    } else if (c == BytecodeFunction.MANDATORY) {
      out.writeByte(MANDATORY);
    } else if (c instanceof FunctionDescriptor d) {
      if (!d.getDefaultValues().isEmpty()) {
        throw new IOException("function descriptor with compile-time defaults: " + d.getName());
      }
      out.writeByte(FUNCTION);
      writeString(out, d.getName());
      writeLocation(out, d.getLocation());
      writeStrings(out, d.getParameterNames());
      out.writeBoolean(d.hasVarargs());
      out.writeBoolean(d.hasKwargs());
      out.writeInt(d.getNumKeywordOnlyParams());
      out.writeInt(d.getLocalCount());
      out.writeInt(d.getFreevarInfos().size());
      for (FunctionDescriptor.FreevarInfo info : d.getFreevarInfos()) {
        out.writeBoolean(info.isFromEnclosingFreevars);
        out.writeInt(info.index);
      }
      writeInts(out, d.getCellIndices());
      write(out, d.getChunk());
    } else {
      throw new IOException("cannot serialize constant of type " + c.getClass().getName());
    }
  }

  // ---- reading ----

  public static BytecodeChunk read(DataInputStream in) throws IOException {
    String name = readString(in);
    int localCount = in.readInt();
    int parameterCount = in.readInt();
    List<String> parameterNames = readStrings(in);
    List<String> localNames = readStrings(in);

    int nScopes = in.readInt();
    List<ComprehensionScope> scopes = new ArrayList<>(nScopes);
    for (int i = 0; i < nScopes; i++) {
      if (in.readBoolean()) {
        String file = readString(in);
        scopes.add(
            new ComprehensionScope(
                readPosition(in, file),
                readPosition(in, file),
                readPosition(in, file),
                readPosition(in, file)));
      } else {
        scopes.add(null);
      }
    }

    List<Integer> lines = readInts(in);
    List<Integer> columns = readInts(in);

    int nConstants = in.readInt();
    List<Object> constants = new ArrayList<>(nConstants);
    for (int i = 0; i < nConstants; i++) {
      constants.add(readConstant(in));
    }

    int nCode = in.readInt();
    Opcode[] opcodes = Opcode.values();
    List<Instruction> code = new ArrayList<>(nCode);
    int offset = 0;
    for (int i = 0; i < nCode; i++) {
      int ordinal = in.readShort();
      if (ordinal < 0 || ordinal >= opcodes.length) {
        throw new IOException("bad opcode " + ordinal);
      }
      Opcode op = opcodes[ordinal];
      Instruction instr;
      switch (op.getOperandCount()) {
        case 0:
          instr = Instruction.create(op, offset);
          break;
        case 1:
          instr = Instruction.create(op, in.readInt(), offset);
          break;
        default:
          int a = in.readInt();
          instr = Instruction.create(op, a, in.readInt(), offset);
      }
      code.add(instr);
      offset += instr.getSize();
    }
    return BytecodeChunk.restore(
        name,
        ConstantPool.restore(constants),
        code,
        localCount,
        parameterCount,
        parameterNames,
        localNames,
        scopes,
        lines,
        columns);
  }

  private static Object readConstant(DataInputStream in) throws IOException {
    byte tag = in.readByte();
    switch (tag) {
      case STRING:
        return readString(in);
      case INT:
        return StarlarkInt.of(in.readLong());
      case BIGINT:
        {
          byte[] bytes = new byte[in.readInt()];
          in.readFully(bytes);
          return StarlarkInt.of(new BigInteger(bytes));
        }
      case FLOAT:
        return StarlarkFloat.of(Double.longBitsToDouble(in.readLong()));
      case BYTES:
        {
          byte[] bytes = new byte[in.readInt()];
          in.readFully(bytes);
          return bytes;
        }
      case MANDATORY:
        return BytecodeFunction.MANDATORY;
      case FUNCTION:
        {
          String name = readString(in);
          Location location = readLocation(in);
          ImmutableList<String> parameterNames = ImmutableList.copyOf(readStrings(in));
          boolean hasVarargs = in.readBoolean();
          boolean hasKwargs = in.readBoolean();
          int numKeywordOnly = in.readInt();
          int localCount = in.readInt();
          int nFree = in.readInt();
          ImmutableList.Builder<FunctionDescriptor.FreevarInfo> freevars = ImmutableList.builder();
          for (int i = 0; i < nFree; i++) {
            boolean fromEnclosing = in.readBoolean();
            freevars.add(new FunctionDescriptor.FreevarInfo(fromEnclosing, in.readInt()));
          }
          ImmutableList<Integer> cellIndices = ImmutableList.copyOf(readInts(in));
          BytecodeChunk chunk = read(in);
          return new FunctionDescriptor(
              name,
              location,
              chunk,
              parameterNames,
              hasVarargs,
              hasKwargs,
              numKeywordOnly,
              ImmutableList.of(),
              localCount,
              freevars.build(),
              cellIndices);
        }
      default:
        throw new IOException("bad constant tag " + tag);
    }
  }

  // ---- primitives ----

  public static void writeString(DataOutputStream out, String s) throws IOException {
    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
    out.writeInt(bytes.length);
    out.write(bytes);
  }

  public static String readString(DataInputStream in) throws IOException {
    byte[] bytes = new byte[in.readInt()];
    in.readFully(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  public static void writeStrings(DataOutputStream out, List<String> list) throws IOException {
    out.writeInt(list.size());
    for (String s : list) {
      writeString(out, s);
    }
  }

  public static List<String> readStrings(DataInputStream in) throws IOException {
    int n = in.readInt();
    List<String> list = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      list.add(readString(in));
    }
    return list;
  }

  private static void writeInts(DataOutputStream out, List<Integer> list) throws IOException {
    out.writeInt(list.size());
    for (int v : list) {
      out.writeInt(v);
    }
  }

  private static List<Integer> readInts(DataInputStream in) throws IOException {
    int n = in.readInt();
    List<Integer> list = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      list.add(in.readInt());
    }
    return list;
  }

  private static void writePosition(DataOutputStream out, Location loc) throws IOException {
    out.writeInt(loc.line());
    out.writeInt(loc.column());
  }

  private static Location readPosition(DataInputStream in, String file) throws IOException {
    int line = in.readInt();
    return Location.fromFileLineColumn(file, line, in.readInt());
  }

  public static void writeLocation(DataOutputStream out, Location loc) throws IOException {
    writeString(out, loc.file());
    writePosition(out, loc);
  }

  public static Location readLocation(DataInputStream in) throws IOException {
    return readPosition(in, readString(in));
  }
}
