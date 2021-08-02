package com.verygood.security.larky.modules;

import static net.starlark.java.eval.StarlarkBytes.StarlarkByteArray;

import java.math.BigInteger;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.verygood.security.larky.modules.utils.ByteArrayUtil;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkInt;
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
  // @formatter:off
  /**
   *
   * Format characters have the following meaning; the conversion between C and Python values should
   * be obvious given their types.
   *
   * The 'Standard size' column refers to the size of the packed value in bytes when using standard
   * size; that is, when the format string starts with one of ``'<'``, ``'>'``, ``'!'`` or ``'='``.
   *
   * When using native size, the size of the packed value is platform-dependent.
   *
   * +--------+--------------------------+--------------------+----------------+
   * | Format | C Type                   | Python type        | Standard size  |
   * +========+==========================+====================+================+
   * | ``x``  | pad byte                 | no value           |                |
   * +--------+--------------------------+--------------------+----------------+
   * | ``c``  | char                     | bytes of length 1  | 1              |
   * +--------+--------------------------+--------------------+----------------+
   * | ``b``  | signed char              | integer            | 1              |
   * +--------+--------------------------+--------------------+----------------+
   * | ``B``  | unsigned char            | integer            | 1              |
   * +--------+--------------------------+--------------------+----------------+
   * | ``?``  | _Bool                    | bool               | 1              |
   * +--------+--------------------------+--------------------+----------------+
   * | ``h``  | short                    | integer            | 2              |
   * +--------+--------------------------+--------------------+----------------+
   * | ``H``  | unsigned short           | integer            | 2              |
   * +--------+--------------------------+--------------------+----------------+
   * | ``i``  | int                      | integer            | 4              |
   * +--------+--------------------------+--------------------+----------------+
   * | ``I``  | unsigned int             | integer            | 4              |
   * +--------+--------------------------+--------------------+----------------+
   * | ``l``  | long                     | integer            | 4              |
   * +--------+--------------------------+--------------------+----------------+
   * | ``L``  | unsigned long            | integer            | 4              |
   * +--------+--------------------------+--------------------+----------------+
   * | ``q``  | long long                | integer            | 8              |
   * +--------+--------------------------+--------------------+----------------+
   * | ``Q``  | unsigned long            | integer            | 8              |
   * |        | long                     |                    |                |
   * +--------+--------------------------+--------------------+----------------+
   * | ``n``  | ssize_t                  | integer            |                |
   * +--------+--------------------------+--------------------+----------------+
   * | ``N``  | size_t                   | integer            |                |
   * +--------+--------------------------+--------------------+----------------+
   * | ``e``  | IEEE754 binary16         | float              | 2              |
   * +--------+--------------------------+--------------------+----------------+
   * | ``f``  | float                    | float              | 4              |
   * +--------+--------------------------+--------------------+----------------+
   * | ``d``  | double                   | float              | 8              |
   * +--------+--------------------------+--------------------+----------------+
   * | ``s``  | char[]                   | bytes              |                |
   * +--------+--------------------------+--------------------+----------------+
   * | ``p``  | char[] (PASCAL STRING)   | bytes              |                |
   * +--------+--------------------------+--------------------+----------------+
   * | ``P``  | void *                   | integer            |                |
   * +--------+--------------------------+--------------------+----------------+
   */
  // @formatter:on
  private static final FormatDef[] littleEndian = {
    new PadFormatDef().init('x', 1, 0),
    new BooleanFormatDef().init('?', 1, 0),
    new CharFormatDef().init('c', 1, 0),
    new ByteFormatDef().init('b', 1, 0),
    new UnsignedByteFormatDef().init('B', 1, 0),
    new StringFormatDef().init('s', 1, 0),
    new PascalStringFormatDef().init('p', 1, 0),
    new LEShortFormatDef().init('h', 2, 0),
    new LEUnsignedShortFormatDef().init('H', 2, 0),
    new LEIntFormatDef().init('i', 4, 0),
    new LEUnsignedIntFormatDef().init('I', 4, 0),
    new LEIntFormatDef().init('l', 4, 0),
    new LEUnsignedIntFormatDef().init('L', 4, 0),
    new LELongFormatDef().init('q', 8, 0),
    new LEUnsignedLongFormatDef().init('Q', 8, 0),
    new LEHalfFloatFormatDef().init('e',2,0),
    new LEFloatFormatDef().init('f', 4, 0),
    new LEDoubleFormatDef().init('d', 8, 0),
  };

  private static final FormatDef[] bigendianTable = {
    new PadFormatDef().init('x', 1, 0),
    new ByteFormatDef().init('b', 1, 0),
    new UnsignedByteFormatDef().init('B', 1, 0),
    new BooleanFormatDef().init('?', 1, 0),
    new CharFormatDef().init('c', 1, 0),
    new StringFormatDef().init('s', 1, 0),
    new PascalStringFormatDef().init('p', 1, 0),
    new BEShortFormatDef().init('h', 2, 0),
    new BEUnsignedShortFormatDef().init('H', 2, 0),
    new BEIntFormatDef().init('i', 4, 0),
    new BEUnsignedIntFormatDef().init('I', 4, 0),
    new BEIntFormatDef().init('l', 4, 0),
    new BEUnsignedIntFormatDef().init('L', 4, 0),
    new BELongFormatDef().init('q', 8, 0),
    new BEUnsignedLongFormatDef().init('Q', 8, 0),
    new BEHalfFloatFormatDef().init('e',2,0),
    new BEFloatFormatDef().init('f', 4, 0),
    new BEDoubleFormatDef().init('d', 8, 0),
  };

  private static final FormatDef[] nativeTable = {
    new PadFormatDef().init('x', 1, 0),
    new ByteFormatDef().init('b', 1, 0),
    new UnsignedByteFormatDef().init('B', 1, 0),
    new BooleanFormatDef().init('?', 1, 0),
    new CharFormatDef().init('c', 1, 0),
    new StringFormatDef().init('s', 1, 0),
    new PascalStringFormatDef().init('p', 1, 0),
    new LEShortFormatDef().init('h', 2, 2),
    new LEUnsignedShortFormatDef().init('H', 2, 2),
    new LEIntFormatDef().init('i', 4, 4),
    new LEUnsignedIntFormatDef().init('I', 4, 4),
    new LEIntFormatDef().init('l', 8, 8),
    new LEUnsignedIntFormatDef().init('L', 8, 8),
    new LELongFormatDef().init('q', 8, 8),
    new LEUnsignedLongFormatDef().init('Q', 8, 8),
    new LELongFormatDef().init('n', 8, 8),
    new LEUnsignedLongFormatDef().init('N', 8, 8),
    new LEHalfFloatFormatDef().init('e',2,2),
    new LEFloatFormatDef().init('f', 4, 4),
    new LEDoubleFormatDef().init('d', 8, 8),
    new PointerFormatDef().init('P'),
  };

  // ❯❯❯ ./a.out
  // sizeof(char): 1
  // bool: 1 => sizeof(st_bool): 2
  // short: 2 => sizeof(st_short): 4
  // int: 4 => sizeof(st_int): 8
  // float: 4 => sizeof(st_float): 8
  // double: 8 => sizeof(st_double): 16
  // voidp: 8 => sizeof(st_void_p): 16
  // size_t: 8 => sizeof(st_size_t): 16
  // long: 8 => sizeof(st_long): 16
  // longlong: 8 => sizeof(s_long_long): 16

  static ByteStream pack(String format, FormatDef[] f, int size, int start, Tuple args) throws EvalException {
    ByteStream res = new ByteStream();

    int i = start;
    int len = format.length();
    for (int j = 0; j < len; j++) {
      char c = format.charAt(j);
      if (j == 0 && (c == '@' || c == '<' || c == '>' || c == '=' || c == '!'))
        continue;
      if (Character.isWhitespace(c))
        continue;
      int num = 1;
      if (Character.isDigit(c)) {
        num = Character.digit(c, 10);
        while (++j < len && Character.isDigit((c = format.charAt(j))))
          num = num * 10 + Character.digit(c, 10);
        if (Character.isDigit(c)) {
          throw Starlark.errorf("repeat count given without format specifier");
        }
        if (j >= len) {
          break;
        }
      }

      FormatDef e = getentry(c, f);

      // Fill pad bytes with zeros
      int nres = align(res.position(), e) - res.position();
      while (nres-- > 0)
        res.writeByte(0);
      i += e.doPack(res, num, i, args);
    }

    if (i < args.size()) {
      throw Starlark.errorf("expected %d items for packing (got %d)", i, args.size());
    }

    return res;
  }

  // @formatter:off
  /**
   *
   *  +-----------+------------------------+----------+-----------+
   *  | Character | Byte order             | Size     | Alignment |
   *  +===========+========================+==========+===========+
   *  | ``@``     | native                 | native   | native    |
   *  +-----------+------------------------+----------+-----------+
   *  | ``=``     | native                 | standard | none      |
   *  +-----------+------------------------+----------+-----------+
   *  | ``<``     | little-endian          | standard | none      |
   *  +-----------+------------------------+----------+-----------+
   *  | ``>``     | big-endian             | standard | none      |
   *  +-----------+------------------------+----------+-----------+
   *  | ``!``     | network (= big-endian) | standard | none      |
   *  +-----------+------------------------+----------+-----------+
   *
   * @param formatString the format string
   * @return a {@code FormatDef} table containing the conversions for the byte-order prefix
   */
  // @formatter:on
  static FormatDef[] whichtable(String formatString) {
    char c = formatString.charAt(0);
    switch (c) {
      case '>':
      case '!':
        // Network byte order is big-endian
        return bigendianTable;
      case '<':
        return littleEndian;
      case '=': /* Host byte order -- different from native in alignment! */
        return ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? littleEndian : bigendianTable;
      case '@':
      default:
        return nativeTable;

    }
  }

  private static FormatDef getentry(char c, FormatDef[] f) throws EvalException {
    for (FormatDef formatDef : f) {
      if (formatDef.name == c) {
        return formatDef;
      }
    }
    String msg = "bad char '%c' in struct format";
    if( c == '\0') {
      msg = "embedded null character - " + msg;
    }
    throw Starlark.errorf(msg, c);
  }

  /**
   * Align a size according to a format code.  Return -1 on overflow.
   *
   * Copied from: https://github.com/python/cpython/blob/6b61d74a3b/Modules/_struct.c#L1272
   *
   * @param size The size to align
   * @param e    The {@link FormatDef} that defines the alignment
   * @return The aligned size according to {@link FormatDef} or -1 on overflow.
   */
  private static int align(int size, FormatDef e) {
    int extra;
    if (e.alignment != 0 && size > 0) {
      extra = (e.alignment - 1) - (size - 1) % (e.alignment);
      if (extra > Integer.MAX_VALUE - size)
          return -1;
      size += extra;
    }
    return size;
  }

  static int calcsize(String format, FormatDef[] f) throws EvalException {
    int size = 0;

    int len = format.length();
    for (int j = 0; j < len; j++) {
      char c = format.charAt(j);
      if (j == 0 && (c == '@' || c == '<' || c == '>' || c == '=' || c == '!')) {
        continue;
      }
      if (Character.isWhitespace(c)) {
        continue;
      }
      int num = 1;
      if (Character.isDigit(c)) {
        num = Character.digit(c, 10);
        while (++j < len && Character.isDigit((c = format.charAt(j)))) {
          int x = num * 10 + Character.digit(c, 10);
          if (x / 10 != num) {
            throw new UnsupportedOperationException("overflow in item count");
          }
          num = x;
        }
        if (j >= len) {
          break;
        }
      }

      FormatDef e = getentry(c, f);

      int itemsize = e.size;
      size = align(size, e);
      int x = num * itemsize;
      size += x;
      if (x / itemsize != num || size < 0) {
        throw new UnsupportedOperationException("total struct size too long");
      }
    }
    return size;
  }

  /**
   * Return the size of the struct (and hence of the string) corresponding to the given format.
   */
  static public int calcsize(String format) throws EvalException {
    FormatDef[] f = whichtable(format);
    return calcsize(format, f);
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
    extraPositionals = @Param(name = "args"))
  public StarlarkBytes struct__pack(Object format, Tuple args) throws Exception {
    String strFmt;
    if (String.class.isAssignableFrom(format.getClass())) {
      strFmt = (String) format;
    } else {
      strFmt = Starlark.str(format);
    }
    final FormatDef[] whichtable = whichtable(strFmt);
    final ByteStream packed = pack(strFmt, whichtable, calcsize(strFmt, whichtable), 0, args);
    return packed.toStarlarkBytes();
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
    })
  public Tuple struct__unpack(Object format, StarlarkBytes buffer) throws Exception {
    String strFmt;

    if (String.class.isAssignableFrom(format.getClass())) {
      strFmt = (String) format;
    } else {
      strFmt = Starlark.str(format);
    }
    final FormatDef[] whichtable = whichtable(strFmt);
    return unpack(whichtable, calcsize(strFmt, whichtable), strFmt, new ByteStream(buffer.toByteArray()));
  }

  private Tuple unpack(FormatDef[] f, int size, String format, ByteStream str) throws EvalException {
    List<Object> res = new ArrayList<>();
    int flen = format.length();
    for (int j = 0; j < flen; j++) {
      char c = format.charAt(j);
      if (j == 0 && (c == '@' || c == '<' || c == '>' || c == '=' || c == '!')) {
        continue;
      }
      if (Character.isWhitespace(c)) {
        continue;
      }
      int num = 1;
      if (Character.isDigit(c)) {
        num = Character.digit(c, 10);
        while (++j < flen && Character.isDigit((c = format.charAt(j)))) {
          num = num * 10 + Character.digit(c, 10);
        }
        if (j > flen) {
          break;
        }
      }

      FormatDef e = getentry(c, f);
      str.skip(align(str.position(), e) - str.position());
      if (size != str.size()) {
        throw Starlark.errorf("unpack requires a buffer of %d bytes - buffer size: %d", size, str.size());
      }
      e.doUnpack(str, num, res);
    }

    Object[] siArr = new Object[res.size()];
    for (int i = 0, loopLength = res.size(); i < loopLength; i++) {
      Object unpack = res.get(i);
      if(unpack instanceof Character) {
        siArr[i] = StarlarkBytes.immutableOf((Character) unpack);
      } else if(unpack instanceof String) {
        siArr[i] =  StarlarkBytes.immutableOf(((String)unpack).getBytes(StandardCharsets.UTF_8));
      }
      else if(unpack instanceof Integer) {
        siArr[i] = StarlarkInt.of((Integer) unpack);
      } else if(unpack instanceof Long) {
        siArr[i] = StarlarkInt.of((Long) unpack);
      } else if(unpack instanceof BigInteger) {
        siArr[i] = StarlarkInt.of(Long.parseUnsignedLong(unpack.toString()));
      } else if(unpack instanceof Boolean) {
        siArr[i] = (Boolean) unpack ? Boolean.TRUE : Boolean.FALSE;
      } else if(unpack instanceof Float || unpack instanceof Double) {
        siArr[i] = StarlarkFloat.of(((Number) unpack).doubleValue());
      }

      else {
        siArr[i] = StarlarkInt.of(Long.parseLong(unpack.toString()));
      }
    }
    return Tuple.of(siArr);
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
        allowedTypes = {@ParamType(type = StarlarkByteArray.class)}
      ),
      @Param(
        name = "offset",
        allowedTypes = {@ParamType(type = StarlarkInt.class)}
      )
    },
    extraPositionals = @Param(name = "args")
  )
  public void struct__pack_into(Object format, StarlarkByteArray buffer, StarlarkInt offset, Tuple args) throws Exception {
    byte[] bytes;
    String strFmt;
    if (String.class.isAssignableFrom(format.getClass())) {
      strFmt = (String) format;
    } else {
      strFmt = Starlark.str(format);
    }
    final FormatDef[] whichtable = whichtable(strFmt);
    int size = calcsize(strFmt, whichtable);
    int offzet = offset.toIntUnchecked();
    int bufferSize = buffer.size();

    /* Support negative offsets. */
     if (offzet < 0) {
          /* Check that negative offset is low enough to fit data */
         if (offzet + size > 0) {
             throw Starlark.errorf(
                          "no space to pack %d bytes at offset %d",
                          size,
                          offzet);
         }

         /* Check that negative offset is not crossing buffer boundary */
         if (offzet + bufferSize < 0) {
           throw Starlark.errorf(
             "offset %d out of range for %d-byte buffer",
             offzet,
             bufferSize);
         }
     }

    if ((bufferSize - offzet) < size) {
      throw Starlark.errorf(
        "pack_into requires a buffer of at least %d bytes " +
          "for packing %d bytes at offset %d (actual buffer size is %d)",
        size + offzet,
        size,
        offzet,
        bufferSize
      );
    }
    final ByteStream packed = pack(strFmt, whichtable, size, 0, args);
    bytes = packed.toStarlarkBytes().toByteArray();
    for (int i = 0, inputLength = bytes.length; i < inputLength; i++) {
      buffer.set(offzet + i, bytes[i]);
    }

  }

  static private Number[] asList(Object elem) throws EvalException {
    if (elem instanceof StarlarkInt) {
      StarlarkInt elemz = ((StarlarkInt) elem);
      if (elemz.signum() != -1) {
        return new Long[]{Long.parseUnsignedLong(Starlark.str(elem))};

      } else {
        return new Long[]{elemz.toLong(elemz.toString())};
      }

    } else if (elem instanceof StarlarkBytes) {
      StarlarkBytes elemz = ((StarlarkBytes) elem);
      final int loopLength = elemz.size();
      final Byte[] bytes = new Byte[loopLength];
      for (int k = 0; k < loopLength; k++) {
        bytes[k] = elemz.byteAt(k);
      }
      return bytes;
    }  else if (elem instanceof StarlarkFloat) {
      StarlarkFloat elemz = ((StarlarkFloat) elem);
      return new Double[]{elemz.toDouble()};
    } else {
      throw Starlark.errorf(
        "got element of type %s, want %s",
        Starlark.type(elem),
        "integer, bytes, boolean, float, or a sequence (for truthiness)");
    }
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
    })
  public Tuple struct__unpack_from(Object format, StarlarkBytes buffer, StarlarkInt offset) throws Exception {
    String strFmt;

    if (String.class.isAssignableFrom(format.getClass())) {
      strFmt = (String) format;
    } else {
      strFmt = Starlark.str(format);
    }
    final FormatDef[] whichtable = whichtable(strFmt);
    int size = calcsize(strFmt, whichtable);
    int offzet = offset.toIntUnchecked();
    int bufferSize = buffer.size();

    /* Support negative offsets. */
     if (offzet < 0) {
          /* Check that negative offset is low enough to fit data */
         if (offzet + size > 0) {
             throw Starlark.errorf(
               "not enough data to unpack %d bytes at offset %d",
               size,
               offzet);
         }

         /* Check that negative offset is not crossing buffer boundary */
         if (offzet + bufferSize < 0) {
           throw Starlark.errorf(
             "offset %d out of range for %d-byte buffer",
             offzet,
             bufferSize);
         }
     }

    if ((bufferSize - offzet) < size) {
      throw Starlark.errorf(
        "unpack_from requires a buffer of at least %d bytes " +
          "for unpacking %d bytes at offset %d (actual buffer size is %d)",
        size + offzet,
        size,
        offzet,
        bufferSize
        );
    }
    return unpack(
      whichtable,
      size,
      strFmt,
      new ByteStream(Arrays.copyOfRange(buffer.toByteArray(), offzet, offzet + size))
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
    })
  public Tuple struct__iter_unpack(Object format, StarlarkBytes buffer) throws Exception {
    return struct__unpack(format, buffer); // no generators in larky, but there are iterators.
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
    final String f;
    if (String.class.isAssignableFrom(format.getClass())) {
      f = (String) format;
    } else {
      f = Starlark.str(format);
    }
    final int x = calcsize(f);
    return StarlarkInt.of(x);
  }

  static class ByteStream {
    byte[] data;
    int pos;

    ByteStream() {
      data = new byte[10];
      pos = 0;
    }

    ByteStream(byte[] bytes) {
      data = bytes;
      pos = 0;
    }

    int readByte() {
      return data[pos++] & 0xFF;
    }

    void read(byte[] buf, int pos, int len) {
      System.arraycopy(data, this.pos, buf, pos, len);
      this.pos += len;
    }


    String readString(int l) {
      byte[] data = new byte[l];
      read(data, 0, l);
      return new String(data);
    }


    private void ensureCapacity(int l) {
      if (pos + l >= data.length) {
        byte[] b = new byte[(pos + l) * 2];
        System.arraycopy(data, 0, b, 0, pos);
        data = b;
      }
    }


    void writeByte(int b) {
      ensureCapacity(1);
      data[pos++] = (byte) (b & 0xFF);
    }


    void write(char[] buf, int pos, int len) {
      ensureCapacity(len);
      System.arraycopy(ByteArrayUtil.charsToBytes(buf), pos, data, this.pos, len);
      this.pos += len;
    }

    void skip(int l) {
      pos += l;
    }

    int position() {
      return pos;
    }

    int size() {
      return data.length;
    }

    public StarlarkBytes toStarlarkBytes() {
      return StarlarkBytes.immutableOf(Arrays.copyOfRange(data, 0, pos));
    }
  }

  static class FormatDef {
    char name;
    int size;
    int alignment;

    FormatDef init(char name, int size, int alignment) {
      this.name = name;
      this.size = size;
      this.alignment = alignment;
      return this;
    }

    void pack(ByteStream buf, Object value) throws EvalException {}

    Object unpack(ByteStream buf) {
      return null;
    }

//    int doPack(ByteStream buf, int count, int pos, Number[][] args) throws EvalException {
    int doPack(ByteStream buf, int count, int pos, Tuple args) throws EvalException {
     if (pos + count > args.size()) {
        throw Starlark.errorf("expected %d items for packing (got %d)", buf.position()-pos, args.size());
      }

      int cnt = count;
      while (count-- > 0)
//        pack(buf, args[pos++]);
        pack(buf, args.get(pos++));
      return cnt;
    }

    void doUnpack(ByteStream buf, int count, List<Object> list) {
      while (count-- > 0) {
        final Object unpacked = unpack(buf);
        list.add(unpacked);
      }
    }

    int get_int(Number value) throws EvalException {
      if(!Long.class.isAssignableFrom(value.getClass())) {
        throw Starlark.errorf(
          "required argument is not an integer - received type of: %s",
          value.getClass());
      }
      return value.intValue();
    }

    long get_long(Number value) throws EvalException {
      if(!Long.class.isAssignableFrom(value.getClass())) {
        throw Starlark.errorf(
          "required argument is not an integer - received type of: %s",
          value.getClass());
      }
      return value.longValue();
    }

    BigInteger get_ulong(Number value) throws EvalException {
      if(!Long.class.isAssignableFrom(value.getClass())) {
        throw Starlark.errorf(
          "required argument is not an integer - received type of: %s",
          value.getClass());
      }
      return BigInteger.valueOf(Long.parseUnsignedLong(value.toString()));
    }

    double get_float(Number value) throws EvalException {
      if(!Double.class.isAssignableFrom(value.getClass())) {
        throw Starlark.errorf(
          "required argument is not an double - received type of: %s",
          value.getClass());
      }
      return value.doubleValue();
    }


    void BEwriteInt(ByteStream buf, int v) {
      buf.writeByte((v >>> 24) & 0xFF);
      buf.writeByte((v >>> 16) & 0xFF);
      buf.writeByte((v >>> 8) & 0xFF);
      buf.writeByte((v) & 0xFF);
    }

    void LEwriteInt(ByteStream buf, int v) {
      buf.writeByte((v) & 0xFF);
      buf.writeByte((v >>> 8) & 0xFF);
      buf.writeByte((v >>> 16) & 0xFF);
      buf.writeByte((v >>> 24) & 0xFF);
    }

    int BEreadInt(ByteStream buf) {
      int b1 = buf.readByte();
      int b2 = buf.readByte();
      int b3 = buf.readByte();
      int b4 = buf.readByte();
      return ((b1 << 24) + (b2 << 16) + (b3 << 8) + (b4));
    }

    int LEreadInt(ByteStream buf) {
      int b1 = buf.readByte();
      int b2 = buf.readByte();
      int b3 = buf.readByte();
      int b4 = buf.readByte();
      return ((b1) + (b2 << 8) + (b3 << 16) + (b4 << 24));
    }
  }

  static class PadFormatDef extends FormatDef {

    @Override
    int doPack(ByteStream buf, int count, int pos, Tuple args) {
      while (count-- > 0) {
        buf.writeByte(0);
      }
      return 0;
    }

    @Override
    void doUnpack(ByteStream buf, int count, List<Object> list) {
      while (count-- > 0) {
        buf.readByte();
      }
    }
  }

  static class StringFormatDef extends FormatDef {

    @Override
    int doPack(ByteStream buf, int count, int pos, Tuple args) {
      CharSequence value = (CharSequence) args.get(pos);
      int len = value.length();
      char[] chrArr = new char[len];
      for (int i = 0; i < len; i++) {
        chrArr[i] = value.charAt(i);
      }
      buf.write(chrArr, 0, Math.min(count, len));
      if (len < count) {
        count -= len;
        for (int i = 0; i < count; i++) {
          buf.writeByte(0);
        }
      }
      return 1;
    }

    @Override
    void doUnpack(ByteStream buf, int count, List<Object> list) {
      list.add(buf.readString(count));
    }

  }

  static class PascalStringFormatDef extends StringFormatDef {

    @Override
    int doPack(ByteStream buf, int count, int pos, Tuple args) {
      CharSequence value = (CharSequence) args.get(pos);
      buf.writeByte(Math.min(0xFF, Math.min(value.length(), count - 1)));
      return super.doPack(buf, count - 1, pos, args);
    }

    @Override
    void doUnpack(ByteStream buf, int count, List<Object> list) {
      int n = buf.readByte();
      if (n >= count) {
        n = count - 1;
      }
      super.doUnpack(buf, n, list);
      buf.skip(Math.max(count - n - 1, 0));
    }
  }

  static class CharFormatDef extends FormatDef {

    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      buf.writeByte(asList(value)[0].byteValue());
    }

    @Override
    Object unpack(ByteStream buf) {
      return (char) buf.readByte();
    }
  }

  static class ByteFormatDef extends FormatDef {
    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      buf.writeByte(get_int(asList(value)[0]));
    }

    @Override
    Object unpack(ByteStream buf) {
      int b = buf.readByte();
      if (b > Byte.MAX_VALUE)
        b -= 0x100;
      return b;
    }
  }

  static class UnsignedByteFormatDef extends ByteFormatDef {
    @Override
    Object unpack(ByteStream buf) {
      return buf.readByte();
    }
  }

  static class BooleanFormatDef extends FormatDef {
    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      if(value instanceof StarlarkValue) {
        buf.writeByte(((StarlarkValue)value).truth() ? 1 : 0);
      } else if (value instanceof Boolean) {
        buf.writeByte((Boolean) value ? 1 : 0);
      }else if (value instanceof CharSequence) {
        // we have to default to truthiness (or we have to pass the format ..)
        buf.writeByte(((CharSequence) value).length() == 0 ? 0 : 1);
      } else {
        buf.writeByte(asList(value)[0].longValue() == (long) 0 ? 0 : 1);
      }
    }

    @Override
    Object unpack(ByteStream buf) {
      int b = buf.readByte();
      return b != 0;
    }
  }

  static class LEShortFormatDef extends FormatDef {

    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      int v = get_int(asList(value)[0]);
      buf.writeByte(v & 0xFF);
      buf.writeByte((v >> 8) & 0xFF);
    }

    @Override
    Object unpack(ByteStream buf) {
      int v = buf.readByte() | (buf.readByte() << 8);
      if (v > Short.MAX_VALUE) {
        v -= 0x10000;
      }
      return v;
    }
  }

  static class LEUnsignedShortFormatDef extends LEShortFormatDef {

    @Override
    Object unpack(ByteStream buf) {
      return buf.readByte() | (buf.readByte() << 8);
    }
  }

  static class BEShortFormatDef extends FormatDef {

    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      int v = get_int(asList(value)[0]);
      buf.writeByte((v >> 8) & 0xFF);
      buf.writeByte(v & 0xFF);
    }

    @Override
    Object unpack(ByteStream buf) {
      int v = (buf.readByte() << 8) | buf.readByte();
      if (v > Short.MAX_VALUE) {
        v -= 0x10000;
      }
      return v;
    }
  }

  static class BEUnsignedShortFormatDef extends BEShortFormatDef {

    @Override
    Object unpack(ByteStream buf) {
      return (buf.readByte() << 8) | buf.readByte();
    }
  }

  static class LEIntFormatDef extends FormatDef {

    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      LEwriteInt(buf, get_int(asList(value)[0]));
    }

    @Override
    Object unpack(ByteStream buf) {
      return LEreadInt(buf);
    }
  }

  static class LEUnsignedIntFormatDef extends FormatDef {

    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      LEwriteInt(buf, (int) (get_long(asList(value)[0])));
    }

    @Override
    Object unpack(ByteStream buf) {
      long v = LEreadInt(buf);
      if (v < 0) {
        v += 0x100000000L;
      }
      return v;
    }
  }

  static class BEIntFormatDef extends FormatDef {

    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      BEwriteInt(buf, get_int(asList(value)[0]));
    }

    @Override
    Object unpack(ByteStream buf) {
      return BEreadInt(buf);
    }
  }

  static class BEUnsignedIntFormatDef extends FormatDef {
    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      BEwriteInt(buf, (int) (get_long(asList(value)[0])));
    }

    @Override
    Object unpack(ByteStream buf) {
      long v = BEreadInt(buf);
      if (v < 0)
        v += 0x100000000L;
      return v;
    }
  }

  static class LEUnsignedLongFormatDef extends FormatDef {
    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      BigInteger bi = get_ulong(asList(value)[0]);
      if (bi.compareTo(BigInteger.valueOf(0)) < 0) {
        throw Starlark.errorf("can't convert negative long to unsigned");
      }
      long lvalue = bi.longValue(); // underflow is OK -- the bits are correct
      int high = (int) ((lvalue & 0xFFFFFFFF00000000L) >> 32);
      int low = (int) (lvalue & 0x00000000FFFFFFFFL);
      LEwriteInt(buf, low);
      LEwriteInt(buf, high);
    }

    @Override
    Object unpack(ByteStream buf) {
      long low = (LEreadInt(buf) & 0X00000000FFFFFFFFL);
      long high = (LEreadInt(buf) & 0X00000000FFFFFFFFL);
      BigInteger result = BigInteger.valueOf(high);
      result = result.multiply(BigInteger.valueOf(0x100000000L));
      result = result.add(BigInteger.valueOf(low));
      return result;
    }
  }

  static class BEUnsignedLongFormatDef extends FormatDef {

    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      BigInteger bi = get_ulong(asList(value)[0]);
      if (bi.compareTo(BigInteger.valueOf(0)) < 0) {
        throw new RuntimeException("can't convert negative long to unsigned");
      }
      long lvalue = bi.longValue(); // underflow is OK -- the bits are correct
      int high = (int) ((lvalue & 0xFFFFFFFF00000000L) >> 32);
      int low = (int) (lvalue & 0x00000000FFFFFFFFL);
      BEwriteInt(buf, high);
      BEwriteInt(buf, low);
    }

    @Override
    Object unpack(ByteStream buf) {
      long high = (BEreadInt(buf) & 0X00000000FFFFFFFFL);
      long low = (BEreadInt(buf) & 0X00000000FFFFFFFFL);
      BigInteger result = BigInteger.valueOf(high);
      result = result.multiply(BigInteger.valueOf(0x100000000L));
      result = result.add(BigInteger.valueOf(low));
      return result;
    }
  }

  static class LELongFormatDef extends FormatDef {

    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      long lvalue = get_long(asList(value)[0]);
      int high = (int) ((lvalue & 0xFFFFFFFF00000000L) >> 32);
      int low = (int) (lvalue & 0x00000000FFFFFFFFL);
      LEwriteInt(buf, low);
      LEwriteInt(buf, high);
    }

    @Override
    Object unpack(ByteStream buf) {
      long low = LEreadInt(buf) & 0x00000000FFFFFFFFL;
      long high = ((long) (LEreadInt(buf)) << 32) & 0xFFFFFFFF00000000L;
      return (high | low);
    }
  }

  static class BELongFormatDef extends FormatDef {

    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      long lvalue = get_long(asList(value)[0]);
      int high = (int) ((lvalue & 0xFFFFFFFF00000000L) >> 32);
      int low = (int) (lvalue & 0x00000000FFFFFFFFL);
      BEwriteInt(buf, high);
      BEwriteInt(buf, low);
    }

    @Override
    Object unpack(ByteStream buf) {
      long high = ((long) (BEreadInt(buf)) << 32) & 0xFFFFFFFF00000000L;
      long low = BEreadInt(buf) & 0x00000000FFFFFFFFL;
      return (high | low);
    }
  }

  static class PointerFormatDef extends LELongFormatDef {
    @SuppressWarnings("SameParameterValue")
    FormatDef init(char name) {
      String dataModel = System.getProperty("sun.arch.data.model");
      if (dataModel == null) {
        throw new UnsupportedOperationException("Can't determine if JVM is 32- or 64-bit");
      }
      int length = dataModel.equals("64") ? 8 : 4;
      super.init(name, length, length);
      return this;
    }
  }

  static abstract class HalfFloatFormatDef extends FormatDef {

    private static class FRexpResult {
      int exponent = 0;
      double mantissa = 0.;
    }

    private static FRexpResult frexp(double value) {
      final FRexpResult result = new FRexpResult();
      long bits = Double.doubleToLongBits(value);
      double realMant;

      // Test for NaN, infinity, and zero.
      if (Double.isNaN(value) ||
            value + value == value ||
            Double.isInfinite(value)) {
        result.exponent = 0;
        result.mantissa = value;
      } else {

        boolean neg = (bits < 0);
        int exponent = (int) ((bits >> 52) & 0x7ffL);
        long mantissa = bits & 0xfffffffffffffL;

        if (exponent == 0) {
          exponent++;
        } else {
          mantissa = mantissa | (1L << 52);
        }

        // bias the exponent - actually biased by 1023.
        // we are treating the mantissa as m.0 instead of 0.m
        //  so subtract another 52.
        exponent -= 1075;
        realMant = mantissa;

        // normalize
        while (realMant >= 1.0) {
          mantissa >>= 1;
          realMant /= 2.;
          exponent++;
        }

        if (neg) {
          realMant = realMant * -1;
        }

        result.exponent = exponent;
        result.mantissa = realMant;
      }
      return result;
    }

    protected short pack(double x) throws EvalException {
      boolean sign;
      int e;
      double f;
      short bits;
      if (x == 0.0) {
        sign = (Math.copySign(1.0, x) == -1.0);
        e = 0;
        bits = 0;
      } else if (Double.isInfinite(x)) {
        sign = x < 0.0;
        e = 0x1f;
        bits = 0;
      } else if (Double.isNaN(x)) {
        sign = Math.copySign(1.0, x) == -1.0;
        e = 0x1f;
        bits = 512;
      } else {
        sign = (x < 0.0);
        if (sign) {
          x = -x;
        }
        FRexpResult r = frexp(x);
        f = r.mantissa;
        e = r.exponent;
        /* Normalize f to be in range [1.0, 2.0) */
        f *= 2.0;
        e--;
        if (e >= 16) {
          throw Starlark.errorf("OverflowError: float too large to pack with e format");
        } else if (e < -25) {
          /* |x| < 2**-25. Underflow to zero. */
          f = 0.0;
          e = 0;
        } else if (e < -14) {
          /* |x| < 2**-14. Gradual underflow */
          f = Math.scalb(f, 14 + e);
          e = 0;
        } else /* if (!(e == 0 && f == 0.0)) */ {
          e += 15;
          f -= 1.0; /* Get rid of leading 1 */
        }

        f *= 1024.0; /* 2**10 */
        /* Round to even */
        bits = (short) f;
        assert (bits < 1024);
        assert (e < 31);
        if ((f - bits > 0.5) || ((f - bits == 0.5) && (bits % 2 == 1))) {
          ++bits;
          if (bits == 1024) {
            /* The carry propagated out of a string of 10 1 bits. */
            bits = 0;
            ++e;
            if (e == 31) {
              throw Starlark.errorf("OverflowError: float too large to pack with e format");
            }
          }
        }
      }
      bits |= (e << 10) | ((sign ? 1 : 0) << 15);
      return bits;
    }

    protected Float unpack(int firstbyte, int secondbyte) {
      boolean sign;
      int e;
      int f;
      double x;
      /* First byte */
      sign = ((firstbyte >> 7) & 1) == 1;
      e = (firstbyte & 0x7C) >> 2;
      f = (firstbyte & 0x03) << 8;

      /* Second byte */
      f |= secondbyte;

      if (e == 0x1F) {
          if (f == 0) {
              /* Infinity */
              return sign ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
          }
          /* Nan */
          return sign ? -Float.NaN : Float.NaN;
      }
      x = f / 1024.0;
      if (e == 0) {
          e = -14;
      } else {
          x += 1.0;
          e -= 15;
      }
      x = Math.scalb(x, e);
      if (sign) {
          return Double.valueOf(-x).floatValue();
      }
      return Double.valueOf(x).floatValue();
    }

  }

  static class LEHalfFloatFormatDef extends HalfFloatFormatDef {

    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      short bits = pack(asList(value)[0].doubleValue());
      int firstbyte = (bits >> 8) & 0xFF;
      int secondbyte = bits & 0xFF;
      /* Write out result. */
      buf.writeByte((byte) secondbyte);
      buf.writeByte((byte) firstbyte);
    }

    @Override
    Object unpack(ByteStream buf) {
      int secondbyte = buf.readByte() & 0xFF;
      int firstbyte = buf.readByte() & 0xFF;
      return unpack(firstbyte, secondbyte);

    }
  }

  static class BEHalfFloatFormatDef extends HalfFloatFormatDef {

    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      short bits = pack(asList(value)[0].doubleValue());
      int firstbyte = (bits >> 8) & 0xFF;
      int secondbyte = bits & 0xFF;
      /* Write out result. */
      buf.writeByte((byte) firstbyte);
      buf.writeByte((byte) secondbyte);
    }

    @Override
    Object unpack(ByteStream buf) {
      int firstbyte = buf.readByte() & 0xFF;
      int secondbyte = buf.readByte() & 0xFF;
      return unpack(firstbyte, secondbyte);
    }
  }

  static class LEFloatFormatDef extends FormatDef {

    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      int bits = Float.floatToIntBits((float) get_float(asList(value)[0]));
      LEwriteInt(buf, bits);
    }

    @Override
    Object unpack(ByteStream buf) {
      int bits = LEreadInt(buf);
      float v = Float.intBitsToFloat(bits);
      if (Float.isInfinite(v) || Float.isNaN(v)) {
        throw new UnsupportedOperationException("can't unpack IEEE 754 special value on non-IEEE platform");
      }
      return v;
    }
  }

  static class LEDoubleFormatDef extends FormatDef {

    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      long bits = Double.doubleToLongBits(get_float(asList(value)[0]));
      LEwriteInt(buf, (int) (bits));
      LEwriteInt(buf, (int) (bits >>> 32));
    }

    @Override
    Object unpack(ByteStream buf) {
      long bits = (LEreadInt(buf) & 0xFFFFFFFFL) + (((long) LEreadInt(buf)) << 32);
      double v = Double.longBitsToDouble(bits);
      if (Double.isInfinite(v) || Double.isNaN(v)) {
        throw new UnsupportedOperationException("can't unpack IEEE 754 special value on non-IEEE platform");
      }
      return v;
    }
  }

  static class BEFloatFormatDef extends FormatDef {

    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      int bits = Float.floatToIntBits((float) get_float(asList(value)[0]));
      BEwriteInt(buf, bits);
    }

    @Override
    Object unpack(ByteStream buf) {
      int bits = BEreadInt(buf);
      float v = Float.intBitsToFloat(bits);
      if (Float.isInfinite(v) || Float.isNaN(v)) {
        throw new UnsupportedOperationException("can't unpack IEEE 754 special value on non-IEEE platform");
      }
      return v;
    }
  }

  static class BEDoubleFormatDef extends FormatDef {

    @Override
    void pack(ByteStream buf, Object value) throws EvalException {
      long bits = Double.doubleToLongBits(get_float(asList(value)[0]));
      BEwriteInt(buf, (int) (bits >>> 32));
      BEwriteInt(buf, (int) (bits));
    }

    @Override
    Object unpack(ByteStream buf) {
      long bits = (((long) BEreadInt(buf)) << 32) + (BEreadInt(buf) & 0xFFFFFFFFL);
      double v = Double.longBitsToDouble(bits);
      if (Double.isInfinite(v) || Double.isNaN(v)) {
        throw new UnsupportedOperationException("can't unpack IEEE 754 special value on non-IEEE platform");
      }
      return v;
    }
  }
}
