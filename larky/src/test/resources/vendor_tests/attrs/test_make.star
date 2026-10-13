# Ported from attrs 26.1.0 (7bfc49e9b22d5ba25b6e429524c3d49fee27cb36): tests/test_make.py. See larky/src/main/resources/VENDORED.md.
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
Tests for `attr._make`.

A subset ported with py2star and adapted: `@attr.s class C: ...` becomes
`attr.s` applied to a `types.new_class` class, hypothesis strategies become
fixed examples, and `pytest.raises(...)` checks the failure message. Not
ported: hashing, slots, pickling, annotations, generics, match_args,
`Converter`, `__attrs_init_subclass__` and internals (`_ClassBuilder`).
"""
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//option/result", safe="safe")
load("@vendor//attr", attr="attr")

Factory = attr.Factory
NOTHING = attr.NOTHING
fields = attr.fields
fields_dict = attr.fields_dict
make_class = attr.make_class
validate = attr.validate
instance_of = attr.validators.instance_of


def simple_attr(name, default=NOTHING, validator=None, repr=True, eq=True, hash=None, init=True, converter=None, kw_only=False, inherited=False):
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


def _class(name, body, bases=()):
    """`class name(*bases): body`."""
    return types.new_class(name, bases, None, lambda ns: ns.update(body))


def _attrs_class(name, body, bases=(), **kwargs):
    """`@attr.s(**kwargs) class name(*bases): body`."""
    return attr.s(**kwargs)(_class(name, body, bases))


def _define(name, body, bases=(), **kwargs):
    """`@attr.define(**kwargs) class name(*bases): body`."""
    return attr.define(**kwargs)(_class(name, body, bases))


def _error(f):
    r = safe(f)()
    asserts.assert_true(r.is_err)
    return "%s" % (r._val,)


def TestCountingAttr_test_returns_Attr():
    """
    Returns an instance of _CountingAttr.
    """
    a = attr.ib()

    asserts.assert_that(type(a)).is_equal_to("_CountingAttr")


def TestCountingAttr_test_validators_lists_to_wrapped_tuples():
    """
    If a list is passed as validator, it's just converted to a tuple.
    """
    def v1(_, __):
        pass

    def v2(_, __):
        pass

    a = attr.ib(validator=[v1, v2])

    asserts.assert_that(a._validator).is_equal_to(attr.validators.and_(v1, v2))
    asserts.assert_that(a._validator._validators).is_equal_to((v1, v2))


def TestCountingAttr_test_validator_decorator_single():
    """
    If _CountingAttr.validator is used as a decorator and there is no
    decorator set, the decorated method is used as the validator.
    """
    a = attr.ib()

    def v():
        pass

    a.validator(v)

    asserts.assert_that(a._validator).is_equal_to(v)


def TestCountingAttr_test_validator_decorator():
    """
    If _CountingAttr.validator is used as a decorator and there is already
    a decorator set, the decorators are composed using `and_`.
    """
    for wrap in [lambda v: v, lambda v: [v], attr.validators.and_]:
        def v(_, __):
            pass

        a = attr.ib(validator=wrap(v))

        def v2(self, _, __):
            pass

        a.validator(v2)

        asserts.assert_that(a._validator._validators).is_equal_to((v, v2))


def TestCountingAttr_test_default_decorator_already_set():
    """
    Raise DefaultAlreadySetError if the decorator is used after a default
    has been set.
    """
    a = attr.ib(default=42)

    def f(self):
        pass

    asserts.assert_fails(lambda: a.default(f), "DefaultAlreadySetError")


def TestCountingAttr_test_default_decorator_sets():
    """
    Decorator wraps the method in a Factory with pass_self=True and sets
    the default.
    """
    a = attr.ib()

    def f(self):
        pass

    a.default(f)

    asserts.assert_that(a._default).is_equal_to(Factory(f, True))


def TestTransformAttrs_test_conflicting_defaults():
    """
    Raises `ValueError` if attributes with defaults are followed by
    mandatory attributes.
    """
    asserts.assert_that(_error(lambda: _attrs_class("C", {
        "x": attr.ib(default=None),
        "y": attr.ib(),
    }))).is_equal_to(
        "ValueError: No mandatory attributes allowed after an attribute with a "
        + "default value or factory.  Attribute in question: Attribute"
        + "(name=\"y\", default=NOTHING, validator=None, repr=True, "
        + "eq=True, eq_key=None, order=True, order_key=None, "
        + "hash=None, init=True, "
        + "metadata=mappingproxy({}), type=None, converter=None, "
        + "kw_only=False, inherited=False, on_setattr=None, alias=\"y\")",
    )


def TestTransformAttrs_test_kw_only():
    """
    Converts all attributes in the current class if `kw_only` is provided.
    Base class attributes are **not** affected.
    """
    B = _attrs_class("B", {"b": attr.ib()})

    for b_a in B.__attrs_attrs__:
        asserts.assert_that(b_a.kw_only).is_equal_to(False)

    body = {"x": attr.ib(default=None), "y": attr.ib()}
    C = _attrs_class("C", body, bases=(B,), kw_only=True, force_kw_only=False)

    asserts.assert_that(len(fields(C))).is_equal_to(3)

    for a in fields(C):
        if a.name == "b":
            asserts.assert_that(a.kw_only).is_equal_to(False)
        else:
            asserts.assert_that(a.kw_only).is_equal_to(True)

    C = _attrs_class("C", {"x": attr.ib(default=None), "y": attr.ib()}, bases=(B,), kw_only=True)

    asserts.assert_that(len(fields(C))).is_equal_to(3)

    for a in fields(C):
        asserts.assert_that(a.kw_only).is_equal_to(True)

    for b_a in B.__attrs_attrs__:
        asserts.assert_that(b_a.kw_only).is_equal_to(False)


def TestTransformAttrs_test_these():
    """
    If these is passed, use it and ignore body and base classes.
    """
    Base = _class("Base", {"z": attr.ib()})
    C = attr.s(these={"x": attr.ib()})(_class("C", {"y": attr.ib()}, bases=(Base,)))

    asserts.assert_that(fields(C)).is_equal_to((simple_attr("x"),))


def TestTransformAttrs_test_these_leave_body():
    """
    If these is passed, no attributes are removed from the body.
    """
    C = attr.s(init=False, these={"x": attr.ib()})(_class("C", {"x": 5}))

    asserts.assert_that(C().x).is_equal_to(5)
    asserts.assert_that(repr(C())).is_equal_to("C(x=5)")


def TestTransformAttrs_test_these_ordered():
    """
    If these is passed ordered attrs, their order respect instead of the
    counter.
    """
    b = attr.ib(default=2)
    a = attr.ib(default=1)

    C = attr.s(these={"a": a, "b": b})(_class("C", {}))

    asserts.assert_that(repr(C())).is_equal_to("C(a=1, b=2)")


def TestTransformAttrs_test_inherited():
    """
    Inherited Attributes have `.inherited` True, otherwise False.
    """
    A = _attrs_class("A", {"a": attr.ib()})
    B = _attrs_class("B", {"b": attr.ib()}, bases=(A,))
    C = _attrs_class("C", {"a": attr.ib(), "c": attr.ib()}, bases=(B,))

    f = attr.fields

    asserts.assert_that(f(A).a.inherited).is_equal_to(False)

    asserts.assert_that(f(B).a.inherited).is_equal_to(True)
    asserts.assert_that(f(B).b.inherited).is_equal_to(False)

    asserts.assert_that(f(C).a.inherited).is_equal_to(False)
    asserts.assert_that(f(C).b.inherited).is_equal_to(True)
    asserts.assert_that(f(C).c.inherited).is_equal_to(False)


def TestAttributes_test_pre_init():
    """
    Verify that __attrs_pre_init__ gets called if defined.
    """
    def __attrs_pre_init__(self2):
        self2.z = 30

    C = _attrs_class("C", {"__attrs_pre_init__": __attrs_pre_init__})

    c = C()

    asserts.assert_that(getattr(c, "z", None)).is_equal_to(30)


def TestAttributes_test_post_init():
    """
    Verify that __attrs_post_init__ gets called if defined.
    """
    def __attrs_post_init__(self2):
        self2.z = self2.x + self2.y

    C = _attrs_class("C", {
        "x": attr.ib(),
        "y": attr.ib(),
        "__attrs_post_init__": __attrs_post_init__,
    })

    c = C(x=10, y=20)

    asserts.assert_that(getattr(c, "z", None)).is_equal_to(30)


def TestAttributes_test_pre_post_init_order():
    """
    Verify that __attrs_post_init__ gets called if defined.
    """
    def __attrs_pre_init__(self2):
        self2.z = 30

    def __attrs_post_init__(self2):
        self2.z += self2.x

    C = _attrs_class("C", {
        "x": attr.ib(),
        "__attrs_pre_init__": __attrs_pre_init__,
        "__attrs_post_init__": __attrs_post_init__,
    })

    c = C(x=10)

    asserts.assert_that(getattr(c, "z", None)).is_equal_to(40)


def TestAttributes_test_types():
    """
    Sets the `Attribute.type` attr from type argument.
    """
    C = _attrs_class("C", {
        "x": attr.ib(type=int),
        "y": attr.ib(type=str),
        "z": attr.ib(),
    })

    asserts.assert_that(fields(C).x.type).is_equal_to(int)
    asserts.assert_that(fields(C).y.type).is_equal_to(str)
    asserts.assert_that(fields(C).z.type).is_equal_to(None)


def TestAttributes_test_clean_class():
    """
    Attribute definitions do not appear on the class body after @attr.s.
    """
    for slots in [True, False]:
        C = _attrs_class("C", {"x": attr.ib()}, slots=slots)

        x = getattr(C, "x", None)

        asserts.assert_that(type(x)).is_not_equal_to("_CountingAttr")


def TestAttributes_test_factory_sugar():
    """
    Passing factory=f is syntactic sugar for passing default=Factory(f).
    """
    C = _attrs_class("C", {"x": attr.ib(factory=list)})

    asserts.assert_that(attr.fields(C).x.default).is_equal_to(Factory(list))


def TestAttributes_test_sugar_factory_mutex():
    """
    Passing both default and factory raises ValueError.
    """
    asserts.assert_fails(
        lambda: _attrs_class("C", {"x": attr.ib(factory=list, default=Factory(list))}),
        "ValueError: .*mutually exclusive",
    )


def TestAttributes_test_sugar_callable():
    """
    Factory has to be a callable to prevent people from passing Factory
    into it.
    """
    asserts.assert_fails(
        lambda: _attrs_class("C", {"x": attr.ib(factory=Factory(list))}),
        "ValueError: .*must be a callable",
    )


def TestAttributes_test_factory_is_called_per_instance():
    """
    Larky: a Factory default is a new value for each instance.
    """
    C = _attrs_class("C", {"x": attr.ib(factory=list)})
    c1 = C()
    c2 = C()
    c1.x.append(1)
    asserts.assert_that(c2.x).is_equal_to([])
    asserts.assert_that(C([2]).x).is_equal_to([2])


def TestKeywordOnlyAttributes_test_adds_keyword_only_arguments():
    """
    Attributes can be added as keyword-only.
    """
    C = _attrs_class("C", {
        "a": attr.ib(),
        "b": attr.ib(default=2, kw_only=True),
        "c": attr.ib(kw_only=True),
        "d": attr.ib(default=attr.Factory(lambda: 4), kw_only=True),
    })

    c = C(1, c=3)

    asserts.assert_that(c.a).is_equal_to(1)
    asserts.assert_that(c.b).is_equal_to(2)
    asserts.assert_that(c.c).is_equal_to(3)
    asserts.assert_that(c.d).is_equal_to(4)


def TestKeywordOnlyAttributes_test_ignores_kw_only_when_init_is_false():
    """
    Specifying ``kw_only=True`` when ``init=False`` is essentially a no-op.
    """
    C = _attrs_class("C", {
        "x": attr.ib(init=False, default=0, kw_only=True),
        "y": attr.ib(),
    })

    c = C(1)

    asserts.assert_that(c.x).is_equal_to(0)
    asserts.assert_that(c.y).is_equal_to(1)


def TestKeywordOnlyAttributes_test_keyword_only_attributes_presence():
    """
    Raises `TypeError` when keyword-only arguments are
    not specified.
    """
    C = _attrs_class("C", {"x": attr.ib(kw_only=True)})

    asserts.assert_that(_error(lambda: C())).is_equal_to(
        "TypeError: C.__init__() missing 1 required keyword-only argument: 'x'",
    )


def TestKeywordOnlyAttributes_test_keyword_only_attributes_unexpected():
    """
    Raises `TypeError` when unexpected keyword argument passed.
    """
    C = _attrs_class("C", {"x": attr.ib(kw_only=True)})

    asserts.assert_that(_error(lambda: C(x=5, y=10))).is_equal_to(
        "TypeError: C.__init__() got an unexpected keyword argument 'y'",
    )


def TestKeywordOnlyAttributes_test_keyword_only_attributes_can_come_in_any_order():
    """
    Mandatory vs non-mandatory attr order only matters when they are part
    of the __init__ signature and when they aren't kw_only (which are
    moved to the end and can be mandatory or non-mandatory in any order,
    as they will be specified as keyword args anyway).
    """
    C = _attrs_class("C", {
        "a": attr.ib(kw_only=True),
        "b": attr.ib(kw_only=True, default="b"),
        "c": attr.ib(kw_only=True),
        "d": attr.ib(),
        "e": attr.ib(default="e"),
        "f": attr.ib(kw_only=True),
        "g": attr.ib(kw_only=True, default="g"),
        "h": attr.ib(kw_only=True),
        "i": attr.ib(init=False),
    })

    c = C("d", a="a", c="c", f="f", h="h")

    asserts.assert_that(c.a).is_equal_to("a")
    asserts.assert_that(c.b).is_equal_to("b")
    asserts.assert_that(c.c).is_equal_to("c")
    asserts.assert_that(c.d).is_equal_to("d")
    asserts.assert_that(c.e).is_equal_to("e")
    asserts.assert_that(c.f).is_equal_to("f")
    asserts.assert_that(c.g).is_equal_to("g")
    asserts.assert_that(c.h).is_equal_to("h")


def TestKeywordOnlyAttributes_test_keyword_only_attributes_allow_subclassing():
    """
    Subclass can define keyword-only attributed without defaults,
    when the base class has attributes with defaults.
    """
    Base = _attrs_class("Base", {"x": attr.ib(default=0)})
    C = _attrs_class("C", {"y": attr.ib(kw_only=True)}, bases=(Base,))

    c = C(y=1)

    asserts.assert_that(c.x).is_equal_to(0)
    asserts.assert_that(c.y).is_equal_to(1)


def TestKeywordOnlyAttributes_test_keyword_only_class_level():
    """
    `kw_only` can be provided at the attr.s level, converting all
    attributes to `kw_only.`
    """
    C = _attrs_class("C", {"x": attr.ib(), "y": attr.ib(kw_only=True)}, kw_only=True)

    asserts.assert_fails(lambda: C(0, y=1), "TypeError: C.__init__\\(\\) takes 1 positional argument but 2 positional arguments \\(and 1 keyword-only argument\\) were given")

    c = C(x=0, y=1)

    asserts.assert_that(c.x).is_equal_to(0)
    asserts.assert_that(c.y).is_equal_to(1)


def TestKeywordOnlyAttributes_test_init_false_attribute_after_keyword_attribute():
    """
    A positional attribute cannot follow a `kw_only` attribute,
    but an `init=False` attribute can because it won't appear
    in `__init__`
    """
    kwarg = attr.ib(kw_only=True)
    non_init_function_default = attr.ib(init=False)

    def _init_to_init(self):
        return self.kwarg + "b"

    non_init_function_default.default(_init_to_init)

    KwArgBeforeInitFalse = _attrs_class("KwArgBeforeInitFalse", {
        "kwarg": kwarg,
        "non_init_function_default": non_init_function_default,
        "non_init_keyword_default": attr.ib(init=False, default="default-by-keyword"),
    })

    c = KwArgBeforeInitFalse(kwarg="a")

    asserts.assert_that(c.kwarg).is_equal_to("a")
    asserts.assert_that(c.non_init_function_default).is_equal_to("ab")
    asserts.assert_that(c.non_init_keyword_default).is_equal_to("default-by-keyword")


def TestKeywordOnlyAttributes_test_attr_level_kw_only_and_class_level_kw_only():
    """
    If a field explicitly sets 'kw_only=False', it should not be made
    keyword-only even if the class sets 'kw_only=True'.
    """
    OldClassOldBehavior = _attrs_class("OldClassOldBehavior", {"yes": attr.ib(), "no": attr.ib(kw_only=False)}, kw_only=True)
    OldClassNewBehavior = _attrs_class("OldClassNewBehavior", {"yes": attr.ib(), "no": attr.ib(kw_only=False)}, kw_only=True, force_kw_only=False)
    NewClassOldBehavior = _define("NewClassOldBehavior", {"yes": attr.field(), "no": attr.field(kw_only=False)}, kw_only=True, force_kw_only=True)
    NewClassNewBehavior = _define("NewClassNewBehavior", {"yes": attr.field(), "no": attr.field(kw_only=False)}, kw_only=True)

    for cls in [OldClassNewBehavior, NewClassNewBehavior]:
        fs = fields_dict(cls)
        asserts.assert_that(fs["yes"].kw_only).is_equal_to(True)
        asserts.assert_that(fs["no"].kw_only).is_equal_to(False)

    for cls in [OldClassOldBehavior, NewClassOldBehavior]:
        fs = fields_dict(cls)

        asserts.assert_that(fs["yes"].kw_only).is_equal_to(True)
        asserts.assert_that(fs["no"].kw_only).is_equal_to(True)


def _assert_kw_only(cls, expected):
    fs = fields_dict(cls)
    for name, kw_only in expected.items():
        asserts.assert_that(fs[name].kw_only).is_equal_to(kw_only)


def TestKeywordOnlyAttributes_test_kw_only_inheritance():
    """
    Comprehensive test about how `kw_only` works when there's multiple
    levels of inheritance with different `kw_only` settings.
    """
    A = _define("A", {"a": attr.field(), "a_t": attr.field(kw_only=True), "a_f": attr.field(kw_only=False)})
    B = _define("B", {"b": attr.field(), "b_t": attr.field(kw_only=True), "b_f": attr.field(kw_only=False)}, bases=(A,), kw_only=True)
    C = _define("C", {"c": attr.field(), "c_t": attr.field(kw_only=True), "c_f": attr.field(kw_only=False)}, bases=(B,), kw_only=False)

    _assert_kw_only(A, {"a": False, "a_t": True, "a_f": False})
    _assert_kw_only(B, {"a": False, "a_t": True, "a_f": False, "b": True, "b_t": True, "b_f": False})
    _assert_kw_only(C, {"a": False, "a_t": True, "a_f": False, "b": True, "b_t": True, "b_f": False, "c": False, "c_t": True, "c_f": False})


def TestKeywordOnlyAttributes_test_kw_only_inheritance_force_kw_only():
    """
    Similar to above, but when `force_kw_only` (pre-25.3.0 behavior).
    """
    A = _define("A", {"a": attr.field(), "a_t": attr.field(kw_only=True), "a_f": attr.field(kw_only=False)}, force_kw_only=True)
    B = _define("B", {"b": attr.field(), "b_t": attr.field(kw_only=True), "b_f": attr.field(kw_only=False)}, bases=(A,), kw_only=True, force_kw_only=True)
    C = _define("C", {"c": attr.field(), "c_t": attr.field(kw_only=True), "c_f": attr.field(kw_only=False)}, bases=(B,), kw_only=False, force_kw_only=True)

    _assert_kw_only(A, {"a": False, "a_t": True, "a_f": False})
    _assert_kw_only(B, {"a": True, "a_t": True, "a_f": True, "b": True, "b_t": True, "b_f": True})
    _assert_kw_only(C, {"a": False, "a_t": True, "a_f": False, "b": True, "b_t": True, "b_f": True, "c": False, "c_t": True, "c_f": False})


def TestMakeClass_test_simple():
    """
    Passing a list of strings creates attributes with default args.
    """
    for ls in [list, tuple]:
        C1 = make_class("C1", ls(["a", "b"]))

        C2 = _attrs_class("C2", {"a": attr.ib(), "b": attr.ib()})

        asserts.assert_that(C1.__attrs_attrs__).is_equal_to(C2.__attrs_attrs__)


def TestMakeClass_test_dict():
    """
    Passing a dict of name: _CountingAttr creates an equivalent class.
    """
    C1 = make_class("C1", {"a": attr.ib(default=42), "b": attr.ib(default=None)})

    C2 = _attrs_class("C2", {"a": attr.ib(default=42), "b": attr.ib(default=None)})

    asserts.assert_that(C1.__attrs_attrs__).is_equal_to(C2.__attrs_attrs__)


def TestMakeClass_test_attr_args():
    """
    attributes_arguments are passed to attributes
    """
    C = make_class("C", ["x"], repr=False)

    # attrs: "<tests.test_make.C object at 0x...>". Larky's default repr.
    asserts.assert_that(repr(C(1))).is_equal_to("<'C' object>")


def TestMakeClass_test_catches_wrong_attrs_type():
    """
    Raise `TypeError` if an invalid type for attrs is passed.
    """
    asserts.assert_that(_error(lambda: make_class("C", object()))).is_equal_to(
        "TypeError: attrs argument must be a dict or a list.",
    )


def TestMakeClass_test_bases():
    """
    Parameter bases default to (object,) and subclasses correctly
    """
    D = _class("D", {})

    cls = make_class("C", {})

    asserts.assert_that(cls.__mro__[-1]).is_equal_to(object)

    cls = make_class("C", {}, bases=(D,))

    asserts.assert_true(D in cls.__mro__)
    asserts.assert_true(larky.is_instance(cls(), D))


def TestMakeClass_test_additional_class_body():
    """
    Additional class_body is added to newly created class.
    """
    def echo_func(cls, *args):
        return args

    cls = make_class("C", {}, class_body={"echo": classmethod(echo_func)})

    asserts.assert_that(cls.echo("a", "b")).is_equal_to(("a", "b"))


def TestMakeClass_test_clean_class():
    """
    Attribute definitions do not appear on the class body.
    """
    for slots in [True, False]:
        C = make_class("C", ["x"], slots=slots)

        x = getattr(C, "x", None)

        asserts.assert_that(type(x)).is_not_equal_to("_CountingAttr")


def TestMakeClass_test_make_class_ordered():
    """
    If `make_class()` is passed ordered attrs, their order is respected
    instead of the counter.
    """
    b = attr.ib(default=2)
    a = attr.ib(default=1)

    C = attr.make_class("C", {"a": a, "b": b})

    asserts.assert_that(repr(C())).is_equal_to("C(a=1, b=2)")


def _simple_class():
    return make_class("C", ["a", "b"], eq=True, order=False, repr=False, init=True)


def TestFields_test_instance():
    """
    Returns the class fields for *attrs* instances too.
    """
    C = _simple_class()
    asserts.assert_that(fields(C(1, 2))).is_equal_to(fields(C))


def TestFields_test_handler_non_attrs_instance():
    """
    Raises `TypeError` on non-*attrs* instances.
    """
    asserts.assert_that(_error(lambda: fields(object()))).is_equal_to(
        "TypeError: Passed object must be a class or attrs instance.",
    )


def TestFields_test_handler_non_attrs_class():
    """
    Raises `ValueError` if passed a non-*attrs* instance.
    """
    asserts.assert_that(_error(lambda: fields(object))).is_equal_to(
        "NotAnAttrsClassError: %r is not an attrs-decorated class." % (object,),
    )


def TestFields_test_fields():
    """
    Returns a list of `Attribute`a.
    """
    C = _simple_class()
    asserts.assert_that([type(a) for a in fields(C)]).is_equal_to(["Attribute", "Attribute"])


def TestFields_test_fields_properties():
    """
    Fields returns a tuple with properties.
    """
    C = _simple_class()
    for attribute in fields(C):
        asserts.assert_that(getattr(fields(C), attribute.name)).is_equal_to(attribute)


def TestFieldsDict_test_instance():
    """
    Raises `TypeError` on non-classes.
    """
    C = _simple_class()
    asserts.assert_that(_error(lambda: fields_dict(C(1, 2)))).is_equal_to(
        "TypeError: Passed object must be a class.",
    )


def TestFieldsDict_test_handler_non_attrs_class():
    """
    Raises `ValueError` if passed a non-*attrs* instance.
    """
    asserts.assert_that(_error(lambda: fields_dict(object))).is_equal_to(
        "NotAnAttrsClassError: %r is not an attrs-decorated class." % (object,),
    )


def TestFieldsDict_test_fields_dict():
    """
    Returns an ordered dict of ``{attribute_name: Attribute}``.
    """
    C = _simple_class()
    d = fields_dict(C)

    asserts.assert_that(type(d)).is_equal_to("dict")
    asserts.assert_that(list(d.values())).is_equal_to(list(fields(C)))
    asserts.assert_that(list(d)).is_equal_to([a.name for a in fields(C)])


def TestConverter_test_converter():
    """
    Return value of converter is used as the attribute's value.
    """
    C = make_class("C", {"x": attr.ib(converter=lambda v: v + 1), "y": attr.ib()})
    c = C(1, 2)

    asserts.assert_that(c.x).is_equal_to(2)
    asserts.assert_that(c.y).is_equal_to(2)


def TestConverter_test_convert_property():
    """
    Property tests for attributes using converter.
    """
    for val in [-1, 0, 7]:
        for init in [True, False]:
            C = make_class("C", {
                "y": attr.ib(),
                "x": attr.ib(init=init, default=val, converter=lambda v: v + 1),
            })
            c = C(2)

            asserts.assert_that(c.x).is_equal_to(val + 1)
            asserts.assert_that(c.y).is_equal_to(2)


def TestConverter_test_converter_factory_property():
    """
    Property tests for attributes with converter, and a factory default.
    """
    for val in [-1, 0, 7]:
        for init in [True, False]:
            C = make_class("C", {
                "y": attr.ib(),
                "x": attr.ib(init=init, default=Factory(lambda: val), converter=lambda v: v + 1),
            })
            c = C(2)

            asserts.assert_that(c.x).is_equal_to(val + 1)
            asserts.assert_that(c.y).is_equal_to(2)


def TestConverter_test_convert_before_validate():
    """
    Validation happens after conversion.
    """
    def validator(inst, attr, val):
        fail("RuntimeError: foo")

    def zero_division(v):
        fail("ZeroDivisionError: division by zero")

    C = make_class("C", {
        "x": attr.ib(validator=validator, converter=zero_division),
        "y": attr.ib(),
    })

    asserts.assert_fails(lambda: C(1, 2), "ZeroDivisionError")


def TestConverter_test_frozen():
    """
    Converters circumvent immutability.
    """
    C = make_class("C", {"x": attr.ib(converter=int)}, frozen=True)
    asserts.assert_that(C("1").x).is_equal_to(1)


def TestConverter_test_nested_attrs_class():
    """
    Larky: a converter (or factory) may build another attrs instance, which
    runs __init__ while the outer __init__ is running.
    """
    Inner = make_class("Inner", {"v": attr.ib(validator=instance_of(int))})
    Outer = make_class("Outer", {
        "inner": attr.ib(converter=lambda d: Inner(**d)),
        "other": attr.ib(factory=lambda: Inner(0)),
    })
    asserts.assert_that(repr(Outer({"v": 1}))).is_equal_to("Outer(inner=Inner(v=1), other=Inner(v=0))")
    asserts.assert_that(Outer({"v": 1})).is_equal_to(Outer({"v": 1}, Inner(0)))
    asserts.assert_that(_error(lambda: Outer({"v": "1"}))).is_equal_to(
        "TypeError: 'v' must be <class 'int'> (got \"1\" that is a <class 'str'>).",
    )


def TestValidate_test_success():
    """
    If the validator succeeds, nothing gets raised.
    """
    C = make_class("C", {"x": attr.ib(validator=lambda *a: None), "y": attr.ib()})
    validate(C(1, 2))


def TestValidate_test_propagates():
    """
    The exception of the validator is handed through.
    """
    def raiser(_, __, value):
        if value == 42:
            fail("FloatingPointError")

    C = make_class("C", {"x": attr.ib(validator=raiser)})
    i = C(1)
    i.x = 42

    asserts.assert_fails(lambda: validate(i), "FloatingPointError")


def TestValidate_test_run_validators():
    """
    Setting `_run_validators` to False prevents validators from running.
    """
    attr.set_run_validators(False)

    def raiser(_, __, ___):
        fail("Exception: obj")

    C = make_class("C", {"x": attr.ib(validator=raiser)})
    c = C(1)
    validate(c)
    asserts.assert_that(c.x).is_equal_to(1)
    attr.set_run_validators(True)

    asserts.assert_fails(lambda: validate(c), "Exception: obj")
    asserts.assert_fails(lambda: C(1), "Exception: obj")


def TestValidate_test_multiple_validators():
    """
    If a list is passed as a validator, all of its items are treated as one
    and must pass.
    """
    def v1(_, __, value):
        if value == 23:
            fail("TypeError: omg")

    def v2(_, __, value):
        if value == 42:
            fail("ValueError: omg")

    C = make_class("C", {"x": attr.ib(validator=[v1, v2])})

    validate(C(1))

    asserts.assert_that(_error(lambda: C(23))).is_equal_to("TypeError: omg")
    asserts.assert_that(_error(lambda: C(42))).is_equal_to("ValueError: omg")


def TestValidate_test_multiple_empty():
    """
    Empty list/tuple for validator is the same as None.
    """
    C1 = make_class("C", {"x": attr.ib(validator=[])})
    C2 = make_class("C", {"x": attr.ib(validator=None)})

    # attrs compares the generated source; Larky checks both run.
    asserts.assert_that(C1(1).x).is_equal_to(C2(1).x)


def TestMetadata_test_metadata():
    """
    If metadata that is not None is passed, it is used.

    This is necessary for coverage because the previous test is
    hypothesis-based.
    """
    md = {}
    a = attr.ib(metadata=md)

    asserts.assert_true(a.metadata == md)
    C = make_class("C", {"x": attr.ib(metadata={"k": "v"}), "y": attr.ib()})
    asserts.assert_that(fields(C).x.metadata).is_equal_to({"k": "v"})
    asserts.assert_that(fields(C).y.metadata).is_equal_to({})


def TestInitAlias_test_default_and_specify():
    """
    alias is present on the Attributes returned from attr.fields.
    If left unspecified, it defaults to standard private-attribute
    handling.  If specified, it passes through the explicit alias.
    """
    # alias is None by default on _CountingAttr
    default_counting = attr.ib()
    asserts.assert_that(default_counting.alias).is_equal_to(None)

    override_counting = attr.ib(alias="specified")
    asserts.assert_that(override_counting.alias).is_equal_to("specified")

    Cases = _attrs_class("Cases", {
        "public_default": attr.ib(),
        "_private_default": attr.ib(),
        "__dunder_default__": attr.ib(),
        "public_override": attr.ib(alias="public"),
        "_private_override": attr.ib(alias="_private"),
        "__dunder_override__": attr.ib(alias="__dunder__"),
    })

    cases = attr.fields_dict(Cases)

    # Default applies private-name mangling logic
    asserts.assert_that(cases["public_default"].name).is_equal_to("public_default")
    asserts.assert_that(cases["public_default"].alias).is_equal_to("public_default")
    asserts.assert_that(cases["_private_default"].name).is_equal_to("_private_default")
    asserts.assert_that(cases["_private_default"].alias).is_equal_to("private_default")
    asserts.assert_that(cases["__dunder_default__"].name).is_equal_to("__dunder_default__")
    asserts.assert_that(cases["__dunder_default__"].alias).is_equal_to("dunder_default__")

    # Override is passed through
    asserts.assert_that(cases["public_override"].name).is_equal_to("public_override")
    asserts.assert_that(cases["public_override"].alias).is_equal_to("public")
    asserts.assert_that(cases["_private_override"].name).is_equal_to("_private_override")
    asserts.assert_that(cases["_private_override"].alias).is_equal_to("_private")
    asserts.assert_that(cases["__dunder_override__"].name).is_equal_to("__dunder_override__")
    asserts.assert_that(cases["__dunder_override__"].alias).is_equal_to("__dunder__")

    # And aliases are applied to the __init__ signature
    example = Cases(
        public_default=1,
        private_default=2,
        dunder_default__=3,
        public=4,
        _private=5,
        __dunder__=6,
    )

    asserts.assert_that(example.public_default).is_equal_to(1)
    asserts.assert_that(example._private_default).is_equal_to(2)
    asserts.assert_that(example.__dunder_default__).is_equal_to(3)
    asserts.assert_that(example.public_override).is_equal_to(4)
    asserts.assert_that(example._private_override).is_equal_to(5)
    asserts.assert_that(example.__dunder_override__).is_equal_to(6)


def TestInitAlias_test_evolve():
    """
    attr.evolve uses Attribute.alias to determine parameter names.
    """
    # Starlark has no name mangling: attrs' `__mangled` is `_EvolveCase__mangled`.
    EvolveCase = _attrs_class("EvolveCase", {
        "_override": attr.ib(alias="_override"),
        "_EvolveCase__mangled": attr.ib(),
        "__dunder__": attr.ib(),
    })

    org = EvolveCase(1, 2, 3)

    asserts.assert_that(attr.evolve(org)).is_equal_to(org)

    # evolve uses the alias to match __init__ signature
    asserts.assert_that(attr.evolve(org, _override=0)).is_equal_to(EvolveCase(0, 2, 3))

    # and properly passes through dunders and mangles
    asserts.assert_that(attr.evolve(org, EvolveCase__mangled=4, dunder__=5)).is_equal_to(EvolveCase(1, 4, 5))


def TestMakeOrder_test_subclasses_cannot_be_compared():
    """
    Calling comparison methods on subclasses raises a TypeError.
    We use the actual operation so we get an error raised.
    """
    A = _attrs_class("A", {"a": attr.ib()})
    B = _attrs_class("B", {}, bases=(A,))

    a = A(42)
    b = B(42)

    asserts.assert_true(a <= a)
    asserts.assert_true(a >= a)
    asserts.assert_true(not a < a)
    asserts.assert_true(not a > a)

    # attrs' methods return NotImplemented for another class, so the
    # operators raise TypeError. Larky's methods raise it directly. Larky
    # orders objects by calling __lt__ first, so the message may name '<'.
    msg = "TypeError: '[<>]=?' not supported between instances of 'A' and 'B'"
    asserts.assert_fails(lambda: a <= b, msg)
    asserts.assert_fails(lambda: a >= b, msg)
    asserts.assert_fails(lambda: a < b, msg)
    asserts.assert_fails(lambda: a > b, msg)
    asserts.assert_fails(lambda: a.__gt__(b), "TypeError: '>' not supported between instances of 'A' and 'B'")


def TestMakeOrder_test_order_by_fields():
    """
    Larky: ordering compares the tuples of field values, honoring order=False
    and order keys.
    """
    C = make_class("C", {"a": attr.ib(), "b": attr.ib(order=False), "c": attr.ib(order=lambda v: -v)})
    asserts.assert_true(C(1, 9, 0) < C(2, 0, 0))
    asserts.assert_true(C(1, 0, 5) < C(1, 9, 4))
    asserts.assert_that(sorted([C(2, 0, 0), C(1, 0, 0)])[0].a).is_equal_to(1)
    asserts.assert_true(C(1, 0, 0) <= C(1, 5, 0))


def TestDetermineAttribEqOrder_test_eq_callable_order_boolean():
    """
    eq=callable or order=callable need to transformed into eq/eq_key
    or order/order_key.
    """
    lower = lambda s: s.lower()
    a = attr.ib(eq=lower, order=False)
    asserts.assert_that((a.eq, a.eq_key, a.order, a.order_key)).is_equal_to((True, lower, False, None))


def TestDetermineAttribEqOrder_test_eq_callable_order_callable():
    """
    eq=callable or order=callable need to transformed into eq/eq_key
    or order/order_key.
    """
    lower = lambda s: s.lower()
    a = attr.ib(eq=lower, order=abs)
    asserts.assert_that((a.eq, a.eq_key, a.order, a.order_key)).is_equal_to((True, lower, True, abs))


def TestDetermineAttribEqOrder_test_order_without_eq():
    """
    eq=False, order=True raises a meaningful ValueError.
    """
    asserts.assert_fails(lambda: attr.ib(eq=False, order=True), "ValueError: `order` can only be True if `eq` is True too")
    asserts.assert_fails(lambda: make_class("C", [], eq=False, order=True), "ValueError: `order` can only be True if `eq` is True too")


def TestDetermineAttribEqOrder_test_mix():
    """
    If cmp is not None, eq and order must be None and vice versa.
    """
    asserts.assert_fails(lambda: attr.ib(cmp=True, eq=True), "ValueError: Don't mix `cmp` with `eq' and `order`")
    asserts.assert_fails(lambda: make_class("C", [], cmp=False, order=True), "ValueError: Don't mix `cmp` with `eq' and `order`")


def TestDetermineAttribEqOrder_test_eq_key():
    """
    Larky: an eq key is used by the generated __eq__.
    """
    C = make_class("C", {"s": attr.ib(eq=lambda s: s.lower())})
    asserts.assert_that(C("A")).is_equal_to(C("a"))
    asserts.assert_true(C("A") != C("b"))


def TestAutoDetect_test_detect_auto_init():
    """
    If auto_detect=True and an __init__ exists, don't write one.
    """
    def __init__(self):
        self.x = 42

    C = _attrs_class("C", {"x": attr.ib(), "__init__": __init__}, auto_detect=True)

    asserts.assert_that(C().x).is_equal_to(42)


def TestAutoDetect_test_detect_auto_repr():
    """
    If auto_detect=True and an __repr__ exists, don't write one.
    """
    C = _attrs_class("C", {"x": attr.ib(), "__repr__": lambda self: "hi"}, auto_detect=True)

    asserts.assert_that(repr(C(42))).is_equal_to("hi")


def TestAutoDetect_test_detect_auto_eq():
    """
    If auto_detect=True and an __eq__ or an __ne__, exist, don't write one.
    """
    def __eq__(self, o):
        fail("ValueError: worked")

    C = _attrs_class("C", {"x": attr.ib(), "__eq__": __eq__}, auto_detect=True)

    asserts.assert_fails(lambda: C(1) == C(1), "ValueError: worked")


def TestAutoDetect_test_override_init():
    """
    If init=True is passed, ignore __init__.
    """
    def __init__(self):
        fail("ValueError: user init")

    C = _attrs_class("C", {"x": attr.ib(), "__init__": __init__}, init=True, auto_detect=True)

    asserts.assert_that(C(1).x).is_equal_to(1)


def TestAutoDetect_test_attrs_init():
    """
    Larky: with init=False, attrs writes __attrs_init__ for a custom __init__.
    """
    def __init__(self, x):
        self.__attrs_init__(x * 2)

    C = _attrs_class("C", {"x": attr.ib(validator=instance_of(int)), "__init__": __init__}, init=False)

    asserts.assert_that(C(2).x).is_equal_to(4)
    asserts.assert_fails(lambda: C("a"), "TypeError: 'x' must be <class 'int'>")


def _testsuite():
    _suite = unittest.TestSuite()
    for t in [
        TestCountingAttr_test_returns_Attr,
        TestCountingAttr_test_validators_lists_to_wrapped_tuples,
        TestCountingAttr_test_validator_decorator_single,
        TestCountingAttr_test_validator_decorator,
        TestCountingAttr_test_default_decorator_already_set,
        TestCountingAttr_test_default_decorator_sets,
        TestTransformAttrs_test_conflicting_defaults,
        TestTransformAttrs_test_kw_only,
        TestTransformAttrs_test_these,
        TestTransformAttrs_test_these_leave_body,
        TestTransformAttrs_test_these_ordered,
        TestTransformAttrs_test_inherited,
        TestAttributes_test_pre_init,
        TestAttributes_test_post_init,
        TestAttributes_test_pre_post_init_order,
        TestAttributes_test_types,
        TestAttributes_test_clean_class,
        TestAttributes_test_factory_sugar,
        TestAttributes_test_sugar_factory_mutex,
        TestAttributes_test_sugar_callable,
        TestAttributes_test_factory_is_called_per_instance,
        TestKeywordOnlyAttributes_test_adds_keyword_only_arguments,
        TestKeywordOnlyAttributes_test_ignores_kw_only_when_init_is_false,
        TestKeywordOnlyAttributes_test_keyword_only_attributes_presence,
        TestKeywordOnlyAttributes_test_keyword_only_attributes_unexpected,
        TestKeywordOnlyAttributes_test_keyword_only_attributes_can_come_in_any_order,
        TestKeywordOnlyAttributes_test_keyword_only_attributes_allow_subclassing,
        TestKeywordOnlyAttributes_test_keyword_only_class_level,
        TestKeywordOnlyAttributes_test_init_false_attribute_after_keyword_attribute,
        TestKeywordOnlyAttributes_test_attr_level_kw_only_and_class_level_kw_only,
        TestKeywordOnlyAttributes_test_kw_only_inheritance,
        TestKeywordOnlyAttributes_test_kw_only_inheritance_force_kw_only,
        TestMakeClass_test_simple,
        TestMakeClass_test_dict,
        TestMakeClass_test_attr_args,
        TestMakeClass_test_catches_wrong_attrs_type,
        TestMakeClass_test_bases,
        TestMakeClass_test_additional_class_body,
        TestMakeClass_test_clean_class,
        TestMakeClass_test_make_class_ordered,
        TestFields_test_instance,
        TestFields_test_handler_non_attrs_instance,
        TestFields_test_handler_non_attrs_class,
        TestFields_test_fields,
        TestFields_test_fields_properties,
        TestFieldsDict_test_instance,
        TestFieldsDict_test_handler_non_attrs_class,
        TestFieldsDict_test_fields_dict,
        TestConverter_test_converter,
        TestConverter_test_convert_property,
        TestConverter_test_converter_factory_property,
        TestConverter_test_convert_before_validate,
        TestConverter_test_frozen,
        TestConverter_test_nested_attrs_class,
        TestValidate_test_success,
        TestValidate_test_propagates,
        TestValidate_test_run_validators,
        TestValidate_test_multiple_validators,
        TestValidate_test_multiple_empty,
        TestMetadata_test_metadata,
        TestInitAlias_test_default_and_specify,
        TestInitAlias_test_evolve,
        TestMakeOrder_test_subclasses_cannot_be_compared,
        TestMakeOrder_test_order_by_fields,
        TestDetermineAttribEqOrder_test_eq_callable_order_boolean,
        TestDetermineAttribEqOrder_test_eq_callable_order_callable,
        TestDetermineAttribEqOrder_test_order_without_eq,
        TestDetermineAttribEqOrder_test_mix,
        TestDetermineAttribEqOrder_test_eq_key,
        TestAutoDetect_test_detect_auto_init,
        TestAutoDetect_test_detect_auto_repr,
        TestAutoDetect_test_detect_auto_eq,
        TestAutoDetect_test_override_init,
        TestAutoDetect_test_attrs_init,
    ]:
        _suite.addTest(unittest.FunctionTestCase(t))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
