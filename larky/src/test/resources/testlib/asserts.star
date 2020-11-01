"""Unit testing support.

This exports `asserts` which contains the assertions used within tests.

This is modeled after assertpy (https://github.com/assertpy/assertpy)
"""

load("sets", "sets")
load("types", "types")
load("partial", "partial")


# assertion extensions
_extensions = {}


def _impl_function_name(f):
    """Derives the name of the given rule implementation function.

    This can be used for better test feedback.

    Args:
      impl: the rule implementation function

    Returns:
      The name of the given function
    """

    # Starlark currently stringifies a function as "<function NAME>", so we use
    # that knowledge to parse the "NAME" portion out. If this behavior ever
    # changes, we'll need to update this.
    # TODO(bazel-team): Expose a ._name field on functions to avoid this.
    impl_name = str(f)
    impl_name = impl_name.partition("<function ")[-1]
    return impl_name.rpartition(">")[0]


def _add_extension(func):
    """Add a new user-defined custom assertion to assertpy.
    Once the assertion is registered with assertpy, use it like any other assertion.  Pass val to
    :meth:`assert_that`, and then call it.
    Args:
        func: the assertion function (to be added)
    Examples:
        Usage::
            from assertpy import _add_extension
            def is_5(self):
                if self.val != 5:
                    self.error(f'{self.val} is NOT 5!')
                return self
            _add_extension(is_5)
            def test_5():
                assert_that(5).is_5()
            def test_6():
                assert_that(6).is_5()  # fails
                # 6 is NOT 5!
    """
    if not types.is_function(func):
        fail('func must be a function')
    _extensions[_impl_function_name(func)] = func


def _remove_extension(func):
    """Remove a user-defined custom assertion.
    Args:
        func: the assertion function (to be removed)
    Examples:
        Usage::
            from assertpy import _remove_extension
            _remove_extension(is_5)
    """
    if not types.is_function(func):
        fail('func must be a function')
    if _impl_function_name(func) in _extensions:
        _extensions.pop(_impl_function_name(func))


def _foo(self, par):
    print("self: ", self, " ++++ ", par)


def _AssertionBuilder(val, description, kind, expected, logger):

    self = dict(val=val,
                description=description,
                kind=kind,
                expected=expected,
                logger=logger)

    print(_impl_function_name(_AssertionBuilder), " - ")

    da = callablestruct(described_as, self=self)

    return mutablestruct(
        described_as = da,
    )


def described_as(self, description):
    """Describes the assertion.  On failure, the description is included in the error message.
    This is not an assertion itself.  But if the any of the following chained assertions fail,
    the description will be included in addition to the regular error message.
    Args:
        description: the error message description
    Examples:
        Usage::
            assert_that(1).described_as('error msg desc').is_equal_to(2)  # fails
            # [error msg desc] Expected <1> to be equal to <2>, but was not.
    Returns:
        AssertionBuilder: returns this instance to chain to the next assertion
    """
    print(self)
    self.description = str(description)
    return self

