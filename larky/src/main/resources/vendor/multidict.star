load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//types", types="types")
load("@vendor//option/result", Error="Error", Ok="Ok")


def __marker():
    return larky.struct(__name__='_marker', __class__=__marker)
_marker = __marker()


def istr(v=""):
    """Case insensitive str."""
    self = larky.mutablestruct(
        __name__='istr',
        __class__=istr,
        __is_istr__ = True
    )

    def __init__(seq):
        if types.is_string(seq):
            self.data = seq
        elif types.is_instance(seq, istr):
            self.data = seq.data[:]
        else:
            self.data = str(seq)
        return self

    self = __init__(v)

    def __str__():
        return str(self.data)
    self.__str__ = __str__

    def __repr__():
        return repr(self.data)
    self.__repr__ = __repr__

    def __hash__():
        return hash(self.data)
    self.__hash__ = __hash__

    def __len__():
        return len(self.data)
    self.__len__ = __len__

    def __eq__(string):
        if types.is_instance(string, istr):
            return self.data == string.data
        return self.data == string
    self.__eq__ = __eq__
    def __lt__(string):
        if types.is_instance(string, istr):
            return self.data < string.data
        return self.data < string
    self.__lt__ = __lt__
    def __le__(string):
        if types.is_instance(string, istr):
            return self.data <= string.data
        return self.data <= string
    self.__le__ = __le__
    def __gt__(string):
        if types.is_instance(string, istr):
            return self.data > string.data
        return self.data > string
    self.__gt__ = __gt__
    def __ge__(string):
        if types.is_instance(string, istr):
            return self.data >= string.data
        return self.data >= string
    self.__ge__ = __ge__

    def __contains__(char):
        if types.is_instance(char, istr):
            char = char.data
        return char in self.data
    self.__contains__ = __contains__

    def title(): return self.__class__(self.data.title())
    self.title = title
    # istr should be hashable, so we must make sure it is immutable.
    return larky.struct(**self.__dict__)


_version = larky.utils.Counter()


def getversion(md):
    return md._impl._version


def _Impl():
    self = larky.mutablestruct(_name__='_Impl', __class__=_Impl)

    def incr_version():
        self._version = _version.add_and_get()

    self.incr_version = incr_version

    def __init__():
        self._items = []
        self.incr_version()
        return self

    self = __init__()
    return self


def _Base():
    self = larky.mutablestruct(__name__='_Base', __class__=_Base)

    def _title(key):
        return key
    self._title = _title

    def getall(key, default=_marker):
        """Return a list of all values matching the key."""
        identity = self._title(key)
        res = [v for i, k, v in self._impl._items if operator.eq(i, identity)]
        if res:
            return res
        if not res and default != _marker:
            return default
        return Error("KeyError: " + "Key not found: %r" % key).unwrap()
    self.getall = getall
    self.get = getall

    def getone(key, default=_marker):
        """Get first value matching the key."""
        identity = self._title(key)
        for i, k, v in self._impl._items:
            if i == identity:
                return v
        if default != _marker:
            return default
        return Error("KeyError: " + "Key not found: %r" % key).unwrap()
    self.getone = getone

    # Mapping interface #

    def __getitem__(key):
        return self.getone(key)
    self.__getitem__ = __getitem__

    def get(key, default=None):
        """Get first value matching the key.

        The method is alias for .getone().
        """
        return self.getone(key, default)
    self.get = get

    def __iter__():
        return iter(self.keys())
    self.__iter__ = __iter__

    def __len__():
        return len(self._impl._items)
    self.__len__ = __len__

    def keys():
        """Return a new view of the dictionary's keys."""
        return list(_KeysView(self._impl))
    self.keys = keys

    def items():
        """Return a new view of the dictionary's items *(key, value) pairs)."""
        return list(_ItemsView(self._impl))
    self.items = items

    def values():
        """Return a new view of the dictionary's values."""
        return list(_ValuesView(self._impl))
    self.values = values

    def __eq__(other):
        if types.is_instance(other, self.__class__):
            lft = self._impl._items
            rht = other._impl._items
            if len(lft) != len(rht):
                return False
            for (i1, k2, v1), (i2, k2, v2) in zip(lft, rht):
                if i1 != i2 or v1 != v2:
                    return False
            return True
        if len(self._impl._items) != len(other):
            return False
        for k, v in self.items():
            nv = other.get(k, _marker)
            if v != nv:
                return False
        return True
    self.__eq__ = __eq__

    def __ne__(other):
        return not self.__eq__(other)
    self.__ne__ = __ne__

    def __contains__(key):
        identity = self._title(key)
        for i, k, v in self._impl._items:
            if i == identity:
                return True
        return False
    self.__contains__ = __contains__

    def __repr__():
        body = ", ".join(["'{}': {}".format(k, repr(v)) for k, v in iter(self.items())])
        return "<{}({})>".format(self.__name__, body)
    self.__repr__ = __repr__
    return self


