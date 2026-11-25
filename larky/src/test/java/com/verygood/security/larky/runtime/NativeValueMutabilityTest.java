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

package com.verygood.security.larky.runtime;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.verygood.security.larky.modules.types.LarkyCounter;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import org.junit.Test;

public class NativeValueMutabilityTest {

  @Test
  public void javaCreatedCountersHaveIndependentOwners() throws EvalException {
    LarkyCounter first = LarkyCounter.newCounter();
    LarkyCounter second = LarkyCounter.newCounter();

    first.mutability().freeze();

    assertThrows(EvalException.class, () -> first.Counter__addAndGet(StarlarkInt.of(1)));
    assertThat(second.Counter__addAndGet(StarlarkInt.of(1))).isEqualTo(StarlarkInt.of(1));
  }

  @Test
  public void counterStaysFrozenInAnotherEvaluation() throws EvalException {
    LarkyCounter counter;
    try (Mutability owner = Mutability.create("initial evaluation");
        CurrentEvaluation ignored = CurrentEvaluation.enter(
            StarlarkThread.createTransient(owner, StarlarkSemantics.DEFAULT))) {
      counter = LarkyCounter.newCounter();
      assertThat(counter.mutability()).isSameInstanceAs(owner);
      assertThat(counter.Counter__addAndGet(StarlarkInt.of(1))).isEqualTo(StarlarkInt.of(1));
    }

    try (Mutability owner = Mutability.create("later evaluation");
        CurrentEvaluation ignored = CurrentEvaluation.enter(
            StarlarkThread.createTransient(owner, StarlarkSemantics.DEFAULT))) {
      assertThrows(EvalException.class, () -> counter.Counter__addAndGet(StarlarkInt.of(1)));
      assertThat(counter.get()).isEqualTo(1);
    }
  }
}
