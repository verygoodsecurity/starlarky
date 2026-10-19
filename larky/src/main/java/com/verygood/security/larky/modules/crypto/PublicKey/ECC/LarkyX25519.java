/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.verygood.security.larky.modules.crypto.PublicKey.ECC;

import java.security.SecureRandom;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.math.ec.rfc7748.X25519;

/**
 * The X25519 function of RFC 7748 (Section 5), backed by BouncyCastle's constant-time Montgomery
 * ladder. This is the primitive behind Curve25519 keys in {@code vendor/Crypto/PublicKey/ECC.star},
 * which stands in for pycryptodome's C module {@code _curve25519}.
 *
 * <p>Scalars and u-coordinates are 32-byte little-endian strings, as in RFC 7748. {@link #x25519}
 * clamps the scalar and masks the top bit of u, as the RFC's decodeScalar25519/decodeUCoordinate
 * do.
 */
@StarlarkBuiltin(name = "X25519", doc = "The X25519 function of RFC 7748.")
public class LarkyX25519 implements StarlarkValue {

  public static final LarkyX25519 INSTANCE = new LarkyX25519();

  private static final int SIZE = X25519.POINT_SIZE; // == X25519.SCALAR_SIZE == 32

  private static byte[] bytes32(String fn, String arg, StarlarkBytes value) throws EvalException {
    if (value.size() != SIZE) {
      throw Starlark.errorf(
          "ValueError: %s(%s=...) expected %d bytes; got %d bytes", fn, arg, SIZE, value.size());
    }
    return value.toByteArray();
  }

  @StarlarkMethod(
      name = "x25519",
      doc =
          "X25519(scalar, u) of RFC 7748, Section 5. Returns the 32-byte u-coordinate of the "
              + "result, or None when it is all zero (the input is a point of small order, "
              + "RFC 7748 Section 6.1). The zero test does not branch on secret bytes.",
      parameters = {
        @Param(name = "scalar", allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
        @Param(name = "u", allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
      },
      allowReturnNones = true,
      useStarlarkThread = true)
  public Object x25519(StarlarkBytes scalar, StarlarkBytes u, StarlarkThread thread)
      throws EvalException {
    byte[] k = bytes32("x25519", "scalar", scalar);
    byte[] uc = bytes32("x25519", "u", u);
    byte[] r = new byte[SIZE];
    // calculateAgreement = scalarMult + a constant-time all-zero check of the output.
    if (!X25519.calculateAgreement(k, 0, uc, 0, r, 0)) {
      return Starlark.NONE;
    }
    return StarlarkBytes.of(thread.mutability(), r);
  }

  @StarlarkMethod(
      name = "public_key",
      doc = "X25519(scalar, 9): the public key of a 32-byte private key.",
      parameters = {
        @Param(name = "scalar", allowedTypes = {@ParamType(type = StarlarkBytes.class)}),
      },
      useStarlarkThread = true)
  public StarlarkBytes publicKey(StarlarkBytes scalar, StarlarkThread thread)
      throws EvalException {
    byte[] k = bytes32("public_key", "scalar", scalar);
    byte[] r = new byte[SIZE];
    X25519.generatePublicKey(k, 0, r, 0);
    return StarlarkBytes.of(thread.mutability(), r);
  }

  @StarlarkMethod(
      name = "generate_private_key",
      doc =
          "32 bytes from Larky's CSPRNG (CryptoServicesRegistrar.getSecureRandom(), as "
              + "Crypto.Random uses), clamped as RFC 7748 Section 5 describes.",
      useStarlarkThread = true)
  public StarlarkBytes generatePrivateKey(StarlarkThread thread) {
    SecureRandom random = CryptoServicesRegistrar.getSecureRandom();
    byte[] k = new byte[SIZE];
    X25519.generatePrivateKey(random, k);
    return StarlarkBytes.of(thread.mutability(), k);
  }
}
