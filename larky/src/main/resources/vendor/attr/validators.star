# Ported from attrs 26.1.0 (7bfc49e9b22d5ba25b6e429524c3d49fee27cb36): src/attr/validators.py. See larky/src/main/resources/VENDORED.md.
#
# The MIT License (MIT)
#
# Copyright (c) 2015 Hynek Schlawack and the attrs contributors
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
"""
Commonly useful validators.

Each validator is a callable ``validator(inst, attr, value)`` that fails with
a message prefixed by the Python exception attrs raises (``TypeError: ...``,
``ValueError: ...``, ``NotCallableError: ...``); Larky has no exception
objects, so the extra exception arguments (the attribute, the expected type,
the value) are not available.

Not ported: ``not_`` (its *exc_types* select Python exception classes, which
Larky does not have) and the ``disabled()`` context manager (Starlark has no
``with``; use `set_disabled`).
"""
load("@stdlib//re", re="re")
load("@stdlib//larky", larky="larky")
load("@stdlib//jattrs", _jattrs="jattrs")
load("@vendor//attr/_make", _make_and="and_")

and_ = _make_and


def _is_list_or_tuple(x):
    return type(x) == "list" or type(x) == "tuple"


def set_disabled(disabled):
    """
    Globally disable or enable running validators.

    By default, they are run.

    Args:
        disabled (bool): If `True`, disable running all validators.

    .. warning::

        This function is not thread-safe!

    Larky: the setting applies to the current evaluation only.
    """
    _jattrs.set_run_validators(not disabled)


def get_disabled():
    """
    Return a bool indicating whether validators are currently disabled or not.

    Returns:
        bool:`True` if validators are currently disabled.
    """
    return not _jattrs.get_run_validators()


def instance_of(type):
    """
    A validator that raises a `TypeError` if the initializer is called with a
    wrong type for this particular attribute (checks are performed using
    `isinstance` therefore it's also valid to pass a tuple of types).

    Args:
        type (type | tuple[type]): The type to check for.

    Raises:
        TypeError:
            With a human readable error message, the attribute (of type
            `attrs.Attribute`), the expected type, and the value it got.

    Larky: *type* is a class (from `types.new_class` or an attrs class), one
    of the builtin types ``bool``, ``int``, ``float``, ``str``, ``bytes``,
    ``bytearray``, ``list``, ``tuple``, ``dict`` and ``set``, or a tuple of
    these.
    """
    return _jattrs.instance_of(type)


def _is_pattern(regex):
    return type(regex) != "string" and hasattr(regex, "pattern") and hasattr(regex, "fullmatch")


def matches_re(regex, flags=0, func=None):
    r"""
    A validator that raises `ValueError` if the initializer is called with a
    string that doesn't match *regex*.

    Args:
        regex (str, re.Pattern):
            A regex string or precompiled pattern to match against

        flags (int):
            Flags that will be passed to the underlying re function (default 0)

        func (typing.Callable):
            Which underlying `re` function to call. Valid options are
            `re.fullmatch`, `re.search`, and `re.match`; the default `None`
            means `re.fullmatch`. For performance reasons, the pattern is
            always precompiled using `re.compile`.
    """
    if func != None and func != re.fullmatch and func != re.search and func != re.match:
        fail("ValueError: 'func' must be one of None, fullmatch, match, search.")

    if _is_pattern(regex):
        if flags:
            fail("TypeError: 'flags' can only be used with a string pattern; pass flags to re.compile() instead")
        pattern = regex
    else:
        pattern = re.compile(regex, flags)

    if func == re.match:
        match_func = pattern.match
    elif func == re.search:
        match_func = pattern.search
    else:
        match_func = pattern.fullmatch

    return _jattrs.matches_re(pattern, match_func)


def optional(validator):
    """
    A validator that makes an attribute optional.  An optional attribute is one
    which can be set to `None` in addition to satisfying the requirements of
    the sub-validator.

    Args:
        validator
            (typing.Callable | tuple[typing.Callable] | list[typing.Callable]):
            A validator (or validators) that is used for non-`None` values.
    """
    return _jattrs.optional(validator)


def in_(options):
    """
    A validator that raises a `ValueError` if the initializer is called with a
    value that does not belong in the *options* provided.

    The check is performed using ``value in options``, so *options* has to
    support that operation.

    To keep the validator hashable, dicts, lists, and sets are transparently
    transformed into a `tuple`.

    Args:
        options: Allowed options.

    Raises:
        ValueError:
            With a human readable error message, the attribute (of type
            `attrs.Attribute`), the expected options, and the value it got.
    """
    repr_options = options
    if type(options) in ("list", "dict", "set"):
        options = tuple(options)

    return _jattrs.in_(options, repr_options)


