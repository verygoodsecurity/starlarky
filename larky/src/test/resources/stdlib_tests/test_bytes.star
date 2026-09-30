"""Unit tests for test_bytes.star"""

load("@stdlib//larky", "larky")
load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@stdlib//builtins", "builtins")
load("@stdlib//types", "types")
load("@stdlib//codecs", "codecs")
load("@vendor//escapes", "escapes")


# Tests of 'bytes' (immutable byte strings).
b = builtins.b


# bytes(string) -- UTF-k to UTF-8 transcoding with U+FFFD replacement
# A bit on replacement:
#
# The byte sequence [239, 191, 189] is the UTF-8 encoding of the Unicode
# character U+FFFD or 'replacement character'. This occurs because input
# may not be a valid character using UTF-8 encoding. However, note that it
# might be a valid character using a different encoding, such as "iso-8859-1".
hello = builtins.bytes("hello, 世界")
goodbye = builtins.bytes("goodbye")
empty = builtins.bytes("")

nonprinting = builtins.bytes(escapes.CEscape()
                             .raw("\t\n") # TAB, NEWLINE,
                             .x("7f") # DEL,
                             .u("200D")) # ZERO_WIDTH_JOINER

sliced = builtins.bytes("hello, 世界")[:-1]
# assert.eq(bytes("hello, 世界"[:-1]), b"hello, 世��")
hello_sliced = "hello, 世界"[:-1]


def _test_bytes_are_ints():
    # can always convert a bytes object into a list of integers using list(b).
    asserts.assert_that(list(sliced.elems())).is_equal_to([104, 101, 108, 108, 111, 44, 32, 228, 184, 150, 231, 149])
    # int in bytes
    asserts.assert_that(97 in b("abc")).is_equal_to(True)  # 97='a'
    asserts.assert_that(100 in b("abc")).is_equal_to(False) # 100='d'
    asserts.assert_fails(lambda: 256 in b("abc"), "int in bytes: 256 out of range")
    asserts.assert_fails(lambda: -1 in b("abc"), "int in bytes: -1 out of range")


def _test_bytes_vs_string():
    # contrasts with text strings, where both indexing and slicing will
    # produce a string of length 1
    simplestr = "hello"
    asserts.assert_true(all([
        types.is_string(simplestr[0]),
        simplestr[0] == "h",
        simplestr[0:1] == simplestr[0],
        len(simplestr[0]) == 1,
    ]))
    # for bytes
    # b[0] is the first byte's int, as in python; ord() of it still works
    asserts.assert_that(sliced[0]).is_equal_to(104)
    asserts.assert_that(ord(sliced[0])).is_equal_to(104)
    # while b[0:1] will be a bytes object of length 1.
    asserts.assert_that(sliced[0:1]).is_equal_to(b'h')
    # asserts.assert_true(all([
    #
    #     sliced[0] == 104,
    #
    #     sliced[0:1] == b("h"),
    # ]))
    asserts.assert_that(
        codecs.decode(sliced, encoding='utf-8', errors='replace')
    ).is_equal_to(r"hello, 世\xe7\x95")


