load("@vendor//asserts", "asserts")
load("@vendor//vgs//nts_helpers", "nts_helpers")
load("@stdlib//unittest", "unittest")


def make_fixture():
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


def _test_render():
    output = nts_helpers.render(
        make_fixture(),
        pan="$.paymentMethod.number",
        cvv="$.paymentMethod.cvv",
        amount="$.amount.value",
        currency_code="$.amount.currency",
        output_exp_month="$.paymentMethod.expiryMonth",
        output_exp_year="$.paymentMethod.expiryYear",
        output_cryptogram_value="$.mpiData.cavv",
        output_cryptogram_eci="$.mpiData.eci",
    )
    asserts.assert_that(output["paymentMethod"]["number"]).is_equal_to("4242424242424242")
    asserts.assert_that(output["paymentMethod"]["expiryMonth"]).is_equal_to(12)
    asserts.assert_that(output["paymentMethod"]["expiryYear"]).is_equal_to(27)
    asserts.assert_that(output["mpiData"]["cavv"]).is_equal_to("MOCK_CRYPTOGRAM_VALUE")
    asserts.assert_that(output["mpiData"]["eci"]).is_equal_to("MOCK_CRYPTOGRAM_ECI")


def _test_render_with_raw_values_input():
    output = nts_helpers.render(
        make_fixture(),
        pan="MOCK_PAN_ALIAS",
        cvv="123",
        amount="45.67",
        currency_code="USD",
        output_pan="$.paymentMethod.number",
        output_exp_month="$.paymentMethod.expiryMonth",
        output_exp_year="$.paymentMethod.expiryYear",
        output_cryptogram_value="$.mpiData.cavv",
        output_cryptogram_eci="$.mpiData.eci",
        raw_pan=True,
        raw_amount=True,
        raw_cvv=True,
        raw_currency_code=True
    )
    asserts.assert_that(output["paymentMethod"]["number"]).is_equal_to("4242424242424242")
    asserts.assert_that(output["paymentMethod"]["expiryMonth"]).is_equal_to(12)
    asserts.assert_that(output["paymentMethod"]["expiryYear"]).is_equal_to(27)
    asserts.assert_that(output["mpiData"]["cavv"]).is_equal_to("MOCK_CRYPTOGRAM_VALUE")
    asserts.assert_that(output["mpiData"]["eci"]).is_equal_to("MOCK_CRYPTOGRAM_ECI")


def _test_render_pan_empty_value():
    asserts.assert_fails(
        lambda: nts_helpers.render(
            {},
            pan="",
            cvv="MOCK_CVV",
            amount="MOCK_AMOUNT",
            currency_code="MOCK_CURRENCY_CODE",
            raw_pan=True,
            raw_amount=True,
            raw_cvv=True,
            raw_currency_code=True
        ),
        "pan argument cannot be blank",
    )


def _test_render_not_found():
    asserts.assert_fails(
        lambda: nts_helpers.render(
            {},
            pan="NOT_FOUND",
            cvv="MOCK_CVV",
            amount="MOCK_AMOUNT",
            currency_code="MOCK_CURRENCY_CODE",
            raw_pan=True,
            raw_amount=True,
            raw_cvv=True,
            raw_currency_code=True
        ),
        "network token is not found",
    )


def _suite():
    _suite = unittest.TestSuite()

    # Redact Tests
    _suite.addTest(unittest.FunctionTestCase(_test_render))
    _suite.addTest(unittest.FunctionTestCase(_test_render_with_raw_values_input))
    _suite.addTest(unittest.FunctionTestCase(_test_render_pan_empty_value))
    _suite.addTest(unittest.FunctionTestCase(_test_render_not_found))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
