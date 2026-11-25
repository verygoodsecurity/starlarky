"""Unit tests for the optional lookup context (entity_key, kind) of VaultModule.java using DefaultVault API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//vault", "vault")

def _test_reveal_with_entity_key():
    secret = "example-secret-1"
    alias = vault.redact(secret, decorator_config={"kind": "example", "entity_key": "entity-a"})

    asserts.assert_that(alias).is_not_equal_to(secret)
    asserts.assert_that(vault.reveal(alias, entity_key="entity-a")).is_equal_to(secret)

def _test_reveal_without_entity_key_is_not_found():
    # DefaultVault answers "not found" by returning the alias.
    secret = "example-secret-2"
    alias = vault.redact(secret, decorator_config={"kind": "example", "entity_key": "entity-a"})

    asserts.assert_that(vault.reveal(alias)).is_equal_to(alias)
    asserts.assert_that(vault.reveal(alias, entity_key=None)).is_equal_to(alias)

def _test_reveal_with_other_entity_key_is_not_found():
    secret = "example-secret-3"
    alias = vault.redact(secret, decorator_config={"kind": "example", "entity_key": "entity-a"})

    asserts.assert_that(vault.reveal(alias, entity_key="entity-b")).is_equal_to(alias)

def _test_reveal_with_entity_key_ignores_unscoped_alias():
    secret = "example-secret-4"
    alias = vault.redact(secret)

    asserts.assert_that(vault.reveal(alias)).is_equal_to(secret)
    asserts.assert_that(vault.reveal(alias, entity_key="entity-a")).is_equal_to(alias)
    asserts.assert_that(vault.reveal(alias, kind="example")).is_equal_to(alias)

def _test_reveal_entity_key_with_storage():
    secret = "example-secret-5"
    alias = vault.redact(secret, storage="volatile", decorator_config={"entity_key": "entity-a"})

    asserts.assert_that(vault.reveal(alias, storage="volatile", entity_key="entity-a")).is_equal_to(secret)
    asserts.assert_that(vault.reveal(alias, storage="persistent", entity_key="entity-a")).is_equal_to(alias)
    asserts.assert_that(vault.reveal(alias, storage="volatile")).is_equal_to(alias)

def _test_kind_is_not_scope_by_default():
    # Without scope_kind, kind is only a generation input: entity_key alone finds the alias.
    secret = "example-secret-14"
    alias = vault.redact(secret, decorator_config={"kind": "example", "entity_key": "entity-a"})

    asserts.assert_that(vault.reveal(alias, entity_key="entity-a")).is_equal_to(secret)
    asserts.assert_that(vault.reveal(alias, entity_key="entity-a", kind="example")).is_equal_to(alias)

def _test_scoped_kind_needs_the_same_context():
    secret = "example-secret-15"
    alias = vault.redact(
        secret,
        decorator_config={"kind": "example", "entity_key": "entity-a", "scope_kind": True},
    )

    asserts.assert_that(vault.reveal(alias, entity_key="entity-a", kind="example")).is_equal_to(secret)
    asserts.assert_that(vault.reveal(alias, entity_key="entity-a")).is_equal_to(alias)
    asserts.assert_that(vault.reveal(alias, kind="example")).is_equal_to(alias)
    asserts.assert_that(vault.reveal(alias)).is_equal_to(alias)
    asserts.assert_that(vault.reveal(alias, entity_key="entity-a", kind="other")).is_equal_to(alias)

def _test_kind_only_scope():
    secret = "example-secret-16"
    alias = vault.redact(secret, decorator_config={"kind": "example", "scope_kind": True})

    asserts.assert_that(vault.reveal(alias, kind="example")).is_equal_to(secret)
    asserts.assert_that(vault.reveal(alias, entity_key=None, kind="example")).is_equal_to(secret)
    asserts.assert_that(vault.reveal(alias)).is_equal_to(alias)
    asserts.assert_that(vault.reveal(alias, entity_key="example")).is_equal_to(alias)

def _test_invalid_scope_kind():
    asserts.assert_fails(
        lambda : vault.redact("example-secret-17", decorator_config={"kind": "example", "scope_kind": "yes"}),
        "scope_kind must be a bool, not string"
    )

def _test_reveal_empty_entity_key():
    asserts.assert_fails(lambda : vault.reveal("tok_123", entity_key=""), "entity_key must not be empty or blank")

def _test_reveal_empty_kind():
    asserts.assert_fails(lambda : vault.reveal("tok_123", kind=""), "kind must not be empty or blank")
    asserts.assert_fails(lambda : vault.delete("tok_123", kind=" "), "kind must not be empty or blank")

def _test_whitespace_only_entity_key():
    asserts.assert_fails(lambda : vault.reveal("tok_123", entity_key="   "), "entity_key must not be empty or blank")
    asserts.assert_fails(lambda : vault.delete("tok_123", entity_key="\t"), "entity_key must not be empty or blank")
    asserts.assert_fails(
        lambda : vault.redact("example-secret-13", decorator_config={"entity_key": " "}),
        "entity_key must not be empty or blank"
    )

def _test_redact_empty_entity_key():
    asserts.assert_fails(
        lambda : vault.redact("example-secret-6", decorator_config={"entity_key": ""}),
        "entity_key must not be empty or blank"
    )

def _test_reveal_invalid_entity_key_type():
    asserts.assert_fails(
        lambda : vault.reveal("tok_123", entity_key=1),
        "in call to reveal\\(\\), parameter 'entity_key' got value of type 'int', want 'NoneType or string'"
    )

def _test_reveal_invalid_kind_type():
    asserts.assert_fails(
        lambda : vault.reveal("tok_123", kind=1),
        "in call to reveal\\(\\), parameter 'kind' got value of type 'int', want 'NoneType or string'"
    )

def _test_reveal_backward_compatible():
    secret = "example-secret-7"
    alias = vault.redact(secret)
    volatile_alias = vault.redact(secret, storage="volatile")

    asserts.assert_that(vault.reveal(alias)).is_equal_to(secret)
    asserts.assert_that(vault.reveal(alias, "persistent")).is_equal_to(secret)
    asserts.assert_that(vault.reveal(alias, storage="persistent")).is_equal_to(secret)
    asserts.assert_that(vault.reveal(alias, entity_key=None, kind=None)).is_equal_to(secret)
    asserts.assert_that(vault.reveal(volatile_alias, storage="volatile")).is_equal_to(secret)
    asserts.assert_that(vault.reveal("tok_unknown")).is_equal_to("tok_unknown")

def _test_redact_with_kind_only_is_unscoped():
    secret = "example-secret-8"
    alias = vault.redact(secret, decorator_config={"kind": "example"})

    asserts.assert_that(vault.reveal(alias)).is_equal_to(secret)

def _test_reveal_context_is_keyword_only():
    asserts.assert_fails(
        lambda : vault.reveal("tok_123", "persistent", "entity-a"),
        "accepts no more than 2 positional arguments"
    )

def _test_delete_with_entity_key():
    secret = "example-secret-9"
    alias = vault.redact(secret, decorator_config={"kind": "example", "entity_key": "entity-a"})

    vault.delete(alias, entity_key="entity-b")
    asserts.assert_that(vault.reveal(alias, entity_key="entity-a")).is_equal_to(secret)
    vault.delete(alias)
    asserts.assert_that(vault.reveal(alias, entity_key="entity-a")).is_equal_to(secret)
    vault.delete(alias, entity_key="entity-a")
    asserts.assert_that(vault.reveal(alias, entity_key="entity-a")).is_equal_to(alias)

def _test_delete_without_kind_keeps_a_kind_scoped_alias():
    secret = "example-secret-18"
    alias = vault.redact(
        secret,
        decorator_config={"kind": "example", "entity_key": "entity-a", "scope_kind": True},
    )

    vault.delete(alias, entity_key="entity-a")
    asserts.assert_that(vault.reveal(alias, entity_key="entity-a", kind="example")).is_equal_to(secret)
    vault.delete(alias, entity_key="entity-a", kind="example")
    asserts.assert_that(vault.reveal(alias, entity_key="entity-a", kind="example")).is_equal_to(alias)

def _test_delete_with_entity_key_and_storage():
    secret = "example-secret-10"
    alias = vault.redact(secret, storage="volatile", decorator_config={"entity_key": "entity-a"})

    vault.delete(alias, storage="persistent", entity_key="entity-a")
    asserts.assert_that(vault.reveal(alias, storage="volatile", entity_key="entity-a")).is_equal_to(secret)
    vault.delete(alias, storage="volatile", entity_key="entity-a")
    asserts.assert_that(vault.reveal(alias, storage="volatile", entity_key="entity-a")).is_equal_to(alias)

def _test_delete_without_context_still_works():
    secret = "example-secret-11"
    alias = vault.redact(secret)
    vault.delete(alias)
    asserts.assert_that(vault.reveal(alias)).is_equal_to(alias)

def _test_delete_context_is_keyword_only():
    asserts.assert_fails(
        lambda : vault.delete("tok_123", "persistent", "entity-a"),
        "accepts no more than 2 positional arguments"
    )

def _test_delete_empty_entity_key():
    asserts.assert_fails(lambda : vault.delete("tok_123", entity_key=""), "entity_key must not be empty or blank")

def _test_synthetic_format_passes_validation_and_default_vault_rejects_it():
    # VaultModule accepts the format, so the call reaches the provider; DefaultVault has no generator for it.
    asserts.assert_fails(
        lambda : vault.redact(
            "example-secret-12",
            format="SYNTHETIC",
            decorator_config={"kind": "example", "entity_key": "entity-a"}
        ),
        "Format 'SYNTHETIC' is not supported"
    )

def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_with_entity_key))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_without_entity_key_is_not_found))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_with_other_entity_key_is_not_found))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_with_entity_key_ignores_unscoped_alias))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_entity_key_with_storage))
    _suite.addTest(unittest.FunctionTestCase(_test_kind_is_not_scope_by_default))
    _suite.addTest(unittest.FunctionTestCase(_test_scoped_kind_needs_the_same_context))
    _suite.addTest(unittest.FunctionTestCase(_test_kind_only_scope))
    _suite.addTest(unittest.FunctionTestCase(_test_invalid_scope_kind))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_empty_entity_key))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_empty_kind))
    _suite.addTest(unittest.FunctionTestCase(_test_whitespace_only_entity_key))
    _suite.addTest(unittest.FunctionTestCase(_test_redact_empty_entity_key))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_invalid_entity_key_type))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_invalid_kind_type))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_backward_compatible))
    _suite.addTest(unittest.FunctionTestCase(_test_redact_with_kind_only_is_unscoped))
    _suite.addTest(unittest.FunctionTestCase(_test_reveal_context_is_keyword_only))
    _suite.addTest(unittest.FunctionTestCase(_test_delete_with_entity_key))
    _suite.addTest(unittest.FunctionTestCase(_test_delete_without_kind_keeps_a_kind_scoped_alias))
    _suite.addTest(unittest.FunctionTestCase(_test_delete_with_entity_key_and_storage))
    _suite.addTest(unittest.FunctionTestCase(_test_delete_without_context_still_works))
    _suite.addTest(unittest.FunctionTestCase(_test_delete_context_is_keyword_only))
    _suite.addTest(unittest.FunctionTestCase(_test_delete_empty_entity_key))
    _suite.addTest(unittest.FunctionTestCase(_test_synthetic_format_passes_validation_and_default_vault_rejects_it))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_suite())
