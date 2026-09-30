load("@stdlib//collections", collections="collections")
load("@stdlib//unittest", unittest="unittest")
load("@stdlib//larky", larky="larky")
load("@vendor//asserts", asserts="asserts")


namedtuple = collections.namedtuple


def _test_namedtuple():
    _Curve = namedtuple("_Curve", "p b")
    z = _Curve(1, 2)
    asserts.assert_that(z[::-1]).is_equal_to((2, 1))
    asserts.assert_that(z[0]).is_equal_to(1)
    asserts.assert_that(z[1]).is_equal_to(2)
    asserts.assert_that(z.p).is_equal_to(z[0])
    asserts.assert_that(z.b).is_equal_to(z[1])
    asserts.assert_that(z[1:]).is_equal_to((2,))
    asserts.assert_that((1 in z)).is_equal_to(True)

    def testunpack(*args):
        asserts.assert_that(args).is_equal_to((1, 2))
    testunpack(*z)

    asserts.assert_that(z._fields).is_equal_to(("p", "b"))
    asserts.assert_that(z._as_dict()).is_equal_to({"p": 1, "b": 2})
    asserts.assert_that(repr(z._make(range(2)))).is_equal_to("_Curve(p=0, b=1)")


def _test_namedtuple_replace():
    Point = namedtuple( "Point" , [ "x" , "y" ] )
    p = Point( 0 , 1 )
    asserts.assert_that( p._fields).is_equal_to( ( "x" , "y" ))

    asserts.assert_that( p[0] ).is_equal_to( 0 )
    asserts.assert_that( p[1]).is_equal_to(  1 )
    asserts.assert_that( p.x ).is_equal_to( 0 )
    asserts.assert_that( p.y ).is_equal_to(1)

    asserts.assert_that( list( p ) ).is_equal_to( [ 0 , 1 ])

    q = p._replace( x=33 )
    asserts.assert_that( q._fields).is_equal_to( ( "x" , "y" ))
    asserts.assert_that( q[0] ).is_equal_to( 33 )
    asserts.assert_that( q[1]).is_equal_to(  1 )
    asserts.assert_that( q.x ).is_equal_to( 33 )
    asserts.assert_that( q.y ).is_equal_to(1)


def _test_namedtuple_equality_and_hash():
    # Expected values from CPython 3: a namedtuple compares and hashes by its
    # elements, like the tuple it is.
    P = namedtuple("P", ["x", "y"])
    Q = namedtuple("Q", ["a", "b"])
    asserts.assert_that(P(1, 2) == P(1, 2)).is_true()
    asserts.assert_that(P(1, 2) != P(1, 2)).is_false()
    asserts.assert_that(P(1, 2) == P(1, 3)).is_false()
    asserts.assert_that(P(1, 2) != P(1, 3)).is_true()
    asserts.assert_that(P(1, 2) == Q(1, 2)).is_true()
    asserts.assert_that(P(1, 2) <= P(1, 2)).is_true()
    asserts.assert_that(P(1, 2) < P(1, 2)).is_false()
    asserts.assert_that(P(1, 2) < P(1, 3)).is_true()
    asserts.assert_that(P(2, 0) > P(1, 3)).is_true()
    asserts.assert_that(sorted([P(2, 1), P(1, 2)])).is_equal_to([P(1, 2), P(2, 1)])
    asserts.assert_that({P(1, 2): 1}.get(P(1, 2))).is_equal_to(1)
    asserts.assert_that(len(set([P(1, 2), P(1, 2)]))).is_equal_to(1)
    d = {P(1, 2): 1}
    d[Q(1, 2)] = 2
    asserts.assert_that(d).is_equal_to({P(1, 2): 2})
    asserts.assert_that(P(1, 2) in [P(1, 2)]).is_true()
    asserts.assert_fails(lambda: {P([1], 2): 1}, "unhashable type: 'list'")
    asserts.assert_fails(lambda: P(1, "a") < P(1, 2), "unsupported comparison")


