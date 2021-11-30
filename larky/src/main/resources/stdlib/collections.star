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
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//operator", operator="operator")
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


"""High performance data structures

 - Copied from pypy repo
 - Copied and completed from the sandbox of CPython
   (nondist/sandbox/collections/pydeque.py rev 1.1, Raymond Hettinger)
"""



NotImplemented = larky.SENTINEL


def _thread_ident():
    return -1


n = 30
LFTLNK = n
RGTLNK = n + 1
BLOCKSIZ = n + 2


def deque(iterable=None, maxlen=None):

    self = larky.mutablestruct(__name__='deque', __class__=deque)

    def _maxlen():
        return self._maxlen
    self.maxlen = larky.property(_maxlen)

    def clear():
        self.right = [None] * BLOCKSIZ
        self.left = self.right
        self.rightndx = n // 2  # points to last written element
        self.leftndx = n // 2 + 1
        self.length = 0
        self.state = 0
    self.clear = clear

    def append(x):
        self.state += 1
        self.rightndx += 1
        if self.rightndx == n:
            newblock = [None] * BLOCKSIZ
            self.right[RGTLNK] = newblock
            newblock[LFTLNK] = self.right
            self.right = newblock
            self.rightndx = 0
        self.length += 1
        self.right[self.rightndx] = x
        if self.maxlen != None and self.length > self.maxlen:
            self.popleft()
    self.append = append

    def appendleft(x):
        self.state += 1
        self.leftndx -= 1
        if self.leftndx == -1:
            newblock = [None] * BLOCKSIZ
            self.left[LFTLNK] = newblock
            newblock[RGTLNK] = self.left
            self.left = newblock
            self.leftndx = n - 1
        self.length += 1
        self.left[self.leftndx] = x
        if self.maxlen != None and self.length > self.maxlen:
            self.pop()
    self.appendleft = appendleft

    def extend(iterable):
        if iterable == self:
            iterable = list(iterable)
        for elem in iterable:
            self.append(elem)
    self.extend = extend

    def extendleft(iterable):
        if iterable == self:
            iterable = list(iterable)
        for elem in iterable:
            self.appendleft(elem)
    self.extendleft = extendleft

    def pop():
        if self.left == self.right and self.leftndx > self.rightndx:
            return IndexError("IndexError: pop from an empty deque")
        x = self.right[self.rightndx]
        self.right[self.rightndx] = None
        self.length -= 1
        self.rightndx -= 1
        self.state += 1
        if self.rightndx == -1:
            prevblock = self.right[LFTLNK]
            if prevblock == None:
                # the deque has become empty; recenter instead of freeing block
                self.rightndx = n // 2
                self.leftndx = n // 2 + 1
            else:
                prevblock[RGTLNK] = None
                self.right[LFTLNK] = None
                self.right = prevblock
                self.rightndx = n - 1
        return x
    self.pop = pop

    def popleft():
        if self.left == self.right and self.leftndx > self.rightndx:
            return IndexError("IndexError: pop from an empty deque")
        x = self.left[self.leftndx]
        self.left[self.leftndx] = None
        self.length -= 1
        self.leftndx += 1
        self.state += 1
        if self.leftndx == n:
            prevblock = self.left[RGTLNK]
            if prevblock == None:
                # the deque has become empty; recenter instead of freeing block
                self.rightndx = n // 2
                self.leftndx = n // 2 + 1
            else:
                prevblock[LFTLNK] = None
                self.left[RGTLNK] = None
                self.left = prevblock
                self.leftndx = 0
        return x
    self.popleft = popleft

    def count(value):
        c = 0
        for item in self:
            if item == value:
                c += 1
        return c
    self.count = count

    def remove(value):
        # Need to be defensive for mutating comparisons
        for i in range(len(self)):
            if self[i] == value:
                operator.delitem(self, i)
                return
        fail("ValueError: deque.remove(x): x not in deque")
    self.remove = remove

    def rotate(n=1):
        length = len(self)
        if length == 0:
            return
        halflen = (length + 1) >> 1
        if n > halflen or n < -halflen:
            n %= length
            if n > halflen:
                n -= length
            elif n < -halflen:
                n += length
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if n <= 0:
                break
            self.appendleft(self.pop())
            n -= 1
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if n >= 0:
                break
            self.append(self.popleft())
            n += 1
    self.rotate = rotate

    def reverse():
        "reverse *IN PLACE*"
        leftblock = self.left
        rightblock = self.right
        leftindex = self.leftndx
        rightindex = self.rightndx
        for i in range(self.length // 2):
            if not (leftblock != rightblock or leftindex < rightindex):
                fail("assert leftblock != rightblock or leftindex < rightindex failed!")

            # Swap
            (rightblock[rightindex], leftblock[leftindex]) = (
                leftblock[leftindex],
                rightblock[rightindex],
            )

            # Advance left block/index pair
            leftindex += 1
            if leftindex == n:
                leftblock = leftblock[RGTLNK]
                if not (leftblock != None):
                    fail("assert leftblock != None failed!")
                leftindex = 0

            # Step backwards with the right block/index pair
            rightindex -= 1
            if rightindex == -1:
                rightblock = rightblock[LFTLNK]
                if not (rightblock != None):
                    fail("assert rightblock != None failed!")
                rightindex = n - 1
    self.reverse = reverse

    def __repr__():
        if self.maxlen != None:
            return "deque(%r, maxlen=%s)" % (list(self), self.maxlen)
        else:
            return "deque(%r)" % (list(self),)
    self.__repr__ = __repr__

    def __iter__():
        return _deque_iterator(self, self._iter_impl)
    self.__iter__ = __iter__

    def _iter_impl(i, *args, **kwargs):
        original_state = args[0]
        giveup = kwargs['giveup']
        # stopping condition
        if i >= self.length:
            return StopIteration()

        item = self[i]
        if self.state != original_state:
            return giveup()
        return Ok(item)
    self._iter_impl = _iter_impl

    def __reversed__():
        return _deque_iterator(self, self._reversed_impl)
    self.__reversed__ = __reversed__

    def _reversed_impl(i, *args, **kwargs):
        original_state = args[0]
        giveup = kwargs['giveup']
        # stopping condition
        if i >= self.length:
           return StopIteration()

        item = self[self.length - (i + self.length + 1)]
        if self.state != original_state:
           return giveup()
        return Ok(item)
    self._reversed_impl = _reversed_impl

    def __len__():
        return self.length
    self.__len__ = __len__

    def __getref(index):
        if index >= 0:
            block = self.left
            for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
                if not block:
                    break
                l, r = 0, n
                if block == self.left:
                    l = self.leftndx
                if block == self.right:
                    r = self.rightndx + 1
                span = r - l
                if index < span:
                    return block, l + index
                index -= span
                block = block[RGTLNK]
        else:
            block = self.right
            for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
                if not block:
                    break
                l, r = 0, n
                if block == self.left:
                    l = self.leftndx
                if block == self.right:
                    r = self.rightndx + 1
                negative_span = l - r
                if index >= negative_span:
                    return block, r + index
                index -= negative_span
                block = block[LFTLNK]
        return IndexError("deque index out of range")
    self.__getref = __getref

    def __getitem__(index):
        block, index = self.__getref(index)
        return block[index]
    self.__getitem__ = __getitem__

    def __setitem__(index, value):
        block, index = self.__getref(index)
        operator.setitem(block, index, value)
    self.__setitem__ = __setitem__

    def __delitem__(index):
        length = len(self)
        if index >= 0:
            if index >= length:
                fail("IndexError: deque index out of range")
            self.rotate(-index)
            self.popleft()
            self.rotate(index)
        else:
            # index = ~index      #todo until bit wise operators are in bython
            index = index ^ pow(2, 31)
            if index >= length:
                fail("IndexError: deque index out of range")
            self.rotate(index)
            self.pop()
            self.rotate(-index)
    self.__delitem__ = __delitem__

    def __hash__():
        # raise TypeError, "deque objects are unhashable"
        fail("TypeError: deque objects are unhashable")
    self.__hash__ = __hash__

    def __copy__():
        return self.__class__(self, self.maxlen)
    self.__copy__ = __copy__

    # XXX make comparison more efficient
    def __eq__(other):
        if types.is_instance(other, deque):
            return list(self) == list(other)
        else:
            return NotImplemented
    self.__eq__ = __eq__

    def __ne__(other):
        if types.is_instance(other, deque):
            return list(self) != list(other)
        else:
            return NotImplemented
    self.__ne__ = __ne__

    def __lt__(other):
        if types.is_instance(other, deque):
            return list(self) < list(other)
        else:
            return NotImplemented
    self.__lt__ = __lt__

    def __le__(other):
        if types.is_instance(other, deque):
            return list(self) <= list(other)
        else:
            return NotImplemented
    self.__le__ = __le__

    def __gt__(other):
        if types.is_instance(other, deque):
            return list(self) > list(other)
        else:
            return NotImplemented
    self.__gt__ = __gt__

    def __ge__(other):
        if types.is_instance(other, deque):
            return list(self) >= list(other)
        else:
            return NotImplemented
    self.__ge__ = __ge__

    def __iadd__(other):
        self.extend(other)
        return self
    self.__iadd__ = __iadd__

    def __init__(iterable, maxlen):
        self.clear()
        if maxlen != None and maxlen < 0:
            fail("maxlen must be non-negative")
        self._maxlen = maxlen
        add = self.append
        iterable = iterable or []
        for elem in iter(iterable):
            add(elem)
        return self
    self.__init__ = __init__
    self = __init__(iterable, maxlen)
    return self


def _deque_iterator(deq, itergen):
    self = larky.mutablestruct(__name__='deque_iterator', __class__=_deque_iterator)

    def __init__(deq, itergen):
        self.counter = len(deq)
        def giveup():
            self.counter = 0
            return Error("RuntimeError: deque mutated during iteration")
        self.giveup = giveup
        self._gen = larky.DeterministicGenerator(itergen, deq.state, giveup=giveup)
        return self
    self = __init__(deq, itergen)

    def __next__():
        res = next(self._gen) #.__next__()
        self.counter -= 1
        return res
    self.__next__ = __next__

    def __iter__():
        return self
    self.__iter__ = __iter__
    return self



collections = larky.struct(
    after_each = _after_each,
    before_each = _before_each,
    uniq = _uniq,
    namedtuple = namedtuple,
    deque=deque,
)
