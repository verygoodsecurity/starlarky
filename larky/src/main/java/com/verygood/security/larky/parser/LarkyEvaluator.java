package com.verygood.security.larky.parser;

import com.google.common.collect.ImmutableMap;
import com.verygood.security.larky.ModuleSupplier;
import com.verygood.security.larky.console.Console;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Module;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.syntax.SyntaxError;

/** @deprecated Use {@link com.verygood.security.larky.runtime.LarkyEvaluator}. */
@Deprecated
public final class LarkyEvaluator {
  public static final String EXECUTION_STEPS =
      com.verygood.security.larky.runtime.LarkyEvaluator.EXECUTION_STEPS;
  public static final String STEP_LIMIT =
      com.verygood.security.larky.runtime.LarkyEvaluator.STEP_LIMIT;
  public static final String EXPIRATION_MS =
      com.verygood.security.larky.runtime.LarkyEvaluator.EXPIRATION_MS;
  public static final String MODULE_PROVIDERS =
      com.verygood.security.larky.runtime.LarkyEvaluator.MODULE_PROVIDERS;

  private final com.verygood.security.larky.runtime.LarkyEvaluator delegate;

  public LarkyEvaluator(LarkyScript script, Console console) {
    this.delegate = new com.verygood.security.larky.runtime.LarkyEvaluator(script, console);
  }

  public ImmutableMap<String, Object> getEnvironment() {
    return delegate.getEnvironment();
  }

  public ModuleSupplier.ModuleSet getModuleSet() {
    return delegate.getModuleSet();
  }

  public StarlarkSemantics getLarkySemantics() {
    return delegate.getLarkySemantics();
  }

  public interface EvaluationResult {
    boolean hasOutput();

    boolean hasModule();

    Object output();

    Module module();
  }

  @Builder
  protected record DefaultEvaluationResult(Object output, Module module)
      implements EvaluationResult {
    @Override
    public boolean hasOutput() {
      return output != null;
    }

    @Override
    public boolean hasModule() {
      return module != null;
    }
  }

  public EvaluationResult eval(StarFile content)
      throws IOException, InterruptedException, EvalException {
    var result = delegate.eval(content);
    return new DefaultEvaluationResult(result.output(), result.module());
  }

  public Map<String, Module> processLoads(StarFile content, List<String> loads) throws EvalException {
    return delegate.processLoads(content, loads);
  }

  public static EvalException compileError(String file, SyntaxError.Exception ex) {
    return com.verygood.security.larky.runtime.LarkyEvaluator.compileError(file, ex);
  }
}
