package com.verygood.security.larky.modules.types.hashing;

import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkValue;

/**
 * Hash objects have these methods: - update(data): Update the hash object with the bytes in data.
 * Repeated calls are equivalent to a single call with the concatenation of all the arguments. -
 * digest():     Return the digest of the bytes passed to the update() method so far as a bytes
 * object. - hexdigest():  Like digest() except the digest is returned as a string of double length,
 * containing only hexadecimal digits. - copy():       Return a copy (clone) of the hash object.
 * This can be used to efficiently compute the digests of datas that share a common initial
 * substring.
 */
public class HashObject implements StarlarkValue {

  @StarlarkMethod(
      name = "update",
      doc = "Update the hash object with the bytes in data. Repeated calls\n" +
          "are equivalent to a single call with the concatenation of all\n" +
          "the arguments."
  )
  public void update(Object data) {

  }

  @StarlarkMethod(
      name = "digest",
      doc = "Return the digest of the bytes passed to the update() method\n" +
          "so far as a bytes object."
  )
  public String digest() {
    return null;
  }

  @StarlarkMethod(

      name = "hexdigest",
      doc = "Like digest() except the digest is returned as a string\n" +
          "of double length, containing only hexadecimal digits."
  )
  public void hexdigest() {

  }

  @StarlarkMethod(
      name = "copy",
      doc = "Update the hash object with the bytes in data. Repeated calls\n" +
          "are equivalent to a single call with the concatenation of all\n" +
          "the arguments."
  )
  public HashObject copy() {
    return new HashObject();
  }
}
