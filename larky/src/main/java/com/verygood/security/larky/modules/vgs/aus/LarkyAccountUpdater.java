package com.verygood.security.larky.modules.vgs.aus;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

public interface LarkyAccountUpdater extends StarlarkValue {
  /**
   * Get updated info for the provided card
   *
   * @param number card's number
   * @param expireMonth card's expiration month
   * @param expireYear card's expiration year
   * @param name the name on the card
   * @param vgsMerchantId vgs merchant public identifier
   * @param thread Starlark thread object
   * @return a dict contains the network token values
   */
  Dict<String, Object> lookupUpdates(
      String number,
      Integer expireMonth,
      Integer expireYear,
      String name,
      String vgsMerchantId,
      StarlarkThread thread)
      throws EvalException;
}