def is_equal_to(self, other, **kwargs):
    """Asserts that val is equal to other.
    Checks actual is equal to expected using the ``==`` operator. When val is *dict-like*,
    optionally ignore or include keys when checking equality.
    Args:
        other: the expected value
        **kwargs: see below
    Keyword Args:
        ignore: the dict key (or list of keys) to ignore
        include: the dict key (of list of keys) to include
    Examples:
        Usage::
            assert_that(1 + 2).is_equal_to(3)
            assert_that('foo').is_equal_to('foo')
            assert_that(123).is_equal_to(123)
            assert_that(123.4).is_equal_to(123.4)
            assert_that(['a', 'b']).is_equal_to(['a', 'b'])
            assert_that((1, 2, 3)).is_equal_to((1, 2, 3))
            assert_that({'a': 1, 'b': 2}).is_equal_to({'a': 1, 'b': 2})
            assert_that({'a', 'b'}).is_equal_to({'a', 'b'})
        When the val is *dict-like*, keys can optionally be *ignored* when checking equality::
            # ignore a single key
            assert_that({'a': 1, 'b': 2}).is_equal_to({'a': 1}, ignore='b')
            # ignore multiple keys
            assert_that({'a': 1, 'b': 2, 'c': 3}).is_equal_to({'a': 1}, ignore=['b', 'c'])
            # ignore nested keys
            assert_that({'a': {'b': 2, 'c': 3, 'd': 4}}).is_equal_to({'a': {'d': 4}}, ignore=[('a', 'b'), ('a', 'c')])
        When the val is *dict-like*, only certain keys can be *included* when checking equality::
            # include a single key
            assert_that({'a': 1, 'b': 2}).is_equal_to({'a': 1}, include='a')
            # include multiple keys
            assert_that({'a': 1, 'b': 2, 'c': 3}).is_equal_to({'a': 1, 'b': 2}, include=['a', 'b'])
        Failure produces a nice error message::
            assert_that(1).is_equal_to(2)  # fails
            # Expected <1> to be equal to <2>, but was not.
    Returns:
        AssertionBuilder: returns this instance to chain to the next assertion
    Raises:
        AssertionError: if actual is **not** equal to expected
    Tip:
        Using :meth:`is_equal_to` with a ``float`` val is just asking for trouble. Instead, you'll
        always want to use *fuzzy* numeric assertions like :meth:`~assertpy.numeric.NumericMixin.is_close_to`
        or :meth:`~assertpy.numeric.NumericMixin.is_between`.
    See Also:
        :meth:`~assertpy.string.StringMixin.is_equal_to_ignoring_case` - for case-insensitive string equality
    """
    if self._check_dict_like(self.val, check_values=False, return_as_bool=True) and \
            self._check_dict_like(other, check_values=False, return_as_bool=True):
        if self._dict_not_equal(self.val, other, ignore=kwargs.get('ignore'), include=kwargs.get('include')):
            self._dict_err(self.val, other, ignore=kwargs.get('ignore'), include=kwargs.get('include'))
    else:
        if self.val != other:
            self.error('Expected <%s> to be equal to <%s>, but was not.' % (self.val, other))
    return self




def _builder(val, description='', kind=None, expected=None, logger=None):
    """Internal helper to build a new :class:`AssertionBuilder` instance and glue on any extension methods."""
    ab = _AssertionBuilder(val, description, kind, expected, logger)
    if _extensions:
        # glue extension method onto new builder instance
        for name, func in _extensions.items():
            meth = types.MethodType(func, ab)
            #setattr(ab, name, meth)
    return ab


def assert_that(val, description=''):
    """Set the value to be tested, plus an optional description, and allow assertions to be called.
    This is a factory method for the :class:`AssertionBuilder`, and the single most important
    method in all of assertpy.
    Args:
        val: the value to be tested (aka the actual value)
        description (str, optional): the extra error message description. Defaults to ``''``
            (aka empty string)
    Examples:
        Just import it once at the top of your test file, and away you go...::
            from assertpy import assert_that
            def test_something():
                assert_that(1 + 2).is_equal_to(3)
                assert_that('foobar').is_length(6).starts_with('foo').ends_with('bar')
                assert_that(['a', 'b', 'c']).contains('a').does_not_contain('x')
    """
    return _builder(val, description)



