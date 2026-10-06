"""Tests for the json module's handling of non-JSON types."""
load("@stdlib//json", "json")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")


def _test_rejects_bytes_and_sets():
    # CPython 3: json.dumps(b"a") raises
    # TypeError("Object of type bytes is not JSON serializable"), and likewise
    # for bytearray and set, at any depth.
    asserts.assert_fails(lambda: json.dumps(b"a"),
                         "^Object of type bytes is not JSON serializable$")
    asserts.assert_fails(lambda: json.dumps(bytearray(b"a")),
                         "^Object of type bytearray is not JSON serializable$")
    asserts.assert_fails(lambda: json.dumps(set([1])),
                         "^Object of type set is not JSON serializable$")
    asserts.assert_fails(lambda: json.dumps({"a": set([1])}),
                         "Object of type set is not JSON serializable$")
    asserts.assert_fails(lambda: json.dumps([b"a"]),
                         "Object of type bytes is not JSON serializable$")
    asserts.assert_fails(lambda: json.encode({"k": (1, [b"x"])}),
                         "Object of type bytes is not JSON serializable$")


def _test_encodes_json_types():
    # output format is unchanged
    asserts.assert_that(json.dumps({"a": [1, (2,)], "b": None, "c": True})).is_equal_to(
        '{"a":[1,[2]],"b":null,"c":true}')
    asserts.assert_that(json.dumps("café")).is_equal_to('"café"')
    asserts.assert_that(json.dumps(1.5)).is_equal_to("1.5")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_rejects_bytes_and_sets))
    _suite.addTest(unittest.FunctionTestCase(_test_encodes_json_types))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