def MultiDictProxy(arg):
    """Read-only proxy for MultiDict instance."""
    self = larky.mutablestruct(__name__='MultiDictProxy', __class__=MultiDictProxy)

    def __init__(arg):
        # if not types.is_instance(arg, (MultiDict, MultiDictProxy)):
        #     return Error()hh
        self._impl = arg._impl
        return self
    self = __init__(arg)

    def __reduce__():
        return Error().unwrap()
    self.__reduce__ = __reduce__

    def copy():
        """Return a copy of itself."""
        return MultiDict(self.items())
    self.copy = copy
    return self


def CIMultiDictProxy(arg):
    """Read-only proxy for CIMultiDict instance."""
    self = larky.mutablestruct(__name__='CIMultiDictProxy', __class__=CIMultiDictProxy)

    def __init__(arg):
        # if not types.is_instance(arg, (CIMultiDict, CIMultiDictProxy)):
        #     return Error()
        self._impl = arg._impl
        return self
    self = __init__(arg)

    def _title(key):
        return key.title()
    self._title = _title

    def copy():
        """Return a copy of itself."""
        return CIMultiDict(self.items())
    self.copy = copy
    return self


def MultiDict(*args, **kwargs):
    """Dictionary with the support for duplicate keys."""
    self = _Base()
    self.__name__ = 'MultiDict'
    self.__class__ = MultiDict

    def __reduce__():
        return self.__class__, (list(self.items()),)
    self.__reduce__ = __reduce__

    def _title(key):
        return key
    self._title = _title

    def _key(key):
        if types.is_string(key) or hasattr(key, "__is_istr__"):
            return key
        return Error(
            "TypeError: MultiDict keys should be str. Received %s" % type(key)
        ).unwrap()
    self._key = _key

    def add(key, value):
        identity = self._title(key)
        self._impl._items.append((identity, self._key(key), value))
        self._impl.incr_version()
    self.add = add

    def copy():
        """Return a copy of itself."""
        cls = self.__class__
        return cls(self.items())
    self.copy = copy
    self.__copy__ = copy

    def extend(*args, **kwargs):
        """Extend current MultiDict with more values.

        This method must be used instead of update.
        """
        self._extend(args, kwargs, "extend", self._extend_items)
    self.extend = extend

    def _extend(args, kwargs, name, method):
        if len(args) > 1:
            return Error(
                "TypeError: {} takes at most 1 positional argument ({} given)"
                    .format(name, len(args))
                ).unwrap()
        if args:
            arg = args[0]
            # if types.is_instance(args[0], (MultiDict, MultiDictProxy)) and not kwargs:
            if not kwargs and (hasattr(arg, "_impl") and hasattr(arg._impl, "_items")):
                items = arg._impl._items
            else:
                if hasattr(arg, "items"):
                    arg = arg.items()
                if kwargs:
                    arg = list(arg)
                    arg.extend(list(kwargs.items()))
                items = []
                for item in arg:
                    if not len(item) == 2:
                        return Error().unwrap()
                    items.append((self._title(item[0]), self._key(item[0]), item[1]))

            method(items)
        else:
            method(
                [
                    (self._title(key), self._key(key), value)
                    for key, value in kwargs.items()
                ]
            )
    self._extend = _extend

    def _extend_items(items):
        for identity, key, value in items:
            self.add(key, value)
    self._extend_items = _extend_items

    def clear():
        """Remove all items from MultiDict."""
        self._impl._items.clear()
        self._impl.incr_version()
    self.clear = clear


    # Mapping interface #

    def __setitem__(key, value):
        self._replace(key, value)
    self.__setitem__ = __setitem__

    def __delitem__(key):
        identity = self._title(key)
        items = self._impl._items
        found = False
        for i in range(len(items) - 1, -1, -1):
            if items[i][0] == identity:
                items.pop(i)
                found = True
        if not found:
            return Error("KeyError: %s" % key).unwrap()
        else:
            self._impl.incr_version()
    self.__delitem__ = __delitem__

    def setdefault(key, default=None):
        """Return value for key, set value to default if key is not present."""
        identity = self._title(key)
        for i, k, v in self._impl._items:
            if i == identity:
                return v
        self.add(key, default)
        return default
    self.setdefault = setdefault

    def popone(key, default=_marker):
        """Remove specified key and return the corresponding value.

        If key is not found, d is returned if given, otherwise
        KeyError is raised.

        """
        identity = self._title(key)
        for i in range(len(self._impl._items)):
            if self._impl._items[i][0] == identity:
                value = self._impl._items[i][2]
                self._impl._items.pop(i)
                self._impl.incr_version()
                return value
        if default == _marker:
            return Error("KeyError: %s" % key).unwrap()
        else:
            return default
    self.popone = popone

    self.pop = popone  # type: ignore

    def popall(key, default=_marker):
        """Remove all occurrences of key and return the list of corresponding
        values.

        If key is not found, default is returned if given, otherwise
        KeyError is raised.

        """
        found = False
        identity = self._title(key)
        ret = []
        for i in range(len(self._impl._items) - 1, -1, -1):
            item = self._impl._items[i]
            if item[0] == identity:
                ret.append(item[2])
                self._impl._items.pop(i)
                self._impl.incr_version()
                found = True
        if not found:
            if default == _marker:
                return Error("KeyError: %s" % key).unwrap()
            else:
                return default
        else:
            return reversed(ret)
    self.popall = popall

    def popitem():
        """Remove and return an arbitrary (key, value) pair."""
        if self._impl._items:
            i = self._impl._items.pop(0)
            self._impl.incr_version()
            return i[1], i[2]
        else:
            return Error("KeyError: empty multidict").unwrap()
    self.popitem = popitem

    def update(*args, **kwargs):
        """Update the dictionary from *other*, overwriting existing keys."""
        self._extend(args, kwargs, "update", self._update_items)
    self.update = update

    def _update_items(items):
        if not items:
            return
        used_keys = {}
        for identity, key, value in items:
            start = used_keys.get(identity, 0)
            exhausted = True
            for i in range(start, len(self._impl._items)):
                item = self._impl._items[i]
                if item[0] == identity:
                    used_keys[identity] = i + 1
                    self._impl._items[i] = (identity, key, value)
                    exhausted = False
                    break
            if exhausted:
                self._impl._items.append((identity, key, value))
                used_keys[identity] = len(self._impl._items)

        # drop tails
        i = 0
        iteration_limit_reached = False
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if i >= len(self._impl._items):
                break
            item = self._impl._items[i]
            identity = item[0]
            pos = used_keys.get(identity)
            if pos == None:
                i += 1
                # Check if this is the last iteration
                if _while_ == WHILE_LOOP_EMULATION_ITERATION - 1:
                    iteration_limit_reached = True
                continue
            if i >= pos:
                self._impl._items.pop(i)
            else:
                i += 1

            # Check if this is the last iteration
            if _while_ == WHILE_LOOP_EMULATION_ITERATION - 1:
                iteration_limit_reached = True

        # If we reached the iteration limit and still have items to process, fail
        if iteration_limit_reached and i < len(self._impl._items):
            fail("Iteration limit exceeded: too many items to process in multidict update, more than WHILE_LOOP_EMULATION_ITERATION limit of %d" % WHILE_LOOP_EMULATION_ITERATION)

        self._impl.incr_version()
    self._update_items = _update_items

    def _replace(key, value):
        key = self._key(key)
        identity = self._title(key)
        items = self._impl._items
        exhausted = True
        for i in range(len(items)):
            item = items[i]
            if item[0] == identity:
                items[i] = (identity, key, value)
                # i points to last found item
                rgt = i
                self._impl.incr_version()
                exhausted = False
                break
        if exhausted:
            self._impl._items.append((identity, key, value))
            self._impl.incr_version()
            return

        # remove all tail items
        i = rgt + 1
        iteration_limit_reached = False
        for _while_ in range(WHILE_LOOP_EMULATION_ITERATION):
            if i >= len(items):
                break
            item = items[i]
            if item[0] == identity:
                self._impl._items.pop(i)
            else:
                i += 1

            # Check if this is the last iteration
            if _while_ == WHILE_LOOP_EMULATION_ITERATION - 1:
                iteration_limit_reached = True

        # If we reached the iteration limit and still have items to process, fail
        if iteration_limit_reached and i < len(items):
            fail("Iteration limit exceeded: too many tail items to remove in multidict replace, more than WHILE_LOOP_EMULATION_ITERATION limit of %d" % WHILE_LOOP_EMULATION_ITERATION)
    self._replace = _replace

    def __init__(*args, **kwargs):
        self._impl = _Impl()
        self._extend(args, kwargs, self.__name__, self._extend_items)
        return self
    self = __init__(*args, **kwargs)
    return self


