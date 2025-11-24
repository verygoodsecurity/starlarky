load("@stdlib//builtins", "builtins")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//larky", larky="larky")
load("@stdlib//math", math="math")
load("@stdlib//struct", "struct")
load("@stdlib//sys", sys="sys")
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")


_b = b"\x00\x01\x00\x02"


def _test_pack():
    # test string format string
    result = struct.pack(">HH", 1, 2)
    asserts.assert_that(result).is_equal_to(_b)

    # test bytes format string
    result = struct.pack(b">HH", 1, 2)
    asserts.assert_that(result).is_equal_to(_b)


def _test_unpack():
    # test bytes/string combination
    a, b = struct.unpack(b">HH", _b)
    asserts.eq(a, 1)
    asserts.eq(b, 2)

    # test string/bytes combination
    a, b = struct.unpack(">HH", _b)
    asserts.eq(a, 1)
    asserts.eq(b, 2)


_bz = b"\x00\x01"

def _test_unpack_from():
    # test string format string
    (a,) = struct.unpack_from('>H', _bz)
    asserts.eq(a, 1)

    # test bytes format string
    (a,) = struct.unpack_from(b'>H', _bz)
    asserts.eq(a, 1)


def _test_pack_into():
    # todo(mahmoudimus) uncomment when we have implemented array module
    # test string format string
    # result = array.array('b', [0, 0])
    # struct.pack_into('>H', result, 0, 0xABCD)
    # self.assertSequenceEqual(result, array.array('b', b"\xAB\xCD"))
    #
    # # test bytes format string
    # result = array.array('b', [0, 0])
    # struct.pack_into(b'>H', result, 0, 0xABCD)
    # self.assertSequenceEqual(result, array.array('b', b"\xAB\xCD"))

    # test bytearray
    result = bytearray(b" " * 2)
    r = struct.pack_into('>H', result, 0, 0xABCD)
    asserts.eq(r, None)
    asserts.eq(result, bytearray([171, 205]))


ISBIGENDIAN = sys.byteorder == "big"

integer_codes = "b", "B", "h", "H", "i", "I", "l", "L", "q", "Q", "n", "N"
byteorders = "", "@", "=", "<", ">", "!"


def iter_integer_formats(byteorders=byteorders):
    formats = []
    for code in integer_codes:
        for byteorder in byteorders:
            if byteorder not in ("", "@") and code in ("n", "N"):
                continue
            formats.append((code, byteorder,))
    return formats


def string_reverse(s):
    return s[::-1]


def bigendian_to_native(value):
    if ISBIGENDIAN:
        return value
    else:
        return string_reverse(value)


def StructTest_test_isbigendian():
    # print("xx:", struct.pack("=i", 1).hex())
    asserts.assert_that((struct.pack("=i", 1)[0] == 0)).is_equal_to(ISBIGENDIAN)


def StructTest_test_consistence():
    asserts.assert_fails(lambda: struct.calcsize("Z"), ".*?")

    sz = struct.calcsize("i")
    asserts.assert_that(sz * 3).is_equal_to(struct.calcsize("iii"))

    fmt = "cbxxxxxxhhhhiillffd?"
    fmt3 = "3c3b18x12h6i6l6f3d3?"
    sz = struct.calcsize(fmt)
    sz3 = struct.calcsize(fmt3)
    asserts.assert_that(sz * 3).is_equal_to(sz3)

    asserts.assert_fails(lambda: struct.pack("iii", 3), r"expected 3 items for \w+ \(got 1\)")
    asserts.assert_fails(lambda: struct.pack("i", 3, 3, 3), r"expected 1 items for \w+ \(got 3\)")
    asserts.assert_fails(lambda: struct.pack("i", "foo"), "element of type string, want int")
    asserts.assert_fails(lambda: struct.pack("P", "foo"), "element of type string, want int")
    asserts.assert_fails(lambda: struct.unpack("d", b"flap"), "unpack requires a buffer of 8 bytes")
    s = struct.pack("ii", 1, 2)
    asserts.assert_fails(lambda: struct.unpack("iii", s), "unpack requires a buffer of 12 bytes")
    asserts.assert_fails(lambda: struct.unpack("i", s), "unpack requires a buffer of 4 bytes")


