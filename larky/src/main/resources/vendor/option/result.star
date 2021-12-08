load("@stdlib//larky", "larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//types", types="types")
load("@stdlib//re", re="re")
load("@stdlib//jresult", _JResult="jresult")
load("@stdlib//enum", "enum")

#
# def _Result(val, is_ok):
#     """
#     :class:`Result` is a type that either success (:meth:`Result.Ok`)
#     or failure (:meth:`Result.Err`).
#
#     To create an Ok value, use :meth:`Result.Ok` or :func:`Ok`.
#
#     To create a Err value, use :meth:`Result.Err` or :func:`Err`.
#
#     Calling the :class:`Result` constructor directly will raise a ``TypeError``.
#
#     Examples:
#         >>> Result.Ok(1)
#         Ok(1)
#         >>> Result.Error('Fail!')
#         Error('Fail!')
#     """
#
#     self = larky.mutablestruct(__class__='Result')
#
#     def __init__(val, is_ok):
#         self._val = val
#         self._is_ok = is_ok
#         self._type = _JResult
#         self._obj = _JResult.Ok(val) if is_ok else _JResult.Error(val)
#         return self
#     self = __init__(val, is_ok)
#
#     def is_ok():
#         """
#         Returns `True` if the result is :meth:`Result.Ok`.
#
#         Examples:
#             >>> Ok(1).is_ok
#             True
#             >>> Err(1).is_ok
#             False
#         """
#         return self._is_ok
#     self.is_ok = larky.property(is_ok)
#
#     def is_err():
#         """
#         Returns `True` if the result is :meth:`Result.Err`.
#
#         Examples:
#             >>> Ok(1).is_err
#             False
#             >>> Err(1).is_err
#             True
#         """
#         return not self._is_ok
#     self.is_err = larky.property(is_err)
#
#     # def ok():
#     #     """
#     #     Converts from :class:`Result` [T, E] to :class:`option.option_.Option` [T].
#     #
#     #     Returns:
#     #         :class:`Option` containing the success value if `self` is
#     #         :meth:`Result.Ok`, otherwise :data:`option.option_.NONE`.
#     #
#     #     Examples:
#     #         >>> Ok(1).ok()
#     #         Some(1)
#     #         >>> Err(1).ok()
#     #         NONE
#     #     """
#     #     return Option.Some(self._val) if self._is_ok else NONE  # type: ignore
#     # self.ok = ok
#
#     # def err():
#     #     """
#     #     Converts from :class:`Result` [T, E] to :class:`option.option_.Option` [E].
#     #
#     #     Returns:
#     #         :class:`Option` containing the error value if `self` is
#     #         :meth:`Result.Err`, otherwise :data:`option.option_.NONE`.
#     #
#     #     Examples:
#     #         >>> Ok(1).err()
#     #         NONE
#     #         >>> Err(1).err()
#     #         Some(1)
#     #     """
#     #     return NONE if self._is_ok else Option.Some(self._val)  # type: ignore
#     # self.err = err
#
#     def map(op):
#         """
#         Applies a function to the contained :meth:`Result.Ok` value.
#
#         Args:
#             op: The function to apply to the :meth:`Result.Ok` value.
#
#         Returns:
#             A :class:`Result` with its success value as the function result
#             if `self` is an :meth:`Result.Ok` value, otherwise returns
#             `self`.
#
#         Examples:
#             >>> Ok(1).map(lambda x: x * 2)
#             Ok(2)
#             >>> Err(1).map(lambda x: x * 2)
#             Err(1)
#             >>> Ok([]).map(iter).map(next).map(lambda x: x * 2)
#             Err(StopIteration)
#
#         """
#         return self._obj.map(op)
#         # return self._type.Ok(op(self._val)) if self._is_ok else self  # type: ignore
#     self.map = map
#
#     def flatmap(op):
#         """
#         Applies a function to the contained :meth:`Result.Ok` value.
#
#         This is different than :meth:`Result.map` because the function
#         result is not wrapped in a new :class:`Result`.
#
#         Args:
#             op: The function to apply to the contained :meth:`Result.Ok` value.
#
#         Returns:
#             The result of the function if `self` is an :meth:`Result.Ok` value,
#              otherwise returns `self`.
#
#         Examples:
#             >>> def sq(x): return Ok(x * x)
#             >>> def err(x): return Err(x)
#             >>> def nextelement(x): return next(x)
#             >>> Ok(2).flatmap(sq).flatmap(sq)
#             Ok(16)
#             >>> Ok(2).flatmap(sq).flatmap(err)
#             Err(4)
#             >>> Ok(2).flatmap(err).flatmap(sq)
#             Err(2)
#             >>> Err(3).flatmap(sq).flatmap(sq)
#             Err(3)
#             >>> Ok([]).flatmap(iter).flatmap(nextelement).flatmap(sq)
#             Err(StopIteration)
#         """
#         return op(self._val) if self._is_ok else self  # type: ignore
#     self.flatmap = flatmap
#
#     def map_err(op):
#         """
#         Applies a function to the contained :meth:`Result.Err` value.
#
#         Args:
#             op: The function to apply to the :meth:`Result.Err` value.
#
#         Returns:
#             A :class:`Result` with its error value as the function result
#             if `self` is a :meth:`Result.Err` value, otherwise returns
#             `self`.
#
#         Examples:
#             >>> Ok(1).map_err(lambda x: x * 2)
#             Ok(1)
#             >>> Err(1).map_err(lambda x: x * 2)
#             Err(2)
#         """
#         return self._obj.map_err(op)
#         # return self if self._is_ok else self._type.Error(op(self._val))  # type: ignore
#     self.map_err = map_err
#
#     def unwrap():
#         """
#         Returns the success value in the :class:`Result`.
#
#         Returns:
#             The success value in the :class:`Result`.
#
#         Raises:
#             ``ValueError`` with the message provided by the error value
#              if the :class:`Result` is a :meth:`Result.Err` value.
#
#         Examples:
#             >>> Ok(1).unwrap()
#             1
#             >>> try:
#             ...     Err(1).unwrap()
#             ... except ValueError as e:
#             ...     print(e)
#             1
#         """
#         return self._obj.unwrap()
#     self.unwrap = unwrap
#
#     def unwrap_or(optb):
#         """
#         Returns the success value in the :class:`Result` or ``optb``.
#
#         Args:
#             optb: The default return value.
#
#         Returns:
#             The success value in the :class:`Result` if it is a
#             :meth:`Result.Ok` value, otherwise ``optb``.
#
#         Notes:
#             If you wish to use a result of a function call as the default,
#             it is recommnded to use :meth:`unwrap_or_else` instead.
#
#         Examples:
#             >>> Ok(1).unwrap_or(2)
#             1
#             >>> Err(1).unwrap_or(2)
#             2
#         """
#         return self._obj.unwrap_or(optb)
#     self.unwrap_or = unwrap_or
#
#     def unwrap_or_else(op):
#         """
#         Returns the sucess value in the :class:`Result` or computes a default
#         from the error value.
#
#         Args:
#             op: The function to computes default with.
#
#         Returns:
#             The success value in the :class:`Result` if it is
#              a :meth:`Result.Ok` value, otherwise ``op(E)``.
#
#         Examples:
#             >>> Ok(1).unwrap_or_else(lambda e: e * 10)
#             1
#             >>> Err(1).unwrap_or_else(lambda e: e * 10)
#             10
#         """
#         return self._obj.unwrap_or_else(op)
#         # return self._val if self._is_ok else op(self._val)  # type: ignore
#     self.unwrap_or_else = unwrap_or_else
#
#     def expect(msg):
#         """
#         Returns the success value in the :class:`Result` or raises
#         a ``ValueError`` with a provided message.
#
#         Args:
#             msg: The error message.
#
#         Returns:
#             The success value in the :class:`Result` if it is
#             a :meth:`Result.Ok` value.
#
#         Raises:
#             ``ValueError`` with ``msg`` as the message if the
#             :class:`Result` is a :meth:`Result.Err` value.
#
#         Examples:
#             >>> Ok(1).expect('no')
#             1
#             >>> try:
#             ...     Err(1).expect('no')
#             ... except ValueError as e:
#             ...     print(e)
#             no
#         """
#         return self._obj.expect(msg)
#     self.expect = expect
#
#     def unwrap_err():
#         """
#         Returns the error value in a :class:`Result`.
#
#         Returns:
#             The error value in the :class:`Result` if it is a
#             :meth:`Result.Err` value.
#
#         Raises:
#             ``ValueError`` with the message provided by the success value
#              if the :class:`Result` is a :meth:`Result.Ok` value.
#
#         Examples:
#             >>> try:
#             ...     Ok(1).unwrap_err()
#             ... except ValueError as e:
#             ...     print(e)
#             1
#             >>> Err('Oh No').unwrap_err()
#             'Oh No'
#         """
#         self._obj.unwrap_err()
#     self.unwrap_err = unwrap_err
#
#     def expect_err(msg):
#         """
#         Returns the error value in a :class:`Result`, or raises a
#         ``ValueError`` with the provided message.
#
#         Args:
#             msg: The error message.
#
#         Returns:
#             The error value in the :class:`Result` if it is a
#             :meth:`Result.Err` value.
#
#         Raises:
#             ``ValueError`` with the message provided by ``msg`` if
#             the :class:`Result` is a :meth:`Result.Ok` value.
#
#         Examples:
#             >>> try:
#             ...     Ok(1).expect_err('Oh No')
#             ... except ValueError as e:
#             ...     print(e)
#             Oh No
#             >>> Err(1).expect_err('Yes')
#             1
#         """
#         self._obj.expect_err(msg)
#     self.expect_err = expect_err
#
#
#     def __bool__():
#         return self._is_ok
#     self.__bool__ = __bool__
#
#     def __repr__():
#         return ''.join([
#             'Ok' if self._is_ok else 'Err', '({', repr(self._val), '})'
#         ])
#
#     self.__repr__ = __repr__
#
#     def __hash__():
#         return hash((self._type, self._is_ok, self._val))
#     self.__hash__ = __hash_
#
#     def __eq__(other):
#         return (types.is_instance(other, self._type)
#                 and self._is_ok == other._is_ok
#                 and self._val == other._val)
#     self.__eq__ = __eq__
#
#     def __ne__(other):
#         return (not types.is_instance(other, self._type)
#                 or self._is_ok != other._is_ok
#                 or self._val != other._val)
#     self.__ne__ = __ne__
#
#     def __lt__(other):
#         if types.is_instance(other, self._type):
#             if self._is_ok == other._is_ok:
#                 return self._val < other._val
#             return self._is_ok
#         fail("NotImplemented")
#     self.__lt__ = __lt__
#
#     def __le__(other):
#         if types.is_instance(other, self._type):
#             if self._is_ok == other._is_ok:
#                 return self._val <= other._val
#             return self._is_ok
#         fail("NotImplemented")
#     self.__le__ = __le__
#
#     def __gt__(other):
#         if types.is_instance(other, self._type):
#             if self._is_ok == other._is_ok:
#                 return self._val > other._val
#             return other._is_ok
#         fail("NotImplemented")
#     self.__gt__ = __gt__
#
#     def __ge__(other):
#         if types.is_instance(other, self._type):
#             if self._is_ok == other._is_ok:
#                 return self._val >= other._val
#             return other._is_ok
#         fail("NotImplemented")
#     self.__ge__ = __ge__
#     return self
#
#
# def Ok(val):
#     """Shortcut function for :meth:`Result.Ok`.
#
#     Contains the success value.
#
#     Args:
#          val: The success value.
#     Returns:
#          The :class:`Result` containing the success value.
#     Examples:
#         >>> res = Result.Ok(1)
#         >>> res
#         Ok(1)
#         >>> res.is_ok
#         True
#     """
#     return _Result(val, True)
#
#
# def Error(err):
#     """Shortcut function for :meth:`Result.Err`.
#
#     Contains the error value.
#
#     Args:
#         err: The error value.
#     Returns:
#         The :class:`Result` containing the error value.
#     Examples:
#         >>> res = Result.Error('Oh No')
#         >>> res
#         Err('Oh No')
#         >>> res.is_err
#         True
#     """
#     return _Result(err, False)

