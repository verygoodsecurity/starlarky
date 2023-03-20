load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//nts", "nts")


def _test_get_network_token():
    output = nts.get_network_token(
        "MOCK_PAN_ALIAS",
    )
    asserts.assert_that(output).is_equal_to({
        "token": "4242424242424242",
        "exp_month": 12,
        "exp_year": 27,
        "cryptogram_value": "MOCK_CRYPTOGRAM_VALUE",
        "cryptogram_eci": "MOCK_CRYPTOGRAM_ECI"
    })


def _test_pan_empty_value():
    asserts.assert_fails(lambda: nts.get_network_token(""), "pan argument cannot be blank")


def _test_not_found():
    input = {
        "pan": "NOT_FOUND",
    }
    asserts.assert_fails(lambda: nts.get_network_token("NOT_FOUND"), "network token is not found")


def _suite():
    _suite = unittest.TestSuite()

    # Redact Tests
    _suite.addTest(unittest.FunctionTestCase(_test_get_network_token))
    _suite.addTest(unittest.FunctionTestCase(_test_pan_empty_value))
    _suite.addTest(unittest.FunctionTestCase(_test_not_found))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
