# Copyright 2018 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Unit tests for types.bzl."""

load("@stdlib//builtins", "builtins")
load("@stdlib//larky", "larky")
load("@stdlib//sets", "sets")
load("@stdlib//types", "types")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")


assert_true = asserts.assert_true
assert_false = asserts.assert_false
struct = larky.struct


def _a_function():
    """A dummy function for testing."""
    pass


def _is_string_test():
    """Unit tests for types.is_string."""

    assert_true(types.is_string(""))
    assert_true(types.is_string("string"))

    assert_false(types.is_string(4))
    assert_false(types.is_string([1]))
    assert_false(types.is_string({}))
    assert_false(types.is_string(()))
    assert_false(types.is_string(True))
    assert_false(types.is_string(None))
    assert_false(types.is_string(_a_function))


def _is_bool_test():
    """Unit tests for types.is_bool."""

    assert_true(types.is_bool(True))
    assert_true(types.is_bool(False))

    assert_false(types.is_bool(4))
    assert_false(types.is_bool([1]))
    assert_false(types.is_bool({}))
    assert_false(types.is_bool(()))
    assert_false(types.is_bool(""))
    assert_false(types.is_bool(None))
    assert_false(types.is_bool(_a_function))


def _is_list_test():
    """Unit tests for types.is_list."""

    assert_true(types.is_list([]))
    assert_true(types.is_list([1]))

    assert_false(types.is_list(4))
    assert_false(types.is_list("s"))
    assert_false(types.is_list({}))
    assert_false(types.is_list(()))
    assert_false(types.is_list(True))
    assert_false(types.is_list(None))
    assert_false(types.is_list(_a_function))


def _is_none_test():
    """Unit tests for types.is_none."""

    assert_true(types.is_none(None))

    assert_false(types.is_none(4))
    assert_false(types.is_none("s"))
    assert_false(types.is_none({}))
    assert_false(types.is_none(()))
    assert_false(types.is_none(True))
    assert_false(types.is_none([]))
    assert_false(types.is_none([1]))
    assert_false(types.is_none(_a_function))


def _is_int_test():
    """Unit tests for types.is_int."""

    assert_true(types.is_int(1))
    assert_true(types.is_int(-1))

    assert_false(types.is_int("s"))
    assert_false(types.is_int({}))
    assert_false(types.is_int(()))
    assert_false(types.is_int(True))
    assert_false(types.is_int([]))
    assert_false(types.is_int([1]))
    assert_false(types.is_int(None))
    assert_false(types.is_int(_a_function))


def _is_tuple_test():
    """Unit tests for types.is_tuple."""

    assert_true(types.is_tuple(()))
    assert_true(types.is_tuple((1,)))

    assert_false(types.is_tuple(1))
    assert_false(types.is_tuple("s"))
    assert_false(types.is_tuple({}))
    assert_false(types.is_tuple(True))
    assert_false(types.is_tuple([]))
    assert_false(types.is_tuple([1]))
    assert_false(types.is_tuple(None))
    assert_false(types.is_tuple(_a_function))


def _is_dict_test():
    """Unit tests for types.is_dict."""

    assert_true(types.is_dict({}))
    assert_true(types.is_dict({"key": "value"}))

    assert_false(types.is_dict(1))
    assert_false(types.is_dict("s"))
    assert_false(types.is_dict(()))
    assert_false(types.is_dict(True))
    assert_false(types.is_dict([]))
    assert_false(types.is_dict([1]))
    assert_false(types.is_dict(None))
    assert_false(types.is_dict(_a_function))


def _is_function_test():
    """Unit tests for types.is_function."""
    assert_true(types.is_function(_a_function))

    assert_false(types.is_function({}))
    assert_false(types.is_function(1))
    assert_false(types.is_function("s"))
    assert_false(types.is_function(()))
    assert_false(types.is_function(True))
    assert_false(types.is_function([]))
    assert_false(types.is_function([1]))
    assert_false(types.is_function(None))


def _is_set_test():
    """Unit test for types.is_set."""
    assert_true(types.is_set(sets.make()))
    assert_true(types.is_set(sets.make([1])))
    assert_false(types.is_set(None))
    assert_false(types.is_set({}))
    assert_false(types.is_set(struct(foo=1)))
    assert_false(types.is_set(struct(_values="not really values")))