def CIMultiDict(*args, **kwargs):
    """Dictionary with the support for duplicate case-insensitive keys."""
    self = MultiDict()
    self.__name__ = 'CIMultiDict'
    self.__class__ = CIMultiDict

    def _title(key):
        return key.title()
    self._title = _title

    def __init__(*args, **kwargs):
        # we have to re-init b/c _title is overridden.
        self._impl = _Impl()
        self._extend(args, kwargs, self.__name__, self._extend_items)
        return self
    self = __init__(*args, **kwargs)
    return self


def _Iter(size, iterator):
    self = larky.mutablestruct(__name__='_Iter', __class__=_Iter)

    def __init__(size, iterator):
        self._size = size
        self._iter = iterator
        return self
    self = __init__(size, iterator)

    def __iter__():
        return self
    self.__iter__ = __iter__

    def __next__():
        return next(self._iter)
    self.__next__ = __next__

    def __length_hint__():
        return self._size
    self.__length_hint__ = __length_hint__
    return self


def _ViewBase(impl):
    self = larky.mutablestruct(__name__='_ViewBase', __class__=_ViewBase)
    def __init__(impl):
        self._impl = impl
        self._version = impl._version
        return self
    self = __init__(impl)

    def __len__():
        return len(self._impl._items)
    self.__len__ = __len__
    return self


