package com.verygood.security.larky.modules.vgs.calm;

import java.util.Map;
import net.starlark.java.eval.StarlarkValue;

public interface Calm extends StarlarkValue { 
  String render(String input, Map<String, String> config);
}
