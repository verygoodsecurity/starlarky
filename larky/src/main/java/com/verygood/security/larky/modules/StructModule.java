package com.verygood.security.larky.modules;

import java.io.ByteArrayInputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.verygood.security.larky.modules.codecs.TextUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;
import net.starlark.java.eval.Tuple;


@StarlarkBuiltin(
    name = "jstruct",
    category = "BUILTIN",
    doc = "This module performs conversions between Larky values and C structs represented as " +
        "Larky bytes objects. This can be used in handling binary data stored in files or from " +
        "network connections, among other sources. It uses Format Strings as compact " +
        "descriptions of the layout of the C structs and the intended conversion to/from " +
        "Larky values.\n" +
        "\n" +
        "Note: By default, the result of packing a given C struct includes pad bytes in order " +
        "to maintain proper alignment for the C types involved; similarly, alignment is taken " +
        "into account when unpacking. This behavior is chosen so that the bytes of a packed " +
        "struct correspond exactly to the layout in memory of the corresponding C struct. " +
        "To handle platform-independent data formats or omit implicit pad bytes, use " +
        "standard size and alignment instead of native size and alignment: see " +
        "Byte Order, Size, and Alignment for details." +
        "\n" +
        "Several struct functions (and methods of Struct) take a buffer argument. This refers " +
        "to objects that implement the Buffer Protocol and provide either a " +
        "readable or read-writable buffer. The most common types used for that purpose " +
        "are bytes and bytearray, but many other types that can be viewed as an " +
        "array of bytes implement the buffer protocol, so that they can be read/filled " +
        "without additional copying from a bytes object." +
        "\n" +
        "This module is a port of: https://docs.python.org/3/library/struct.html"
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

  private byte[] packRaw_8b(byte val)  {
    byte[] bx = new byte[1];
    if(val >= 0) {
      bx[0] = (byte) (val & 0xff);
    } else {
      int v2 = Byte.toUnsignedInt(val);
      bx[0] = (byte) (v2 & 0xff);
    }
    return bx;
  }

  private byte[] packRaw_u8b(byte val)  {
     byte[] bx = new byte[1];
     bx[0] = (byte) (val & 0xff);
     return bx;
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

  private byte[] packRaw_64b(long val) {
    byte[] bx = new byte[8];

    if (val >= 0) {
      bx[0] = (byte) (val & 0xff);
      bx[1] = (byte) ((val >> 8) & 0xff);
      bx[2] = (byte) ((val >> 16) & 0xff);
      bx[3] = (byte) ((val >> 24) & 0xff);
      bx[4] = (byte) ((val >> 32) & 0xff);
      bx[5] = (byte) ((val >> 40) & 0xff);
      bx[6] = (byte) ((val >> 48) & 0xff);
      bx[7] = (byte) ((val >> 56) & 0xff);
    }
    else {
      long v2 = Math.abs(val);
      v2 = (v2 ^ Long.MAX_VALUE) + 1; // invert bits and add 1
      v2 = v2 | (1L << 63); // add the 63rd bit as negative bit
      bx[0] = (byte) (v2 & 0xff);
      bx[1] = (byte) ((v2 >> 8) & 0xff);
      bx[2] = (byte) ((v2 >> 16) & 0xff);
      bx[3] = (byte) ((v2 >> 24) & 0xff);
      bx[4] = (byte) ((v2 >> 32) & 0xff);
      bx[5] = (byte) ((v2 >> 40) & 0xff);
      bx[6] = (byte) ((v2 >> 48) & 0xff);
      bx[7] = (byte) ((v2 >> 56) & 0xff);
    }

    if (byteOrder == BigEndian) {
      bx = reverseBytes(bx);
    }
    return bx;
  }

  private byte[] packRaw_u64b(long val) {
    byte[] bx = new byte[8];

    if (val >= 0) {
      bx[0] = (byte) (val & 0xff);
      bx[1] = (byte) ((val >> 8) & 0xff);
      bx[2] = (byte) ((val >> 16) & 0xff);
      bx[3] = (byte) ((val >> 24) & 0xff);
      bx[4] = (byte) ((val >> 32) & 0xff);
      bx[5] = (byte) ((val >> 40) & 0xff);
      bx[6] = (byte) ((val >> 48) & 0xff);
      bx[7] = (byte) ((val >> 56) & 0xff);
    }

    if (byteOrder == BigEndian) {
      bx = reverseBytes(bx);
    }
    return bx;
  }

  public byte[] pack_single_data(char fmt, long val) {
    byte[] bx;
    switch (fmt) {
      case 'b':
        byte bv = (byte) (val & 0xff);
        bx = packRaw_8b(bv);
        break;
      case 'B':
        bx = packRaw_u8b((byte) val);
        break;

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

      case 'q':
        bx = packRaw_64b(val);
        break;

      case 'Q':
        bx = packRaw_u64b(val);
        break;

      default:
        //do nothing
        System.out.println("Invalid format specifier: " + fmt);
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

  private byte unpackRaw_8b(byte[] val) {
    return (byte) (val[0] & 0xff);
  }

  private long unpackRaw_u8b(byte[] val) {
    return (byte) (val[0] & 0xff);
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

  private long unpackRaw_64b(byte[] val) {
    if (byteOrder == LittleEndian)
      reverseBytes(val);

    long x;
    //@formatter:off
    x =   ((long) val[0] << 56)
        | ((long) val[1] << 48)
        | ((long) val[2] << 40)
        | ((long) val[3] << 32)
        | ((long) val[4] << 24)
        | ((long) val[5] << 16)
        | ((long) val[6] << 8)
        | ((long) val[7]);
    //@formatter:on
    if ((x >>> 63 & 1) == 1) {
      x = ((x ^ Long.MAX_VALUE) & Long.MAX_VALUE) + 1; //2's complement 64 bit
      x *= -1;
    }
    return x;
  }

  private long unpackRaw_u64b(byte[] val) {
    if (byteOrder == LittleEndian)
      reverseBytes(val);
    //@formatter:off
    long x;
    x =   (((long) (val[0] & 0xff)) << 56)
        | (((long) (val[1] & 0xff)) << 48)
        | (((long) (val[2] & 0xff)) << 40)
        | (((long) (val[3] & 0xff)) << 32)
        | (((long) (val[4] & 0xff)) << 24)
        | (((long) (val[5] & 0xff)) << 16)
        | (((long) (val[6] & 0xff)) << 8)
        | ((long) (val[7] & 0xff));
    return x;
    //@formatter:on
  }

  public long unpack_single_data(char fmt, byte[] val) throws Exception {
    long var = 0;
    switch (fmt) {
      case 'b':
        if (val.length != 1)
          throw new Exception("Byte length mismatch");
        var = unpackRaw_8b(val);
        break;
      case 'B':
        if (val.length != 1)
          throw new Exception("Byte length mismatch");
        var = unpackRaw_u8b(val);
        break;

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

      case 'q':
        if (val.length != 8)
         throw new Exception("Byte length mismatch");
        var = unpackRaw_64b(val);
        break;

      case 'Q':
        if (val.length != 8)
         throw new Exception("Byte length mismatch");
        var = unpackRaw_u64b(val);
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
      switch (x) {
        case 'b':
        case 'B':
          counter += 1;
          break;
        case 'h':
        case 'H':
          counter += 2;
          break;
        case 'i':
        case 'I':
          counter += 4;
          break;
        case 'q':
        case 'Q':
          counter += 8;
          break;
      }
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
                  @ParamType(type = StarlarkBytes.class)
              }
          )
      },
      extraPositionals = @Param(name = "args"),
      useStarlarkThread = true)
  public StarlarkBytes struct__pack(Object format, Tuple args, StarlarkThread thread) throws Exception {
    ArrayList<Long> l = asList(args);
    byte[] bytes;
    if (String.class.isAssignableFrom(format.getClass())) {
      bytes = pack((String) format, l.stream().flatMapToLong(LongStream::of).toArray());
    } else {

      bytes = pack(Starlark.str(format), l.stream().flatMapToLong(LongStream::of).toArray());
    }
    return StarlarkBytes.of(thread.mutability(), bytes);
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
                  @ParamType(type = StarlarkBytes.class)
              }
          ),
          @Param(
              name = "buffer",
              allowedTypes = {@ParamType(type = StarlarkBytes.class)}
          )
      },
      useStarlarkThread = true)
  public Tuple struct__unpack(Object format, StarlarkBytes buffer, StarlarkThread thread) throws Exception {
    long[] unpacked;
    if (String.class.isAssignableFrom(format.getClass())) {
      unpacked = unpack((String) format, buffer.toByteArray());
    }
    else {
      unpacked = unpack(Starlark.str(format), buffer.toByteArray());
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
            allowedTypes = {@ParamType(type = StarlarkBytes.class), @ParamType(type = StarlarkBytes.StarlarkByteArray.class)}
          ),
          @Param(
              name = "offset",
              allowedTypes = {@ParamType(type = StarlarkInt.class)}
          )
      },
      extraPositionals = @Param(name = "args"),
      useStarlarkThread = true)
  public StarlarkBytes struct__pack_into(Object format, StarlarkBytes buffer, StarlarkInt offset, Tuple args, StarlarkThread thread) throws Exception {
    ArrayList<Long> l = asList(args);
    byte[] bytes;
    if (String.class.isAssignableFrom(format.getClass())) {
      bytes = pack((String) format, l.stream().skip(offset.toIntUnchecked()).flatMapToLong(LongStream::of).toArray());
    } else {

      bytes = pack(Starlark.str(format), l.stream().skip(offset.toIntUnchecked()).flatMapToLong(LongStream::of).toArray());
    }

    bytes = TextUtil.concatByteArray(
        TextUtil.subarray(buffer.toByteArray(), 0, offset.toIntUnchecked()),
        bytes);
    if(buffer instanceof StarlarkBytes.StarlarkByteArray) {
      ((StarlarkBytes.StarlarkByteArray) buffer).replaceAll(StarlarkBytes.of(thread.mutability(), bytes));
    }
    return StarlarkBytes.of(thread.mutability(), bytes);
  }

  private ArrayList<Long> asList(Tuple args) throws EvalException {
    Iterator<Object> iter = args.stream().iterator();
    ArrayList<Long> l = new ArrayList<>();
    while (iter.hasNext()) {
      final StarlarkInt thing1 = (StarlarkInt) iter.next();
      l.add(thing1.toLong("Not long?"));
      if (iter.hasNext()) { // don't forget this one
        final StarlarkInt thing2 = (StarlarkInt) iter.next();
        l.add(thing2.toLong("Not Long?"));
      }
    }
    return l;
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
              allowedTypes = {
                  @ParamType(type = String.class),
                  @ParamType(type = StarlarkBytes.class)}
          ),
          @Param(
              name = "buffer",
              allowedTypes = {@ParamType(type = StarlarkBytes.class)}
          ),
          @Param(
              name = "offset",
              allowedTypes = {@ParamType(type = StarlarkInt.class)},
              defaultValue = "0"
          )
      },
      useStarlarkThread = true)
  public Tuple struct__unpack_from(Object format, StarlarkBytes buffer, StarlarkInt offset, StarlarkThread thread) throws Exception {
    long[] unpacked;
    if (String.class.isAssignableFrom(format.getClass())) {
      unpacked = unpack(
          (String) format,
          TextUtil.subarray(buffer.toByteArray(), offset.toIntUnchecked(), buffer.size()));
    }
    else {
      unpacked = unpack(
          Starlark.str(format),
          TextUtil.subarray(buffer.toByteArray(), offset.toIntUnchecked(), buffer.size()));
    }
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
              allowedTypes = {
                  @ParamType(type = String.class),
                  @ParamType(type = StarlarkBytes.class)}
          ),
          @Param(
              name = "buffer",
              allowedTypes = {@ParamType(type = StarlarkBytes.class)}
          ),
      },
      useStarlarkThread = true)
  public Tuple struct__iter_unpack(Object format, StarlarkBytes buffer, StarlarkThread thread) throws Exception {
    return struct__unpack(format, buffer, thread); // no iterators in starlarky.
  }

  // struct.calcsize(format)
  @StarlarkMethod(name = "calcsize",
      doc = "Return the size of the struct (and hence of the bytes object produced by" +
          " pack(format, ...)) corresponding to the format string format.",
      parameters = {
          @Param(
              name = "format",
              allowedTypes = {
                  @ParamType(type = String.class),
                  @ParamType(type = StarlarkBytes.class)}
          )
      }
  )
  public StarlarkInt struct__calcsize(Object format) throws EvalException {
    if (String.class.isAssignableFrom(format.getClass())) {
      return StarlarkInt.of(lenEst((String) format));
    }
    else {
      return StarlarkInt.of(lenEst(Starlark.str(format)));
    }
  }

}
