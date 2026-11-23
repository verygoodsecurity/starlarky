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

import com.verygood.security.larky.parser.Computation;
import com.verygood.security.larky.parser.DefaultLarkyInterpreter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import javax.script.ScriptContext;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkEvalWrapper;

/** Executes compiled scripts for Java hosts. Each invocation creates its own evaluation. */
public final class LarkyScriptEvaluator {

  private LarkyScriptEvaluator() {}

  /**
   * Evaluates the entry module and runs the host computation before its values freeze.
   * Bindings supply evaluation inputs. The computation returns its result directly and records
   * execution metadata on {@code context}. Use a context per invocation.
   */
  public static <I, O> O compute(
      LarkyCompiledScript script, LarkyScriptContext context, I input, Computation<I, O> computation)
      throws LarkyEvaluationScriptException {
    return compute(script, context, List.of(), input, computation);
  }

  /**
   * As {@link #compute(LarkyCompiledScript, LarkyScriptContext, Object, Computation)}, with additional modules for
   * the host to use through {@link Computation.Scope#export}. These modules use the normal
   * script loader and are already frozen when the computation begins.
   */
  public static <I, O> O compute(
      LarkyCompiledScript script,
      LarkyScriptContext context,
      List<String> imports,
      I input,
      Computation<I, O> computation)
      throws LarkyEvaluationScriptException {
    Objects.requireNonNull(computation, "computation");
    try {
      // a computation refused here (its global scope) leaves the context's metadata as it was
      var globals = script.globalBindings(context);
      context.setMetadata(null);
      var interpreter = new DefaultLarkyInterpreter(
          LarkyCompiledScript.LARKY_MODE, context.getSettings(), globals,
          context.getBindings(ScriptContext.ENGINE_SCOPE));
      return interpreter.compute(
          script.interpretedFile(context), context.getWriter(), imports, input, computation,
          context::setMetadata);
    } catch (IOException | InterruptedException | EvalException | StarlarkEvalWrapper.Exc.RuntimeEvalException
             | Starlark.UncheckedEvalException e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      var failure = LarkyEvaluationScriptException.of(e).withMetadataIfAbsent(context.metadata());
      context.setMetadata(failure.metadata());
      throw failure;
    }
  }
}