def StructTest_test_transitiveness():
    c = b"a"
    b = 1
    h = 255
    i = 65535
    l = 65536
    q = 9223372036854775807
    f = 3.1415
    d = 3.1415
    t = True

    for prefix in ("", "@", "<", ">", "=", "!"):
        for format in ("xcbhilqfd?", "xcBHILQfd?"):
            format = prefix + format
            s = struct.pack(format, c, b, h, i, l, q, f, d, t)
            # print(s.hex())
            cp, bp, hp, ip, lp, qp, fp, dp, tp = struct.unpack(format, s)
            # print(cp, bp, hp, ip, lp, qp, fp, dp, tp)
            asserts.assert_that(cp).is_equal_to(c)
            asserts.assert_that(bp).is_equal_to(b)
            asserts.assert_that(hp).is_equal_to(h)
            asserts.assert_that(ip).is_equal_to(i)
            asserts.assert_that(lp).is_equal_to(l)
            asserts.assert_that(qp).is_equal_to(q)
            asserts.assert_that(int(100 * fp)).is_equal_to(int(100 * f))
            asserts.assert_that(int(100 * dp)).is_equal_to(int(100 * d))
            asserts.assert_that(tp).is_equal_to(t)


def StructTest_test_new_features():
    # Test some of the new features in detail
    # (format, argument, big-endian result, little-endian result, asymmetric)
    tests = [
        ("c", b"a", b"a", b"a", 0),
        ("xc", b"a", b"\0a", b"\0a", 0),
        ("cx", b"a", b"a\0", b"a\0", 0),
        ("s", b"a", b"a", b"a", 0),
        ("0s", b"helloworld", b"", b"", 1),
        ("1s", b"helloworld", b"h", b"h", 1),
        ("9s", b"helloworld", b"helloworl", b"helloworl", 1),
        ("10s", b"helloworld", b"helloworld", b"helloworld", 0),
        ("11s", b"helloworld", b"helloworld\0", b"helloworld\0", 1),
        (
            "20s",
            b"helloworld",
            b"helloworld" + 10 * b"\0",
            b"helloworld" + 10 * b"\0",
            1,
        ),
        ("b", 7, b"\7", b"\7", 0),
        ("b", -7, b"\371", b"\371", 0),
        ("B", 7, b"\7", b"\7", 0),
        ("B", 249, b"\371", b"\371", 0),
        ("h", 700, b"\002\274", b"\274\002", 0),
        ("h", -700, b"\375D", b"D\375", 0),
        ("H", 700, b"\002\274", b"\274\002", 0),
        ("H", 0x10000 - 700, b"\375D", b"D\375", 0),
        ("i", 70000000, b"\004,\035\200", b"\200\035,\004", 0),
        ("i", -70000000, b"\373\323\342\200", b"\200\342\323\373", 0),
        ("I", 70000000, b"\004,\035\200", b"\200\035,\004", 0),
        ("I", 0x100000000 - 70000000, b"\373\323\342\200", b"\200\342\323\373", 0),
        ("l", 70000000, b"\004,\035\200", b"\200\035,\004", 0),
        ("l", -70000000, b"\373\323\342\200", b"\200\342\323\373", 0),
        ("L", 70000000, b"\004,\035\200", b"\200\035,\004", 0),
        ("L", 0x100000000 - 70000000, b"\373\323\342\200", b"\200\342\323\373", 0),
        ("f", 2.0, b"@\000\000\000", b"\000\000\000@", 0),
        (
            "d",
            2.0,
            b"@\000\000\000\000\000\000\000",
            b"\000\000\000\000\000\000\000@",
            0,
        ),
        ("f", -2.0, b"\300\000\000\000", b"\000\000\000\300", 0),
        (
            "d",
            -2.0,
            b"\300\000\000\000\000\000\000\000",
            b"\000\000\000\000\000\000\000\300",
            0,
        )
    ]

    for fmt, arg, big, lil, asy in tests:
        for (xfmt, exp) in [
            (">" + fmt, big),
            ("!" + fmt, big),
            ("<" + fmt, lil),
            ("=" + fmt, ISBIGENDIAN and big or lil),
        ]:
            res = struct.pack(xfmt, arg)
            asserts.assert_that(res).is_equal_to(exp)
            asserts.assert_that(struct.calcsize(xfmt)).is_equal_to(len(res))
            rev = struct.unpack(xfmt, res)[0]
            if rev != arg:
                asserts.assert_that(asy).is_true()

    # For `?`, this test diverges from Python's test_struct.py (specifically,
    #   we seperated these test from above to specialize it for booleans).
    # Original test:
    # https://github.com/python/cpython/blob/b08c48e/Lib/test/test_struct.py#L82
    boolean_tests = [
       # (format, argument, big-endian result, little-endian result, asymmetric)
        ("?", 0, b"\0", b"\0", 1),
        ("?", 3, b"\1", b"\1", 1),
        ("?", True, b"\1", b"\1", 0),
        ("?", [], b"\0", b"\0", 1),
        ("?", (1,), b"\1", b"\1", 1),
    ]
    for fmt, arg, big, lil, asy in boolean_tests:
        for (xfmt, exp) in [
            (">" + fmt, big),
            ("!" + fmt, big),
            ("<" + fmt, lil),
            ("=" + fmt, ISBIGENDIAN and big or lil),
        ]:
            res = struct.pack(xfmt, arg)
            asserts.assert_that(res).is_equal_to(exp)
            asserts.assert_that(struct.calcsize(xfmt)).is_equal_to(len(res))
            rev = struct.unpack(xfmt, res)[0]
            # In python3, a boolean is subtype of int fixed at 0 and 1 (i.e.
            #  False == 0 and True == 1).
            # See github.com/bazelbuild/starlark/issues/30
            # This apparently was done for historical reasons.
            # In Starlark, this is _NOT_ the case  so as a result, this is
            # asymmetrical.
            # However, to keep the tests the same, we will specialize the equals
            if types.is_bool(rev) and types.is_int(arg):
                rev = 1 if rev else 0
            if rev != arg:
                asserts.assert_that(asy).is_true()


