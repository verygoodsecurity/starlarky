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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;

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
 * <p>Standard streams are wrapped on first access. A Java host that already has a log writer
 * can provide it together with the settings via {@link #LarkyScriptContext(EvaluationSettings, Writer)}.
 *
 * <p>Settings were formerly given as {@code LARKY_EVAL_*} bindings; a binding with such a name, or
 * {@code _EXECUTION_STEPS_}, is now refused.
 */
public class LarkyScriptContext implements ScriptContext {

  private static final List<Integer> SCOPES = List.of(ENGINE_SCOPE, GLOBAL_SCOPE);

  private Bindings engineBindings = new SimpleBindings();
  @Nullable private Bindings globalBindings;

  // Remember the standard streams at construction, but allocate their wrappers only on use.
  @Nullable private InputStream defaultInput = System.in;
  @Nullable private PrintStream defaultOutput = System.out;
  @Nullable private PrintStream defaultError = System.err;
  @Nullable private Reader reader;
  @Nullable private Writer writer;
  @Nullable private Writer errorWriter;

  private volatile EvaluationSettings settings = EvaluationSettings.NONE;
  @Nullable private volatile EvaluationMetadata metadata;

  public LarkyScriptContext() {}

  /** Creates a context for a host that supplies the evaluation settings and log writer. */
  public LarkyScriptContext(EvaluationSettings settings, Writer writer) {
    this.settings = Objects.requireNonNull(settings, "settings");
    this.writer = Objects.requireNonNull(writer, "writer");
    defaultOutput = null;
  }

  @Override
  public List<Integer> getScopes() {
    return SCOPES;
  }

  @Override
  public @Nullable Bindings getBindings(int scope) {
    return switch (scope) {
      case ENGINE_SCOPE -> engineBindings;
      case GLOBAL_SCOPE -> globalBindings;
      default -> throw new IllegalArgumentException("invalid scope " + scope);
    };
  }

  @Override
  public void setBindings(@Nullable Bindings bindings, int scope) {
    switch (scope) {
      case ENGINE_SCOPE -> engineBindings = Objects.requireNonNull(bindings, "engine bindings");
      case GLOBAL_SCOPE -> globalBindings = bindings;
      default -> throw new IllegalArgumentException("invalid scope " + scope);
    }
  }

  @Override
  public @Nullable Object getAttribute(String name) {
    int scope = getAttributesScope(name);
    return scope == -1 ? null : getAttribute(name, scope);
  }

  @Override
  public @Nullable Object getAttribute(String name, int scope) {
    checkName(name);
    Bindings bindings = getBindings(scope);
    return bindings == null ? null : bindings.get(name);
  }

  @Override
  public int getAttributesScope(String name) {
    checkName(name);
    if (engineBindings.containsKey(name)) {
      return ENGINE_SCOPE;
    }
    return globalBindings != null && globalBindings.containsKey(name) ? GLOBAL_SCOPE : -1;
  }

  @Override
  public void setAttribute(String name, @Nullable Object value, int scope) {
    checkName(name);
    Bindings bindings = getBindings(scope);
    if (bindings != null) {
      bindings.put(name, value);
    }
  }

  @Override
  public @Nullable Object removeAttribute(String name, int scope) {
    checkName(name);
    Bindings bindings = getBindings(scope);
    return bindings == null ? null : bindings.remove(name);
  }

  private static void checkName(String name) {
    if (Objects.requireNonNull(name, "name").isEmpty()) {
      throw new IllegalArgumentException("attribute name cannot be empty");
    }
  }

  @Override
  public @Nullable Reader getReader() {
    if (defaultInput != null) {
      reader = new InputStreamReader(defaultInput);
      defaultInput = null;
    }
    return reader;
  }

  @Override
  public void setReader(@Nullable Reader reader) {
    this.reader = reader;
    defaultInput = null;
  }

  @Override
  public @Nullable Writer getWriter() {
    if (defaultOutput != null) {
      writer = new PrintWriter(defaultOutput, true);
      defaultOutput = null;
    }
    return writer;
  }

  @Override
  public void setWriter(@Nullable Writer writer) {
    this.writer = writer;
    defaultOutput = null;
  }

  @Override
  public @Nullable Writer getErrorWriter() {
    if (defaultError != null) {
      errorWriter = new PrintWriter(defaultError, true);
      defaultError = null;
    }
    return errorWriter;
  }

  @Override
  public void setErrorWriter(@Nullable Writer writer) {
    errorWriter = writer;
    defaultError = null;
  }

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
