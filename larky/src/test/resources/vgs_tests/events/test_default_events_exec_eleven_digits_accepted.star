"""Execution digit budget: 6 + 5 digits across two events -> both accepted."""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//events", "events")


def _rec():
    return __vgs_recorded_events__


def _test_exactly_eleven_digits_across_two_events_accepted():
    before_dropped = _rec().dropped()
    before_count = len(_rec().events())
    events.record("ev_one", {"a": "123456"})
    events.record("ev_two", {"a": "12345"})
    asserts.assert_that(len(_rec().events())).is_equal_to(before_count + 2)
    asserts.assert_that(_rec().dropped()).is_equal_to(before_dropped)


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_exactly_eleven_digits_across_two_events_accepted))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
