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

import net.starlark.java.syntax.Resolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A chunk of bytecode representing a compiled Starlark function or module.
 *
 * A bytecode chunk contains:
 * - A constant pool
 * - A sequence of instructions
 * - Metadata (arity, local count, etc.)
 * - Debug information (line numbers, source locations)
 */
public final class BytecodeChunk {
  private final String name;
  private final ConstantPool constantPool;
  private final List<Instruction> instructions;
  private final int localCount;
  private final int parameterCount;
  private final List<String> parameterNames;
  private final List<String> localNames; // Names of all local variables for error messages
  // For each local slot, the scope of the comprehension variable stored there, or null. Used to
  // present comprehension variables to the debugger as Eval does.
  private final List<ComprehensionScope> localScopes;
  private final List<Integer> lineNumbers; // Line number for each instruction
  private final List<Integer> columnNumbers; // Column number for each instruction
  private final boolean frozen;

  private BytecodeChunk(
      String name,
      ConstantPool constantPool,
      List<Instruction> instructions,
      int localCount,
      int parameterCount,
      List<String> parameterNames,
      List<String> localNames,
      List<ComprehensionScope> localScopes,
      List<Integer> lineNumbers,
      List<Integer> columnNumbers,
      boolean frozen) {
    this.name = name;
    this.constantPool = constantPool;
    this.instructions = instructions;
    this.localCount = localCount;
    this.parameterCount = parameterCount;
    this.parameterNames = parameterNames;
    this.localNames = localNames != null ? localNames : new ArrayList<>();
    this.localScopes = localScopes;
    this.lineNumbers = lineNumbers;
    this.columnNumbers = columnNumbers != null ? columnNumbers : new ArrayList<>();
    this.frozen = frozen;
  }

  // Code generated for this chunk by a JIT backend (JvmBytecodeCompiler), set once; null until
  // then. Chunks are shared by every execution of a (cached) program, so this is too.
  private volatile Object jitCode;

  // The constant pool as an array, for generated code.
  private volatile Object[] constantsArray;

  // Executions of this chunk so far, for tiered compilation (approximate: updated without locks).
  private int executions;

  /** Counts one execution and returns the new count. */
  public int countExecution() {
    return ++executions;
  }

  public Object getJitCode() {
    return jitCode;
  }

  public void setJitCode(Object code) {
    this.jitCode = code;
  }

  public Object[] getConstantsArray() {
    Object[] a = constantsArray;
    if (a == null) {
      a = constantPool.getConstants().toArray();
      constantsArray = a;
    }
    return a;
  }

  public String getName() {
    return name;
  }

  public ConstantPool getConstantPool() {
    return constantPool;
  }

  public List<Instruction> getInstructions() {
    return Collections.unmodifiableList(instructions);
  }

  public int getInstructionCount() {
    return instructions.size();
  }

  public Instruction getInstruction(int index) {
    return instructions.get(index);
  }

  public int getLocalCount() {
    return localCount;
  }

  public int getParameterCount() {
    return parameterCount;
  }

  public List<String> getParameterNames() {
    return Collections.unmodifiableList(parameterNames);
  }

  public List<String> getLocalNames() {
    return Collections.unmodifiableList(localNames);
  }

  /** Returns, per local slot, the scope of a comprehension variable there, or null. */
  public List<ComprehensionScope> getLocalScopes() {
    return Collections.unmodifiableList(localScopes);
  }

  public List<Integer> getLineNumbers() {
    return Collections.unmodifiableList(lineNumbers);
  }

  public List<Integer> getColumnNumbers() {
    return Collections.unmodifiableList(columnNumbers);
  }

  public boolean isFrozen() {
    return frozen;
  }

  /**
   * Returns the line number for the instruction at the given index.
   */
  public int getLineNumber(int instructionIndex) {
    if (instructionIndex < 0 || instructionIndex >= lineNumbers.size()) {
      return -1;
    }
    return lineNumbers.get(instructionIndex);
  }

  /**
   * Returns the column number for the instruction at the given index.
   */
  public int getColumnNumber(int instructionIndex) {
    if (instructionIndex < 0 || instructionIndex >= columnNumbers.size()) {
      return 0; // Return 0 as default column if not available
    }
    return columnNumbers.get(instructionIndex);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("BytecodeChunk '").append(name).append("' {\n");
    sb.append("  Parameters: ").append(parameterCount).append(" ");
    sb.append(parameterNames).append("\n");
    sb.append("  Locals: ").append(localCount).append("\n");
    sb.append("  Instructions: ").append(instructions.size()).append("\n\n");

    sb.append(constantPool).append("\n");

    sb.append("  Bytecode:\n");
    for (int i = 0; i < instructions.size(); i++) {
      Instruction instr = instructions.get(i);
      int lineNum = i < lineNumbers.size() ? lineNumbers.get(i) : -1;
      sb.append("    ");
      if (lineNum >= 0) {
        sb.append(String.format("L%-4d ", lineNum));
      } else {
        sb.append("      ");
      }
      sb.append(instr).append("\n");
    }

    sb.append("}");
    return sb.toString();
  }

