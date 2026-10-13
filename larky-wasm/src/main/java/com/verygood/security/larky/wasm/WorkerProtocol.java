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

import com.verygood.security.larky.wasm.WasmRuntime.WasiHostPolicy;
import com.verygood.security.larky.wasm.WasmRuntime.WasmException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** A bounded, versioned one-request protocol. No Java object deserialization crosses the pipe. */
final class WorkerProtocol {
  static final int MAGIC = 0x4c574b31; // LWK1
  static final int COMPILE = 1;
  static final int RUN = 2;
  static final int ENDIVE = 1;
  static final int GRAAL_ISOLATE = 2;
  static final int SUCCESS = 0;
  static final int FAILURE = 1;
  static final int MAX_MESSAGE_BYTES = 1024;
  static final int MAX_POLICY_FUNCTIONS = 64;
  static final int MAX_FUNCTION_NAME_BYTES = 64;

  private static int errorCode(WasmException.Kind kind) throws IOException {
    return switch (kind) {
      case INVALID_MODULE -> 1;
      case TRAP -> 2;
      case MEMORY_LIMIT -> 3;
      case OUTPUT_LIMIT -> 4;
      case TIMEOUT -> 5;
    };
  }

  private static WasmException.Kind errorKind(int code) throws IOException {
    return switch (code) {
      case 1 -> WasmException.Kind.INVALID_MODULE;
      case 2 -> WasmException.Kind.TRAP;
      case 3 -> WasmException.Kind.MEMORY_LIMIT;
      case 4 -> WasmException.Kind.OUTPUT_LIMIT;
      case 5 -> WasmException.Kind.TIMEOUT;
      default -> throw new IOException("bad worker error kind");
    };
  }

  record Request(int operation, int backend, byte[] wasm, byte[] stdin, long maxMemoryBytes,
      long remainingMillis, int maxOutputBytes, Long randomSeed, WasiHostPolicy policy) {}

  record Response(WasmRuntime.Result result, WasmException error) {}

  private WorkerProtocol() {}

  static byte[] encodeRequest(Request request) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(bytes);
    out.writeInt(MAGIC);
    out.writeByte(request.operation());
    out.writeByte(request.backend());
    out.writeLong(request.maxMemoryBytes());
    out.writeLong(request.remainingMillis());
    out.writeInt(request.maxOutputBytes());
    out.writeBoolean(request.randomSeed() != null);
    if (request.randomSeed() != null) out.writeLong(request.randomSeed());
    Set<String> functions = request.policy().enabledFunctions();
    out.writeInt(functions.size());
    for (String name : functions.stream().sorted().toList()) {
      byte[] nameBytes = name.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
      if (nameBytes.length > MAX_FUNCTION_NAME_BYTES) throw new IOException("WASI name too long");
      out.writeByte(nameBytes.length);
      out.write(nameBytes);
    }
    out.writeInt(request.wasm().length);
    out.write(request.wasm());
    out.writeInt(request.stdin().length);
    out.write(request.stdin());
    return bytes.toByteArray();
  }

  static Request readRequest(DataInputStream in, int maxModuleBytes, int maxInputBytes)
      throws IOException {
    if (in.readInt() != MAGIC) throw new IOException("bad worker request magic");
    int operation = in.readUnsignedByte();
    int backend = in.readUnsignedByte();
    if ((operation != COMPILE && operation != RUN)
        || (backend != ENDIVE && backend != GRAAL_ISOLATE)) {
      throw new IOException("unsupported worker operation or backend");
    }
    long memory = in.readLong();
    long remaining = in.readLong();
    int output = in.readInt();
    if (memory < 0 || remaining <= 0 || output < 0) throw new IOException("invalid worker limits");
    Long seed = in.readBoolean() ? in.readLong() : null;
    int count = in.readInt();
    if (count < 0 || count > MAX_POLICY_FUNCTIONS) throw new IOException("WASI policy too large");
    Set<String> functions = new HashSet<>();
    for (int i = 0; i < count; i++) {
      int length = in.readUnsignedByte();
      if (length == 0 || length > MAX_FUNCTION_NAME_BYTES) throw new IOException("bad WASI name");
      byte[] name = in.readNBytes(length);
      if (name.length != length) throw new IOException("truncated WASI name");
      functions.add(new String(name, java.nio.charset.StandardCharsets.US_ASCII));
    }
    int wasmLength = in.readInt();
    if (wasmLength < 0 || wasmLength > maxModuleBytes) throw new IOException("module too large");
    byte[] wasm = in.readNBytes(wasmLength);
    if (wasm.length != wasmLength) throw new IOException("truncated module");
    int inputLength = in.readInt();
    if (inputLength < 0 || inputLength > maxInputBytes || operation == COMPILE && inputLength != 0) {
      throw new IOException("input too large");
    }
    byte[] stdin = in.readNBytes(inputLength);
    if (stdin.length != inputLength || in.read() != -1) throw new IOException("trailing request bytes");
    try {
      return new Request(operation, backend, wasm, stdin, memory, remaining, output, seed,
          new WasiHostPolicy(functions));
    } catch (IllegalArgumentException e) {
      throw new IOException("invalid WASI policy", e);
    }
  }

  static byte[] encodeSuccess(WasmRuntime.Result result) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(bytes);
    out.writeInt(MAGIC);
    out.writeByte(SUCCESS);
    out.writeInt(result.exitCode());
    out.writeInt(result.stdout().length);
    out.write(result.stdout());
    out.writeInt(result.stderr().length);
    out.write(result.stderr());
    return bytes.toByteArray();
  }

  static byte[] encodeFailure(WasmException error) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(bytes);
    out.writeInt(MAGIC);
    out.writeByte(FAILURE);
    out.writeByte(errorCode(error.kind()));
    byte[] message = error.getMessage().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    out.writeInt(Math.min(message.length, MAX_MESSAGE_BYTES));
    out.write(message, 0, Math.min(message.length, MAX_MESSAGE_BYTES));
    return bytes.toByteArray();
  }

  static Response decodeResponse(byte[] bytes, int maxOutputBytes) throws IOException {
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
    if (in.readInt() != MAGIC) throw new IOException("bad worker response magic");
    int status = in.readUnsignedByte();
    if (status == SUCCESS) {
      int exitCode = in.readInt();
      byte[] stdout = readBytes(in, maxOutputBytes);
      byte[] stderr = readBytes(in, maxOutputBytes);
      if (in.read() != -1) throw new IOException("trailing worker response");
      return new Response(new WasmRuntime.Result(exitCode, stdout, stderr), null);
    }
    if (status == FAILURE) {
      int kind = in.readUnsignedByte();
      byte[] message = readBytes(in, MAX_MESSAGE_BYTES);
      if (in.read() != -1) throw new IOException("trailing worker error");
      return new Response(null, new WasmException(errorKind(kind),
          new String(message, java.nio.charset.StandardCharsets.UTF_8)));
    }
    throw new IOException("bad worker status");
  }

  private static byte[] readBytes(DataInputStream in, int limit) throws IOException {
    int size = in.readInt();
    if (size < 0 || size > limit) throw new IOException("worker response exceeded limit");
    byte[] value = in.readNBytes(size);
    if (value.length != size) throw new IOException("truncated worker response");
    return value;
  }
}
