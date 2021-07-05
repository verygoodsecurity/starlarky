load("@stdlib//builtins", "builtins")
load("@stdlib//hashlib", "hashlib")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")


b = builtins.bytes

def _test_md5_basic():
    asserts.eq(hashlib.md5(b("hello")).hexdigest(), '5d41402abc4b2a76b9719d911017c592')
    # TypeError: Unicode-objects must be encoded before hashing
    # asserts.eq(hashlib.md5("hello").hexdigest(), '5d41402abc4b2a76b9719d911017c592')


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_md5_basic))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())