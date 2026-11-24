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

import javax.annotation.Nullable;
import net.starlark.java.eval.StarlarkThread;

/**
 * The {@link StarlarkThread} that is running Starlark on the current Java thread.
 *
 * <p>Some Java entry points that Starlark values implement have no thread parameter: {@code
 * equals}, {@code hashCode}, {@code compareTo}, {@code toString}, {@code truth}, {@code
 * iterator}, and the {@code Structure#getValue(String)} lookup. A value that must call back into
 * Starlark from one of them (a struct with {@code __eq__}, {@code __repr__}, a {@code
 * larky.property}, ...) takes the thread from here instead of storing the thread that created it.
 * A stored thread outlives its evaluation and would keep running callbacks on a finished
 * evaluation (its closed {@code Mutability}, its step count, its print handler), and a value that
 * is shared across evaluations would run them on another evaluation's thread.
 *
 * <p>Code that creates a thread to run Starlark enters it for the duration of the run:
 *
 * <pre>{@code
 * try (CurrentEvaluation ignored = CurrentEvaluation.enter(thread)) {
 *   Starlark.execFileProgram(program, module, thread);
 * }
 * }</pre>
 *
 * Entering nests: closing restores the thread that was current before. The class is per Java
 * thread (a {@link ThreadLocal}), so concurrent evaluations on different Java threads do not see
 * each other. Starlark code that runs on a thread of another Java thread (none does in Larky)
 * must enter it there.
 *
 * <p>Outside any evaluation {@link #requireThread()} fails with an {@link IllegalStateException}
 * rather than falling back to some other thread. Embedding code that reads script values after
 * the evaluation has finished should use {@link HostValues}, which runs each read on a fresh
 * thread.
 */
public final class CurrentEvaluation implements AutoCloseable {

  private static final ThreadLocal<StarlarkThread> CURRENT = new ThreadLocal<>();

  @Nullable private final StarlarkThread previous;
  private final Thread owner = Thread.currentThread();
  private boolean closed;

  private CurrentEvaluation(@Nullable StarlarkThread previous) {
    this.previous = previous;
  }

  /**
   * Makes {@code thread} the current evaluation of this Java thread until the returned handle is
   * closed. Close it on the same Java thread, in a try-with-resources.
   */
  public static CurrentEvaluation enter(StarlarkThread thread) {
    if (thread == null) {
      throw new NullPointerException("thread");
    }
    CurrentEvaluation handle = new CurrentEvaluation(CURRENT.get());
    CURRENT.set(thread);
    return handle;
  }

  /** The thread running Starlark on this Java thread, or null if none is. */
  @Nullable
  public static StarlarkThread currentOrNull() {
    return CURRENT.get();
  }

  /**
   * The thread running Starlark on this Java thread.
   *
   * @throws IllegalStateException if no evaluation is active on this Java thread
   */
  public static StarlarkThread requireThread() {
    StarlarkThread thread = CURRENT.get();
    if (thread == null) {
      throw new IllegalStateException(
          "no Starlark evaluation is active on this thread: a script value was used outside the"
              + " evaluation that created it. Read script values through HostValues"
              + " (ParsedStarFile#hostValues), which runs each access on a fresh thread.");
    }
    return thread;
  }

  /** The current thread, or {@code thread} when it is non-null. */
  public static StarlarkThread orRequire(@Nullable StarlarkThread thread) {
    return thread != null ? thread : requireThread();
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    if (Thread.currentThread() != owner) {
      throw new IllegalStateException("CurrentEvaluation must be closed on the thread that entered it");
    }
    closed = true;
    if (previous == null) {
      CURRENT.remove();
    } else {
      CURRENT.set(previous);
    }
  }
}
