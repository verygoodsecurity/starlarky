// Copyright 2020 The Bazel Authors. All rights reserved.
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
import net.starlark.java.annot.StarlarkBuiltin;

/** The Starlark int data type. */
@StarlarkBuiltin(
    name = "int",
    category = "core",
    doc =
        "The type of integers in Starlark. Starlark integers may be of any magnitude; arithmetic"
            + " is exact. Examples of integer expressions:<br>"
            + "<pre class=\"language-python\">153\n"
            + "0x2A  # hexadecimal literal\n"
            + "0o54  # octal literal\n"
            + "23 * 2 + 5\n"
            + "100 / -7\n"
            + "100 % -7  # -5 (unlike in some other languages)\n"
            + "int(\"18\")\n"
            + "</pre>")
public abstract class StarlarkInt implements StarlarkValue, Comparable<StarlarkInt> {

  // A cache of small integers >= LEAST_SMALLINT.
  private static final int LEAST_SMALLINT = -128;
  private static final Int32[] smallints = new Int32[100_000];

  static final StarlarkInt ZERO = StarlarkInt.of(0);

  /** Returns the Starlark int value that represents x. */
  public static StarlarkInt of(int x) {
    int index = x - LEAST_SMALLINT; // (may overflow)
    if (0 <= index && index < smallints.length) {
      Int32 xi = smallints[index];
      if (xi == null) {
        xi = new Int32(x);
        smallints[index] = xi;
      }
      return xi;
    }
    return new Int32(x);
  }

  /** Returns the Starlark int value that represents x. */
  public static StarlarkInt of(long x) {
    if ((long) (int) x == x) {
      return StarlarkInt.of((int) x);
    }
    return new Int64(x);
  }

  /** Returns the Starlark int value that represents x. */
  public static StarlarkInt of(BigInteger x) {
    if (x.bitLength() < 64) {
      return StarlarkInt.of(x.longValue());
    }
    return new Big(x);
  }

  /**
   * Returns the int denoted by a literal string in the specified base, as if by the Starlark
   * expression {@code int(str, base)}.
   */
  public static StarlarkInt parse(String str, int base) throws NumberFormatException {
    return MethodLibrary.parseInt(str, base);
  }

  // Subclass for values exactly representable in a Java int.
  private static final class Int32 extends StarlarkInt {
    final int v;

    Int32(int v) {
      this.v = v;
    }

    @Override
    public int toInt(String what) {
      return v;
    }

    @Override
    public long toLong(String what) {
      return (long) v;
    }

    @Override
    public BigInteger toBigInteger() {
      return BigInteger.valueOf(v);
    }

    @Override
    public Number toNumber() {
      return v;
    }

    @Override
    public int signum() {
      return Integer.signum(v);
    }

    @Override
    public int hashCode() {
      return 0x316c5239 * Integer.hashCode(v) ^ 0x67c4a7d5;
    }

    @Override
    public boolean equals(Object that) {
      return that instanceof Int32 && this.v == ((Int32) that).v;
    }
  }

  // Subclass for values exactly representable in a Java long.
  private static final class Int64 extends StarlarkInt {
    final long v;

    Int64(long v) {
      this.v = v;
    }

    @Override
    public long toLong(String what) {
      return v;
    }

    @Override
    public BigInteger toBigInteger() {
      return BigInteger.valueOf(v);
    }

    @Override
    public Number toNumber() {
      return v;
    }

    @Override
    public int signum() {
      return Long.signum(v);
    }

    @Override
    public int hashCode() {
      return 0x67c4a7d5 * Long.hashCode(v) ^ 0xee914a1b;
    }

    @Override
    public boolean equals(Object that) {
      return that instanceof Int64 && this.v == ((Int64) that).v;
    }
  }

  // Subclass for values not exactly representable in a long.
  private static final class Big extends StarlarkInt {
    final BigInteger v;

    Big(BigInteger v) {
      this.v = v;
    }

    @Override
    public BigInteger toBigInteger() {
      return v;
    }

    @Override
    public Number toNumber() {
      return v;
    }

    @Override
    public int signum() {
      return v.signum();
    }

    @Override
    public int hashCode() {
      return 0xee914a1b * v.hashCode() ^ 0x6406918f;
    }

    @Override
    public boolean equals(Object that) {
      return that instanceof Big && this.v.equals(((Big) that).v);
    }
  }

  /** Returns the value of this StarlarkInt as a Number (Integer, Long, or BigInteger). */
  public abstract Number toNumber();

  /** Returns the signum of this StarlarkInt (-1, 0, or +1). */
  public abstract int signum();

  /** Returns this StarlarkInt as a string of decimal digits. */
  @Override
  public String toString() {
    // TODO(adonovan): opt: avoid Number allocation
    return toNumber().toString();
  }

  @Override
  public void repr(Printer printer) {
    // TODO(adonovan): opt: avoid Number and String allocations.
    printer.append(toString());
  }

  /** Returns the signed int32 value of this StarlarkInt, or fails if not exactly representable. */
  public int toInt(String what) throws EvalException {
    throw Starlark.errorf("got %s for %s, want value in signed 32-bit range", this, what);
  }