def StructTest_test_calcsize():
    expected_size = {
        "b": 1,
        "B": 1,
        "h": 2,
        "H": 2,
        "i": 4,
        "I": 4,
        "l": 4,
        "L": 4,
        "q": 8,
        "Q": 8,
    }

    # standard integer sizes
    for code, byteorder in iter_integer_formats(("=", "<", ">", "!")):
        format = byteorder + code
        size = struct.calcsize(format)
        asserts.assert_that(size).is_equal_to(expected_size[code])

    # native integer sizes
    native_pairs = "bB", "hH", "iI", "lL", "nN", "qQ"
    for format_pair in native_pairs:
        for byteorder in "", "@":
            signed_size = struct.calcsize(byteorder + format_pair[0])
            unsigned_size = struct.calcsize(byteorder + format_pair[1])
            asserts.assert_that(signed_size).is_equal_to(unsigned_size)

        # bounds for native integer sizes
    asserts.assert_that(struct.calcsize("b")).is_equal_to(1)
    asserts.assert_that(2).is_less_than_or_equal_to(struct.calcsize("h"))
    asserts.assert_that(4).is_less_than_or_equal_to(struct.calcsize("l"))
    asserts.assert_that(struct.calcsize("h")).is_less_than_or_equal_to(struct.calcsize("i"))
    asserts.assert_that(struct.calcsize("i")).is_less_than_or_equal_to(struct.calcsize("l"))
    asserts.assert_that(8).is_less_than_or_equal_to(struct.calcsize("q"))
    asserts.assert_that(struct.calcsize("l")).is_less_than_or_equal_to(struct.calcsize("q"))
    asserts.assert_that(struct.calcsize("n")).is_greater_than_or_equal_to(struct.calcsize("i"))
    asserts.assert_that(struct.calcsize("n")).is_greater_than_or_equal_to(struct.calcsize("P"))


