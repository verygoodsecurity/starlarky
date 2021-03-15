package com.verygood.security.larky.modules;

import com.verygood.security.larky.modules.codecs.TextUtil;
import com.verygood.security.larky.modules.types.LarkyByteArray;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;

import java.io.ByteArrayInputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.LongStream;


@StarlarkBuiltin(
    name = "jstruct",
    category = "BUILTIN",
    doc = ""
)
public class StructModule implements StarlarkValue {

  public static final StructModule INSTANCE = new StructModule();
  private final short BigEndian = 0;
  private final short LittleEndian = 1;
  private final short nativeByteOrder;
  private short byteOrder;

  public StructModule() {
    ByteOrder x = ByteOrder.nativeOrder();
    if (x == ByteOrder.LITTLE_ENDIAN) {
      nativeByteOrder = LittleEndian;
    } else {
      nativeByteOrder = BigEndian;
    }
    byteOrder = nativeByteOrder;
  }

  private byte[] reverseBytes(byte[] b) {
    byte tmp;
    for (int i = 0; i < (b.length / 2); i++) {
      tmp = b[i];
      b[i] = b[b.length - i - 1];
      b[b.length - i - 1] = tmp;
    }

    return b;
  }


  private byte[] packRaw_16b(short val) {
    byte[] bx = new byte[2];

    if (val >= 0) {
      bx[0] = (byte) (val & 0xff);
      bx[1] = (byte) ((val >> 8) & 0xff);

    } else {
      int v2 = Math.abs(val);
      v2 = (v2 ^ 0x7fff) + 1; // invert bits and add 1
      v2 = v2 | (1 << 15);
      bx[0] = (byte) (v2 & 0xff);
      bx[1] = (byte) ((v2 >> 8) & 0xff);

    }

    if (byteOrder == BigEndian) {
      bx = reverseBytes(bx);
    }

    return bx;
  }

  private byte[] packRaw_u16b(int val) {
    byte[] bx = new byte[2];

    val = val & 0xffff; //truncate

    if (val >= 0) {
      bx[0] = (byte) (val & 0xff);
      bx[1] = (byte) ((val >> 8) & 0xff);

    }

    if (byteOrder == BigEndian) {
      bx = reverseBytes(bx);
    }

    return bx;
  }

  private byte[] packRaw_32b(int val) {
    byte[] bx = new byte[4];

    if (val >= 0) {
      bx[0] = (byte) (val & 0xff);
      bx[1] = (byte) ((val >> 8) & 0xff);
      bx[2] = (byte) ((val >> 16) & 0xff);
      bx[3] = (byte) ((val >> 24) & 0xff);

    } else {
      long v2 = Math.abs(val);
      v2 = (v2 ^ 0x7fffffff) + 1; // invert bits and add 1
      v2 = v2 | (1L << 31); // add the 32nd bit as negative bit
      bx[0] = (byte) (v2 & 0xff);
      bx[1] = (byte) ((v2 >> 8) & 0xff);
      bx[2] = (byte) ((v2 >> 16) & 0xff);
      bx[3] = (byte) ((v2 >> 24) & 0xff);

    }

    if (byteOrder == BigEndian) {
      bx = reverseBytes(bx);
    }

    return bx;
  }

  private byte[] packRaw_u32b(long val) {
    byte[] bx = new byte[4];

    if (val >= 0) {
      bx[0] = (byte) (val & 0xff);
      bx[1] = (byte) ((val >> 8) & 0xff);
      bx[2] = (byte) ((val >> 16) & 0xff);
      bx[3] = (byte) ((val >> 24) & 0xff);

    }

    if (byteOrder == BigEndian) {
      bx = reverseBytes(bx);
    }
    return bx;
  }


  public byte[] pack_single_data(char fmt, long val) {
    byte[] bx;
    switch (fmt) {
      case 'h':
        short value = (short) (val & 0xffff);
        bx = packRaw_16b(value);
        break;

      case 'H':
        bx = packRaw_u16b((int) val);
        break;


      case 'i':
        int ival = (int) (val);
        bx = packRaw_32b(ival);
        break;

      case 'I':
        bx = packRaw_u32b(val);
        break;

      default:
        //do nothing
        System.out.println("Invalid format specifier");
        bx = null;
        break;

    }

    return bx;
  }


