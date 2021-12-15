package com.verygood.security.larky.modules;

import com.verygood.security.larky.modules.vgs.config.http.HTTPConfigModule;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;


@StarlarkBuiltin(
    name = "config",
    category = "BUILTIN",
    doc = "Provides configuration interface for VGS resources"
)
public class ConfigModule implements StarlarkValue {
  public static final ConfigModule INSTANCE = new ConfigModule();

  @StarlarkMethod(name="http", structField = true)
  public HTTPConfigModule httpConfig() {
    return HTTPConfigModule.INSTANCE;
  }
}
