"""Unit testing for operator.star.

copy most tests from: https://github.com/python/cpython/blob/main/Lib/test/test_operator.py
"""
load("@stdlib//larky", "larky")
load("@stdlib//operator", module="operator")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")


def Seq1(lst):
    self = larky.mutablestruct(__class__='Seq1')
    def __init__(lst):
        self.lst = lst
        return self
    self = __init__(lst)
    def __len__():
        return len(self.lst)
    self.__len__ = __len__
    def __getitem__(i):
        return self.lst[i]
    self.__getitem__ = __getitem__
    def __add__(other):
        return self.lst + other.lst
    self.__add__ = __add__
    def __mul__(other):
        return self.lst * other
    self.__mul__ = __mul__
    def __rmul__(other):
        return other * self.lst
    self.__rmul__ = __rmul__
    return self


def Seq2(lst):
    self = larky.mutablestruct(__class__='Seq2')
    def __init__(lst):
        self.lst = lst
        return self
    self = __init__(lst)
    def __len__():
        return len(self.lst)
    self.__len__ = __len__
    def __getitem__(i):
        return self.lst[i]
    self.__getitem__ = __getitem__
    def __add__(other):
        return self.lst + other.lst
    self.__add__ = __add__
    def __mul__(other):
        return self.lst * other
    self.__mul__ = __mul__
    def __rmul__(other):
        return other * self.lst
    self.__rmul__ = __rmul__
    return self


def BadIterable():
    self = larky.mutablestruct(__class__='BadIterable')
    def __iter__():
        fail(" ZeroDivisionError")
    self.__iter__ = __iter__
    return self


def OperatorTestCase_test_lt():
    operator = module
    asserts.assert_fails(lambda : operator.lt(), "missing 2 required positional arguments: a, b")
    asserts.assert_that(operator.lt(1, 0)).is_false()
    asserts.assert_that(operator.lt(1, 0.0)).is_false()
    asserts.assert_that(operator.lt(1, 1)).is_false()
    asserts.assert_that(operator.lt(1, 1.0)).is_false()
    asserts.assert_that(operator.lt(1, 2)).is_true()
    asserts.assert_that(operator.lt(1, 2.0)).is_true()

def OperatorTestCase_test_le():
    operator = module
    asserts.assert_fails(lambda : operator.le(), "missing 2 required positional arguments: a, b")
    asserts.assert_that(operator.le(1, 0)).is_false()
    asserts.assert_that(operator.le(1, 0.0)).is_false()
    asserts.assert_that(operator.le(1, 1)).is_true()
    asserts.assert_that(operator.le(1, 1.0)).is_true()
    asserts.assert_that(operator.le(1, 2)).is_true()
    asserts.assert_that(operator.le(1, 2.0)).is_true()

def OperatorTestCase_test_eq():
    operator = module
    def C():
        self = larky.mutablestruct(__class__='C')
        def __eq__(other):
            fail(" SyntaxError")
        self.__eq__ = __eq__
        return self
    asserts.assert_fails(lambda : operator.eq(), "missing 2 required positional arguments: a, b")
    asserts.assert_fails(lambda : operator.eq(C(), C()), ".*?SyntaxError")
    asserts.assert_that(operator.eq(1, 0)).is_false()
    asserts.assert_that(operator.eq(1, 0.0)).is_false()
    asserts.assert_that(operator.eq(1, 1)).is_true()
    asserts.assert_that(operator.eq(1, 1.0)).is_true()
    asserts.assert_that(operator.eq(1, 2)).is_false()
    asserts.assert_that(operator.eq(1, 2.0)).is_false()


def OperatorTestCase_test_general():
    def A():
        self = larky.mutablestruct(__class__='A')
        def method(*args, **kwargs):
            return (args, kwargs)
        self.method = method
        return self

    a = A()

    a.name = "foo"
    f = module.attrgetter('name')
    asserts.assert_(f(a) == "foo")

    fm = module.methodcaller("method")
    asserts.assert_(fm(a) == ((), {}))

    fm = module.methodcaller("method", 1, 2, 3, kw1="foo", kw2=10)
    asserts.assert_(fm(a) == ((1, 2, 3), {"kw1": "foo", "kw2": 10}))

    im = module.itemgetter(1)
    asserts.assert_(im([1, 2, 3]) == 2)

    im = module.itemgetter(0, 2)
    asserts.assert_(im([1, 2, 3]) == (1, 3))


