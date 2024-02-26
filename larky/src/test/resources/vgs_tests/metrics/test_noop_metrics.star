"""Unit tests for MetricsModule.java using NoopMetrics API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//metrics", "metrics")


def _test_noop_track():
    asserts.assert_fails(
        lambda: metrics.track({"psp":"stripe"}),
        "metrics.track operation must be overridden")


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_noop_track))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
