"""Unit tests for MetricsModule.java using DefaultMetrics API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//metrics", "metrics")


def _test_default_track_no_args():
    metrics.track()


def _test_default_track_None_args():
    metrics.track(None, None, None, None, None, None)


def _test_default_track_kwargs_keys():
    metrics.track(foo="bar", color="red")


def _test_default_track_kwargs_dict():
    metrics.track(**{"foo": "bar"})


def _test_default_track_without_keys():
    # will be mapped to amount and bin
    metrics.track("bar", "red")


def _test_default_track_extra_values_without_keys():
    asserts.assert_fails(
        lambda: metrics.track("a", "b", "c", "p", "r", "t", "d", "e"),
        ".*accepts no more than 6 positional arguments but got 8.*")


def _test_default_track_extra_values_with_keys_dict():
    metrics.track("a", "b", "c", "p", "r", "t", d="d", e="e")


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


# TODO: verify can amount be float?
def _test_default_track_invalid_amount():
    asserts.assert_fails(
        lambda: metrics.track(amount=11.21, bin=-1),
        ".*parameter 'amount' got value of type 'float', want 'int, string, or NoneType'.*")


def _test_default_track_amount_bin_string():
    metrics.track(bin="123456", amount="123")


# TODO: verify skip it or give error back for non numeric amount and bin?
def _test_default_track_amount_bin_alpha_numeric():
    metrics.track(bin="abcde", amount="23wed")


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_no_args))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_None_args))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_kwargs_keys))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_kwargs_dict))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_without_keys))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_extra_values_without_keys))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_extra_values_with_keys_dict))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_named_key_values))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_named_key_values_dict))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_keys_unordered))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_amt_dict))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_invalid_amount))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_amount_bin_string))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_amount_bin_alpha_numeric))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
