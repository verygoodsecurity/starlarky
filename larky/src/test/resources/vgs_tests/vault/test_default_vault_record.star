"""Unit tests for record-scoped reveal in VaultModule.java using DefaultVault API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//vault", "vault")

def _test_reveal_with_record():
    secret = "example-secret-1"
    alias = vault.redact(secret, decorator_config={"kind": "example", "record": "record-a"})

    asserts.assert_that(alias).is_not_equal_to(secret)
    asserts.assert_that(vault.reveal(alias, record="record-a")).is_equal_to(secret)

def _test_reveal_without_record_is_not_found():
    # DefaultVault answers "not found" by returning the alias.
    secret = "example-secret-2"
    alias = vault.redact(secret, decorator_config={"kind": "example", "record": "record-a"})

    asserts.assert_that(vault.reveal(alias)).is_equal_to(alias)
    asserts.assert_that(vault.reveal(alias, record=None)).is_equal_to(alias)

def _test_reveal_with_other_record_is_not_found():
    secret = "example-secret-3"
    alias = vault.redact(secret, decorator_config={"kind": "example", "record": "record-a"})

    asserts.assert_that(vault.reveal(alias, record="record-b")).is_equal_to(alias)

def _test_reveal_with_record_ignores_unscoped_alias():
    secret = "example-secret-4"
    alias = vault.redact(secret)

    asserts.assert_that(vault.reveal(alias)).is_equal_to(secret)
    asserts.assert_that(vault.reveal(alias, record="record-a")).is_equal_to(alias)

def _test_reveal_record_with_storage():
    secret = "example-secret-5"
    alias = vault.redact(secret, storage="volatile", decorator_config={"record": "record-a"})

    asserts.assert_that(vault.reveal(alias, storage="volatile", record="record-a")).is_equal_to(secret)
    asserts.assert_that(vault.reveal(alias, storage="persistent", record="record-a")).is_equal_to(alias)
    asserts.assert_that(vault.reveal(alias, storage="volatile")).is_equal_to(alias)

def _test_reveal_empty_record():
    asserts.assert_fails(lambda : vault.reveal("tok_123", record=""), "record must not be empty")

def _test_redact_empty_record():
    asserts.assert_fails(
        lambda : vault.redact("example-secret-6", decorator_config={"record": ""}),
        "record must not be empty"
    )

def _test_reveal_invalid_record_type():
    asserts.assert_fails(
        lambda : vault.reveal("tok_123", record=1),
        "in call to reveal\\(\\), parameter 'record' got value of type 'int', want 'NoneType or string'"
    )

def _test_reveal_backward_compatible():
    secret = "example-secret-7"
    alias = vault.redact(secret)
    volatile_alias = vault.redact(secret, storage="volatile")

    asserts.assert_that(vault.reveal(alias)).is_equal_to(secret)
    asserts.assert_that(vault.reveal(alias, "persistent")).is_equal_to(secret)
    asserts.assert_that(vault.reveal(alias, storage="persistent")).is_equal_to(secret)
    asserts.assert_that(vault.reveal(volatile_alias, storage="volatile")).is_equal_to(secret)
    asserts.assert_that(vault.reveal("tok_unknown")).is_equal_to("tok_unknown")

def _test_redact_with_kind_only_is_unscoped():
    secret = "example-secret-8"
    alias = vault.redact(secret, decorator_config={"kind": "example"})

    asserts.assert_that(vault.reveal(alias)).is_equal_to(secret)

def _test_reveal_record_is_keyword_only():
    asserts.assert_fails(
        lambda : vault.reveal("tok_123", "persistent", "record-a"),
        "accepts no more than 2 positional arguments"
    )

def _test_delete_with_record():
    secret = "example-secret-9"
    alias = vault.redact(secret, decorator_config={"kind": "example", "record": "record-a"})

    vault.delete(alias, record="record-b")
    asserts.assert_that(vault.reveal(alias, record="record-a")).is_equal_to(secret)
    vault.delete(alias)
    asserts.assert_that(vault.reveal(alias, record="record-a")).is_equal_to(secret)
    vault.delete(alias, record="record-a")
    asserts.assert_that(vault.reveal(alias, record="record-a")).is_equal_to(alias)

def _test_delete_with_record_and_storage():
    secret = "example-secret-10"
    alias = vault.redact(secret, storage="volatile", decorator_config={"record": "record-a"})

    vault.delete(alias, storage="persistent", record="record-a")
    asserts.assert_that(vault.reveal(alias, storage="volatile", record="record-a")).is_equal_to(secret)
    vault.delete(alias, storage="volatile", record="record-a")
    asserts.assert_that(vault.reveal(alias, storage="volatile", record="record-a")).is_equal_to(alias)

def _test_delete_without_record_still_works():
    secret = "example-secret-11"
    alias = vault.redact(secret)
    vault.delete(alias)
    asserts.assert_that(vault.reveal(alias)).is_equal_to(alias)

def _test_delete_record_is_keyword_only():
    asserts.assert_fails(
        lambda : vault.delete("tok_123", "persistent", "record-a"),
        "accepts no more than 2 positional arguments"
    )

def _test_delete_empty_record():
    asserts.assert_fails(lambda : vault.delete("tok_123", record=""), "record must not be empty")

def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_with_record))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_without_record_is_not_found))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_with_other_record_is_not_found))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_with_record_ignores_unscoped_alias))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_record_with_storage))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_empty_record))
    _suite.addTest(unittest.FunctionTestCase(_test_redact_empty_record))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_invalid_record_type))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_backward_compatible))
    _suite.addTest(unittest.FunctionTestCase(_test_redact_with_kind_only_is_unscoped))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_record_is_keyword_only))
    _suite.addTest(unittest.FunctionTestCase(_test_delete_with_record))
    _suite.addTest(unittest.FunctionTestCase(_test_delete_with_record_and_storage))
    _suite.addTest(unittest.FunctionTestCase(_test_delete_without_record_still_works))
    _suite.addTest(unittest.FunctionTestCase(_test_delete_record_is_keyword_only))
    _suite.addTest(unittest.FunctionTestCase(_test_delete_empty_record))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_suite())
