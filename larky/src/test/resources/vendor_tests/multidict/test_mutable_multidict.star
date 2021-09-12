load("@stdlib//larky", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//string", string="string")
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//multidict", _multidict="multidict")
load("@vendor//asserts", asserts="asserts")

cls = _multidict.MultiDict
proxy_cls = _multidict.MultiDictProxy
istr = _multidict.istr


def TestMutableMultiDict_test_copy():
    d1 = cls(key="value", a="b")

    d2 = d1.copy()
    asserts.assert_that(d1).is_equal_to(d2)


def TestMutableMultiDict_test__repr__():
    d = cls()
    asserts.assert_that(str(d)).is_equal_to("<%s()>" % d.__name__)

    d = cls([("key", "one"), ("key", "two")])
    expected = "<%s('key': \"one\", 'key': \"two\")>" % d.__name__
    asserts.assert_that(str(d)).is_equal_to(expected)


def TestMutableMultiDict_test_getall():
    d = cls([("key", "value1")], key="value2")
    asserts.assert_that(len(d)).is_equal_to(2)

    asserts.assert_that(d.getall("key")).is_equal_to(["value1", "value2"])

    asserts.assert_fails(lambda: d.getall("some_key"), ".*?KeyError.*some_key")

    default = larky.SENTINEL
    asserts.assert_that(d.getall("some_key", default)).is_equal_to(default)


def TestMutableMultiDict_test_add():
    d = cls()
    # we differ from Python here because we do not have a starlark way to
    # check if mappings are equal.
    #
    # see the following issues:
    # - https://github.com/bazelbuild/starlark/issues/206
    # - https://github.com/bazelbuild/starlark/issues/198
    # - https://github.com/bazelbuild/bazel/issues/13605
    # TODO(mahmoudimus): Uncomment this when the above is fixed.
    # asserts.assert_that(d).is_equal_to({})

    # Commenting the below because Starlark does not support `SetIndexable`
    # even though it is part of the spec..
    # See:
    # - https://github.com/bazelbuild/starlark/issues/206
    # We end up using operator.setitem() instead to mimic dict["key"] = "value"
    # d["key"] = "one"
    operator.setitem(d, "key", "one")
    # See comment above re: equal mappings
    # asserts.assert_that(d).is_equal_to({"key": "one"})
    asserts.assert_that(d.getall("key")).is_equal_to(["one"])

    # d["key"] = "two"
    operator.setitem(d, "key", "two")
    # See comment above re: equal mappings
    # asserts.assert_that(d).is_equal_to({"key": "two"})
    asserts.assert_that(d.getall("key")).is_equal_to(["two"])

    d.add("key", "one")
    asserts.assert_that(2).is_equal_to(len(d))
    asserts.assert_that(d.getall("key")).is_equal_to(["two", "one"])

    d.add("foo", "bar")
    asserts.assert_that(3).is_equal_to(len(d))
    asserts.assert_that(d.getall("foo")).is_equal_to(["bar"])


def TestMutableMultiDict_test_extend():
    d = cls()
    # see comment in TestMutableMultiDict_test_add about mapping equality
    # asserts.assert_that(d).is_equal_to({})

    d.extend([("key", "one"), ("key", "two")], key=3, foo="bar")
    # asserts.assert_that(d).is_not_equal_to({"key": "one", "foo": "bar"})
    asserts.assert_that(d).is_not_equal_to(cls(**{"key": "one", "foo": "bar"}))
    asserts.assert_that(4).is_equal_to(len(d))
    itms = d.items()
    # we can't guarantee order of kwargs
    # TODO(mahmoudimus): actually, in Larky we can. confirm this.
    asserts.assert_that(itms).contains(("key", "one"))
    asserts.assert_that(itms).contains(("key", "two"))
    asserts.assert_that(itms).contains(("key", 3))
    asserts.assert_that(itms).contains(("foo", "bar"))

    other = cls(bar="baz")
    asserts.assert_that(other.getall("bar")).is_equal_to(["baz"])
    # see comment in TestMutableMultiDict_test_add about mapping equality
    # asserts.assert_that(other).is_equal_to({"bar": "baz"})

    d.extend(other)
    asserts.assert_that(d.items()).contains(("bar", "baz"))

    d.extend({"foo": "moo"})
    asserts.assert_that(d.items()).contains(("foo", "moo"))

    d.extend()
    asserts.assert_that(6).is_equal_to(len(d))

    asserts.assert_fails(
        lambda: d.extend("foo", "bar"),
        ".*TypeError.*extend takes at most 1 positional argument")


def TestMutableMultiDict_test_extend_from_proxy():
    d = cls([("a", "a"), ("b", "b")])
    proxy = proxy_cls(d)

    d2 = cls()
    d2.extend(proxy)

    asserts.assert_that([("a", "a"), ("b", "b")]).is_equal_to(list(d2.items()))


def TestMutableMultiDict_test_clear():
    d = cls([("key", "one")], key="two", foo="bar")

    d.clear()
    asserts.assert_that(len(d)).is_equal_to(0)
    asserts.assert_that(list(d.items())).is_equal_to([])


def TestMutableMultiDict_test_del():
    d = cls([("key", "one"), ("key", "two")], foo="bar")
    asserts.assert_that(list(d.keys())).is_equal_to(["key", "key", "foo"])
    operator.delitem(d, "key")
    asserts.assert_that(d).is_equal_to(cls(**{"foo": "bar"}))
    asserts.assert_that(list(d.items())).is_equal_to([("foo", "bar")])

    asserts.assert_that(lambda: operator.delitem(d, "key"), ".*KeyError.*key")


def TestMutableMultiDict_test_set_default():
    d = cls([("key", "one"), ("key", "two")], foo="bar")
    asserts.assert_that("one").is_equal_to(d.setdefault("key", "three"))
    asserts.assert_that("three").is_equal_to(d.setdefault("otherkey", "three"))
    asserts.assert_that(d).contains("otherkey")
    asserts.assert_that("three").is_equal_to(operator.getitem(d, "otherkey"))


def TestMutableMultiDict_test_popitem():
    d = cls()
    d.add("key", "val1")
    d.add("key", "val2")

    asserts.assert_that(("key", "val1")).is_equal_to(d.popitem())
    asserts.assert_that([("key", "val2")]).is_equal_to(list(d.items()))


def TestMutableMultiDict_test_popitem_empty_multidict():
    d = cls()
    asserts.assert_fails(d.popitem, ".*KeyError")


def TestMutableMultiDict_test_pop():
    d = cls()
    d.add("key", "val1")
    d.add("key", "val2")

    asserts.assert_that("val1").is_equal_to(d.pop("key"))
    asserts.assert_that(cls(**{"key": "val2"})).is_equal_to(d)


def TestMutableMultiDict_test_pop2():
    d = cls()
    d.add("key", "val1")
    d.add("key2", "val2")
    d.add("key", "val3")

    asserts.assert_that("val1").is_equal_to(d.pop("key"))
    asserts.assert_that([("key2", "val2"), ("key", "val3")]).is_equal_to(list(d.items()))


def TestMutableMultiDict_test_pop_default():
    d = cls(other="val")

    asserts.assert_that("default").is_equal_to(d.pop("key", "default"))
    asserts.assert_that(d).contains("other")


def TestMutableMultiDict_test_pop_raises():
    d = cls(other="val")
    asserts.assert_fails(lambda: d.pop("key"), ".*KeyError.*key")
    asserts.assert_that(d).contains("other")


def TestMutableMultiDict_test_replacement_order():
    d = cls()
    d.add("key1", "val1")
    d.add("key2", "val2")
    d.add("key1", "val3")
    d.add("key2", "val4")

    # see comment in TestMutableMultiDict_test_add about SetIndexable
    # d["key1"] = "val"
    operator.setitem(d, "key1", "val")
    expected = [("key1", "val"), ("key2", "val2"), ("key2", "val4")]
    asserts.assert_that(expected).is_equal_to(list(d.items()))


def TestMutableMultiDict_test_nonstr_key():
    d = cls()
    asserts.assert_fails(lambda: operator.setitem(d, 1, "val"), ".*TypeError")


def TestMutableMultiDict_test_istr_key():
    d = cls()
    # see comment in TestMutableMultiDict_test_add about SetIndexable
    # d[istr("1")] = "val"
    key = istr("1")
    operator.setitem(d, key, "val")
    asserts.assert_that(type(key)).is_equal_to("istr")
    asserts.assert_that(type(list(d.keys())[0])).is_equal_to(type(key))


def A_test_istr_key_add():
    d = cls()
    key = istr("1")
    d.add(key, "val")
    asserts.assert_that(type(list(d.keys())[0])).is_equal_to(type(key))


def A_test_str_derived_key_add():
    def A(v):
        self = istr(v)
        self.__class__ = A
        self.__name__ = 'A'
        return self

    d = cls()
    key = A("1")
    d.add(key, "val")
    asserts.assert_that(type(key)).is_equal_to("A")
    asserts.assert_that(type(list(d.keys())[0])).is_equal_to(type(key))


def A_test_popall():
    d = cls()
    d.add("key1", "val1")
    d.add("key2", "val2")
    d.add("key1", "val3")
    ret = d.popall("key1")
    asserts.assert_that(["val1", "val3"]).is_equal_to(ret)
    asserts.assert_that(cls(**{"key2": "val2"})).is_equal_to(d)


def A_test_popall_default():
    d = cls()
    asserts.assert_that("val").is_equal_to(d.popall("key", "val"))


def A_test_popall_key_error():
    d = cls()
    asserts.assert_that(lambda: d.popall("key"), ".*KeyError.*key")


def A_test_large_multidict_resizing():
    SIZE = 1024
    d = cls()
    for i in range(SIZE):
        operator.setitem(d, "key" + str(i), i)

    for i in range(SIZE - 1):
        operator.delitem(d, "key" + str(i))

    asserts.assert_that(cls(**{"key" + str(SIZE - 1): SIZE - 1})).is_equal_to(d)


ci_cls =  _multidict.CIMultiDict
proxy_ci_cls = _multidict.CIMultiDictProxy

def TestCIMutableMultiDict_test_getall():
    d = ci_cls([("KEY", "value1")], KEY="value2")

    asserts.assert_that(d).is_not_equal_to(ci_cls(**{"KEY": "value1"}))
    asserts.assert_that(len(d)).is_equal_to(2)

    asserts.assert_that(d.getall("key")).is_equal_to(["value1", "value2"])
    asserts.assert_fails(lambda: d.getall("some_key"), ".*KeyError.*some_key")


def TestCIMutableMultiDict_test_ctor():
    d = ci_cls(k1="v1")
    asserts.assert_that("v1").is_equal_to(operator.getitem(d, "K1"))
    asserts.assert_that(d.items()).contains(("k1", "v1"))


def TestCIMutableMultiDict_test_setitem():
    d = ci_cls()

    # see comment in TestMutableMultiDict_test_add about SetIndexable
    # d["k1"] = "v1"
    operator.setitem(d, "k1", "v1")
    asserts.assert_that("v1").is_equal_to(operator.getitem(d, "K1"))
    asserts.assert_that(d.items()).contains(("k1", "v1"))


def TestCIMutableMultiDict_test_delitem():
    d = ci_cls()
    # see comment in TestMutableMultiDict_test_add about SetIndexable
    # d["k1"] = "v1"
    operator.setitem(d, "k1", "v1")
    asserts.assert_that(d).contains("K1")
    operator.delitem(d, "k1")
    asserts.assert_that(d).does_not_contain("K1")


def TestCIMutableMultiDict_test_copy():
    d1 = ci_cls(key="KEY", a="b")

    d2 = d1.copy()
    asserts.assert_that(d1).is_equal_to(d2)
    asserts.assert_that(d1.items()).is_equal_to(d2.items())


def TestCIMutableMultiDict_test__repr__():
    d = ci_cls()
    asserts.assert_that(str(d)).is_equal_to("<%s()>" % d.__name__)

    d = ci_cls([("KEY", "one"), ("KEY", "two")])

    expected = "<%s('KEY': \"one\", 'KEY': \"two\")>" % d.__name__
    asserts.assert_that(str(d)).is_equal_to(expected)

def TestCIMutableMultiDict_test_add():
    d = ci_cls()

    asserts.assert_that(len(d)).is_equal_to(0)
    # see comment in TestMutableMultiDict_test_add about SetIndexable
    # d["KEY"] = "one"
    operator.setitem(d, "KEY", "one")
    asserts.assert_that(d.items()).contains(("KEY", "one"))
    asserts.assert_that(d).is_equal_to(ci_cls({"Key": "one"}))
    asserts.assert_that(d.getall("key")).is_equal_to(["one"])


    # see comment in TestMutableMultiDict_test_add about SetIndexable
    # d["KEY"] = "two"
    operator.setitem(d, "KEY", "two")
    asserts.assert_that(d.items()).contains(("KEY", "two"))
    asserts.assert_that(d).is_equal_to(ci_cls({"Key": "two"}))
    asserts.assert_that(d.getall("key")).is_equal_to(["two"])

    d.add("KEY", "one")
    asserts.assert_that(d.items()).contains(("KEY", "one"))
    asserts.assert_that(2).is_equal_to(len(d))
    asserts.assert_that(d.getall("key")).is_equal_to(["two", "one"])

    d.add("FOO", "bar")
    asserts.assert_that(d.items()).contains(("FOO", "bar"))
    asserts.assert_that(3).is_equal_to(len(d))
    asserts.assert_that(d.getall("foo")).is_equal_to(["bar"])

    d.add(key="test", value="test")
    asserts.assert_that(d.items()).contains(("test", "test"))
    asserts.assert_that(4).is_equal_to(len(d))
    asserts.assert_that(d.getall("test")).is_equal_to(["test"])


def TestCIMutableMultiDict_test_extend():
    d = ci_cls()
    asserts.assert_that(len(d)).is_equal_to(0)

    d.extend([("KEY", "one"), ("key", "two")], key=3, foo="bar")
    asserts.assert_that(4).is_equal_to(len(d))
    itms = d.items()
    # we can't guarantee order of kwargs
    asserts.assert_that(itms).contains(("KEY", "one"))
    asserts.assert_that(itms).contains(("key", "two"))
    asserts.assert_that(itms).contains(("key", 3))
    asserts.assert_that(itms).contains(("foo", "bar"))

    other = ci_cls(Bar="baz")
    asserts.assert_that(other).is_equal_to(ci_cls({"Bar": "baz"}))

    d.extend(other)
    asserts.assert_that(d.items()).contains(("Bar", "baz"))
    asserts.assert_that(d).contains("bar")

    d.extend({"Foo": "moo"})
    asserts.assert_that(d.items()).contains(("Foo", "moo"))
    asserts.assert_that(d).contains("foo")

    d.extend()
    asserts.assert_that(6).is_equal_to(len(d))

    asserts.assert_fails(lambda: d.extend("foo", "bar"), ".*TypeError")


def TestCIMutableMultiDict_test_extend_from_proxy():
    d = ci_cls([("a", "a"), ("b", "b")])
    proxy = proxy_ci_cls(d)

    d2 = ci_cls()
    d2.extend(proxy)

    asserts.assert_that([("a", "a"), ("b", "b")]).is_equal_to(list(d2.items()))


def TestCIMutableMultiDict_test_clear():
    d = ci_cls([("KEY", "one")], key="two", foo="bar")

    d.clear()
    asserts.assert_that(len(d)).is_equal_to(0)
    asserts.assert_that(list(d.items())).is_equal_to([])


def TestCIMutableMultiDict_test_del():
    d = ci_cls([("KEY", "one"), ("key", "two")], foo="bar")
    operator.delitem(d, "key")
    asserts.assert_that(d).is_equal_to(ci_cls(**{"foo": "bar"}))
    asserts.assert_that(list(d.items())).is_equal_to([("foo", "bar")])
    asserts.assert_fails(lambda: operator.delitem(d, "key"), ".*KeyError.*key")


def TestCIMutableMultiDict_test_set_default():
    d = ci_cls([("KEY", "one"), ("key", "two")], foo="bar")
    asserts.assert_that("one").is_equal_to(d.setdefault("key", "three"))
    asserts.assert_that("three").is_equal_to(d.setdefault("otherkey", "three"))
    asserts.assert_that(d).contains("otherkey")
    asserts.assert_that(d.items()).contains(("otherkey", "three"))
    asserts.assert_that("three").is_equal_to(operator.getitem(d, "OTHERKEY"))


def TestCIMutableMultiDict_test_popitem():
    d = ci_cls()
    d.add("KEY", "val1")
    d.add("key", "val2")

    pair = d.popitem()
    asserts.assert_that(("KEY", "val1")).is_equal_to(pair)
    asserts.assert_that(types.is_string(pair[0])).is_true()
    asserts.assert_that([("key", "val2")]).is_equal_to(list(d.items()))


def TestCIMutableMultiDict_test_popitem_empty_multidict():
    d = ci_cls()
    asserts.assert_fails(d.popitem, ".*KeyError")


def TestCIMutableMultiDict_test_pop():
    d = ci_cls()
    d.add("KEY", "val1")
    d.add("key", "val2")

    asserts.assert_that("val1").is_equal_to(d.pop("KEY"))
    asserts.assert_that(ci_cls(**{"key": "val2"})).is_equal_to(d)


def TestCIMutableMultiDict_test_pop_lowercase():
    d = ci_cls()
    d.add("KEY", "val1")
    d.add("key", "val2")

    asserts.assert_that("val1").is_equal_to(d.pop("key"))
    asserts.assert_that(ci_cls(**{"key": "val2"})).is_equal_to(d)


def TestCIMutableMultiDict_test_pop_default():
    d = ci_cls(OTHER="val")

    asserts.assert_that("default").is_equal_to(d.pop("key", "default"))
    asserts.assert_that(d).contains("other")


def TestCIMutableMultiDict_test_pop_raises():
    d = ci_cls(OTHER="val")

    asserts.assert_fails(lambda: d.pop("KEY"), ".*KeyError.*KEY")
    asserts.assert_that(d).contains("other")


def TestCIMutableMultiDict_test_extend_with_istr():
    # expecting this test to fail b/c istr() != str()
    us = istr("aBc")
    d = ci_cls()

    d.extend([(us, "val")])
    asserts.assert_that([("aBc", "val")]).is_equal_to(list(d.items()))

def TestCIMutableMultiDict_test_copy_istr():
    # expecting this test to fail b/c larky structs are not hashable
    d = ci_cls({istr("Foo"): "bar"})
    d2 = d.copy()
    asserts.assert_that(d).is_equal_to(d2)


def TestCIMutableMultiDict_test_eq():
    d1 = ci_cls(Key="val")
    d2 = ci_cls(KEY="val")

    asserts.assert_that(d1).is_equal_to(d2)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test_copy))
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test__repr__))
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test_getall))
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test_add))
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test_extend))
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test_extend_from_proxy))
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test_clear))
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test_del))
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test_set_default))
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test_popitem))
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test_popitem_empty_multidict))
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test_pop))
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test_pop2))
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test_pop_default))
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test_pop_raises))
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test_replacement_order))
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test_nonstr_key))
    _suite.addTest(unittest.FunctionTestCase(TestMutableMultiDict_test_istr_key))
    _suite.addTest(unittest.FunctionTestCase(A_test_istr_key_add))
    _suite.addTest(unittest.FunctionTestCase(A_test_str_derived_key_add))
    _suite.addTest(unittest.FunctionTestCase(A_test_popall))
    _suite.addTest(unittest.FunctionTestCase(A_test_popall_default))
    _suite.addTest(unittest.FunctionTestCase(A_test_popall_key_error))
    _suite.addTest(unittest.FunctionTestCase(A_test_large_multidict_resizing))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_getall))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_ctor))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_setitem))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_delitem))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_copy))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test__repr__))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_add))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_extend))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_extend_from_proxy))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_clear))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_del))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_set_default))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_popitem))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_popitem_empty_multidict))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_pop))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_pop_lowercase))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_pop_default))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_pop_raises))
    _suite.addTest(unittest.expectedFailure(unittest.FunctionTestCase(TestCIMutableMultiDict_test_extend_with_istr)))
    _suite.addTest(unittest.expectedFailure(unittest.FunctionTestCase(TestCIMutableMultiDict_test_copy_istr)))
    _suite.addTest(unittest.FunctionTestCase(TestCIMutableMultiDict_test_eq))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())