load("@stdlib//larky", "larky")
load("@stdlib//builtins", "builtins")
load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@stdlib//types", types="types")
load("@vendor//option/result", safe="safe", Error="Error", Result="Result")


# sum()
# // start //
def test_sum_len_1():
    asserts.assert_that(builtins.sum([1])).is_equal_to(1)

def test_sum_len_2():
    asserts.assert_that(builtins.sum([1,2])).is_equal_to(3)

def test_sum_len_3():
    asserts.assert_that(builtins.sum([1,2,3])).is_equal_to(6)

def test_sum_len_10():
    asserts.assert_that(builtins.sum([1,1,1,1,1,1,1,1,1,1])).is_equal_to(10)

def test_sum_kwstart():
    asserts.assert_that(builtins.sum([1,2,3,4,5],start=3)).is_equal_to(18)

def test_sum_start():
    asserts.assert_that(builtins.sum([1,2,3],4)).is_equal_to(10)

def test_too_many_args():
    asserts.assert_fails(lambda: builtins.sum([1,2,3],4,5), ".*?TypeError")

def test_too_many_args2():
    asserts.assert_fails(lambda: builtins.sum([1,2,3],4,start=5), ".*?TypeError")

def _add_sum_suite(suite):
    suite.addTest(unittest.FunctionTestCase(test_sum_len_1))
    suite.addTest(unittest.FunctionTestCase(test_sum_len_2))
    suite.addTest(unittest.FunctionTestCase(test_sum_len_3))
    suite.addTest(unittest.FunctionTestCase(test_sum_len_10))
    suite.addTest(unittest.FunctionTestCase(test_sum_start))
    suite.addTest(unittest.FunctionTestCase(test_sum_kwstart))
    suite.addTest(unittest.FunctionTestCase(test_too_many_args))
    suite.addTest(unittest.FunctionTestCase(test_too_many_args2))
    return suite
# // end //


# iter()
# // start test iter() //
def test_iter_string():
    s = "foo"
    asserts.assert_fails(lambda: [x for x in s], ".*not iterable*")
    s_iter = iter(s)
    asserts.assert_that(next(s_iter)).is_equal_to("f")
    asserts.assert_that(next(s_iter)).is_equal_to("o")
    asserts.assert_that(next(s_iter)).is_equal_to("o")
    asserts.assert_fails(lambda: next(s_iter), ".*StopIteration")
    # test normal for iteration works fine
    asserts.assert_that([x for x in iter(s)]).is_equal_to(["f", "o", "o"])
    asserts.assert_that(list(iter(s))).is_equal_to(["f", "o", "o"])


def test_iter_with_Result_type(func):
    s = "foo"
    s_iter = iter(s)
    asserts.assert_that(list(s_iter)).is_equal_to(["f", "o", "o"])

    rval = func(s_iter)#
    asserts.assert_that(rval.is_err).is_true()
    # Confirm that safe() will return the actual instance of the the Error subtype
    asserts.assert_that(rval).is_instance_of(StopIteration)
    # This confirms that we can retrieve the underling exception instance
    asserts.assert_that(rval.expect_err("fail if you see this")).is_instance_of(StopIteration)
    # this deviates from python b/c StopIteration() isn't the same but in
    # Larky it is a sentinel => just like None
    asserts.assert_that(StopIteration()).is_equal_to(rval)
    # Can we check to see if rval is matched to StopIteration?
    asserts.assert_that(Result.error_is(str(StopIteration()), rval)).is_not_none()
    # Check to see if we can match it for isinstance() for python compatibility
    # reasons
    asserts.assert_that(builtins.isinstance(rval, StopIteration))


def test_standard_iter_operations():
    s = "foo"

    asserts.assert_that(
        dir(iter(s))
    ).is_equal_to(
        ["__iter__", "__next__"]
    )
    asserts.assert_that(repr(iter(s))).matches(r"<str_iterator at 0x")
    s_iter = iter("foo")
    asserts.assert_that("o" in s_iter).is_true()
    # we should have iterated over "f" and "o"
    # confirm that we are at the final "o"
    asserts.assert_that(next(s_iter)).is_equal_to("o")
    # we have exhausted the iterator, right?
    asserts.assert_fails(lambda: next(s_iter), ".*StopIteration")


def test_iter_list():
    s = "foo".elems()
    asserts.assert_that(s).is_instance_of(list)
    s_iter = iter(s)
    asserts.assert_that(next(s_iter)).is_equal_to("f")
    asserts.assert_that(next(s_iter)).is_equal_to("o")
    asserts.assert_that(next(s_iter)).is_equal_to("o")
    asserts.assert_fails(lambda: next(s_iter), ".*StopIteration")
    asserts.assert_that(repr(s_iter)).matches(r"<list_iterator at 0x")