def _test_namedtuple_is_a_tuple():
    # Expected values from CPython 3: a namedtuple is a tuple.
    P = namedtuple("P", ["x", "y"])
    D = namedtuple("D", ["a", "b"], defaults=[7])
    p = P(1, 2)
    # Equal to, hashing like and ordering against the plain tuple.
    asserts.assert_that(p == (1, 2)).is_true()
    asserts.assert_that((1, 2) == p).is_true()
    asserts.assert_that(p != (1, 2)).is_false()
    asserts.assert_that({(1, 2): "t"}.get(p)).is_equal_to("t")
    asserts.assert_that({p: "n"}.get((1, 2))).is_equal_to("n")
    asserts.assert_that(len(set([p, (1, 2)]))).is_equal_to(1)
    asserts.assert_that(p < (1, 3)).is_true()
    asserts.assert_that((1, 1) < p).is_true()
    asserts.assert_that(sorted([P(2, 0), (1, 5)])).is_equal_to([(1, 5), (2, 0)])
    # Tuple operations; slicing, + and * give plain tuples.
    asserts.assert_that(p[-1]).is_equal_to(2)
    asserts.assert_that(type(p[0:1])).is_equal_to("tuple")
    asserts.assert_that(p + (3,)).is_equal_to((1, 2, 3))
    asserts.assert_that((0,) + p).is_equal_to((0, 1, 2))
    asserts.assert_that(p + p).is_equal_to((1, 2, 1, 2))
    asserts.assert_that(p * 2).is_equal_to((1, 2, 1, 2))
    asserts.assert_that(type(p + (3,))).is_equal_to("tuple")
    asserts.assert_that(tuple(p)).is_equal_to((1, 2))
    asserts.assert_that(type(tuple(p))).is_equal_to("tuple")
    asserts.assert_that(p.count(1)).is_equal_to(1)
    asserts.assert_that(p.index(2)).is_equal_to(1)
    asserts.assert_fails(lambda: p.index(9), "tuple.index\\(x\\): x not in tuple")
    # Names.
    asserts.assert_that(repr(p)).is_equal_to("P(x=1, y=2)")
    asserts.assert_that(p._asdict()).is_equal_to({"x": 1, "y": 2})
    asserts.assert_that(repr(p._replace(x=9))).is_equal_to("P(x=9, y=2)")
    asserts.assert_that(P._fields).is_equal_to(("x", "y"))
    asserts.assert_that(repr(P._make([5, 6]))).is_equal_to("P(x=5, y=6)")
    asserts.assert_fails(lambda: P._make([1]), "Expected 2 arguments, got 1")
    # Arguments bind as in Python's generated __new__, including defaults.
    asserts.assert_that(repr(P(y=2, x=1))).is_equal_to("P(x=1, y=2)")
    asserts.assert_that(repr(P(1, y=2))).is_equal_to("P(x=1, y=2)")
    asserts.assert_that(repr(D(1))).is_equal_to("D(a=1, b=7)")
    asserts.assert_that(repr(D(1, 5))).is_equal_to("D(a=1, b=5)")
    asserts.assert_fails(lambda: P(1), "missing 1 required positional argument: 'y'")
    asserts.assert_fails(lambda: P(1, 2, 3), "takes 3 positional arguments but 4 were given")
    asserts.assert_fails(lambda: P(1, z=2), "got an unexpected keyword argument 'z'")
    asserts.assert_fails(lambda: P(1, x=2), "got multiple values for argument 'x'")
    asserts.assert_fails(lambda: p._replace(z=1), "Got unexpected field names")


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_namedtuple))
    _suite.addTest(unittest.FunctionTestCase(_test_namedtuple_replace))
    _suite.addTest(unittest.FunctionTestCase(_test_namedtuple_equality_and_hash))
    _suite.addTest(unittest.FunctionTestCase(_test_namedtuple_is_a_tuple))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
