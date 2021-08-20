package com.verygood.security.larky.modules.vgs.cerebro.text.analyzer.analyzers;

import com.verygood.security.larky.modules.vgs.cerebro.text.analyzer.dto.TextPIIEntity;
import com.verygood.security.larky.modules.vgs.cerebro.text.analyzer.spi.TextPIIAnalyzer;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkFloat;

import java.util.List;

public class NotImplementedTextPIIAnalyzer implements TextPIIAnalyzer {
  @Override
  public List<TextPIIEntity> analyze(String text, String language, List<String> entities, StarlarkFloat scoreThreshold)
      throws EvalException {
    throw Starlark.errorf("cerebro.pii_analyzer.analyze method must be overridden");
  }

  @Override
  public List<String> supportedEntities(String language) throws EvalException {
    throw Starlark.errorf("cerebro.pii_analyzer.supported_entities method must be overridden");
  }

  @Override
  public List<String> supportedLanguages() throws EvalException {
    throw Starlark.errorf("cerebro.pii_analyzer.supported_languages method must be overridden");
  }
}
