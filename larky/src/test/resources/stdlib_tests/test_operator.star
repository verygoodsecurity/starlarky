"""Unit testing for operator.star.

copy most tests from: https://github.com/python/cpython/blob/main/Lib/test/test_operator.py
"""
load("@stdlib//larky", "larky")
load("@stdlib//operator", module="operator")
load("@stdlib//unittest", "unittest")
load("@vendor//asserts", "asserts")
load("@stdlib//builtins", map="map")


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
    asserts.assert_fails(lambda : operator.invert(), "missing 1 required positional argument")
    asserts.assert_fails(lambda : operator.invert(None), "unsupported unary operation\\: \\~")
    asserts.assert_that(operator.inv(4)).is_equal_to(-5)

def OperatorTestCase_test_lshift():
    operator = module
    asserts.assert_fails(lambda : operator.lshift(), "missing 2 required positional arguments")
    asserts.assert_fails(lambda : operator.lshift(None, 42), "unsupported binary operation: \\w+ << \\w+")
    asserts.assert_that(operator.lshift(5, 1)).is_equal_to(10)
    asserts.assert_that(operator.lshift(5, 0)).is_equal_to(5)
    asserts.assert_fails(lambda : operator.lshift(2, -1), "negative shift count: -1")

def OperatorTestCase_test_mod():
    operator = module
    asserts.assert_fails(lambda : operator.mod(), "missing 2 required positional arguments")
    asserts.assert_fails(lambda : operator.mod(None, 42), "unsupported binary operation: \\w+ % \\w+")
    asserts.assert_that(operator.mod(5, 2)).is_equal_to(1)

def OperatorTestCase_test_mul():
    operator = module
    asserts.assert_fails(lambda : operator.mul(), "missing 2 required positional arguments")
    asserts.assert_fails(lambda : operator.mul(None, None), "unsupported binary operation: \\w+ \\* \\w+")
    asserts.assert_that(operator.mul(5, 2)).is_equal_to(10)

def OperatorTestCase_test_matmul():
    operator = module
    asserts.assert_fails(lambda : operator.matmul(), ".*?TypeError")
    asserts.assert_fails(lambda : operator.matmul(42, 42), ".*?TypeError")
    def M():
        self = larky.mutablestruct(__class__='M')
        def __matmul__(other):
            return other - 1
        self.__matmul__ = __matmul__
        return self
    asserts.assert_that(operator.matmul(M(), 42)).is_equal_to(41)

def OperatorTestCase_test_neg():
    operator = module
    asserts.assert_fails(lambda : operator.neg(), "missing 1 required positional argument")
    asserts.assert_fails(lambda : operator.neg(None), "unsupported unary operation: -\\w+")
    asserts.assert_that(operator.neg(5)).is_equal_to(-5)
    asserts.assert_that(operator.neg(-5)).is_equal_to(5)
    asserts.assert_that(operator.neg(0)).is_equal_to(0)
    asserts.assert_that(operator.neg(-0)).is_equal_to(0)

def OperatorTestCase_test_bitwise_or():
    operator = module
    asserts.assert_fails(lambda : operator.or_(), "missing 2 required positional arguments")
    asserts.assert_fails(lambda : operator.or_(None, None), "unsupported binary operation: \\w+ \\| \\w+")
    asserts.assert_that(operator.or_(0xa, 0x5)).is_equal_to(0xf)

def OperatorTestCase_test_pos():
    operator = module
    asserts.assert_fails(lambda : operator.pos(), "missing 1 required positional argument")
    asserts.assert_fails(lambda : operator.pos(None), "unsupported unary operation: \\+\\w+")
    asserts.assert_that(operator.pos(5)).is_equal_to(5)
    asserts.assert_that(operator.pos(-5)).is_equal_to(-5)
    asserts.assert_that(operator.pos(0)).is_equal_to(0)
    asserts.assert_that(operator.pos(-0)).is_equal_to(0)

def OperatorTestCase_test_pow():
    operator = module
    asserts.assert_fails(lambda : operator.pow(), "missing 2 required positional arguments")
    asserts.assert_fails(lambda : operator.pow(None, None), ".*?got value of type '\\w+', want 'int'")
    asserts.assert_that(operator.pow(3,5)).is_equal_to(pow(3, 5))
    asserts.assert_fails(lambda : operator.pow(1), "missing 1 required positional argument")
    asserts.assert_fails(lambda : operator.pow(1, 2, 3), ".*?accepts no more than 2 positional arguments but got")

def OperatorTestCase_test_rshift():
    operator = module
    asserts.assert_fails(lambda : operator.rshift(), "missing 2 required positional arguments")
    asserts.assert_fails(lambda : operator.rshift(None, 42), ".*?unsupported binary operation: \\w+ >> \\w+")
    asserts.assert_that(operator.rshift(5, 1)).is_equal_to(2)
    asserts.assert_that(operator.rshift(5, 0)).is_equal_to(5)
    asserts.assert_fails(lambda : operator.rshift(2, -1), ".*?negative shift count")

