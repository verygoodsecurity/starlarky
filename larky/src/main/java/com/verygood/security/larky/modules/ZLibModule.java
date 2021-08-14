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

  @StarlarkMethod(name="NO_FLUSH", structField = true)
  public StarlarkInt NO_FLUSH() {
    return StarlarkInt.of(Deflater.NO_FLUSH);
  }

  @StarlarkMethod(name="SYNC_FLUSH", structField = true)
  public StarlarkInt SYNC_FLUSH() {
    return StarlarkInt.of(Deflater.SYNC_FLUSH);
  }

  @StarlarkMethod(name="FULL_FLUSH", structField = true)
  public StarlarkInt FULL_FLUSH() {
    return StarlarkInt.of(Deflater.FULL_FLUSH);
  }

  public enum Flush {
    Z_NO_FLUSH, Z_PARTIAL_FLUSH, Z_SYNC_FLUSH, Z_FULL_FLUSH, Z_FINISH, Z_BLOCK;

    public static int mapFlush(Flush flush) {
      switch (flush) {
        case Z_SYNC_FLUSH:
          return Deflater.SYNC_FLUSH;
        case Z_FULL_FLUSH:
          return Deflater.FULL_FLUSH;
        case Z_NO_FLUSH:
        default:
          return Deflater.NO_FLUSH;
      }
    }

    public static int mapFlush(int flush) {
      return mapFlush(Flush.values()[flush]);
    }

  }

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
    private static final byte[] EMPTY_ARRAY = new byte[0];
    private final Inflater inflater;
    private final boolean rawInflate;
    private byte[] zdict;

    private LarkyInflater(boolean rawInflate) {
      this.rawInflate = rawInflate;
      this.inflater = new Inflater(rawInflate);
      this.zdict = EMPTY_ARRAY;
    }

    public static @NotNull LarkyInflater of(boolean rawInflate) {
      return new LarkyInflater(rawInflate);
    }

    @StarlarkMethod(name="setDictionary", parameters = {@Param(name="zdict")})
    public void setDictionary(StarlarkBytes zdict) throws EvalException {
      if(StarlarkUtil.isNullOrNoneOrUnbound(zdict)) {
        throw Starlark.errorf("setDictionary requires a sequence of bytes of at least length 1");
      }
      this.zdict = zdict.toByteArray();
    }

    @StarlarkMethod(name="setInput", parameters = {@Param(name = "param")})
    public void setInput(StarlarkBytes param) {
      this.inflater.setInput(param.toByteArray());
    }

    @StarlarkMethod(name="needs_dictionary")
    public boolean needsDictionary() {
      return inflater.needsDictionary();
    }

    @StarlarkMethod(name="needs_input")
    public boolean needsInput() {
      return inflater.needsInput();
    }

    @StarlarkMethod(name="getRemaining")
    public StarlarkInt getRemaining() {
      return StarlarkInt.of(this.inflater.getRemaining());
    }

    @StarlarkMethod(name="reset")
    public void reset() {
      inflater.reset();
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
      if(rawInflate) {
        // The docs (https://github.com/madler/zlib/blob/master/zlib.h#L828) say that in raw mode
        // setDictionary can be called right after inflateInit2, so set the dictionary before
        // inflate() if the mode is INFLATERAW
        this.inflater.setDictionary(this.zdict);
      }
      try {
        return _inflate(buf, offsetO, lengthO);
      } catch (DataFormatException e) {
        throw new EvalException(e.getMessage(), e.getCause());
      }
    }

    @NotNull
    private StarlarkInt _inflate(StarlarkByteArray buf, Object offsetO, Object lengthO) throws DataFormatException {
      final byte[] bytes = buf.toByteArray();
      int result;
      if(!StarlarkUtil.isNullOrNoneOrUnbound(lengthO)) {
        final StarlarkInt off = (StarlarkInt) StarlarkUtil.valueToStarlark(offsetO);
        final StarlarkInt len = (StarlarkInt) StarlarkUtil.valueToStarlark(lengthO);
        result = this.inflater.inflate(bytes, off.toIntUnchecked(), len.toIntUnchecked());
      }
      else {
        result = this.inflater.inflate(bytes);
      }

      if (!rawInflate && result == 0 && inflater.needsDictionary() && zdict.length > 0) {
        inflater.setDictionary(zdict);
        result = inflater.inflate(bytes);
      }
      buf.replaceAll(StarlarkBytes.immutableOf(bytes));
      return StarlarkInt.of(result);
    }

  }

  @StarlarkMethod(name="Inflater", parameters = {@Param(name = "bool")})
  public LarkyInflater inflater(boolean param) {
    return LarkyInflater.of(param);
  }

  // start COMPRESSION (OR DEFLATE)
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

    @StarlarkMethod(name="reset")
    public void reset() {
      deflater.reset();
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

    @StarlarkMethod(name="setDictionary", parameters = {@Param(name="zdict")})
    public void setDictionary(StarlarkBytes zdict) throws EvalException {
      if(StarlarkUtil.isNullOrNoneOrUnbound(zdict)) {
        throw Starlark.errorf("setDictionary requires a sequence of bytes of at least length 1");
      }
      deflater.setDictionary(zdict.toByteArray());
    }

    @StarlarkMethod(name="deflate", parameters = {
      @Param(name = "buf"),
      @Param(name = "flush"),
    })
    public StarlarkInt deflate(StarlarkByteArray buf, StarlarkInt flush) throws EvalException {
      final int outLength = buf.size();
      final byte[] out = new byte[outLength];
      int result;
      try {
        // flush here means we have to reset input?
        result = deflater.deflate(out, 0, outLength, flush.toIntUnchecked());
      } catch(IllegalArgumentException e) {
        throw new EvalException(e.getMessage(), e.getCause());
      }
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