def is_callable():
    """
    A validator that raises a `attrs.exceptions.NotCallableError` if the
    initializer is called with a value for this particular attribute that is
    not callable.

    Raises:
        attrs.exceptions.NotCallableError:
            With a human readable error message containing the attribute
            (`attrs.Attribute`) name, and the value it got.
    """
    return _jattrs.is_callable()


def deep_iterable(member_validator, iterable_validator=None):
    """
    A validator that performs deep validation of an iterable.

    Args:
        member_validator: Validator(s) to apply to iterable members.

        iterable_validator:
            Validator(s) to apply to iterable itself (optional).

    Raises
        TypeError: if any sub-validators fail
    """
    if _is_list_or_tuple(member_validator):
        member_validator = and_(*member_validator)
    if _is_list_or_tuple(iterable_validator):
        iterable_validator = and_(*iterable_validator)
    return _jattrs.deep_iterable(member_validator, iterable_validator)


def deep_mapping(key_validator=None, value_validator=None, mapping_validator=None):
    """
    A validator that performs deep validation of a dictionary.

    All validators are optional, but at least one of *key_validator* or
    *value_validator* must be provided.

    Args:
        key_validator: Validator(s) to apply to dictionary keys.

        value_validator: Validator(s) to apply to dictionary values.

        mapping_validator:
            Validator(s) to apply to top-level mapping attribute.

    Raises:
        TypeError: If any sub-validator fails on validation.

        ValueError:
            If neither *key_validator* nor *value_validator* is provided on
            instantiation.
    """
    if key_validator == None and value_validator == None:
        fail("ValueError: At least one of key_validator or value_validator must be provided")

    if _is_list_or_tuple(key_validator):
        key_validator = and_(*key_validator)
    if _is_list_or_tuple(value_validator):
        value_validator = and_(*value_validator)
    if _is_list_or_tuple(mapping_validator):
        mapping_validator = and_(*mapping_validator)

    return _jattrs.deep_mapping(key_validator, value_validator, mapping_validator)


def lt(val):
    """
    A validator that raises `ValueError` if the initializer is called with a
    number larger or equal to *val*.

    The validator uses `operator.lt` to compare the values.

    Args:
        val: Exclusive upper bound for values.
    """
    return _jattrs.number(val, "<")


def le(val):
    """
    A validator that raises `ValueError` if the initializer is called with a
    number greater than *val*.

    The validator uses `operator.le` to compare the values.

    Args:
        val: Inclusive upper bound for values.
    """
    return _jattrs.number(val, "<=")


def ge(val):
    """
    A validator that raises `ValueError` if the initializer is called with a
    number smaller than *val*.

    The validator uses `operator.ge` to compare the values.

    Args:
        val: Inclusive lower bound for values
    """
    return _jattrs.number(val, ">=")


def gt(val):
    """
    A validator that raises `ValueError` if the initializer is called with a
    number smaller or equal to *val*.

    The validator uses `operator.gt` to compare the values.

    Args:
       val: Exclusive lower bound for values
    """
    return _jattrs.number(val, ">")


def max_len(length):
    """
    A validator that raises `ValueError` if the initializer is called
    with a string or iterable that is longer than *length*.

    Args:
        length (int): Maximum length of the string or iterable
    """
    return _jattrs.max_len(length)


def min_len(length):
    """
    A validator that raises `ValueError` if the initializer is called
    with a string or iterable that is shorter than *length*.

    Args:
        length (int): Minimum length of the string or iterable
    """
    return _jattrs.min_len(length)


def or_(*validators):
    """
    A validator that composes multiple validators into one.

    When called on a value, it runs all wrapped validators until one of them is
    satisfied.

    Args:
        validators (~collections.abc.Iterable[typing.Callable]):
            Arbitrary number of validators.

    Raises:
        ValueError:
            If no validator is satisfied. Raised with a human-readable error
            message listing all the wrapped validators and the value that
            failed all of them.
    """
    return _jattrs.or_(*validators)


validators = larky.struct(
    __name__="validators",
    and_=and_,
    deep_iterable=deep_iterable,
    deep_mapping=deep_mapping,
    ge=ge,
    get_disabled=get_disabled,
    gt=gt,
    in_=in_,
    instance_of=instance_of,
    is_callable=is_callable,
    le=le,
    lt=lt,
    matches_re=matches_re,
    max_len=max_len,
    min_len=min_len,
    optional=optional,
    or_=or_,
    set_disabled=set_disabled,
)