def _test_bytes_construction():
    # # bytes(iterable of int) -- construct from numeric byte values
    asserts.assert_fails(lambda: builtins.bytes(1),
                         "want string, bytes.*or iterable")
    asserts.assert_that(builtins.bytes([65, 66, 67])).is_equal_to(b("ABC"))
    asserts.assert_that(builtins.bytes((65, 66, 67))).is_equal_to(b("ABC"))
    asserts.assert_that(builtins.bytes(range(65, 68))).is_equal_to(b("ABC"))
    asserts.assert_that(builtins.bytes(b("ABC").elems())).is_equal_to(b("ABC"))
    asserts.assert_that(builtins.bytes({65: None, 66: None})).is_equal_to(b("AB"))
    asserts.assert_that(builtins.bytes(())).is_equal_to(b(""))
    asserts.assert_that(builtins.bytes(builtins.bytearray(b("AB")))).is_equal_to(b("AB"))
    asserts.assert_that(builtins.bytearray((65, 66))).is_equal_to(builtins.bytearray(b("AB")))
    asserts.assert_that(builtins.bytearray(range(65, 67))).is_equal_to(builtins.bytearray(b("AB")))
    asserts.assert_fails(lambda: builtins.bytes((65, "B")),
                 "at index 1 of tuple, got element of type string, want int")
    asserts.assert_fails(lambda: builtins.bytes(range(250, 260)),
                 "256 out of range .+want value in unsigned 8-bit range")
    asserts.assert_that(builtins.bytes([0xf0, 0x9f, 0x98, 0xbf])).is_equal_to(b("😿"))
    asserts.assert_fails(lambda: builtins.bytes([300]),
                  "300 out of range .+want value in unsigned 8-bit range")
    asserts.assert_fails(lambda: builtins.bytes([b("a")]),
                 "got element of type bytes, want int")

    # literals .... not really b() simulates a literal...
    asserts.assert_that(b("hello, 世界")).is_equal_to(hello)
    asserts.assert_that(b("goodbye")).is_equal_to(goodbye)
    asserts.assert_that(b("")).is_equal_to(empty)
    asserts.assert_that(b(escapes.CEscape().raw("\t\n").x("7f").u("200D"))).is_equal_to(nonprinting)
    asserts.assert_that("abc").is_not_equal_to(b("abc"))


# type
def _test_byte_types():
    asserts.assert_that(type(hello)).is_equal_to("bytes")


# len
def _test_byte_len():
    asserts.assert_that(len(hello)).is_equal_to(13)
    asserts.assert_that(len(goodbye)).is_equal_to(7)
    asserts.assert_that(len(empty)).is_equal_to(0)
    asserts.assert_that(len(b("A"))).is_equal_to(1)
    asserts.assert_that(len(b("Ѐ"))).is_equal_to(2)
    asserts.assert_that(len(b("世"))).is_equal_to(3)
    asserts.assert_that(len(b("😿"))).is_equal_to(4)


def _test_truthiness():
    # truth
    asserts.assert_true(hello)
    asserts.assert_true(goodbye)
    asserts.assert_true(not empty)


def _test_str_does_transcoding():
    # starlark spec says that str(bytes) does UTF-8 to UTF-k transcoding.
    # where K matches the encoding of the host language:
    #   golang + rust = utf-8 to utf-8
    #   java = utf-8 to utf-16
    asserts.assert_that(str(hello)).is_equal_to("hello, 世界")
    asserts.assert_that(str(hello[:-1])).is_equal_to("hello, 世��")
    asserts.assert_that(str(goodbye)).is_equal_to("goodbye")
    asserts.assert_that(str(empty)).is_equal_to("")
    asserts.assert_that(str(nonprinting)).is_equal_to("\t\n\x7f\u200d")
    f = builtins.bytes([237, 176, 128])
    # print(repr(f))
    asserts.assert_that(
        str(f)
        # UTF-8 encoding of unpaired surrogate => U+FFFD * 3
    ).is_equal_to("���")


def _test_repr_for_bytes():
    # repr
    asserts.assert_that(repr(hello)).is_equal_to(r'b"hello, 世界"')
    asserts.assert_that(repr(goodbye)).is_equal_to('b"goodbye"')
    asserts.assert_that(repr(empty)).is_equal_to('b""')
    # assert.eq(repr(nonprinting), 'b"\\t\\n\\x7f\\u200d"')
    asserts.assert_that(repr(nonprinting)).is_equal_to('b"\\t\\n\\x7f\\u200d"')
    asserts.assert_that(repr(hello[:-1])).is_equal_to(r'b"hello, 世\xe7\x95"')  # (incomplete UTF-8 encoding )

# equality
def _test_equality():
    asserts.assert_that(hello).is_equal_to(hello)
    asserts.assert_that(hello).is_not_equal_to(goodbye)
    asserts.assert_that(b("goodbye")).is_equal_to(goodbye)