def OperatorTestCase_test_contains():
    operator = module
    asserts.assert_fails(lambda : operator.contains(), "missing 2 required positional arguments")
    asserts.assert_fails(lambda : operator.contains(None, None), ".*?TypeError: type '\\w+' is not iterable")
    asserts.assert_fails(lambda : operator.contains(BadIterable(), 1), ".*?ZeroDivisionError")
    asserts.assert_that(operator.contains(range(4), 2)).is_true()
    asserts.assert_that(operator.contains(range(4), 5)).is_false()

def OperatorTestCase_test_setitem():
    operator = module
    a = list(range(3))
    asserts.assert_fails(lambda : operator.setitem(a), "missing 2 required positional arguments")
    asserts.assert_fails(lambda : operator.setitem(a, None, None), ".*?got \\w+ for list index, want int")
    asserts.assert_that(operator.setitem(a, 0, 2)).is_none()
    asserts.assert_that(a).is_equal_to([2, 1, 2])
    asserts.assert_fails(lambda : operator.setitem(a, 4, 2), ".*?index out of range")

def OperatorTestCase_test_sub():
    operator = module
    asserts.assert_fails(lambda : operator.sub(), "missing 2 required positional arguments")
    asserts.assert_fails(lambda : operator.sub(None, None), ".*?unsupported binary operation: \\w+ \\- \\w+")
    asserts.assert_that(operator.sub(5, 2)).is_equal_to(3)

def OperatorTestCase_test_truth():
    operator = module
    def C():
        self = larky.mutablestruct(__class__='C')
        def __bool__():
            fail(" SyntaxError")
        self.__bool__ = __bool__
        return self
    asserts.assert_fails(lambda : operator.truth(), "missing 1 required positional argument")
    asserts.assert_fails(lambda : operator.truth(C()), ".*?SyntaxError")
    asserts.assert_that(operator.truth(5)).is_true()
    asserts.assert_that(operator.truth([0])).is_true()
    asserts.assert_that(operator.truth(0)).is_false()
    asserts.assert_that(operator.truth([])).is_false()

def OperatorTestCase_test_bitwise_xor():
    operator = module
    asserts.assert_fails(lambda : operator.xor(), "missing 2 required positional arguments")
    asserts.assert_fails(lambda : operator.xor(None, None), ".*?unsupported binary operation: \\w+ \\^ \\w+")
    asserts.assert_that(operator.xor(0xb, 0xc)).is_equal_to(0x7)

def OperatorTestCase_test_is():
    operator = module
    a = 'xyzpdq'
    b = a
    c = a[:3] + b[3:]
    asserts.assert_fails(lambda : operator.is_(), "missing 2 required positional arguments")
    asserts.assert_that(operator.is_(a, b)).is_true()
    # in starlark, there is no is operator, so this test will never be false!
    # asserts.assert_that(operator.is_(a,c)).is_false()

def OperatorTestCase_test_is_not():
    operator = module
    a = 'xyzpdq'
    b = a
    c = a[:3] + b[3:]
    asserts.assert_fails(lambda : operator.is_not(), "missing 2 required positional arguments")
    asserts.assert_that(operator.is_not(a, b)).is_false()
    # in starlark, there is no is operator, so this test will never be true!
    # asserts.assert_that(operator.is_not(a,c)).is_true()