def StructTest_test_nN_code():
    # n and N don't exist in standard sizes
    def assertStructError(func, *args, **kwargs):
        asserts.assert_fails(lambda: func(*args, **kwargs), r"bad char '[nN]{1}' in struct format")

    for code in "nN".elems():
        for byteorder in ("=", "<", ">", "!"):
            format = byteorder + code
            assertStructError(struct.calcsize, format)
            assertStructError(struct.pack, format, 0)
            assertStructError(struct.unpack, format, b"")


def StructTest_test_p_code():
    # Test p ("Pascal string") code.
    for code, input, expected, expectedback in [
        ("p", b"abc", b"\x00", b""),
        ("1p", b"abc", b"\x00", b""),
        ("2p", b"abc", b"\x01a", b"a"),
        ("3p", b"abc", b"\x02ab", b"ab"),
        ("4p", b"abc", b"\x03abc", b"abc"),
        ("5p", b"abc", b"\x03abc\x00", b"abc"),
        ("6p", b"abc", b"\x03abc\x00\x00", b"abc"),
        ("1000p", b"x" * 1000, b"\xff" + b"x" * 999, b"x" * 255),
    ]:
        got = struct.pack(code, input)
        asserts.assert_that(got).is_equal_to(expected)
        (got,) = struct.unpack(code, got)
        asserts.assert_that(got).is_equal_to(expectedback)


def StructTest_test_705836():
    # SF bug 705836.  "<f" and ">f" had a severe rounding bug, where a carry
    # from the low-order discarded bits could propagate into the exponent
    # field, causing the result to be wrong by a factor of 2.
    for base in range(1, 33):
        # smaller <- largest representable float less than base.
        delta = 0.5
        for _while_ in larky.while_true():
            if base - delta / 2.0 == base:
                break
            delta /= 2.0
        smaller = base - delta
        # Packing this rounds away a solid string of trailing 1 bits.
        packed = struct.pack("<f", smaller)
        unpacked = struct.unpack("<f", packed)[0]
        # This failed at base = 2, 4, and 32, with unpacked = 1, 2, and
        # 16, respectively.
        asserts.assert_that(base).is_equal_to(unpacked)
        bigpacked = struct.pack(">f", smaller)
        asserts.assert_that(bigpacked).is_equal_to(string_reverse(packed))
        unpacked = struct.unpack(">f", bigpacked)[0]
        asserts.assert_that(base).is_equal_to(unpacked)

    # TODO(mahmoudimus): support math.ldexp
    # # Largest finite IEEE single.
    # big = (1 << 24) - 1
    # big = math.ldexp(big, 127 - 23)
    # packed = struct.pack(">f", big)
    # unpacked = struct.unpack(">f", packed)[0]
    # asserts.assert_that(big).is_equal_to(unpacked)
    #
    # # The same, but tack on a 1 bit so it rounds up to infinity.
    # big = (1 << 25) - 1
    # big = math.ldexp(big, 127 - 24)
    # asserts.assert_fails(lambda: struct.pack(">f", big), ".*?OverflowError")


def StructTest_test_1530559():
    for code, byteorder in iter_integer_formats():
        format = byteorder + code
        asserts.assert_fails(lambda: struct.pack(format, 1.0), ".*?")
        asserts.assert_fails(lambda: struct.pack(format, 1.5), ".*?")
    asserts.assert_fails(lambda: struct.pack("P", 1.0), ".*?")
    asserts.assert_fails(lambda: struct.pack("P", 1.5), ".*?")


