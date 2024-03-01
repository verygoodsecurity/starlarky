"""Unit tests for MetricsModule.java using DefaultMetrics API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//metrics", "metrics")


def _test_default_track_all_values():
    metrics.track(
        amount=123,
        bin=123456,
        currency="USD",
        psp="ADYEN",
        result="SUCCESS",
        type="AUTHORIZATION")


def _test_default_track_without_keys():
    metrics.track(
        123,
        123456,
        "USD",
        "ADYEN",
        "SUCCESS",
        "AUTHORIZATION")


def _test_default_track_missing_values():
    metrics.track()


def _test_default_track_interchanging_values():
    metrics.track(
        123,
        123456,
        "ADYEN",
        "SUCCESS",
        "AUTHORIZATION",
        currency="USD")


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_all_values))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_without_keys))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_missing_values))
    _suite.addTest(unittest.FunctionTestCase(_test_default_track_interchanging_values))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
