# dict(m) and d.update(m) accept any object with a keys() method, as in Python.
load("@stdlib//unittest", unittest="unittest")
load("@vendor//multidict", _multidict="multidict")
load("@vendor//asserts", asserts="asserts")


def test_dict_from_multidict():
    md = _multidict.CIMultiDict([("Content-Type", "a"), ("X-Y", "b")])
    asserts.assert_that(dict(md)).is_equal_to({"Content-Type": "a", "X-Y": "b"})
    asserts.assert_that(dict(md, extra="c")).is_equal_to(
        {"Content-Type": "a", "X-Y": "b", "extra": "c"})
    asserts.assert_that(dict(_multidict.CIMultiDict())).is_equal_to({})


def test_dict_from_multidict_repeated_keys():
    # keys() lists a repeated key once per value; __getitem__ returns the first value.
    md = _multidict.MultiDict([("k", "first"), ("j", "x"), ("k", "second")])
    asserts.assert_that(dict(md)).is_equal_to({"k": "first", "j": "x"})
    asserts.assert_that(dict(md)).is_equal_to({k: md[k] for k in md.keys()})


def test_update_from_multidict():
    md = _multidict.MultiDict([("k", "first"), ("k", "second"), ("j", "x")])
    d = {"k": "old", "z": "z"}
    d.update(md)
    asserts.assert_that(d).is_equal_to({"k": "first", "z": "z", "j": "x"})
    d.update(md, q="1")
    asserts.assert_that(d["q"]).is_equal_to("1")


def test_pairs_and_dicts_unchanged():
    asserts.assert_that(dict([("a", 1), ["b", 2]])).is_equal_to({"a": 1, "b": 2})
    asserts.assert_that(dict({"a": 1})).is_equal_to({"a": 1})
    d = {}
    d.update([("a", 1)])
    asserts.assert_that(d).is_equal_to({"a": 1})


def test_bad_input_still_errors():
    asserts.assert_fails(lambda: dict(["ab"]), ".*dictionary update sequence element #0 is not iterable \\(string\\)")
    asserts.assert_fails(lambda: dict(1), ".*in dict, got int, want iterable")
    asserts.assert_fails(lambda: dict([(1, 2, 3)]), ".*item #0 has length 3")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_dict_from_multidict))
    _suite.addTest(unittest.FunctionTestCase(test_dict_from_multidict_repeated_keys))
    _suite.addTest(unittest.FunctionTestCase(test_update_from_multidict))
    _suite.addTest(unittest.FunctionTestCase(test_pairs_and_dicts_unchanged))
    _suite.addTest(unittest.FunctionTestCase(test_bad_input_still_errors))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
