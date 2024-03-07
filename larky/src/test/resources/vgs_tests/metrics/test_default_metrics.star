"""Unit tests for MetricsModule.java using DefaultMetrics API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//metrics", "metrics")


def _test_default_track_no_args():
    metrics.track()


def _test_default_track_None_args():
    metrics.track(None, None, None, None, None, None)


def _test_default_track_blank_values():
    metrics.track("", "", "", "", "", "")


def _test_default_track_extra_params_with_kwargs_keys():
    metrics.track(a="", b="", c="", d="", e="", f="", g="")


def _test_default_track_kwargs_values():
    metrics.track(foo="bar", color="red")


def _test_default_track_kwargs_dict():
    metrics.track(**{"foo": "bar"})


def _test_default_track_kwargs_dict_mapped_keys():
    metrics.track(**{"amount": "123"})


def _test_default_track_without_keys():
    # will be mapped to amount and bin
    metrics.track("bar", "red")


def _test_default_track_amount_bin_with_keys():
    metrics.track(bin="abcde", amount="23wed")


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
    metrics.track({"foo": "bar"})


def _test_default_track_float_amount():
    metrics.track(amount=11.21, bin=11.21)


def _test_default_track_amount_starlark_int():
    # much larger than java.util.Integer
    metrics.track(amount="1234567890123")


def _test_default_track_amount_negative():
    metrics.track(amount=-11, bin=-11)


def _test_default_track_kwargs_object():
    metrics.track(amount={"1": 2}, bin={2: 1})


def _test_default_track_fail_extra_params_without_keys():
    asserts.assert_fails(
        lambda: metrics.track("", "", "", "", "", "", ""),
        ".*accepts no more than 6 positional arguments but got 7.*")


def _test_default_track_fail_kwargs_key_non_string():
    asserts.assert_fails(
        lambda: metrics.track(**{1: 2}),
        "keywords must be strings, not int")


def _test_default_track_fail_multiple_same_key():
    asserts.assert_fails(
        lambda: metrics.track(amount=444, **{"amount": "123"}),
        ".*got multiple values for parameter 'amount'.*")


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_no_args))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_None_args))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_blank_values))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_extra_params_with_kwargs_keys))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_kwargs_values))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_kwargs_dict))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_kwargs_dict_mapped_keys))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_without_keys))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_amount_bin_with_keys))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_extra_values_with_keys_dict))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_named_key_values))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_named_key_values_dict))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_named_key_values_kwargs))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_keys_unordered))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_float_amount))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_amount_starlark_int))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_amount_negative))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_kwargs_object))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_fail_extra_params_without_keys))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_fail_kwargs_key_non_string))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_fail_multiple_same_key))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
