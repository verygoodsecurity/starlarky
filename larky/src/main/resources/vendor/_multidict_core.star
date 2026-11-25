# Ported from multidict 4.7.6-5.1.0, pinned to 5.1.0 (c3eed83): multidict/_multidict_py.py. See larky/src/main/resources/VENDORED.md.
#
# The multidicts are implemented natively (com.verygood.security.larky.modules.multidict); this
# module is their facade. Each constructor passes itself as the instance's __class__, so
# types.is_instance(md, CIMultiDict) and copy() (which calls md.__class__) work as before.
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//jmultidict", _multidict="jmultidict")


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


def getversion(md):
    """The number of changes made to md (or to the multidict md is a proxy of), plus one."""
    return _multidict.getversion(md)


def MultiDict(*args, **kwargs):
    """Dictionary with the support for duplicate keys."""
    return _multidict.new_multidict(MultiDict, "MultiDict", False, args, kwargs)


def CIMultiDict(*args, **kwargs):
    """Dictionary with the support for duplicate case-insensitive keys."""
    return _multidict.new_multidict(CIMultiDict, "CIMultiDict", True, args, kwargs)


def MultiDictProxy(arg):
    """Read-only proxy for MultiDict instance."""
    return _multidict.new_proxy(MultiDictProxy, "MultiDictProxy", False, MultiDict, arg)


def CIMultiDictProxy(arg):
    """Read-only proxy for CIMultiDict instance."""
    return _multidict.new_proxy(CIMultiDictProxy, "CIMultiDictProxy", True, CIMultiDict, arg)


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
