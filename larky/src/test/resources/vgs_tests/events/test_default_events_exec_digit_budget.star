"""Execution digit budget: two 8-digit events -> second dropped."""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//events", "events")


def _rec():
    return __vgs_recorded_events__


def _test_two_events_of_eight_digits_second_dropped():
    before_dropped = _rec().dropped()
    before_count = len(_rec().events())
    eight = {"a": "12345678"}
    events.record("ev_one", eight)
    events.record("ev_two", eight)
    asserts.assert_that(len(_rec().events())).is_equal_to(before_count + 1)
    asserts.assert_that(_rec().dropped()).is_equal_to(before_dropped + 1)


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_two_events_of_eight_digits_second_dropped))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
