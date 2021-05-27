load("@stdlib//larky", "larky")
load("@stdlib//types", types="types")
load("@stdlib//jresult", _JResult="jresult")
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
#             >>> Ok(2).flatmap(sq).flatmap(sq)
#             Ok(16)
#             >>> Ok(2).flatmap(sq).flatmap(err)
#             Err(4)
#             >>> Ok(2).flatmap(err).flatmap(sq)
#             Err(2)
#             >>> Err(3).flatmap(sq).flatmap(sq)
#             Err(3)
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


def of(o):
    return _JResult.of(o)


Result = larky.struct(
    Ok=Ok,
    Error=Error,
    safe=safe,
    of=of,
)