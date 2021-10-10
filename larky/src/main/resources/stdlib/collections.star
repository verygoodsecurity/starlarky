# Copyright 2017 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Starlark module containing functions that operate on collections.

"""
load("@stdlib/larky", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//jcollections", _collections="jcollections")
load("@stdlib//operator", operator="operator")
load("@stdlib//sets", sets="sets")
load("@stdlib//re", re="re")
load("@stdlib//builtins", builtins="builtins")
load("@vendor//option/result", Result="Result", Ok="Ok", Error="Error", safe="safe")

map = builtins.map
_tuplegetter = lambda index, _: larky.property(operator.itemgetter(index))

kwlist = [
    'False',
    'None',
    'True',
    'and',
    'as',
    'assert',
    'async',
    'await',
    'break',
    'class',
    'continue',
    'def',
    'del',
    'elif',
    'else',
    'except',
    'finally',
    'for',
    'from',
    'global',
    'if',
    'import',
    'in',
    'is',
    'lambda',
    'nonlocal',
    'not',
    'or',
    'pass',
    'raise',
    'return',
    'try',
    'while',
    'with',
    'yield'
]

softkwlist = [
    '_',
    'case',
    'match'
]

iskeyword = sets.Set(kwlist).__contains__
issoftkeyword = sets.Set(softkwlist).__contains__

_name_re = re.compile(r"[a-zA-Z_][a-zA-Z0-9_]*$")
def isidentifier(s, dotted=False):
    if not dotted:
        return bool(_name_re.match(s))

    for a in s.split("."):
        if not isidentifier(a):
            return False
    return True


_iskeyword = iskeyword

def namedtuple(typename, field_names, rename=False, defaults=None, module=None):
    """Returns a new subclass of tuple with named fields.
    >>> Point = namedtuple('Point', ['x', 'y'])
    >>> Point.__doc__                   # docstring for the new class
    'Point(x, y)'
    >>> p = Point(11, y=22)             # instantiate with positional args or keywords
    >>> p[0] + p[1]                     # indexable like a plain tuple
    33
    >>> x, y = p                        # unpack like a regular tuple
    >>> x, y
    (11, 22)
    >>> p.x + p.y                       # fields also accessible by name
    33
    >>> d = p._asdict()                 # convert to a dictionary
    >>> d['x']
    11
    >>> Point(**d)                      # convert from a dictionary
    Point(x=11, y=22)
    >>> p._replace(x=100)               # _replace() is like str.replace() but targets named fields
    Point(x=100, y=22)
    """

    # Validate the field names.  At the user's option, either generate an error
    # message or automatically replace the field name with a valid name.
    if types.is_string(field_names):
        field_names = re.split(r'\s+', field_names.replace(',', ' '))
    field_names = list(builtins.map(str, field_names))
    __name__ = str(typename)

    if rename:
        seen = sets.Set()
        for index, name in enumerate(field_names):
            if (not isidentifier(name)
                or _iskeyword(name)
                or name.startswith('_')
                or operator.contains(seen, name)):
                field_names[index] = '_{}'.format(index)
            seen.add(name)

    for name in [typename] + field_names:
        if not types.is_string(name):
            return Error('TypeError: Type names and field names must be strings').unwrap()
        if not isidentifier(name):
            return Error('ValueError: Type names and field names must be valid ' +
                         'identifiers: %r' % name).unwrap()
        if _iskeyword(name):
            return Error('ValueError: Type names and field names cannot be a  ' +
                         'keyword: %r' % name).unwrap()

    seen = sets.Set()
    for name in field_names:
        if name.startswith('_') and not rename:
            return Error('Field names cannot start with an underscore: ' +
                         '%r' % name).unwrap()
        if operator.contains(seen, name):
            return Error('ValueError: Encountered duplicate field name: %r' % name).unwrap()
        seen.add(name)

    field_defaults = {}
    if defaults != None:
        defaults = tuple(defaults)
        if len(defaults) > len(field_names):
            return Error('TypeError: Got more default values than field names').unwrap()
        field_defaults = dict(reversed(list(zip(reversed(field_names),
                                                reversed(defaults)))))

    result = _collections.namedtuple(typename, tuple(field_names))
    return result


def _after_each(separator, iterable):
    """Inserts `separator` after each item in `iterable`.

    Args:
      separator: The value to insert after each item in `iterable`.
      iterable: The list into which to intersperse the separator.

    Returns:
      A new list with `separator` after each item in `iterable`.
    """
    result = []
    for x in iterable:
        result.append(x)
        result.append(separator)

    return result

def _before_each(separator, iterable):
    """Inserts `separator` before each item in `iterable`.

    Args:
      separator: The value to insert before each item in `iterable`.
      iterable: The list into which to intersperse the separator.

    Returns:
      A new list with `separator` before each item in `iterable`.
    """
    result = []
    for x in iterable:
        result.append(separator)
        result.append(x)

    return result

def _uniq(iterable):
    """Returns a list of unique elements in `iterable`.

    Requires all the elements to be hashable.

    Args:
      iterable: An iterable to filter.

    Returns:
      A new list with all unique elements from `iterable`.
    """
    unique_elements = {element: None for element in iterable}

    # list() used here for python3 compatibility.
    # TODO(bazel-team): Remove when testing frameworks no longer require python compatibility.
    return list(unique_elements.keys())


collections = larky.struct(
    after_each = _after_each,
    before_each = _before_each,
    uniq = _uniq,
    namedtuple = namedtuple,
)
