# Values whose Java classes used to keep the StarlarkThread that created them, all built while
# this module loads (ThreadFieldIsolationTest).
load("@stdlib//collections", "collections")

def _eq(self, other):
    print("eq ran")
    return True

def _boom(self, other):
    fail("eq boom")

A = type("A", (), {"__eq__": _eq})
A1 = A()
A2 = A()
B = type("B", (), {"__eq__": _boom})
B1 = B()
B2 = B()
C = type("C", (A,), {})
SUPER = super(C, C())
Point = collections.namedtuple("Point", "x y")
P = Point(1, 2)
IT = iter([1, 2, 3])