def OperatorTestCase_test_ne():
    operator = module
    def C():
        self = larky.mutablestruct(__class__='C')
        def __ne__(other):
            fail(" SyntaxError")
        self.__ne__ = __ne__
        return self
    asserts.assert_fails(lambda : operator.ne(), "missing 2 required positional arguments: a, b")
    asserts.assert_fails(lambda : operator.ne(C(), C()), ".*?SyntaxError")
    asserts.assert_that(operator.ne(1, 0)).is_true()
    asserts.assert_that(operator.ne(1, 0.0)).is_true()
    asserts.assert_that(operator.ne(1, 1)).is_false()
    asserts.assert_that(operator.ne(1, 1.0)).is_false()
    asserts.assert_that(operator.ne(1, 2)).is_true()
    asserts.assert_that(operator.ne(1, 2.0)).is_true()


def OperatorTestCase_test_ge():
    operator = module
    asserts.assert_fails(lambda : operator.ge(), "missing 2 required positional arguments: a, b")
    asserts.assert_that(operator.ge(1, 0)).is_true()
    asserts.assert_that(operator.ge(1, 0.0)).is_true()
    asserts.assert_that(operator.ge(1, 1)).is_true()
    asserts.assert_that(operator.ge(1, 1.0)).is_true()
    asserts.assert_that(operator.ge(1, 2)).is_false()
    asserts.assert_that(operator.ge(1, 2.0)).is_false()


def OperatorTestCase_test_gt():
    operator = module
    asserts.assert_fails(lambda : operator.gt(), "missing 2 required positional arguments: a, b")
    asserts.assert_that(operator.gt(1, 0)).is_true()
    asserts.assert_that(operator.gt(1, 0.0)).is_true()
    asserts.assert_that(operator.gt(1, 1)).is_false()
    asserts.assert_that(operator.gt(1, 1.0)).is_false()
    asserts.assert_that(operator.gt(1, 2)).is_false()
    asserts.assert_that(operator.gt(1, 2.0)).is_false()


def OperatorTestCase_test_abs():
    operator = module
    asserts.assert_fails(lambda : operator.abs(), "missing 1 required positional argument: a")
    asserts.assert_fails(lambda : operator.abs(None), ".*?TypeError")
    asserts.assert_that(operator.abs(-1)).is_equal_to(1)
    asserts.assert_that(operator.abs(1)).is_equal_to(1)


def OperatorTestCase_test_add():
    operator = module
    asserts.assert_fails(lambda : operator.add(), "missing 2 required positional arguments: a, b")
    asserts.assert_fails(lambda : operator.add(None, None), "unsupported binary operation")
    asserts.assert_that(operator.add(3, 4)).is_equal_to(7)


def OperatorTestCase_test_bitwise_and():
    operator = module
    asserts.assert_fails(lambda : operator.and_(), "missing 2 required positional arguments: a, b")
    asserts.assert_fails(lambda : operator.and_(None, None), "unsupported binary operation")
    asserts.assert_that(operator.and_(0xf, 0xa)).is_equal_to(0xa)

def OperatorTestCase_test_concat():
    operator = module
    asserts.assert_fails(lambda : operator.concat(), "missing 2 required positional arguments: a, b")
    asserts.assert_fails(lambda : operator.concat(None, None), ".*?TypeError")
    asserts.assert_that(operator.concat('py', 'thon')).is_equal_to('python')
    asserts.assert_that(operator.concat([1, 2], [3, 4])).is_equal_to([1, 2, 3, 4])
    asserts.assert_that(operator.concat(Seq1([5, 6]), Seq1([7]))).is_equal_to([5, 6, 7])
    asserts.assert_that(operator.concat(Seq2([5, 6]), Seq2([7]))).is_equal_to([5, 6, 7])
    asserts.assert_fails(lambda : operator.concat(13, 29), ".*?TypeError")

def OperatorTestCase_test_countOf():
    operator = module
    asserts.assert_fails(lambda : operator.countOf(), "missing 2 required positional arguments: a, b")
    asserts.assert_fails(lambda : operator.countOf(None, None), "type '.*' is not iterable")
    asserts.assert_fails(lambda : operator.countOf(BadIterable(), 1), ".*?ZeroDivisionError")
    asserts.assert_that(operator.countOf([1, 2, 1, 3, 1, 4], 3)).is_equal_to(1)
    asserts.assert_that(operator.countOf([1, 2, 1, 3, 1, 4], 5)).is_equal_to(0)

