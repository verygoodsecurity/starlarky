"""Redo the builtin repr() (representation) but with limits on most sizes.

The reprlib module provides a means for producing object representations with
limits on the size of the resulting strings. This is used in the Python debugger
and may be useful in other contexts as well.

This module provides a struct, an instance, and a function:

``reprlib.Repr`` - struct which provides formatting services useful in
implementing functions similar to the built-in repr(); size limits for different
object types are added to avoid the generation of representations which are
excessively long.

``reprlib.aRepr`` - This is an instance of Repr which is used to provide the repr()
function described below. Changing the attributes of this object will affect the
size limits used by repr() and Python (or in the future, a Larky) debugger.

``reprlib.repr(obj)`` - This is the repr() method of aRepr. It returns a string
similar to that returned by the built-in function of the same name, but with
limits on most sizes.

This diverges from CPython's ``reprlib`` in that it does not provide
a decorator for detecting recursive calls to __repr__() since Larky does not
support recursion.

More here: https://docs.python.org/3/library/reprlib.html#module-reprlib
ported to Larky from: https://github.com/python/cpython/blob/3.9/Lib/reprlib.py
"""
load("@stdlib//larky", larky="larky")
load("@stdlib//builtins", builtins="builtins")

def _Repr():
    """
    Repr struct:

    - Repr instances provide several attributes which can be used to provide size
      limits for the representations of different object types, and methods which
      format specific object types.
    :return:
    """
    self = larky.mutablestruct(__name__='Repr', __class__=_Repr)

    def __init__():
        #: Depth limit on the creation of recursive representations. The default
        #: is 6.
        self.maxlevel = 6
        #: Limits on the number of entries represented for the named object
        #: type. The default is 4 for maxdict, 5 for maxarray, and 6 for the
        #: others.
        self.maxtuple = 6
        self.maxlist = 6
        self.maxarray = 5
        self.maxdict = 4
        self.maxset = 6
        self.maxfrozenset = 6
        self.maxdeque = 6
        #: Limit on the number of characters in the representation of the
        #: string. Note that the “normal” representation of the string is used as
        #: the character source: if escape sequences are needed in the
        #: representation, these may be mangled when the representation is
        #: shortened. The default is 30.
        self.maxstring = 30
        #: Maximum number of characters in the representation for an integer.
        #: Digits are dropped from the middle. The default is 40.
        self.maxlong = 40
        #: This limit is used to control the size of object types for which no
        #: specific formatting method is available on the Repr object. It is
        #: applied in a similar manner as maxstring. The default is 20.
        self.maxother = 30
        return self
    self = __init__()

    def repr(x):
        """
        The equivalent to the built-in repr() that uses the formatting imposed
        by the instance.

        :param x:
        :return:
        """
        return self.repr1(x, self.maxlevel)
    self.repr = repr

    def repr1(x, level):
        """
        Implementation used by repr(). This uses the type of obj to determine
        which formatting method to call, passing it obj and level.

        The type-specific methods should call repr1() to perform recursive
        formatting, with level - 1 for the value of level in the recursive call.

        :param x:
        :param level:
        :return:
        """
        typename = bytes(larky.impl_function_name(x), encoding='utf-8')
        if b' ' in typename:
            parts = typename.split()
            typename = b'_'.join(parts)
        typename = typename.decode('iso-8859-1')
        if hasattr(self, 'repr_' + typename):
            return getattr(self, 'repr_' + typename)(x, level)
        else:
            return self.repr_instance(x, level)
    self.repr1 = repr1

    # Formatting methods for specific types are implemented as methods with
    # a name based on the type name.
    #
    # In the method name, TYPE is replaced by:
    #  '_'.join(type(obj).__name__.split()).
    #
    # Dispatch to these methods is handled by repr1().
    #
    # Type-specific methods which need to recursively format a value
    # are not supported in Larky.
    def repr_str(x, level):
        s = builtins.repr(x[:self.maxstring])
        if len(s) > self.maxstring:
            i = max(0, (self.maxstring-3)//2)
            j = max(0, self.maxstring-3-i)
            s = builtins.repr(x[:i] + x[len(x)-j:])
            s = s[:i] + '...' + s[len(s)-j:]
        return s
    self.repr_str = repr_str

    def repr_instance(x, level):
        s = builtins.repr(x)
        if len(s) > self.maxother:
            i = max(0, (self.maxother-3)//2)
            j = max(0, self.maxother-3-i)
            s = s[:i] + '...' + s[len(s)-j:]
        return s
    self.repr_instance = repr_instance
    return self


aRepr = _Repr()
reprlib = larky.struct(
    aRepr=aRepr,
    Repr=_Repr,
    repr=aRepr.repr
)