def OperatorTestCase_test_attrgetter():
    operator = module
    def A():
        self = larky.mutablestruct(__class__='A')
        return self
    a = A()
    a.name = 'arthur'
    f = operator.attrgetter('name')
    asserts.assert_that(f(a)).is_equal_to('arthur')
    asserts.assert_fails(lambda : f(), "missing 1 required positional argument")
    asserts.assert_fails(lambda : f(a, 'dent'), ".*?accepts no more than 1 positional argument")
    asserts.assert_fails(lambda : f(a, surname='dent'), ".*?got unexpected keyword argument")
    f = operator.attrgetter('rank')
    asserts.assert_fails(lambda : f(a), ".*?value has no field or method")
    asserts.assert_fails(lambda : operator.attrgetter(2), ".*?TypeError")
    asserts.assert_fails(lambda : operator.attrgetter(), ".*?TypeError: attrgetter expected 1 argument")

    # multiple gets
    record = A()
    record.x = 'X'
    record.y = 'Y'
    record.z = 'Z'
    asserts.assert_that(operator.attrgetter('x','z','y')(record)).is_equal_to(('X', 'Z', 'Y'))
    asserts.assert_fails(lambda : operator.attrgetter(('x', (), 'y')), ".*?TypeError")
    def C():
        self = larky.mutablestruct(__class__='C')
        def __getattr__(name):
            fail(" SyntaxError")
        self.__getattr__ = __getattr__
        return self
    asserts.assert_fails(lambda : operator.attrgetter('foo')(C()), ".*?SyntaxError")

    # recursive gets
    a = A()
    a.name = 'arthur'
    a.child = A()
    a.child.name = 'thomas'
    asserts.assert_fails(lambda : operator.attrgetter('child.name'), ".*not supported")
    # f = operator.attrgetter('child.name')
    # asserts.assert_that(f(a)).is_equal_to('thomas')
    # asserts.assert_fails(lambda : f(a.child), ".*?AttributeError")
    # f = operator.attrgetter('name', 'child.name')
    # asserts.assert_that(f(a)).is_equal_to(('arthur', 'thomas'))
    # f = operator.attrgetter('name', 'child.name', 'child.child.name')
    # asserts.assert_fails(lambda : f(a), ".*?AttributeError")
    # f = operator.attrgetter('child.')
    # asserts.assert_fails(lambda : f(a), ".*?AttributeError")
    # f = operator.attrgetter('.child')
    # asserts.assert_fails(lambda : f(a), ".*?AttributeError")
    #
    # a.child.child = A()
    # a.child.child.name = 'johnson'
    # f = operator.attrgetter('child.child.name')
    # asserts.assert_that(f(a)).is_equal_to('johnson')
    # f = operator.attrgetter('name', 'child.name', 'child.child.name')
    # asserts.assert_that(f(a)).is_equal_to(('arthur', 'thomas', 'johnson'))

def OperatorTestCase_test_itemgetter():
    operator = module
    a = 'ABCDE'
    f = operator.itemgetter(2)
    asserts.assert_that(f(a)).is_equal_to('C')
    asserts.assert_fails(lambda : f(), "missing 1 required positional argument")
    asserts.assert_fails(lambda : f(a, 3), ".*?accepts no more than 1 positional argument but got")
    asserts.assert_fails(lambda : f(a, size=3), ".*?got unexpected keyword argument")
    f = operator.itemgetter(10)
    asserts.assert_fails(lambda : f(a), ".*?index out of range")
    def C():
        self = larky.mutablestruct(__class__='C')
        def __getitem__(name):
            fail(" SyntaxError")
        self.__getitem__ = __getitem__
        return self
    asserts.assert_fails(lambda : operator.itemgetter(42)(C()), ".*?SyntaxError")

    f = operator.itemgetter('name')
    asserts.assert_fails(lambda : f(a), ".*?got string for string index, want int")
    asserts.assert_fails(lambda : operator.itemgetter(), ".*?TypeError")

    d = dict(key='val')
    f = operator.itemgetter('key')
    asserts.assert_that(f(d)).is_equal_to('val')
    f = operator.itemgetter('nonkey')
    asserts.assert_fails(lambda : f(d), '.*?key "nonkey" not found in dictionary')

    # example used in the docs
    inventory = [('apple', 3), ('banana', 2), ('pear', 5), ('orange', 1)]
    getcount = operator.itemgetter(1)
    asserts.assert_that(list(map(getcount, inventory))).is_equal_to([3, 2, 5, 1])
    asserts.assert_that(sorted(inventory, key=getcount)).is_equal_to(
        [('orange', 1), ('banana', 2), ('apple', 3), ('pear', 5)])

    # multiple gets
    data = list(map(str, range(20)))
    asserts.assert_that(operator.itemgetter(2,10,5)(data)).is_equal_to(('2', '10', '5'))
    asserts.assert_fails(lambda : operator.itemgetter(2, 'x', 5)(data), ".*?got string for sequence index, want int")

    # interesting indices
    t = tuple('abcde'.elems())
    asserts.assert_that(operator.itemgetter(-1)(t)).is_equal_to('e')
    #asserts.assert_that(operator.itemgetter(slice(2, 4))(t)).is_equal_to(('c', 'd'))
    # Larky does not support any subclasses
    # def T():
    #     'Tuple subclass'
    #     self = larky.mutablestruct(__class__='T')
    #     return self
    # asserts.assert_that(operator.itemgetter(0)(T('abc'))).is_equal_to('a')
    asserts.assert_that(operator.itemgetter(0)(['a', 'b', 'c'])).is_equal_to('a')
    asserts.assert_that(operator.itemgetter(0)(range(100, 200))).is_equal_to(100)

