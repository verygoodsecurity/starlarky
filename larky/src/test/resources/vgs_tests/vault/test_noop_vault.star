"""Unit tests for VaultModule.java using NoopVault API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//vaultapi", "vault")

def _test_put():
    card_number = "4111111111111111"
    asserts.assert_fails(lambda : vault.redact(card_number), "vault.redact operation must be overridden")

def _test_get():
    card_number = "4111111111111111"
    asserts.assert_fails(lambda : vault.reveal(card_number), "vault.reveal operation must be overridden")

def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_put))
    _suite.addTest(unittest.FunctionTestCase(_test_get))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
