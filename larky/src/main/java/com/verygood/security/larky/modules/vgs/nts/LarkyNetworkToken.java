package com.verygood.security.larky.modules.vgs.nts;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

public interface LarkyNetworkToken extends StarlarkValue {
  /**
   * Get network token by pan alias.
   *
   * @param pan the pan alias value for getting network token
   * @param thread Starlark thread object
   * @return a dict contains the network token values
   */
  Dict<String, Object> getNetworkToken(String pan, StarlarkThread thread)
      throws EvalException;
}