def StructTest_test_unpack_from():
    test_string = b"abcd01234"
    fmt = "4s"
    for cls in (bytes, bytearray):
        data = cls(test_string)
        asserts.assert_that(struct.unpack_from(fmt, data)).is_equal_to((b"abcd",))
        asserts.assert_that(struct.unpack_from(fmt, data, 2)).is_equal_to((b"cd01",))
        asserts.assert_that(struct.unpack_from(fmt, data, 4)).is_equal_to((b"0123",))
        for i in range(6):
            asserts.assert_that(struct.unpack_from(fmt, data, i)).is_equal_to((data[i : i + 4],))
        for i in range(6, len(test_string) + 1):
            asserts.assert_fails(lambda: struct.unpack_from(fmt, data, i), ".*?")

    # keyword arguments
    asserts.assert_that(struct.unpack_from(fmt, buffer=test_string, offset=2)).is_equal_to((b"cd01",))

def StructTest_test_pack_into():
    test_string = b"Reykjavik rocks, eow!"
    writable_buf = bytearray(b" " * 100)
    fmt = "21s"

    # Test without offset
    struct.pack_into(fmt, writable_buf, 0, test_string)
    from_buf = writable_buf[: len(test_string)]
    asserts.assert_that(from_buf).is_equal_to(test_string)

    # Test with offset.
    struct.pack_into(fmt, writable_buf, 10, test_string)
    from_buf = writable_buf[:len(test_string)+10]
    asserts.assert_that(from_buf).is_equal_to(test_string[:10] + test_string)

    # Go beyond boundaries.
    small_buf = bytearray(b" " * 10)
    asserts.assert_fails(
        lambda: struct.pack_into(fmt, small_buf, 0, test_string),
        ".*?")
    asserts.assert_fails(
        lambda: struct.pack_into(fmt, small_buf, 2, test_string),
        ".*?")

    # Test bogus offset (issue 3694)
    sb = small_buf
    asserts.assert_fails(lambda: struct.pack_into(b"", sb, None), ".*?")


def StructTest_test_pack_into_fn():
    test_string = b"Reykjavik rocks, eow!"
    writable_buf = bytearray(b" " * 100)
    fmt = "21s"
    pack_into = lambda *args: struct.pack_into(fmt, *args)

    # Test without offset.
    pack_into(writable_buf, 0, test_string)
    from_buf = writable_buf[: len(test_string)]
    asserts.assert_that(from_buf).is_equal_to(test_string)

    # Test with offset.
    pack_into(writable_buf, 10, test_string)
    from_buf = writable_buf[: len(test_string) + 10]
    asserts.assert_that(from_buf).is_equal_to(test_string[:10] + test_string)

    # Go beyond boundaries.
    small_buf = bytearray(b" " * 10)
    asserts.assert_fails(lambda: pack_into(small_buf, 0, test_string
    ), ".*?")
    asserts.assert_fails(lambda: pack_into(small_buf, 2, test_string
    ), ".*?")


def StructTest_test_unpack_with_buffer():
    # SF bug 1563759: struct.unpack doesn't support buffer protocol objects
    data1 = b"\x12\x34\x56\x78"
    data2 = bytearray(b"\x12\x34\x56\x78")  # XXX b'......XXXX......', 6, 4
    for data in [data1, data2]:
        (value,) = struct.unpack(">I", data)
        asserts.assert_that(value).is_equal_to(0x12345678)


def ExplodingBool_test_bool():

    for prefix in tuple("<>!=".elems()) + ("",):
        false = (), [], [], "", 0
        true = [1], "test", 5, -1, 0xFFFFFFFF + 1, 0xFFFFFFFF / 2

        falseFormat = prefix + "?" * len(false)
        packedFalse = struct.pack(falseFormat, *false)
        unpackedFalse = struct.unpack(falseFormat, packedFalse)

        trueFormat = prefix + "?" * len(true)
        packedTrue = struct.pack(trueFormat, *true)
        unpackedTrue = struct.unpack(trueFormat, packedTrue)

        asserts.assert_that(len(true)).is_equal_to(len(unpackedTrue))
        asserts.assert_that(len(false)).is_equal_to(len(unpackedFalse))

        for t in unpackedFalse:
            asserts.assert_that(t).is_false()
        for t in unpackedTrue:
            asserts.assert_that(t).is_true()

        packed = struct.pack(prefix + "?", 1)

        asserts.assert_that(len(packed)).is_equal_to(struct.calcsize(prefix + "?"))

        if len(packed) != 1:
            asserts.assert_that(prefix).is_false()

        # try:
        #     struct.pack(prefix + "?", ExplodingBool())
        # except OSError:
        #     pass
        # else:
        #     fail(
        #         "Expected OSError: struct.pack(%r, " +
        #         "ExplodingBool())" % (prefix + "?")
        #     )

    for c in [b"\x01", b"\x7f", b"\xff", b"\x0f", b"\xf0"]:
        asserts.assert_that(struct.unpack(">?", c)[0]).is_true()