def _ItemsView(impl):
    self = _ViewBase(impl)
    self.__name__ = '_ItemsView'
    self.__class__ = _ItemsView

    # abc.ItemsView (i.e. abc.Set)..
    def __le__(other):
        if not types.is_instance(other, _ItemsView):
            return Error("NotImplemented")
        if len(self) > len(other):
            return False
        for elem in iter(self):
            if elem not in other:
                return False
        return True
    self.__le__ = __le__

    def __lt__(other):
        if not types.is_instance(other, _ItemsView):
            return Error("NotImplemented")
        return len(self) < len(other) and self.__le__(other)
    self.__lt__ = __lt__

    def __gt__(other):
        if not types.is_instance(other, _ItemsView):
            return Error("NotImplemented")
        return len(self) > len(other) and self.__ge__(other)
    self.__gt__ = __gt__

    def __ge__(other):
        if not types.is_instance(other, _ItemsView):
            return Error("NotImplemented")
        if len(self) < len(other):
            return False
        for elem in iter(other):
            if elem not in self:
                return False
        return True
    self.__ge__ = __ge__

    def __eq__(other):
        if not types.is_instance(other, _ItemsView):
            return Error("NotImplemented")
        return len(self) == len(other) and self.__le__(other)
    self.__eq__ = __eq__

    def __ne__(other):
        return not self.__eq__(other)
    self.__ne__ = __ne__

    def __contains__(item):
        if not (types.is_instance(item, tuple) or types.is_instance(item, list)):
            fail("assert types.is_instance(item, tuple) or types.is_instance(item, list) failed!")
        if not (len(item) == 2):
            fail("assert len(item) == 2 failed!")
        for i, k, v in self._impl._items:
            if item[0] == k and item[1] == v:
                return True
        return False

    self.__contains__ = __contains__

    def __iter__():
        return _Iter(len(self), larky.DeterministicGenerator(self._iter))
    self.__iter__ = __iter__

    def _iter(i):
        # stopping condition
        if i >= len(self._impl._items):
            return StopIteration()

        _idx, k, v = self._impl._items[i]

        if self._version != self._impl._version:
            return Error("RuntimeError: Dictionary changed during iteration")
        return Ok((k, v,))

    self._iter = _iter

    def __repr__():
        lst = []
        for item in self._impl._items:
            lst.append("{}: {}".format(repr(item[1]), repr(item[2])))
        body = ", ".join(lst)
        return "{}({})".format(self.__name__, body)
    self.__repr__ = __repr__
    return self


