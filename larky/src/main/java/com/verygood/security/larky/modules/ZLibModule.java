package com.verygood.security.larky.modules;

import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.verygood.security.larky.parser.StarlarkUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkBytes.StarlarkByteArray;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkValue;

import org.jetbrains.annotations.NotNull;


@StarlarkBuiltin(
    name = "jzlib",
    category = "BUILTIN",
    doc = "java specific zlib implementation")
public class ZLibModule implements StarlarkValue {

  public static final ZLibModule INSTANCE = new ZLibModule();

  /**
   *  The NMAX optimization avoids modulo calculations on every iteration.
   *
   *  Simply put, NMAX is the max number of additions to make before you have to
   *  perform the modulo calculation. As a result, NMAX is the largest n such that:
   *
   *      255n(n+1)/2 + (n+1)(BASE-1) <= 2^32-1
   */
  static final int NMAX = 5552;
  static final int LARGEST_PRIME_SMALLER_THAN_65536 = 65521;

  @StarlarkMethod(
    name = "adler32",
    doc = "Computes an Adler-32 checksum of data. " +
            "(An Adler-32 checksum is almost as reliable as a CRC32 but can " +
            " be computed much more quickly.) The result is an " +
            "unsigned 32-bit integer. If value is present, it is used as the" +
            " starting value of the checksum; otherwise, a default value " +
            "of 1 is used. Passing in value allows computing a running " +
            "checksum over the concatenation of several inputs. The algorithm " +
            "is not cryptographically strong, and should not be used for " +
            "authentication or digital signatures. " +
            "Since the algorithm is designed for use as a checksum algorithm, " +
            "it is not suitable for use as a general hash algorithm.\n" +
            "\n" +
            "Changed in version 3.0: Always returns an unsigned value. To " +
            "generate the same numeric value across all Python versions and " +
            "platforms, use adler32(data) & 0xffffffff.",
    parameters = {
      @Param(name = "data"),
      @Param(
        name = "value",
        defaultValue = "1",
        allowedTypes = {@ParamType(type = StarlarkInt.class)})
    }
  )
  public StarlarkInt adler32(StarlarkBytes data, StarlarkInt value) throws EvalException {
    int val = 1;
    if(value != StarlarkInt.of(1)) {
      try {
        val = Integer.parseUnsignedInt(value.toString());
      } catch (NumberFormatException e) {
        throw Starlark.errorf("%s must be an integer (0x0 and 0xFFFF_FFFF)!", value);
      }
    }

    return StarlarkInt.of(
      Integer.toUnsignedLong(
        calculateAdler32(data.toByteArray(),data.size(),val)));
  }

  /**
   * The Adler32 checksum is discussed in RFC1950.  The sample implementation
   * from this RFC is shown below:
   *
   * <pre>
   *    #define BASE 65521  largest prime smaller than 65536
   *    unsigned long update_adler32(unsigned long adler,
   *       unsigned char *buf, int len)
   *    {
   *      unsigned long s1 = adler & 0xffff;
   *      unsigned long s2 = (adler >> 16) & 0xffff;
   *      int n;
   *
   *      for (n = 0; n < len; n++) {
   *        s1 = (s1 + buf[n]) % BASE;
   *        s2 = (s2 + s1)     % BASE;
   *      }
   *      return (s2 << 16) + s1;
   *    }
   *
   *    unsigned long adler32(unsigned char *buf, int len)
   *    {
   *      return update_adler32(1L, buf, len);
   *    }
   * </pre>   * @param bytes
   */
  int calculateAdler32(byte[] bytes, int len, int value) {
      int index = 0;
      int result = value;
      int s1 = result & 0xffff;
      int s2 = result >>> 16;
      while (index < len) {
          int max = Math.min(index + NMAX, index + len);
          while (index < max) {
              s1 = (bytes[index++] & 0xff) + s1;
              s2 += s1;
          }
          s1 %= LARGEST_PRIME_SMALLER_THAN_65536;
          s2 %= LARGEST_PRIME_SMALLER_THAN_65536;
      }
      result = (s2 << 16) | s1;
      return result;
  }

  static class LarkyInflater implements StarlarkValue {

    private final Inflater inflater;

    private LarkyInflater(boolean nowrap) {
      this.inflater = new Inflater(nowrap);
    }

    public static @NotNull LarkyInflater of(boolean param) {
      return new LarkyInflater(param);
    }

    @StarlarkMethod(name="setInput", parameters = {@Param(name = "param")})
    public void setInput(StarlarkBytes param) {
      this.inflater.setInput(param.toByteArray());
    }

