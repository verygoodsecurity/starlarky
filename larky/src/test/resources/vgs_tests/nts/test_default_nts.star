load("@stdlib//larky", "larky")
load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//nts", "nts")
load("@vgs//vault", vault="vault")
load("@vendor//option/result", safe="safe")


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
        },
        "vgs_merchant_id": "MCdAhTydCJMZEzxgqhvVdkgo"
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


def _test_get_network_token_for_merchant():
    output = nts.get_network_token(
        pan="MOCK_PAN_ALIAS",
        cvv="MOCK_CVV",
        amount="123.45",
        currency_code="USD",
        vgs_merchant_id="MCdAhTydCJMZEzxgqhvVdkgo",
    )
    asserts.assert_that(output).is_equal_to({
        "token": "4111111111111111",
        "exp_month": 10,
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


def _test_render_for_merchant():
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
        vgs_merchant_id="MCdAhTydCJMZEzxgqhvVdkgo",
    )
    asserts.assert_that(output["paymentMethod"]["number"]).is_equal_to("4111111111111111")
    asserts.assert_that(output["paymentMethod"]["expiryMonth"]).is_equal_to(10)
    asserts.assert_that(output["paymentMethod"]["expiryYear"]).is_equal_to(27)
    asserts.assert_that(output["mpiData"]["cavv"]).is_equal_to("MOCK_CRYPTOGRAM_VALUE")
    asserts.assert_that(output["mpiData"]["eci"]).is_equal_to("MOCK_CRYPTOGRAM_ECI")


def _test_render_for_merchant_from_jsonpath():
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
        vgs_merchant_id="$.vgs_merchant_id",
    )
    asserts.assert_that(output["paymentMethod"]["number"]).is_equal_to("4111111111111111")
    asserts.assert_that(output["paymentMethod"]["expiryMonth"]).is_equal_to(10)
    asserts.assert_that(output["paymentMethod"]["expiryYear"]).is_equal_to(27)
    asserts.assert_that(output["mpiData"]["cavv"]).is_equal_to("MOCK_CRYPTOGRAM_VALUE")
    asserts.assert_that(output["mpiData"]["eci"]).is_equal_to("MOCK_CRYPTOGRAM_ECI")


def _test_render_for_merchant_from_jsonpath_not_found():
    asserts.assert_fails(
        lambda: nts.render(
            _make_fixture(),
            pan="$.paymentMethod.number",
            cvv="$.paymentMethod.cvv",
            amount="$.amount.value",
            currency_code="$.amount.currency",
            exp_month="$.paymentMethod.expiryMonth",
            exp_year="$.paymentMethod.expiryYear",
            cryptogram_value="$.mpiData.cavv",
            cryptogram_eci="$.mpiData.eci",
            vgs_merchant_id="$.unknown_vgs_merchant_id",
        ),
        'Key "{unknown_vgs_merchant_id}" does not exist in node',
    )


def _test_render_for_merchant_not_found():
    asserts.assert_fails(
        lambda: nts.render(
            _make_fixture(),
            pan="$.paymentMethod.number",
            cvv="$.paymentMethod.cvv",
            amount="$.amount.value",
            currency_code="$.amount.currency",
            exp_month="$.paymentMethod.expiryMonth",
            exp_year="$.paymentMethod.expiryYear",
            cryptogram_value="$.mpiData.cavv",
            cryptogram_eci="$.mpiData.eci",
            vgs_merchant_id="vgs_merchant_id_not_found",
        ),
        "network token is not found",
    )


