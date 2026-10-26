# Ported from attrs 26.1.0 (7bfc49e9b22d5ba25b6e429524c3d49fee27cb36): tests/test_validators.py. See larky/src/main/resources/VENDORED.md.
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
Tests for `attr.validators`.

Ported with py2star and adapted: classes are built with `attr.s` on
`types.new_class` (Starlark has no class statement), pytest parametrization
is a loop, and `pytest.raises(...)` checks the failure message, which carries
Larky's repr of values (strings in double quotes). Not ported: the
``disabled()`` context manager, ``not_`` and ``_subclass_of`` (not in the
Larky port), and the hashing and ``filters`` tests.
"""
load("@stdlib//re", re="re")
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//option/result", safe="safe")
load("@vendor//attr", attr="attr")
load("@vendor//attr/validators", validator_module="validators")

and_ = validator_module.and_
deep_iterable = validator_module.deep_iterable
deep_mapping = validator_module.deep_mapping
ge = validator_module.ge
gt = validator_module.gt
in_ = validator_module.in_
instance_of = validator_module.instance_of
is_callable = validator_module.is_callable
le = validator_module.le
lt = validator_module.lt
matches_re = validator_module.matches_re
max_len = validator_module.max_len
min_len = validator_module.min_len
optional = validator_module.optional
or_ = validator_module.or_
fields = attr.fields


def simple_attr(
        name,
        default=attr.NOTHING,
        validator=None,
        repr=True,
        eq=True,
        hash=None,
        init=True,
        converter=None,
        kw_only=False,
        inherited=False):
    """
    Return an attribute with a name and no other bells and whistles.
    """
    return attr.Attribute(
        name=name,
        default=default,
        validator=validator,
        repr=repr,
        cmp=None,
        eq=eq,
        hash=hash,
        init=init,
        converter=converter,
        kw_only=kw_only,
        inherited=inherited,
        alias=name.lstrip("_"),
        alias_is_default=True,
    )


def _attrs_class(name, body, **kwargs):
    """`@attr.s(**kwargs) class name: body`."""
    return attr.s(**kwargs)(types.new_class(name, (), None, lambda ns: ns.update(body)))


def _error(f):
    """The message of the failure of `f()`; fails if `f()` succeeds."""
    r = safe(f)()
    asserts.assert_true(r.is_err)
    return "%s" % (r._val,)


def _in_all(name):
    # Larky modules have no __all__: the validator is exported by the module.
    asserts.assert_true(hasattr(validator_module, name))


def TestDisableValidators_test_default():
    """
    Run validators by default.
    """
    asserts.assert_that(validator_module.get_disabled()).is_equal_to(False)
    asserts.assert_that(attr.get_run_validators()).is_equal_to(True)


def TestDisableValidators_test_set_validators_disabled():
    """
    Sets `_run_validators`.
    """
    for value, expected in [(True, False), (False, True)]:
        validator_module.set_disabled(value)
        asserts.assert_that(attr.get_run_validators()).is_equal_to(expected)
    validator_module.set_disabled(False)


def TestDisableValidators_test_disabled():
    """
    Returns `_run_validators`.
    """
    for value, expected in [(True, False), (False, True)]:
        attr.set_run_validators(value)
        asserts.assert_that(validator_module.get_disabled()).is_equal_to(expected)
    attr.set_run_validators(True)


def TestDisableValidators_test_disabled_skips_validation():
    """
    Larky: with validators disabled, __init__ does not run them.
    """
    C = attr.make_class("C", {"x": attr.ib(validator=instance_of(int))})
    validator_module.set_disabled(True)
    asserts.assert_that(C("1").x).is_equal_to("1")
    validator_module.set_disabled(False)
    asserts.assert_fails(lambda: C("1"), "TypeError: 'x' must be")


def TestInstanceOf_test_in_all():
    """
    Verify that this validator is in ``__all__``.
    """
    _in_all("instance_of")


def TestInstanceOf_test_success():
    """
    Nothing happens if types match.
    """
    v = instance_of(int)
    v(None, simple_attr("test"), 42)


def TestInstanceOf_test_subclass():
    """
    Subclasses are accepted too.
    """
    v = instance_of(int)
    # yep, bools are a subclass of int :(
    v(None, simple_attr("test"), True)


def TestInstanceOf_test_fail():
    """
    Raises `TypeError` on wrong types.
    """
    v = instance_of(int)
    a = simple_attr("test")
    asserts.assert_that(_error(lambda: v(None, a, "42"))).is_equal_to(
        "TypeError: 'test' must be <class 'int'> (got \"42\" that is a <class 'str'>).",
    )


def TestInstanceOf_test_repr():
    """
    Returned validator has a useful `__repr__`.
    """
    v = instance_of(int)
    asserts.assert_that(repr(v)).is_equal_to("<instance_of validator for type <class 'int'>>")


def TestInstanceOf_test_classes_and_tuples():
    """
    Larky: classes (and their subclasses) and tuples of types are accepted.
    """
    A = attr.make_class("A", [])
    B = attr.make_class("B", [], bases=(A,))
    v = instance_of((str, A))
    a = simple_attr("test")
    v(None, a, "s")
    v(None, a, A())
    v(None, a, B())
    asserts.assert_that(_error(lambda: v(None, a, 1))).is_equal_to(
        "TypeError: 'test' must be (<class 'str'>, <class 'A'>) (got 1 that is a <class 'int'>).",
    )
    asserts.assert_that(_error(lambda: instance_of(B)(None, a, A()))).is_equal_to(
        "TypeError: 'test' must be <class 'B'> (got A() that is a <class 'A'>).",
    )


def TestMatchesRe_test_in_all():
    """
    validator is in ``__all__``.
    """
    _in_all("matches_re")


def TestMatchesRe_test_match():
    """
    Silent on matches, raises ValueError on mismatches.
    """
    ReTester = _attrs_class("ReTester", {"str_match": attr.ib(validator=matches_re("a|ab"))})

    ReTester("ab")  # shouldn't raise exceptions
    # attrs: TypeError from re. Larky's re rejects the int argument.
    asserts.assert_fails(lambda: ReTester(1), "want 'string or bytes'")
    asserts.assert_that(_error(lambda: ReTester("1"))).is_equal_to(
        "ValueError: 'str_match' must match regex \"a|ab\" (\"1\" doesn't)",
    )
    asserts.assert_fails(lambda: ReTester("a1"), "ValueError: 'str_match' must match regex")


def TestMatchesRe_test_flags():
    """
    Flags are propagated to the match function.
    """
    MatchTester = _attrs_class("MatchTester", {"val": attr.ib(validator=matches_re("a", re.IGNORECASE, re.match))})

    MatchTester("A1")  # test flags and using re.match


def TestMatchesRe_test_precompiled_pattern():
    """
    Pre-compiled patterns are accepted.
    """
    pattern = re.compile("a")

    RePatternTester = _attrs_class("RePatternTester", {"val": attr.ib(validator=matches_re(pattern))})

    RePatternTester("a")


def TestMatchesRe_test_precompiled_pattern_no_flags():
    """
    A pre-compiled pattern cannot be combined with a 'flags' argument.
    """
    pattern = re.compile("")

    asserts.assert_fails(
        lambda: matches_re(pattern, flags=re.IGNORECASE),
        "TypeError: 'flags' can only be used with a string pattern",
    )


def TestMatchesRe_test_different_func():
    """
    Changing the match functions works.
    """
    SearchTester = _attrs_class("SearchTester", {"val": attr.ib(validator=matches_re("a", 0, re.search))})

    SearchTester("bab")  # re.search will match


def TestMatchesRe_test_catches_invalid_func():
    """
    Invalid match functions are caught.
    """
    asserts.assert_that(_error(lambda: matches_re("a", 0, lambda: None))).is_equal_to(
        "ValueError: 'func' must be one of None, fullmatch, match, search.",
    )


def TestMatchesRe_test_accepts_all_valid_func():
    """
    Every valid match function is accepted.
    """
    for func in [None, re.fullmatch, re.match, re.search]:
        matches_re("a", func=func)


def TestMatchesRe_test_repr():
    """
    __repr__ is meaningful.
    """
    asserts.assert_true(repr(matches_re("a")).startswith("<matches_re validator for pattern"))


def always_pass(_, __, ___):
    """
    Toy validator that always passes.
    """


def always_fail(_, __, ___):
    """
    Toy validator that always fails.
    """
    fail("ZeroDivisionError: division by zero")


def TestAnd_test_in_all():
    """
    Verify that this validator is in ``__all__``.
    """
    _in_all("and_")


def TestAnd_test_success():
    """
    Succeeds if all wrapped validators succeed.
    """
    v = and_(instance_of(int), always_pass)

    v(None, simple_attr("test"), 42)


def TestAnd_test_fail():
    """
    Fails if any wrapped validator fails.
    """
    v = and_(instance_of(int), always_fail)

    asserts.assert_fails(lambda: v(None, simple_attr("test"), 42), "ZeroDivisionError")


def TestAnd_test_sugar():
    """
    `and_(v1, v2, v3)` and `[v1, v2, v3]` are equivalent.
    """
    C = _attrs_class("C", {
        "a1": attr.ib("a1", validator=and_(instance_of(int))),
        "a2": attr.ib("a2", validator=[instance_of(int)]),
    })

    asserts.assert_that(C.__attrs_attrs__[0].validator).is_equal_to(C.__attrs_attrs__[1].validator)


_OPTIONAL_VALIDATORS = [
    instance_of(int),
    [always_pass, instance_of(int)],
    (always_pass, instance_of(int)),
]


def TestOptional_test_in_all():
    """
    Verify that this validator is in ``__all__``.
    """
    _in_all("optional")


def TestOptional_test_success():
    """
    Nothing happens if validator succeeds.
    """
    for validator in _OPTIONAL_VALIDATORS:
        v = optional(validator)
        v(None, simple_attr("test"), 42)


def TestOptional_test_success_with_none():
    """
    Nothing happens if None.
    """
    for validator in _OPTIONAL_VALIDATORS:
        v = optional(validator)
        v(None, simple_attr("test"), None)


def TestOptional_test_fail():
    """
    Raises `TypeError` on wrong types.
    """
    for validator in _OPTIONAL_VALIDATORS:
        v = optional(validator)
        a = simple_attr("test")
        asserts.assert_that(_error(lambda: v(None, a, "42"))).is_equal_to(
            "TypeError: 'test' must be <class 'int'> (got \"42\" that is a <class 'str'>).",
        )


def TestOptional_test_repr():
    """
    Returned validator has a useful `__repr__`.
    """
    for validator in _OPTIONAL_VALIDATORS:
        v = optional(validator)

        if type(validator) == "list":
            repr_s = (
                "<optional validator for _AndValidator(_validators=[{}, ".format(repr(always_pass))
                + "<instance_of validator for type <class 'int'>>]) or None>"
            )
        elif type(validator) == "tuple":
            repr_s = (
                "<optional validator for _AndValidator(_validators=({}, ".format(repr(always_pass))
                + "<instance_of validator for type <class 'int'>>)) or None>"
            )
        else:
            repr_s = (
                "<optional validator for <instance_of validator for type "
                + "<class 'int'>> or None>"
            )

        asserts.assert_that(repr(v)).is_equal_to(repr_s)


def TestIn__test_in_all():
    """
    Verify that this validator is in ``__all__``.
    """
    _in_all("in_")


def TestIn__test_success_with_value():
    """
    If the value is in our options, nothing happens.
    """
    v = in_([1, 2, 3])
    a = simple_attr("test")

    v(1, a, 3)


def TestIn__test_fail():
    """
    Raise ValueError if the value is outside our options.
    """
    v = in_([1, 2, 3])
    a = simple_attr("test")

    asserts.assert_that(_error(lambda: v(None, a, None))).is_equal_to(
        "ValueError: 'test' must be in [1, 2, 3] (got None)",
    )


def TestIn__test_fail_with_string():
    """
    Raise ValueError if the value is outside our options when the
    options are specified as a string and the value is not a string.
    """
    v = in_("abc")
    a = simple_attr("test")
    asserts.assert_that(_error(lambda: v(None, a, None))).is_equal_to(
        "ValueError: 'test' must be in \"abc\" (got None)",
    )


def TestIn__test_repr():
    """
    Returned validator has a useful `__repr__`.
    """
    v = in_([3, 4, 5])
    asserts.assert_that(repr(v)).is_equal_to("<in_ validator with options [3, 4, 5]>")


_MEMBER_VALIDATORS = (
    instance_of(int),
    [always_pass, instance_of(int)],
    (always_pass, instance_of(int)),
)


def TestDeepIterable_test_in_all():
    """
    Verify that this validator is in ``__all__``.
    """
    _in_all("deep_iterable")


def TestDeepIterable_test_success_member_only():
    """
    If the member validator succeeds and the iterable validator is not set,
    nothing happens.
    """
    for member_validator in _MEMBER_VALIDATORS:
        v = deep_iterable(member_validator)
        a = simple_attr("test")
        v(None, a, [42])


def TestDeepIterable_test_success_member_and_iterable():
    """
    If both the member and iterable validators succeed, nothing happens.
    """
    for member_validator in _MEMBER_VALIDATORS:
        iterable_validator = instance_of(list)
        v = deep_iterable(member_validator, iterable_validator)
        a = simple_attr("test")
        v(None, a, [42])


def TestDeepIterable_test_noncallable_validators():
    """
    Raise `TypeError` if any validators are not callable.
    """
    cases = [
        (instance_of(int), 42),
        (42, instance_of(list)),
        (42, 42),
        (42, None),
        ([instance_of(int), 42], 42),
        ([42, instance_of(int)], 42),
    ]
    for member_validator, iterable_validator in cases:
        message = _error(lambda: deep_iterable(member_validator, iterable_validator))
        asserts.assert_true(message.startswith("NotCallableError: "))
        asserts.assert_true("must be callable (got 42 that is a <class 'int'>)." in message)


def TestDeepIterable_test_fail_invalid_member():
    """
    Raise member validator error if an invalid member is found.
    """
    for member_validator in _MEMBER_VALIDATORS:
        v = deep_iterable(member_validator)
        a = simple_attr("test")
        asserts.assert_fails(lambda: v(None, a, [42, "42"]), "TypeError: ")


def TestDeepIterable_test_fail_invalid_iterable():
    """
    Raise iterable validator error if an invalid iterable is found.
    """
    member_validator = instance_of(int)
    iterable_validator = instance_of(tuple)
    v = deep_iterable(member_validator, iterable_validator)
    a = simple_attr("test")
    asserts.assert_fails(lambda: v(None, a, [42]), "TypeError: 'test' must be <class 'tuple'>")


def TestDeepIterable_test_fail_invalid_member_and_iterable():
    """
    Raise iterable validator error if both the iterable
    and a member are invalid.
    """
    for member_validator in _MEMBER_VALIDATORS:
        iterable_validator = instance_of(tuple)
        v = deep_iterable(member_validator, iterable_validator)
        a = simple_attr("test")
        asserts.assert_fails(lambda: v(None, a, [42, "42"]), "TypeError: 'test' must be <class 'tuple'>")


def TestDeepIterable_test_repr_member_only():
    """
    Returned validator has a useful `__repr__`
    when only member validator is set.
    """
    member_validator = instance_of(int)
    member_repr = "<instance_of validator for type <class 'int'>>"
    v = deep_iterable(member_validator)
    expected_repr = "<deep_iterable validator for iterables of {}>".format(member_repr)
    asserts.assert_that(repr(v)).is_equal_to(expected_repr)


def TestDeepIterable_test_repr_member_only_sequence():
    """
    Returned validator has a useful `__repr__`
    when only member validator is set and the member validator is a list of
    validators
    """
    member_validator = [always_pass, instance_of(int)]
    member_repr = (
        "_AndValidator(_validators=({}, ".format(repr(always_pass))
        + "<instance_of validator for type <class 'int'>>))"
    )
    v = deep_iterable(member_validator)
    expected_repr = "<deep_iterable validator for iterables of {}>".format(member_repr)
    asserts.assert_that(repr(v)).is_equal_to(expected_repr)


def TestDeepIterable_test_repr_member_and_iterable():
    """
    Returned validator has a useful `__repr__` when both member
    and iterable validators are set.
    """
    member_validator = instance_of(int)
    member_repr = "<instance_of validator for type <class 'int'>>"
    iterable_validator = instance_of(list)
    iterable_repr = "<instance_of validator for type <class 'list'>>"
    v = deep_iterable(member_validator, iterable_validator)
    expected_repr = (
        "<deep_iterable validator for"
        + " {} iterables of {}>".format(iterable_repr, member_repr)
    )
    asserts.assert_that(repr(v)).is_equal_to(expected_repr)


def TestDeepIterable_test_repr_sequence_member_and_iterable():
    """
    Returned validator has a useful `__repr__` when both member
    and iterable validators are set and the member validator is a list of
    validators
    """
    member_validator = [always_pass, instance_of(int)]
    member_repr = (
        "_AndValidator(_validators=({}, ".format(repr(always_pass))
        + "<instance_of validator for type <class 'int'>>))"
    )
    iterable_validator = instance_of(list)
    iterable_repr = "<instance_of validator for type <class 'list'>>"
    v = deep_iterable(member_validator, iterable_validator)
    expected_repr = (
        "<deep_iterable validator for"
        + " {} iterables of {}>".format(iterable_repr, member_repr)
    )

    asserts.assert_that(repr(v)).is_equal_to(expected_repr)


def TestDeepIterable_test_validators_iterables():
    """
    If iterables are passed as validators, they are combined with and_.
    """
    for conv in [list, tuple]:
        member_validator = (instance_of(int),)
        iterable_validator = (instance_of(list), min_len(1))

        v = deep_iterable(conv(member_validator), conv(iterable_validator))

        asserts.assert_that(v.member_validator).is_equal_to(and_(*member_validator))
        asserts.assert_that(v.iterable_validator).is_equal_to(and_(*iterable_validator))


def TestDeepIterable_test_nested():
    """
    Larky: validators of the same kind nest (Starlark forbids recursive calls,
    so this checks they are not implemented as one recursive Starlark function).
    """
    v = deep_iterable(deep_iterable(optional(optional(instance_of(int)))))
    a = simple_attr("test")
    v(None, a, [[1, None], [], [2]])
    asserts.assert_that(_error(lambda: v(None, a, [[1], ["x"]]))).is_equal_to(
        "TypeError: 'test' must be <class 'int'> (got \"x\" that is a <class 'str'>).",
    )


def TestDeepMapping_test_in_all():
    """
    Verify that this validator is in ``__all__``.
    """
    _in_all("deep_mapping")


def TestDeepMapping_test_success():
    """
    If both the key and value validators succeed, nothing happens.
    """
    key_validator = instance_of(str)
    value_validator = instance_of(int)
    v = deep_mapping(key_validator, value_validator)
    a = simple_attr("test")
    v(None, a, {"a": 6, "b": 7})


def TestDeepMapping_test_noncallable_validators():
    """
    Raise `TypeError` if any validators are not callable.
    """
    cases = [
        (42, instance_of(int), None),
        (instance_of(str), 42, None),
        (instance_of(str), instance_of(int), 42),
        (42, 42, None),
        (42, 42, 42),
        (42, None, None),
        (None, 42, None),
    ]
    for key_validator, value_validator, mapping_validator in cases:
        message = _error(lambda: deep_mapping(key_validator, value_validator, mapping_validator))
        asserts.assert_true(message.startswith("NotCallableError: "))
        asserts.assert_true("must be callable (got 42 that is a <class 'int'>)." in message)


def TestDeepMapping_test_fail_invalid_mapping():
    """
    Raise `TypeError` if mapping validator fails.
    """
    key_validator = instance_of(str)
    value_validator = instance_of(int)
    mapping_validator = instance_of(dict)
    v = deep_mapping(key_validator, value_validator, mapping_validator)
    a = simple_attr("test")
    asserts.assert_fails(lambda: v(None, a, None), "TypeError: 'test' must be <class 'dict'>")


def TestDeepMapping_test_fail_invalid_key():
    """
    Raise key validator error if an invalid key is found.
    """
    key_validator = instance_of(str)
    value_validator = instance_of(int)
    v = deep_mapping(key_validator, value_validator)
    a = simple_attr("test")
    asserts.assert_fails(lambda: v(None, a, {"a": 6, 42: 7}), "TypeError: 'test' must be <class 'str'>")


def TestDeepMapping_test_fail_invalid_member():
    """
    Raise key validator error if an invalid member value is found.
    """
    key_validator = instance_of(str)
    value_validator = instance_of(int)
    v = deep_mapping(key_validator, value_validator)
    a = simple_attr("test")
    asserts.assert_fails(lambda: v(None, a, {"a": "6", "b": 7}), "TypeError: 'test' must be <class 'int'>")


def TestDeepMapping_test_repr():
    """
    Returned validator has a useful `__repr__`.
    """
    key_validator = instance_of(str)
    key_repr = "<instance_of validator for type <class 'str'>>"
    value_validator = instance_of(int)
    value_repr = "<instance_of validator for type <class 'int'>>"
    v = deep_mapping(key_validator, value_validator)
    expected_repr = (
        "<deep_mapping validator for objects mapping "
        + "{} to {}>".format(key_repr, value_repr)
    )

    asserts.assert_that(repr(v)).is_equal_to(expected_repr)


def TestDeepMapping_test_error_neither_validator_provided():
    """
    Raise ValueError if neither key_validator nor value_validator is
    provided.
    """
    asserts.assert_that(_error(lambda: deep_mapping())).is_equal_to(
        "ValueError: At least one of key_validator or value_validator must be provided",
    )


def TestDeepMapping_test_key_validator_can_be_none():
    """
    The key validator can be None.
    """
    v = deep_mapping(value_validator=instance_of(int))
    a = simple_attr("test")

    v(None, a, {"a": 6, "b": 7})


def TestDeepMapping_test_value_validator_can_be_none():
    """
    The value validator can be None.
    """
    v = deep_mapping(key_validator=instance_of(str))
    a = simple_attr("test")

    v(None, a, {"a": 6, "b": 7})


def TestDeepMapping_test_validators_iterables():
    """
    If iterables are passed as validators, they are combined with and_.
    """
    for conv in [list, tuple]:
        key_validator = (instance_of(str), min_len(2))
        value_validator = (instance_of(int), ge(10))
        mapping_validator = (instance_of(dict), max_len(2))

        v = deep_mapping(
            conv(key_validator),
            conv(value_validator),
            conv(mapping_validator),
        )

        asserts.assert_that(v.key_validator).is_equal_to(and_(*key_validator))
        asserts.assert_that(v.value_validator).is_equal_to(and_(*value_validator))
        asserts.assert_that(v.mapping_validator).is_equal_to(and_(*mapping_validator))


def TestIsCallable_test_in_all():
    """
    Verify that this validator is in ``__all__``.
    """
    _in_all("is_callable")


def TestIsCallable_test_success():
    """
    If the value is callable, nothing happens.
    """
    v = is_callable()
    a = simple_attr("test")
    v(None, a, len)
    v(None, a, always_pass)
    v(None, a, instance_of(int))


def TestIsCallable_test_fail():
    """
    Raise TypeError if the value is not callable.
    """
    v = is_callable()
    a = simple_attr("test")
    asserts.assert_that(_error(lambda: v(None, a, None))).is_equal_to(
        "NotCallableError: 'test' must be callable (got None that is a <class 'NoneType'>).",
    )


def TestIsCallable_test_repr():
    """
    Returned validator has a useful `__repr__`.
    """
    v = is_callable()
    asserts.assert_that(repr(v)).is_equal_to("<is_callable validator>")


BOUND = 4


def TestLtLeGeGt_test_in_all():
    """
    validator is in ``__all__``.
    """
    for name in ["lt", "le", "ge", "gt"]:
        _in_all(name)


def TestLtLeGeGt_test_retrieve_bound():
    """
    The configured bound for the comparison can be extracted from the
    Attribute.
    """
    for v in [lt, le, ge, gt]:
        Tester = _attrs_class("Tester", {"value": attr.ib(validator=v(BOUND))})

        asserts.assert_that(fields(Tester).value.validator.bound).is_equal_to(BOUND)


def TestLtLeGeGt_test_check_valid():
    """Silent if value {op} bound."""
    cases = [
        (lt, 3),
        (le, 3),
        (le, 4),
        (ge, 4),
        (ge, 5),
        (gt, 5),
    ]
    for v, value in cases:
        Tester = _attrs_class("Tester", {"value": attr.ib(validator=v(BOUND))})

        Tester(value)  # shouldn't raise exceptions


def TestLtLeGeGt_test_check_invalid():
    """Raise ValueError if value {op} bound."""
    cases = [
        (lt, 4, "<"),
        (le, 5, "<="),
        (ge, 3, ">="),
        (gt, 4, ">"),
    ]
    for v, value, op in cases:
        Tester = _attrs_class("Tester", {"value": attr.ib(validator=v(BOUND))})

        asserts.assert_that(_error(lambda: Tester(value))).is_equal_to(
            "ValueError: 'value' must be %s %d: %d" % (op, BOUND, value),
        )


def TestLtLeGeGt_test_repr():
    """
    __repr__ is meaningful.
    """
    for v in [lt, le, ge, gt]:
        nv = v(23)
        asserts.assert_that(repr(nv)).is_equal_to("<Validator for x {} {}>".format(nv.compare_op, 23))


MAX_LENGTH = 4


def TestMaxLen_test_in_all():
    """
    validator is in ``__all__``.
    """
    _in_all("max_len")


def TestMaxLen_test_retrieve_max_len():
    """
    The configured max. length can be extracted from the Attribute
    """
    Tester = _attrs_class("Tester", {"value": attr.ib(validator=max_len(MAX_LENGTH))})

    asserts.assert_that(fields(Tester).value.validator.max_length).is_equal_to(MAX_LENGTH)


def TestMaxLen_test_check_valid():
    """
    Silent if len(value) <= max_len.
    Values can be strings and other iterables.
    """
    for value in ["", "foo", "spam", [], list(range(MAX_LENGTH)), {"spam": 3, "eggs": 4}]:
        Tester = _attrs_class("Tester", {"value": attr.ib(validator=max_len(MAX_LENGTH))})

        Tester(value)  # shouldn't raise exceptions


def TestMaxLen_test_check_invalid():
    """
    Raise ValueError if len(value) > max_len.
    """
    for value in ["bacon", list(range(6))]:
        Tester = _attrs_class("Tester", {"value": attr.ib(validator=max_len(MAX_LENGTH))})

        asserts.assert_that(_error(lambda: Tester(value))).is_equal_to(
            "ValueError: Length of 'value' must be <= 4: %d" % len(value),
        )


def TestMaxLen_test_repr():
    """
    __repr__ is meaningful.
    """
    asserts.assert_that(repr(max_len(23))).is_equal_to("<max_len validator for 23>")


MIN_LENGTH = 2


def TestMinLen_test_in_all():
    """
    validator is in ``__all__``.
    """
    _in_all("min_len")


def TestMinLen_test_retrieve_min_len():
    """
    The configured min. length can be extracted from the Attribute
    """
    Tester = _attrs_class("Tester", {"value": attr.ib(validator=min_len(MIN_LENGTH))})

    asserts.assert_that(fields(Tester).value.validator.min_length).is_equal_to(MIN_LENGTH)


def TestMinLen_test_check_valid():
    """
    Silent if len(value) => min_len.
    Values can be strings and other iterables.
    """
    for value in ["foo", "spam", list(range(MIN_LENGTH)), {"spam": 3, "eggs": 4}]:
        Tester = _attrs_class("Tester", {"value": attr.ib(validator=min_len(MIN_LENGTH))})

        Tester(value)  # shouldn't raise exceptions


def TestMinLen_test_check_invalid():
    """
    Raise ValueError if len(value) < min_len.
    """
    for value in ["", list(range(1))]:
        Tester = _attrs_class("Tester", {"value": attr.ib(validator=min_len(MIN_LENGTH))})

        asserts.assert_that(_error(lambda: Tester(value))).is_equal_to(
            "ValueError: Length of 'value' must be >= 2: %d" % len(value),
        )


def TestMinLen_test_repr():
    """
    __repr__ is meaningful.
    """
    asserts.assert_that(repr(min_len(23))).is_equal_to("<min_len validator for 23>")


def TestOr_test_in_all():
    """
    Verify that this validator is in ``__all__``.
    """
    _in_all("or_")


def TestOr_test_success():
    """
    Succeeds if at least one of wrapped validators succeed.
    """
    v = or_(instance_of(str), always_pass)

    v(None, simple_attr("test"), 42)


def TestOr_test_fail():
    """
    Fails if all wrapped validators fail.
    """
    v = or_(instance_of(str), always_fail)

    asserts.assert_fails(lambda: v(None, simple_attr("test"), 42), "ValueError: None of .* satisfied for value 42")


def TestOr_test_repr():
    """
    Returned validator has a useful `__repr__`.
    """
    v = or_(instance_of(int), instance_of(str))
    asserts.assert_that(repr(v)).is_equal_to(
        "<or validator wrapping (<instance_of validator for type "
        + "<class 'int'>>, <instance_of validator for type <class 'str'>>)>",
    )


def TestOr_test_nested_in_and():
    """
    Larky: or_ inside and_ inside optional, and the same function failing
    more than once, still work after errors are caught.
    """
    v = optional([or_(always_fail, instance_of(int), instance_of(str)), or_(always_fail, always_fail, always_pass)])
    a = simple_attr("test")
    v(None, a, 1)
    v(None, a, None)
    v(None, a, "s")
    asserts.assert_fails(lambda: v(None, a, 1.5), "ValueError: None of .* satisfied for value 1.5")


def TestOr_test_restores_call_stack():
    """
    Larky: a validator that or_ caught failing (here with an argument error)
    can be called again afterwards without being reported as a recursive
    call; or_ restores the call stack as safe() does.
    """
    bad = lambda value: value  # wrong arity for a validator
    v = or_(bad, always_pass)
    a = simple_attr("test")
    v(None, a, 1)
    v(None, a, 1)
    asserts.assert_that(bad(7)).is_equal_to(7)


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(TestDisableValidators_test_default))
    _suite.addTest(unittest.FunctionTestCase(TestDisableValidators_test_set_validators_disabled))
    _suite.addTest(unittest.FunctionTestCase(TestDisableValidators_test_disabled))
    _suite.addTest(unittest.FunctionTestCase(TestDisableValidators_test_disabled_skips_validation))
    _suite.addTest(unittest.FunctionTestCase(TestInstanceOf_test_in_all))
    _suite.addTest(unittest.FunctionTestCase(TestInstanceOf_test_success))
    _suite.addTest(unittest.FunctionTestCase(TestInstanceOf_test_subclass))
    _suite.addTest(unittest.FunctionTestCase(TestInstanceOf_test_fail))
    _suite.addTest(unittest.FunctionTestCase(TestInstanceOf_test_repr))
    _suite.addTest(unittest.FunctionTestCase(TestInstanceOf_test_classes_and_tuples))
    _suite.addTest(unittest.FunctionTestCase(TestMatchesRe_test_in_all))
    _suite.addTest(unittest.FunctionTestCase(TestMatchesRe_test_match))
    _suite.addTest(unittest.FunctionTestCase(TestMatchesRe_test_flags))
    _suite.addTest(unittest.FunctionTestCase(TestMatchesRe_test_precompiled_pattern))
    _suite.addTest(unittest.FunctionTestCase(TestMatchesRe_test_precompiled_pattern_no_flags))
    _suite.addTest(unittest.FunctionTestCase(TestMatchesRe_test_different_func))
    _suite.addTest(unittest.FunctionTestCase(TestMatchesRe_test_catches_invalid_func))
    _suite.addTest(unittest.FunctionTestCase(TestMatchesRe_test_accepts_all_valid_func))
    _suite.addTest(unittest.FunctionTestCase(TestMatchesRe_test_repr))
    _suite.addTest(unittest.FunctionTestCase(TestAnd_test_in_all))
    _suite.addTest(unittest.FunctionTestCase(TestAnd_test_success))
    _suite.addTest(unittest.FunctionTestCase(TestAnd_test_fail))
    _suite.addTest(unittest.FunctionTestCase(TestAnd_test_sugar))
    _suite.addTest(unittest.FunctionTestCase(TestOptional_test_in_all))
    _suite.addTest(unittest.FunctionTestCase(TestOptional_test_success))
    _suite.addTest(unittest.FunctionTestCase(TestOptional_test_success_with_none))
    _suite.addTest(unittest.FunctionTestCase(TestOptional_test_fail))
    _suite.addTest(unittest.FunctionTestCase(TestOptional_test_repr))
    _suite.addTest(unittest.FunctionTestCase(TestIn__test_in_all))
    _suite.addTest(unittest.FunctionTestCase(TestIn__test_success_with_value))
    _suite.addTest(unittest.FunctionTestCase(TestIn__test_fail))
    _suite.addTest(unittest.FunctionTestCase(TestIn__test_fail_with_string))
    _suite.addTest(unittest.FunctionTestCase(TestIn__test_repr))
    _suite.addTest(unittest.FunctionTestCase(TestDeepIterable_test_in_all))
    _suite.addTest(unittest.FunctionTestCase(TestDeepIterable_test_success_member_only))
    _suite.addTest(unittest.FunctionTestCase(TestDeepIterable_test_success_member_and_iterable))
    _suite.addTest(unittest.FunctionTestCase(TestDeepIterable_test_noncallable_validators))
    _suite.addTest(unittest.FunctionTestCase(TestDeepIterable_test_fail_invalid_member))
    _suite.addTest(unittest.FunctionTestCase(TestDeepIterable_test_fail_invalid_iterable))
    _suite.addTest(unittest.FunctionTestCase(TestDeepIterable_test_fail_invalid_member_and_iterable))
    _suite.addTest(unittest.FunctionTestCase(TestDeepIterable_test_repr_member_only))
    _suite.addTest(unittest.FunctionTestCase(TestDeepIterable_test_repr_member_only_sequence))
    _suite.addTest(unittest.FunctionTestCase(TestDeepIterable_test_repr_member_and_iterable))
    _suite.addTest(unittest.FunctionTestCase(TestDeepIterable_test_repr_sequence_member_and_iterable))
    _suite.addTest(unittest.FunctionTestCase(TestDeepIterable_test_validators_iterables))
    _suite.addTest(unittest.FunctionTestCase(TestDeepIterable_test_nested))
    _suite.addTest(unittest.FunctionTestCase(TestDeepMapping_test_in_all))
    _suite.addTest(unittest.FunctionTestCase(TestDeepMapping_test_success))
    _suite.addTest(unittest.FunctionTestCase(TestDeepMapping_test_noncallable_validators))
    _suite.addTest(unittest.FunctionTestCase(TestDeepMapping_test_fail_invalid_mapping))
    _suite.addTest(unittest.FunctionTestCase(TestDeepMapping_test_fail_invalid_key))
    _suite.addTest(unittest.FunctionTestCase(TestDeepMapping_test_fail_invalid_member))
    _suite.addTest(unittest.FunctionTestCase(TestDeepMapping_test_repr))
    _suite.addTest(unittest.FunctionTestCase(TestDeepMapping_test_error_neither_validator_provided))
    _suite.addTest(unittest.FunctionTestCase(TestDeepMapping_test_key_validator_can_be_none))
    _suite.addTest(unittest.FunctionTestCase(TestDeepMapping_test_value_validator_can_be_none))
    _suite.addTest(unittest.FunctionTestCase(TestDeepMapping_test_validators_iterables))
    _suite.addTest(unittest.FunctionTestCase(TestIsCallable_test_in_all))
    _suite.addTest(unittest.FunctionTestCase(TestIsCallable_test_success))
    _suite.addTest(unittest.FunctionTestCase(TestIsCallable_test_fail))
    _suite.addTest(unittest.FunctionTestCase(TestIsCallable_test_repr))
    _suite.addTest(unittest.FunctionTestCase(TestLtLeGeGt_test_in_all))
    _suite.addTest(unittest.FunctionTestCase(TestLtLeGeGt_test_retrieve_bound))
    _suite.addTest(unittest.FunctionTestCase(TestLtLeGeGt_test_check_valid))
    _suite.addTest(unittest.FunctionTestCase(TestLtLeGeGt_test_check_invalid))
    _suite.addTest(unittest.FunctionTestCase(TestLtLeGeGt_test_repr))
    _suite.addTest(unittest.FunctionTestCase(TestMaxLen_test_in_all))
    _suite.addTest(unittest.FunctionTestCase(TestMaxLen_test_retrieve_max_len))
    _suite.addTest(unittest.FunctionTestCase(TestMaxLen_test_check_valid))
    _suite.addTest(unittest.FunctionTestCase(TestMaxLen_test_check_invalid))
    _suite.addTest(unittest.FunctionTestCase(TestMaxLen_test_repr))
    _suite.addTest(unittest.FunctionTestCase(TestMinLen_test_in_all))
    _suite.addTest(unittest.FunctionTestCase(TestMinLen_test_retrieve_min_len))
    _suite.addTest(unittest.FunctionTestCase(TestMinLen_test_check_valid))
    _suite.addTest(unittest.FunctionTestCase(TestMinLen_test_check_invalid))
    _suite.addTest(unittest.FunctionTestCase(TestMinLen_test_repr))
    _suite.addTest(unittest.FunctionTestCase(TestOr_test_in_all))
    _suite.addTest(unittest.FunctionTestCase(TestOr_test_success))
    _suite.addTest(unittest.FunctionTestCase(TestOr_test_fail))
    _suite.addTest(unittest.FunctionTestCase(TestOr_test_repr))
    _suite.addTest(unittest.FunctionTestCase(TestOr_test_nested_in_and))
    _suite.addTest(unittest.FunctionTestCase(TestOr_test_restores_call_stack))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
