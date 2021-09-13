package com.verygood.security.larky.modules;

import com.verygood.security.larky.modules.vgs.cerebro.text.analyzer.TextAnalyzerModule;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

@StarlarkBuiltin(
    name = "cerebro",
    category = "BUILTIN",
    doc = "Module that exposes API for intelligent data analysis of PII data on VGS platform"
)
public class CerebroModule implements StarlarkValue {

  public static final CerebroModule INSTANCE = new CerebroModule();

  @StarlarkMethod(name="TextAnalyzer", structField = true)
  public TextAnalyzerModule TextAnalyzer() { return TextAnalyzerModule.INSTANCE; }

}
