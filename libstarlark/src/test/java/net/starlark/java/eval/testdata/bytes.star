
def assert_ne(arg1, arg2):
    assert_(arg1 != arg2, "%s == %s!" % (arg1, arg2))


def assert_true(arg1):
    assert_(arg1, "bool(%s) is falsy" % arg1)


def assert_lt(arg1, arg2):
    assert_(arg1 < arg2, "%s >= %s" % (arg1, arg2))


# bytes(string) -- UTF-k to UTF-8 transcoding with U+FFFD replacement
# The result is a bytes whose elements are the UTF-8 encoding of the string.
# Each element of the string that is not part of a valid encoding of a
# code point is replaced by the UTF-8 encoding of the
# replacement character, U+FFFD.
hello = bytes("hello, 世界")
goodbye = bytes("goodbye")
empty = bytes("")
nonprinting = bytes("\t\n\x7F\u200D")  # TAB, NEWLINE, DEL, ZERO_WIDTH_JOINER
# in Starlark, [:-1] will cut off the last UTF-K code unit
# (e.g. byte in Go, char in Java), yielding an invalid string
# ("hello, 世" plus one half of the encoding of 😿). This test ensures that
# each invalid byte in a text string is replaced by U+FFFD.
assert_eq(bytes("hello, 世😿"[:-1]), b"hello, 世�")
assert_eq(bytes("hello 😃"[:-1]), b"hello \uFFFD")


# bytes(iterable of int) -- construct from numeric byte values
assert_eq(bytes([65, 66, 67]), b"ABC")
assert_eq(bytes((65, 66, 67)), b"ABC")
assert_eq(bytes([0xf0, 0x9f, 0x98, 0xbf]), b"😿")
assert_fails(lambda: bytes([300]),
             "at index 0, 300 out of range .want value in unsigned 8-bit range")
assert_fails(lambda: bytes([b"a"]),
             "at index 0 .* got element of type bytes, want int")
assert_fails(lambda: bytes(1), "want string, bytes, or iterable of ints")

# literals
assert_eq(b"hello, 世界", hello)
assert_eq(b"goodbye", goodbye)
assert_eq(b"", empty)
assert_eq(b"\t\n\x7F\u200D", nonprinting)
assert_ne("abc", b"abc")
assert_eq(b"\012\xff\u0400\U0001F63F", b"\n\xffЀ😿") # see scanner tests for more
assert_eq(rb"\r\n\t", b"\\r\\n\\t") # raw

# type
assert_eq(type(hello), "bytes")

# len
assert_eq(len(hello), 13)
assert_eq(len(goodbye), 7)
assert_eq(len(empty), 0)
assert_eq(len(b"A"), 1)
assert_eq(len(b"Ѐ"), 2)
assert_eq(len(b"世"), 3)
assert_eq(len(b"😿"), 4)

# truth
assert_true(hello)
assert_true(goodbye)
assert_true(not empty)

# str(bytes) does UTF-8 to UTF-k transcoding.
# TODO(adonovan): specify.
assert_eq(str(hello), "hello, 世界")
assert_eq(str(hello[:-1]), "hello, 世��")  # incomplete UTF-8 encoding => U+FFFD
assert_eq(str(goodbye), "goodbye")
assert_eq(str(empty), "")
assert_eq(str(nonprinting), "\t\n\x7f\u200d")
assert_eq(str(b"\xED\xB0\x80"), "���") # UTF-16 encoding of unpaired surrogate => U+FFFD * 3

# repr
assert_eq(repr(hello), r'b"hello, 世界"')
assert_eq(repr(hello[:-1]), r'b"hello, 世\xe7\x95"')  # (incomplete UTF-8 encoding )
assert_eq(repr(goodbye), 'b"goodbye"')
assert_eq(repr(empty), 'b""')
assert_eq(repr(nonprinting), 'b"\\t\\n\\x7f\\u200d"')

# equality
assert_eq(hello, hello)
assert_ne(hello, goodbye)
assert_eq(b"goodbye", goodbye)

# ordered comparison
assert_lt(b"abc", b"abd")
assert_lt(b"abc", b"abcd")
assert_lt(b"\x7f", b"\x80") # bytes compare as uint8, not int8
assert_lt(b"A"[0], b"\xe4"[0]) # and so do their elements
assert_lt(b"\xe4"[0], b"\xff"[0])
assert_lt(b"\x00"[0], b"A"[0])
assert_eq(sorted([b"\xe4"[0], b"A"[0], b"\xff"[0], b"\x00"[0]]), [0, 0x41, 0xe4, 0xff])

# bytes are dict-hashable
dict = {hello: 1, goodbye: 2}
dict[b"goodbye"] = 3
assert_eq(len(dict), 2)
assert_eq(dict[goodbye], 3)

