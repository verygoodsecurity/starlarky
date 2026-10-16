"""Per-event digit budget acceptance (isolated RecordedEvents per test file)."""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//events", "events")


def _rec():
    return __vgs_recorded_events__


def _test_eight_and_three_digits_across_fields_accepted():
    before_count = len(_rec().events())
    before_dropped = _rec().dropped()
    events.record("evt", {"a": "12345678", "b": "123"})
    asserts.assert_that(len(_rec().events())).is_equal_to(before_count + 1)
    asserts.assert_that(_rec().dropped()).is_equal_to(before_dropped)
    evt = _rec().events()[before_count]
    asserts.assert_that(evt["fields"]["a"]).is_equal_to("12345678")
    asserts.assert_that(evt["fields"]["b"]).is_equal_to("123")


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_eight_and_three_digits_across_fields_accepted))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
