load("@stdlib//base64", "base64")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@stdlib//codecs", codecs="codecs")


def b(s):
    return builtins.bytes(s, encoding="utf-8")


eq = asserts.eq


def _test_b64encode():

    eq(base64.b64encode(b("www.python.org")), b("d3d3LnB5dGhvbi5vcmc="))
    # eq(base64.b64encode('\x00'), 'AA==')
    eq(base64.b64encode(b("a")), b("YQ=="))
    eq(base64.b64encode(b("ab")), b("YWI="))
    eq(base64.b64encode(b("abc")), b("YWJj"))
    eq(base64.b64encode(b("")), b(""))
    eq(
        base64.b64encode(
            b(
                "abcdefghijklmnopqrstuvwxyz"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789!@#0^&*();:<>,. []{}"
            )
        ),
        b(
            "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNE"
            + "RUZHSElKS0xNTk9QUVJTVFVWV1hZWjAxMjM0NT"
            + "Y3ODkhQCMwXiYqKCk7Ojw+LC4gW117fQ=="
        ),
    )
    # Test with arbitrary alternative characters
    # eq(base64.b64encode('\xd3V\xbeo\xf7\x1d', altchars='*$'), '01a*b$cd')


def _test_b64decode():
    tests = {
        b("d3d3LnB5dGhvbi5vcmc="): b("www.python.org"),
        b("AA=="): b([0x00]),
        b("YQ=="): b("a"),
        b("YWI="): b("ab"),
        b("YWJj"): b("abc"),
        b("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNE"
          + "RUZHSElKS0xNTk9QUVJTVFVWV1hZWjAxMjM0\nNT"
          + "Y3ODkhQCMwXiYqKCk7Ojw+LC4gW117fQ=="
        ): b(
            "abcdefghijklmnopqrstuvwxyz"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "0123456789!@#0^&*();:<>,. []{}"
        ),
        b(""): b(""),
    }
    for data, res in tests.items():
        eq(base64.b64decode(data), res)
        eq(base64.b64decode(codecs.decode(data, encoding="ascii")), res)



# RFC 4648 section 10 test vectors, and binary input; the encodings are CPython's.
_B32_VECTORS = [
    (b"", b""),
    (b"f", b"MY======"),
    (b"fo", b"MZXQ===="),
    (b"foo", b"MZXW6==="),
    (b"foob", b"MZXW6YQ="),
    (b"fooba", b"MZXW6YTB"),
    (b"foobar", b"MZXW6YTBOI======"),
    (b"\x00\xff\x10 hello", b"AD7RAIDIMVWGY3Y="),
    (bytes(list(range(20))), b"AAAQEAYEAUDAOCAJBIFQYDIOB4IBCEQT"),
]


def _test_b32encode():
    for raw, encoded in _B32_VECTORS:
        asserts.assert_that(base64.b32encode(raw)).is_equal_to(encoded)


def _test_b32decode():
    for raw, encoded in _B32_VECTORS:
        asserts.assert_that(base64.b32decode(encoded)).is_equal_to(raw)
    asserts.assert_that(base64.b32decode("MZXW6YTBOI======")).is_equal_to(b"foobar")
    asserts.assert_that(base64.b32decode(b"mzxw6ytboi======", casefold=True)).is_equal_to(b"foobar")
    asserts.assert_fails(lambda: base64.b32decode(b"mzxw6ytboi======"), "Non-base32 digit found")
    asserts.assert_fails(lambda: base64.b32decode(b"MZXW6YT"), "Incorrect padding")


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_b64encode))
    _suite.addTest(unittest.FunctionTestCase(_test_b64decode))
    _suite.addTest(unittest.FunctionTestCase(_test_b32encode))
    _suite.addTest(unittest.FunctionTestCase(_test_b32decode))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