# ordered comparison
def _test_ordered_comparison():
    asserts.assert_that(b("abc")).is_less_than(b("abd"))
    asserts.assert_that(b("abc")).is_less_than(b("abcd"))

    asserts\
        .assert_that(b(escapes.CEscape().x("7f")))\
        .is_less_than(b(escapes.CEscape().x("80"))) # bytes compare as uint8, not int8


def _test_bytes_are_dict_hashable():
    # bytes are dict-hashable
    # -> # decode bytes into string!
    #dict = {_decode(hello): 1, _decode(goodbye): 2}
    dict = {hello: 1, goodbye: 2}
    dict[b("goodbye")] = 3
    asserts.assert_that(len(dict)).is_equal_to(2)
    asserts.assert_that(dict[goodbye]).is_equal_to(3)


def _test_byte_hashing():
    # go: hash(bytes) is unsigned 32-bit FNV-1a.
    # java: hash(bytes) is signed 32-bit FNV-1a
    def _as_unsigned(n):
        return n + pow(2, 32) if n < 0 else n
    asserts.assert_that(_as_unsigned(hash(b("")))).is_equal_to(0x811c9dc5)
    asserts.assert_that(_as_unsigned(hash(b("a")))).is_equal_to(0xe40c292c)
    asserts.assert_that(_as_unsigned(hash(b("ab")))).is_equal_to(0x4d2505ca)
    asserts.assert_that(_as_unsigned(hash(b("abc")))).is_equal_to(0x1a47e90b)


def _test_indexing():
    # indexing
    asserts.assert_that(goodbye[0]).is_equal_to(b("g")[0])
    asserts.assert_that(goodbye[-1]).is_equal_to(b("e")[0])
    asserts.assert_fails(lambda: goodbye[100], "out of range")


def _test_slicing():
    # slicing
    asserts.assert_that(goodbye[:4]).is_equal_to(b("good"))
    asserts.assert_that(goodbye[4:]).is_equal_to(b("bye"))
    asserts.assert_that(goodbye[::2]).is_equal_to(b("gobe"))
    asserts.assert_that(goodbye[3:4]).is_equal_to(b("d"))  # special case: len=1
    asserts.assert_that(goodbye[4:4]).is_equal_to(b(""))  # special case: len=0


def _test_bytes_in_operator():
    # bytes in bytes
    # asserts.assert_that(b("bc") in b("abcd")).is_equal_to(True)
    # asserts.assert_that(b("bc") in b("dcab")).is_equal_to(False)
    asserts.assert_fails(
        lambda: "bc" in b("dcab"),
        "requires bytes or int as left operand, not string"
    )


def _test_byte_ord():
    # ord
    # TODO(adonovan): specify
    asserts.assert_that(ord(b("a"))).is_equal_to(97)
    asserts.assert_fails(lambda: ord(b("ab")), "ord: bytes has length 2, want 1")
    asserts.assert_fails(lambda: ord(b("")), "ord: bytes has length 0, want 1")


def _test_repeat():
    # repeat (bytes * int)
    asserts.assert_that(goodbye * 3).is_equal_to(b("goodbyegoodbyegoodbye"))
    asserts.assert_that(3 * goodbye).is_equal_to(b("goodbyegoodbyegoodbye"))


def _test_elems():
    # elems() returns an iterable value over 1-byte substrings.
    asserts.assert_that(type(hello.elems())).is_equal_to("bytes.elems")
    asserts.assert_that(list(hello.elems())).is_equal_to([104, 101, 108, 108, 111, 44, 32, 228, 184, 150, 231, 149,140])
    asserts.assert_that(list(goodbye.elems())).is_equal_to([103, 111, 111, 100, 98, 121, 101])
    asserts.assert_that(list(empty.elems())).is_equal_to([])
    asserts.assert_that(builtins.bytes([104, 101, 108, 108, 111, 44, 32, 228, 184, 150, 231, 149, 140])).is_equal_to(hello)
    asserts.assert_that(str(hello.elems())).is_equal_to("b\"hello, 世界\".elems()")
    asserts.assert_that(builtins.bytes(hello.elems())).is_equal_to(hello) # bytes(iterable) is dual to bytes.elems()


