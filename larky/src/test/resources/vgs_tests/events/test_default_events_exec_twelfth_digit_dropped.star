"""Execution digit budget: 6 + 6 digits -> second event dropped."""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//events", "events")


def _rec():
    return __vgs_recorded_events__


def _test_twelfth_digit_across_events_dropped():
    before_dropped = _rec().dropped()
    before_count = len(_rec().events())
    six = {"a": "123456"}
    events.record("e1", six)
    events.record("e2", six)
    asserts.assert_that(len(_rec().events())).is_equal_to(before_count + 1)
    asserts.assert_that(_rec().dropped()).is_equal_to(before_dropped + 1)


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_twelfth_digit_across_events_dropped))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
