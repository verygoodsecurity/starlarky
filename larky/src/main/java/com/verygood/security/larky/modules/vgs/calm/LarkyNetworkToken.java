package com.verygood.security.larky.modules.vgs.calm;

import net.starlark.java.eval.StarlarkValue;

public interface LarkyNetworkToken extends StarlarkValue {
  /**
   * Input JSON payload object, get the network token by given PAN alias and replace the input JSON
   * payload object values by given JSONPaths for each token properties and return.
   *
   * @param input Input JSON payload object
   * @param pan JSONPath to the PAN alias value in the input JSON payload
   * @param expireMonth JSONPath to the expire month to insert in the result JSON payload object
   * @param expireYear JSONPath to the expire year to insert in the result JSON payload object
   * @param cryptogramValue JSONPath to the cryptogram value to insert in the result JSON payload
   *     object
   * @param cryptogramEci JSONPath to the cryptogram ECI to insert in the result JSON payload object
   * @return
   */
  Object render(
      Object input,
      String pan,
      String expireMonth,
      String expireYear,
      String cryptogramValue,
      String cryptogramEci);
}