# hash(bytes) is 32-bit FNV-1a.
assert_eq(hash(b"") & 0xffffffff, 0x811c9dc5)
assert_eq(hash(b"a") & 0xffffffff, 0xe40c292c)
assert_eq(hash(b"ab") & 0xffffffff, 0x4d2505ca)
assert_eq(hash(b"abc") & 0xffffffff, 0x1a47e90b)
# Bytes >= 0x80 must not be sign-extended.
assert_eq(hash(b"\xe4") & 0xffffffff, 0x610b5af3)
assert_eq(hash(b"\xff\x00\x80") & 0xffffffff, 0xa36cd47e)
assert_eq(hash(hello) & 0xffffffff, 0xc5f68969)

# indexing
# VGS: indexing yields the byte's int, as the spec says (starlark-go yields a 1-byte bytes).
assert_eq(goodbye[0], 103)
assert_eq(goodbye[-1], 101)
assert_fails(lambda: goodbye[100], "out of range")
assert_eq(b"A\xe4"[-1], 228)
assert_eq(type(goodbye[0]), "int")

# VGS: iteration yields ints too, as in Python, so every way of reading one
# element of a bytes yields the same int.
ab = b"A\xe4"
assert_eq([c for c in ab], [65, 228])
assert_eq([type(c) for c in ab], ["int", "int"])
assert_eq(list(ab), [65, 228])
assert_eq(tuple(ab), (65, 228))
assert_eq(sorted(b"BA"), [65, 66])
assert_eq(list(enumerate(ab)), [(0, 65), (1, 228)])
assert_eq(list(zip(ab, b"xy")), [(65, 120), (228, 121)])
assert_eq(max(ab), 228)
assert_eq(min(ab), 65)
assert_eq(any(b"\x00"), False)
assert_eq(all(b"\x01\x02"), True)
assert_eq(list(reversed(ab)), [228, 65])
x, y = ab
assert_eq((x, y), (65, 228))
assert_eq((lambda *a: a)(*ab), (65, 228))

# Indexed and iterated elements are the same dict key.
table = {v: i for i, v in enumerate(b"ABC")}
assert_eq([table[c] for c in b"CAB"], [2, 0, 1])
assert_eq(table[b"CAB"[0]], 2)
assert_eq(len(set([b"A"[0], 65] + list(b"A"))), 1)

# An element is an int, not a bytes.
assert_ne(ab[0], b"A")
assert_eq(ab[0] + 1, 66)
assert_eq(bytes([ab[0], ab[1]]), ab)
assert_eq(bytes(list(ab)), ab)
assert_fails(lambda: ab[0] + b"x", "unsupported binary operation: int \\+ bytes")

# slicing
assert_eq(goodbye[:4], b"good")
assert_eq(goodbye[4:], b"bye")
assert_eq(goodbye[::2], b"gobe")
assert_eq(goodbye[3:4], b"d")  # special case: len=1
assert_eq(goodbye[4:4], b"")  # special case: len=0

# concatenation (from starlark-go's bytes.star, google/starlark-go#648)
# The compiler folds sums of adjacent bytes literals, so each case below keeps at
# least one operand dynamic to exercise the interpreter instead.
def concat(x, y):
    return x + y

assert_eq(concat(b"abc", b"def"), b"abc" + b"def")  # both paths must agree
assert_eq(goodbye + b"!", b"goodbye!")
assert_eq(b"good" + goodbye[4:], goodbye)
assert_eq(b"[" + goodbye + b"]", b"[goodbye]")
assert_eq(goodbye + empty, goodbye)
assert_eq(empty + goodbye, goodbye)

# Concatenation joins bytes, not text: no UTF-8 validation or U+FFFD replacement.
assert_eq(concat(b"\xed\xb0", b"\x80"), b"\xed\xb0\x80")
assert_eq(concat(hello[:-1], hello[-1:]), hello)  # split mid-code-point

def inplace():
    x = goodbye
    x += b"!"
    return x

assert_eq(inplace(), b"goodbye!")

# Text and binary strings do not mix. (starlark-go says "unknown binary op".)
assert_fails(lambda: goodbye + "!", "unsupported binary operation: bytes \\+ string")
assert_fails(lambda: "!" + goodbye, "unsupported binary operation: string \\+ bytes")
# Nor do lists (a VGS case; Python rejects these too).
assert_fails(lambda: [1] + goodbye, "unsupported binary operation: list \\+ bytes")
assert_fails(lambda: goodbye + [1], "unsupported binary operation: bytes \\+ list")

# bytes in bytes
assert_eq(b"bc" in b"abcd", True)
assert_eq(b"bc" in b"dcab", False)
assert_fails(lambda: "bc" in b"dcab", "requires bytes or int as left operand, not string")