def TestStruct_test_trailing_counter():
    store = bytearray(b" " * 100)

    # format lists containing only count spec should result in an error
    asserts.assert_fails(lambda: struct.pack("12345"), ".*?")
    asserts.assert_fails(lambda: struct.unpack("12345", b""), ".*?")
    asserts.assert_fails(lambda: struct.pack_into("12345", store, 0), ".*?")
    asserts.assert_fails(lambda: struct.unpack_from("12345", store, 0), ".*?")

    # Format lists with trailing count spec should result in an error
    asserts.assert_fails(lambda: struct.pack("c12345", "x"), ".*?")
    asserts.assert_fails(lambda: struct.unpack("c12345", b"x"), ".*?")
    asserts.assert_fails(lambda: struct.pack_into("c12345", store, 0, "x"), ".*?")
    asserts.assert_fails(lambda: struct.unpack_from("c12345", store, 0), ".*?")

    # Mixed format tests
    asserts.assert_fails(lambda: struct.pack("14s42", "spam and eggs"), ".*?")
    asserts.assert_fails(lambda: struct.unpack("14s42", b"spam and eggs"), ".*?")
    asserts.assert_fails(lambda: struct.pack_into("14s42", store, 0, "spam and eggs"
    ), ".*?")
    asserts.assert_fails(lambda: struct.unpack_from("14s42", store, 0), ".*?")


def TestStruct_test_boundary_error_message():
    regex1 = (
        r"pack_into requires a buffer of at least 6 "
        + r"bytes for packing 1 bytes at offset 5 "
        + r"\(actual buffer size is 1\)"
    )
    def _larky_125241448():
        struct.pack_into("b", bytearray(b" " * 1), 5, 1)
    asserts.assert_fails(lambda: _larky_125241448(), regex1)

    regex2 = (
        r"unpack_from requires a buffer of at least 6 "
        + r"bytes for unpacking 1 bytes at offset 5 "
        + r"\(actual buffer size is 1\)"
    )
    def _larky_3508643095():
        struct.unpack_from("b", bytearray(b" " * 1), 5)
    asserts.assert_fails(lambda: _larky_3508643095(), regex2)


def TestStruct_test_boundary_error_message_with_negative_offset():
    byte_list = bytearray(b" " * 10)

    def _larky_2595284807():
        struct.pack_into("<I", byte_list, -2, 123)
    asserts.assert_fails(lambda: _larky_2595284807(), ".*?.*no space to pack 4 bytes at offset -2")

    def _larky_1108256932():
        struct.pack_into("<B", byte_list, -11, 123)
    asserts.assert_fails(lambda: _larky_1108256932(), ".*?.*offset -11 out of range for 10-byte buffer")

    def _larky_290550463():
        struct.unpack_from("<I", byte_list, -2)
    asserts.assert_fails(lambda: _larky_290550463(), ".*?.*not enough data to unpack 4 bytes at offset -2")

    def _larky_3846392003():
        struct.unpack_from("<B", byte_list, -11)
    asserts.assert_fails(lambda: _larky_3846392003(), ".*?.*offset -11 out of range for 10-byte buffer")


def StructTest_test_issue35714():
    # Embedded null characters should not be allowed in format strings.
    for s in "\0", "2\0i", b"\0":
        asserts.assert_fails(lambda: struct.calcsize(s), ".*?.*embedded null character")


