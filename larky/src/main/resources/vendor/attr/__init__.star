# Ported from attrs 26.1.0 (7bfc49e9b22d5ba25b6e429524c3d49fee27cb36): src/attr/__init__.py. See larky/src/main/resources/VENDORED.md.
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
r"""
Classes Without Boilerplate

A Larky port of the `attrs <https://www.attrs.org/>`_ package (26.1.0): define
classes from a list of fields, with generated ``__init__``, ``__repr__``,
equality and ordering, and validators and converters that run on
initialization::

    load("@vendor//attr", "attr")

    C = attr.make_class("C", {
        "x": attr.ib(validator=attr.validators.instance_of(int)),
        "y": attr.ib(default="a", validator=attr.validators.in_(["a", "b"])),
    })
    C(1)            # C(x=1, y="a")
    C("1")          # fails: TypeError: 'x' must be <class 'int'> (got "1" that is a <class 'str'>).

Starlark has no ``class`` statement and no decorator syntax, so classes are
made with `make_class` (upstream API), or by calling `attr.s` /
`attrs.define` on a class built with ``types.new_class`` whose namespace
holds the ``attr.ib()``\ s: ``C = attr.s(C)``. Each returns a new class.

Supported: ``attr.ib``/``attrib``/``attr`` and ``attrs.field`` (default,
factory, validator incl. lists and the ``.validator``/``.default`` decorator
methods, converter, repr incl. a callable, eq/order incl. key callables,
init, kw_only, alias, metadata, type, on_setattr); ``attr.s``/``attrs``/
``attributes``, ``attrs.define``/``mutable``/``frozen`` (these, repr, eq,
order, init, str, frozen, kw_only, force_kw_only, auto_detect, on_setattr,
field_transformer); ``make_class``; ``Factory``; ``NOTHING``; ``Attribute``
(with ``evolve``); ``fields`` (name access, e.g. ``fields(C).x``),
``fields_dict``, ``has``, ``validate``, ``evolve``, ``asdict``, ``astuple``;
``__attrs_pre_init__`` (called without arguments) and
``__attrs_post_init__``; ``validators`` (``instance_of``, ``matches_re``,
``optional``, ``in_``, ``and_``, ``or_``, ``deep_iterable``,
``deep_mapping``, ``lt``/``le``/``gt``/``ge``, ``min_len``/``max_len``,
``is_callable``, ``set_disabled``/``get_disabled``); ``setters``
(``frozen``, ``validate``, ``convert``, ``pipe``, ``NO_OP``);
``set_run_validators``/``get_run_validators``.

Differences from attrs:

- Errors are Larky failures whose message starts with the name of the
  exception attrs raises (``TypeError: ...``, ``ValueError: ...``,
  ``FrozenInstanceError: can't set attribute``, ``NotAnAttrsClassError: ...``).
  There are no exception objects, so their extra arguments are not available.
- Values in messages and reprs use Larky's repr (strings in double quotes);
  builtin types print as Python does (``<class 'int'>``), and classes without
  their module (``<class 'C'>``).
- No annotations: fields are the ``attr.ib()``/``field()`` entries, in the
  order they were created (as attrs orders them without annotations).
- Hashing: instances hash by identity, so ``hash=True``, ``unsafe_hash=True``
  and ``cache_hash=True`` fail.
- ``slots``, ``weakref_slot``, ``getstate_setstate``, ``match_args``,
  ``auto_exc``, ``collect_by_mro`` and ``repr_ns`` are accepted and have no
  effect; base class fields are always collected in MRO order.
- Frozen classes protect their fields; assigning another attribute to a
  frozen instance is not prevented. Reading a field from the class returns a
  descriptor (as for a slotted class).
- ``set_disabled``/``set_run_validators`` apply to the current evaluation, not
  the whole process.
- Not ported: ``attrs.converters`` and ``Converter``, ``validators.not_``
  and ``validators.disabled``, ``cmp_using``, ``resolve_types``, ``assoc``,
  ``inspect``, ``filters``, ``exceptions``, ``__attrs_init_subclass__``,
  ``auto_exc`` exception classes, pickling and ``copy.replace``.
"""
load("@stdlib//larky", larky="larky")
load(
    "@vendor//attr/_make",
    "NOTHING",
    "Attribute",
    "Factory",
    "asdict",
    "astuple",
    "attrib",
    "attrs",
    "evolve",
    "fields",
    "fields_dict",
    "get_run_validators",
    "has",
    "make_class",
    "set_run_validators",
    "validate",
)
load("@vendor//attr/_next_gen", "define", "field", "frozen", "mutable")
load("@vendor//attr/setters", "setters")
load("@vendor//attr/validators", "validators")

s = attrs
attributes = attrs
ib = attrib

attr = larky.struct(
    __name__="attr",
    NOTHING=NOTHING,
    Attribute=Attribute,
    Factory=Factory,
    asdict=asdict,
    astuple=astuple,
    attr=attrib,
    attrib=attrib,
    attributes=attrs,
    attrs=attrs,
    define=define,
    evolve=evolve,
    field=field,
    fields=fields,
    fields_dict=fields_dict,
    frozen=frozen,
    get_run_validators=get_run_validators,
    has=has,
    ib=attrib,
    make_class=make_class,
    mutable=mutable,
    s=attrs,
    set_run_validators=set_run_validators,
    setters=setters,
    validate=validate,
    validators=validators,
)