  /** Returns the signed int64 value of this StarlarkInt, or fails if not exactly representable. */
  public long toLong(String what) throws EvalException {
    throw Starlark.errorf("got %s for %s, want value in the signed 64-bit range", this, what);
  }

  /** Returns the BigInteger value of this StarlarkInt. */
  public abstract BigInteger toBigInteger();

  /**
   * Returns the the value of this StarlarkInt as a Java signed 32-bit int.
   *
   * @throws IllegalArgumentException if this int is not in that value range.
   */
  public final int toIntUnchecked() throws IllegalArgumentException {
    if (this instanceof Int32) {
      return ((Int32) this).v;
    }
    // Use a constant exception to avoid allocation.
    // This operator is provided for fast access and case discrimination.
    // Use toInt(String) for user-visible errors.
    throw NOT_INT32;
  }

  private static final IllegalArgumentException NOT_INT32 =
      new IllegalArgumentException("not a signed 32-bit value");

  /** Returns the result of truncating this value into the signed 32-bit range. */
  public int truncateToInt() {
    if (this instanceof Int32) {
      return ((Int32) this).v;
    } else if (this instanceof Int64) {
      return (int) ((Int64) this).v;
    } else {
      return toBigInteger().intValue();
    }
  }

  @Override
  public boolean isImmutable() {
    return true;
  }

  @Override
  public boolean truth() {
    return this != ZERO;
  }

  @Override
  public int compareTo(StarlarkInt x) {
    return compare(this, x);
  }

  // binary operators

  // In the common case, both operands are int32, so the operations
  // can be done using longs with minimal fuss around overflows.
  // All other combinations are promoted to BigInteger.
  // TODO(adonovan): use long x long operations even if one or both
  // operands are Int64; promote to Big x Big only upon overflow.
  // (See the revision history for the necessary overflow checks.)

  /** Returns a value whose signum is equal to x - y. */
  public static int compare(StarlarkInt x, StarlarkInt y) {
    if (x instanceof Int32 && y instanceof Int32) {
      return Integer.compare(((Int32) x).v, ((Int32) y).v);
    }
    return x.toBigInteger().compareTo(y.toBigInteger());
  }

  /** Returns x + y. */
  public static StarlarkInt add(StarlarkInt x, StarlarkInt y) {
    if (x instanceof Int32 && y instanceof Int32) {
      long xl = ((Int32) x).v;
      long yl = ((Int32) y).v;
      return StarlarkInt.of(xl + yl);
    }

    BigInteger xbig = x.toBigInteger();
    BigInteger ybig = y.toBigInteger();
    BigInteger zbig = xbig.add(ybig);
    return StarlarkInt.of(zbig);
  }

  /** Returns x - y. */
  public static StarlarkInt subtract(StarlarkInt x, StarlarkInt y) {
    if (x instanceof Int32 && y instanceof Int32) {
      long xl = ((Int32) x).v;
      long yl = ((Int32) y).v;
      return StarlarkInt.of(xl - yl);
    }

    BigInteger xbig = x.toBigInteger();
    BigInteger ybig = y.toBigInteger();
    BigInteger zbig = xbig.subtract(ybig);
    return StarlarkInt.of(zbig);
  }

  /** Returns x * y. */
  public static StarlarkInt multiply(StarlarkInt x, StarlarkInt y) {
    if (x instanceof Int32 && y instanceof Int32) {
      long xl = ((Int32) x).v;
      long yl = ((Int32) y).v;
      return StarlarkInt.of(xl * yl);
    }

    BigInteger xbig = x.toBigInteger();
    BigInteger ybig = y.toBigInteger();
    BigInteger zbig = xbig.multiply(ybig);
    return StarlarkInt.of(zbig);
  }

  /** Returns x / y (real division). */
  public static StarlarkInt divide(StarlarkInt x, StarlarkInt y) throws EvalException {
    if (y == ZERO) {
      throw Starlark.errorf("real division by zero");
    }
    if (x instanceof Int32 && y instanceof Int32) {
      long xl = ((Int32) x).v;
      long yl = ((Int32) y).v;
      return StarlarkInt.of(xl / yl);
    }

    BigInteger xbig = x.toBigInteger();
    BigInteger ybig = y.toBigInteger();
    BigInteger zbig = xbig.divide(ybig);
    return StarlarkInt.of(zbig);
  }

  /** Returns x // y (floor of integer division). */
  public static StarlarkInt floordiv(StarlarkInt x, StarlarkInt y) throws EvalException {
    if (y == ZERO) {
      throw Starlark.errorf("integer division by zero");
    }
    if (x instanceof Int32 && y instanceof Int32) {
      long xl = ((Int32) x).v;
      long yl = ((Int32) y).v;
      // http://python-history.blogspot.com/2010/08/why-pythons-integer-division-floors.html
      long quo = xl / yl;
      long rem = xl % yl;
      if ((xl < 0) != (yl < 0) && rem != 0) {
        quo--;
      }
      return StarlarkInt.of(quo);
    }

    BigInteger xbig = x.toBigInteger();
    BigInteger ybig = y.toBigInteger();
    BigInteger[] quorem = xbig.divideAndRemainder(ybig);
    if ((xbig.signum() < 0) != (ybig.signum() < 0) && quorem[1].signum() != 0) {
      quorem[0] = quorem[0].subtract(BigInteger.ONE);
    }
    return StarlarkInt.of(quorem[0]);
  }

