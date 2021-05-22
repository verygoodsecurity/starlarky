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

#_abs = abs
#_pow = pow


# Comparison Operations *******************************************************#

def lt(a, b):
    "Same as a < b."
    return a < b

def le(a, b):
    "Same as a <= b."
    return a <= b

def eq(a, b):
    "Same as a == b."
    return a == b

def ne(a, b):
    "Same as a != b."
    return a != b

def ge(a, b):
    "Same as a >= b."
    return a >= b

def gt(a, b):
    "Same as a > b."
    return a > b

# Logical Operations **********************************************************#

def not_(a):
    "Same as not a."
    return not a

def truth(a):
    "Return True if a is true, False otherwise."
    return True if a else False

def is_(a, b):
    "Same as a is b."
    return a == b

def is_not(a, b):
    "Same as a is not b."
    return a != b

# Mathematical/Bitwise Operations *********************************************#

def abs_(a):
    "Same as abs(a)."
    return abs(a)

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
    return a.__index__()

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

def pow_(a, b):
    "Same as a ** b."
    return pow_(a, b)

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
    if not hasattr(a, '__getitem__'):
        msg = "'%s' object can't be concatenated" % type(a)
        fail("TypeError: " + msg)
    return a + b

def contains(a, b):
    "Same as b in a (note reversed operands)."
    return b in a

def countOf(a, b):
    "Return the number of times b occurs in a."
    count = 0
    for i in a:
        if i == b:
            count += 1
    return count

def delitem(a, b):
    "Same as del a[b]."
    a.__delitem__(b)   # no del in starlark!

def getitem(a, b):
    "Same as a[b]."
    return a.__getitem__(b) if hasattr(a, '__getitem__') else a[b]

def indexOf(a, b):
    "Return the first index of b in a."
    for i, j in enumerate(a):
        if j == b:
            return i
    fail("ValueError: " + 'sequence.index(x): x not in sequence')

def setitem(a, b, c):
    "Same as a[b] = c."
    if hasattr(a, '__setitem__'):
        a.__setitem__(b, c)
    else:
        a[b] = c

def length_hint(obj, default=0):
    """
    Return an estimate of the number of items in obj.
    This is useful for presizing containers when building from an iterable.
    If the object supports len(), the result will be exact. Otherwise, it may
    over- or under-estimate by an arbitrary amount. The result will be an
    integer >= 0.
    """
    fail("not implemented")

def itemgetter(*args):
    def _itemgetter(obj):
        return obj[args[0]]
    def _itemsgetter(obj):
        return tuple([obj[i] for i in args])
    if len(args) == 1:
        return _itemgetter
    return _itemsgetter


def attrgetter(attr):
    if "." in attr:
        fail('"." in %s' % attr)
    def _attrgetter(obj):
        return getattr(obj, attr)
    return _attrgetter


def methodcaller(name, *args, **kwargs):
    def _methodcaller(obj):
        return getattr(obj, name)(*args, **kwargs)
    return _methodcaller


# In-place Operations *********************************************************#

def iadd(a, b):
    "Same as a += b."
    a += b
    return a

def iand(a, b):
    "Same as a &= b."
    a &= b
    return a

def iconcat(a, b):
    "Same as a += b, for a and b sequences."
    if not hasattr(a, '__getitem__'):
        msg = "'%s' object can't be concatenated" % type(a).__name__
        fail("TypeError: " + msg)
    a += b
    return a

def ifloordiv(a, b):
    "Same as a //= b."
    a //= b
    return a

def ilshift(a, b):
    "Same as a <<= b."
    a <<= b
    return a

def imod(a, b):
    "Same as a %= b."
    a %= b
    return a

def imul(a, b):
    "Same as a *= b."
    a *= b
    return a

def imatmul(a, b):
    "Same as a @= b."
    a = matmul(a, b)
    return a

def ior(a, b):
    "Same as a |= b."
    a |= b
    return a

def ipow(a, b):
    "Same as a **= b."
    a = pow(a, b)
    return a

def irshift(a, b):
    "Same as a >>= b."
    a >>= b
    return a

def isub(a, b):
    "Same as a -= b."
    a -= b
    return a

def itruediv(a, b):
    "Same as a /= b."
    a /= b
    return a

def ixor(a, b):
    "Same as a ^= b."
    a ^= b
    return a

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
    abs_=abs_,
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
    pow_=pow_,
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
