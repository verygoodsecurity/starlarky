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

import com.verygood.security.larky.modules.vgs.ModuleProviders;
import com.verygood.security.larky.parser.LarkyScript;
import java.time.Instant;
import java.util.Objects;
import java.util.OptionalLong;
import javax.annotation.Nullable;

/**
 * The settings a host gives one evaluation: its step limit, its expiration date and its {@code
 * @vgs//} module providers.
 *
 * <p>A host passes them outside the script's bindings: on a {@link
 * com.verygood.security.larky.jsr223.LarkyScriptContext} for JSR-223, or as {@link LarkyScript}'s
 * settings parameter:
 *
 * <pre>{@code
 * LarkyScriptContext context = new LarkyScriptContext();
 * context.setSettings(EvaluationSettings.builder()
 *     .stepLimit(1_000_000)
 *     .expiration(Instant.now().plusMillis(500))
 *     .moduleProviders(providers)
 *     .build());
 * }</pre>
 *
 * <p>They are the host's, not the script's: the script cannot read them, and nothing the script
 * does changes them for this evaluation or a later one. Each evaluation reads them once, when it
 * starts, and {@link StarlarkInterop} reads after it continue from them.
 *
 * <p>They were formerly given as {@code LARKY_EVAL_*} bindings; such bindings are now refused
 * ({@link LarkyEvaluator#refuseRetiredSettings}).
 */
public final class EvaluationSettings {

  /** No step limit, no expiration date, no providers. */
  public static final EvaluationSettings NONE = builder().build();

  private final long stepLimit; // Long.MAX_VALUE: none
  private final long expirationMs; // Long.MAX_VALUE: none
  private final ModuleProviders moduleProviders;

  private EvaluationSettings(Builder b) {
    this.stepLimit = b.stepLimit;
    this.expirationMs = b.expirationMs;
    this.moduleProviders = b.moduleProviders;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** The configured execution step limit, if present. */
  public OptionalLong stepLimit() {
    return stepLimit == Long.MAX_VALUE ? OptionalLong.empty() : OptionalLong.of(stepLimit);
  }

  /** When the evaluation must stop, in milliseconds since the epoch, if it must. */
  public OptionalLong expirationMs() {
    return expirationMs == Long.MAX_VALUE ? OptionalLong.empty() : OptionalLong.of(expirationMs);
  }

  public ModuleProviders moduleProviders() {
    return moduleProviders;
  }

  /** A builder starting from these settings. */
  public Builder toBuilder() {
    Builder b = new Builder();
    b.stepLimit = stepLimit;
    b.expirationMs = expirationMs;
    b.moduleProviders = moduleProviders;
    return b;
  }

  /** Applies the host settings to a file's evaluation thread. */
  public void applyTo(net.starlark.java.eval.StarlarkThread thread) {
    moduleProviders.installOn(thread);
    if (stepLimit != Long.MAX_VALUE) {
      thread.setMaxExecutionSteps(stepLimit);
    }
    if (expirationMs != Long.MAX_VALUE) {
      thread.setExpirationMs(expirationMs);
    }
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof EvaluationSettings that
        && stepLimit == that.stepLimit
        && expirationMs == that.expirationMs
        && moduleProviders.equals(that.moduleProviders);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stepLimit, expirationMs, moduleProviders);
  }

  @Override
  public String toString() {
    return "EvaluationSettings{stepLimit=" + stepLimit() + ", expirationMs=" + expirationMs()
        + ", moduleProviders=" + (moduleProviders.isEmpty() ? "none" : "given")
        + "}";
  }

  /** Builds {@link EvaluationSettings}; every setting is optional. */
  public static final class Builder {
    private long stepLimit = Long.MAX_VALUE;
    private long expirationMs = Long.MAX_VALUE;
    private ModuleProviders moduleProviders = ModuleProviders.NONE;

    private Builder() {}

    /** At most {@code steps} steps in all (see {@link EvaluationSettings#stepLimit()}); must be positive. */
    public Builder stepLimit(long steps) {
      if (steps <= 0) {
        throw new IllegalArgumentException("stepLimit must be positive, not " + steps);
      }
      this.stepLimit = steps;
      return this;
    }

    /** Stop at {@code expirationMs} milliseconds since the epoch. */
    public Builder expirationMs(long expirationMs) {
      this.expirationMs = expirationMs;
      return this;
    }

    /** Stop at {@code expiration}. */
    public Builder expiration(Instant expiration) {
      return expirationMs(expiration.toEpochMilli());
    }

    public Builder moduleProviders(@Nullable ModuleProviders providers) {
      this.moduleProviders = providers == null ? ModuleProviders.NONE : providers;
      return this;
    }

    public EvaluationSettings build() {
      return new EvaluationSettings(this);
    }
  }
}
