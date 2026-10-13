# Ported from attrs 26.1.0 (7bfc49e9b22d5ba25b6e429524c3d49fee27cb36): src/attr/setters.py. See larky/src/main/resources/VENDORED.md.
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
Commonly used hooks for on_setattr.

Each hook is a callable ``hook(instance, attrib, new_value)`` returning the
value to store.
"""
load("@stdlib//larky", larky="larky")
load("@stdlib//jattrs", _jattrs="jattrs")


def pipe(*setters):
    """
    Run all *setters* and return the return value of the last one.
    """
    return _jattrs.setters_pipe(*setters)


frozen = _jattrs.setters_frozen
"""
Prevent an attribute to be modified.
"""

validate = _jattrs.setters_validate
"""
Run *attrib*'s validator on *new_value* if it has one.
"""

convert = _jattrs.setters_convert
"""
Run *attrib*'s converter -- if it has one -- on *new_value* and return the
result.
"""

NO_OP = _jattrs.setters_NO_OP
"""
Sentinel for disabling class-wide *on_setattr* hooks for certain attributes.
"""

setters = larky.struct(
    __name__="setters",
    pipe=pipe,
    frozen=frozen,
    validate=validate,
    convert=convert,
    NO_OP=NO_OP,
)
