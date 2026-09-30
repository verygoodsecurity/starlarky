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

package net.starlark.java.eval;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import javax.annotation.Nullable;
import net.starlark.java.eval.compiler.BytecodeChunk;
import net.starlark.java.eval.compiler.BytecodeCompiler;
import net.starlark.java.eval.compiler.ChunkCodec;
import net.starlark.java.eval.compiler.Opcode;
import net.starlark.java.syntax.Identifier;
import net.starlark.java.syntax.NodeVisitor;
import net.starlark.java.syntax.Program;
import net.starlark.java.syntax.Resolver;
import net.starlark.java.syntax.StarlarkFile;

/**
 * A Starlark file compiled to bytecode, in a form that can be saved and loaded without parsing
 * or resolving the source again (for example, precompiled at build time).
 *
 * <p>The file format starts with a header that must match this build exactly: a magic number, the
 * format version, {@link BytecodeCompiler#VERSION} and a fingerprint of the opcode set; a mismatch
 * makes {@link #read} fail, and the caller compiles the source instead.
 *
 * <p>Resolution depends on the environment only through the names the file resolved as
 * PREDECLARED or UNIVERSAL; those are recorded, and {@link #resolvesTheSameIn} checks them against
 * the module the file is about to run in.
 */
public final class CompiledModule {

  private static final int MAGIC = 0x534C4243; // "SLBC"
  private static final int FORMAT_VERSION = 1;
  private static final int OPCODE_FINGERPRINT = Arrays.toString(Opcode.values()).hashCode();

  private final String filename;
  private final ImmutableList<String> loads;
  @Nullable private final String documentation;
  private final ImmutableSet<String> predeclared;
  private final ImmutableSet<String> universal;
  private final byte[] sourceDigest;
  private final BytecodeChunk chunk;

  private CompiledModule(
      String filename,
      ImmutableList<String> loads,
      @Nullable String documentation,
      ImmutableSet<String> predeclared,
      ImmutableSet<String> universal,
      byte[] sourceDigest,
      BytecodeChunk chunk) {
    this.filename = filename;
    this.loads = loads;
    this.documentation = documentation;
    this.predeclared = predeclared;
    this.universal = universal;
    this.sourceDigest = sourceDigest;
    this.chunk = chunk;
  }

  /**
   * Captures {@code program}, compiled from {@code file} with bytecode enabled.
   *
   * @param sourceDigest a digest of the source text, recorded for staleness checks
   */
  public static CompiledModule of(Program program, StarlarkFile file, byte[] sourceDigest) {
    BytecodeChunk chunk = program.getBytecode();
    if (chunk == null) {
      throw new IllegalArgumentException(program.getFilename() + " was not compiled to bytecode");
    }
    ImmutableSet.Builder<String> predeclared = ImmutableSet.builder();
    ImmutableSet.Builder<String> universal = ImmutableSet.builder();
    new NodeVisitor() {
      @Override
      public void visit(Identifier id) {
        Resolver.Binding binding = id.getBinding();
        if (binding == null) {
          return;
        }
        if (binding.getScope() == Resolver.Scope.PREDECLARED) {
          predeclared.add(id.getName());
        } else if (binding.getScope() == Resolver.Scope.UNIVERSAL) {
          universal.add(id.getName());
        }
      }
    }.visit(file);
    return new CompiledModule(
        program.getFilename(),
        program.getLoads(),
        program.getResolvedFunction().getDocumentation(),
        predeclared.build(),
        universal.build(),
        sourceDigest.clone(),
        chunk);
  }

  public String getFilename() {
    return filename;
  }

  /** The modules this file loads, in source order. */
  public ImmutableList<String> getLoads() {
    return loads;
  }

  /** The names the file resolved as PREDECLARED. */
  public ImmutableSet<String> getPredeclaredNames() {
    return predeclared;
  }

  public byte[] getSourceDigest() {
    return sourceDigest.clone();
  }

  /**
   * Reports whether the source would resolve the same way in {@code module}: its PREDECLARED
   * names are still predeclared, and none of its UNIVERSAL names is shadowed by a predeclared one.
   */
  public boolean resolvesTheSameIn(Module module) {
    Map<String, Object> env = module.getPredeclaredBindings();
    for (String name : predeclared) {
      if (!env.containsKey(name)) {
        return false;
      }
    }
    for (String name : universal) {
      if (env.containsKey(name)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Executes the file's top-level code in {@code module} on the configured bytecode VM, as
   * {@link Starlark#execFileProgram} does for a Program compiled to bytecode.
   */
  public Object exec(Module module, StarlarkThread thread)
      throws EvalException, InterruptedException {
    if (module.getDocumentation() == null && documentation != null) {
      module.setDocumentation(Starlark.trimDocString(documentation));
    }
    return BytecodeVms.execute(
        chunk, thread, new BytecodeGlobals(module, module.getPredeclaredBindings()), filename);
  }

  // ---- serialization ----

  public byte[] toBytes() throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    write(bytes);
    return bytes.toByteArray();
  }

  public void write(OutputStream stream) throws IOException {
    DataOutputStream out = new DataOutputStream(stream);
    out.writeInt(MAGIC);
    out.writeInt(FORMAT_VERSION);
    out.writeInt(BytecodeCompiler.VERSION);
    out.writeInt(OPCODE_FINGERPRINT);
    ChunkCodec.writeString(out, filename);
    out.writeInt(sourceDigest.length);
    out.write(sourceDigest);
    ChunkCodec.writeStrings(out, loads);
    out.writeBoolean(documentation != null);
    if (documentation != null) {
      ChunkCodec.writeString(out, documentation);
    }
    ChunkCodec.writeStrings(out, predeclared.asList());
    ChunkCodec.writeStrings(out, universal.asList());
    ChunkCodec.write(out, chunk);
    out.flush();
  }

  /**
   * Reads a module written by {@link #write}.
   *
   * @throws IOException if the data is malformed or was written by an incompatible build
   */
  public static CompiledModule read(InputStream stream) throws IOException {
    DataInputStream in = new DataInputStream(stream);
    if (in.readInt() != MAGIC) {
      throw new IOException("not a compiled Starlark module");
    }
    int format = in.readInt();
    int compiler = in.readInt();
    int opcodes = in.readInt();
    if (format != FORMAT_VERSION
        || compiler != BytecodeCompiler.VERSION
        || opcodes != OPCODE_FINGERPRINT) {
      throw new IOException(
          String.format(
              "compiled by an incompatible build (format %d, compiler %d, opcodes %08x;"
                  + " this build: %d, %d, %08x)",
              format, compiler, opcodes, FORMAT_VERSION, BytecodeCompiler.VERSION,
              OPCODE_FINGERPRINT));
    }
    String filename = ChunkCodec.readString(in);
    byte[] digest = new byte[in.readInt()];
    in.readFully(digest);
    ImmutableList<String> loads = ImmutableList.copyOf(ChunkCodec.readStrings(in));
    String documentation = in.readBoolean() ? ChunkCodec.readString(in) : null;
    ImmutableSet<String> predeclared = ImmutableSet.copyOf(ChunkCodec.readStrings(in));
    ImmutableSet<String> universal = ImmutableSet.copyOf(ChunkCodec.readStrings(in));
    BytecodeChunk chunk = ChunkCodec.read(in);
    return new CompiledModule(
        filename, loads, documentation, predeclared, universal, digest, chunk);
  }
}
