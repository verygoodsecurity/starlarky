"""Unit tests for VaultModule.java using NoopVault API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//nts", "nts")

def _test_render():
    pan = "4111111111111111"
    asserts.assert_fails(lambda : nts.render({}, pan), "nts.render's getNetworkToken operation must be overridden")

def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_render))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
