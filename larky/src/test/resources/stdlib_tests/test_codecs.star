"""codecs.decode with utf-8 returns the decoded text, as CPython does (no repr-style escaping)."""
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")


def _test_decode_utf8_is_not_escaped():
    asserts.assert_that(codecs.decode(b'{"a": "b\\\\c"}', "utf-8")).is_equal_to('{"a": "b\\\\c"}')
    asserts.assert_that(codecs.decode(b"caf\xc3\xa9", "utf-8")).is_equal_to("café")
    asserts.assert_that(codecs.decode(b"caf\xc3\xa9", "UTF-8")).is_equal_to("café")
    asserts.assert_that(codecs.decode(b"caf\xc3\xa9", "utf8")).is_equal_to("café")
    asserts.assert_that(codecs.decode(b"tab\there", "utf-8")).is_equal_to("tab\there")


def _test_decode_utf8_invalid_bytes():
    # CPython 3.10: b"a\xffb".decode("utf-8", <errors>)
    asserts.assert_that(codecs.decode(b"a\xffb", "utf-8", "backslashreplace")).is_equal_to("a\\xffb")
    asserts.assert_that(codecs.decode(b"a\xffb", "utf-8", "replace")).is_equal_to("a\ufffdb")
    asserts.assert_that(codecs.decode(b"a\xffb", "utf-8", "ignore")).is_equal_to("ab")
    asserts.assert_fails(lambda: codecs.decode(b"a\xffb", "utf-8"), ".*")


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_decode_utf8_is_not_escaped))
    _suite.addTest(unittest.FunctionTestCase(_test_decode_utf8_invalid_bytes))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