def Ok(val):

    return _JResult.Ok(val)


def Error(val):
    """
    Contains the error value.
    Args:
        err: The error value.
    Returns:
        The :class:`Result` containing the error value.
    Examples:
        >>> res = Result.Error('Oh No')
        >>> res
        Err('Oh No')
        >>> res.is_err
        True
    """
    return _JResult.Error(val)


Err = Error


## larky extensions below:

def safe(function):
    """
    Decorator to convert exception-throwing function to ``Result`` container.

    This decorator only works with sync functions. Example:

    .. code:: python

      >>> load("@stdlib//larky", "larky")
      >>> load("@vendor//result", Result="Result", safe="safe")
      >>> load("@vendor//asserts", "asserts")
      >>> def might_raise(arg):
      ...     return 1 / arg
      >>> asserts.assert_that(safe(might_raise)(1)).is_equal_to(Ok(1.0))
      >>> failed = safe(might_raise)(0)
      >>> asserts.assert_fails(lambda: failed.unwrap(), ".*division by zero")
      >>> asserts.assert_true(failed.is_err)

    """
    def decorator(*args, **kwargs):
        return _JResult.safe(function, *args, **kwargs)
    return decorator


def _error_is(regex, error_obj):
    """
    In python ::

     - re.search("division.*by zero", rval.unwrap_err().args[0])

     OR *larky compatible* ::

     - re.search("division.*by zero", ("%s" % rval._val))
    :param regex: a ``re`` string to pass to re.search
    :param error_obj: the ``Result.Err`` object
    :return: match obj if the regex matches stringified Err object is matched
             else None
    """
    # TODO: assert error_obj.is_err?
    return re.search(regex, ("%s" % error_obj._val))