# x[i] = ...
def f():
    b("abc")[1] = b("B")


def _test_bytes_are_immutable():
    asserts.assert_fails(f, "can only assign an .*, not in a 'bytes'")


def _test_bytes_join():
    # from the bytes.join docstring
    # Example: b'.'.join([b'ab', b'pq', b'rs']) -> b'ab.pq.rs'.
    expected = bytes('ab.pq.rs', encoding='utf-8')
    sut = bytes('.', encoding='utf-8').join([b('ab'), b('pq'), b('rs')])
    asserts.assert_that(sut).is_equal_to(expected)
    expected = bytearray('ab.pq.rs', encoding='utf-8')
    sut = bytearray([0x2e]).join([bytes([0x61,0x62]), bytes([0x70,0x71]), bytes([0x72,0x73])])
    asserts.assert_that(sut).is_equal_to(expected)

def _test_skip_unescape_encode():
    bytes = b"{\"field\": \"\\n  function(a):\\n    a=dosomething()\\n    return a\\n\\n\",\"simple_field\":{\"input\":\"123344444\"}}"
    text = codecs.decode(bytes,'ISO-8859-1')
    asserts.assert_that(codecs.encode(text,'ISO-8859-1','error')).is_not_equal_to(bytes)
    asserts.assert_that(codecs.encode(text,'ISO-8859-1','error', False)).is_equal_to(bytes)


# TODO(adonovan): the specification is not finalized in many areas:
# - chr, ord functions
# - encoding/decoding bytes to string. (NOTE mahmoudimus - I added this).
# - methods: find, index, split, etc.
#
# Summary of string operations (put this in spec).
#
# string to number:
# - bytes[i]  returns numeric value of ith byte.
# - ord(string)  returns numeric value of sole code point in string.
# - ord(string[i])  is not a useful operation: fails on non-ASCII; see below.
#   Q. Perhaps ord should return the first (not sole) code point? Then it becomes a UTF-8 decoder.
#      Perhaps ord(string, index=int) should apply the index and relax the len=1 check.
# - string.codepoint()  iterates over 1-codepoint substrings.
# - string.codepoint_ords()  iterates over numeric values of code points in string.
# - string.elems()  iterates over 1-element (UTF-k code) substrings.
# - string.elem_ords()  iterates over numeric UTF-k code values.
# - string.elem_ords()[i]  returns numeric value of ith element (UTF-k code).
# - string.elems()[i]  returns substring of a single element (UTF-k code).
# - int(string)  parses string as decimal (or other) numeric literal.
#
# number to string:
# - chr(int) returns string, UTF-k encoding of Unicode code point (like Python).
#   Redundant with '%c' % int (which Python2 calls 'unichr'.)
# - bytes(chr(int)) returns byte string containing UTF-8 encoding of one code point.
# - bytes([int]) returns 1-byte string (with regrettable list allocation).
# - str(int) - format number as decimal.


