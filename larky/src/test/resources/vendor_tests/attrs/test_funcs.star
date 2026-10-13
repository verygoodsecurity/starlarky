# Ported from attrs 26.1.0 (7bfc49e9b22d5ba25b6e429524c3d49fee27cb36): tests/test_funcs.py. See larky/src/main/resources/VENDORED.md.
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
Tests for `attr._funcs`.

A subset ported with py2star and adapted: hypothesis-generated classes are a
fixed two-field class `C(x, y)`, and classes are built with `attr.s` on
`types.new_class`. Not ported: `assoc` (not in the Larky port), named tuples,
frozensets and custom mapping types.
"""
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@vendor//asserts", asserts="asserts")
load("@vendor//option/result", safe="safe")
load("@vendor//attr", attr="attr")
load("@vendor//attrs", attrs="attrs")

asdict = attr.asdict
astuple = attr.astuple
evolve = attr.evolve
fields = attr.fields
has = attr.has
instance_of = attr.validators.instance_of


def _class(name, body, bases=()):
    return types.new_class(name, bases, None, lambda ns: ns.update(body))


def _attrs_class(name, body, bases=(), **kwargs):
    return attr.s(**kwargs)(_class(name, body, bases))


def _error(f):
    r = safe(f)()
    asserts.assert_true(r.is_err)
    return "%s" % (r._val,)


# tests/test_funcs.py's `C` fixture.
C = _attrs_class("C", {"x": attr.ib(), "y": attr.ib()})


def TestAsDict_test_shallow():
    """
    Shallow asdict returns correct dict.
    """
    asserts.assert_that(asdict(C(x=1, y=2), False)).is_equal_to({"x": 1, "y": 2})


def TestAsDict_test_recurse():
    """
    Deep asdict returns correct dict.
    """
    asserts.assert_that(asdict(C(C(1, 2), C(3, 4)))).is_equal_to({"x": {"x": 1, "y": 2}, "y": {"x": 3, "y": 4}})


def TestAsDict_test_nested_lists():
    """
    Test unstructuring deeply nested lists.
    """
    inner = C(1, 2)
    outer = C([[inner]], None)

    asserts.assert_that(asdict(outer)).is_equal_to({"x": [[{"x": 1, "y": 2}]], "y": None})


def TestAsDict_test_nested_dicts():
    """
    Test unstructuring deeply nested dictionaries.
    """
    inner = C(1, 2)
    outer = C({1: {2: inner}}, None)

    asserts.assert_that(asdict(outer)).is_equal_to({"x": {1: {2: {"x": 1, "y": 2}}}, "y": None})


def TestAsDict_test_filter():
    """
    Attributes that are supposed to be skipped are skipped.
    """
    asserts.assert_that(asdict(
        C(C(1, 2), C(3, 4)),
        filter=lambda a, v: a.name != "y",
    )).is_equal_to({"x": {"x": 1}})


def TestAsDict_test_lists_tuples():
    """
    If recurse is True, also recurse into lists.
    """
    for container in [list, tuple]:
        asserts.assert_that(asdict(C(1, container([C(2, 3), C(4, 5), "a"])))).is_equal_to({
            "x": 1,
            "y": [{"x": 2, "y": 3}, {"x": 4, "y": 5}, "a"],
        })


def TestAsDict_test_lists_tuples_retain_type():
    """
    If recurse and retain_collection_types are True, also recurse
    into lists and do not convert them into list.
    """
    for container in [list, tuple]:
        asserts.assert_that(asdict(
            C(1, container([C(2, 3), C(4, 5), "a"])),
            retain_collection_types=True,
        )).is_equal_to({
            "x": 1,
            "y": container([{"x": 2, "y": 3}, {"x": 4, "y": 5}, "a"]),
        })


def TestAsDict_test_dicts():
    """
    If recurse is True, also recurse into dicts.
    """
    res = asdict(C(1, {"a": C(4, 5)}))
    asserts.assert_that(res).is_equal_to({"x": 1, "y": {"a": {"x": 4, "y": 5}}})
    asserts.assert_that(type(res)).is_equal_to("dict")


def TestAsDict_test_roundtrip():
    """
    Test dumping to dicts and back for Hypothesis-generated classes.
    """
    instance = C(1, [2])
    dict_instance = asdict(instance)
    roundtrip_instance = C(**dict_instance)
    asserts.assert_that(roundtrip_instance).is_equal_to(instance)


def TestAsDict_test_asdict_preserve_order():
    """
    Field order should be preserved when dumping to an ordered_dict.
    """
    D = attr.make_class("D", ["z", "a", "m"])
    dict_instance = asdict(D(1, 2, 3), dict_factory=dict)

    asserts.assert_that(list(dict_instance.keys())).is_equal_to([a.name for a in fields(D)])


def TestAsDict_test_retain_keys_are_tuples():
    """
    retain_collect_types also retains keys.
    """
    A = _attrs_class("A", {"a": attr.ib()})

    instance = A({(1,): 1})

    asserts.assert_that(attr.asdict(instance, retain_collection_types=True)).is_equal_to({"a": {(1,): 1}})


def TestAsDict_test_tuple_keys():
    """
    If a key is collection type, retain_collection_types is False,
     the key is serialized as a tuple.
    """
    A = _attrs_class("A", {"a": attr.ib()})

    instance = A({(1,): 1})

    asserts.assert_that(attr.asdict(instance)).is_equal_to({"a": {(1,): 1}})


def TestAsDict_test_value_serializer():
    """
    Larky: value_serializer sees every attribute and dict key/value.
    """
    seen = []

    def serializer(inst, field, value):
        seen.append((field.name if field != None else None, value))
        return value * 10 if type(value) == "int" else value

    asserts.assert_that(asdict(C(1, {"k": 2}), value_serializer=serializer)).is_equal_to({"x": 10, "y": {"k": 20}})
    asserts.assert_that(seen).is_equal_to([("x", 1), ("y", {"k": 2}), (None, "k"), (None, 2)])


def TestAsTuple_test_shallow():
    """
    Shallow astuple returns correct dict.
    """
    asserts.assert_that(astuple(C(x=1, y=2), False)).is_equal_to((1, 2))
    asserts.assert_that(astuple(C(x=1, y=2), False, tuple_factory=list)).is_equal_to([1, 2])


def TestAsTuple_test_recurse():
    """
    Deep astuple returns correct tuple.
    """
    asserts.assert_that(astuple(C(C(1, 2), C(3, 4)))).is_equal_to(((1, 2), (3, 4)))


def TestAsTuple_test_filter():
    """
    Attributes that are supposed to be skipped are skipped.
    """
    asserts.assert_that(astuple(
        C(C(1, 2), C(3, 4)), filter=lambda a, v: a.name != "y",
    )).is_equal_to(((1,),))


def TestAsTuple_test_lists_tuples():
    """
    If recurse is True, also recurse into lists.
    """
    for container in [list, tuple]:
        asserts.assert_that(astuple(C(1, container([C(2, 3), C(4, 5), "a"])))).is_equal_to(
            (1, [(2, 3), (4, 5), "a"]),
        )


def TestAsTuple_test_dicts():
    """
    If recurse is True, also recurse into dicts.
    """
    res = astuple(C(1, {"a": C(4, 5)}))
    asserts.assert_that(res).is_equal_to((1, {"a": (4, 5)}))
    asserts.assert_that(type(res)).is_equal_to("tuple")


def TestAsTuple_test_lists_tuples_retain_type():
    """
    If recurse and retain_collection_types are True, also recurse
    into lists and do not convert them into list.
    """
    for container in [list, tuple]:
        asserts.assert_that(astuple(
            C(1, container([C(2, 3), C(4, 5), "a"])),
            retain_collection_types=True,
        )).is_equal_to((1, container([(2, 3), (4, 5), "a"])))


def TestAsTuple_test_roundtrip():
    """
    Test dumping to tuple and back for Hypothesis-generated classes.
    """
    instance = C(1, "a")
    tuple_instance = astuple(instance)
    roundtrip_instance = C(*tuple_instance)
    asserts.assert_that(roundtrip_instance).is_equal_to(instance)


def TestHas_test_positive():
    """
    Returns `True` on decorated classes.
    """
    asserts.assert_true(has(C))


def TestHas_test_positive_empty():
    """
    Returns `True` on decorated classes even if there are no attributes.
    """
    D = _attrs_class("D", {})

    asserts.assert_true(has(D))


def TestHas_test_negative():
    """
    Returns `False` on non-decorated classes.
    """
    asserts.assert_false(has(object))
    asserts.assert_false(has(_class("E", {})))


def TestEvolve_test_empty():
    """
    Empty classes without changes get copied.
    """
    for slots in [True, False]:
        for frozen in [True, False]:
            D = _attrs_class("D", {}, slots=slots, frozen=frozen)

            i1 = D()
            i2 = evolve(i1)

            asserts.assert_true(i1 != None)
            asserts.assert_that(i2).is_equal_to(i1)


def TestEvolve_test_no_changes():
    """
    No changes means a verbatim copy.
    """
    i1 = C(1, 2)
    i2 = evolve(i1)

    asserts.assert_that(i2).is_equal_to(i1)


def TestEvolve_test_change():
    """
    Changes work.
    """
    original = C(1, 2)
    changed = evolve(original, y=5)
    asserts.assert_that(changed.x).is_equal_to(1)
    asserts.assert_that(changed.y).is_equal_to(5)
    asserts.assert_that(original.y).is_equal_to(2)


def TestEvolve_test_unknown():
    """
    Wanting to change an unknown attribute raises an
    AttrsAttributeNotFoundError.
    """
    asserts.assert_true(_error(lambda: evolve(C(1, 2), aaaa=2)).endswith(
        "__init__() got an unexpected keyword argument 'aaaa'",
    ))


def TestEvolve_test_validator_failure():
    """
    TypeError isn't swallowed when validation fails within evolve.
    """
    D = _attrs_class("D", {"a": attr.ib(validator=instance_of(int))})

    m = _error(lambda: evolve(D(a=1), a="some string"))
    asserts.assert_true(m.startswith("TypeError: 'a' must be <class 'int'>"))


def TestEvolve_test_private():
    """
    evolve() acts as `__init__` with regards to private attributes.
    """
    D = _attrs_class("D", {"_a": attr.ib()})

    asserts.assert_that(evolve(D(1), a=2)._a).is_equal_to(2)

    asserts.assert_fails(lambda: evolve(D(1), _a=2), "TypeError")

    asserts.assert_fails(lambda: evolve(D(1), a=3, _a=2), "TypeError")


def TestEvolve_test_non_init_attrs():
    """
    evolve() handles `init=False` attributes.
    """
    D = _attrs_class("D", {"a": attr.ib(), "b": attr.ib(init=False, default=0)})

    asserts.assert_that(evolve(D(1), a=2).a).is_equal_to(2)


def TestEvolve_test_regression_attrs_classes():
    """
    evolve() can evolve fields that are instances of attrs classes.
    """
    Cls1 = _attrs_class("Cls1", {"param1": attr.ib()})
    Cls2 = _attrs_class("Cls2", {"param2": attr.ib()})

    obj2a = Cls2(param2="a")
    obj2b = Cls2(param2="b")

    obj1a = Cls1(param1=obj2a)

    asserts.assert_that(attr.evolve(obj1a, param1=obj2b)).is_equal_to(Cls1(param1=Cls2(param2="b")))


def TestEvolve_test_dicts():
    """
    evolve() can replace an attrs class instance with a dict.
    """
    Cls1 = _attrs_class("Cls1", {"param1": attr.ib()})
    Cls2 = _attrs_class("Cls2", {"param2": attr.ib()})

    obj2a = Cls2(param2="a")
    obj2b = {"foo": 42, "param2": 42}

    obj1a = Cls1(param1=obj2a)

    asserts.assert_that(attr.evolve(obj1a, param1=obj2b)).is_equal_to(Cls1({"foo": 42, "param2": 42}))


def TestEvolve_test_no_inst():
    """
    Missing inst argument raises a TypeError like Python would.
    """
    asserts.assert_fails(lambda: evolve(x=1), "TypeError: evolve\\(\\) takes 1 positional argument")


def TestEvolve_test_too_many_pos_args():
    """
    More than one positional argument raises a TypeError like Python would.
    """
    asserts.assert_that(_error(lambda: evolve(1, 2))).is_equal_to(
        "TypeError: evolve() takes 1 positional argument, but 2 were given",
    )


def TestEvolve_test_can_change_inst():
    """
    If the instance is passed by positional argument, a field named `inst`
    can be changed.
    """
    D = attrs.define(_class("D", {"inst": attrs.field()}))

    asserts.assert_that(evolve(D(23), inst=42)).is_equal_to(D(42))


def _testsuite():
    _suite = unittest.TestSuite()
    for t in [
        TestAsDict_test_shallow,
        TestAsDict_test_recurse,
        TestAsDict_test_nested_lists,
        TestAsDict_test_nested_dicts,
        TestAsDict_test_filter,
        TestAsDict_test_lists_tuples,
        TestAsDict_test_lists_tuples_retain_type,
        TestAsDict_test_dicts,
        TestAsDict_test_roundtrip,
        TestAsDict_test_asdict_preserve_order,
        TestAsDict_test_retain_keys_are_tuples,
        TestAsDict_test_tuple_keys,
        TestAsDict_test_value_serializer,
        TestAsTuple_test_shallow,
        TestAsTuple_test_recurse,
        TestAsTuple_test_filter,
        TestAsTuple_test_lists_tuples,
        TestAsTuple_test_dicts,
        TestAsTuple_test_lists_tuples_retain_type,
        TestAsTuple_test_roundtrip,
        TestHas_test_positive,
        TestHas_test_positive_empty,
        TestHas_test_negative,
        TestEvolve_test_empty,
        TestEvolve_test_no_changes,
        TestEvolve_test_change,
        TestEvolve_test_unknown,
        TestEvolve_test_validator_failure,
        TestEvolve_test_private,
        TestEvolve_test_non_init_attrs,
        TestEvolve_test_regression_attrs_classes,
        TestEvolve_test_dicts,
        TestEvolve_test_no_inst,
        TestEvolve_test_too_many_pos_args,
        TestEvolve_test_can_change_inst,
    ]:
        _suite.addTest(unittest.FunctionTestCase(t))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
