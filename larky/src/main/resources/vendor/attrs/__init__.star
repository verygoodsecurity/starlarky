# Ported from attrs 26.1.0 (7bfc49e9b22d5ba25b6e429524c3d49fee27cb36): src/attrs/__init__.py. See larky/src/main/resources/VENDORED.md.
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
The ``attrs`` namespace of attrs: `define`, `field` and the other APIs with
the newer defaults. See ``@vendor//attr`` for what the Larky port supports.

    load("@vendor//attrs", "attrs")

    C = attrs.make_class("C", {"x": attrs.field(validator=attrs.validators.gt(0))})
"""
load("@stdlib//larky", larky="larky")
load(
    "@vendor//attr/_make",
    "NOTHING",
    "Attribute",
    "Factory",
    "evolve",
    "fields",
    "fields_dict",
    "has",
    "make_class",
    "validate",
)
load("@vendor//attr/_next_gen", "asdict", "astuple", "define", "field", "frozen", "mutable")
load("@vendor//attr/setters", "setters")
load("@vendor//attr/validators", "validators")

attrs = larky.struct(
    __name__="attrs",
    NOTHING=NOTHING,
    Attribute=Attribute,
    Factory=Factory,
    asdict=asdict,
    astuple=astuple,
    define=define,
    evolve=evolve,
    field=field,
    fields=fields,
    fields_dict=fields_dict,
    frozen=frozen,
    has=has,
    make_class=make_class,
    mutable=mutable,
    setters=setters,
    validate=validate,
    validators=validators,
)
