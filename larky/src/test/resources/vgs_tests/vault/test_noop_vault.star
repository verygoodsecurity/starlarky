"""Unit tests for VaultModule.java using NoopVault API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//vault", "vault")

def _test_put():
    card_number = "4111111111111111"
    asserts.assert_fails(lambda : vault.redact(card_number), "vault.redact operation must be overridden")

def _test_get():
    card_number = "4111111111111111"
    asserts.assert_fails(lambda : vault.reveal(card_number), "vault.reveal operation must be overridden")

def _test_delete():
    card_number = "4111111111111111"
    asserts.assert_fails(lambda : vault.delete(card_number), "vault.delete operation must be overridden")

def _test_sign():
    asserts.assert_fails(lambda: vault.sign("keyId", "message", "algo"), "vault.sign operation must be overridden")

def _test_verify():
    asserts.assert_fails(lambda: vault.verify("keyId", "message", "signature", "algo"), "vault.verify operation must be overridden")

def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_put))
    _suite.addTest(unittest.FunctionTestCase(_test_get))
    _suite.addTest(unittest.FunctionTestCase(_test_delete))
    _suite.addTest(unittest.FunctionTestCase(_test_sign))
    _suite.addTest(unittest.FunctionTestCase(_test_verify))


    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
