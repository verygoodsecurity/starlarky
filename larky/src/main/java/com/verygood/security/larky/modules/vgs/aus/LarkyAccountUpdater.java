package com.verygood.security.larky.modules.vgs.aus;

import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

public interface LarkyAccountUpdater extends StarlarkValue {
  /**
   * Get updated info for the provided card.
   *
   * @param pan card's number
   * @param expireMonth card's expiration month
   * @param expireYear card's expiration year as two digits
   * @param name the name on the card
   * @param clientId client id of service account to access calm API
   * @param clientSecret client secret of service account to access calm API
   * @param thread Starlark thread object
   * @return a dict contains the network token values
   */
  Object lookupCard(
      String pan,
      StarlarkInt expireMonth,
      StarlarkInt expireYear,
      String name,
      String clientId,
      String clientSecret,
      StarlarkThread thread)
      throws EvalException;
}
