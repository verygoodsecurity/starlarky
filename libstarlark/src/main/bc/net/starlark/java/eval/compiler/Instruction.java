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

import java.util.Objects;

/**
 * Represents a single bytecode instruction with its operands.
 *
 * Instructions are immutable and can have 0, 1, or 2 operands depending on the opcode.
 */
public final class Instruction {
  private final Opcode opcode;
  private final int operand1;
  private final int operand2;
  private final int offset; // Byte offset in the bytecode stream

  private Instruction(Opcode opcode, int operand1, int operand2, int offset) {
    this.opcode = opcode;
    this.operand1 = operand1;
    this.operand2 = operand2;
    this.offset = offset;
  }

  /**
   * Creates an instruction with no operands.
   */
  public static Instruction create(Opcode opcode, int offset) {
    if (opcode.getOperandCount() != 0) {
      throw new IllegalArgumentException(
          "Opcode " + opcode + " requires " + opcode.getOperandCount() + " operands");
    }
    return new Instruction(opcode, 0, 0, offset);
  }

  /**
   * Creates an instruction with one operand.
   */
  public static Instruction create(Opcode opcode, int operand1, int offset) {
    if (opcode.getOperandCount() != 1) {
      throw new IllegalArgumentException(
          "Opcode " + opcode + " requires " + opcode.getOperandCount() + " operands");
    }
    return new Instruction(opcode, operand1, 0, offset);
  }

  /**
   * Creates an instruction with two operands.
   */
  public static Instruction create(Opcode opcode, int operand1, int operand2, int offset) {
    if (opcode.getOperandCount() != 2) {
      throw new IllegalArgumentException(
          "Opcode " + opcode + " requires " + opcode.getOperandCount() + " operands");
    }
    return new Instruction(opcode, operand1, operand2, offset);
  }

  public Opcode getOpcode() {
    return opcode;
  }

  public int getOperand1() {
    return operand1;
  }

  public int getOperand2() {
    return operand2;
  }

  public int getOffset() {
    return offset;
  }

  /**
   * Returns the size of this instruction in bytes.
   * Format: 1 byte opcode + 4 bytes per operand
   */
  public int getSize() {
    return 1 + (opcode.getOperandCount() * 4);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("%04d: %s", offset, opcode));
    if (opcode.getOperandCount() >= 1) {
      sb.append(" ").append(operand1);
    }
    if (opcode.getOperandCount() >= 2) {
      sb.append(" ").append(operand2);
    }
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Instruction)) return false;
    Instruction that = (Instruction) o;
    return operand1 == that.operand1
        && operand2 == that.operand2
        && opcode == that.opcode;
  }

  @Override
  public int hashCode() {
    return Objects.hash(opcode, operand1, operand2);
  }
}