def _test_render_with_nested_safe():
    result = safe(nts.render)(
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
    print(result.unwrap())
    asserts.assert_that(result.is_ok).is_true()


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


def _test_render_without_cvv_json_path():
    output = nts.render(
        _make_fixture(),
        pan="$.paymentMethod.number",
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
    asserts.assert_that(output["mpiData"]["eci"]).is_equal_to("MOCK_CRYPTOGRAM_ECI")


def _test_render_without_cvv_value():
    input = _make_fixture()
    input["paymentMethod"].pop("cvv")
    output = nts.render(
        input,
        pan="$.paymentMethod.number",
        cvv="$.paymentMethod.cvv",
        amount="$.amount.value",
        currency_code="$.amount.currency",
    )
    asserts.assert_that("cvv" in output["paymentMethod"]).is_false()


def _test_render_without_dcvv_value():
    input = _make_fixture()
    input["paymentMethod"].pop("cvv")
    output = nts.render(
        input,
        pan="$.paymentMethod.number",
        dcvv="$.paymentMethod.cvv",
        amount="$.amount.value",
        currency_code="$.amount.currency",
    )
    asserts.assert_that("cvv" in output["paymentMethod"]).is_false()


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


def _test_get_psp_type():
    for url, psp_type in [
        ("https://example.com", nts.PSPType.UNKNOWN),
        ("https://api.stripe.com/v1/charges", nts.PSPType.STRIPE),
        ("https://checkout-test.adyen.com/v68/payments", nts.PSPType.ADYEN),
        ("https://checkoutshopper-test.adyen.com/v68/payments", nts.PSPType.ADYEN),
        ("https://checkoutshopper-live.adyen.com/v68/payments", nts.PSPType.ADYEN),
        ("https://checkoutshopper-live-us.adyen.com/v68/payments", nts.PSPType.ADYEN),
    ]:
        asserts.assert_that(nts.get_psp_type(larky.struct(url=url))).is_equal_to(psp_type)

def _test_use_network_token():
    asserts.assert_that(nts.use_network_token({})).is_false()
    asserts.assert_that(nts.use_network_token({"vgs-network-token": ""})).is_false()
    asserts.assert_that(nts.use_network_token({"vgs-network-token": "no"})).is_false()
    asserts.assert_that(nts.use_network_token({"vgs-network-token": "other"})).is_false()
    asserts.assert_that(nts.use_network_token({"vgs-network-token": "yes"})).is_true()
    asserts.assert_that(nts.use_network_token({"Vgs-Network-Token": "yes"})).is_true()
    asserts.assert_that(nts.use_network_token({"Vgs-Network-Token": "Yes"})).is_true()
    asserts.assert_that(nts.use_network_token({"VGS-NETWORK-TOKEN": "YES"})).is_true()


def _test_supports_cryptogram():
    for url, result in [
        ("https://example.com", False),
        ("https://api.stripe.com/v1/charges", True),
        ("https://checkout-test.adyen.com/v68/payments", True),
    ]:
        asserts.assert_that(nts.supports_cryptogram(larky.struct(url=url))).is_equal_to(result)


def _test_is_token_connect_enrollment():
    asserts.assert_that(nts.is_token_connect_enrollment({})).is_false()
    asserts.assert_that(nts.is_token_connect_enrollment({"mock-key": "value"})).is_false()

    asserts.assert_that(nts.is_token_connect_enrollment({"push_account_receipt": "MOCK_VALUE"})).is_true()
    asserts.assert_that(nts.is_token_connect_enrollment({"push_account_data": "MOCK_VALUE"})).is_true()


def _test_extract_push_account_receipt():
    asserts.assert_that(nts.extract_push_account_receipt({"push_account_receipt": "MOCK_VALUE"})).is_equal_to("MOCK_VALUE")
    # signature with just one push account receipt
    push_account_data = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjIwMjEwOTI3MDkxMzQwLU1ERVMtdG9rZW4tY29ubmVjdC1tdGYifQ.eyJwdXNoQWNjb3VudFJlY2VpcHRzIjpbIk1DQy1TVEwtRkQxOTgwRTEtNTUwNy00N0E2LTg5OTEtOUZCRTJCMTE4N0Y5Il0sImNhbGxiYWNrVVJMIjoiaHR0cHM6Ly90b2tlbmNvbm5lY3QubWNzcmN0ZXN0c3RvcmUuY29tL3Rva2VuaXphdGlvbi1yZXN1bHRzIiwiY29tcGxldGVXZWJzaXRlQWN0aXZhdGlvbiI6dHJ1ZSwiYWNjb3VudEhvbGRlckRhdGFTdXBwbGllZCI6dHJ1ZSwibG9jYWxlIjoiZW5fVVMifQ.AbNdt-SKiOvXsB79k7Dl2_pOnSZWIN6gN9aT7uUgybhiLxA7QF59qUVoA0pzCb1aAdx6_yPzwkLCks_mhAR94LCdwdhXQl3ixD666cmai-IzBkRo7MGx3ZDykjN4qC1YbTeSREsRlO4pN0wJJfvsua3WCCH-ivcn9rEc02oDRak5OfU2qX1REtn_OHZ-6eb1uR12YpzSswJZzL2VRJArpAou_C5lyK6wy5HrdoAxUsJ1CY9KCePSqbHC1xTa-dllJoOt-pG6kTYSRKkCG2b0mHsLcYitrOD8hmqQrIwXGWs0Tc5XPQTUkYMbLXAN6KihQ3RQBW2s-ht5xmVkRwMSkg"
    asserts.assert_that(
        nts.extract_push_account_receipt({
            "push_account_data": push_account_data,
        })
    ).is_equal_to("MCC-STL-FD1980E1-5507-47A6-8991-9FBE2B1187F9")
    asserts.assert_that(
        nts.extract_push_account_receipt({
            "push_account_data": push_account_data,
            "index": 0,
        })
    ).is_equal_to("MCC-STL-FD1980E1-5507-47A6-8991-9FBE2B1187F9")
    asserts.assert_that(
        nts.extract_push_account_receipt({
            "push_account_data": push_account_data,
            "index": 1,
        })
    ).is_none()
    asserts.assert_that(
        nts.extract_push_account_receipt({
            "push_account_data": push_account_data,
            "index": -1,
        })
    ).is_none()
    # signature with multiple push account receipts
    push_account_data = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjIwMjEwOTI3MDkxMzQwLU1ERVMtdG9rZW4tY29ubmVjdC1tdGYifQ.eyJwdXNoQWNjb3VudFJlY2VpcHRzIjpbIk1DQy1TVEwtRDBCOUM4NkMtQjRFQS00REZGLTg1NkUtMTE0MzdCOUQwNTg2IiwiTVNJLVNUTC1GRjcxQTVCQi0yM0NDLTREMDEtQUZGOC0zNzYxMThGMTREMzciLCJNQ0MtU1RMLTQ3MUUwQUQ4LUUyMzMtNDkyRC04RkZFLTA2MjgzQ0JENTAxOCJdLCJjYWxsYmFja1VSTCI6Imh0dHBzOi8vdG9rZW5jb25uZWN0Lm1jc3JjdGVzdHN0b3JlLmNvbS90b2tlbml6YXRpb24tcmVzdWx0cyIsImNvbXBsZXRlV2Vic2l0ZUFjdGl2YXRpb24iOnRydWUsImFjY291bnRIb2xkZXJEYXRhU3VwcGxpZWQiOnRydWUsImxvY2FsZSI6ImVuX1VTIn0.wurTMGRXU2Icu8kHmsndHafW-LOjd6NQBWsM8IqsSqCGDCUg9aVwAyltaa1g-7WtSY9R9A2KuRNfr6GutnVnkDN7hIM3j3MXiiiuHCnDnVt4qfe1VyfHmqlZU4WoIBjYssa7NVUA8etLvzZuJzTn0PiN3RdESZ_PXBVUggNV89UWLwFGINF7m9HW6uG3gwSg5c9SE9LXCX_aFLZHgb0D1s3G0fLkOxXxkiA81rJO9xlTJiMLrQEpIYS-iP0FT4s3rRnBd2Z2XVt8az0ybCcEMXIcAblfIsRq6yRZXPdWi8eyULx9Jpf9bhga4UP6HgfxtbW5nLHbV5BV9OYgTNfa_A"
    asserts.assert_that(
        nts.extract_push_account_receipt({
            "push_account_data": push_account_data,
            "index": 0,
        })
    ).is_equal_to("MCC-STL-D0B9C86C-B4EA-4DFF-856E-11437B9D0586")
    asserts.assert_that(
        nts.extract_push_account_receipt({
            "push_account_data": push_account_data,
            "index": 1,
        })
    ).is_equal_to("MSI-STL-FF71A5BB-23CC-4D01-AFF8-376118F14D37")
    asserts.assert_that(
        nts.extract_push_account_receipt({
            "push_account_data": push_account_data,
            "index": 2,
        })
    ).is_equal_to("MCC-STL-471E0AD8-E233-492D-8FFE-06283CBD5018")
    asserts.assert_that(
        nts.extract_push_account_receipt({
            "push_account_data": push_account_data,
            "index": 3,
        })
    ).is_none()


def _test_is_mastercard_push_account_receipt():
    for pan, result in [
        ("4111111111111111", False),
        ("5555555555554444", False),
        ("Other", False),
        ("MCC-STL-471E0AD8-E233-492D-8FFE-06283CBD5018", True),
    ]:
        pan_alias = vault.redact(pan)
        asserts.assert_that(
            nts.is_mastercard_push_account_receipt(pan_alias)
        ).is_equal_to(result)


def _suite():
    _suite = unittest.TestSuite()

    # Get network token tests
    _suite.addTest(unittest.FunctionTestCase(_test_get_network_token))
    _suite.addTest(unittest.FunctionTestCase(_test_get_network_token_for_merchant))
    _suite.addTest(unittest.FunctionTestCase(_test_get_network_token_with_dtvv_type))
    _suite.addTest(unittest.FunctionTestCase(_test_get_network_token_pan_empty_value))
    _suite.addTest(unittest.FunctionTestCase(_test_get_network_token_not_found))
    # Render tests
    _suite.addTest(unittest.FunctionTestCase(_test_render))
    _suite.addTest(unittest.FunctionTestCase(_test_render_for_merchant))
    _suite.addTest(unittest.FunctionTestCase(_test_render_for_merchant_from_jsonpath))
    _suite.addTest(unittest.FunctionTestCase(_test_render_for_merchant_from_jsonpath_not_found))
    _suite.addTest(unittest.FunctionTestCase(_test_render_for_merchant_not_found))
    _suite.addTest(unittest.FunctionTestCase(_test_render_with_nested_safe))
    _suite.addTest(unittest.FunctionTestCase(_test_render_without_cvv_value))
    _suite.addTest(unittest.FunctionTestCase(_test_render_with_dcvv))
    _suite.addTest(unittest.FunctionTestCase(_test_render_without_cvv_json_path))
    _suite.addTest(unittest.FunctionTestCase(_test_render_pan_empty_value))
    _suite.addTest(unittest.FunctionTestCase(_test_render_not_found))
    _suite.addTest(unittest.FunctionTestCase(_test_render_with_both_cvv_and_dcvv))
    # Support DCVV tests
    _suite.addTest(unittest.FunctionTestCase(_test_supports_dcvv_returns_true))
    _suite.addTest(unittest.FunctionTestCase(_test_supports_dcvv_returns_false))
    # Use network token tests
    _suite.addTest(unittest.FunctionTestCase(_test_use_network_token))
    # Get PSP type tests
    _suite.addTest(unittest.FunctionTestCase(_test_get_psp_type))
    # Support cryptogram tests
    _suite.addTest(unittest.FunctionTestCase(_test_supports_cryptogram))
    # Token connect helper tests
    _suite.addTest(unittest.FunctionTestCase(_test_is_token_connect_enrollment))
    _suite.addTest(unittest.FunctionTestCase(_test_extract_push_account_receipt))
    # is_mastercard_push_account_receipt tests
    _suite.addTest(unittest.FunctionTestCase(_test_is_mastercard_push_account_receipt))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