def IteratorProxyClass(i):
    self = larky.mutablestruct(__name__='IteratorProxyClass',
                               __class__=IteratorProxyClass)

    def __init__(i):
        self.i = i
        return self

    self = __init__(i)

    def __next__():
        return next(self.i)

    self.__next__ = __next__

    def __iter__():
        return self

    self.__iter__ = __iter__
    return self


def SequenceClass(n):
    self = larky.mutablestruct(__name__='SequenceClass',
                               __class__=SequenceClass)

    def __init__(n):
        self.n = n
        return self

    self = __init__(n)

    def __getitem__(i):
        if (0 <= i) and (i < self.n):
            return i
        return IndexError()

    self.__getitem__ = __getitem__
    return self


# Test two-argument iter() with callable instance
def TwoArgumentIter():
    self = larky.mutablestruct(__name__='TwoArgumentIter',
                               __class__=TwoArgumentIter)
    def __init__():
        self.i = 0
        return self
    self = __init__()

    def __call__():
        i = self.i
        self.i = i + 1
        if i > 100:
            return IndexError()  # Emergency stop
        return i
    self.__call__ = __call__
    return self


def test_iter_userdefined():
    clz = IteratorProxyClass(iter(range(3)))
    s_iter = iter(clz)
    asserts.assert_that(repr(s_iter)).matches(r"<IteratorProxyClass_iterator at 0x")
    asserts.assert_that(next(s_iter)).is_equal_to(0)
    asserts.assert_that(next(s_iter)).is_equal_to(1)
    asserts.assert_that(next(s_iter)).is_equal_to(2)
    asserts.assert_fails(lambda: next(s_iter), ".*StopIteration")
    asserts.assert_that(
        [x for x in iter(IteratorProxyClass(iter(range(3))))]
    ).is_equal_to([0, 1, 2])

    asserts.assert_that(
        list(iter(IteratorProxyClass(iter(range(3)))))
    ).is_equal_to([0, 1, 2])

    z = iter(IteratorProxyClass(iter(range(3))))
    for i in range(3):
        asserts.assert_that(i in z).is_true()
    asserts.assert_fails(lambda: next(z), ".*StopIteration")

    z = iter(IteratorProxyClass(iter(range(3))))
    for i in range(3, 5):
        asserts.assert_that(i in z).is_false()
    asserts.assert_fails(lambda: next(z), ".*StopIteration")

    z = iter(SequenceClass(5))
    for i in range(5):
        asserts.assert_that(i).is_equal_to(next(z))
    asserts.assert_fails(lambda: next(z), ".*StopIteration")

    # test next(iterator[, default])
    asserts.assert_that(next(z, larky.SENTINEL)).is_equal_to(larky.SENTINEL)


def test_iter_callable():
    asserts.assert_that(
        list(iter(TwoArgumentIter(), 10))
     ).is_equal_to(
        list(range(10))
    )

# Test two-argument iter() with function
def test_iter_function():
    def spam(state=[0]):
        i = state[0]
        state[0] = i + 1
        return i

    asserts.assert_that(
        list(iter(spam, 10))
    ).is_equal_to(
        list(range(10))
    )


# Test two-argument iter() with function that raises StopIteration
def test_iter_function_stop():
    def spam(state=[0]):
        i = state[0]
        if i == 10:
            return StopIteration()
        state[0] = i + 1
        return i

    asserts.assert_that(
        list(iter(spam, 20))
    ).is_equal_to(
        list(range(10))
    )


def _Result_map(s_iter):
    return Result.Ok(s_iter).map(next)

def _Result_safe(s_iter):
    return safe(next)(s_iter)

def _add_iter_suite(suite):
    suite.addTest(unittest.FunctionTestCase(test_iter_string))

    larky.parametrize(
        suite.addTest,
        unittest.FunctionTestCase,
        'func',
        [
            _Result_map,
            _Result_safe
        ],
    )(test_iter_with_Result_type)

    suite.addTest(unittest.FunctionTestCase(test_standard_iter_operations))
    suite.addTest(unittest.FunctionTestCase(test_iter_list))
    suite.addTest(unittest.FunctionTestCase(test_iter_userdefined))
    suite.addTest(unittest.FunctionTestCase(test_iter_callable))
    suite.addTest(unittest.FunctionTestCase(test_iter_function))
    suite.addTest(unittest.FunctionTestCase(test_iter_function_stop))

    return suite
# // end test iter() //


def _testsuite():
    _suite = unittest.TestSuite()
    #_add_sum_suite(_suite)
    _add_iter_suite(_suite)
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
