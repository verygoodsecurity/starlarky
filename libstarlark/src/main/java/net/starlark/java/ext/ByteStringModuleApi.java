package net.starlark.java.ext;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Sequence;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkList;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.Tuple;

public interface ByteStringModuleApi {

  @StarlarkMethod(
      name = "hex",
      doc =
        "Return a string object containing two hexadecimal digits for each byte in " +
          "the instance.\n" +
          "\n" +
        ">>> b'\\xf0\\xf1\\xf2'.hex()\n" +
        "'f0f1f2'" +
        "\n" +
        "If you want to make the hex string easier to read, you can specify a single character " +
          "separator sep parameter to include in the output. By default between each byte. " +
          "A second optional bytes_per_sep parameter controls the spacing. Positive values " +
          "calculate the separator position from the right, negative values from the left.\n" +
        "\n" +
        ">>> value = b'\\xf0\\xf1\\xf2'\n" +
        ">>> value.hex('-')\n" +
        "'f0-f1-f2'" + "\n" +
        ">>> value.hex('_', 2)\n" +
        "'f0_f1f2'" + "\n" +
        ">>> b'UUDDLRLRAB'.hex(' ', -4)\n" +
        "'55554444 4c524c52 4142'",
      parameters = {
        @Param(name = "sep", allowedTypes = {
          @ParamType(type = String.class),
          @ParamType(type = StarlarkBytes.class),
        },defaultValue = "unbound"),
        @Param(
          name = "bytes_per_sep",
          allowedTypes = {
            @ParamType(type = StarlarkInt.class)
          },
          defaultValue = "-1"
        )
      })
    String hex(Object sepO, StarlarkInt bytesPerSep) throws EvalException;


  @StarlarkMethod(
    name = "count",
    doc =
      "Return the number of non-overlapping occurrences of subsequence sub in the range " +
        "[start, end]. Optional arguments start and end are interpreted as in slice " +
        "notation." +
        "\n" +
        "The subsequence to search for may be any bytes-like object or an integer in the " +
        "range 0 to 255.",
    parameters = {
      @Param(name = "sub", allowedTypes = {
        @ParamType(type = StarlarkInt.class),
        @ParamType(type = StarlarkBytes.class),
      }),
      @Param(
        name = "start",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "0",
        doc = "Restrict to search from this position."),
      @Param(
        name = "end",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "unbound",
        doc = "optional position before which to restrict to search.")
    })
  int count(Object sub, Object start, Object end) throws EvalException;

  @StarlarkMethod(
    name = "removeprefix",
    doc =
      "If the binary data starts with the prefix string, return bytes[len(prefix):]. " +
        "Otherwise, return a copy of the original binary data:" +
        "",
    parameters = {
      @Param(name = "prefix", allowedTypes = {
        @ParamType(type = StarlarkBytes.class),
      },
        doc = "The prefix may be any bytes-like object.")})
  StarlarkBytes removeprefix(StarlarkBytes prefix) throws EvalException;

  @StarlarkMethod(
    name = "removesuffix",
    doc =
      "If the binary data ends with the suffix string and that suffix is not empty, " +
        "return bytes[:-len(suffix)]. Otherwise, return a copy of the original binary" +
        " data:\n" +
        ">>> b'MiscTests'.removesuffix(b'Tests')\n" +
        "b'Misc'\n" +
        ">>> b'TmpDirMixin'.removesuffix(b'Tests')\n" +
        "b'TmpDirMixin'\n" +
        "The suffix may be any bytes-like object.",
    parameters = {
      @Param(name = "suffix", allowedTypes = {
        @ParamType(type = StarlarkBytes.class),
      },
        doc = "The suffix may be any bytes-like object.")})
  StarlarkBytes removesuffix(StarlarkBytes suffix) throws EvalException;

    @StarlarkMethod(
        name = "decode",
        parameters = {
            @Param(name = "encoding", defaultValue = "'utf-8'"),
            @Param(name ="errors", defaultValue = "'strict'")
        })
  String decode(String encoding, String errors) throws EvalException;

