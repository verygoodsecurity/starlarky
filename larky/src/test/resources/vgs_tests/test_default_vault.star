"""Unit tests for VaultModule.java using DefaultVault API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//vault", "vault")

def _test_default_redact():
    account_number = "4111111111111111"
    redacted_account_number = vault.redact(account_number)

    asserts.assert_that(redacted_account_number[:4]).is_equal_to('tok_')

def _test_params_redact():
    account_number = "4111111111111112"
    redacted_account_number = vault.redact(
        account_number,
        storage="volatile",
        format="default",
        tags=['tag1','tag2']
    )

    asserts.assert_that(redacted_account_number[:4]).is_equal_to('tok_')

def _test_invalid_list_redact():
    account_number = ["4111111111111111"]

    asserts.assert_fails(lambda : vault.redact(account_number),
        "Value of type net.starlark.java.eval.StarlarkList is not supported in DefaultVault, expecting String"
    )

def _test_default_reveal():
    account_number = "4111111111111113"
    redacted_account_number = vault.redact(account_number)
    revealed_account_number = vault.reveal(redacted_account_number)

    asserts.assert_that(revealed_account_number).is_equal_to(account_number)

def _test_empty_reveal():
    token = "tok_123"
    revealed = vault.reveal(token)

    asserts.assert_that(revealed).is_equal_to("token")

def _test_invalid_list_reveal():
    token = ["tok_123"]

    asserts.assert_fails(lambda : vault.reveal(token),
        "Value of type net.starlark.java.eval.StarlarkList is not supported in DefaultVault, expecting String"
    )

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

def _test_unknown_storage():
    account_number = "4111111111111116"
    asserts.assert_fails(lambda : vault.redact(account_number, storage="unknown"),
            "Storage 'unknown' not found in available storage list \\[persistent, volatile\\]"
    )

def _test_unknown_format():
    account_number = "4111111111111117"
    asserts.assert_fails(lambda : vault.redact(account_number, format="unknown"),
        "Format 'unknown' not found"
    )

def _test_valid_format_pfpt():
    account_number = "4444333322221111"
    redacted_account_number = vault.redact(account_number, format="pfpt")

    asserts.assert_that(len(redacted_account_number)).is_equal_to(19)
    asserts.assert_that(redacted_account_number[:5]).is_equal_to('99144')
    asserts.assert_that(redacted_account_number[-4:]).is_equal_to('1111')
    asserts.assert_that(_luhn_mod10(redacted_account_number)).is_equal_to(0)

def _test_invalid_format_pfpt():
    account_number = "4111111111111118"
    redacted_account_number = vault.redact(account_number, format="pfpt")

    asserts.assert_that(len(redacted_account_number)).is_not_equal_to(19)
    asserts.assert_that(redacted_account_number).is_not_equal_to(account_number)
    asserts.assert_that(redacted_account_number[:3]).is_not_equal_to('991')

def _test_valid_format_preserving():
    account_number = "12345678"
    redacted_account_number = vault.redact(account_number, format="num_preserving")

    asserts.assert_that(len(redacted_account_number)).is_equal_to(len(account_number))
    asserts.assert_that(redacted_account_number).is_not_equal_to(account_number)
    asserts.assert_true(int(account_number))

def _test_invalid_format_preserving():
    input_1 = "12"
    input_2 = "1x2"
    input_3 = "abc"

    redacted_1 = vault.redact(input_1, format="num_preserving")
    redacted_2 = vault.redact(input_2, format="num_preserving")
    redacted_3 = vault.redact(input_3, format="num_preserving")

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

    # Storage Tests
    _suite.addTest(unittest.FunctionTestCase(_test_persistent_storage))
    _suite.addTest(unittest.FunctionTestCase(_test_volatile_storage))
    _suite.addTest(unittest.FunctionTestCase(_test_unknown_storage))

    # Format Tests
    _suite.addTest(unittest.FunctionTestCase(_test_unknown_format))
    _suite.addTest(unittest.FunctionTestCase(_test_valid_format_pfpt))
    _suite.addTest(unittest.FunctionTestCase(_test_invalid_format_pfpt))
    _suite.addTest(unittest.FunctionTestCase(_test_valid_format_preserving))
    _suite.addTest(unittest.FunctionTestCase(_test_invalid_format_preserving))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
