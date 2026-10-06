// Copyright 2026 Very Good Security Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package net.starlark.java.eval;

import java.math.BigInteger;

/**
 * Size limits on ints, so that one operation of an untrusted script can't run for seconds.
 *
 * <p>An int operation is one Java call that step limits and the expiration date can't interrupt:
 * {@code x = x * x} reaches 53 million bits in 25 steps (2.7 s), and {@code pow(3, 10**7)} takes
 * 0.8 s. Operations that would make an int of more than {@link #MAX_BITS} bits fail before doing
 * the work, as Starlark's {@code <<} already refuses shifts of 512 bits or more. A modular power's
 * cost grows with the cube of its size, so its modulus and exponent have their own, smaller limit.
 */
public final class IntLimits {

  private IntLimits() {}

  /** The largest int, in bits (-Dstarlark.int.maxBits, default 65,536: 19,729 digits). */
  public static final int MAX_BITS = Math.max(64, Integer.getInteger("starlark.int.maxBits", 65_536));

  /**
   * The largest modulus and exponent of a modular power, in bits (-Dstarlark.int.maxModPowBits,
   * default 16,384: about 0.7 s for one full-size modPow; 32,768 bits take 33 s).
   */
  public static final int MAX_MODPOW_BITS =
      Math.max(64, Integer.getInteger("starlark.int.maxModPowBits", 16_384));

  /** Fails if an int of {@code bits} bits, made by {@code op}, would exceed {@link #MAX_BITS}. */
  public static void checkBits(long bits, String op) throws EvalException {
    if (bits > MAX_BITS) {
      throw Starlark.errorf(
          "int too large: %s would make an int of about %d bits; the limit is %d",
          op, bits, MAX_BITS);
    }
  }

  /** Fails unless {@code x * y} fits: a product has at most bitLength(x) + bitLength(y) bits. */
  static void checkProduct(StarlarkInt x, StarlarkInt y) throws EvalException {
    long bits = (long) bitLength(x) + bitLength(y);
    if (bits > MAX_BITS) {
      checkBits(bits, "*");
    }
  }

  /** The bit length of |x| (as BigInteger.bitLength), without allocating for a small x. */
  static int bitLength(StarlarkInt x) {
    try {
      long v = x.toLongFast();
      return 64 - Long.numberOfLeadingZeros(v < 0 ? ~v : v);
    } catch (Exception big) { // StarlarkInt.Overflow: x doesn't fit in a long
      return x.toBigInteger().bitLength();
    }
  }

  /**
   * Fails if {@code base ** exp} (exp >= 0) certainly exceeds the limit, before computing it: the
   * result has at least (bitLength(|base|) - 1) * exp + 1 bits. A result that may fit is at most
   * twice the limit; check it with {@link #checkBits} after computing.
   */
  public static void checkPow(BigInteger base, long exp) throws EvalException {
    int b = base.abs().bitLength();
    if (b <= 1 || exp == 0) {
      return; // 0, 1 and -1 stay small
    }
    checkBits((long) (b - 1) * exp + 1, "pow");
  }

  /** Fails if {@code pow(base, exp, mod)} is too costly to compute in one step. */
  public static void checkModPow(BigInteger exp, BigInteger mod) throws EvalException {
    if (mod.bitLength() > MAX_MODPOW_BITS || exp.bitLength() > MAX_MODPOW_BITS) {
      throw Starlark.errorf(
          "int too large: pow() with a %d-bit exponent and a %d-bit modulus; the limit is %d bits"
              + " each",
          exp.bitLength(), mod.bitLength(), MAX_MODPOW_BITS);
    }
  }

  /**
   * Fails (NumberFormatException, as other parse errors) if {@code digits} in {@code base} would
   * make an int beyond the limit: parsing is quadratic in the number of digits.
   */
  static void checkDigits(String digits, int base) {
    // Each digit carries at most log2(base) bits; leading zeros only make this an overestimate.
    long bits = (long) Math.ceil(digits.length() * (Math.log(base) / Math.log(2)));
    if (bits > MAX_BITS + 64) {
      throw new NumberFormatException(
          String.format(
              "int too large: a %d-digit base-%d literal; the limit is %d bits",
              digits.length(), base, MAX_BITS));
    }
  }
}