def UnpackIteratorTest_test_half_float():
    # Little-endian examples from:
    # http://en.wikipedia.org/wiki/Half_precision_floating-point_format
    format_bits_float__cleanRoundtrip_list = [
        (b'\x00\x3c', 1.0),
        (b'\x00\xc0', -2.0),
        (b'\xff\x7b', 65504.0), #  (max half precision)
        (b'\x00\x04', pow(2, -14)), # ~= 6.10352 * 10**-5 (min pos normal)
        (b'\x01\x00', pow(2, -24)), # ~= 5.96046 * 10**-8 (min pos subnormal)
        (b'\x00\x00', 0.0),
        (b'\x00\x80', -0.0),
        (b'\x00\x7c', float('+inf')),
        (b'\x00\xfc', float('-inf')),
        (b'\x55\x35', 0.333251953125), # ~= 1/3
    ]

    for le_bits, f in format_bits_float__cleanRoundtrip_list:
        be_bits = le_bits[::-1]
        asserts.assert_that(f).is_equal_to(struct.unpack('<e', le_bits)[0])
        asserts.assert_that(le_bits).is_equal_to(struct.pack('<e', f))
        asserts.assert_that(f).is_equal_to(struct.unpack('>e', be_bits)[0])
        asserts.assert_that(be_bits).is_equal_to(struct.pack('>e', f))
        if sys.byteorder == 'little':
            asserts.assert_that(f).is_equal_to(struct.unpack('e', le_bits)[0])
            asserts.assert_that(le_bits).is_equal_to(struct.pack('e', f))
        else:
            asserts.assert_that(f).is_equal_to(struct.unpack('e', be_bits)[0])
            asserts.assert_that(be_bits).is_equal_to(struct.pack('e', f))

    # TODO(mahmoudimus): support math.isnan
    #
    # # Check for NaN handling:
    # format_bits__nan_list = [
    #     ('<e', b'\x01\xfc'),
    #     ('<e', b'\x00\xfe'),
    #     ('<e', b'\xff\xff'),
    #     ('<e', b'\x01\x7c'),
    #     ('<e', b'\x00\x7e'),
    #     ('<e', b'\xff\x7f'),
    # ]
    #
    # for formatcode, bits in format_bits__nan_list:
    #     asserts.assert_that(math.isnan(struct.unpack('<e', bits)[0])).is_true()
    #     asserts.assert_that(math.isnan(struct.unpack('>e', bits[::-1])[0])).is_true()
    #
    # # Check that packing produces a bit pattern representing a quiet NaN:
    # # all exponent bits and the msb of the fraction should all be 1.
    # packed = struct.pack('<e', math.nan)
    # asserts.assert_that(packed[1] & 0x7e).is_equal_to(0x7e)
    # packed = struct.pack('<e', -math.nan)
    # asserts.assert_that(packed[1] & 0x7e).is_equal_to(0x7e)

    # Checks for round-to-even behavior
    format_bits_float__rounding_list = [
        ('>e', b'\x00\x01', pow(2.0, -25) + pow(2.0, -35)), # Rounds to minimum subnormal
        ('>e', b'\x00\x00', pow(2.0, -25)), # Underflows to zero (nearest even mode)
        ('>e', b'\x00\x00', pow(2.0, -26)), # Underflows to zero
        ('>e', b'\x03\xff', pow(2.0, -14) - pow(2.0, -24)), # Largest subnormal.
        ('>e', b'\x03\xff', pow(2.0, -14) - pow(2.0, -25) - pow(2.0, -65)),
        ('>e', b'\x04\x00', pow(2.0, -14) - pow(2.0, -25)),
        ('>e', b'\x04\x00', pow(2.0, -14)), # Smallest normal.
        ('>e', b'\x3c\x01', 1.0+pow(2.0, -11) + pow(2.0, -16)), # rounds to 1.0+2**(-10)
        ('>e', b'\x3c\x00', 1.0+pow(2.0, -11)), # rounds to 1.0 (nearest even mode)
        ('>e', b'\x3c\x00', 1.0+pow(2.0, -12)), # rounds to 1.0
        ('>e', b'\x7b\xff', 65504), # largest normal
        ('>e', b'\x7b\xff', 65519), # rounds to 65504
        ('>e', b'\x80\x01', -pow(2.0, -25) - pow(2.0, -35)), # Rounds to minimum subnormal
        ('>e', b'\x80\x00', -pow(2.0, -25)), # Underflows to zero (nearest even mode)
        ('>e', b'\x80\x00', -pow(2.0, -26)), # Underflows to zero
        ('>e', b'\xbc\x01', -1.0-pow(2.0, -11) - pow(2.0, -16)), # rounds to 1.0+2**(-10)
        ('>e', b'\xbc\x00', -1.0-pow(2.0, -11)), # rounds to 1.0 (nearest even mode)
        ('>e', b'\xbc\x00', -1.0-pow(2.0, -12)), # rounds to 1.0
        ('>e', b'\xfb\xff', -65519), # rounds to 65504
    ]

    for formatcode, bits, f in format_bits_float__rounding_list:
        asserts.assert_that(bits).is_equal_to(struct.pack(formatcode, f))

    # This overflows, and so raises an error
    format_bits_float__roundingError_list = [
        # Values that round to infinity.
        ('>e', 65520.0),
        ('>e', 65536.0),
        ('>e', 1e300),
        ('>e', -65520.0),
        ('>e', -65536.0),
        ('>e', -1e300),
        ('<e', 65520.0),
        ('<e', 65536.0),
        ('<e', 1e300),
        ('<e', -65520.0),
        ('<e', -65536.0),
        ('<e', -1e300),
    ]

    for formatcode, f in format_bits_float__roundingError_list:
        asserts.assert_fails(lambda: struct.pack(formatcode, f), ".*?OverflowError")

    # Double rounding
    format_bits_float__doubleRoundingError_list = [
        ('>e', b'\x67\xff', 0x1ffdffffff * pow(2, -26)), # should be 2047, if double-rounded 64>32>16, becomes 2048
    ]

    for formatcode, bits, f in format_bits_float__doubleRoundingError_list:
        asserts.assert_that(bits).is_equal_to(struct.pack(formatcode, f))


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_pack))
    _suite.addTest(unittest.FunctionTestCase(_test_unpack))
    _suite.addTest(unittest.FunctionTestCase(_test_unpack_from))
    _suite.addTest(unittest.FunctionTestCase(_test_pack_into))
    _suite.addTest(unittest.FunctionTestCase(StructTest_test_isbigendian))
    _suite.addTest(unittest.FunctionTestCase(StructTest_test_transitiveness))
    _suite.addTest(unittest.FunctionTestCase(StructTest_test_consistence))
    _suite.addTest(unittest.FunctionTestCase(StructTest_test_new_features))
    _suite.addTest(unittest.FunctionTestCase(StructTest_test_calcsize))
    _suite.addTest(unittest.FunctionTestCase(StructTest_test_nN_code))
    _suite.addTest(unittest.FunctionTestCase(StructTest_test_p_code))
    _suite.addTest(unittest.FunctionTestCase(StructTest_test_705836))
    _suite.addTest(unittest.FunctionTestCase(StructTest_test_1530559))
    _suite.addTest(unittest.FunctionTestCase(StructTest_test_unpack_from))
    _suite.addTest(unittest.FunctionTestCase(StructTest_test_pack_into))
    _suite.addTest(unittest.FunctionTestCase(StructTest_test_pack_into_fn))
    _suite.addTest(unittest.FunctionTestCase(StructTest_test_unpack_with_buffer))
    _suite.addTest(unittest.FunctionTestCase(ExplodingBool_test_bool))
    _suite.addTest(unittest.FunctionTestCase(TestStruct_test_trailing_counter))
    _suite.addTest(unittest.FunctionTestCase(TestStruct_test_boundary_error_message))
    _suite.addTest(unittest.FunctionTestCase(TestStruct_test_boundary_error_message_with_negative_offset))
    _suite.addTest(unittest.FunctionTestCase(StructTest_test_issue35714))
    _suite.addTest(unittest.FunctionTestCase(UnpackIteratorTest_test_half_float))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())