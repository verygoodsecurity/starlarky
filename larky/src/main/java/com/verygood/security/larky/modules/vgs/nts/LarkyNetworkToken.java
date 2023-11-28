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
   * @param cvv cvv of card for retrieving cryptogram
   * @param amount amount of payment for retrieving cryptogram
   * @param currencyCode currency code of payment for retrieving cryptogram
   * @param cryptogramType type of cryptogram
   * @param thread Starlark thread object
   * @return a dict contains the network token values
   */
  Dict<String, Object> getNetworkToken(
      String pan,
      String cvv,
      String amount,
      String currencyCode,
      String cryptogramType,
      String merchantId,
      StarlarkThread thread)
      throws EvalException;
}
