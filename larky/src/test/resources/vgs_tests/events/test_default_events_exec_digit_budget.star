"""Execution-level digit budget (isolated RecordedEvents per test file)."""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//events", "events")


def _rec():
    return __vgs_recorded_events__


def _test_execution_digit_budget_third_event_dropped():
    before_dropped = _rec().dropped()
    nine = {"a": "12345678", "b": "1"}
    events.record("e1", nine)
    events.record("e2", nine)
    events.record("e3", nine)
    asserts.assert_that(len(_rec().events())).is_equal_to(2)
    asserts.assert_that(_rec().dropped()).is_equal_to(before_dropped + 1)


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_execution_digit_budget_third_event_dropped))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