def _test_bytearray_results_are_independent_bytearrays():
    # As in Python, a bytearray method returns a new bytearray with its own bytes. They used to be
    # bytes sharing the bytearray's buffer, so changing the bytearray changed a "bytes" value,
    # even one already used as a dict key.
    ba = builtins.bytearray(b("abc"))
    sliced = ba[:]
    stepped = ba[::2]
    upper = ba.upper()
    ba[0] = 120
    asserts.assert_that(sliced).is_equal_to(b("abc"))
    asserts.assert_that(stepped).is_equal_to(b("ac"))
    asserts.assert_that(upper).is_equal_to(b("ABC"))
    asserts.assert_that(type(sliced)).is_equal_to("bytearray")
    asserts.assert_fails(lambda: {sliced: 1}, ".*unhashable type: 'bytearray'")

    ba = builtins.bytearray(b("  ab  "))
    stripped = ba.strip()
    ba[2] = 122
    asserts.assert_that(stripped).is_equal_to(b("ab"))
    asserts.assert_that(type(stripped)).is_equal_to("bytearray")

    ba = builtins.bytearray(b("a,b"))
    parts = ba.split(b(","))
    ba[0] = 90
    asserts.assert_that(parts).is_equal_to([b("a"), b("b")])
    asserts.assert_that([type(p) for p in parts]).is_equal_to(["bytearray", "bytearray"])

    # An unchanged result is still a copy.
    ba = builtins.bytearray(b("abc"))
    same = ba.removeprefix(b("zz"))
    same.append(33)
    asserts.assert_that(ba).is_equal_to(b("abc"))

    # bytearray(bytearray) copies too.
    a = builtins.bytearray(b("ab"))
    c = builtins.bytearray(a)
    c.append(99)
    asserts.assert_that(a).is_equal_to(b("ab"))
    asserts.assert_that(c).is_equal_to(b("abc"))

    # bytes results are unaffected.
    asserts.assert_that(type(b("xyz")[:])).is_equal_to("bytes")


def _test_bytes_and_bytearray_order_by_content():
    # As in Python, bytes and bytearray compare by content, in either order.
    asserts.assert_that(b"\x80" > builtins.bytearray(b"\x7f")).is_true()
    asserts.assert_that(builtins.bytearray(b("a")) < b("b")).is_true()
    asserts.assert_that(b("a") < builtins.bytearray(b("a"))).is_false()
    asserts.assert_that(b("a") <= builtins.bytearray(b("a"))).is_true()
    asserts.assert_that(
        sorted([builtins.bytearray(b("b")), b("a"), builtins.bytearray(b("c"))])
    ).is_equal_to([b("a"), b("b"), b("c")])


def _testsuite():
    _suite = unittest.TestSuite()

    _suite.addTest(unittest.FunctionTestCase(_test_elems))
    _suite.addTest(unittest.FunctionTestCase(_test_bytes_in_operator))
    _suite.addTest(unittest.FunctionTestCase(_test_byte_hashing))
    _suite.addTest(unittest.FunctionTestCase(_test_repeat))
    _suite.addTest(unittest.FunctionTestCase(_test_str_does_transcoding))
    _suite.addTest(unittest.FunctionTestCase(_test_bytes_are_ints))
    _suite.addTest(unittest.FunctionTestCase(_test_bytes_vs_string))
    _suite.addTest(unittest.FunctionTestCase(_test_bytes_construction))
    _suite.addTest(unittest.FunctionTestCase(_test_byte_types))
    _suite.addTest(unittest.FunctionTestCase(_test_byte_len))
    _suite.addTest(unittest.FunctionTestCase(_test_truthiness))
    _suite.addTest(unittest.FunctionTestCase(_test_repr_for_bytes))
    _suite.addTest(unittest.FunctionTestCase(_test_equality))
    _suite.addTest(unittest.FunctionTestCase(_test_ordered_comparison))
    _suite.addTest(unittest.FunctionTestCase(_test_bytes_are_dict_hashable))
    _suite.addTest(unittest.FunctionTestCase(_test_indexing))
    _suite.addTest(unittest.FunctionTestCase(_test_slicing))
    _suite.addTest(unittest.FunctionTestCase(_test_bytes_are_immutable))
    _suite.addTest(unittest.FunctionTestCase(_test_bytes_join))
    _suite.addTest(unittest.FunctionTestCase(_test_skip_unescape_encode))
    _suite.addTest(unittest.FunctionTestCase(_test_bytearray_results_are_independent_bytearrays))
    _suite.addTest(unittest.FunctionTestCase(_test_bytes_and_bytearray_order_by_content))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
