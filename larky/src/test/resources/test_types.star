"""Unit tests for larky.star."""
load("@stdlib/larky", "larky")

load("@stdlib/asserts",  "asserts")
load("@stdlib/unittest", "unittest")
load("@stdlib/types", "types")


def b(one):
    print("invoke")


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
    o.foo()


def _suite():
    _suite = unittest.TestSuite()
    for t in [
        # test_,
        test_new_class_basics,
    ]:
        _suite.addTest(unittest.FunctionTestCase(t))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())