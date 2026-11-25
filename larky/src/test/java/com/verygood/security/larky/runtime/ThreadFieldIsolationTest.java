/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.verygood.security.larky.runtime;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.verygood.security.larky.parser.InMemMapBackedStarFile;
import com.verygood.security.larky.parser.LarkyScript;
import com.verygood.security.larky.parser.ParsedStarFile;
import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.Console;
import com.verygood.security.larky.console.testing.TestingConsole;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkThread;
import org.junit.jupiter.api.Test;

/**
 * Larky classes, their instances, {@code super()} objects, namedtuples and iterators keep no
 * reference to the thread that created them: used by another evaluation (here through a shared
 * module cache) they run on that evaluation's thread, printing to its console and showing its
 * frames in a traceback.
 */
public class ThreadFieldIsolationTest {

  private static final String LOAD =
      "load('@vendor//_thread_fixtures/types_at_load', 'A', 'A1', 'A2', 'B1', 'B2', 'C', 'SUPER',"
          + " 'Point', 'P', 'IT')\n";

  private static ParsedStarFile run(String script, ModuleCache cache, Console console)
      throws Exception {
    return new CachedTestScript(ModuleSupplier.CORE_MODULES, LarkyScript.StarlarkMode.STRICT)
        .withModuleCache(cache)
        .evaluate(InMemMapBackedStarFile.createStarFile("main.star", script), console);
  }

  private static List<String> printed(TestingConsole console) {
    List<String> out = new ArrayList<>();
    console.getMessages().forEach(m -> out.add(m.getText()));
    return out;
  }

  @Test
  public void aClassFromACachedModulePrintsToTheEvaluationUsingIt() throws Exception {
    ModuleCache cache = new ModuleCache(true);
    String script = LOAD + "def check():\n    return A1 == A2\nresult = check()\n";

    TestingConsole first = new TestingConsole();
    run(script, cache, first);
    TestingConsole second = new TestingConsole();
    ParsedStarFile parsed = run(script, cache, second);

    assertThat(cache.size()).isGreaterThan(0); // the module was shared
    assertThat(parsed.getGlobalEnvironmentVariable("result", Object.class)).isEqualTo(true);
    assertThat(printed(first)).hasSize(1);
    assertThat(printed(first).get(0)).endsWith("eq ran");
    assertThat(printed(second)).hasSize(1);
    assertThat(printed(second).get(0)).endsWith("eq ran");
  }

  @Test
  public void aFailingEqFromACachedModuleShowsTheCallersFrames() throws Exception {
    ModuleCache cache = new ModuleCache(true);
    String script = LOAD + "def check():\n    return B1 == B2\nresult = check()\n";
    assertThrows(Exception.class, () -> run(script, cache, new TestingConsole()));

    Exception second = assertThrows(Exception.class, () -> run(script, cache, new TestingConsole()));
    String traceback = tracebackOf(second);
    assertThat(traceback).contains("eq boom");
    assertThat(traceback).contains("in check");
  }

  @Test
  public void namedtuplesSuperAndIteratorsWorkInALaterEvaluation() throws Exception {
    ModuleCache cache = new ModuleCache(true);
    String script =
        LOAD
            + "def check():\n"
            + "    return [P.x + P.y, Point(3, 4).y, C() == A1, SUPER != None]\n"
            + "result = str(check())\n";
    run(script, cache, new TestingConsole());
    ParsedStarFile parsed = run(script, cache, new TestingConsole());
    assertThat(parsed.getGlobalEnvironmentVariable("result", Object.class))
        .isEqualTo("[3, 4, True, True]");
  }

  @Test
  public void outsideAnEvaluationTheyFailClosed() throws Exception {
    ParsedStarFile parsed = run(LOAD, new ModuleCache(false), new TestingConsole());
    Object a1 = parsed.getGlobalEnvironmentVariable("A1", Object.class);
    Object a2 = parsed.getGlobalEnvironmentVariable("A2", Object.class);
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> a1.equals(a2));
    assertThat(e).hasMessageThat().contains("no Starlark evaluation is active");
  }

  @Test
  public void noValueBuiltAtLoadReachesAStarlarkThread() throws Exception {
    ParsedStarFile parsed = run(LOAD, new ModuleCache(false), new TestingConsole());
    List<Object> roots = new ArrayList<>();
    for (String name : List.of("A", "A1", "B1", "C", "SUPER", "Point", "P", "IT")) {
      roots.add(parsed.getGlobalEnvironmentVariable(name, Object.class));
    }
    List<String> paths = threadPaths(roots);
    assertThat(paths).isEmpty();
  }

  /** The Starlark traceback in {@code t}'s cause chain, else the chain's messages. */
  private static String tracebackOf(Throwable t) {
    StringBuilder chain = new StringBuilder();
    for (Throwable c = t; c != null; c = c.getCause()) {
      if (c instanceof EvalException e) {
        return e.getMessageWithStack();
      }
      if (c instanceof net.starlark.java.eval.Starlark.UncheckedEvalException u) {
        chain.append(u.getMessage()).append('\n');
      }
      chain.append(c).append('\n');
    }
    return chain.toString();
  }

  /** Paths from {@code roots} to a {@link StarlarkThread}, following fields, arrays and collections. */
  private static List<String> threadPaths(List<Object> roots) throws IllegalAccessException {
    List<String> found = new ArrayList<>();
    Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    Deque<Object[]> work = new ArrayDeque<>();
    for (Object r : roots) {
      work.add(new Object[] {r, r == null ? "null" : r.getClass().getSimpleName()});
    }
    while (!work.isEmpty() && seen.size() < 200_000) {
      Object[] item = work.pop();
      Object x = item[0];
      String path = (String) item[1];
      if (x == null || !seen.add(x)) {
        continue;
      }
      if (x instanceof StarlarkThread) {
        found.add(path);
        continue;
      }
      Class<?> cls = x.getClass();
      if (cls.isArray()) {
        if (!cls.getComponentType().isPrimitive()) {
          for (int i = 0; i < Array.getLength(x); i++) {
            work.push(new Object[] {Array.get(x, i), path + "[" + i + "]"});
          }
        }
        continue;
      }
      String name = cls.getName();
      if (name.startsWith("java.") || name.startsWith("jdk.") || name.startsWith("sun.")) {
        if (x instanceof Collection<?> c) {
          for (Object e : c) {
            work.push(new Object[] {e, path + "[]"});
          }
        } else if (x instanceof Map<?, ?> m) {
          for (Map.Entry<?, ?> e : m.entrySet()) {
            work.push(new Object[] {e.getKey(), path + "{key}"});
            work.push(new Object[] {e.getValue(), path + "{value}"});
          }
        }
        continue;
      }
      for (Class<?> k = cls; k != null && k != Object.class; k = k.getSuperclass()) {
        if (k.getName().startsWith("java.")) {
          break;
        }
        for (Field f : k.getDeclaredFields()) {
          if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) {
            continue;
          }
          f.setAccessible(true);
          work.push(new Object[] {f.get(x), path + "." + f.getName()});
        }
      }
    }
    return found;
  }
}