def OperatorTestCase_test_methodcaller():
    operator = module
    asserts.assert_fails(lambda : operator.methodcaller(), "missing 1 required positional argument")
    asserts.assert_fails(lambda : operator.methodcaller(12), ".*?TypeError")
    def A():
        self = larky.mutablestruct(__class__='A')
        def foo(*args, **kwds):
            return args[0] + args[1]
        self.foo = foo
        def bar(f=42):
            return f
        self.bar = bar
        def baz(*args, **kwds):
            return kwds['name'], kwds['self']
        self.baz = baz
        return self
    a = A()
    f = operator.methodcaller('foo')
    asserts.assert_fails(lambda : f(a), ".*?index out of range")
    f = operator.methodcaller('foo', 1, 2)
    asserts.assert_that(f(a)).is_equal_to(3)
    asserts.assert_fails(lambda : f(), ".*?missing 1 required positional argument")
    asserts.assert_fails(lambda : f(a, 3), ".*?accepts no more than 1 positional argument")
    asserts.assert_fails(lambda : f(a, spam=3), ".*?got unexpected keyword argument")
    f = operator.methodcaller('bar')
    asserts.assert_that(f(a)).is_equal_to(42)
    asserts.assert_fails(lambda : f(a, a), ".*?accepts no more than 1 positional argument")
    f = operator.methodcaller('bar', f=5)
    asserts.assert_that(f(a)).is_equal_to(5)
    # Larky does not support multiple values with same name as parameters
    # f = operator.methodcaller('baz', name='spam', self='eggs')
    # asserts.assert_that(f(a)).is_equal_to(('spam', 'eggs'))

def OperatorTestCase_test_inplace():
    operator = module
    def C():
        self = larky.mutablestruct(__class__='C')
        def __iadd__     (other): return "iadd"
        self.__iadd__ = __iadd__
        def __iand__     (other): return "iand"
        self.__iand__ = __iand__
        def __ifloordiv__(other): return "ifloordiv"
        self.__ifloordiv__ = __ifloordiv__
        def __ilshift__  (other): return "ilshift"
        self.__ilshift__ = __ilshift__
        def __imod__     (other): return "imod"
        self.__imod__ = __imod__
        def __imul__     (other): return "imul"
        self.__imul__ = __imul__
        def __imatmul__  (other): return "imatmul"
        self.__imatmul__ = __imatmul__
        def __ior__      (other): return "ior"
        self.__ior__ = __ior__
        def __ipow__     (other): return "ipow"
        self.__ipow__ = __ipow__
        def __irshift__  (other): return "irshift"
        self.__irshift__ = __irshift__
        def __isub__     (other): return "isub"
        self.__isub__ = __isub__
        def __itruediv__ (other): return "itruediv"
        self.__itruediv__ = __itruediv__
        def __ixor__     (other): return "ixor"
        self.__ixor__ = __ixor__
        def __getitem__(other): return 5  # so that C is a sequence
        self.__getitem__ = __getitem__
        return self
    c = C()
    asserts.assert_that(operator.iadd     (c, 5)).is_equal_to("iadd")
    asserts.assert_that(operator.iand     (c, 5)).is_equal_to("iand")
    asserts.assert_that(operator.ifloordiv(c, 5)).is_equal_to("ifloordiv")
    asserts.assert_that(operator.ilshift  (c, 5)).is_equal_to("ilshift")
    asserts.assert_that(operator.imod     (c, 5)).is_equal_to("imod")
    asserts.assert_that(operator.imul     (c, 5)).is_equal_to("imul")
    asserts.assert_that(operator.imatmul  (c, 5)).is_equal_to("imatmul")
    asserts.assert_that(operator.ior      (c, 5)).is_equal_to("ior")
    asserts.assert_that(operator.ipow     (c, 5)).is_equal_to("ipow")
    asserts.assert_that(operator.irshift  (c, 5)).is_equal_to("irshift")
    asserts.assert_that(operator.isub     (c, 5)).is_equal_to("isub")
    asserts.assert_that(operator.itruediv (c, 5)).is_equal_to("itruediv")
    asserts.assert_that(operator.ixor     (c, 5)).is_equal_to("ixor")
    asserts.assert_that(operator.iconcat  (c, c)).is_equal_to("iadd")

def OperatorTestCase_test_dunder_is_original():
    operator = module

    names = [name for name in dir(operator) if not name.startswith('_')]
    for name in names:
        orig = getattr(operator, name)
        dunder = getattr(operator, '__' + name.strip('_') + '__', None)
        if dunder:
            asserts.assert_that(dunder).is_equal_to(orig)

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
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_neg))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_bitwise_or))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_pos))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_pow))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_rshift))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_contains))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_setitem))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_sub))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_truth))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_bitwise_xor))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_is))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_is_not))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_attrgetter))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_itemgetter))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_methodcaller))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_inplace))
    _suite.addTest(unittest.FunctionTestCase(OperatorTestCase_test_dunder_is_original))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())