def of(o):
    return _JResult.of(o)


def try_(func):
    """
    An attempt at
    try_(foo_s_try)\
       .except_(foo_s_Exception)\
       .else_(foo_s_else)\
       .finally_(foo_s_finally)\
       .build()
    """

    _enum = enum.Enum('ExceptionFlowState', [
        'TRY',
        'EXCEPT',
        'ELSE',
        'FINALLY',
        'BUILD'
    ])

    _state = {
        # try => except, finally
        _enum.TRY: [_enum.EXCEPT, _enum.FINALLY],
        # except => except, else, finally, build
        _enum.EXCEPT: [_enum.EXCEPT, _enum.ELSE, _enum.FINALLY, _enum.BUILD],
        # else => finally, build
        _enum.ELSE: [_enum.FINALLY, _enum.BUILD],
        # finally => build
        _enum.FINALLY: [_enum.BUILD],
        # build => // END
        _enum.BUILD: []
    }

    self = larky.mutablestruct(
        _attempt = func,
        _exc = [],
        _else = None,
        _finally = None,
        _current_state=_enum.TRY,
    )

    def _assert_valid_transition(next_state):
        if next_state in _state[self._current_state]:
            return
        # at this point, we have an invalid state transition
        # (wrong try/except/else/finally order!)
        _current_state = _enum.reverse_mapping[self._current_state]
        _valid_transitions = ", ".join([
            "%s => %s" % (_current_state, _enum.reverse_mapping[i])
            for i in _state[self._current_state]
        ])
        fail(("Invalid state transition: %s => %s. " +
              "The try builder was constructed in the wrong order. " +
              "The next valid state transitions allowed are: %s")% (
            _current_state,
            _enum.reverse_mapping[next_state],
            _valid_transitions,
        ))

    def except_(except_handler):
        _assert_valid_transition(_enum.EXCEPT)
        self._current_state = _enum.EXCEPT
        self._exc.append(except_handler)
        return self
    self.except_ = except_

    def else_(else_handler):
        _assert_valid_transition(_enum.ELSE)
        self._current_state = _enum.ELSE
        self._else = else_handler
        return self
    self.else_ = else_

    def finally_(finally_handler):
        _assert_valid_transition(_enum.FINALLY)
        self._current_state = _enum.FINALLY
        self._finally = finally_handler
        return self
    self.finally_ = finally_

    def build(*args, **kwargs):
        _assert_valid_transition(_enum.BUILD)
        self._current_state = _enum.BUILD
        rval = safe(self._attempt)(*args, **kwargs)
        if rval.is_err and self._exc:
            for e in self._exc:
                rval = rval.map_err(e)
        if rval.is_ok and self._else:
            rval = rval.map(self._else)
        if self._finally:
            _finally_returnval = self._finally(rval)
            # -> make this an option when setting up finally?
            # if finally does not return None, set it to rval
            # TODO: is this right?
            if _finally_returnval != None:
                rval = _finally_returnval
        return rval

    self.build = build
    return self