# int in bytes
assert_eq(97 in b"abc", True)  # 97='a'
assert_eq(100 in b"abc", False) # 100='d'
assert_fails(lambda: 256 in b"abc", "int in bytes: 256 out of range")
assert_fails(lambda: -1 in b"abc", "int in bytes: -1 out of range")

# ord   TODO(adonovan): specify
assert_eq(ord(b"a"), 97)
assert_eq(ord(b"\xe4"), 228)
assert_fails(lambda: ord(b"ab"), "ord: bytes has length 2, want 1")
assert_fails(lambda: ord(b""), "ord: bytes has length 0, want 1")
# VGS: ord(string) is the code point, as in Python.
assert_eq(ord("a"), 97)
assert_eq(ord("\x7f"), 127)
assert_eq(ord("é"), 233)
assert_eq(ord("€"), 8364)
assert_eq(ord("世"), 19990)
assert_eq(ord("😿"), 128575)  # one code point, two UTF-16 chars
assert_fails(lambda: ord("ab"), "ord: string has length 2, want 1")
assert_fails(lambda: ord(""), "ord: string has length 0, want 1")
# VGS: scripts wrote ord(b[i]) when b[i] was a 1-byte bytes; that keeps working.
assert_eq(ord(ab[0]), 65)
assert_eq(ord(ab[1]), 228)
assert_fails(lambda: ord(256), "ord: int 256 out of range")
assert_fails(lambda: ord(-1), "ord: int -1 out of range")

# VGS: an empty needle (these used to loop forever) and count's slice bounds, as in Python.
assert_eq(b"abc".count(b""), 4)
assert_eq(b"abc".count(b"", 3), 1)
assert_eq(b"abc".count(b"", 4), 0)
assert_eq(b"abc".count(b"", 2, 1), 0)
assert_eq(b"abc".count(b"", -1), 2)
assert_eq(b"abc".count(b"", -10), 4)
assert_eq(b"abcb".count(b"b", -2), 1)
assert_eq(b"aaaa".count(b"aa"), 2)
assert_eq(b"abcabc".count(b"bc", 1, 5), 1)
assert_eq(b"abc".count(b"c", 0, -1), 0)
assert_eq(b"abc".replace(b"", b"x"), b"xaxbxcx")
assert_eq(b"abc".replace(b"", b"x", 2), b"xaxbc")
assert_eq(b"".replace(b"", b"x"), b"x")
assert_eq(b"abab".replace(b"ab", b"x", 1), b"xab")
assert_fails(lambda: b"abc".split(b""), "empty separator")
assert_fails(lambda: b"abc".rsplit(b""), "empty separator")
assert_fails(lambda: b"abc".partition(b""), "empty separator")
assert_fails(lambda: b"abc".rpartition(b""), "empty separator")

# VGS: whitespace is exactly b" \t\n\r\x0b\x0c", as in Python; NUL and bytes >= 0x80
# (e.g. \xa0) are not.
assert_eq([i for i in range(256) if bytes([i]).isspace()], [9, 10, 11, 12, 13, 32])
assert_eq(b"\x00ab\x80".strip(), b"\x00ab\x80")
assert_eq(b" \t\n\x0b\x0c\rab\r\n ".strip(), b"ab")
assert_eq(b"\xa0ab\xe0".lstrip(), b"\xa0ab\xe0")
assert_eq(b"\xa0ab\xe0".rstrip(), b"\xa0ab\xe0")
assert_eq(b"a\x00b \xa0c".split(), [b"a\x00b", b"\xa0c"])
assert_eq(b"a\x85b\x0bc".rsplit(), [b"a\x85b", b"c"])

# repeat (bytes * int)
assert_eq(goodbye * 3, b"goodbyegoodbyegoodbye")
assert_eq(3 * goodbye, b"goodbyegoodbyegoodbye")

# elems() returns an iterable value over the bytes' ints.
assert_eq(type(hello.elems()), "bytes.elems")
assert_eq(str(hello.elems()), "b\"hello, 世界\".elems()")
assert_eq(list(hello.elems()), [104, 101, 108, 108, 111, 44, 32, 228, 184, 150, 231, 149, 140])
assert_eq(bytes([104, 101, 108, 108, 111, 44, 32, 228, 184, 150, 231, 149, 140]), hello)
assert_eq(list(goodbye.elems()), [103, 111, 111, 100, 98, 121, 101])
assert_eq(list(empty.elems()), [])
assert_eq(bytes(hello.elems()), hello) # bytes(iterable) is dual to bytes.elems()

# x[i] = ...
def f():
    b"abc"[1] = b"B"

assert_fails(f, "can only assign an element in a .*, not in a 'bytes'")

# TODO(adonovan): the specification is not finalized in many areas:
# - chr, ord functions
# - encoding/decoding bytes to string.
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
