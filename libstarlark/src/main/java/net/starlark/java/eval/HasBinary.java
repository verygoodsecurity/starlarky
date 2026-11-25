// Copyright 2020 The Bazel Authors. All rights reserved.
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

import javax.annotation.Nullable;
import net.starlark.java.syntax.TokenKind;

/**
 * A Starlark value that supports binary operators such as {@code x+y}.
 *
 * <p>When no built-in case handles the operation, the operands are tried left to right. For each
 * operand, the evaluator calls {@link Threaded#binaryOp} if available, otherwise {@link #binaryOp}
 * if implemented. A {@code null} result declines the operation. Evaluation fails if neither
 * operand handles it.
 *
 * <p>Subclasses should strive for appropriate symmetries in their implementations, such as {@code x
 * * y == y * x}.
 */
// TODO(adonovan): rename BinaryOperand?
public interface HasBinary extends StarlarkValue {

  /**
   * Returns {@code this op that}, if thisLeft, or {@code that op this} otherwise. May return null
   * to indicate that the operation is not supported, or may throw a specific exception.
   */
  @Nullable
  Object binaryOp(TokenKind op, Object that, boolean thisLeft) throws EvalException;

  /** A binary operator implementation that receives the executing Starlark thread. */
  interface Threaded extends StarlarkValue {
    /**
     * Handles the operator as in {@link HasBinary#binaryOp}, using {@code thread} for the current
     * evaluation.
     */
    @Nullable
    Object binaryOp(StarlarkThread thread, TokenKind op, Object that, boolean thisLeft)
        throws EvalException;
  }
}
