"""Unit tests for VaultModule.java using DefaultVault API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//vault", "vault")

def _test_default_redact():
    card_number = "4111111111111111"
    redacted_card_number = vault.redact(card_number)

    asserts.assert_that(redacted_card_number[:4]).is_equal_to('tok_')

def _test_params_redact():
    card_number = "4111111111111112"
    redacted_card_number = vault.redact(
        card_number,
        storage="volatile",
        format="UUID",
        tags=['tag1','tag2']
    )

    asserts.assert_that(redacted_card_number[:4]).is_equal_to('tok_')

def _test_invalid_list_redact():
    card_number = ["4111111111111111"]

    asserts.assert_fails(lambda : vault.redact(card_number),
        "Value of type net.starlark.java.eval.StarlarkList is not supported in DefaultVault, expecting String"
    )

def _test_default_reveal():
    card_number = "4111111111111113"
    redacted_card_number = vault.redact(card_number)
    revealed_card_number = vault.reveal(redacted_card_number)

    asserts.assert_that(revealed_card_number).is_equal_to(card_number)

def _test_default_delete():
    card_number = "4111111111111113"
    redacted_card_number = vault.redact(card_number)
    vault.delete(redacted_card_number)
    revealed_card_number = vault.reveal(redacted_card_number)

    asserts.assert_that(revealed_card_number).is_equal_to(redacted_card_number)

def _test_default_delete_volatile():
    card_number = "4111111111111113"
    redacted_card_number = vault.redact(card_number, storage='volatile')
    vault.delete(redacted_card_number, storage='volatile')
    revealed_card_number = vault.reveal(redacted_card_number, storage='volatile')

    asserts.assert_that(revealed_card_number).is_equal_to(redacted_card_number)

def _test_empty_reveal():
    alias = "tok_123"
    revealed = vault.reveal(alias)

    asserts.assert_that(revealed).is_equal_to(alias)

def _test_invalid_list_reveal():
    alias = ["tok_123"]

    asserts.assert_fails(lambda : vault.reveal(alias),
        "Value of type net.starlark.java.eval.StarlarkList is not supported in DefaultVault, expecting String"
    )

def _test_persistent_storage():
    card_number = "4111111111111114"
    redacted_card_number = vault.redact(card_number)

    revealed_card_number_implicit = vault.reveal(redacted_card_number) # default storage
    revealed_card_number_explicit = vault.reveal(redacted_card_number, storage='persistent')
    revealed_card_number_volatile = vault.reveal(redacted_card_number, storage='volatile')

    asserts.assert_that(revealed_card_number_implicit).is_equal_to(card_number)
    asserts.assert_that(revealed_card_number_explicit).is_equal_to(card_number)
    asserts.assert_that(revealed_card_number_volatile).is_equal_to(redacted_card_number)

def _test_volatile_storage():
    card_number = "4111111111111115"
    redacted_card_number = vault.redact(card_number, storage='volatile')

    revealed_card_number_volatile = vault.reveal(redacted_card_number, storage='volatile')
    revealed_card_number_persistent = vault.reveal(redacted_card_number, storage='persistent')

    asserts.assert_that(revealed_card_number_volatile).is_equal_to(card_number)
    asserts.assert_that(revealed_card_number_persistent).is_equal_to(redacted_card_number)

def _test_unknown_storage():
    card_number = "4111111111111116"
    asserts.assert_fails(lambda : vault.redact(card_number, storage="unknown"),
            "Storage 'unknown' not found in supported storage types: \\[persistent, volatile\\]"
    )

def _test_unknown_format():
    card_number = "4111111111111117"
    asserts.assert_fails(lambda : vault.redact(card_number, format="unknown"),
        "Format 'unknown' not found in supported format types: " +
        "\\[RAW_UUID, UUID, NUM_LENGTH_PRESERVING, PFPT, FPE_SIX_T_FOUR, FPE_T_FOUR, NON_LUHN_FPE_ALPHANUMERIC, " +
        "FPE_SSN_T_FOUR, FPE_ACC_NUM_T_FOUR, FPE_ALPHANUMERIC_ACC_NUM_T_FOUR, GENERIC_T_FOUR, ALPHANUMERIC_SIX_T_FOUR, VGS_FIXED_LEN_GENERIC\\]"
    )

def _test_unsupported_format():
    card_number = "4111111111111117"
    asserts.assert_fails(lambda : vault.redact(card_number, format="ALPHANUMERIC_SIX_T_FOUR"),
        "Format 'ALPHANUMERIC_SIX_T_FOUR' is not supported"
    )

def _test_valid_format_preserving():
    card_number = "12345678"
    redacted_card_number = vault.redact(card_number, format="NUM_LENGTH_PRESERVING")

    asserts.assert_that(len(redacted_card_number)).is_equal_to(len(card_number))
    asserts.assert_that(redacted_card_number).is_not_equal_to(card_number)
    asserts.assert_true(int(card_number))

def _test_invalid_format_preserving():
    input_1 = "12"
    input_2 = "1x2"
    input_3 = "abc"

    redacted_1 = vault.redact(input_1, format="NUM_LENGTH_PRESERVING")
    redacted_2 = vault.redact(input_2, format="NUM_LENGTH_PRESERVING")
    redacted_3 = vault.redact(input_3, format="NUM_LENGTH_PRESERVING")

    asserts.assert_that(len(redacted_1)).is_not_equal_to(len(input_1))
    asserts.assert_that(len(redacted_2)).is_not_equal_to(len(input_2))
    asserts.assert_that(len(redacted_3)).is_not_equal_to(len(input_3))

def _luhn_mod10(digits):
    LUHN_DIGITS = [0, 2, 4, 6, 8, 1, 3, 5, 7, 9]

    sum = 0
    len_d = len(digits)
    for i in range(len_d):
        r_i = len_d-i-1
        d = int(digits[r_i])
        if r_i % 2:
            sum += LUHN_DIGITS[d]
        else:
            sum += d

    return sum%10


def _test_alias_decorator():
    secret = "1111111111"
    alias = vault.redact(secret, decorator_config={
        "searchPattern": "(?<token>[0-9]{6})(?<lastFour>[0-9]{4})",
        "replacePattern": "98${token}${lastFour}",
        "nonLuhnValidPattern": {
            "validatePattern": "[0-9]{6}(?<tokenized>[0-9]{6})[0-9]{4}",
            "transformPatterns": [
                {
                    "search": "(?<token>[0-9]{6})(?<lastFour>[0-9]{4})",
                    "replace": "98${token}${lastFour}",
                }
        ]
    }
    })

    asserts.assert_that(alias).is_not_equal_to(secret)
    asserts.assert_true(alias.startswith("98"))
    asserts.assert_true(alias.endswith("1111"))

def _test_alias_decorator_empty():
    card_number = "4111111111111111"
    redacted_card_number = vault.redact(card_number, decorator_config={})

    asserts.assert_that(redacted_card_number[:4]).is_equal_to('tok_')

def _test_invalid_alias_decorator_invalid_config_format():
    asserts.assert_fails(lambda : vault.redact("whatever", decorator_config="invalid"),
     """in call to redact\\(\\), parameter 'decorator_config' got value of type 'string', want 'NoneType or Map'"""
     )

def _test_invalid_alias_decorator_invalid_config_search_pattern():
    asserts.assert_fails(lambda : vault.redact("1111111111", decorator_config={
        "searchPattern": {},
        "replacePattern": "98${token}${lastFour}"
    }),
    """Decorator config '{"searchPattern": {}, "replacePattern": "98\\${token}\\${lastFour}"}' is invalid\\. 'searchPattern' field must be string"""
    )

def _test_invalid_alias_decorator_invalid_config_replace_pattern():
    asserts.assert_fails(lambda : vault.redact("1111111111", decorator_config={
        "searchPattern": "(?<token>[0-9]{6})(?<lastFour>[0-9]{4})",
        "replacePattern": ["98${token}${lastFour}"]
    }),
 """Decorator config '{"searchPattern": "\\(\\?<token>\\[0-9]\\{6}\\)\\(\\?<lastFour>\\[0-9\\]\\{4}\\)", "replacePattern": \\["98\\${token}\\${lastFour}"]}' is invalid\\. 'replacePattern' field must be string"""
 )

def _test_invalid_alias_decorator_invalid_config_non_luhn_valid_format():
    asserts.assert_fails(lambda : vault.redact("1111111111", decorator_config={
        "nonLuhnValidPattern": "[0-9]+"
    }),
     """Decorator config '{"nonLuhnValidPattern": "\\[0-9\\]\\+"}' is invalid\\. 'nonLuhnValidPattern' field must be dict"""
     )

def _test_invalid_alias_decorator_invalid_config_non_luhn_valid_validate_pattern():
    asserts.assert_fails(lambda : vault.redact("1111111111", decorator_config={
        "nonLuhnValidPattern": {
            "validatePattern": [],
            "transformPatterns": [
                {
                    "search": "[0-9]{10}",
                    "replace": "98${token}",
                }
            ]
        }
    }),
     """Decorator config '{"nonLuhnValidPattern": {"validatePattern": \\[\\], "transformPatterns": \\[{"search": "\\[0-9\\]\\{10}", "replace": "98\\${token}"}\\]}}' is invalid\\. 'validatePattern' field must be string"""
     )

def _test_invalid_alias_decorator_invalid_config_non_luhn_valid_transform_patterns():
    asserts.assert_fails(lambda : vault.redact("1111111111", decorator_config={
        "nonLuhnValidPattern": {
            "validatePattern": "[0-9]{10}",
            "transformPatterns": "[0-9]{10}"
        }
    }),
     """Decorator config '{"nonLuhnValidPattern": {"validatePattern": "\\[0-9\\]\\{10}", "transformPatterns": "\\[0-9\\]\\{10\\}"}}' is invalid\\. 'transformPatterns' field must be array"""
     )

def _test_invalid_alias_decorator_invalid_config_non_luhn_valid_search():
    asserts.assert_fails(lambda : vault.redact("1111111111", decorator_config={
        "nonLuhnValidPattern": {
            "validatePattern": "[0-9]{10}",
            "transformPatterns": [
                {
                    "search": {},
                    "replace": "98${token}",
                }
            ]
        }
    }),
     """Decorator config '{"nonLuhnValidPattern": {"validatePattern": "\\[0-9\\]\\{10}", "transformPatterns": \\[{"search": \\{}, "replace": "98\\${token}"}\\]}}' is invalid\\. 'search' field must be string"""
     )

def _test_invalid_alias_decorator_invalid_config_non_luhn_valid_replace():
    asserts.assert_fails(lambda : vault.redact("1111111111", decorator_config={
        "nonLuhnValidPattern": {
            "validatePattern": "[0-9]{10}",
            "transformPatterns": [
                {
                    "search": "[0-9]{10}",
                    "replace": ["98${token}", "99${token}"],
                }
            ]
        }
    }),
     """Decorator config '{"nonLuhnValidPattern": {"validatePattern": "\\[0-9\\]\\{10}", "transformPatterns": \\[{"search": "\\[0-9\\]\\{10}", "replace": \\["98\\${token}", "99\\${token}"\\]}\\]}}' is invalid\\. 'replace' field must be string"""
     )

def _test_default_sign():
    message = "message"
    signature = vault.sign("keyId", message, "RSASSA_PSS_SHA_256")
    asserts.assert_that(message).is_equal_to(signature)

def _test_sign_bad_algorithm():
    asserts.assert_fails(lambda: vault.sign("keyId", "message", "bad_algo"), "Algorithm 'bad_algo' not found in supported algorithms: \\[RSASSA_PSS_SHA_256, RSASSA_PSS_SHA_384, RSASSA_PSS_SHA_512, ECDSA_SHA_256, ECDSA_SHA_384, ECDSA_SHA_512\\]")

def _test_default_verify():
    message = "message"
    signature = vault.sign("keyId", message, "RSASSA_PSS_SHA_256")
    valid = vault.verify("keyId", message, signature, "RSASSA_PSS_SHA_256")
    asserts.assert_false(valid)

def _suite():
    _suite = unittest.TestSuite()

    # Redact Tests
    _suite.addTest(unittest.FunctionTestCase(_test_default_redact))
    _suite.addTest(unittest.FunctionTestCase(_test_params_redact))
    _suite.addTest(unittest.FunctionTestCase(_test_invalid_list_redact))

    # Reveal Tests
    _suite.addTest(unittest.FunctionTestCase(_test_default_reveal))
    _suite.addTest(unittest.FunctionTestCase(_test_empty_reveal))
    _suite.addTest(unittest.FunctionTestCase(_test_invalid_list_reveal))

    # Delete Tests
    _suite.addTest(unittest.FunctionTestCase(_test_default_delete))
    _suite.addTest(unittest.FunctionTestCase(_test_default_delete_volatile))

    # Storage Tests
    _suite.addTest(unittest.FunctionTestCase(_test_persistent_storage))
    _suite.addTest(unittest.FunctionTestCase(_test_volatile_storage))
    _suite.addTest(unittest.FunctionTestCase(_test_unknown_storage))

    # Format Tests
    _suite.addTest(unittest.FunctionTestCase(_test_unknown_format))
    _suite.addTest(unittest.FunctionTestCase(_test_unsupported_format))
    _suite.addTest(unittest.FunctionTestCase(_test_valid_format_preserving))
    _suite.addTest(unittest.FunctionTestCase(_test_invalid_format_preserving))

    # Alias Decorator Tests
    _suite.addTest(unittest.FunctionTestCase(_test_alias_decorator))
    _suite.addTest(unittest.FunctionTestCase(_test_alias_decorator_empty))
    _suite.addTest(unittest.FunctionTestCase(_test_invalid_alias_decorator_invalid_config_format))
    _suite.addTest(unittest.FunctionTestCase(_test_invalid_alias_decorator_invalid_config_search_pattern))
    _suite.addTest(unittest.FunctionTestCase(_test_invalid_alias_decorator_invalid_config_replace_pattern))
    _suite.addTest(unittest.FunctionTestCase(_test_invalid_alias_decorator_invalid_config_non_luhn_valid_format))
    _suite.addTest(unittest.FunctionTestCase(_test_invalid_alias_decorator_invalid_config_non_luhn_valid_validate_pattern))
    _suite.addTest(unittest.FunctionTestCase(_test_invalid_alias_decorator_invalid_config_non_luhn_valid_transform_patterns))
    _suite.addTest(unittest.FunctionTestCase(_test_invalid_alias_decorator_invalid_config_non_luhn_valid_search))
    _suite.addTest(unittest.FunctionTestCase(_test_invalid_alias_decorator_invalid_config_non_luhn_valid_replace))

    # Sign and Verify Tests
    _suite.addTest(unittest.FunctionTestCase(_test_default_sign))
    _suite.addTest(unittest.FunctionTestCase(_test_sign_bad_algorithm))
    _suite.addTest(unittest.FunctionTestCase(_test_default_verify))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
