# For compatibility help with Python, introduced globals are going to be using
# this as a namespace


def _to_dict(s):
    """Converts a `struct` to a `dict`.
    Args:
      s: A `struct`.
    Returns:
      A `dict` whose keys and values are the same as the fields in `s`. The
      transformation is only applied to the struct's fields and not to any
      nested values.
    """
    # NOTE: This will evaluate properties on getattr(), so it is *IMPORTANT*
    # that if you use this function, to not expect properties to fail()
    #
    # You might be better of using repr() instead!
    attributes = dir(s)
    if "to_json" in attributes:
        attributes.remove("to_json")
    if "to_proto" in attributes:
        attributes.remove("to_proto")
    return {key: getattr(s, key) for key in attributes}


# emulates while loop but will iterate *only* for 4096 steps.
WHILE_LOOP_EMULATION_ITERATION = 4096

_SENTINEL = _sentinel()

# TODO: maybe move to a testutils?
def _parametrize(testaddr, testcase, param, args):
    """
    Emulates the pytest.parametrize() but with some larky differences.

    Decorator a testaddr to create a testcase that is parameterized by `param`
    and a list of `args` that are set to the param.

    :param testaddr: in Larky, this would be `suite.addTest`
    :param testcase: in Larky, this would be `unittest.FunctionTestCase`
    :param param: the parameter to set
    :param args: The variable parameters to set param to
    :return: None

    Examples:
        >>> load("@stdlib//larky", "larky")
        >>> load("@stdlib//unittest", "unittest")
        >>> load("@vendor//asserts", "asserts")
        >>>
        >>> def _test(val):
        ...    asserts.assert_eq(val).is_equal_to(val)
        >>>
        >>> _suite = unittest.TestSuite()
        >>> larky.parametrize(
        ...    _suite.addTest,
        ...    unittest.FunctionTestCase,
        ...    'val',
        ...    [0, None, {}, [], False])(_test)
    """
    split_on = ',' if ',' in param else None
    # cannot import types in larky module, so this test allows
    # to see if multiple params exist
    if not hasattr(param, 'append') and split_on:
        param = param.split(split_on)
    elif not hasattr(param, 'append'):
        param = [param]

    def parametrized(func):
        if len(param) == 1:
            for arg in args:
                testaddr(testcase(_partial(func, **dict(zip(param, [arg])))))
        else:
            for arg in args:
                testaddr(testcase(_partial(func, **dict(zip(param, arg)))))
    return parametrized



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
    return _func_name(f)
    # cls_type = str(f)
    # if 'built-in' in cls_type or '<function ' in cls_type:
    #     cls_type = cls_type.split(" ")[-1].rpartition(">")[0]
    # return cls_type


def _is_instance(instance, some_class_or_factory):
    t = type(instance)
    # are the types the same?
    _hasname = None
    for i in ('__class__', '__name__',):
        _hasname = getattr(some_class_or_factory, i, None)
        if _hasname:
            break
    if _hasname:
        _hasname = str(_hasname)
        if 'built-in' in _hasname or '<function ' in _hasname:
            _hasname = _hasname.split(" ")[-1].rpartition(">")[0]

    if _hasname and t == _hasname:
        return True
    # otherwise
    cls_type = _impl_function_name(some_class_or_factory)
    # TODO(Larky::Difference) this hack here is specialization for comparing
    #  str to string when we do str(str) in larky, we get
    #  <built-in function str>, but in python, this is <class 'str'>.
    #  This could actually be a starlark inconsistency, but unclear.
    if t == 'string' and cls_type == 'str':
        return True
    return t == cls_type


def translate_bytes(s, original, replace):
    """
    Return a copy of the bytes or bytearray object where all bytes occurring
    in the optional argument delete are removed, and the remaining bytes have
    been mapped through the given translation table, which must be a bytes
    object of length 256.

    TODO: You can use the bytes.maketrans() method to create a translation
     table.

    Set the table argument to None for translations that only
    delete characters:

        >>> translate_bytes(b'read this short text', None, b'aeiou')
        b'rd ths shrt txt'

    :param s:
    :param original:
    :param replace:
    :return:
    """
    if not (_is_instance(s, bytes) or _is_instance(s, bytearray)):
        fail('TypeError: expected bytes, not %s' % type(s))
    return s.translate(original, replace)
    # original_arr = bytearray(original)
    # replace_arr = bytearray(replace)
    #
    # if len(original_arr) != len(replace_arr):
    #     fail('Original and replace bytes should be same in length')
    # translated = bytearray()
    # replace_dics = dict()
    #
    # for i in range(len(original_arr)):
    #     replace_dics[original_arr[i]] = replace_arr[i]
    # content_arr = bytearray(s)
    #
    # for c in content_arr:
    #     if c in replace_dics.keys():
    #         translated += bytearray([replace_dics[c]])
    #     else:
    #         translated += bytearray([c])
    # return bytes(translated)


