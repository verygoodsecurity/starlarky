package com.verygood.security.larky.nativelib;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;


@StarlarkBuiltin(
    name = "hashlib",
    category = "BUILTIN",
    doc = "This module implements a common interface to many different secure hash and message digest algorithms.")
public class LarkyHashlib implements StarlarkValue {

  @StarlarkMethod(
      name = "md5",
      doc = "hex digest",
      parameters = {
          @Param(name = "toHash", doc = "String to md5 hash")
      },
      useStarlarkThread = true)
  public String md5(String toHash, StarlarkThread thread) {
    //noinspection UnstableApiUsage
    HashCode hashCode = Hashing.md5().hashBytes(toHash.getBytes());
    //noinspection UnstableApiUsage
    return hashCode.toString();
  }

  @StarlarkMethod(
      name = "sha512",
      doc = "hex digest",
      parameters = {
          @Param(name = "toHash", doc = "String to sha512 hash")
      },
      useStarlarkThread = true)
  public String sha512(String toHash, StarlarkThread thread) {
    //noinspection UnstableApiUsage
    HashCode hashCode = Hashing.sha512().hashBytes(toHash.getBytes());
    //noinspection UnstableApiUsage
    return hashCode.toString();
  }
}