  /** Returns x % y. */
  public static StarlarkInt mod(StarlarkInt x, StarlarkInt y) throws EvalException {
    if (y == ZERO) {
      throw Starlark.errorf("integer modulo by zero");
    }
    if (x instanceof Int32 && y instanceof Int32) {
      long xl = ((Int32) x).v;
      long yl = ((Int32) y).v;
      // In Starlark, the sign of the result is the sign of the divisor.
      long z = xl % yl;
      if ((xl < 0) != (yl < 0) && z != 0) {
        z += yl;
      }
      return StarlarkInt.of(z);
    }

    BigInteger xbig = x.toBigInteger();
    BigInteger ybig = y.toBigInteger();
    BigInteger zbig = xbig.remainder(ybig);
    if ((x.signum() < 0) != (y.signum() < 0) && zbig.signum() != 0) {
      zbig = zbig.add(ybig);
    }
    return StarlarkInt.of(zbig);
  }

  /** Returns x >> y. */
  public static StarlarkInt shiftRight(StarlarkInt x, StarlarkInt y) throws EvalException {
    int yi = y.toInt("shift count");
    if (yi < 0) {
      throw Starlark.errorf("negative shift count: %d", yi);
    }
    if (x instanceof Int32) {
      long xl = ((Int32) x).v;
      if (yi >= Integer.SIZE) {
        return xl < 0 ? StarlarkInt.of(-1) : ZERO;
      }
      return StarlarkInt.of(xl >> yi);
    }

    BigInteger xbig = x.toBigInteger();
    BigInteger zbig = xbig.shiftRight(yi);
    return StarlarkInt.of(zbig);
  }

  /** Returns x << y. */
  public static StarlarkInt shiftLeft(StarlarkInt x, StarlarkInt y) throws EvalException {
    int yi = y.toInt("shift count");
    if (yi < 0) {
      throw Starlark.errorf("negative shift count: %d", yi);
    } else if (yi >= 512) {
      throw Starlark.errorf("shift count too large: %d", yi);
    }
    if (x instanceof Int32) {
      long xl = ((Int32) x).v;
      long z = xl << yi; // only uses low 6 bits of yi
      if ((z >> yi) == xl && yi < 64) {
        return StarlarkInt.of(z);
      }
      /* overflow */
    }

    BigInteger xbig = x.toBigInteger();
    BigInteger zbig = xbig.shiftLeft(yi);
    return StarlarkInt.of(zbig);
  }

  /** Returns x ^ y. */
  public static StarlarkInt xor(StarlarkInt x, StarlarkInt y) {
    if (x instanceof Int32 && y instanceof Int32) {
      long xl = ((Int32) x).v;
      long yl = ((Int32) y).v;
      return StarlarkInt.of(xl ^ yl);
    }

    BigInteger xbig = x.toBigInteger();
    BigInteger ybig = y.toBigInteger();
    BigInteger zbig = xbig.xor(ybig);
    return StarlarkInt.of(zbig);
  }

  /** Returns x | y. */
  public static StarlarkInt or(StarlarkInt x, StarlarkInt y) {
    if (x instanceof Int32 && y instanceof Int32) {
      long xl = ((Int32) x).v;
      long yl = ((Int32) y).v;
      return StarlarkInt.of(xl | yl);
    }

    BigInteger xbig = x.toBigInteger();
    BigInteger ybig = y.toBigInteger();
    BigInteger zbig = xbig.or(ybig);
    return StarlarkInt.of(zbig);
  }

  /** Returns x & y. */
  public static StarlarkInt and(StarlarkInt x, StarlarkInt y) {
    if (x instanceof Int32 && y instanceof Int32) {
      long xl = ((Int32) x).v;
      long yl = ((Int32) y).v;
      return StarlarkInt.of(xl & yl);
    }

    BigInteger xbig = x.toBigInteger();
    BigInteger ybig = y.toBigInteger();
    BigInteger zbig = xbig.and(ybig);
    return StarlarkInt.of(zbig);
  }

  /** Returns ~x. */
  public static StarlarkInt bitnot(StarlarkInt x) {
    if (x instanceof Int32) {
      long xl = ((Int32) x).v;
      return StarlarkInt.of(~xl);
    }

    BigInteger xbig = x.toBigInteger();
    BigInteger ybig = MINUS1BIG.subtract(xbig);
    return StarlarkInt.of(ybig);
  }

  /** Returns -x. */
  public static StarlarkInt uminus(StarlarkInt x) {
    if (x instanceof Int32) {
      long xl = ((Int32) x).v;
      return StarlarkInt.of(-xl);
    }

    BigInteger xbig = x.toBigInteger();
    BigInteger ybig = xbig.negate();
    return StarlarkInt.of(ybig);
  }

  private static final BigInteger MINUS1BIG = BigInteger.ONE.negate();
}