def _zfill(x, leading=4):
    if len(str(x)) < leading:
        return (('0' * leading) + str(x))[-leading:]
    else:
        return str(x)


def _DeterministicGenerator(func, *args, **kwargs):
    """
    Exploits iterator protocol support to emulate a generator that
    preserves state by returning an iterator that supports `__getitem__`
    to emulate a "deterministic" generator.

    :param func: Must be a function that returns a
      `vendor/option/results.star`#`Result` object
    :return: an iterator that iterates over a fixed and deterministic sequence
    """
    self = _mutablestruct(__name__='DeterministicGenerator',
                          __class__=_DeterministicGenerator)

    def __init__(func, *args, **kwargs):
        self.f = func
        self.args = args
        self.kwargs = kwargs
        return self
    self = __init__(func, *args, **kwargs)

    def __getitem__(i):
        r = self.f(i, *self.args, **self.kwargs)
        if r.is_err and r == StopIteration():
            return IndexError()
        return r.unwrap()

    self.__getitem__ = __getitem__
    return iter(self)



def _fromkeys(iterable, value=None):
    """dict.fromkeys(S[, v]) ->

    Create a new dictionary with keys from iterable and values set to value.
    """
    return {key: value for key in iterable}

#
# def _with(ctx):
#     l = ctx.__enter__()
#     try:
#         f(*args, **kwargs)
#         if not len(l) > 0:
#             raise AssertionError("No warning raised when calling %s"
#                     % f.__name__)
#         if not l[0].category is DeprecationWarning:
#             raise AssertionError("First warning for %s is not a " \
#                     "DeprecationWarning( is %s)" % (f.__name__, l[0]))
#     finally:
#         ctx.__exit__()
#

def _Peekable(iterator, retain_max_elems=5):
    """An iterable class which can return the next element of the wrapped
    iterator without advancing it."""

    self = _mutablestruct(__name__='Peekable', __class__=_Peekable)
    self.SENTINEL = _SENTINEL

    def __init__(iterator, retain_max_elems):
        self.cache = []
        self.peeked = []
        self._retain_max_elems = retain_max_elems

        if hasattr(iterator, '__iter__'):
            self.iterator = iter(iterator)
        else:
            self.iterator = iterator
        return self
    self = __init__(iterator, retain_max_elems)

    def __iter__():
        return self
    self.__iter__ = __iter__

    def __bool__():
        rv = self.peek()
        if rv == StopIteration:
            return False
        return True
    self.__bool__ = __bool__

    def __next__():
        # TODO: fix this
        i = self.peeked.pop() if self.peeked else next(self.iterator)
        self.cache.append(i)
        self._ensure_size()
        return i
    self.__next__ = __next__
    self.next = __next__

    def peek(default=_SENTINEL):
        if self.peeked:
            # we already have done the peek, let's return the head
            return self.peeked[-1]

        i = next(self.iterator)
        if i == StopIteration:
            if default == self.SENTINEL:
                return StopIteration
            i = default

        self.peeked.append(i)
        return i
    self.peek = peek

    def rewind(n):
        if not (len(self.cache) >= n):
            fail("assert len(self.cache) >= n failed!")

        for _ in range(n):
            self.peeked.append(self.cache.pop())
    self.rewind = rewind

    def putback(*items):
        for item in items:
            self.cache.append(item)
        self._ensure_size()
        self.rewind(len(items))
    self.putback = putback

    def flush():
        self.cache.clear()
    self.flush = flush

    def _ensure_size():
        if len(self.cache) >= self._retain_max_elems:
            self.cache = self.cache[-self._retain_max_elems :]
    self._ensure_size = _ensure_size
    return self


larky = _struct(
    struct=_struct,
    mutablestruct=_mutablestruct,
    to_dict=_to_dict,
    partial=_partial,
    property=_property,
    WHILE_LOOP_EMULATION_ITERATION=WHILE_LOOP_EMULATION_ITERATION,
    SENTINEL=_SENTINEL,
    parametrize=_parametrize,
    is_instance=_is_instance,
    impl_function_name=_impl_function_name,
    translate_bytes=translate_bytes,
    DeterministicGenerator=_DeterministicGenerator,
    strings=_struct(
        zfill=_zfill,
    ),
    dicts=_struct(
      fromkeys=_fromkeys,
    ),
    utils=_struct(
        Counter=_Counter,
        ThreadsafeCounter=_ThreadsafeCounter,
        Peekable=_Peekable,
    ))