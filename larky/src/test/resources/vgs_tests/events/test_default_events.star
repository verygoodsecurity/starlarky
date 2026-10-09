"""Unit tests for @vgs//events and RecordedEvents."""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@vgs//events", "events")


def _rec():
    return __vgs_recorded_events__


def _test_valid_record_kept():
    before = len(_rec().events())
    events.record("payment_success", {"amount": "100", "currency": "usd"})
    evts = _rec().events()
    asserts.assert_that(len(evts)).is_equal_to(before + 1)
    evt = evts[before]
    asserts.assert_that(evt["name"]).is_equal_to("payment_success")
    asserts.assert_that(evt["fields"]["amount"]).is_equal_to("100")
    asserts.assert_that(evt["fields"]["currency"]).is_equal_to("usd")


def _test_invalid_name_dropped():
    before_dropped = _rec().dropped()
    before_count = len(_rec().events())
    events.record("BadName", None)
    asserts.assert_that(_rec().dropped()).is_equal_to(before_dropped + 1)
    asserts.assert_that(len(_rec().events())).is_equal_to(before_count)


def _test_name_too_long_dropped():
    before = _rec().dropped()
    events.record("a" * 65, None)
    asserts.assert_that(_rec().dropped()).is_equal_to(before + 1)


def _test_fields_not_dict_dropped():
    before = _rec().dropped()
    events.record("evt", "not_a_dict")
    asserts.assert_that(_rec().dropped()).is_equal_to(before + 1)


def _test_too_many_fields_dropped():
    before = _rec().dropped()
    fields = {}
    for i in range(9):
        fields["k{}".format(i)] = "v"
    events.record("evt", fields)
    asserts.assert_that(_rec().dropped()).is_equal_to(before + 1)


def _test_invalid_field_key_dropped():
    before = _rec().dropped()
    events.record("evt", {"Bad-Key": "x"})
    asserts.assert_that(_rec().dropped()).is_equal_to(before + 1)


def _test_field_value_too_long_dropped():
    before = _rec().dropped()
    events.record("evt", {"msg": "x" * 129})
    asserts.assert_that(_rec().dropped()).is_equal_to(before + 1)


def _test_non_printable_field_value_dropped():
    before = _rec().dropped()
    events.record("evt", {"msg": "hello\nworld"})
    asserts.assert_that(_rec().dropped()).is_equal_to(before + 1)


def _test_int_and_bool_field_values_kept():
    before = len(_rec().events())
    events.record("typed", {"count": 42, "ok": True})
    evts = _rec().events()
    evt = evts[before]
    asserts.assert_that(evt["fields"]["count"]).is_equal_to("42")
    asserts.assert_that(evt["fields"]["ok"]).is_equal_to("True")


def _test_card_number_spaced_dropped():
    before = _rec().dropped()
    events.record("evt", {"pan": "4111 1111 1111 1111"})
    asserts.assert_that(_rec().dropped()).is_equal_to(before + 1)


def _test_card_number_dashed_dropped():
    before = _rec().dropped()
    events.record("evt", {"pan": "4111-1111-1111-1111"})
    asserts.assert_that(_rec().dropped()).is_equal_to(before + 1)


def _test_card_number_plain_dropped():
    before = _rec().dropped()
    events.record("evt", {"pan": "4111111111111111"})
    asserts.assert_that(_rec().dropped()).is_equal_to(before + 1)


def _test_eleventh_event_dropped():
    start_count = len(_rec().events())
    remaining = 10 - start_count
    if remaining < 0:
        remaining = 0
    for i in range(remaining):
        events.record("fill_{}".format(i), None)
    asserts.assert_that(len(_rec().events())).is_equal_to(10)
    before_dropped = _rec().dropped()
    events.record("one_over", None)
    asserts.assert_that(len(_rec().events())).is_equal_to(10)
    asserts.assert_that(_rec().dropped()).is_equal_to(before_dropped + 1)


def _test_record_never_raises_bad_input():
    events.record(None, None)
    events.record("", None)
    events.record("ok", {1: 2})


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_valid_record_kept))
    _suite.addTest(unittest.FunctionTestCase(_test_invalid_name_dropped))
    _suite.addTest(unittest.FunctionTestCase(_test_name_too_long_dropped))
    _suite.addTest(unittest.FunctionTestCase(_test_fields_not_dict_dropped))
    _suite.addTest(unittest.FunctionTestCase(_test_too_many_fields_dropped))
    _suite.addTest(unittest.FunctionTestCase(_test_invalid_field_key_dropped))
    _suite.addTest(unittest.FunctionTestCase(_test_field_value_too_long_dropped))
    _suite.addTest(unittest.FunctionTestCase(_test_non_printable_field_value_dropped))
    _suite.addTest(unittest.FunctionTestCase(_test_int_and_bool_field_values_kept))
    _suite.addTest(unittest.FunctionTestCase(_test_card_number_spaced_dropped))
    _suite.addTest(unittest.FunctionTestCase(_test_card_number_dashed_dropped))
    _suite.addTest(unittest.FunctionTestCase(_test_card_number_plain_dropped))
    _suite.addTest(unittest.FunctionTestCase(_test_eleventh_event_dropped))
    _suite.addTest(unittest.FunctionTestCase(_test_record_never_raises_bad_input))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