# def _GeneratorContextManager(func, args, kwds):
#     "A base class or mixin that enables context managers to work as decorators."
#     self = larky.mutablestruct(__class__='ContextDecorator')
#
#     def __init__(func, args, kwds):
#         self.gen = func
#         self.func, self.args, self.kwds = func, args, kwds
#         return self
#     self = __init__(func, args, kwds)
#
#     def _recreate_cm():
#         """Return a recreated instance of self.
#         Allows an otherwise one-shot context manager like
#         _GeneratorContextManager to support use as
#         a decorator via implicit recreation.
#         This is a private interface just for _GeneratorContextManager.
#         See issue #11647 for details.
#         """
#         return _GeneratorContextManager(self.func, self.args, self.kwds)
#     self._recreate_cm = _recreate_cm
#
#     def __call__(func):
#         def inner(*args, **kwds):
#             rval = Result.with_(self._recreate_cm, func)
#             return rval.unwrap()
#         return inner
#     self.__call__ = __call__
#
#     def __enter__():
#         return self.gen(*self.args, **self.kwds)
#     self.__enter__ = __enter__
#
#     def __exit__(type, value, traceback):
#         if type is None:
#            return
#
#         return Result.Error(str(type) + ': ' + value)
#     self.__exit__ = __exit__
#     return self
#
#
# def contextmanager(func):
#     """@contextmanager decorator.
#     Typical usage:
#         @contextmanager
#         def some_generator(<arguments>):
#             <setup>
#             try:
#                 yield <value>
#             finally:
#                 <cleanup>
#     This makes this:
#         with some_generator(<arguments>) as <variable>:
#             <body>
#     equivalent to this:
#         <setup>
#         try:
#             <variable> = <value>
#             <body>
#         finally:
#             <cleanup>
#     """
#     def helper(*args, **kwds):
#         return _GeneratorContextManager(func, args, kwds)
#     return helper

# https://github.com/python/cpython/blob/v3.5.10/Lib/test/test_contextlib.py#L17
def with_(ctxmgrs, callback):

    __dict__ = dict(
        error = None,
        result = None
    )

    if not types.is_iterable(ctxmgrs):
        ctxmgrs = [ctxmgrs]

    for i in ctxmgrs:
        i.__enter__()

    result = (
        try_(callback)
        .except_(lambda e: operator.setitem(__dict__, 'error', e))
        .build()
    )

    for i in reversed(ctxmgrs):
        i.__exit__(__dict__['error'])

    return result


Result = larky.struct(
    Ok=Ok,
    Error=Error,
    Err=Err,  # alias to Error
    # below are non-Result extensions.
    safe=safe,
    of=of,
    error_is=_error_is,
    try_=try_,
    with_=with_
)