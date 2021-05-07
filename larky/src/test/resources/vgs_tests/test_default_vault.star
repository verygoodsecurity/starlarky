"""Unit tests with vault override for VaultModule.java"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//vault", "vault")

def _test_put():
    account_number = "4111111111111111"
    redacted_account_number = vault.redact(account_number)
    asserts.assert_that(redacted_account_number).is_equal_to('tok_1537796765')

def _test_get():
    account_number = "4111111111111111"
    redacted_account_number = vault.redact(account_number)
    revealed_account_number = vault.reveal(redacted_account_number)
    asserts.assert_that(revealed_account_number).is_equal_to(account_number)

def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_put))
    _suite.addTest(unittest.FunctionTestCase(_test_get))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
