# Ported from attrs 26.1.0 (7bfc49e9b22d5ba25b6e429524c3d49fee27cb36): tests/test_next_gen.py, tests/test_setattr.py. See larky/src/main/resources/VENDORED.md.
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
Integration tests for next-generation APIs (`attrs.define` & co) and for
on_setattr hooks (`attrs.setters`).

A subset ported with py2star and adapted: annotated class bodies become
`attrs.field()`s in a `types.new_class` namespace passed to `attrs.define`.
Not ported: annotations and auto_attribs detection, exceptions (auto_exc),
slots, pickling and `attrs.Converter`.
"""
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//option/result", safe="safe")
load("@vendor//attr", _attr="attr")
load("@vendor//attrs", attrs="attrs")

setters = attrs.setters
instance_of = attrs.validators.instance_of
matches_re = attrs.validators.matches_re


def _class(name, body, bases=()):
    return types.new_class(name, bases, None, lambda ns: ns.update(body))


def _define(name, body, bases=(), **kwargs):
    """`@attrs.define(**kwargs) class name(*bases): body`."""
    return attrs.define(**kwargs)(_class(name, body, bases))


def _frozen(name, body, bases=(), **kwargs):
    """`@attrs.frozen(**kwargs) class name(*bases): body`."""
    return attrs.frozen(**kwargs)(_class(name, body, bases))


def _attr_s(name, body, bases=(), **kwargs):
    """`@attr.s(**kwargs) class name(*bases): body`."""
    return _attr.s(**kwargs)(_class(name, body, bases))


def _error(f):
    r = safe(f)()
    asserts.assert_true(r.is_err)
    return "%s" % (r._val,)


# tests/test_next_gen.py's module-level `C(x: str, y: int)`.
C = _define("C", {"x": attrs.field(), "y": attrs.field()})


def _set(obj, name, value):
    def f():
        setattr(obj, name, value)
    return f


def TestNextGen_test_simple():
    """
    Instantiation works.
    """
    C("1", 2)


def TestNextGen_test_field_type():
    """
    Make class with attrs.field and type parameter.
    """
    classFields = {"testint": attrs.field(type=int)}

    A = attrs.make_class("A", classFields)

    asserts.assert_that(attrs.fields(A).testint.type).is_equal_to(int)


def TestNextGen_test_no_slots():
    """
    slots can be deactivated.
    """
    NoSlots = _define("NoSlots", {"x": attrs.field()}, slots=False)

    ns = NoSlots(1)

    asserts.assert_that(ns.__dict__).is_equal_to({"x": 1})


def TestNextGen_test_validates():
    """
    Validators at __init__ and __setattr__ work.
    """
    Validated = _define("Validated", {"x": attrs.field(validator=attrs.validators.instance_of(int))})

    v = Validated(1)

    asserts.assert_fails(lambda: Validated(None), "TypeError: 'x' must be <class 'int'>")
    asserts.assert_fails(_set(v, "x", "1"), "TypeError: 'x' must be <class 'int'>")
    asserts.assert_that(v.x).is_equal_to(1)


def TestNextGen_test_no_order():
    """
    Order is off by default but can be added.
    """
    asserts.assert_fails(lambda: C("1", 2) < C("2", 3), ".")

    Ordered = _define("Ordered", {"x": attrs.field()}, order=True)

    asserts.assert_true(Ordered(1) < Ordered(2))


def TestNextGen_test_override_auto_attribs_true():
    """
    Don't guess if auto_attrib is set explicitly.

    Having an unannotated attrs.ib/attrs.field fails.
    """
    asserts.assert_fails(
        lambda: _define("ThisFails", {"x": attrs.field()}, auto_attribs=True),
        "UnannotatedAttributeError: The following `attr.ib`s lack a type annotation: x.",
    )


def TestNextGen_test_auto_attribs_detect():
    """
    define correctly detects if a class lacks type annotations.
    """
    OldSchool = _define("OldSchool", {"x": attrs.field()})

    asserts.assert_that(OldSchool(1)).is_equal_to(OldSchool(1))

    # Test with maybe_cls = None
    OldSchool2 = attrs.define()(_class("OldSchool2", {"x": attrs.field()}))

    asserts.assert_that(OldSchool2(1)).is_equal_to(OldSchool2(1))


def TestNextGen_test_frozen():
    """
    attrs.frozen freezes classes.
    """
    F = _frozen("F", {"x": attrs.field()})

    f = F(1)

    asserts.assert_that(_error(_set(f, "x", 2))).is_equal_to("FrozenInstanceError: can't set attribute")
    asserts.assert_that(f.x).is_equal_to(1)


def TestNextGen_test_auto_detect_eq():
    """
    auto_detect=True works for eq.

    Regression test for #670.
    """
    def __eq__(self, o):
        fail("ValueError")

    D = _define("D", {"__eq__": __eq__})

    asserts.assert_fails(lambda: D() == D(), "ValueError")


def TestNextGen_test_subclass_frozen():
    """
    It's possible to subclass an `attrs.frozen` class and the frozen-ness
    is inherited.
    """
    A = _frozen("A", {"a": attrs.field()})
    B = _frozen("B", {"b": attrs.field()}, bases=(A,))
    D = _define("D", {"c": attrs.field()}, bases=(B,), on_setattr=setters.NO_OP)

    asserts.assert_that(B(1, 2)).is_equal_to(B(1, 2))
    asserts.assert_that(D(1, 2, 3)).is_equal_to(D(1, 2, 3))

    frozen = "FrozenInstanceError: can't set attribute"
    asserts.assert_fails(_set(A(1), "a", 1), frozen)
    asserts.assert_fails(_set(B(1, 2), "a", 1), frozen)
    asserts.assert_fails(_set(B(1, 2), "b", 2), frozen)
    asserts.assert_fails(_set(D(1, 2, 3), "c", 3), frozen)


def TestNextGen_test_catches_frozen_on_setattr():
    """
    Passing frozen=True and on_setattr hooks is caught, even if the
    immutability is inherited.
    """
    A = _define("A", {}, frozen=True)

    asserts.assert_fails(
        lambda: _define("B", {}, frozen=True, on_setattr=setters.validate),
        "ValueError: Frozen classes can't use on_setattr",
    )

    asserts.assert_that(_error(lambda: _define("D", {}, bases=(A,), on_setattr=setters.validate))).is_equal_to(
        "ValueError: Frozen classes can't use on_setattr (frozen-ness was inherited).",
    )


def TestNextGen_test_converts_and_validates_by_default():
    """
    If no on_setattr is set, assume setters.convert, setters.validate.
    """
    x = attrs.field(converter=int)

    def _v(self, _, value):
        if value < 10:
            fail("ValueError: must be >=10")

    x.validator(_v)

    D = _define("D", {"x": x})

    inst = D(10)

    # Converts
    inst.x = "11"

    asserts.assert_that(inst.x).is_equal_to(11)

    # Validates
    asserts.assert_fails(_set(inst, "x", "9"), "ValueError: must be >=10")


def TestNextGen_test_mro_ng():
    """
    Attributes and methods are looked up the same way in NG by default.
    """
    A = _define("A", {"x": attrs.field(default=10), "xx": lambda self: 10})
    B = _define("B", {"y": attrs.field(default=20)}, bases=(A,))
    D = _define("D", {"x": attrs.field(default=50), "xx": lambda self: 50}, bases=(A,))
    E = _define("E", {}, bases=(B, D))

    e = E()

    asserts.assert_that(e.x).is_equal_to(e.xx())
    asserts.assert_that(e.x).is_equal_to(50)


def TestAsTuple_test_smoke():
    """
    `attrs.astuple` only changes defaults, so we just call it and compare.
    """
    inst = C("foo", 42)

    asserts.assert_that(attrs.astuple(inst)).is_equal_to(_attr.astuple(inst))


def TestAsDict_test_smoke():
    """
    `attrs.asdict` only changes defaults, so we just call it and compare.
    """
    inst = C("foo", {(1,): 42})

    asserts.assert_that(attrs.asdict(inst)).is_equal_to(_attr.asdict(inst, retain_collection_types=True))


def TestSetAttr_test_change():
    """
    The return value of a hook overwrites the value. But they are not run
    on __init__.
    """
    def hook(*a, **kw):
        return "hooked!"

    Hooked = _attr_s("Hooked", {"x": _attr.ib(on_setattr=hook), "y": _attr.ib()})

    h = Hooked("x", "y")

    asserts.assert_that(h.x).is_equal_to("x")
    asserts.assert_that(h.y).is_equal_to("y")

    h.x = "xxx"
    h.y = "yyy"

    asserts.assert_that(h.y).is_equal_to("yyy")
    asserts.assert_that(h.x).is_equal_to("hooked!")


def TestSetAttr_test_frozen_attribute():
    """
    Frozen attributes raise FrozenAttributeError, others are not affected.
    """
    PartiallyFrozen = _attr_s("PartiallyFrozen", {"x": _attr.ib(on_setattr=setters.frozen), "y": _attr.ib()})

    pf = PartiallyFrozen("x", "y")

    pf.y = "yyy"

    asserts.assert_that(pf.y).is_equal_to("yyy")

    asserts.assert_that(_error(_set(pf, "x", "xxx"))).is_equal_to("FrozenAttributeError: can't set attribute")

    asserts.assert_that(pf.x).is_equal_to("x")


def TestSetAttr_test_validator():
    """
    Validators are run and they don't alter the value.
    """
    for on_setattr in [setters.validate, [setters.validate], setters.pipe(setters.validate)]:
        ValidatedAttribute = _attr_s("ValidatedAttribute", {
            "x": _attr.ib(),
            "y": _attr.ib(validator=[instance_of(str), matches_re("foo.*qux")]),
        }, on_setattr=on_setattr)

        va = ValidatedAttribute(42, "foobarqux")

        asserts.assert_true(_error(_set(va, "y", 42)).startswith("TypeError: 'y' must be <"))

        asserts.assert_that(va.y).is_equal_to("foobarqux")

        asserts.assert_true(_error(_set(va, "y", "quxbarfoo")).startswith("ValueError: 'y' must match regex \""))

        asserts.assert_that(va.y).is_equal_to("foobarqux")

        va.y = "foobazqux"

        asserts.assert_that(va.y).is_equal_to("foobazqux")


def TestSetAttr_test_pipe():
    """
    Multiple hooks are possible, in that case the last return value is
    used. They can be supplied using the pipe functions or by passing a
    list to on_setattr.
    """
    s = [setters.convert, lambda _, __, nv: nv + 1]

    Piped = _attr_s("Piped", {
        "x1": _attr.ib(converter=int, on_setattr=setters.pipe(*s)),
        "x2": _attr.ib(converter=int, on_setattr=s),
    })

    p = Piped("41", "22")

    asserts.assert_that(p.x1).is_equal_to(41)
    asserts.assert_that(p.x2).is_equal_to(22)

    p.x1 = "41"
    p.x2 = "22"

    asserts.assert_that(p.x1).is_equal_to(42)
    asserts.assert_that(p.x2).is_equal_to(23)


def TestSetAttr_test_make_class():
    """
    on_setattr of make_class gets forwarded.
    """
    D = _attr.make_class("D", {"x": _attr.ib()}, on_setattr=setters.frozen)

    d = D(1)

    asserts.assert_fails(_set(d, "x", 2), "FrozenAttributeError")


def TestSetAttr_test_no_validator_no_converter():
    """
    validate and convert tolerate missing validators and converters.
    """
    D = _attr_s("D", {"x": _attr.ib()}, on_setattr=[setters.convert, setters.validate])

    d = D(1)

    d.x = 2

    asserts.assert_that(d.x).is_equal_to(2)


def TestSetAttr_test_validate_respects_run_validators_config():
    """
    If run validators is off, validate doesn't run them.
    """
    D = _attr_s("D", {"x": _attr.ib(validator=_attr.validators.instance_of(int))}, on_setattr=setters.validate)

    d = D(1)

    _attr.set_run_validators(False)

    d.x = "1"

    asserts.assert_that(d.x).is_equal_to("1")

    _attr.set_run_validators(True)

    asserts.assert_true(_error(_set(d, "x", "1")).startswith("TypeError: 'x' must be <"))


def TestSetAttr_test_frozen_on_setattr_class_is_caught():
    """
    @attr.s(on_setattr=X, frozen=True) raises an ValueError.
    """
    asserts.assert_that(_error(lambda: _attr_s("D", {"x": _attr.ib()}, frozen=True, on_setattr=setters.validate))).is_equal_to(
        "ValueError: Frozen classes can't use on_setattr.",
    )


def TestSetAttr_test_frozen_on_setattr_attribute_is_caught():
    """
    attr.ib(on_setattr=X) on a frozen class raises an ValueError.
    """
    asserts.assert_that(_error(lambda: _attr_s("D", {"x": _attr.ib(on_setattr=setters.validate)}, frozen=True))).is_equal_to(
        "ValueError: Frozen classes can't use on_setattr.",
    )


def TestSetAttr_test_frozen_on_setattr_nops():
    """
    on_setattr on frozen classes can be used for None and NO_OP.
    """
    for nop in [None, setters.NO_OP]:
        _attr_s("D", {"x": _attr.ib(on_setattr=nop)}, frozen=True)


def TestSetAttr_test_no_op_disables_class_hook():
    """
    Larky: setters.NO_OP on a field disables the class-wide hook for it.
    """
    D = _define("D", {
        "x": attrs.field(validator=instance_of(int)),
        "y": attrs.field(validator=instance_of(int), on_setattr=setters.NO_OP),
    })
    d = D(1, 2)
    d.y = "unchecked"
    asserts.assert_that(d.y).is_equal_to("unchecked")
    asserts.assert_fails(_set(d, "x", "checked"), "TypeError: 'x' must be <class 'int'>")


def _testsuite():
    _suite = unittest.TestSuite()
    for t in [
        TestNextGen_test_simple,
        TestNextGen_test_field_type,
        TestNextGen_test_no_slots,
        TestNextGen_test_validates,
        TestNextGen_test_no_order,
        TestNextGen_test_override_auto_attribs_true,
        TestNextGen_test_auto_attribs_detect,
        TestNextGen_test_frozen,
        TestNextGen_test_auto_detect_eq,
        TestNextGen_test_subclass_frozen,
        TestNextGen_test_catches_frozen_on_setattr,
        TestNextGen_test_converts_and_validates_by_default,
        TestNextGen_test_mro_ng,
        TestAsTuple_test_smoke,
        TestAsDict_test_smoke,
        TestSetAttr_test_change,
        TestSetAttr_test_frozen_attribute,
        TestSetAttr_test_validator,
        TestSetAttr_test_pipe,
        TestSetAttr_test_make_class,
        TestSetAttr_test_no_validator_no_converter,
        TestSetAttr_test_validate_respects_run_validators_config,
        TestSetAttr_test_frozen_on_setattr_class_is_caught,
        TestSetAttr_test_frozen_on_setattr_attribute_is_caught,
        TestSetAttr_test_frozen_on_setattr_nops,
        TestSetAttr_test_no_op_disables_class_hook,
    ]:
        _suite.addTest(unittest.FunctionTestCase(t))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