    @StarlarkMethod(name="getRemaining")
    public StarlarkInt getRemaining() {
      return StarlarkInt.of(this.inflater.getRemaining());
    }

    @StarlarkMethod(name="finished")
    public boolean finished() {
      return this.inflater.finished();
    }

    @StarlarkMethod(name="end")
    public void end() {
      this.inflater.end();
    }

    @StarlarkMethod(name="inflate", parameters = {
      @Param(name = "buf"),
      @Param(name = "offset", defaultValue = "0"),
      @Param(name = "length", defaultValue = "unbound"),
    })
    public StarlarkInt inflate(StarlarkByteArray buf, Object offsetO, Object lengthO) throws EvalException {
      final byte[] bytes = buf.toByteArray();
      int result;

      if(!StarlarkUtil.isNullOrNoneOrUnbound(lengthO)) {
        final StarlarkInt off = (StarlarkInt) StarlarkUtil.valueToStarlark(offsetO);
        final StarlarkInt len = (StarlarkInt) StarlarkUtil.valueToStarlark(lengthO);
        try {
          result = this.inflater.inflate(bytes, off.toIntUnchecked(), len.toIntUnchecked());
        }catch (DataFormatException e) {
          throw new EvalException(e.getMessage(), e.getCause());
        }
      }
      else {
        try {
          result = this.inflater.inflate(bytes);
        }catch(DataFormatException e) {
          throw new EvalException(e.getMessage(), e.getCause());
        }
      }

      buf.replaceAll(StarlarkBytes.immutableOf(bytes));
      return StarlarkInt.of(result);
    }

  }

  @StarlarkMethod(name="Inflater", parameters = {@Param(name = "bool")})
  public LarkyInflater inflater(boolean param) {
    return LarkyInflater.of(param);
  }

  static class LarkyDeflater implements StarlarkValue {

    private final Deflater deflater;

    private LarkyDeflater(int level, boolean nowrap) {
      this.deflater = new Deflater(level, nowrap);
    }

    /**
     * Creates a new compressor using the specified compression level.
     * If 'nowrap' is true then the ZLIB header and checksum fields will
     * not be used in order to support the compression format used in
     * both GZIP and PKZIP.
     * @param level the compression level (0-9)
     * @param nowrap if true then use GZIP compatible compression
     */
    public static @NotNull LarkyDeflater of(int level, boolean nowrap) {
      return new LarkyDeflater(level, nowrap);
    }

    @StarlarkMethod(name="setInput", parameters = {
      @Param(name = "data"),
      @Param(name = "offset", defaultValue = "0"),
      @Param(name = "length", defaultValue = "unbound"),
    })
    public void setInput(StarlarkBytes data, Object offsetO, Object lengthO) {
      if(!StarlarkUtil.isNullOrNoneOrUnbound(lengthO)) {
        final StarlarkInt off = (StarlarkInt) StarlarkUtil.valueToStarlark(offsetO);
        final StarlarkInt len = (StarlarkInt) StarlarkUtil.valueToStarlark(lengthO);
        deflater.setInput(data.toByteArray(), off.toIntUnchecked(), len.toIntUnchecked());
      }
      else {
        deflater.setInput(data.toByteArray());
      }
    }

    @StarlarkMethod(name="finish")
    public void finish() {
      deflater.finish();
    }

    @StarlarkMethod(name="finished")
    public boolean finished() {
      return deflater.finished();
    }

    @StarlarkMethod(name="end")
    public void end() {
      deflater.end();
    }

    @StarlarkMethod(name="setStrategy", parameters = {@Param(name="strategy")})
    public void setStrategy(StarlarkInt strategy) {
      deflater.setStrategy(strategy.toIntUnchecked());
    }

    @StarlarkMethod(name="deflate", parameters = {
      @Param(name = "buf"),
    })
    public StarlarkInt deflate(StarlarkByteArray buf) {
      final int outLength = buf.size();
      final byte[] out = new byte[outLength];
      int result = deflater.deflate(out);
      for (int i = 0; i < outLength; i++) {
        byte b = out[i];
        buf.set(i, b);
      }
      return StarlarkInt.of(result);
    }
  }

  @StarlarkMethod(name="Deflater", parameters = {
    @Param(name = "level", allowedTypes = {@ParamType(type= StarlarkInt.class)}),
    @Param(name = "nowrap", allowedTypes = {@ParamType(type= Boolean.class)}),
  })
  public LarkyDeflater deflater(StarlarkInt level, boolean nowrap) {
    return LarkyDeflater.of(level.toIntUnchecked(), nowrap);
  }
}