  @StarlarkMethod(
    name = "endswith",
    doc = "B.endswith(suffix[, start[, end]]) -> bool\n" +
            "\n" +
            "Return True if B ends with the specified suffix, False otherwise.\n" +
            "With optional start, test B beginning at that position.\n" +
            "With optional end, stop comparing B at that position.\n" +
            "suffix can also be a tuple of bytes to try.\n.",
    parameters = {
      @Param(
        name = "suffix",
        allowedTypes = {
          @ParamType(type = StarlarkBytes.class),
          @ParamType(type = Tuple.class, generic1 = StarlarkBytes.class),
        },
        doc = "The suffix (or tuple of alternative suffixes) to match."),
      @Param(
        name = "start",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "0",
        doc = "Test beginning at this position."),
      @Param(
        name = "end",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "None",
        doc = "optional position at which to stop comparing.")
    })
  boolean endsWith(Object suffix, Object start, Object end) throws EvalException;

  @StarlarkMethod(
    name = "find",
    doc =
      "Return the lowest index in the data where the subsequence sub is found, such that sub is contained in the slice s[start:end]. Optional arguments start and end are interpreted as in slice notation. Return -1 if sub is not found.\n" +
        "\n" +
        "The subsequence to search for may be any bytes-like object or an integer in the range 0 to 255.\n" +
        "\n" +
        "Note The find() method should be used only if you need to know the position of sub. To check if sub is a substring or not, use the in operator:\n" +
        ">>>\n" +
        ">>> b'Py' in b'Python'\n" +
        "True",
    parameters = {
      @Param(name = "sub", allowedTypes = {
        @ParamType(type = StarlarkInt.class),
        @ParamType(type = StarlarkBytes.class),
      }),
      @Param(
        name = "start",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "0",
        doc = "Restrict to search from this position."),
      @Param(
        name = "end",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "unbound",
        doc = "optional position before which to restrict to search.")
    })
  int find(Object sub, Object start, Object end) throws EvalException;

  @StarlarkMethod(
    name = "index",
    doc =
      "Like find(), but raise ValueError when the subsequence is not found.\n" +
        "\n" +
        "The subsequence to search for may be any bytes-like object or an integer in the range 0 to 255.",
    parameters = {
      @Param(name = "sub", allowedTypes = {
        @ParamType(type = StarlarkInt.class),
        @ParamType(type = StarlarkBytes.class),
      }),
      @Param(
        name = "start",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "0",
        doc = "Restrict to search from this position."),
      @Param(
        name = "end",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "unbound",
        doc = "optional position before which to restrict to search.")
    })
  int index(Object sub, Object start, Object end) throws EvalException;


  @StarlarkMethod(
    name = "join",
    doc = "Concatenate any number of bytes objects.\n" +
            "\n" +
            "The bytes whose method is called is inserted in between each pair.\n" +
            "\n" +
            "The result is returned as a new bytes object.\n" +
            "\n" +
            "Example: b'.'.join([b'ab', b'pq', b'rs']) -> b'ab.pq.rs'.\n",
    parameters = {
      @Param(name = "iterable_of_bytes", doc = "The bytes to join,",
        allowedTypes = {
          @ParamType(type = Sequence.class, generic1 = StarlarkBytes.class)
        })
  })
  StarlarkBytes join(Sequence<StarlarkBytes> elements) throws EvalException;

  @StarlarkMethod(
    name = "partition",
    doc =
      "Split the sequence at the first occurrence of sep, and return a 3-tuple containing the" +
        " part before the separator, the separator itself or its bytearray copy, and the part" +
        " after the separator. If the separator is not found, return a 3-tuple containing a " +
        "copy of the original sequence, followed by two empty bytes or bytearray objects." +
        "\n" +
        "The separator to search for may be any bytes-like object.",
    parameters = {@Param(name = "sep", doc = "The bytes-like object separator to search for")})
  Tuple partition(StarlarkBytes sep) throws EvalException;

