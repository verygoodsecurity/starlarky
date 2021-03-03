def lt(a, b):
    """
    Same as a < b.
    """
def le(a, b):
    """
    Same as a <= b.
    """
def eq(a, b):
    """
    Same as a == b.
    """
def ne(a, b):
    """
    Same as a != b.
    """
def ge(a, b):
    """
    Same as a >= b.
    """
def gt(a, b):
    """
    Same as a > b.
    """
def not_(a):
    """
    Same as not a.
    """
def truth(a):
    """
    Return True if a is true, False otherwise.
    """
def is_(a, b):
    """
    Same as a is b.
    """
def is_not(a, b):
    """
    Same as a is not b.
    """
def abs(a):
    """
    Same as abs(a).
    """
def add(a, b):
    """
    Same as a + b.
    """
def and_(a, b):
    """
    Same as a & b.
    """
def floordiv(a, b):
    """
    Same as a // b.
    """
def index(a):
    """
    Same as a.__index__().
    """
def inv(a):
    """
    Same as ~a.
    """
def lshift(a, b):
    """
    Same as a << b.
    """
def mod(a, b):
    """
    Same as a % b.
    """
def mul(a, b):
    """
    Same as a * b.
    """
def matmul(a, b):
    """
    Same as a @ b.
    """
def neg(a):
    """
    Same as -a.
    """
def or_(a, b):
    """
    Same as a | b.
    """
def pos(a):
    """
    Same as +a.
    """
def pow(a, b):
    """
    Same as a ** b.
    """
def rshift(a, b):
    """
    Same as a >> b.
    """
def sub(a, b):
    """
    Same as a - b.
    """
def truediv(a, b):
    """
    Same as a / b.
    """
def xor(a, b):
    """
    Same as a ^ b.
    """
def concat(a, b):
    """
    Same as a + b, for a and b sequences.
    """
def contains(a, b):
    """
    Same as b in a (note reversed operands).
    """
def countOf(a, b):
    """
    Return the number of times b occurs in a.
    """
def delitem(a, b):
    """
    Same as del a[b].
    """
def getitem(a, b):
    """
    Same as a[b].
    """
def indexOf(a, b):
    """
    Return the first index of b in a.
    """
def setitem(a, b, c):
    """
    Same as a[b] = c.
    """
def length_hint(obj, default=0):
    """

        Return an estimate of the number of items in obj.
        This is useful for presizing containers when building from an iterable.

        If the object supports len(), the result will be exact. Otherwise, it may
        over- or under-estimate by an arbitrary amount. The result will be an
        integer >= 0.
    
    """
def attrgetter:
    """

        Return a callable object that fetches the given attribute(s) from its operand.
        After f = attrgetter('name'), the call f(r) returns r.name.
        After g = attrgetter('name', 'date'), the call g(r) returns (r.name, r.date).
        After h = attrgetter('name.first', 'name.last'), the call h(r) returns
        (r.name.first, r.name.last).
    
    """
    def __init__(self, attr, *attrs):
        """
        'attribute name must be a string'
        """
            def func(obj):
                """
                '%s.%s(%s)'
                """
    def __reduce__(self):
        """

            Return a callable object that fetches the given item(s) from its operand.
            After f = itemgetter(2), the call f(r) returns r[2].
            After g = itemgetter(2, 5, 3), the call g(r) returns (r[2], r[5], r[3])
    
        """
    def __init__(self, item, *items):
        """
        '%s.%s(%s)'
        """
    def __reduce__(self):
        """

            Return a callable object that calls the given method on its operand.
            After f = methodcaller('name'), the call f(r) returns r.name().
            After g = methodcaller('name', 'date', foo=1), the call g(r) returns
            r.name('date', foo=1).
    
        """
    def __init__(self, name, /, *args, **kwargs):
        """
        'method name must be a string'
        """
    def __call__(self, obj):
        """
        '%s=%r'
        """
    def __reduce__(self):
        """
         In-place Operations *********************************************************#


        """
def iadd(a, b):
    """
    Same as a += b.
    """
def iand(a, b):
    """
    Same as a &= b.
    """
def iconcat(a, b):
    """
    Same as a += b, for a and b sequences.
    """
def ifloordiv(a, b):
    """
    Same as a //= b.
    """
def ilshift(a, b):
    """
    Same as a <<= b.
    """
def imod(a, b):
    """
    Same as a %= b.
    """
def imul(a, b):
    """
    Same as a *= b.
    """
def imatmul(a, b):
    """
    Same as a @= b.
    """
def ior(a, b):
    """
    Same as a |= b.
    """
def ipow(a, b):
    """
    Same as a **= b.
    """
def irshift(a, b):
    """
    Same as a >>= b.
    """
def isub(a, b):
    """
    Same as a -= b.
    """
def itruediv(a, b):
    """
    Same as a /= b.
    """
def ixor(a, b):
    """
    Same as a ^= b.
    """