def _is_bytes_test():
    """Unit test for types.is_bytes."""
    assert_true(type(builtins.bytes(r"", encoding="utf-8")) == 'bytes')
    assert_true(str(bytes) == '<built-in function bytes>')
    assert_true(
        types.is_instance(
            builtins.bytes(r"", encoding="utf-8"),
            bytes
        )
    )
    assert_true(types.is_bytes(builtins.bytes(r"", encoding="utf-8")))


def _is_bytearray_test():
    """Unit test for types.is_bytearray."""
    assert_true(type(builtins.bytearray(r"", encoding="utf-8")) == 'bytearray')
    assert_true(str(bytearray) == '<built-in function bytearray>')
    assert_true(
        types.is_instance(
            builtins.bytearray(r"", encoding="utf-8"),
            bytearray
        )
    )
    assert_true(types.is_bytearray(builtins.bytearray(r"", encoding="utf-8")))


def _is_iterable_test():
    assert_false(types.is_iterable("123"))
    assert_false(types.is_iterable(1,))
    assert_true(types.is_iterable("".elems()))
    assert_true(types.is_iterable([]))
    assert_true(types.is_iterable((1,)))


def _is_instance_test():
    assert_true(types.is_instance("", str))
    assert_true(types.is_instance(1, int))


## Test Objects


def test_():
    """
    In [39]: c = object()

    In [40]: c.foo = foo
    ---------------------------------------------------------------------------
    AttributeError                            Traceback (most recent call last)
    <ipython-input-40-256c5ba4e153> in <module>
    ----> 1 c.foo = foo

    AttributeError: 'object' object has no attribute 'foo'
    :return:
    """
    def b(one):
        print("invoke")

    s = larky.struct()
    s.foo = b
    # Error: ImmutableStruct value does not support field assignment
    s.foo(1)


def test_new_class_basics():
    C = types.new_class("C")
    asserts.assert_that(str(C)).is_equal_to("<type: C>")
    o = C()
    asserts.assert_that(str(o)).is_equal_to("<types.C object>")
    asserts.assert_that(type(o)).is_equal_to("LarkyObject")



def test_create_with_fields():
    # ns updates schema with cls_dict
    # stock.py
    # Example of making a class manually from parts
    # Methods
    def __init__(self, name, shares, price):
        self.name = name
        self.shares = shares
        self.price = price

    def cost(self):
        return self.shares * self.price

    cls_dict = {
        '__init__' : __init__,
        'cost' : cost,
    }

    def ns(x):
        print("here")
        x.update(cls_dict)

    Stock = types.new_class('Stock', (), {}, ns)
    # this completes making a normal class that acts as you would expect
    s = Stock(name='ACME', shares=50, price=91.1)
    print(dir(s))
    print(s)
    print(type(s))
    print(s.name)
    print(s.cost())


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_is_string_test))
    _suite.addTest(unittest.FunctionTestCase(_is_bool_test))
    _suite.addTest(unittest.FunctionTestCase(_is_list_test))
    _suite.addTest(unittest.FunctionTestCase(_is_none_test))
    _suite.addTest(unittest.FunctionTestCase(_is_int_test))
    _suite.addTest(unittest.FunctionTestCase(_is_tuple_test))
    _suite.addTest(unittest.FunctionTestCase(_is_dict_test))
    _suite.addTest(unittest.FunctionTestCase(_is_function_test))
    _suite.addTest(unittest.FunctionTestCase(_is_set_test))
    _suite.addTest(unittest.FunctionTestCase(_is_bytes_test))
    _suite.addTest(unittest.FunctionTestCase(_is_bytearray_test))
    _suite.addTest(unittest.FunctionTestCase(_is_iterable_test))
    _suite.addTest(unittest.FunctionTestCase(_is_instance_test))
    # TODO: uncomment when enabling basic OO
    # _suite.addTest(unittest.FunctionTestCase(test_new_class_basics))
    # _suite.addTest(unittest.FunctionTestCase(test_create_with_fields))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