  @StarlarkMethod(
    name = "replace",
    doc =
      "Return a copy of the sequence with all occurrences of subsequence old replaced " +
        "by new. If the optional argument count is given, only the first count " +
        "occurrences are replaced." +
        "\n" +
        "The subsequence to search for and its replacement may be any bytes-like" +
        " object.",
    parameters = {
      @Param(name = "old", doc = "The bytes-like object to be replaced."),
      @Param(name = "new", doc = "The bytes-like object to replace with."),
      @Param(
        name = "count",
        defaultValue = "-1",
        doc =
          "The maximum number of replacements. If omitted, or if the value is negative, "
            + "there is no limit.")
    },
    useStarlarkThread = true)
  StarlarkBytes replace(StarlarkBytes oldBytes, StarlarkBytes newBytes, StarlarkInt countI, StarlarkThread thread)
    throws EvalException;

  @StarlarkMethod(
    name = "rfind",
    doc =
      "Return the highest index in the sequence where the subsequence sub is found, " +
        "such that sub is contained within s[start:end]. " +
        "Optional arguments start and end are interpreted as in slice notation. " +
        "Return -1 on failure.\n" +
        "\n" +
        "The subsequence to search for may be any bytes-like object or an integer in " +
        "the range 0 to 255.",
    parameters = {
      @Param(name = "sub", allowedTypes = {
        @ParamType(type = StarlarkInt.class),
        @ParamType(type = StarlarkBytes.class),
      }),
      @Param(
        name = "start",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "0",
        doc = "Restrict to search from this position."),
      @Param(
        name = "end",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "None",
        doc = "optional position before which to restrict to search.")
    })
  int rfind(Object sub, Object start, Object end) throws EvalException;

  @StarlarkMethod(
    name = "rindex",
    doc =
      "Like rfind(), but raise ValueError when the subsequence sub is not found.\n" +
        "\n" +
        "The subsequence to search for may be any bytes-like object or an integer in the range 0 to 255.",
    parameters = {
      @Param(name = "sub", allowedTypes = {
        @ParamType(type = StarlarkInt.class),
        @ParamType(type = StarlarkBytes.class),
      }),
      @Param(
        name = "start",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "0",
        doc = "Restrict to search from this position."),
      @Param(
        name = "end",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "None",
        doc = "optional position before which to restrict to search.")
    })
  int rindex(Object sub, Object start, Object end) throws EvalException;

  @StarlarkMethod(
    name = "rpartition",
    doc =
      "Split the sequence at the last occurrence of sep, and return a 3-tuple containing " +
        "the part before the separator, the separator itself or its bytearray copy, and the" +
        " part after the separator. If the separator is not found, return a 3-tuple " +
        "containing two empty bytes or bytearray objects, followed by a copy of the " +
        "original sequence." +
        "\n" +
        "The separator to search for may be any bytes-like object.",
    parameters = {@Param(name = "sep", doc = "The bytes-like object separator to search for")})
  Tuple rpartition(StarlarkBytes sep) throws EvalException;

  @StarlarkMethod(
    name = "startswith",
    doc = "B.startswith(prefix[, start[, end]]) -> bool\n" +
            "\n" +
            "Return True if B starts with the specified prefix, False otherwise.\n" +
            "With optional start, test B beginning at that position.\n" +
            "With optional end, stop comparing B at that position.\n" +
            "prefix can also be a tuple of bytes to try.\n",
    parameters = {
      @Param(
        name = "prefix",
        allowedTypes = {
          @ParamType(type = StarlarkBytes.class),
          @ParamType(type = Tuple.class, generic1 = StarlarkBytes.class),
        },
        doc = "The prefix (or tuple of alternative prefixes) to match."),
      @Param(
        name = "start",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "0",
        doc = "Test beginning at this position."),
      @Param(
        name = "end",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "None",
        doc = "Stop comparing at this position.")
    })
  boolean startsWith(Object prefix, Object start, Object end)
    throws EvalException;

