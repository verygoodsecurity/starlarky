"""Unit tests for VaultModule.java using DefaultVault API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//vault", "vault")

def _test_default_redact():
    account_number = "4111111111111111"
    redacted_account_number = vault.redact(account_number)

    asserts.assert_that(redacted_account_number).is_equal_to('tok_1537796765')

def _test_params_redact():
    account_number = "4111111111111112"
    redacted_account_number = vault.redact(
        account_number,
        storage="volatile",
        format="tok_xxxxxxxxxx",
        tags=['tag1','tag2']
    )

    asserts.assert_that(redacted_account_number).is_equal_to('tok_1537796766')

def _test_default_reveal():
    account_number = "4111111111111113"
    redacted_account_number = vault.redact(account_number)
    revealed_account_number = vault.reveal(redacted_account_number)

    asserts.assert_that(revealed_account_number).is_equal_to(account_number)

def _test_empty_reveal():
    token = "tok_123"
    revealed = vault.reveal(token)

    asserts.assert_that(revealed).is_equal_to("token")

def _test_persistent_storage():
    account_number = "4111111111111114"
    redacted_account_number = vault.redact(account_number)

    revealed_account_number_implicit = vault.reveal(redacted_account_number) # default storage
    revealed_account_number_explicit = vault.reveal(redacted_account_number, storage='persistent')
    revealed_account_number_volatile = vault.reveal(redacted_account_number, storage='volatile')

    asserts.assert_that(revealed_account_number_implicit).is_equal_to('4111111111111114')
    asserts.assert_that(revealed_account_number_explicit).is_equal_to('4111111111111114')
    asserts.assert_that(revealed_account_number_volatile).is_equal_to('token')

def _test_volatile_storage():
    account_number = "4111111111111115"
    redacted_account_number = vault.redact(account_number, storage='volatile')

    revealed_account_number_volatile = vault.reveal(redacted_account_number, storage='volatile')
    revealed_account_number_persistent = vault.reveal(redacted_account_number, storage='persistent')

    asserts.assert_that(revealed_account_number_volatile).is_equal_to('4111111111111115')
    asserts.assert_that(revealed_account_number_persistent).is_equal_to('token')

def _test_invalid_storage():
    account_number = "4111111111111116"
    asserts.assert_fails(lambda : vault.redact(account_number, storage="invalid"), "'invalid' not found in available storage list \\[persistent, volatile\\]")

def _suite():
    _suite = unittest.TestSuite()

    # Redact Tests
    _suite.addTest(unittest.FunctionTestCase(_test_default_redact))
    _suite.addTest(unittest.FunctionTestCase(_test_params_redact))

    # Reveal Tests
    _suite.addTest(unittest.FunctionTestCase(_test_default_reveal))
    _suite.addTest(unittest.FunctionTestCase(_test_empty_reveal))

    # Storage Tests
    _suite.addTest(unittest.FunctionTestCase(_test_persistent_storage))
    _suite.addTest(unittest.FunctionTestCase(_test_volatile_storage))
    _suite.addTest(unittest.FunctionTestCase(_test_invalid_storage))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
