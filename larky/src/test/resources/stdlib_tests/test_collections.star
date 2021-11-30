load("@stdlib//collections", collections="collections")
load("@stdlib//unittest", unittest="unittest")
load("@stdlib//larky", WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION", larky="larky")
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


def _test_deque_simple():
    d = collections.deque([1, 6, 2, 4])
    asserts.assert_that(list(d)).is_equal_to([1, 6, 2, 4])
    d = d.__reversed__()
    asserts.assert_that(list(d)).is_equal_to(list(reversed([1, 6, 2, 4])))


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_namedtuple))
    _suite.addTest(unittest.FunctionTestCase(_test_namedtuple_replace))
    _suite.addTest(unittest.FunctionTestCase(_test_deque_simple))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
