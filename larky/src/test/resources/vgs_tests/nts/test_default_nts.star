load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//nts", "nts")


def _test_render():
    input = {
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

    output = nts.render(
        input,
        pan="$.paymentMethod.number",
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


def _test_render_json_path_list_match():
    input = {
        "pan": "785840aLpH4nUmV9985",
        "data": [
            {"token": "TO_BE_REPLACED"},
            {"token": "TO_BE_REPLACED"},
            {"token": "TO_BE_REPLACED"},
        ]
    }

    output = nts.render(
        input,
        pan="$.pan",
        cryptogram_value="$.data[*].token",
    )
    asserts.assert_that(len(output["data"])).is_equal_to(3)
    asserts.assert_that(output["data"][0]["token"]).is_equal_to("MOCK_CRYPTOGRAM_VALUE")
    asserts.assert_that(output["data"][1]["token"]).is_equal_to("MOCK_CRYPTOGRAM_VALUE")
    asserts.assert_that(output["data"][2]["token"]).is_equal_to("MOCK_CRYPTOGRAM_VALUE")


def _test_render_not_found():
    input = {
        "pan": "NOT_FOUND",
    }
    asserts.assert_fails(lambda: nts.render(input, "$.pan"), "network token not found")


def _suite():
    _suite = unittest.TestSuite()

    # Redact Tests
    _suite.addTest(unittest.FunctionTestCase(_test_render))
    _suite.addTest(unittest.FunctionTestCase(_test_render_json_path_list_match))
    _suite.addTest(unittest.FunctionTestCase(_test_render_not_found))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
