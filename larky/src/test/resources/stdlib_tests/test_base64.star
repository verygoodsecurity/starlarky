"""Unit tests for re.star"""

load("@stdlib/asserts", "asserts")
load("@stdlib/unittest", "unittest")
load("@stdlib/base64", "base64")

def eq(first, second):
    asserts.assert_that(first).is_equal_to(second)

def _test_b64encode():

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

def _test_b64decode():
    tests = {"d3d3LnB5dGhvbi5vcmc=": "www.python.org",
                # b'AA==': b'\x00',
                "YQ==": "a",
                "YWI=": "ab",
                "YWJj": "abc",

                "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXpBQkNE" +
                "RUZHSElKS0xNTk9QUVJTVFVWV1hZWjAxMjM0NT" +
                "Y3ODkhQCMwXiYqKCk7Ojw+LC4gW117fQ==":

                    "abcdefghijklmnopqrstuvwxyz" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "0123456789!@#0^&*();:<>,. []{}",
                '': '',
                }
    for data, res in tests.items():
        print('checking', data, res)
        eq(base64.b64decode(data), res)
        # need some bytes
        # eq(base64.b64decode(data.decode('ascii')), res)


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_b64encode))
    _suite.addTest(unittest.FunctionTestCase(_test_b64decode))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
