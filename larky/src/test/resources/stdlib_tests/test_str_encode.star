"""Tests of str.encode: it is Python's, and agrees with bytes(s, encoding) and codecs.encode."""

load("@stdlib//codecs", "codecs")
load("@stdlib//hashlib", "hashlib")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@vendor//option/result", safe="safe")
load("@vendor//MasterCard/oauth1", util="coreutils")


def _test_basic():
    asserts.assert_that("abc".encode()).is_equal_to(b"abc")
    asserts.assert_that(type("abc".encode())).is_equal_to("bytes")
    asserts.assert_that("hé€".encode()).is_equal_to(b"h\xc3\xa9\xe2\x82\xac")
    asserts.assert_that("hé€".encode("utf-8")).is_equal_to(b"h\xc3\xa9\xe2\x82\xac")
    asserts.assert_that("hé€".encode(encoding="UTF8")).is_equal_to(b"h\xc3\xa9\xe2\x82\xac")
    asserts.assert_that("hé".encode("latin-1")).is_equal_to(b"h\xe9")
    asserts.assert_that("hé".encode("iso-8859-1")).is_equal_to(b"h\xe9")
    asserts.assert_that("hé".encode("ascii", "ignore")).is_equal_to(b"h")
    asserts.assert_that("hé".encode("ascii", errors="replace")).is_equal_to(b"h?")
    asserts.assert_that("hé".encode(errors="strict")).is_equal_to(b"h\xc3\xa9")
    asserts.assert_that("hé€".encode("utf-16-le")).is_equal_to(b"h\x00\xe9\x00\xac\x20")
    asserts.assert_that("hé".encode("cp1252")).is_equal_to(b"h\xe9")


def _test_agrees_with_bytes_and_codecs():
    texts = ["", "abc", "héllo", "€ uro", "\U0001f600 face", "aé€\U0001f600b"]
    encodings = ["utf-8", "utf8", "latin-1", "iso-8859-1", "ascii", "utf-16", "utf-32-be", "cp1252"]
    handlers = ["strict", "ignore", "replace", "backslashreplace", "xmlcharrefreplace", "namereplace"]
    for t in texts:
        for e in encodings:
            for h in handlers:
                want = _try(lambda: bytes(t, e, h))
                got = _try(lambda: t.encode(e, h))
                also = _try(lambda: codecs.encode(t, e, h, unescape=False))
                asserts.assert_that(got).is_equal_to(want)
                asserts.assert_that(got).is_equal_to(also)


def _try(f):
    # The Result's text: the bytes on success, the error message on failure, so that messages
    # are compared too.
    return str(safe(f)())


def _test_errors():
    asserts.assert_fails(
        lambda: "hé".encode("ascii"),
        "'ascii' codec can't encode character '\\\\xe9' in position 1: ordinal not in range\\(128\\)",
    )
    asserts.assert_fails(lambda: "abc".encode("no-such-codec"), "unknown encoding: no-such-codec")
    asserts.assert_fails(lambda: bytes("abc", "no-such-codec"), "unknown encoding: no-such-codec")
    asserts.assert_fails(
        lambda: "hé".encode("ascii", "bogus"), "unknown error handler name 'bogus'")
    asserts.assert_fails(
        lambda: bytes("hé", "ascii", "bogus"), "unknown error handler name 'bogus'")
    asserts.assert_fails(lambda: "abc".encode(1), "got value of type 'int', want 'string'")


def _test_stdlib_users_of_str_encode():
    # coreutils.sha256_encode hashes a str and a bytes alike (its str branch calls
    # text.encode('utf8')).
    expected = hashlib.sha256(b"h\xc3\xa9llo").digest()
    asserts.assert_that(util.sha256_encode("héllo")).is_equal_to(expected)
    asserts.assert_that(util.sha256_encode(b"h\xc3\xa9llo")).is_equal_to(expected)


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_basic))
    _suite.addTest(unittest.FunctionTestCase(_test_agrees_with_bytes_and_codecs))
    _suite.addTest(unittest.FunctionTestCase(_test_errors))
    _suite.addTest(unittest.FunctionTestCase(_test_stdlib_users_of_str_encode))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
