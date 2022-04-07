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

load("@stdlib//types", "make_class", "new", "get", "set_", "del_")



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


def test_class_dont_require_attributes():
    TestClass = make_class('TestClass')
    instance = TestClass()
    print(instance)
    asserts.assert_that(larky.is_instance(instance, TestClass)).is_true()


def test_class_instances_have_a_name():
    OtherTestClass = make_class('OtherTestClass')
    asserts.assert_that(OtherTestClass.__name__).is_equal_to('OtherTestClass')


def test_init_is_called_upon_new():
    val = [False]

    def initializer(*args, **kwargs):
        val.clear()
        val.append(True)

    TestClass = make_class('TestClass', cls_dict={
        '__init__': initializer
    })

    instance = TestClass()
    asserts.assert_that(val[0]).is_true()
    asserts.assert_that(larky.is_instance(instance, TestClass)).is_true()
    print(type(TestClass))
    print(type(instance))
    # print(type.int2("1"))
    # print(type("1") == "string")
    # print(type(type))


def test_init_is_passed_arguments():
    _self = []
    _args = []
    _kwargs = {}

    def initializer(self, *args, **kwargs):
        _self.append(self)
        _args.extend(args)
        _kwargs.update(kwargs)

    TestClass = make_class('TestClass', cls_dict={
        '__init__': initializer
    })

    instance = TestClass(1, 2, a='b')
    asserts.assert_that(larky.is_instance(instance, TestClass)).is_true()
    asserts.assert_that(_self[0]).is_equal_to(instance)
    asserts.assert_that(_args).is_equal_to([1, 2])
    asserts.assert_that(_kwargs).is_equal_to({'a': 'b'})


def test_instances_can_set_and_get_attributes():
    TestClass = make_class('TestClass', cls_dict={
        '__init__': lambda self: set_(self, 'val', 1)
    })

    instance = TestClass()

    asserts.assert_that(get(instance, 'val')).is_equal_to(1)
    asserts.assert_that(instance.val).is_equal_to(1)


def test_attributes_can_exist_on_the_class():
    TestClass = make_class('TestClass', cls_dict={
        'x': 1
    })

    instance = TestClass()
    asserts.assert_that(get(instance, 'x')).is_equal_to(1)
    asserts.assert_that(instance.x).is_equal_to(1)


def test_setting_an_attribute_shadows_the_class_attribute():
    TestClass = make_class('TestClass', cls_dict={
        'x': 1
    })

    instance = TestClass()
    set_(instance, 'x', 2)
    asserts.assert_that(get(instance, 'x')).is_equal_to(2)
    instance.x = 3
    asserts.assert_that(instance.x).is_equal_to(3)


def test_missing_attributes_raise_attribute_error():
    TestClass = make_class('TestClass')

    instance = TestClass()

    asserts.assert_fails(lambda: get(instance, 'missing_value'), ".*AttributeError")


def test_methods_receive_the_instance_as_the_first_param():
    _args = []

    def method(self, arg1, arg2):
        _args.extend([self, arg1, arg2])

    TestClass = make_class('TestClass', cls_dict={
        'method': method
    })

    instance = new(TestClass,)
    get(instance, 'method')(1, 2)

    asserts.assert_that(len(_args)).is_equal_to(3)
    asserts.assert_that(_args).is_equal_to([instance, 1, 2])


# def test_setting_an_instance_callable_does_not_receive_the_instance():
#     args = []
#
#     def method(arg1, arg2):
#         args.extend([arg1, arg2])
#
#     TestClass = make_class('TestClass')
#
#     instance = TestClass()
#     set_(instance, 'method', method)
#     get(instance, 'method')(1, 2)
#
#     asserts.assert_that(args).is_equal_to([1, 2])


# @raises(AttributeError)
# def test_can_delete_instance_attributes():
#     TestClass = make_class('TestClass')
#
#     instance = new(TestClass)
#     set_(instance, 'x', 1)
#     get(instance, 'x')
#
#     del_(instance, 'x')
#     get(instance, 'x')
#
#
# @raises(AttributeError)
# def test_cannot_delete_class_attributes():
#     TestClass = make_class('TestClass', cls_dict={
#         'x': 1
#     })
#
#     instance = new(TestClass)
#
#     del_(instance, 'x')
#
#
# def test_static_methods_do_not_get_instance_as_first_param():
#     method = Mock()
#
#     TestClass = make_class('TestClass', cls_dict={
#         'method': staticmethod(method)
#     })
#
#     instance = new(TestClass)
#
#     get(instance, 'method')(1)
#
#     method.assert_called_once_with(1)
#
#
# def test_class_methods_get_class_as_first_param():
#     method = Mock()
#
#     TestClass = make_class('TestClass', cls_dict={
#         'method': classmethod(method)
#     })
#
#     instance = new(TestClass)
#
#     get(instance, 'method')(1)
#
#     method.assert_called_once_with(TestClass, 1)
#
#
# def test_attribute_lookups_will_use_inheritance():
#     BaseClass = make_class('BaseClass', cls_dict={
#         'x': 1
#     })
#     TestClass = make_class('TestClass', bases=(BaseClass,))
#
#     instance = new(TestClass)
#
#     eq_(get(instance, 'x'), 1)


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
    # OBJECTS?
    _suite.addTest(unittest.FunctionTestCase(test_class_dont_require_attributes))
    _suite.addTest(unittest.FunctionTestCase(test_class_instances_have_a_name))
    _suite.addTest(unittest.FunctionTestCase(test_init_is_called_upon_new))
    _suite.addTest(unittest.FunctionTestCase(test_init_is_passed_arguments))
    _suite.addTest(unittest.FunctionTestCase(test_instances_can_set_and_get_attributes))
    _suite.addTest(unittest.FunctionTestCase(test_attributes_can_exist_on_the_class))
    _suite.addTest(unittest.FunctionTestCase(test_setting_an_attribute_shadows_the_class_attribute))
    _suite.addTest(unittest.FunctionTestCase(test_missing_attributes_raise_attribute_error))
    _suite.addTest(unittest.FunctionTestCase(test_methods_receive_the_instance_as_the_first_param))
    # _suite.addTest(unittest.FunctionTestCase(test_setting_an_instance_callable_does_not_receive_the_instance))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