def _ValuesView(impl):
    self = _ViewBase(impl)
    self.__name__ = '_ValuesView'
    self.__class__ = _ValuesView

    def __contains__(value):
        for item in self._impl._items:
            if item[2] == value:
                return True
        return False
    self.__contains__ = __contains__

    def __iter__():
        return _Iter(len(self), larky.DeterministicGenerator(self._iter))
    self.__iter__ = __iter__

    def _iter(i):
        # stopping condition
        if i >= len(self._impl._items):
            return StopIteration()

        item = self._impl._items[i]

        if self._version != self._impl._version:
            return Error("RuntimeError: Dictionary changed during iteration")
        return Ok(item[2])
    self._iter = _iter

    def __repr__():
        lst = []
        for item in self._impl._items:
            lst.append("{}".format(repr(item[2])))
        body = ", ".join(lst)
        return "{}({})".format(self.__name__, body)
    self.__repr__ = __repr__
    return self


def _KeysView(impl):
    self = _ViewBase(impl)
    self.__name__ = '_KeysView'
    self.__class__ = _KeysView

    # abc.ItemsView (i.e. abc.Set)..
    def __le__(other):
        if not types.is_instance(other, _KeysView):
            return Error("NotImplemented")
        if len(self) > len(other):
            return False
        for elem in iter(self):
            if elem not in other:
                return False
        return True
    self.__le__ = __le__

    def __lt__(other):
        if not types.is_instance(other, _KeysView):
            return Error("NotImplemented")
        return len(self) < len(other) and self.__le__(other)
    self.__lt__ = __lt__

    def __gt__(other):
        if not types.is_instance(other, _KeysView):
            return Error("NotImplemented")
        return len(self) > len(other) and self.__ge__(other)
    self.__gt__ = __gt__

    def __ge__(other):
        if not types.is_instance(other, _KeysView):
            return Error("NotImplemented")
        if len(self) < len(other):
            return False
        for elem in iter(other):
            if elem not in self:
                return False
        return True
    self.__ge__ = __ge__

    def __eq__(other):
        if not types.is_instance(other, _KeysView):
            return Error("NotImplemented")
        return len(self) == len(other) and self.__le__(other)
    self.__eq__ = __eq__

    def __ne__(other):
        return not self.__eq__(other)
    self.__ne__ = __ne__

    def __contains__(key):
        for item in self._impl._items:
            if item[1] == key:
                return True
        return False
    self.__contains__ = __contains__

    def __iter__():
        return _Iter(len(self), larky.DeterministicGenerator(self._iter))
    self.__iter__ = __iter__

    def _iter(i):
        # stopping condition
        if i >= len(self._impl._items):
            return StopIteration()

        item = self._impl._items[i]

        if self._version != self._impl._version:
            return Error("RuntimeError: Dictionary changed during iteration")
        return Ok(item[1])
    self._iter = _iter

    def __repr__():
        lst = []
        for item in self._impl._items:
            lst.append("{}".format(repr(item[1])))
        body = ", ".join(lst)
        return "{}({})".format(self.__name__, body)
    self.__repr__ = __repr__
    return self


multidict = larky.struct(
    __name__='multidict',
    CIMultiDict=CIMultiDict,
    CIMultiDictProxy=CIMultiDictProxy,
    MultiDict=MultiDict,
    MultiDictProxy=MultiDictProxy,
    getversion=getversion,
    istr=istr,
    upstr=istr,
)
