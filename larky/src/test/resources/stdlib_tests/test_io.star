load("@stdlib//io", io="io")
load("@stdlib//types", types="types")
load("@stdlib//unittest", unittest="unittest")
load("@stdlib//codecs", codecs="codecs")

load("@stdlib//larky", larky="larky")
load("@vendor//asserts", asserts="asserts")

# TODO: port over https://github.com/python/cpython/blob/main/Lib/test/test_io.py

def _test_StringIO():
    # issue 1047
    s = io.StringIO()
    s.write(chr(8364))
    asserts.assert_that(s.getvalue()).is_equal_to("€")
    s = chr(8364)
    asserts.assert_that(s).is_equal_to("€")
    b = codecs.encode(s, encoding="utf-8")
    asserts.assert_that(b).is_equal_to(bytes([0xe2, 0x82, 0xac]))
    s1 = b.decode("utf-8")
    asserts.assert_that(s1).is_equal_to("€")

    # issue 1690
    s = io.StringIO('abc')
    asserts.assert_that(s.tell()).is_equal_to(0)
    asserts.assert_that(s.write('defg')).is_equal_to(4)
    asserts.assert_that(s.tell()).is_equal_to(4)
    asserts.assert_that(s.getvalue()).is_equal_to('defg')

    s = io.StringIO('abc')
    asserts.assert_that(s.write('d')).is_equal_to(1)
    asserts.assert_that(s.tell()).is_equal_to(1)
    asserts.assert_that(s.getvalue()).is_equal_to('dbc')
    asserts.assert_that(s.write('e')).is_equal_to(1)
    asserts.assert_that(s.tell()).is_equal_to(2)
    asserts.assert_that(s.getvalue()).is_equal_to('dec')

    s = io.StringIO('abc')
    s.seek(2)
    s.write('x')
    asserts.assert_that(s.getvalue()).is_equal_to('abx')

    # issue
    s = io.StringIO('foo\n  bar\n  baz')
    index = 0
    t = []
    for _while_ in larky.while_true():
        line = s.readline()
        t.append(line)
        index += 1
        if not line:
            break

    asserts.assert_that(t).is_equal_to(['foo\n', '  bar\n', '  baz', ''])


def _test_BytesIO():
    b = io.BytesIO(b'foo\n  bar\n  baz')
    index = 0
    t = []
    for _while_ in larky.while_true():
        line = b.readline()
        t.append(line)
        index += 1
        if not line:
            break

    asserts.assert_that(t).is_equal_to([b'foo\n', b'  bar\n', b'  baz', b''])


def _test_issue1763():
    # issue #1763
    # problem with readlines working wrong on linux text files.

    def myreadlines(sio):
        """Read and return the list of all logical lines using readline."""
        lines = []
        for _while_ in larky.while_true():
            line = sio.readline()
            if not line:
                return lines
            else:
                lines.append(line)

    # test readline
    #, r"myreadline failed with \n\n")
    asserts.assert_that(len(myreadlines(io.StringIO("foo\n\n\n")))).is_equal_to(3)
    #, r"myreadline failed with \r\n\r\n")
    asserts.assert_that(len(myreadlines(io.StringIO("foo\r\n\r\n\r\n")))).is_equal_to(3)

    # Test readlines()
    #, r"readlines failed with \r\n")
    asserts.assert_that(len(io.StringIO("foo\r\n\r\n\r\n").readlines())).is_equal_to(3)

    #, r"readlines failed with \n\n!")
    asserts.assert_that(len(io.StringIO("foo\n\n\n").readlines())).is_equal_to(3)



def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_StringIO))
    _suite.addTest(unittest.FunctionTestCase(_test_BytesIO))
    _suite.addTest(unittest.FunctionTestCase(_test_issue1763))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
