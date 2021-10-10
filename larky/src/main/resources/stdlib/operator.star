"""
Operator Interface

This module exports a set of functions corresponding to the intrinsic
operators of Python.

For example, operator.add(x, y) is equivalent to the expression x+y.

The function names are those used for special methods;
variants without leading and trailing '__' are also provided for convenience.

This is a pure Python implementation of the module.
Taken from: https://github.com/python/cpython/blob/main/Lib/operator.py
"""
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@stdlib//builtins", builtins="builtins")


_abs = builtins.abs
_pow = builtins.pow


# Comparison Operations *******************************************************#

def lt(a, b):
    "Same as a < b."
    _m = getattr(a, '__lt__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        return a < b
    return _m(b)

def le(a, b):
    "Same as a <= b."
    _m = getattr(a, '__le__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        return a <= b
    return _m(b)

def eq(a, b):
    "Same as a == b."
    _m = getattr(a, '__eq__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        return a == b
    return _m(b)

def ne(a, b):
    "Same as a != b."
    _m = getattr(a, '__ne__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        return a != b
    return _m(b)

def ge(a, b):
    "Same as a >= b."
    _m = getattr(a, '__ge__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        return a >= b
    return _m(b)

def gt(a, b):
    "Same as a > b."
    _m = getattr(a, '__gt__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        return a > b
    return _m(b)

# Logical Operations **********************************************************#


def truth(a):
    "Return True if a is true, False otherwise."
    if a == True:
        return True
    elif a == False:
        return False
    elif a == None:
        return False

    _m = getattr(a, '__bool__', getattr(a, '__nonzero__', larky.SENTINEL))
    if _m == larky.SENTINEL:
        _m = getattr(a, '__len__', larky.SENTINEL)
        if _m == larky.SENTINEL:
            # return True if a else False
            return bool(a)
        return True if len(a) > 0 else False
    return bool(_m())

def not_(a):
    "Same as not a."
    return False if truth(a) else True

def is_(a, b):
    "Same as a is b."
    return a == b

def is_not(a, b):
    "Same as a is not b."
    return a != b

# Mathematical/Bitwise Operations *********************************************#

def abs(a):
    "Same as abs(a)."
    return _abs(a)

def add(a, b):
    "Same as a + b."
    return a + b

def and_(a, b):
    "Same as a & b."
    return a & b

def floordiv(a, b):
    "Same as a // b."
    return a // b

def index(a):
    "Same as a.__index__()."
    if types.is_int(a) or types.is_float(a) or types.is_bool(a) or types.is_string(a):
        return int(a)
    if hasattr(a, "__index__"):
        return a.__index__()
    fail("TypeError: '%s' object cannot be interpreted as an integer" % type(a))

def inv(a):
    "Same as ~a."
    return ~a
invert = inv

def lshift(a, b):
    "Same as a << b."
    return a << b

def mod(a, b):
    "Same as a % b."
    return a % b

def mul(a, b):
    "Same as a * b."
    return a * b

def matmul(a, b):
    "Same as a @ b."
    return a.__matmul__(b)

def neg(a):
    "Same as -a."
    return -a

def or_(a, b):
    "Same as a | b."
    return a | b

def pos(a):
    "Same as +a."
    return +a

def pow(a, b):
    "Same as a ** b."
    return _pow(a, b)

def rshift(a, b):
    "Same as a >> b."
    return a >> b

def sub(a, b):
    "Same as a - b."
    return a - b

def truediv(a, b):
    "Same as a / b."
    return a / b

def xor(a, b):
    "Same as a ^ b."
    return a ^ b

# Sequence Operations *********************************************************#

def concat(a, b):
    "Same as a + b, for a and b sequences."
    if not any((
        types.is_iterable(a),
        types.is_string(a),
        hasattr(a, '__add__'),
        hasattr(a, '__getitem__'),
    )):
        msg = "TypeError: '%s' object can't be concatenated with '%s'"
        msg %= (type(a), type(b))
        fail(msg)
    if hasattr(a, '__add__'):
        return a.__add__(b)
    return a + b

def contains(a, b):
    "Same as b in a (note reversed operands)."
    if hasattr(a, '__contains__'):
        return a.__contains__(b)
    return countOf(a, b) != 0

def countOf(a, b):
    "Return the number of times b occurs in a."

    # ... hasattr(), iterable(), is_string() ... should probably be moved
    # to types.star
    if not any([
        hasattr(a, '__iter__'),
        types.is_iterable(a),
        types.is_string(a),
    ]):
        msg = "TypeError: type '%s' is not iterable"
        msg %= (type(a))
        fail(msg)
    count = 0
    iterable = a
    if types.is_string(a):
        iterable = a.elems()
    elif hasattr(a, '__iter__'):
        iterable = a.__iter__()
    # TODO: __getitem__
    for i in iterable:
        if i == b:
            count += 1
    return count


def delitem(a, b):
    "Same as del a[b]."
    if not any((
        hasattr(a, '__delitem__'),
        types.is_list(a),
        types.is_dict(a)
    )):
        fail("TypeError: '%s' does not support delitem" % type(a))

    if types.is_list(a):
        if not any((
            types.is_int(b),
            #types.is_slice(b)  # todo(mahmoudimus): not supported yet..
        )):
            fail("TypeError: list indices must be integers or slices, not %s"
                     % type(b))
        a.pop(b)
        return
    elif types.is_dict(a):
        if b not in a:
            fail("KeyError: '%s'" % b)
        a.pop(b)
    else:
        a.__delitem__(b)   # no del in starlark!
        return
    fail("TypeError: '%s' does not support delitem" % type(a))


def getitem(a, b):
    "Same as a[b]."
    return a.__getitem__(b) if hasattr(a, '__getitem__') else a[b]


def indexOf(a, b):
    "Return the first index of b in a."
    for i, j in enumerate(builtins.iter(a)):
        if j == b:
            return i
    fail("ValueError: " + 'sequence.index(x): x not in sequence')

def setitem(a, b, c):
    "Same as a[b] = c."
    _m = getattr(a, '__setitem__', larky.SENTINEL)
    if _m != larky.SENTINEL:
        _m(b, c)
    else:
        a[b] = c

def length_hint(obj, default=0):
    """
    As defined in PEP 424.
    Return an estimate of the number of items in obj.
    This is useful for presizing containers when building from an iterable.

    If the object supports `len()`, the result will be exact.

    Otherwise, it may over- or under-estimate by an arbitrary amount. The
    result will be an integer >= 0.
    """
    if types.is_iterable(obj):
        return len(obj)
    if hasattr(obj, '__len__'):
        return obj.__len__()
    if hasattr(obj, '__length_hint__'):
        hint = obj.__length_hint__()
        if not types.is_int(hint):
            fail("TypeError: Length hint must be an integer, not %r" %
                                        type(hint))
        if hint < 0:
            fail("ValueError: __length_hint__() should return >= 0")
        return hint

    return default


def itemgetter(*args):
    if len(args) == 0:
        fail("TypeError: itemgetter expected 1 argument, got %s" % len(args))

    def _itemgetter(obj):
        return getitem(obj, args[0])
    def _itemsgetter(obj):
        return tuple([getitem(obj, i) for i in args])
    if len(args) == 1:
        return _itemgetter
    return _itemsgetter


def attrgetter(*attr):
    if len(attr) == 0:
        fail("TypeError: attrgetter expected 1 argument, got %s" % len(attr))

    if len(attr) == 1 and not types.is_iterable(attr[0]):
        attr = [attr[0]]

    for pos, a in enumerate(list(attr)):
        if not types.is_string(a):
            fail("TypeError: attribute name must be a string")
        if "." in a:
            fail(('nested attributes / recursive gets ("%s") are ' +
                 'not supported') % a)

    def _attrgetter(obj):
        r = []

        for a in attr:
            # if not types.is_string(a):
            #     fail("TypeError: attribute name must be a string")
            # if "." in a:
            #     fail('"." in %s' % a)
            r.append(getattr(obj, a))

        # r = [getattr(obj, a) for a in attr]
        if len(r) == 1:
            return r[0]
        return tuple(r)
    return _attrgetter


def methodcaller(name, *args, **kwargs):
    if not types.is_string(name):
        fail("TypeError: method name must be a string")
    def _methodcaller(obj):
        return getattr(obj, name)(*args, **kwargs)
    return _methodcaller


# In-place Operations *********************************************************#

def iadd(a, b):
    "Same as a += b."
    _m = getattr(a, '__iadd__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        a += b
        return a
    else:
        return _m(b)


def iand(a, b):
    "Same as a &= b."
    _m = getattr(a, '__iand__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        a &= b
        return a
    else:
        return _m(b)

def iconcat(a, b):
    "Same as a += b, for a and b sequences."
    if not any((
        types.is_iterable(a),
        types.is_string(a),
        hasattr(a, '__add__'),
        hasattr(a, '__getitem__'),
    )):
        msg = "TypeError: '%s' object can't be concatenated with '%s'"
        msg %= (type(a), type(b))
        fail(msg)
    if hasattr(a, '__iadd__'):
        return a.__iadd__(b)

    _m = getattr(a, '__iconcat__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        a += b
        return a
    else:
        return _m(b)

def ifloordiv(a, b):
    "Same as a //= b."
    _m = getattr(a, '__ifloordiv__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        a //= b
        return a
    else:
        return _m(b)

def ilshift(a, b):
    "Same as a <<= b."
    _m = getattr(a, '__ilshift__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        a <<= b
        return a
    else:
        return _m(b)

def imod(a, b):
    "Same as a %= b."
    _m = getattr(a, '__imod__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        a %= b
        return a
    else:
        return _m(b)

def imul(a, b):
    "Same as a *= b."
    _m = getattr(a, '__imul__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        a *= b
        return a
    else:
        return _m(b)

def imatmul(a, b):
    "Same as a @= b."
    _m = getattr(a, '__imatmul__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        a = matmul(a, b)
        return a
    else:
        return _m(b)

def ior(a, b):
    "Same as a |= b."
    _m = getattr(a, '__ior__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        a |= b
        return a
    else:
        return _m(b)

def ipow(a, b):
    "Same as a **= b."
    _m = getattr(a, '__ipow__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        a = pow(a, b)
        return a
    else:
        return _m(b)

def irshift(a, b):
    "Same as a >>= b."
    _m = getattr(a, '__irshift__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        a >>= b
        return a
    else:
        return _m(b)

def isub(a, b):
    "Same as a -= b."
    _m = getattr(a, '__isub__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        a -= b
        return a
    else:
        return _m(b)

def itruediv(a, b):
    "Same as a /= b."
    _m = getattr(a, '__itruediv__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        a /= b
        return a
    else:
        return _m(b)

def ixor(a, b):
    "Same as a ^= b."
    _m = getattr(a, '__ixor__', larky.SENTINEL)
    if _m == larky.SENTINEL:
        a ^= b
        return a
    else:
        return _m(b)

# All of these "__func__ = func" assignments have to happen after importing
# from _operator to make sure they're set to the right function
__lt__ = lt
__le__ = le
__eq__ = eq
__ne__ = ne
__ge__ = ge
__gt__ = gt
__not__ = not_
__abs__ = abs
__add__ = add
__and__ = and_
__floordiv__ = floordiv
__index__ = index
__inv__ = inv
__invert__ = invert
__lshift__ = lshift
__mod__ = mod
__mul__ = mul
__matmul__ = matmul
__neg__ = neg
__or__ = or_
__pos__ = pos
__pow__ = pow
__rshift__ = rshift
__sub__ = sub
__truediv__ = truediv
__xor__ = xor
__concat__ = concat
__contains__ = contains
__delitem__ = delitem
__getitem__ = getitem
__setitem__ = setitem
__iadd__ = iadd
__iand__ = iand
__iconcat__ = iconcat
__ifloordiv__ = ifloordiv
__ilshift__ = ilshift
__imod__ = imod
__imul__ = imul
__imatmul__ = imatmul
__ior__ = ior
__ipow__ = ipow
__irshift__ = irshift
__isub__ = isub
__itruediv__ = itruediv
__ixor__ = ixor

operator = larky.struct(
    lt=lt,
    le=le,
    eq=eq,
    ne=ne,
    ge=ge,
    gt=gt,
    not_=not_,
    truth=truth,
    is_=is_,
    is_not=is_not,
    abs=abs,
    add=add,
    and_=and_,
    floordiv=floordiv,
    index=index,
    inv=inv,
    invert=invert,
    lshift=lshift,
    mod=mod,
    mul=mul,
    matmul=matmul,
    neg=neg,
    or_=or_,
    pos=pos,
    pow=pow,
    rshift=rshift,
    sub=sub,
    truediv=truediv,
    xor=xor,
    concat=concat,
    contains=contains,
    countOf=countOf,
    delitem=delitem,
    getitem=getitem,
    indexOf=indexOf,
    setitem=setitem,
    length_hint=length_hint,
    itemgetter=itemgetter,
    attrgetter=attrgetter,
    methodcaller=methodcaller,
    iadd=iadd,
    iand=iand,
    iconcat=iconcat,
    ifloordiv=ifloordiv,
    ilshift=ilshift,
    imod=imod,
    imul=imul,
    imatmul=imatmul,
    ior=ior,
    ipow=ipow,
    irshift=irshift,
    isub=isub,
    itruediv=itruediv,
    ixor=ixor,
    __lt__=__lt__,
    __le__=__le__,
    __eq__=__eq__,
    __ne__=__ne__,
    __ge__=__ge__,
    __gt__=__gt__,
    __not__=__not__,
    __abs__=__abs__,
    __add__=__add__,
    __and__=__and__,
    __floordiv__=__floordiv__,
    __index__=__index__,
    __inv__=__inv__,
    __invert__=__invert__,
    __lshift__=__lshift__,
    __mod__=__mod__,
    __mul__=__mul__,
    __matmul__=__matmul__,
    __neg__=__neg__,
    __or__=__or__,
    __pos__=__pos__,
    __pow__=__pow__,
    __rshift__=__rshift__,
    __sub__=__sub__,
    __truediv__=__truediv__,
    __xor__=__xor__,
    __concat__=__concat__,
    __contains__=__contains__,
    __delitem__=__delitem__,
    __getitem__=__getitem__,
    __setitem__=__setitem__,
    __iadd__=__iadd__,
    __iand__=__iand__,
    __iconcat__=__iconcat__,
    __ifloordiv__=__ifloordiv__,
    __ilshift__=__ilshift__,
    __imod__=__imod__,
    __imul__=__imul__,
    __imatmul__=__imatmul__,
    __ior__=__ior__,
    __ipow__=__ipow__,
    __irshift__=__irshift__,
    __isub__=__isub__,
    __itruediv__=__itruediv__,
    __ixor__=__ixor__,
)
