"""Unit tests for MetricsModule.java using DefaultMetrics API"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//metrics", "metrics")


def _test_default_track():
    metrics.track({"psp":"stripe","amount":123})


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_default_track))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
