"""Unit tests without vault override for VaultModule.java"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//vault", "vault")

def _test_put():
    account_number = "4111111111111111"
    asserts.assert_fails(lambda : vault.redact(account_number), "vault.redact operation must be overridden")

def _test_get():
    account_number = "4111111111111111"
    asserts.assert_fails(lambda : vault.reveal(account_number), "vault.reveal operation must be overridden")

def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_put))
    _suite.addTest(unittest.FunctionTestCase(_test_get))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
