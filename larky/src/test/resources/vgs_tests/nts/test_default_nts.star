load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//nts", "nts")
load("@vgs//vault", vault="vault")


def _make_fixture():
    return {
        "merchantAccount": "YOUR_MERCHANT_ACCOUNT",
        "reference": "YOUR_PAYMENT_REFERENCE",
        "amount": {
            "currency": "USD",
            "value": 1000,
        },
        "paymentMethod": {
            "type": "networkToken",
            "holderName": "CARDHOLDER_NAME",
            "number": "785840aLpH4nUmV9985",
            "expiryMonth": "TO_BE_REPLACED",
            "expiryYear": "TO_BE_REPLACED",
            "cvv": "123",
        },
        "returnUrl": "https://your-company.com/",
        "shopperReference": "YOUR_SHOPPER_REFERENCE",
        "recurringProcessingModel": "CardOnFile",
        "shopperInteraction": "Ecommerce",
        # Deep JSONPath is not supported by the JSONPath lib, so that we need to
        # create an empty object here manually.
        # ref: https://github.com/json-path/JsonPath/issues/83
        "mpiData": {
            "cavv": "TO_BE_REPLACED",
            "eci": "TO_BE_REPLACED",
        }
    }


def _test_get_network_token():
    output = nts.get_network_token(
        pan="MOCK_PAN_ALIAS",
        cvv="MOCK_CVV",
        amount="123.45",
        currency_code="USD",
    )
    asserts.assert_that(output).is_equal_to({
        "token": "4242424242424242",
        "exp_month": 12,
        "exp_year": 27,
        "cryptogram_value": "MOCK_CRYPTOGRAM_VALUE",
        "cryptogram_eci": "MOCK_CRYPTOGRAM_ECI",
        "cryptogram_type": "TAVV"
    })


def _test_get_network_token_with_dtvv_type():
    output = nts.get_network_token(
        pan="MOCK_PAN_ALIAS",
        cvv="MOCK_CVV",
        amount="123.45",
        currency_code="USD",
        cryptogram_type="DTVV",
    )
    asserts.assert_that(output).is_equal_to({
        "token": "4242424242424242",
        "exp_month": 12,
        "exp_year": 27,
        "cryptogram_value": "MOCK_DYNAMIC_CVV",
        "cryptogram_eci": "MOCK_CRYPTOGRAM_ECI",
        "cryptogram_type": "DTVV",
    })


def _test_get_network_token_pan_empty_value():
    asserts.assert_fails(lambda: nts.get_network_token("", cvv="MOCK_CVV", amount="123.45", currency_code="USD"),
                         "pan argument cannot be blank")


def _test_get_network_token_not_found():
    input = {
        "pan": "NOT_FOUND",
    }
    asserts.assert_fails(
        lambda: nts.get_network_token("NOT_FOUND", cvv="MOCK_CVV", amount="123.45", currency_code="USD"),
        "network token is not found")


def _test_render():
    output = nts.render(
        _make_fixture(),
        pan="$.paymentMethod.number",
        cvv="$.paymentMethod.cvv",
        amount="$.amount.value",
        currency_code="$.amount.currency",
        exp_month="$.paymentMethod.expiryMonth",
        exp_year="$.paymentMethod.expiryYear",
        cryptogram_value="$.mpiData.cavv",
        cryptogram_eci="$.mpiData.eci",
    )
    asserts.assert_that(output["paymentMethod"]["number"]).is_equal_to("4242424242424242")
    asserts.assert_that(output["paymentMethod"]["expiryMonth"]).is_equal_to(12)
    asserts.assert_that(output["paymentMethod"]["expiryYear"]).is_equal_to(27)
    asserts.assert_that(output["mpiData"]["cavv"]).is_equal_to("MOCK_CRYPTOGRAM_VALUE")
    asserts.assert_that(output["mpiData"]["eci"]).is_equal_to("MOCK_CRYPTOGRAM_ECI")


def _test_render_with_dcvv():
    input = _make_fixture()
    input["paymentMethod"]["cvv"] = "USE_DYNAMIC_CVV"
    output = nts.render(
        input,
        pan="$.paymentMethod.number",
        dcvv="$.paymentMethod.cvv",
        amount="$.amount.value",
        currency_code="$.amount.currency",
    )
    asserts.assert_that(output["paymentMethod"]["cvv"]).is_equal_to("MOCK_DYNAMIC_CVV")


def _test_render_pan_empty_value():
    asserts.assert_fails(
        lambda: nts.render(
            {"pan": "", "cvv": "MOCK_CVV", "amount": "MOCK_AMOUNT", "currency_code": "MOCK_CURRENCY_CODE"},
            pan="$.pan",
            cvv="$.cvv",
            amount="$.amount",
            currency_code="$.currency_code",
        ),
        "pan argument cannot be blank",
    )


def _test_render_not_found():
    asserts.assert_fails(
        lambda: nts.render(
            {"pan": "NOT_FOUND", "cvv": "MOCK_CVV", "amount": "MOCK_AMOUNT", "currency_code": "MOCK_CURRENCY_CODE"},
            pan="$.pan",
            cvv="$.cvv",
            amount="$.amount",
            currency_code="$.currency_code",
        ),
        "network token is not found",
    )


def _test_render_with_both_cvv_and_dcvv():
    asserts.assert_fails(
        lambda: nts.render(
            _make_fixture(),
            pan="$.paymentMethod.number",
            cvv="$.paymentMethod.cvv",
            dcvv="$.paymentMethod.cvv",
            amount="$.amount.value",
            currency_code="$.amount.currency",
        ),
        "ValueError: only either one of cvv or dvcc can be provided",
    )


def _test_render_without_either_cvv_or_dcvv():
    asserts.assert_fails(
        lambda: nts.render(
            _make_fixture(),
            pan="$.paymentMethod.number",
            amount="$.amount.value",
            currency_code="$.amount.currency",
        ),
        "ValueError: either one of cvv or dvcc need to be provided",
    )


def _test_supports_dcvv_returns_true():
    input = {
        "payload": {
            "number": vault.redact("4242424242424242")
        }
    }
    asserts.assert_that(nts.supports_dcvv(input, "$.payload.number")).is_true()


def _test_supports_dcvv_returns_false():
    input = {
        "payload": {
            "number": vault.redact("5555555555554444")
        }
    }
    asserts.assert_that(nts.supports_dcvv(input, "$.payload.number")).is_false()


def _suite():
    _suite = unittest.TestSuite()

    # Get network token tests
    _suite.addTest(unittest.FunctionTestCase(_test_get_network_token))
    _suite.addTest(unittest.FunctionTestCase(_test_get_network_token_with_dtvv_type))
    _suite.addTest(unittest.FunctionTestCase(_test_get_network_token_pan_empty_value))
    _suite.addTest(unittest.FunctionTestCase(_test_get_network_token_not_found))
    # Render tests
    _suite.addTest(unittest.FunctionTestCase(_test_render))
    _suite.addTest(unittest.FunctionTestCase(_test_render_with_dcvv))
    _suite.addTest(unittest.FunctionTestCase(_test_render_pan_empty_value))
    _suite.addTest(unittest.FunctionTestCase(_test_render_not_found))
    _suite.addTest(unittest.FunctionTestCase(_test_render_with_both_cvv_and_dcvv))
    _suite.addTest(unittest.FunctionTestCase(_test_render_without_either_cvv_or_dcvv))
    _suite.addTest(unittest.FunctionTestCase(_test_supports_dcvv_returns_true))
    _suite.addTest(unittest.FunctionTestCase(_test_supports_dcvv_returns_false))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