  public byte[] pack(String fmt, long val) throws Exception {
    if (fmt.length() > 2) {
      throw new Exception("Single values may not have multiple format specifiers");
    }

    byte[] bx = new byte[1];
    for (int i = 0; i < fmt.length(); i++) {
      char c = fmt.charAt(i);
      if ((i == 0) && ((c == '>') || (c == '<') || (c == '@') || (c == '!'))) {
        if (c == '>')
          byteOrder = BigEndian;
        else if (c == '<')
          byteOrder = LittleEndian;
        else if (c == '!')
          byteOrder = BigEndian;
        else // c must == '@'
          byteOrder = nativeByteOrder;
      } else if ((c != '>') && (c != '<') && (c != '@') && (c != '!')) {

        bx = pack_single_data(c, val);

        if (bx == null)
          throw new Exception("Invalid character specifier");


      }

    }
    return bx;
  }

  public byte[] pack(String fmt, long[] vals) throws Exception {
    char c0 = fmt.charAt(0);
    int len;
    if ((c0 == '@') || (c0 == '>') || (c0 == '<') || (c0 == '!')) {
      len = fmt.length() - 1;
    } else {
      len = fmt.length();
    }

    if (len != vals.length)
      throw new Exception("format length and values aren't equal");

    len = lenEst(fmt);

    byte[] bxx = new byte[0];
    byte[] bx;
    byte[] temp;

    for (int i = 0; i < fmt.length(); i++) {
      char c = fmt.charAt(i);
      if ((i == 0) && ((c == '>') || (c == '<') || (c == '@') || (c == '!'))) {
        if (c == '>')
          byteOrder = BigEndian;
        else if (c == '<')
          byteOrder = LittleEndian;
        else if (c == '!')
          byteOrder = BigEndian;
        else // c must == '@'
          byteOrder = nativeByteOrder;
      } else if ((c != '>') && (c != '<') && (c != '@') && (c != '!')) {
        if ((c0 == '@') || (c0 == '>') || (c0 == '<') || (c0 == '!')) {
          bx = pack(Character.toString(c), vals[i - 1]);
        } else {
          bx = pack(Character.toString(c), vals[i]);
        }
        temp = new byte[bxx.length + bx.length];
        System.arraycopy(bxx, 0, temp, 0, bxx.length);
        System.arraycopy(bx, 0, temp, bxx.length, bx.length);

        bxx = Arrays.copyOf(temp, temp.length);
      }
    }

    return bxx;
  }


  private long unpackRaw_16b(byte[] val) {
    if (byteOrder == LittleEndian)
      reverseBytes(val);

    long x;
    x = (val[0] << 8) | (val[1] & 0xff);
    if ((x >>> 15 & 1) == 1) {
      x = ((x ^ 0x7fff) & 0x7fff) + 1; //2's complement 16 bit
      x *= -1;
    }
    return x;
  }

  private long unpackRaw_u16b(byte[] val) {
    if (byteOrder == LittleEndian)
      reverseBytes(val);

    long x;
    x = ((val[0] & 0xff) << 8) | (val[1] & 0xff);
    return x;
  }

  private long unpackRaw_32b(byte[] val) {
    if (byteOrder == LittleEndian)
      reverseBytes(val);

    long x;
    x = (val[0] << 24) | (val[1] << 16) | (val[2] << 8) | (val[3]);
    if ((x >>> 31 & 1) == 1) {
      x = ((x ^ 0x7fffffff) & 0x7fffffff) + 1; //2's complement 32 bit
      x *= -1;
    }
    return x;
  }

  private long unpackRaw_u32b(byte[] val) {
    if (byteOrder == LittleEndian)
      reverseBytes(val);

    long x;
    x = (((long) (val[0] & 0xff)) << 24) | (((long) (val[1] & 0xff)) << 16) | (((long) (val[2] & 0xff)) << 8) | ((long) (val[3] & 0xff));
    return x;
  }

