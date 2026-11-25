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

package com.verygood.security.larky.parser;

import static com.google.common.truth.Truth.assertThat;

import com.verygood.security.larky.runtime.CurrentEvaluation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.LarkySemantics;
import com.verygood.security.larky.jsr223.LarkyScriptEngine;
import java.util.ArrayList;
import java.util.List;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkThread;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Reading or writing a field runs its property's getter or setter (or {@code __getattr__}) on the
 * thread of the code that does it: the interpreter passes its thread ({@code Structure.Threaded})
 * rather than the object looking one up in {@link CurrentEvaluation}.
 *
 * <p>Each case calls a function on one thread while another is the current evaluation, and checks
 * which thread printed.
 */
public class ThreadedFieldAccessTest {

  private static ParsedStarFile file;

  @BeforeClass
  public static void evaluate() throws Exception {
    file = (ParsedStarFile) new LarkyScriptEngine().eval("""
        load("@stdlib//larky", larky="larky")
        load("@stdlib//types", types="types")

        def _get(*args):
            print("get")
            return 1

        def _set(*args):
            print("set")

        FROZEN = larky.mutablestruct(p=larky.property(_get, _set))
        HOOKED = larky.struct(__getattr__=lambda name: _get())
        K = types.new_class("K", (), {}, lambda ns: ns.update({"p": larky.property(_get, _set)}))
        INSTANCE = K()

        def struct_read():
            return FROZEN.p

        def struct_getattr_hook():
            return HOOKED.other

        def struct_write():
            s = larky.mutablestruct(p=larky.property(_get, _set))
            s.p = 2

        def struct_augmented_write():
            s = larky.mutablestruct(p=larky.property(_get, _set))
            s.p += 2

        def struct_setattr():
            s = larky.mutablestruct(p=larky.property(_get, _set))
            setattr(s, "p", 2)

        def instance_read():
            return INSTANCE.p

        def instance_write():
            k = K()
            k.p = 2
        """);
  }

  /** What each thread printed when {@code function} ran on "caller" while "current" was the current evaluation. */
  private static List<String> printed(String function) throws Exception {
    List<String> printed = new ArrayList<>();
    try (Mutability mu = Mutability.create("test")) {
      StarlarkThread current = StarlarkThread.createTransient(mu, LarkySemantics.LARKY_SEMANTICS);
      current.setPrintHandler((thread, message) -> printed.add("current: " + message));
      StarlarkThread caller = StarlarkThread.createTransient(mu, LarkySemantics.LARKY_SEMANTICS);
      caller.setPrintHandler((thread, message) -> printed.add("caller: " + message));
      try (CurrentEvaluation ignored = CurrentEvaluation.enter(current)) {
        Starlark.call(caller, file.getModule().getGlobal(function), ImmutableList.of(), ImmutableMap.of());
      }
    }
    return printed;
  }

  @Test
  public void aStructPropertyIsReadOnTheCallersThread() throws Exception {
    assertThat(printed("struct_read")).containsExactly("caller: get");
    assertThat(printed("struct_getattr_hook")).containsExactly("caller: get");
  }

  @Test
  public void aStructPropertyIsSetOnTheCallersThread() throws Exception {
    assertThat(printed("struct_write")).containsExactly("caller: set");
    assertThat(printed("struct_augmented_write")).containsExactly("caller: get", "caller: set").inOrder();
    assertThat(printed("struct_setattr")).containsExactly("caller: set");
  }

  @Test
  public void anInstancePropertyIsReadAndSetOnTheCallersThread() throws Exception {
    assertThat(printed("instance_read")).containsExactly("caller: get");
    assertThat(printed("instance_write")).containsExactly("caller: set");
  }
}