  @StarlarkMethod(
    name = "translate",
    doc = "Return a copy of the bytes or bytearray object where all bytes occurring in the " +
            "optional argument delete are removed, and the remaining bytes have been mapped through the given translation table, which must be a bytes object of length 256.\n" +
            "\n" +
            "You can use the bytes.maketrans() method to create a translation table.\n" +
            "\n" +
            "Set the table argument to None for translations that only delete characters:\n" +
            "\n" +
            ">>>\n" +
            ">>> b'read this short text'.translate(None, b'aeiou')\n" +
            "b'rd ths shrt txt'",
    parameters = {
      @Param(
        name = "table",
        allowedTypes = {
          @ParamType(type = StarlarkBytes.class),
          @ParamType(type = NoneType.class),
        },
        doc = "Translation table, which must be a bytes object of length 256."),
      @Param(
        name = "delete",
        allowedTypes = {
          @ParamType(type = StarlarkBytes.class)
        },
        named = true,
        defaultValue = "b''",
        doc = "All characters occurring in the argument delete are removed.")
    })
  StarlarkBytes translate(Object tableO, StarlarkBytes delete)
    throws EvalException;

  //  The following methods on bytes and bytearray objects have default
  //  behaviours that assume the use of ASCII compatible binary formats,
  //  but can still be used with arbitrary binary data by passing
  //  appropriate arguments.
  //
  //  Note that all of the bytearray methods in this section do not
  //  operate in place, and instead produce new objects

