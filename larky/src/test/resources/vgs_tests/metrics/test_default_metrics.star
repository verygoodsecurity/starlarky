"""Unit tests for MetricsModule.java using DefaultMetrics API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//metrics", "metrics")


def _test_default_track_no_args():
    metrics.track()


def _test_default_track_None_args():
    metrics.track(None, None, None, None, None, None)


def _test_default_track_blank_amount_bin():
    asserts.assert_fails(
        lambda: metrics.track("", ""),
        "parameter 'amount' invalid value, want 'int'")


def _test_default_track_blank_enums():
    metrics.track(0, 123456, "", "", "", "")


def _test_default_track_kwargs_keys():
    metrics.track(foo="bar", color="red")


def _test_default_track_kwargs_dict():
    metrics.track(**{"foo": "bar"})


def _test_default_track_kwargs_dict_mapped_keys():
    metrics.track(**{"amount": "123"})


def _test_default_track_multiple_same_key():
    asserts.assert_fails(
        lambda: metrics.track(amount=444, **{"amount": "123"}),
        ".*got multiple values for parameter 'amount'.*")


def _test_default_track_multiple_same_key_invalid_value_in_dict():
    asserts.assert_fails(
        lambda: metrics.track(amount=444, **{"amount": "bar"}),
        ".*got multiple values for parameter 'amount'.*")


def _test_default_track_multiple_same_key_invalid_value():
    asserts.assert_fails(
        lambda: metrics.track(amount="bar", **{"amount": "123"}),
        ".*got multiple values for parameter 'amount'.*")


def _test_default_track_without_keys():
    # will be mapped to amount and bin
    asserts.assert_fails(
        lambda: metrics.track("bar", "red"),
        "parameter 'amount' invalid value, want 'int'")


def _test_default_track_amount_bin_alpha_numeric():
    asserts.assert_fails(
        lambda: metrics.track(bin="abcde", amount="23wed"),
        "parameter 'amount' invalid value, want 'int'")


def _test_default_track_extra_values_without_keys():
    asserts.assert_fails(
        lambda: metrics.track("a", "b", "c", "p", "r", "t", "d", "e"),
        ".*accepts no more than 6 positional arguments but got 8.*")


def _test_default_track_extra_values_with_keys_dict():
    metrics.track("123", "123456", "c", "p", "r", "t", d="d", e="e")


def _test_default_track_named_key_values():
    metrics.track(
        amount=123,
        bin=123456,
        currency="USD",
        psp="ADYEN",
        result="SUCCESS",
        type="AUTHORIZATION")


def _test_default_track_bin_out_of_range():
    asserts.assert_fails(
        lambda: metrics.track(
            amount=123,
            bin=1234,
            currency="USD",
            psp="ADYEN",
            result="SUCCESS",
            type="AUTHORIZATION"),
        "parameter 'bin' length must be in range \\[6,9\\]")


def _test_default_track_named_key_values_dict():
    metrics.track(
        amount=123,
        bin=123456,
        currency="USD",
        psp="ADYEN",
        result="SUCCESS",
        type="AUTHORIZATION",
        key1="value1",
        key2="value2",
    )


def _test_default_track_named_key_values_kwargs():
    metrics.track(
        amount=123,
        bin=123456,
        currency="USD",
        psp="ADYEN",
        result="SUCCESS",
        type="AUTHORIZATION",
        **{"key3": "value1", "key4": "value2"}
    )


def _test_default_track_keys_unordered():
    metrics.track(
        bin=123456,
        amount=123,
        psp="ADYEN",
        key1="value1",
        currency="USD",
        type="AUTHORIZATION",
        result="SUCCESS",
        key2="value2",
    )


def _test_default_track_amt_dict():
    asserts.assert_fails(
        lambda: metrics.track({"foo": "bar"}),
        ".*parameter 'amount' got value of type 'dict', want 'int, string, or NoneType'.*")


def _test_default_track_float_amount():
    asserts.assert_fails(
        lambda: metrics.track(amount=11.21),
        ".*parameter 'amount' got value of type 'float', want 'int, string, or NoneType'.*")


def _test_default_track_amount_starlark_int():
    # much larger than java.util.Integer
    asserts.assert_fails(
        lambda: metrics.track(amount="1234567890123"),
        "parameter 'amount' invalid value, want 'int'")


def _test_default_track_float_amount_string():
    asserts.assert_fails(
        lambda: metrics.track(amount="11.21"),
        "parameter 'amount' invalid value, want 'int'")


def _test_default_track_float_amount_string_negative():
    asserts.assert_fails(
        lambda: metrics.track(amount="-11.21"),
        "parameter 'amount' invalid value, want 'int'")


def _test_default_track_amount_negative():
    asserts.assert_fails(
        lambda: metrics.track(amount=-11),
        "negative 'amount' not allowed")


def _test_default_track_amount_string_negative():
    asserts.assert_fails(
        lambda: metrics.track(amount="-11"),
        "negative 'amount' not allowed")


def _test_default_track_amount_bin_string():
    metrics.track(bin="123456", amount="123")


def _test_default_track_bin_negative():
    asserts.assert_fails(
        lambda: metrics.track(bin=-11),
        "negative 'bin' not allowed")


def _test_default_track_bin_string_negative():
    asserts.assert_fails(
        lambda: metrics.track(bin="-11"),
        "negative 'bin' not allowed")


def _test_default_track_float_bin():
    asserts.assert_fails(
        lambda: metrics.track(bin=11.21),
        ".*parameter 'bin' got value of type 'float', want 'int, string, or NoneType'.*")


def _test_default_track_float_bin_negative():
    asserts.assert_fails(
        lambda: metrics.track(bin=-11.21),
        ".*parameter 'bin' got value of type 'float', want 'int, string, or NoneType'.*")


def _test_default_track_float_bin_string_negative():
    asserts.assert_fails(
        lambda: metrics.track(bin="-11.21"),
        "parameter 'bin' invalid value, want 'int'")


def _test_default_track_kwargs_object():
    metrics.track(foo={"1": 2}, color={2: 1})


def _test_default_track_kwargs_dict_non_string():
    asserts.assert_fails(
        lambda: metrics.track(**{1: 2}),
        "keywords must be strings, not int")


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_no_args))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_None_args))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_blank_amount_bin))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_blank_enums))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_kwargs_keys))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_kwargs_dict))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_kwargs_dict_mapped_keys))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_multiple_same_key))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_multiple_same_key_invalid_value_in_dict))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_multiple_same_key_invalid_value))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_without_keys))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_amount_bin_alpha_numeric))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_extra_values_without_keys))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_extra_values_with_keys_dict))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_named_key_values))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_bin_out_of_range))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_named_key_values_dict))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_named_key_values_kwargs))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_keys_unordered))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_amt_dict))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_float_amount))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_amount_starlark_int))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_float_amount_string))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_float_amount_string_negative))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_amount_negative))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_amount_string_negative))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_amount_bin_string))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_bin_negative))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_bin_string_negative))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_float_bin))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_float_bin_negative))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_float_bin_string_negative))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_kwargs_object))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_kwargs_dict_non_string))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