  public long unpack_single_data(char fmt, byte[] val) throws Exception {
    long var = 0;
    switch (fmt) {
      case 'h':
        if (val.length != 2)
          throw new Exception("Byte length mismatch");
        var = unpackRaw_16b(val);
        break;

      case 'H':
        if (val.length != 2)
          throw new Exception("Byte length mismatch");

        var = unpackRaw_u16b(val);
        break;

      case 'i':
        if (val.length != 4)
          throw new Exception("Byte length mismatch");

        var = unpackRaw_32b(val);
        break;

      case 'I':
        if (val.length != 4)
          throw new Exception("Byte length mismatch");
        var = unpackRaw_u32b(val);
        break;

      default:
        // do nothing;
        break;
    }

    return var;
  }


  private int lenEst(String fmt) {
    int counter = 0;
    char x = '\0';
    for (int i = 0; i < fmt.length(); i++) {
      x = fmt.charAt(i);
      if (x == 'i' || x == 'I')
        counter += 4;
      else if (x == 'h' || x == 'H')
        counter += 2;
    }
    return counter;
  }

  public long[] unpack(String fmt, byte[] vals) throws Exception {
    int len;
    len = lenEst(fmt);

    if (len != vals.length)
      throw new Exception("format length and values aren't equal");

    char c0 = fmt.charAt(0);

    long[] bxx;
    if (c0 == '@' || c0 == '<' || c0 == '>' || c0 == '!') {
      bxx = new long[fmt.length() - 1];
    } else {
      bxx = new long[fmt.length()];
    }
    char c;
    byte[] bShort = new byte[2];
    byte[] bLong = new byte[4];
    ByteArrayInputStream bs = new ByteArrayInputStream(vals);

    int p = 0;
    for (int i = 0; i < fmt.length(); i++) {
      c = fmt.charAt(i);
      if ((i == 0) && ((c == '>') || (c == '<') || (c == '@') || (c == '!'))) {
        if (c == '>')
          byteOrder = BigEndian;
        else if (c == '<')
          byteOrder = LittleEndian;
        else if (c == '!')
          byteOrder = BigEndian;
        else
          byteOrder = nativeByteOrder;
      } else {
        if ((c != '>') && (c != '<') && (c != '@') && (c != '!')) {
          if (c == 'h' || c == 'H') {
            int read = bs.read(bShort);
            bxx[p] = unpack_single_data(c, bShort);
          } else if (c == 'i' || c == 'I') {
            int read = bs.read(bLong);
            bxx[p] = unpack_single_data(c, bLong);
          }
          p++;
        }
      }

    }
    return bxx;
  }

  //  struct.pack(format, v1, v2, ...)
  @StarlarkMethod(name = "pack",
      doc = "Return a bytes object containing the values v1, v2, … packed according to the format" +
          " string format. The arguments must match the values required by the format exactly.\n",
      parameters = {
          @Param(
              name = "format",
              allowedTypes = {
                  @ParamType(type = String.class),
                  @ParamType(type = LarkyByteArray.class)
              }
          )
      },
      extraPositionals = @Param(name = "args"),
      useStarlarkThread = true)
  public LarkyByteArray struct__pack(Object format, Tuple args, StarlarkThread thread) throws Exception {
    Iterator<Object> iter = args.stream().iterator();
    ArrayList<Long> l = new ArrayList<>();
    while (iter.hasNext()) {
      final StarlarkInt thing1 = (StarlarkInt) iter.next();
      if (iter.hasNext()) { // don't forget this one
        final StarlarkInt thing2 = (StarlarkInt) iter.next();
        l.add(thing1.toLong("Not long?"));
        l.add(thing2.toLong("Not Long?"));
      }
    }

    byte[] bytes;
    if (String.class.isAssignableFrom(format.getClass())) {
      bytes = pack((String) format, l.stream().flatMapToLong(LongStream::of).toArray());
    } else {

      bytes = pack(((LarkyByteArray) format).getString(), l.stream().flatMapToLong(LongStream::of).toArray());
    }
    return new LarkyByteArray(thread, bytes);
  }

