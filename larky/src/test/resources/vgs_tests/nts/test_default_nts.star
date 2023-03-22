load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//nts", "nts")


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
        "cryptogram_eci": "MOCK_CRYPTOGRAM_ECI"
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


def _suite():
    _suite = unittest.TestSuite()

    # Get network token tests
    _suite.addTest(unittest.FunctionTestCase(_test_get_network_token))
    _suite.addTest(unittest.FunctionTestCase(_test_get_network_token_pan_empty_value))
    _suite.addTest(unittest.FunctionTestCase(_test_get_network_token_not_found))
    # Render tests
    _suite.addTest(unittest.FunctionTestCase(_test_render))
    _suite.addTest(unittest.FunctionTestCase(_test_render_pan_empty_value))
    _suite.addTest(unittest.FunctionTestCase(_test_render_not_found))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
