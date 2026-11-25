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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.modules.types.LarkyCounter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import org.junit.jupiter.api.Test;

/** What the module cache shares, what it declines and why (ModuleAdmission, ModuleCache). */
class ModuleAdmissionTest {

  private static final ModuleAdmission.Modules NO_MODULES =
      new ModuleAdmission.Modules() {
        @Override
        public boolean admitted(Module m) {
          return false;
        }

        @Override
        public boolean nativeWrapper(Module m) {
          return false;
        }

        @Override
        public String label(Module m) {
          return "@test//other";
        }
      };

  private static ModuleAdmission.Decline check(Map<String, Object> globals) {
    return check(globals, ImmutableMap.of(), List.of());
  }

  private static ModuleAdmission.Decline check(
      Map<String, Object> globals, Map<String, Object> predeclared, List<Object> shared) {
    Module m = Module.create();
    globals.forEach(m::setGlobal);
    return ModuleAdmission.check(m, predeclared, shared, NO_MODULES);
  }

  private static Dict<String, Object> frozenDict(Map<String, Object> entries) {
    Mutability mu = Mutability.create("test");
    Dict.Builder<String, Object> b = Dict.builder();
    entries.forEach(b::put);
    Dict<String, Object> d = b.build(mu);
    mu.freeze();
    return d;
  }

  @Test
  void frozenStarlarkDataIsAccepted() {
    assertThat(
            check(
                ImmutableMap.of(
                    "TABLE",
                    frozenDict(ImmutableMap.of("a", "x", "b", Starlark.NONE)),
                    "N",
                    net.starlark.java.eval.StarlarkInt.of(3))))
        .isNull();
  }

  @Test
  void aJavaCollectionOutsideAFrozenValueIsUnknown() {
    ModuleAdmission.Decline d = check(ImmutableMap.of("x", new ArrayList<>()));
    assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.UNKNOWN_CLASS);
    assertThat(d.subject()).isEqualTo("java.util.ArrayList");
    assertThat(d.path()).isEqualTo("x");
  }

  @Test
  void anUnfrozenValueIsOwnedMutable() {
    ModuleAdmission.Decline d =
        check(ImmutableMap.of("x", StarlarkList.newList(Mutability.create("open"))));
    assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.OWNED_MUTABLE);
    assertThat(d.path()).isEqualTo("x");
  }

  @Test
  void aHostCounterDoesNotInheritCacheAdmissionFromItsInterface() {
    LarkyCounter counter = new LarkyCounter() {
      private final StringBuilder delegate = new StringBuilder();

      @Override
      public Mutability mutability() {
        return Mutability.IMMUTABLE;
      }

      @Override
      public long addAndGet(long delta) {
        throw new UnsupportedOperationException();
      }

      @Override
      public long get() {
        return delegate.length();
      }
    };

    ModuleAdmission.Decline declined = check(ImmutableMap.of("counter", counter));
    assertThat(declined).isNotNull();
    assertThat(declined.reason()).isEqualTo(ModuleAdmission.Reason.UNKNOWN_CLASS);
    assertThat(declined.path()).isEqualTo("counter.delegate");
  }

  @Test
  void aThreadInsideACollectionIsFoundWithItsPath() {
    StarlarkThread thread =
        StarlarkThread.createTransient(Mutability.create("t"), StarlarkSemantics.DEFAULT);
    ModuleAdmission.Decline d =
        check(ImmutableMap.of("x", ImmutableMap.of("t", ImmutableList.of(thread))));
    assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.THREAD);
    assertThat(d.path()).isEqualTo("x{0}[0]");
  }

  /** A Larky-package object with a field that can be reassigned. */
  static final class Reassignable {
    @SuppressWarnings("unused")
    private String value = "v";
  }

  @Test
  void aNonFinalFieldOfAnUnfrozenObjectIsMutable() {
    ModuleAdmission.Decline d = check(ImmutableMap.of("x", new Reassignable()));
    assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.MUTABLE_FIELD);
    assertThat(d.subject()).isEqualTo(Reassignable.class.getName() + "#value");
    assertThat(d.path()).isEqualTo("x");
  }

  @Test
  void aModuleThatIsNotSharedIsADependency() {
    ModuleAdmission.Decline d = check(ImmutableMap.of("dep", Module.create()));
    assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.DEPENDS_ON);
    assertThat(d.subject()).isEqualTo("@test//other");
    assertThat(d.path()).isEqualTo("dep");
  }

  @Test
  void aHostBindingTheModuleUsesIsChecked() {
    ModuleAdmission.Decline d =
        check(ImmutableMap.of(), ImmutableMap.of("ctx", new HashMap<>()), List.of());
    assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.UNKNOWN_CLASS);
    assertThat(d.path()).isEqualTo("<predeclared ctx>");
  }

  @Test
  void aSharedBuiltinIsNotWalked() {
    ArrayList<Object> builtin = new ArrayList<>();
    assertThat(check(ImmutableMap.of("x", builtin), ImmutableMap.of(), List.of(builtin))).isNull();
  }

  @Test
  void collectionPathsUseEntryPositionsInsteadOfKeyText() {
    String key = "syntheticKeyMarker";
    List<Object> mappings =
        List.of(
            ImmutableMap.of("other", "safe", key, new ArrayList<>()),
            ImmutableListMultimap.of(key, "safe", key, new ArrayList<>()));
    for (Object mapping : mappings) {
      ModuleAdmission.Decline d = check(ImmutableMap.of("x", mapping));
      assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.UNKNOWN_CLASS);
      assertThat(d.path()).isEqualTo("x{1}");
      assertThat(d.toString()).doesNotContain(key);
    }
  }

  @Test
  void tooManyObjectsIsDeclined() {
    List<Object> many = new ArrayList<>();
    for (int i = 0; i <= 50; i++) {
      many.add(new Object());
    }
    Module m = Module.create();
    m.setGlobal("x", ImmutableList.copyOf(many));
    ModuleAdmission.Decline d =
        ModuleAdmission.check(m, ImmutableMap.of(), Set.of(), NO_MODULES, 50);
    assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.TOO_LARGE);
  }

  @Test
  void repeatedReferencesConsumeTheWalkBudget() {
    Module m = Module.create();
    m.setGlobal("x", ImmutableList.copyOf(Collections.nCopies(49, "safe")));
    assertThat(ModuleAdmission.check(m, ImmutableMap.of(), Set.of(), NO_MODULES, 50)).isNull();

    m.setGlobal("x", ImmutableList.copyOf(Collections.nCopies(50, "safe")));
    ModuleAdmission.Decline d =
        ModuleAdmission.check(m, ImmutableMap.of(), Set.of(), NO_MODULES, 50);
    assertThat(d).isNotNull();
    assertThat(d.reason()).isEqualTo(ModuleAdmission.Reason.TOO_LARGE);
    assertThat(d.path()).isEqualTo("x[49]");
  }

}
