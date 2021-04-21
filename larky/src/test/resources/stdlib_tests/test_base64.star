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


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_b64encode))
    _suite.addTest(unittest.FunctionTestCase(_test_b64decode))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
