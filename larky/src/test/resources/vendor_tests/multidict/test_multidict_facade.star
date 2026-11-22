"""The @vendor//multidict facade over the native multidicts: what the upstream tests do not cover."""
load("@stdlib//larky", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//multidict", "CIMultiDict", "CIMultiDictProxy", "MultiDict", "MultiDictProxy", "getversion", "istr")


def test_instances_name_their_facade_class():
    d = CIMultiDict(a="1")
    asserts.assert_that(type(d)).is_equal_to("CIMultiDict")
    asserts.assert_that(d.__class__).is_equal_to(CIMultiDict)
    asserts.assert_that(types.is_instance(d, CIMultiDict)).is_true()
    asserts.assert_that(types.is_instance(d, MultiDict)).is_false()


def test_attributes_set_on_an_instance_override_its_methods():
    d = MultiDict([("a", "1")])
    d.__name__ = "Renamed"
    d.items = lambda: [("b", "2")]
    asserts.assert_that(type(d)).is_equal_to("Renamed")
    asserts.assert_that(d.items()).is_equal_to([("b", "2")])
    # copy() calls self.items(), so it copies what the override returns
    asserts.assert_that(d.copy().getall("b")).is_equal_to(["2"])


def test_version_counts_each_change():
    d = CIMultiDict([("a", "1"), ("b", "2")])
    asserts.assert_that(getversion(d)).is_equal_to(3)
    d["A"] = "3"
    d.add("c", "4")
    asserts.assert_that(getversion(d)).is_equal_to(5)
    d.get("a")
    asserts.assert_that(getversion(d)).is_equal_to(5)


def test_proxy_is_a_live_read_only_view():
    d = CIMultiDict([("Key", "1")])
    p = CIMultiDictProxy(d)
    d.add("key", "2")
    asserts.assert_that(p.getall("KEY")).is_equal_to(["1", "2"])
    asserts.assert_that(p["key"]).is_equal_to("1")
    asserts.assert_that("KEY" in p).is_true()
    asserts.assert_that(len(p)).is_equal_to(2)
    asserts.assert_that(getversion(p)).is_equal_to(getversion(d))
    asserts.assert_that(type(p.copy())).is_equal_to("CIMultiDict")
    asserts.assert_that(hasattr(p, "add")).is_false()

    def assign():
        p["k"] = "v"

    asserts.assert_fails(assign, ".*does not support item assignment")
    asserts.assert_fails(lambda: MultiDictProxy({"a": "1"}), ".*requires a multidict")


def test_multidicts_of_different_classes_compare_contents():
    asserts.assert_that(MultiDict(a="1") == CIMultiDict(a="1")).is_true()
    # a key the other lacks makes them unequal (the .star port raised KeyError here)
    asserts.assert_that(MultiDict(a="1") == CIMultiDict(b="1")).is_false()
    asserts.assert_that(CIMultiDict(a="1") == {"a": "1"}).is_false()
    asserts.assert_that(CIMultiDict(a="1").__eq__({"a": "1"})).is_true()
    asserts.assert_that(CIMultiDict(a="1") == None).is_false()


def test_in_falls_back_to_keys_when_the_key_has_no_title():
    d = CIMultiDict(a="1")
    asserts.assert_that(7 in d).is_false()


def test_getall_matches_an_istr_key_against_a_str_key():
    d = MultiDict([("k", "1")])
    asserts.assert_that(d.getall(istr("k"))).is_equal_to(["1"])
    asserts.assert_that(d.get(istr("k"))).is_equal_to(None)


def test_non_pair_items_are_a_type_error():
    asserts.assert_fails(lambda: MultiDict([("a", "b", "c")]), ".*TypeError: MultiDict expects \\(key, value\\) pairs")
    asserts.assert_fails(lambda: CIMultiDict().update([1]), ".*TypeError: update expects \\(key, value\\) pairs")


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(test_instances_name_their_facade_class))
    _suite.addTest(unittest.FunctionTestCase(test_attributes_set_on_an_instance_override_its_methods))
    _suite.addTest(unittest.FunctionTestCase(test_version_counts_each_change))
    _suite.addTest(unittest.FunctionTestCase(test_proxy_is_a_live_read_only_view))
    _suite.addTest(unittest.FunctionTestCase(test_multidicts_of_different_classes_compare_contents))
    _suite.addTest(unittest.FunctionTestCase(test_in_falls_back_to_keys_when_the_key_has_no_title))
    _suite.addTest(unittest.FunctionTestCase(test_getall_matches_an_istr_key_against_a_str_key))
    _suite.addTest(unittest.FunctionTestCase(test_non_pair_items_are_a_type_error))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