  @StarlarkMethod(
    name = "center",
    doc = "Return a copy of the object centered in a sequence of length width. Padding is done using the specified fillbyte (default is an ASCII space). For bytes objects, the original sequence is returned if width is less than or equal to len(s).",
    parameters = {
      @Param(
        name = "width",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class)
        }), @Param(
      name = "fillbyte",
      allowedTypes = {
        @ParamType(type = StarlarkBytes.class),
      }, defaultValue = "b' '")
    })
  StarlarkBytes center(StarlarkInt width, StarlarkBytes fillbyte) throws EvalException;

  @StarlarkMethod(
    name = "ljust",
    doc = "Return a copy of the object left justified in a sequence of length width. Padding is done using the specified fillbyte (default is an ASCII space). For bytes objects, the original sequence is returned if width is less than or equal to len(s).",
    parameters = {
      @Param(
        name = "width",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class)
        }), @Param(
      name = "fillbyte",
      allowedTypes = {
        @ParamType(type = StarlarkBytes.class),
      }, defaultValue = "b' '")
    })
  StarlarkBytes ljust(StarlarkInt width, StarlarkBytes fillbyte) throws EvalException;

  @StarlarkMethod(
    name = "lstrip",
    doc = "" +
            "Return a copy of the sequence with specified leading bytes removed. The chars argument is a binary sequence specifying the set of byte values to be removed - the name refers to the fact this method is usually used with ASCII characters. If omitted or None, the chars argument defaults to removing ASCII whitespace. The chars argument is not a prefix; rather, all combinations of its values are stripped:\n" +
            "\n" +
            ">>>\n" +
            ">>> b'   spacious   '.lstrip()\n" +
            "b'spacious   '\n" +
            ">>> b'www.example.com'.lstrip(b'cmowz.')\n" +
            "b'example.com'\n" +
            "The binary sequence of byte values to remove may be any bytes-like object. See removeprefix() for a method that will remove a single prefix string rather than all of a set of characters. For example:\n" +
            "\n" +
            ">>>\n" +
            ">>> b'Arthur: three!'.lstrip(b'Arthur: ')\n" +
            "b'ee!'\n" +
            ">>> b'Arthur: three!'.removeprefix(b'Arthur: ')\n" +
            "b'three!'",
    parameters = {
      @Param(
        name = "chars",
        allowedTypes = {
          @ParamType(type = StarlarkBytes.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "None")
    })
  StarlarkBytes lstrip(Object charsO);

  @StarlarkMethod(
    name = "rjust",
    doc = "Return a copy of the object right justified in a sequence of length width. Padding is done using the specified fillbyte (default is an ASCII space). For bytes objects, the original sequence is returned if width is less than or equal to len(s).",
    parameters = {
      @Param(
        name = "width",
        allowedTypes = {
          @ParamType(type = StarlarkInt.class)
        }), @Param(
      name = "fillbyte",
      allowedTypes = {
        @ParamType(type = StarlarkBytes.class),
      }, defaultValue = "b' '")
    })
  StarlarkBytes rjust(StarlarkInt width, StarlarkBytes fillbyte) throws EvalException;

  @StarlarkMethod(
    name = "rsplit",
    doc = "" +
            "Split the binary sequence into subsequences of the same type, using sep as the" +
            " delimiter string. If maxsplit is given, at most maxsplit splits are done, the" +
            " rightmost ones. If sep is not specified or None, any subsequence consisting " +
            "solely of ASCII whitespace is a separator. Except for splitting from the " +
            "right, rsplit() behaves like split() which is described in detail below.\n" +
            "\n",
    parameters = {
      @Param(name = "sep", doc = "The delimiter to split on.", named=true,
        allowedTypes = {
        @ParamType(type = StarlarkBytes.class),
        @ParamType(type = NoneType.class)
      }, defaultValue = "None"),
      @Param(
        name = "maxsplit", named=true,
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "-1",
        doc = "The maximum number of splits.")
    },
    useStarlarkThread = true)
  StarlarkList<StarlarkBytes> rsplit(Object bytesO, Object maxSplitO, StarlarkThread thread) throws EvalException;

  @StarlarkMethod(
    name = "rstrip",
    doc = "Return a copy of the sequence with specified trailing bytes removed. The chars argument is a binary sequence specifying the set of byte values to be removed - the name refers to the fact this method is usually used with ASCII characters. If omitted or None, the chars argument defaults to removing ASCII whitespace. The chars argument is not a suffix; rather, all combinations of its values are stripped:\n" +
            "\n" +
            ">>>\n" +
            ">>> b'   spacious   '.rstrip()\n" +
            "b'   spacious'\n" +
            ">>> b'mississippi'.rstrip(b'ipz')\n" +
            "b'mississ'\n" +
            "The binary sequence of byte values to remove may be any bytes-like object. See removesuffix() for a method that will remove a single suffix string rather than all of a set of characters. For example:\n" +
            "\n" +
            ">>>\n" +
            ">>> b'Monty Python'.rstrip(b' Python')\n" +
            "b'M'\n" +
            ">>> b'Monty Python'.removesuffix(b' Python')\n" +
            "b'Monty'\n",
    parameters = {
      @Param(
        name = "chars",
        allowedTypes = {
          @ParamType(type = StarlarkBytes.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "None")
    })
  StarlarkBytes rstrip(Object charsO);

  @StarlarkMethod(
    name = "split",
    doc = "" +
      "Return a list of the sections in the bytes, using sep as the delimiter.\n" +
      "\n" +
      "sep\n" +
      "  The delimiter according which to split the bytes.\n" +
      "  None (the default value) means split on ASCII whitespace characters\n" +
      "  (space, tab, return, newline, formfeed, vertical tab).\n" +
      "maxsplit\n" +
      "  Maximum number of splits to do.\n" +
      "  -1 (the default value) means no limit.",
    parameters = {
      @Param(name = "sep", doc = "The delimiter to split on.", named = true, allowedTypes = {
        @ParamType(type = StarlarkBytes.class),
        @ParamType(type = NoneType.class)
      }, defaultValue = "None"),
      @Param(
        name = "maxsplit", named = true,
        allowedTypes = {
          @ParamType(type = StarlarkInt.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "-1",
        doc = "The maximum number of splits.")
    },
    useStarlarkThread = true)
  StarlarkList<StarlarkBytes> split(Object bytesO, Object maxSplitO, StarlarkThread thread) throws EvalException;

  @StarlarkMethod(
    name = "strip",
    doc = "Return a copy of the sequence with specified leading and trailing bytes removed. The " +
            "chars argument is a binary sequence specifying the set of byte values to be" +
            " removed - the name refers to the fact this method is usually used with ASCII " +
            "characters. " +
            "If omitted or None, the chars argument defaults to removing ASCII whitespace. " +
            "" +
            "The chars argument is not a prefix or suffix; rather, all combinations of its " +
            "values are stripped:" +
            "\n" +
            ">>> b'   spacious   '.strip()\n" +
            "b'spacious'\n" +
            ">>> b'www.example.com'.strip(b'cmowz.')\n" +
            "b'example'\n" +
            "The binary sequence of byte values to remove may be any bytes-like object.\n" +
            "\n",
    parameters = {
      @Param(
        name = "chars",
        doc = "binary sequence of byte values to remove may be any bytes-like object.",
        allowedTypes = {
          @ParamType(type = StarlarkBytes.class),
          @ParamType(type = NoneType.class),
        },
        defaultValue = "None")
    })
  StarlarkBytes strip(Object charsO);

  //
  //  The following methods on bytes and bytearray objects assume the use of ASCII compatible
  //  binary formats and should not be applied to arbitrary binary data.
  //
  //  Note that all of the bytearray methods in this section do not operate in place, and
  //  instead produce new objects.
  //

  @StarlarkMethod(
    name = "capitalize",
    doc =
      "Return a copy of the sequence with each byte interpreted as an ASCII character, and the first byte capitalized and the rest lowercased. Non-ASCII byte values are passed through unchanged."
  )
  StarlarkBytes capitalize();

  @StarlarkMethod(
    name = "expandtabs",
    doc = "Return a copy of the sequence where all ASCII tab characters are replaced by one or more ASCII spaces, depending on the current column and the given tab size. Tab positions occur every tabsize bytes (default is 8, giving tab positions at columns 0, 8, 16 and so on). To expand the sequence, the current column is set to zero and the sequence is examined byte by byte. If the byte is an ASCII tab character (b'\\t'), one or more space characters are inserted in the result until the current column is equal to the next tab position. (The tab character itself is not copied.) If the current byte is an ASCII newline (b'\\n') or carriage return (b'\\r'), it is copied and the current column is reset to zero. Any other byte value is copied unchanged and the current column is incremented by one regardless of how the byte value is represented when printed:\n" +
            "\n" +
            ">>>\n" +
            ">>> b'01\\t012\\t0123\\t01234'.expandtabs()\n" +
            "b'01      012     0123    01234'\n" +
            ">>> b'01\\t012\\t0123\\t01234'.expandtabs(4)\n" +
            "b'01  012 0123    01234'" +
            "\n",
    parameters = {@Param(
      name = "tabsize", named = true,
      allowedTypes = {@ParamType(type = StarlarkInt.class)},
      defaultValue = "8"
  )})
  StarlarkBytes expandTabs(StarlarkInt tabSize) throws EvalException;

  @StarlarkMethod(
    name = "isalnum",
    doc =
      "Return True if all bytes in the sequence are alphabetical ASCII characters or ASCII decimal digits and the sequence is not empty, False otherwise. Alphabetic ASCII characters are those byte values in the sequence b'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'. ASCII decimal digits are those byte values in the sequence b'0123456789'.\n" +
        "\n" +
        "For example:\n" +
        "\n" +
        ">>>\n" +
        ">>> b'ABCabc1'.isalnum()\n" +
        "True\n" +
        ">>> b'ABC abc1'.isalnum()\n" +
        "False"
  )
  boolean isAlnum() throws EvalException;

  @StarlarkMethod(
    name = "isalpha",
    doc =
      "Return True if all bytes in the sequence are alphabetic ASCII characters and the sequence is not empty, False otherwise. Alphabetic ASCII characters are those byte values in the sequence b'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'.\n" +
        "\n" +
        "For example:\n" +
        "\n" +
        ">>>\n" +
        ">>> b'ABCabc'.isalpha()\n" +
        "True\n" +
        ">>> b'ABCabc1'.isalpha()\n" +
        "False\n"
  )
  boolean isAlpha() throws EvalException;

  @StarlarkMethod(
    name = "isascii",
    doc =
      "Return True if the sequence is empty or all bytes in the sequence are ASCII, False otherwise. ASCII bytes are in the range 0-0x7F."
  )
  boolean isAscii() throws EvalException;

  @StarlarkMethod(
    name = "isdigit",
    doc =
      "Return True if all bytes in the sequence are ASCII decimal digits and the sequence is not empty, False otherwise. ASCII decimal digits are those byte values in the sequence b'0123456789'.\n" +
        "\n" +
        "For example:\n" +
        "\n" +
        ">>>\n" +
        ">>> b'1234'.isdigit()\n" +
        "True\n" +
        ">>> b'1.23'.isdigit()\n" +
        "False"
  )
  boolean isDigit();

  @StarlarkMethod(
    name = "islower",
    doc =
      "Return True if there is at least one lowercase ASCII character in the sequence and no uppercase ASCII characters, False otherwise.\n" +
        "\n" +
        "For example:\n" +
        "\n" +
        ">>>\n" +
        ">>> b'hello world'.islower()\n" +
        "True\n" +
        ">>> b'Hello world'.islower()\n" +
        "False\n" +
        "Lowercase ASCII characters are those byte values in the sequence b'abcdefghijklmnopqrstuvwxyz'. Uppercase ASCII characters are those byte values in the sequence b'ABCDEFGHIJKLMNOPQRSTUVWXYZ'."
  )
  boolean isLower();

  @StarlarkMethod(
    name = "isspace",
    doc =
      "Return True if all bytes in the sequence are ASCII whitespace and the sequence is not empty, False otherwise. ASCII whitespace characters are those byte values in the sequence b' \\t\\n\\r\\x0b\\f' (space, tab, newline, carriage return, vertical tab, form feed)."
  )
  boolean isSpace() throws EvalException;

  @StarlarkMethod(
    name = "istitle",
    doc =
      "Return True if the sequence is ASCII titlecase and the sequence is not empty, False otherwise. See bytes.title() for more details on the definition of “titlecase”.\n" +
        "\n" +
        "For example:\n" +
        "\n" +
        ">>>\n" +
        ">>> b'Hello World'.istitle()\n" +
        "True\n" +
        ">>> b'Hello world'.istitle()\n" +
        "False"
  )
  boolean isTitle() throws EvalException;

  @StarlarkMethod(
    name = "isupper",
    doc =
      "Return True if there is at least one uppercase alphabetic ASCII character in the sequence and no lowercase ASCII characters, False otherwise.\n" +
        "\n" +
        "For example:\n" +
        "\n" +
        ">>>\n" +
        ">>> b'HELLO WORLD'.isupper()\n" +
        "True\n" +
        ">>> b'Hello world'.isupper()\n" +
        "False\n" +
        "Lowercase ASCII characters are those byte values in the sequence b'abcdefghijklmnopqrstuvwxyz'. Uppercase ASCII characters are those byte values in the sequence b'ABCDEFGHIJKLMNOPQRSTUVWXYZ'."
  )
  boolean isUpper();

  @StarlarkMethod(
    name = "lower",
    doc = "B.lower() -> copy of B\n" +
            "\n" +
            "Return a copy of B with all ASCII characters converted to lowercase.")
  StarlarkBytes lower();

  @StarlarkMethod(
    name = "splitlines",
    doc =
      "Return a list of the lines in the binary sequence, breaking at ASCII " +
        "line boundaries. This method uses the universal newlines approach " +
        "to splitting lines. Line breaks are not included in the resulting " +
        "list unless keepends is given and true." +
        "\n" +
        "For example:\n" +
        "\n" +
        ">>>\n" +
        ">>> b'ab c\\n\\nde fg\\rkl\\r\\n'.splitlines()\n" +
        "[b'ab c', b'', b'de fg', b'kl']\n" +
        ">>> b'ab c\\n\\nde fg\\rkl\\r\\n'.splitlines(keepends=True)\n" +
        "[b'ab c\\n', b'\\n', b'de fg\\r', b'kl\\r\\n']\n" +
        "Unlike split() when a delimiter string sep is given, this method returns an empty list for the empty string, and a terminal line break does not result in an extra line:\n" +
        "\n" +
        ">>>\n" +
        ">>> b\"\".split(b'\\n'), b\"Two lines\\n\".split(b'\\n')\n" +
        "([b''], [b'Two lines', b''])\n" +
        ">>> b\"\".splitlines(), b\"One line\\n\".splitlines()\n" +
        "([], [b'One line'])\n",
    parameters = {
      @Param(
        name = "keepends", named=true,
        defaultValue = "False",
        doc = "Whether the line breaks should be included in the resulting list.")
  })
  Sequence<StarlarkBytes> splitLines(boolean keepEnds)
    throws EvalException;

  @StarlarkMethod(
    name = "swapcase",
    doc =
      "Return a copy of the sequence with all the lowercase ASCII characters converted to their corresponding uppercase counterpart and vice-versa.\n" +
        "\n" +
        "For example:\n" +
        "\n" +
        ">>>\n" +
        ">>> b'Hello World'.swapcase()\n" +
        "b'hELLO wORLD'\n" +
        "Lowercase ASCII characters are those byte values in the sequence b'abcdefghijklmnopqrstuvwxyz'. Uppercase ASCII characters are those byte values in the sequence b'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.\n" +
        "\n" +
        "Unlike str.swapcase(), it is always the case that bin.swapcase().swapcase() == bin for the binary versions. Case conversions are symmetrical in ASCII, even though that is not generally true for arbitrary Unicode code points."
  )
  StarlarkBytes swapcase() throws EvalException;

  @StarlarkMethod(
    name = "title",
    doc =
      "Return a titlecased version of the binary sequence where words start with an uppercase ASCII character and the remaining characters are lowercase. Uncased byte values are left unmodified.\n" +
        "\n" +
        "For example:\n" +
        "\n" +
        ">>>\n" +
        ">>> b'Hello world'.title()\n" +
        "b'Hello World'\n" +
        "Lowercase ASCII characters are those byte values in the sequence b'abcdefghijklmnopqrstuvwxyz'. Uppercase ASCII characters are those byte values in the sequence b'ABCDEFGHIJKLMNOPQRSTUVWXYZ'. All other byte values are uncased.\n" +
        "\n" +
        "The algorithm uses a simple language-independent definition of a word as groups of consecutive letters. The definition works in many contexts but it means that apostrophes in contractions and possessives form word boundaries, which may not be the desired result:\n" +
        "\n" +
        ">>>\n" +
        ">>> b\"they're bill's friends from the UK\".title()\n" +
        "b\"They'Re Bill'S Friends From The Uk\"\n" +
        "A workaround for apostrophes can be constructed using regular expressions:\n" +
        "\n" +
        ">>>\n" +
        ">>> load(\"@stdlib//re\", \"re\")\n" +
        ">>> def titlecase(s):\n" +
        "...     return re.sub(rb\"[A-Za-z]+('[A-Za-z]+)?\",\n" +
        "...                   lambda mo: mo.group(0)[0:1].upper() +\n" +
        "...                              mo.group(0)[1:].lower(),\n" +
        "...                   s)\n" +
        "...\n" +
        ">>> titlecase(b\"they're bill's friends.\")\n" +
        "b\"They're Bill's Friends.\""
  )
  StarlarkBytes title();

  @StarlarkMethod(
    name = "upper",
    doc = "B.upper() -> copy of B\n" +
            "\n" +
            "Return a copy of B with all ASCII characters converted to uppercase.")
  StarlarkBytes upper();

  @StarlarkMethod(
    name = "zfill",
    doc = "Return a copy of the sequence left filled with ASCII b'0' " +
            "digits to make a sequence of length width. A leading " +
            "sign prefix (b'+'/ b'-') is handled by inserting the " +
            "padding after the sign character rather than before. " +
            "\n" +
            "For bytes objects, the original sequence is returned if " +
            "width is less than or equal to len(seq).\n" +
            "\n" +
            "For example:\n" +
            "\n" +
            ">>>\n" +
            ">>> b\"42\".zfill(5)\n" +
            "b'00042'\n" +
            ">>> b\"-42\".zfill(5)\n" +
            "b'-0042'\n", parameters = {
    @Param(
      name = "width",
      allowedTypes = {
        @ParamType(type = StarlarkInt.class)})}
  )
  StarlarkBytes zfill(StarlarkInt width) throws EvalException;
}
