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

package com.verygood.security.larky.modules.types;

import com.verygood.security.larky.runtime.CurrentEvaluation;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;

/**
 * The {@link Mutability} of a native value whose Java state a Starlark call changes in place
 * (a hash, cipher, counter, regex matcher, EC point, zlib stream...).
 *
 * <p>Such a value belongs to the evaluation that created it, as a list or dict does: it records
 * the {@link Mutability} of the {@link StarlarkThread} running when it was created, and each
 * method that changes its state first calls {@link #checkMutable}. Once that {@code Mutability}
 * is frozen (the file that created the value has finished executing), the method fails with
 * "trying to mutate a frozen ... value", as appending to a frozen list does. So a value kept in a
 * module's globals cannot be changed by later calls, from this evaluation or, when the loaded
 * module is shared, from another one.
 *
 * <p>A {@code Mutability} holds only its annotation (a string here) and, until frozen, its
 * iterator counts; it does not refer to its thread, so holding it does not keep the creating
 * thread or its frames reachable.
 */
public final class NativeMutability {

  private NativeMutability() {}

  /**
   * The {@code Mutability} of the evaluation running on this Java thread, which is the owner of a
   * native value created now.
   *
   * <p>Outside an evaluation (a value built directly by Java code) the value gets a {@code
   * Mutability} of its own that is never frozen, so it stays mutable as before.
   */
  public static Mutability current() {
    StarlarkThread thread = CurrentEvaluation.currentOrNull();
    return thread != null ? thread.mutability() : Mutability.create("native value");
  }

  /**
   * A native value whose Java state is guarded by its owner's {@code Mutability}: every method
   * that changes that state calls {@link #checkMutable} first. Once its {@code Mutability} is
   * frozen nothing can change it, so the module cache admits it without looking at its Java
   * fields (which are library objects such as digests and ciphers, mutable only through it).
   */
  public interface Guarded extends Mutability.Freezable {}

  /**
   * Fails if {@code value} may not be changed: its {@code Mutability} is frozen, or it is being
   * iterated.
   */
  public static void checkMutable(Mutability.Freezable value) throws EvalException {
    Starlark.checkMutable(value);
  }
}
