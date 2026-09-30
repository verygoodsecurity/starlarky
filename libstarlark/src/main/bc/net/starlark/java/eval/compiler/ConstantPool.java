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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.starlark.java.eval.StarlarkFloat;

/**
 * A pool of constants used in bytecode.
 *
 * The constant pool stores strings, numbers, and other constant values that are referenced
 * by bytecode instructions. Each constant is assigned an index which is used in the bytecode.
 *
 * This class is mutable during compilation and becomes immutable after freezing.
 */
public final class ConstantPool {
  private final List<Object> constants;
  private final Map<Object, Integer> constantIndices;
  private boolean frozen;

  /** Rebuilds a frozen pool with exactly these constants, in order (for deserialization). */
  static ConstantPool restore(List<Object> values) {
    ConstantPool pool = new ConstantPool();
    for (Object value : values) {
      pool.constantIndices.putIfAbsent(key(value), pool.constants.size());
      pool.constants.add(value);
    }
    pool.frozen = true;
    return pool;
  }

  public ConstantPool() {
    this.constants = new ArrayList<>();
    this.constantIndices = new HashMap<>();
    this.frozen = false;
  }

  /**
   * Adds a constant to the pool and returns its index.
   * If the constant already exists, returns the existing index.
   *
   * @param value the constant value (must be immutable: String, Integer, Long, Double, etc.)
   * @return the index of the constant
   */
  public int addConstant(Object value) {
    if (frozen) {
      throw new IllegalStateException("Cannot add constants to a frozen pool");
    }

    // Check if we already have this constant
    Integer existing = constantIndices.get(key(value));
    if (existing != null) {
      return existing;
    }

    // Add new constant
    int index = constants.size();
    constants.add(value);
    constantIndices.put(key(value), index);
    return index;
  }

  /**
   * Returns the deduplication key for a constant. Starlark numbers compare equal across types
   * (1 == 1.0, 0.0 == -0.0), so keying by {@code equals} alone would substitute one for the
   * other; the key includes the class, and floats are compared by their exact bits.
   */
  private static Object key(Object value) {
    if (value instanceof StarlarkFloat) {
      return List.of(StarlarkFloat.class, Double.doubleToRawLongBits(((StarlarkFloat) value).toDouble()));
    }
    return List.of(value.getClass(), value);
  }

  /**
   * Gets a constant by its index.
   *
   * @param index the constant index
   * @return the constant value
   */
  public Object getConstant(int index) {
    if (index < 0 || index >= constants.size()) {
      throw new IndexOutOfBoundsException("Invalid constant index: " + index);
    }
    return constants.get(index);
  }

  /**
   * Returns the number of constants in the pool.
   */
  public int size() {
    return constants.size();
  }

  /**
   * Returns true if the pool is empty.
   */
  public boolean isEmpty() {
    return constants.isEmpty();
  }

  /**
   * Freezes the constant pool, preventing further modifications.
   */
  public void freeze() {
    frozen = true;
  }

  /**
   * Returns true if the pool is frozen.
   */
  public boolean isFrozen() {
    return frozen;
  }

  /**
   * Returns an immutable list of all constants.
   */
  public List<Object> getConstants() {
    return new ArrayList<>(constants);
  }

  /**
   * Returns the index of a constant, or -1 if not found.
   */
  public int indexOf(Object value) {
    Integer index = constantIndices.get(key(value));
    return index != null ? index : -1;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("ConstantPool[");
    sb.append(constants.size()).append(" constants");
    if (frozen) {
      sb.append(", frozen");
    }
    sb.append("]:\n");
    for (int i = 0; i < constants.size(); i++) {
      sb.append("  ").append(i).append(": ");
      Object constant = constants.get(i);
      if (constant instanceof String) {
        sb.append('"').append(constant).append('"');
      } else {
        sb.append(constant);
      }
      sb.append(" (").append(constant.getClass().getSimpleName()).append(")\n");
    }
    return sb.toString();
  }
}
