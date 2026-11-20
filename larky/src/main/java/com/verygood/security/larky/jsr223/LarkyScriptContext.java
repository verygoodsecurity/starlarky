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

package com.verygood.security.larky.jsr223;

import com.verygood.security.larky.parser.EvaluationMetadata;
import com.verygood.security.larky.parser.EvaluationSettings;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

/**
 * A {@link ScriptContext} that carries the host's {@link EvaluationSettings} for the evaluations
 * run in it, and reports what happened in the last one ({@link #metadata()}).
 *
 * <pre>{@code
 * LarkyScriptContext context = new LarkyScriptContext();
 * context.setSettings(EvaluationSettings.builder()
 *     .stepLimit(1_000_000)
 *     .expiration(Instant.now().plusMillis(500))
 *     .moduleProviders(providers)
 *     .build());
 * context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
 * engine.eval(script, context);
 * long steps = context.metadata().executedSteps();
 * }</pre>
 *
 * <p>The settings are not bindings: a script can neither read nor change them. Each evaluation
 * reads them once, when it starts. When this is the engine's context ({@link
 * LarkyScriptEngine#setContext}), they also apply to {@code eval(script, bindings)}, which runs in
 * a context built from the engine's, and the metadata of such an evaluation is recorded here.
 * {@link LarkyScriptEngine}'s default context is a {@code LarkyScriptContext}.
 *
 * <p>Settings were formerly given as {@code LARKY_EVAL_*} bindings; a binding with such a name, or
 * {@code _EXECUTION_STEPS_}, is now refused.
 */
public class LarkyScriptContext extends SimpleScriptContext {

  private volatile EvaluationSettings settings = EvaluationSettings.NONE;
  @Nullable private volatile EvaluationMetadata metadata;

  public EvaluationSettings getSettings() {
    return settings;
  }

  public void setSettings(EvaluationSettings settings) {
    this.settings = Objects.requireNonNull(settings, "settings");
  }

  /**
   * What happened in the last evaluation run in this context (or, when it is the engine's
   * context, through {@code eval(script, bindings)}), whether it succeeded or failed; null before
   * the first one has ended.
   */
  @Nullable
  public EvaluationMetadata metadata() {
    return metadata;
  }

  void setMetadata(@Nullable EvaluationMetadata metadata) {
    this.metadata = metadata;
  }
}
