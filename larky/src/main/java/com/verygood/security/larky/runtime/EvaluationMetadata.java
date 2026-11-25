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

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.parser.ParsedStarFile;
import java.util.Map;
import java.util.Objects;

/**
 * What happened in one evaluation, for the host: how it ended, how many steps it executed, how
 * long it took and the settings it ran with. A successful evaluation reports it in {@link
 * ParsedStarFile#metadata()}; a failed one in its exception ({@code
 * StarlarkEvalWrapper.Exc.RuntimeEvalException#metadata()}, {@code
 * LarkyEvaluationScriptException#metadata()}).
 *
 * <p>A step is a unit of the engine that ran the script ({@link #engine()}): the same script
 * executes the same number of steps on the same engine and Larky version, every time.
 *
 * @param outcome how the evaluation ended
 * @param executedSteps the steps of the script's own code: the script, the files the host gave it
 *     to load, and everything they call, Larky's library functions included. It does not depend on
 *     which of Larky's modules were already loaded in the process.
 * @param libraryInitSteps the steps of initializing the Larky modules (stdlib, vendor, vgs) this
 *     evaluation loaded that were not already loaded and shared ({@code ModuleCache}): their
 *     top-level code. It varies with what earlier evaluations in the process loaded, as a cold
 *     start does. Whether it counts against the step limit is {@link
 *     EvaluationSettings#chargeLibraryInit()}.
 * @param elapsedNanos the time from the start of the evaluation to its end, loads included
 * @param engine what executed the steps, e.g. {@link #TREE_WALKER}; steps of different engines
 *     are not comparable
 * @param settings the settings the evaluation ran with
 * @param uncachedModules with the module cache on, the Larky modules this evaluation loaded and
 *     ran that the cache declined to share, by load label (e.g. {@code @vgs//vault}), each with why:
 *     a reason ({@code RETAINS_HOST_SUPPLIED_PROVIDER}, {@code OWNED_MUTABLE}, {@code UNKNOWN_CLASS},
 *     {@code MUTABLE_FIELD}, {@code THREAD}, {@code DEPENDS_ON}, {@code TOO_LARGE},
 *     {@code NOT_ANALYZED}), what blocked it (a class name,
 *     or the module it depends on) and where, by global and field names and collection positions, e.g. {@code
 *     "RETAINS_HOST_SUPPLIED_PROVIDER com.verygood.security.larky.modules.VaultModule at vault"}. Never a value.
 *     Empty with the cache off.
 */
public record EvaluationMetadata(
    Outcome outcome,
    long executedSteps,
    long libraryInitSteps,
    long elapsedNanos,
    String engine,
    EvaluationSettings settings,
    Map<String, String> uncachedModules) {

  /**
   * Starlark's tree-walking evaluator ({@code net.starlark.java.eval.Eval}): one step per
   * statement executed and per expression evaluated.
   */
  public static final String TREE_WALKER = "tree-walker";

  /** How an evaluation ended. */
  public enum Outcome {
    /** It ran to the end. */
    OK,
    /** The script failed: an error it raised, or a failing operation. */
    SCRIPT_ERROR,
    /** The evaluation executed as many steps as {@link EvaluationSettings#stepLimit()} allows. */
    STEP_LIMIT,
    /** It ran past {@link EvaluationSettings#expirationMs()}. */
    DEADLINE,
    /** The thread running it was interrupted. */
    INTERRUPTED,
    /** Larky failed: an unexpected Java exception, not the script's error. */
    INTERNAL
  }

  public EvaluationMetadata {
    Objects.requireNonNull(outcome, "outcome");
    Objects.requireNonNull(engine, "engine");
    Objects.requireNonNull(settings, "settings");
    uncachedModules = ImmutableMap.copyOf(uncachedModules);
  }
}
