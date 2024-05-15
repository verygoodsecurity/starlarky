package net.starlark.java.eval;

public class StarlarkDictWrapper {

  /**
   * Exposes {@code StarlarkDict#update} a package-public common implementation of dict(pairs, **kwargs) and
   * dict.update(pairs, **kwargs) publicly so that {@link com.verygood.security.larky.modules.types.LarkyMapping#update}
   * does not need to copy and paste
   *
   * @param funcname The name of the function invoking this update method, used for error messaging.
   * @param dict     The dictionary to be updated.
   * @param pairs    An object representing a collection of pairs. This can be a Dict or any iterable object where each
   *                 element is an iterable of two elements (key and value) similar to a tuple.
   * @param kwargs   A dictionary of additional key-value pairs to add to the dict.
   * @throws EvalException if pairs is not iterable, or if any element within pairs is not a two-element iterable, or if
   *                       any other error occurs during the update process. {@code @example} update("update", dict,
   *                       pairs, kwargs); // Direct call to update a dictionary {@code @example} Dict.update("dict",
   *                       dict, pairs, kwargs); // Call via Dict class
   */
  public static void update(
    String funcname, Dict<Object, Object> dict, Object pairs, Dict<String, Object> kwargs)
    throws EvalException {
    Dict.update(funcname, dict, pairs, kwargs);
  }
}
