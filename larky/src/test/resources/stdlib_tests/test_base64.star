"""Unit tests for re.star"""

load("@stdlib/asserts", "asserts")
load("@stdlib/unittest", "unittest")
load("@stdlib/base64", "base64")


def _test_b64encode():
    def eq(first, second):
        asserts.assert_that(first).is_equal_to(second)

    eq(base64.b64encode("www.python.org"), "d3d3LnB5dGhvbi5vcmc=")
    # eq(base64.b64encode('\x00'), 'AA==')
    eq(base64.b64encode("a"), "YQ==")
    eq(base64.b64encode("ab"), "YWI=")
    eq(base64.b64encode("abc"), "YWJj")
    eq(base64.b64encode(""), "")
    eq(base64.b64encode("abcdefghijklmnopqrstuvwxyz" +
                        "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                        "0123456789!@#0^&*();:<>,. []{}"),
        "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNE" +
        "RUZHSElKS0xNTk9QUVJTVFVWV1hZWjAxMjM0NT" +
        "Y3ODkhQCMwXiYqKCk7Ojw+LC4gW117fQ==")
    # Test with arbitrary alternative characters
    # eq(base64.b64encode('\xd3V\xbeo\xf7\x1d', altchars='*$'), '01a*b$cd')


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_b64encode))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