def OperatorTestCase_test_delitem():
    operator = module
    a = [4, 3, 2, 1]
    asserts.assert_fails(lambda : operator.delitem(a), "missing 1 required positional argument")
    asserts.assert_fails(lambda : operator.delitem(a, None), ".*?TypeError")
    asserts.assert_that(operator.delitem(a, 1)).is_none()
    asserts.assert_that(a).is_equal_to([4, 2, 1])

def OperatorTestCase_test_floordiv():
    operator = module
    asserts.assert_fails(lambda : operator.floordiv(5), "missing 1 required positional argument")
    asserts.assert_fails(lambda : operator.floordiv(None, None), "unsupported binary operation")
    asserts.assert_that(operator.floordiv(5, 2)).is_equal_to(2)

def OperatorTestCase_test_truediv():
    operator = module
    asserts.assert_fails(lambda : operator.truediv(5), "missing 1 required positional argument")
    asserts.assert_fails(lambda : operator.truediv(None, None), "unsupported binary operation")
    asserts.assert_that(operator.truediv(5, 2)).is_equal_to(2.5)

def OperatorTestCase_test_getitem():
    operator = module
    a = range(10)
    asserts.assert_fails(lambda : operator.getitem(), "missing 2 required positional arguments")
    asserts.assert_fails(lambda : operator.getitem(a, None), "got \\w+ for sequence index, want int")
    asserts.assert_that(operator.getitem(a, 2)).is_equal_to(2)

def OperatorTestCase_test_indexOf():
    operator = module
    asserts.assert_fails(lambda : operator.indexOf(), "missing 2 required positional arguments")
    asserts.assert_fails(lambda : operator.indexOf(None, None), "type '\\w+' is not iterable")
    asserts.assert_fails(lambda : operator.indexOf(BadIterable(), 1), ".*?ZeroDivisionError")
    asserts.assert_that(operator.indexOf([4, 3, 2, 1], 3)).is_equal_to(1)
    asserts.assert_fails(lambda : operator.indexOf([4, 3, 2, 1], 0), ".*?ValueError")

def OperatorTestCase_test_invert():
    operator = module
    asserts.assert_fails(lambda : operator.invert(), "missing 1 required positional arguments")
    asserts.assert_fails(lambda : operator.invert(None), ".*?TypeError")
    asserts.assert_that(operator.inv(4)).is_equal_to(-5)

def OperatorTestCase_test_lshift():
    operator = module
    asserts.assert_fails(lambda : operator.lshift(), "missing 2 required positional arguments")
    asserts.assert_fails(lambda : operator.lshift(None, 42), ".*?TypeError")
    asserts.assert_that(operator.lshift(5, 1)).is_equal_to(10)
    asserts.assert_that(operator.lshift(5, 0)).is_equal_to(5)
    asserts.assert_fails(lambda : operator.lshift(2, -1), ".*?ValueError")

def OperatorTestCase_test_mod():
    operator = module
    asserts.assert_fails(lambda : operator.mod(), "missing 2 required positional arguments")
    asserts.assert_fails(lambda : operator.mod(None, 42), ".*?TypeError")
    asserts.assert_that(operator.mod(5, 2)).is_equal_to(1)

def OperatorTestCase_test_mul():
    operator = module
    asserts.assert_fails(lambda : operator.mul(), "missing 2 required positional arguments")
    asserts.assert_fails(lambda : operator.mul(None, None), ".*?TypeError")
    asserts.assert_that(operator.mul(5, 2)).is_equal_to(10)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_general))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_lt))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_le))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_eq))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_ne))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_ge))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_gt))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_abs))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_add))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_bitwise_and))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_concat))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_countOf))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_delitem))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_floordiv))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_truediv))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_getitem))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_indexOf))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_invert))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_lshift))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_mod))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_mul))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_matmul))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_neg))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_bitwise_or))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_pos))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_pow))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_rshift))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_contains))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_setitem))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_sub))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_truth))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_bitwise_xor))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_is))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_is_not))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_attrgetter))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_itemgetter))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_methodcaller))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_inplace))
    # _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_dunder_is_original))
    # _suite.addTest(unittest.FunctionTestCase(OperatorPickleTestCase_test_attrgetter))
    # _suite.addTest(unittest.FunctionTestCase(OperatorPickleTestCase_test_itemgetter))
    # _suite.addTest(unittest.FunctionTestCase(OperatorPickleTestCase_test_methodcaller))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())





