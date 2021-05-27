"""Unit tests for parse.star"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@stdlib//parse", "parse")
load("@stdlib//base64", "base64")
load("@stdlib//builtins", builtins="builtins")

def b(s):
    return builtins.bytes(s, encoding="utf-8")

eq = asserts.eq

def _add_test():
    """Unit tests for """

    # Test .
    res1 = parse.urlparse('http://www.cwi.nl:80/%7Eguido/Python.html')
    eq(b('http'),b(res1['scheme']))
    res2 = parse.urlsplit('http://www.cwi.nl:80/%7Eguido/Python.html')
    eq(b('www.cwi.nl:80'),b(res2['netloc']))


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_add_test))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