  //  struct.unpack(format, buffer)
  @StarlarkMethod(name = "unpack",
      doc = "Unpack from the buffer buffer (presumably packed by pack(format, ...)) according" +
          " to the format string format. The result is a tuple even if it contains exactly" +
          " one item. The buffer’s size in bytes must match the size required by the" +
          " format, as reflected by calcsize().",
      parameters = {
          @Param(
              name = "format",
              allowedTypes = {
                  @ParamType(type = String.class),
                  @ParamType(type = LarkyByteArray.class)
              }
          ),
          @Param(
              name = "buffer",
              allowedTypes = {@ParamType(type = LarkyByteArray.class)}
          )
      },
      useStarlarkThread = true)
  public Tuple struct__unpack(Object format, LarkyByteArray buffer, StarlarkThread thread) throws Exception {
    long[] unpacked;
    if (String.class.isAssignableFrom(format.getClass())) {
      unpacked = unpack((String) format, buffer.toBytes());
    }
    else {
      unpacked = unpack(((LarkyByteArray) format).getString(), buffer.toBytes());
    }
    return Tuple.copyOf(LongStream.of(unpacked).mapToObj(StarlarkInt::of).collect(Collectors.toList()));
  }

  //  struct.pack_into(format, buffer, offset, v1, v2, ...)
  @StarlarkMethod(name = "pack_into",
      doc = "Pack the values v1, v2, … according to the format string format and write the " +
          "packed bytes into the writable buffer buffer starting at position offset. " +
          "Note that offset is a required argument.",
      parameters = {
          @Param(
              name = "format",
              allowedTypes = {@ParamType(type = String.class)}
          ),
          @Param(
              name = "buffer",
              allowedTypes = {@ParamType(type = LarkyByteArray.class)}
          ),
          @Param(
              name = "offset",
              allowedTypes = {@ParamType(type = StarlarkInt.class)}
          )
      },
      extraPositionals = @Param(name = "args"),
      useStarlarkThread = true)
  public LarkyByteArray struct__pack_into(String format, LarkyByteArray buffer, StarlarkInt offset, Tuple args, StarlarkThread thread) throws EvalException {
    return null;
  }

  // struct.unpack_from(format, /, buffer, offset=0)
  //
  @StarlarkMethod(name = "unpack_from",
      doc = "Unpack from buffer starting at position offset, according to the format string " +
          "format. The result is a tuple even if it contains exactly one item. The buffer's size " +
          "in bytes, starting at position offset, must be at least the size required by the " +
          "format, as reflected by calcsize().",
      parameters = {
          @Param(
              name = "format",
              allowedTypes = {@ParamType(type = String.class)}
          ),
          @Param(
              name = "buffer",
              allowedTypes = {@ParamType(type = LarkyByteArray.class)}
          ),
          @Param(
              name = "offset",
              allowedTypes = {@ParamType(type = StarlarkInt.class)},
              defaultValue = "0"
          )
      },
      useStarlarkThread = true)
  public Tuple struct__unpack_from(String format, LarkyByteArray buffer, StarlarkInt offset, StarlarkThread thread) throws Exception {
    long[] unpacked = unpack(
        format,
        TextUtil.subarray(buffer.toBytes(), offset.toIntUnchecked(), buffer.size())
    );
    return Tuple.copyOf(LongStream.of(unpacked)
        .mapToObj(StarlarkInt::of)
        .collect(Collectors.toList())
    );
  }

  // struct.iter_unpack(format, buffer)
  @StarlarkMethod(name = "iter_unpack",
      doc = " Iteratively unpack from the buffer according to the format string format. " +
          "This function returns an iterator which will read equally-sized chunks from the " +
          "buffer until all its contents have been consumed. The buffer’s size in bytes must " +
          "be a multiple of the size required by the format, as reflected by calcsize()." +
          "" +
          "Each iteration yields a tuple as specified by the format string.",
      parameters = {
          @Param(
              name = "format",
              allowedTypes = {@ParamType(type = String.class)}
          ),
          @Param(
              name = "buffer",
              allowedTypes = {@ParamType(type = LarkyByteArray.class)}
          ),
      },
      useStarlarkThread = true)
  public Tuple struct__iter_unpack(String format, LarkyByteArray buffer, StarlarkThread thread) throws Exception {
    return struct__unpack(format, buffer, thread); // no iterators in starlarky.
  }

  // struct.calcsize(format)
  @StarlarkMethod(name = "calcsize",
      doc = "Return the size of the struct (and hence of the bytes object produced by" +
          " pack(format, ...)) corresponding to the format string format.",
      parameters = {
          @Param(
              name = "format",
              allowedTypes = {@ParamType(type = String.class)}
          )
      }
  )
  public StarlarkInt struct__calcsize(String format) throws EvalException {
    return StarlarkInt.of(lenEst(format));
  }

}