#
#
# def is_not_equal_to(self, other):
#     """Asserts that val is not equal to other.
#     Checks actual is not equal to expected using the ``!=`` operator.
#     Args:
#         other: the expected value
#     Examples:
#         Usage::
#             assert_that(1 + 2).is_not_equal_to(4)
#             assert_that('foo').is_not_equal_to('bar')
#             assert_that(123).is_not_equal_to(456)
#             assert_that(123.4).is_not_equal_to(567.8)
#             assert_that(['a', 'b']).is_not_equal_to(['c', 'd'])
#             assert_that((1, 2, 3)).is_not_equal_to((1, 2, 4))
#             assert_that({'a': 1, 'b': 2}).is_not_equal_to({'a': 1, 'b': 3})
#             assert_that({'a', 'b'}).is_not_equal_to({'a', 'x'})
#     Returns:
#         AssertionBuilder: returns this instance to chain to the next assertion
#     Raises:
#         AssertionError: if actual **is** equal to expected
#     """
#     if self.val == other:
#         self.error('Expected <%s> to be not equal to <%s>, but was.' % (self.val, other))
#     return self
#
# def is_same_as(self, other):
#     """Asserts that val is identical to other.
#     Checks actual is identical to expected using the ``is`` operator.
#     Args:
#         other: the expected value
#     Examples:
#         Basic types are identical::
#             assert_that(1).is_same_as(1)
#             assert_that('foo').is_same_as('foo')
#             assert_that(123.4).is_same_as(123.4)
#         As are immutables like ``tuple``::
#             assert_that((1, 2, 3)).is_same_as((1, 2, 3))
#         But mutable collections like ``list``, ``dict``, and ``set`` are not::
#             # these all fail...
#             assert_that(['a', 'b']).is_same_as(['a', 'b'])  # fails
#             assert_that({'a': 1, 'b': 2}).is_same_as({'a': 1, 'b': 2})  # fails
#             assert_that({'a', 'b'}).is_same_as({'a', 'b'})  # fails
#         Unless they are the same object::
#             x = {'a': 1, 'b': 2}
#             y = x
#             assert_that(x).is_same_as(y)
#     Returns:
#         AssertionBuilder: returns this instance to chain to the next assertion
#     Raises:
#         AssertionError: if actual is **not** identical to expected
#     """
#     if self.val is not other:
#         self.error('Expected <%s> to be identical to <%s>, but was not.' % (self.val, other))
#     return self
#
# def is_not_same_as(self, other):
#     """Asserts that val is not identical to other.
#     Checks actual is not identical to expected using the ``is`` operator.
#     Args:
#         other: the expected value
#     Examples:
#         Usage::
#             assert_that(1).is_not_same_as(2)
#             assert_that('foo').is_not_same_as('bar')
#             assert_that(123.4).is_not_same_as(567.8)
#             assert_that((1, 2, 3)).is_not_same_as((1, 2, 4))
#             # mutable collections, even if equal, are not identical...
#             assert_that(['a', 'b']).is_not_same_as(['a', 'b'])
#             assert_that({'a': 1, 'b': 2}).is_not_same_as({'a': 1, 'b': 2})
#             assert_that({'a', 'b'}).is_not_same_as({'a', 'b'})
#     Returns:
#         AssertionBuilder: returns this instance to chain to the next assertion
#     Raises:
#         AssertionError: if actual **is** identical to expected
#     """
#     if self.val is other:
#         self.error('Expected <%s> to be not identical to <%s>, but was.' % (self.val, other))
#     return self
#
#
# def is_true(self):
#     """Asserts that val is true.
#     Examples:
#         Usage::
#             assert_that(True).is_true()
#             assert_that(1).is_true()
#             assert_that('foo').is_true()
#             assert_that(1.0).is_true()
#             assert_that(['a', 'b']).is_true()
#             assert_that((1, 2, 3)).is_true()
#             assert_that({'a': 1, 'b': 2}).is_true()
#             assert_that({'a', 'b'}).is_true()
#     Returns:
#         AssertionBuilder: returns this instance to chain to the next assertion
#     Raises:
#         AssertionError: if val **is** false
#     """
#     if not self.val:
#         self.error('Expected <True>, but was not.')
#     return self
#
#
# def is_false(self):
#     """Asserts that val is false.
#     Examples:
#         Usage::
#             assert_that(False).is_false()
#             assert_that(0).is_false()
#             assert_that('').is_false()
#             assert_that(0.0).is_false()
#             assert_that([]).is_false()
#             assert_that(()).is_false()
#             assert_that({}).is_false()
#             assert_that(set()).is_false()
#     Returns:
#         AssertionBuilder: returns this instance to chain to the next assertion
#     Raises:
#         AssertionError: if val **is** true
#     """
#     if self.val:
#         self.error('Expected <False>, but was not.')
#     return self
#
# def is_none(self):
#     """Asserts that val is none.
#     Examples:
#         Usage::
#             assert_that(None).is_none()
#             assert_that(print('hello world')).is_none()
#     Returns:
#         AssertionBuilder: returns this instance to chain to the next assertion
#     Raises:
#         AssertionError: if val is **not** none
#     """
#     if self.val is not None:
#         self.error('Expected <%s> to be <None>, but was not.' % self.val)
#     return self
#
# def is_not_none(self):
#     """Asserts that val is not none.
#     Examples:
#         Usage::
#             assert_that(0).is_not_none()
#             assert_that('foo').is_not_none()
#             assert_that(False).is_not_none()
#     Returns:
#         AssertionBuilder: returns this instance to chain to the next assertion
#     Raises:
#         AssertionError: if val **is** none
#     """
#     if self.val is None:
#         self.error('Expected not <None>, but was.')
#     return self
#
# def _type(self, val):
#     if hasattr(val, '__name__'):
#         return val.__name__
#     elif hasattr(val, '__class__'):
#         return val.__class__.__name__
#     return 'unknown'
#
# def is_type_of(self, some_type):
#     """Asserts that val is of the given type.
#     Args:
#         some_type (type): the expected type
#     Examples:
#         Usage::
#             assert_that(1).is_type_of(int)
#             assert_that('foo').is_type_of(str)
#             assert_that(123.4).is_type_of(float)
#             assert_that(['a', 'b']).is_type_of(list)
#             assert_that((1, 2, 3)).is_type_of(tuple)
#             assert_that({'a': 1, 'b': 2}).is_type_of(dict)
#             assert_that({'a', 'b'}).is_type_of(set)
#             assert_that(True).is_type_of(bool)
#     Returns:
#         AssertionBuilder: returns this instance to chain to the next assertion
#     Raises:
#         AssertionError: if val is **not** of the given type
#     """
#     if type(some_type) is not type and not issubclass(type(some_type), type):
#         raise TypeError('given arg must be a type')
#     if type(self.val) is not some_type:
#         t = self._type(self.val)
#         self.error('Expected <%s:%s> to be of type <%s>, but was not.' % (self.val, t, some_type.__name__))
#     return self
#
# def is_instance_of(self, some_class):
#     """Asserts that val is an instance of the given class.
#     Args:
#         some_class: the expected class
#     Examples:
#         Usage::
#             assert_that(1).is_instance_of(int)
#             assert_that('foo').is_instance_of(str)
#             assert_that(123.4).is_instance_of(float)
#             assert_that(['a', 'b']).is_instance_of(list)
#             assert_that((1, 2, 3)).is_instance_of(tuple)
#             assert_that({'a': 1, 'b': 2}).is_instance_of(dict)
#             assert_that({'a', 'b'}).is_instance_of(set)
#             assert_that(True).is_instance_of(bool)
#         With a user-defined class::
#             class Foo: pass
#             f = Foo()
#             assert_that(f).is_instance_of(Foo)
#             assert_that(f).is_instance_of(object)
#     Returns:
#         AssertionBuilder: returns this instance to chain to the next assertion
#     Raises:
#         AssertionError: if val is **not** an instance of the given class
#     """
#     try:
#         if not isinstance(self.val, some_class):
#             t = self._type(self.val)
#             self.error('Expected <%s:%s> to be instance of class <%s>, but was not.' % (self.val, t, some_class.__name__))
#     except TypeError:
#         raise TypeError('given arg must be a class')
#     return self
#
# def is_length(self, length):
#     """Asserts that val is the given length.
#     Checks val is the given length using the ``len()`` built-in.
#     Args:
#         length (int): the expected length
#     Examples:
#         Usage::
#             assert_that('foo').is_length(3)
#             assert_that(['a', 'b']).is_length(2)
#             assert_that((1, 2, 3)).is_length(3)
#             assert_that({'a': 1, 'b': 2}).is_length(2)
#             assert_that({'a', 'b'}).is_length(2)
#     Returns:
#         AssertionBuilder: returns this instance to chain to the next assertion
#     Raises:
#         AssertionError: if val is **not** the given length
#     """
#     if type(length) is not int:
#         raise TypeError('given arg must be an int')
#     if length < 0:
#         raise ValueError('given arg must be a positive int')
#     if len(self.val) != length:
#         self.error('Expected <%s> to be of length <%d>, but was <%d>.' % (self.val, length, len(self.val)))
#     return self


