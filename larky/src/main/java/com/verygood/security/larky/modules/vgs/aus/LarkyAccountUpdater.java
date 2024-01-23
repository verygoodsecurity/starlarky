package com.verygood.security.larky.modules.vgs.aus;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkInt;
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
   * @param clientId client id of service account to access calm API
   * @param clientSecret client secret of service account to access calm API
   * @param thread Starlark thread object
   * @return a dict contains the network token values
   */
  Dict<String, Object> lookupCard(
      String number,
      StarlarkInt expireMonth,
      StarlarkInt expireYear,
      String name,
      String clientId,
      String clientSecret,
      StarlarkThread thread)
      throws EvalException;
}
