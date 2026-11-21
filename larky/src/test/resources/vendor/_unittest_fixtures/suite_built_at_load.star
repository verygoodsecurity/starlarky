# A test suite built while this module loads, then run by the module that loads it
# (UnittestModuleThreadTest).
load("@stdlib//unittest", "unittest")

def _inner():
    fail("boom")

def test_fails():
    _inner()

def _build():
    s = unittest.TestSuite()
    s.addTest(unittest.FunctionTestCase(test_fails))
    return s

SUITE = _build()