# def _begin(ctx):
#     """Begins a unit test.
#     This should be the first function called in a unit test implementation
#     function. It initializes a "test environment" that is used to collect
#     assertion failures so that they can be reported and logged at the end of the
#     test.
#     Args:
#       ctx: The Starlark context. Pass the implementation function's `ctx` argument
#           in verbatim.
#     Returns:
#       A test environment struct that must be passed to assertions and finally to
#       `unittest.end`. Do not rely on internal details about the fields in this
#       struct as it may change.
#     """
#     return struct(ctx = ctx, failures = [])
#
#
# def _fail(env, msg):
#     """Unconditionally causes the current test to fail.
#     Args:
#       env: The test environment returned by `unittest.begin`.
#       msg: The message to log describing the failure.
#     """
#     full_msg = "In test %s: %s" % (env.ctx.attr._impl_name, msg)
#
#     # There isn't a better way to output the message in Starlark, so use print.
#     # buildifier: disable=print
#     print(full_msg)
#     env.failures.append(full_msg)
#
# def _assert_true(
#         env,
#         condition,
#         msg = "Expected condition to be true, but was false."):
#     """Asserts that the given `condition` is true.
#     Args:
#       env: The test environment returned by `unittest.begin`.
#       condition: A value that will be evaluated in a Boolean context.
#       msg: An optional message that will be printed that describes the failure.
#           If omitted, a default will be used.
#     """
#     if not condition:
#         _fail(env, msg)
#
# def _assert_false(
#         env,
#         condition,
#         msg = "Expected condition to be false, but was true."):
#     """Asserts that the given `condition` is false.
#     Args:
#       env: The test environment returned by `unittest.begin`.
#       condition: A value that will be evaluated in a Boolean context.
#       msg: An optional message that will be printed that describes the failure.
#           If omitted, a default will be used.
#     """
#     if condition:
#         _fail(env, msg)
#
# def _assert_equals(env, expected, actual, msg = None):
#     """Asserts that the given `expected` and `actual` values are equal.
#     Args:
#       env: The test environment returned by `unittest.begin`.
#       expected: The expected value of some computation.
#       actual: The actual value returned by some computation.
#       msg: An optional message that will be printed that describes the failure.
#           If omitted, a default will be used.
#     """
#     if expected != actual:
#         expectation_msg = 'Expected "%s", but got "%s"' % (expected, actual)
#         if msg:
#             full_msg = "%s (%s)" % (msg, expectation_msg)
#         else:
#             full_msg = expectation_msg
#         _fail(env, full_msg)
#
# def _assert_set_equals(env, expected, actual, msg = None):
#     """Asserts that the given `expected` and `actual` sets are equal.
#     Args:
#       env: The test environment returned by `unittest.begin`.
#       expected: The expected set resulting from some computation.
#       actual: The actual set returned by some computation.
#       msg: An optional message that will be printed that describes the failure.
#           If omitted, a default will be used.
#     """
#     if not new_sets.is_equal(expected, actual):
#         missing = new_sets.difference(expected, actual)
#         unexpected = new_sets.difference(actual, expected)
#         expectation_msg = "Expected %s, but got %s" % (new_sets.str(expected), new_sets.str(actual))
#         if new_sets.length(missing) > 0:
#             expectation_msg += ", missing are %s" % (new_sets.str(missing))
#         if new_sets.length(unexpected) > 0:
#             expectation_msg += ", unexpected are %s" % (new_sets.str(unexpected))
#         if msg:
#             full_msg = "%s (%s)" % (msg, expectation_msg)
#         else:
#             full_msg = expectation_msg
#         _fail(env, full_msg)
#
# _assert_new_set_equals = _assert_set_equals
#
# def _expect_failure(env, expected_failure_msg = ""):
#     """Asserts that the target under test has failed with a given error message.
#     This requires that the analysis test is created with `analysistest.make()` and
#     `expect_failures = True` is specified.
#     Args:
#       env: The test environment returned by `analysistest.begin`.
#       expected_failure_msg: The error message to expect as a result of analysis failures.
#     """
#     dep = _target_under_test(env)
#     if AnalysisFailureInfo in dep:
#         actual_errors = ""
#         for cause in dep[AnalysisFailureInfo].causes.to_list():
#             actual_errors += cause.message + "\n"
#         if actual_errors.find(expected_failure_msg) < 0:
#             expectation_msg = "Expected errors to contain '%s' but did not. " % expected_failure_msg
#             expectation_msg += "Actual errors:%s" % actual_errors
#             _fail(env, expectation_msg)
#     else:
#         _fail(env, "Expected failure of target_under_test, but found success")
#
# def _target_under_test(env):
#     """Returns the target under test.
#     Args:
#       env: The test environment returned by `analysistest.begin`.
#     Returns:
#       The target under test.
#     """
#     result = getattr(env.ctx.attr, "target_under_test")
#     if types.is_list(result):
#         if result:
#             return result[0]
#         else:
#             fail("test rule does not have a target_under_test")
#     return result
#
# asserts = struct(
#     expect_failure = _expect_failure,
#     equals = _assert_equals,
#     false = _assert_false,
#     set_equals = _assert_set_equals,
#     new_set_equals = _assert_new_set_equals,
#     true = _assert_true,
# )

asserts = struct(
    add_extension = _add_extension,
    remove_extension = _remove_extension,
    assert_that = assert_that
)