  /**
   * Builder for creating BytecodeChunk instances.
   */
  public static class Builder {
    private final String name;
    private final ConstantPool constantPool;
    private final List<Instruction> instructions;
    private final List<Integer> lineNumbers;
    private final List<Integer> columnNumbers;
    private final List<String> parameterNames;
    private final List<String> localNames;
    private final List<ComprehensionScope> localScopes = new ArrayList<>();
    private int localCount;
    private int parameterCount;
    private int currentOffset;

    public Builder(String name) {
      this.name = name;
      this.constantPool = new ConstantPool();
      this.instructions = new ArrayList<>();
      this.lineNumbers = new ArrayList<>();
      this.columnNumbers = new ArrayList<>();
      this.parameterNames = new ArrayList<>();
      this.localNames = new ArrayList<>();
      this.localCount = 0;
      this.parameterCount = 0;
      this.currentOffset = 0;
    }

    public Builder setLocalCount(int count) {
      this.localCount = count;
      return this;
    }

    public Builder setParameterCount(int count) {
      this.parameterCount = count;
      return this;
    }

    public Builder addParameter(String name) {
      this.parameterNames.add(name);
      return this;
    }

    public Builder addLocalName(String name) {
      this.localNames.add(name);
      return this;
    }

    /** Adds the next local slot, named after its resolver binding. */
    public Builder addLocal(Resolver.Binding binding) {
      this.localNames.add(binding.getName() != null ? binding.getName() : "?");
      this.localScopes.add(null);
      return this;
    }

    /** Records that local slot {@code index} holds a comprehension variable with this scope. */
    public Builder setLocalScope(int index, ComprehensionScope scope) {
      if (index < localScopes.size()) {
        localScopes.set(index, scope);
      }
      return this;
    }

    public ConstantPool getConstantPool() {
      return constantPool;
    }

    public int addConstant(Object value) {
      return constantPool.addConstant(value);
    }

    public Builder emit(Opcode opcode, int lineNumber) {
      Instruction instr = Instruction.create(opcode, currentOffset);
      instructions.add(instr);
      lineNumbers.add(lineNumber);
      columnNumbers.add(0); // Default column to 0
      currentOffset += instr.getSize();
      return this;
    }

    public Builder emit(Opcode opcode, int operand1, int lineNumber) {
      Instruction instr = Instruction.create(opcode, operand1, currentOffset);
      instructions.add(instr);
      lineNumbers.add(lineNumber);
      columnNumbers.add(0); // Default column to 0
      currentOffset += instr.getSize();
      return this;
    }

    public Builder emit(Opcode opcode, int operand1, int operand2, int lineNumber) {
      Instruction instr = Instruction.create(opcode, operand1, operand2, currentOffset);
      instructions.add(instr);
      lineNumbers.add(lineNumber);
      columnNumbers.add(0); // Default column to 0
      currentOffset += instr.getSize();
      return this;
    }

    // New methods with explicit column numbers (for future use)
    public Builder emitWithColumn(Opcode opcode, int lineNumber, int columnNumber) {
      Instruction instr = Instruction.create(opcode, currentOffset);
      instructions.add(instr);
      lineNumbers.add(lineNumber);
      columnNumbers.add(columnNumber);
      currentOffset += instr.getSize();
      return this;
    }

    public Builder emitWithColumn(Opcode opcode, int operand1, int lineNumber, int columnNumber) {
      Instruction instr = Instruction.create(opcode, operand1, currentOffset);
      instructions.add(instr);
      lineNumbers.add(lineNumber);
      columnNumbers.add(columnNumber);
      currentOffset += instr.getSize();
      return this;
    }

    public Builder emitWithColumn(Opcode opcode, int operand1, int operand2, int lineNumber, int columnNumber) {
      Instruction instr = Instruction.create(opcode, operand1, operand2, currentOffset);
      instructions.add(instr);
      lineNumbers.add(lineNumber);
      columnNumbers.add(columnNumber);
      currentOffset += instr.getSize();
      return this;
    }

    public int getCurrentOffset() {
      return currentOffset;
    }

    public int getInstructionCount() {
      return instructions.size();
    }

    // Update an instruction's first operand (for jump patching)
    public void updateInstructionOperand(int index, int newOperand) {
      if (index < 0 || index >= instructions.size()) {
        throw new IllegalArgumentException("Invalid instruction index: " + index);
      }
      Instruction oldInstr = instructions.get(index);
      // Create new instruction with updated operand (preserve old offset)
      Instruction newInstr = Instruction.create(oldInstr.getOpcode(), newOperand, oldInstr.getOffset());
      if (oldInstr.getOpcode().getOperandCount() == 2) {
        newInstr = Instruction.create(oldInstr.getOpcode(), newOperand, oldInstr.getOperand2(), oldInstr.getOffset());
      }
      instructions.set(index, newInstr);
    }

    public BytecodeChunk build() {
      constantPool.freeze();
      return new BytecodeChunk(
          name,
          constantPool,
          new ArrayList<>(instructions),
          localCount,
          parameterCount,
          new ArrayList<>(parameterNames),
          new ArrayList<>(localNames),
          new ArrayList<>(localScopes),
          new ArrayList<>(lineNumbers),
          new ArrayList<>(columnNumbers),
          true);
    }
  }
}
