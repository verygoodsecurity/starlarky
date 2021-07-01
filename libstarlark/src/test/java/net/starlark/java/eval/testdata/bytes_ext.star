# copied from:
# https://github.com/python/cpython/blob/main/Lib/test/test_bytes.py
# https://github.com/python/cpython/blob/main/Lib/test/string_tests.py


# CONSTANTS
SYS_MAXSIZE = 2147483647


# HELPERS
def assert_ne(arg1, arg2):
    assert_(arg1 != arg2, "%s == %s!" % (arg1, arg2))


def assert_true(arg1):
    assert_(arg1, "bool(%s) is falsy" % arg1)


def assert_false(arg1):
    assert_(not arg1, "bool(%s) is true!" % arg1)


def assert_lt(arg1, arg2):
    assert_(arg1 < arg2, "%s >= %s" % (arg1, arg2))


# check that obj.method(*args) returns result
def checkequal(result, obj, methodname, *args, **kwargs):
    realresult = getattr(obj, methodname)(*args, **kwargs)
    assert_eq(realresult, result)


def test_hex():
    three_bytes = b'\xb9\x01\xef'
    # test failures
    assert_fails(lambda: three_bytes.hex(''), ".*sep must be length 1")
    assert_fails(lambda: three_bytes.hex('xx'), ".*sep must be length 1")
    assert_fails(lambda: three_bytes.hex(None, 0),
                 "parameter 'sep' got value of type 'NoneType', want 'string or bytes'")
    assert_fails(lambda: three_bytes.hex('\u00ff'),
                 ".*must be ASCII.")
    assert_fails(lambda: three_bytes.hex(b'\xff'), ".*must be ASCII.")
    assert_fails(lambda: three_bytes.hex(b'\x80'), ".*must be ASCII.")

    assert_eq(three_bytes.hex(), 'b901ef')
    assert_eq(three_bytes.hex(':', 0), 'b901ef')
    assert_eq(three_bytes.hex(':', 0), 'b901ef')
    assert_eq(three_bytes.hex(b'\x00'), 'b9\x0001\x00ef')
    assert_eq(three_bytes.hex('\x00'), 'b9\x0001\x00ef')
    assert_eq(three_bytes.hex(b'\x7f'), 'b9\x7f01\x7fef')
    assert_eq(three_bytes.hex('\x7f'), 'b9\x7f01\x7fef')
    assert_eq(three_bytes.hex(':', 3), 'b901ef')
    assert_eq(three_bytes.hex(':', 4), 'b901ef')
    assert_eq(three_bytes.hex(':', -4), 'b901ef')
    assert_eq(three_bytes.hex(':'), 'b9:01:ef')
    assert_eq(three_bytes.hex(b'$'), 'b9$01$ef')
    assert_eq(three_bytes.hex(':', 1), 'b9:01:ef')
    assert_eq(three_bytes.hex(':', -1), 'b9:01:ef')
    assert_eq(three_bytes.hex(':', 2), 'b9:01ef')
    assert_eq(three_bytes.hex(':', 1), 'b9:01:ef')
    assert_eq(three_bytes.hex('*', -2), 'b901*ef')

    value = b'{s\005\000\000\000worldi\002\000\000\000s\005\000\000\000helloi\001\000\000\0000'
    assert_eq(value.hex('.', 8), '7b7305000000776f.726c646902000000.730500000068656c.6c6f690100000030')


def test_count():
    # b.count
    b = b'mississippi'
    i = 105
    p = 112
    w = 119

    assert_eq(b.count(b'i'), 4)
    assert_eq(b.count(b'ss'), 2)
    assert_eq(b.count(b'w'), 0)

    assert_eq(b.count(i), 4)
    assert_eq(b.count(w), 0)

    assert_eq(b.count(b'i', 6), 2)
    assert_eq(b.count(b'p', 6), 2)
    assert_eq(b.count(b'i', 1, 3), 1)
    assert_eq(b.count(b'p', 7, 9), 1)

    assert_eq(b.count(i, 6), 2)
    assert_eq(b.count(p, 6), 2)
    assert_eq(b.count(i, 1, 3), 1)
    assert_eq(b.count(p, 7, 9), 1)

    # count.test_none_arguments
    nonetest_b = b'hello'
    nonetest_l = b'l'
    nonetest_h = b'h'
    nonetest_x = b'x'
    nonetest_o = b'o'
    assert_eq(2, nonetest_b.count(nonetest_l, None))
    assert_eq(1, nonetest_b.count(nonetest_l, -2, None))
    assert_eq(1, nonetest_b.count(nonetest_l, None, -2))
    assert_eq(0, nonetest_b.count(nonetest_x, None, None))


def test_endswith():
    b = b'hello'
    assert_false(bytes('').endswith(b"anything"))
    assert_true(b.endswith(b"hello"))
    assert_true(b.endswith(b"llo"))
    assert_true(b.endswith(b"o"))
    assert_false(b.endswith(b"whello"))
    assert_false(b.endswith(b"no"))
    assert_fails(lambda: b.endswith([b'o']), '.*bytes or tuple.*')
    # count.test_none_arguments
    nonetest_b = b'hello'
    nonetest_l = b'l'
    nonetest_h = b'h'
    nonetest_x = b'x'
    nonetest_o = b'o'
    assert_true(nonetest_b.endswith(nonetest_o, None))
    assert_true(nonetest_b.endswith(nonetest_o, -2, None))
    assert_true(nonetest_b.endswith(nonetest_l, None, -2))
    assert_false(nonetest_b.endswith(nonetest_x, None, None))


def test_find():
    b = b'mississippi'
    i = 105
    w = 119

    assert_eq(b.find(b'ss'), 2)
    assert_eq(b.find(b'w'), -1)
    assert_eq(b.find(b'mississippian'), -1)

    assert_eq(b.find(i), 1)
    assert_eq(b.find(w), -1)

    assert_eq(b.find(b'ss', 3), 5)
    assert_eq(b.find(b'ss', 1, 7), 2)
    assert_eq(b.find(b'ss', 1, 3), -1)

    assert_eq(b.find(i, 6), 7)
    assert_eq(b.find(i, 1, 3), 1)
    assert_eq(b.find(w, 1, 3), -1)

    for index in (-1, 256, 9223372036854775807 + 1):
        assert_fails(lambda: b.find(index),
                     r'.*byte must be in range\(0, 256\)')


def test_index():
    b = b'mississippi'
    i = 105
    w = 119
    assert_eq(b.index(b'ss'), 2)
    assert_fails(lambda: b.index(b'w'), 'subsection not found')
    assert_fails(lambda: b.index(b'mississippian'), 'subsection not found')

    assert_eq(b.index(i), 1)
    assert_fails(lambda: b.index(w), 'subsection not found')

    assert_eq(b.index(b'ss', 3), 5)
    assert_eq(b.index(b'ss', 1, 7), 2)
    assert_fails(lambda: b.index(b'ss', 1, 3), 'subsection not found')

    assert_eq(b.index(i, 6), 7)
    assert_eq(b.index(i, 1, 3), 1)
    assert_fails(lambda: b.index(w, 1, 3), 'subsection not found')


def test_join():
    assert_eq(b"".join([]), b"")
    assert_eq(b"".join([b""]), b"")
    for lst in [[b"abc"], [b"a", b"bc"], [b"ab", b"c"], [b"a", b"b", b"c"]]:
        assert_eq(b"".join(lst), b"abc")
        assert_eq(b"".join(tuple(lst)), b"abc")
        assert_eq(b"".join([i for i in lst]), b"abc")
    dot_join = b".:".join
    assert_eq(dot_join([b"ab", b"cd"]), b"ab.:cd")
    # Stress it with many items
    seq = [b"abc"] * 100000
    expected = b"abc" + b".:abc" * 99999
    assert_eq(dot_join(seq), expected)
    # Stress test with empty separator
    seq = [b"abc"] * 100000
    expected = b"abc" * 100000
    assert_eq(b"".join(seq), expected)
    assert_fails(lambda: b" ".join(None),
                 ".*got value of type 'NoneType', want 'sequence'")


def test_partition():
    b = b'mississippi'
    assert_eq(b.partition(b'ss'), (b'mi', b'ss', b'issippi'))
    assert_eq(b.partition(b'w'), (b'mississippi', b'', b''))
    assert_fails(lambda: b'a b'.partition(' '),
                 "got value of type 'string', want 'bytes'")
    assert_fails(lambda: b'a b'.partition(32),
                 "got value of type 'int', want 'bytes'")
    checkequal((b'this is the par', b'ti', b'tion method'),
        b'this is the partition method', 'partition', b'ti')

    # from raymond's original specification
    S = b'http://www.python.org'
    checkequal((b'http', b'://', b'www.python.org'), S, 'partition', b'://')
    checkequal((b'http://www.python.org', b'', b''), S, 'partition', b'?')
    checkequal((b'', b'http://', b'www.python.org'), S, 'partition', b'http://')
    checkequal((b'http://www.python.', b'org', b''), S, 'partition', b'org')


def test_rpartition():
    b = b'mississippi'
    assert_eq(b.rpartition(b'ss'), (b'missi', b'ss', b'ippi'))
    assert_eq(b.rpartition(b'i'), (b'mississipp', b'i', b''))
    assert_eq(b.rpartition(b'w'), (b'', b'', b'mississippi'))
    assert_fails(lambda: b'a b'.rpartition(' '),
                 "got value of type 'string', want 'bytes'")
    assert_fails(lambda: b'a b'.rpartition(32),
                 "got value of type 'int', want 'bytes'")
    checkequal((b'this is the rparti', b'ti', b'on method'),
        b'this is the rpartition method', 'rpartition', b'ti')

    # from raymond's original specification
    S = b'http://www.python.org'
    checkequal((b'http', b'://', b'www.python.org'), S, 'rpartition', b'://')
    checkequal((b'', b'', b'http://www.python.org'), S, 'rpartition', b'?')
    checkequal((b'', b'http://', b'www.python.org'), S, 'rpartition', b'http://')
    checkequal((b'http://www.python.', b'org', b''), S, 'rpartition', b'org')


def test_startswith():
    b = b'hello'
    assert_false(b"".startswith(b"anything"))
    assert_true(b.startswith(b"hello"))
    assert_true(b.startswith(b"hel"))
    assert_true(b.startswith(b"h"))
    assert_false(b.startswith(b"hellow"))
    assert_false(b.startswith(b"ha"))
    assert_fails(lambda: b.startswith([b'h']), '.*bytes or tuple.*')


def test_replace():
    b = b'mississippi'
    assert_eq(b.replace(b'i', b'a'), b'massassappa')
    assert_eq(b.replace(b'ss', b'x'), b'mixixippi')
    assert_fails(lambda: b'a b'.replace(32), ".*got value of type 'int', want 'bytes'")


def test_split():
    assert_fails(lambda: b'a b'.split(' '),
                 "got value of type 'string', want 'bytes or NoneType'")
    assert_fails(lambda: b'a b'.split(32),
                 "got value of type 'int', want 'bytes or NoneType'")
    for b in (b'a\x1Cb', b'a\x1Db', b'a\x1Eb', b'a\x1Fb'):
        assert_eq(b.split(), [b])
    b = b"\x09\x0A\x0B\x0C\x0D\x1C\x1D\x1E\x1F"
    assert_eq(b.split(), [b'\x1c\x1d\x1e\x1f'])
    assert_eq(b'a b'.split(b' '), [b'a', b'b'])
    # by 1 byte
    checkequal([b'a', b'b', b'c', b'd'], b'a|b|c|d', 'split', b'|')
    checkequal([b'a|b|c|d'], b'a|b|c|d', 'split', b'|', 0)
    checkequal([b'a', b'b|c|d'], b'a|b|c|d', 'split', b'|', 1)
    checkequal([b'a', b'b', b'c|d'], b'a|b|c|d', 'split', b'|', 2)
    checkequal([b'a', b'b', b'c', b'd'], b'a|b|c|d', 'split', b'|', 3)
    checkequal([b'a', b'b', b'c', b'd'], b'a|b|c|d', 'split', b'|', 4)
    checkequal([b'a', b'b', b'c', b'd'], b'a|b|c|d', 'split', b'|', SYS_MAXSIZE-2)
    checkequal([b'a|b|c|d'], b'a|b|c|d', 'split', b'|', 0)
    checkequal([b'a', b'', b'b||c||d'], b'a||b||c||d', 'split', b'|', 2)
    checkequal([b'abcd'], b'abcd', 'split', b'|')
    checkequal([b''], b'', 'split', b'|')
    checkequal([b'endcase ', b''], b'endcase |', 'split', b'|')
    checkequal([b'', b' startcase'], b'| startcase', 'split', b'|')
    checkequal([b'', b'bothcase', b''], b'|bothcase|', 'split', b'|')
    checkequal([b'a', b'', b'b\x00c\x00d'], b'a\x00\x00b\x00c\x00d', 'split', b'\x00', 2)

    checkequal([b'a']*20, (b'a|'*20)[:-1], 'split', b'|')
    checkequal([b'a']*15 +[b'a|a|a|a|a'],
                               (b'a|'*20)[:-1], 'split', b'|', 15)

    # by bytestring
    checkequal([b'a', b'b', b'c', b'd'], b'a//b//c//d', 'split', b'//')
    checkequal([b'a', b'b//c//d'], b'a//b//c//d', 'split', b'//', 1)
    checkequal([b'a', b'b', b'c//d'], b'a//b//c//d', 'split', b'//', 2)
    checkequal([b'a', b'b', b'c', b'd'], b'a//b//c//d', 'split', b'//', 3)
    checkequal([b'a', b'b', b'c', b'd'], b'a//b//c//d', 'split', b'//', 4)
    checkequal([b'a', b'b', b'c', b'd'], b'a//b//c//d', 'split', b'//',SYS_MAXSIZE-10)
    checkequal([b'a//b//c//d'], b'a//b//c//d', 'split', b'//', 0)
    checkequal([b'a', b'b', b'c//d'], b'a//b//c//d', 'split', b'//', 2)
    checkequal([b'endcase ', b''], b'endcase test', 'split', b'test')
    checkequal([b'', b' begincase'], b'test begincase', 'split', b'test')
    checkequal([b'', b' bothcase ', b''], b'test bothcase test',
                    'split', b'test')
    checkequal([b'a', b'bc'], b'abbbc', 'split', b'bb')
    checkequal([b'', b''], b'aaa', 'split', b'aaa')
    checkequal([b'aaa'], b'aaa', 'split', b'aaa', 0)
    checkequal([b'ab', b'ab'], b'abbaab', 'split', b'ba')
    checkequal([b'aaaa'], b'aaaa', 'split', b'aab')
    checkequal([b''], b'', 'split', b'aaa')
    checkequal([b'aa'], b'aa', 'split', b'aaa')
    checkequal([b'A', b'bobb'], b'Abbobbbobb', 'split', b'bbobb')
    checkequal([b'A', b'B', b''], b'AbbobbBbbobb', 'split', b'bbobb')

    checkequal([b'a']*20, (b'aBLAH'*20)[:-4], 'split', b'BLAH')
    checkequal([b'a']*20, (b'aBLAH'*20)[:-4], 'split', b'BLAH', 19)
    checkequal([b'a']*18 + [b'aBLAHa'], (b'aBLAH'*20)[:-4],
                    'split', b'BLAH', 18)

    # with keyword args
    checkequal([b'a', b'b', b'c', b'd'], b'a|b|c|d', 'split', sep=b'|')
    checkequal([b'a', b'b|c|d'], b'a|b|c|d', 'split', b'|', maxsplit=1)
    checkequal([b'a', b'b|c|d'], b'a|b|c|d', 'split', sep=b'|', maxsplit=1)
    checkequal([b'a', b'b|c|d'], b'a|b|c|d', 'split', maxsplit=1, sep=b'|')
    checkequal([b'a', b'b c d'], b'a b c d', 'split', maxsplit=1)


def test_rsplit():
    assert_fails(lambda: b'a b'.rsplit(' '),
                 "got value of type 'string', want 'bytes or NoneType'")
    assert_fails(lambda: b'a b'.rsplit(32),
                 "got value of type 'int', want 'bytes or NoneType'")
    b = b"\x09\x0A\x0B\x0C\x0D\x1C\x1D\x1E\x1F"
    assert_eq(b.rsplit(), [b'\x1c\x1d\x1e\x1f'])
    assert_eq(b'a b'.rsplit(b' '), [b'a', b'b'])
    # by a char
    checkequal([b'a', b'b', b'c', b'd'], b'a|b|c|d', 'rsplit', b'|')
    checkequal([b'a|b|c', b'd'], b'a|b|c|d', 'rsplit', b'|', 1)
    checkequal([b'a|b', b'c', b'd'], b'a|b|c|d', 'rsplit', b'|', 2)
    checkequal([b'a', b'b', b'c', b'd'], b'a|b|c|d', 'rsplit', b'|', 3)
    checkequal([b'a', b'b', b'c', b'd'], b'a|b|c|d', 'rsplit', b'|', 4)
    checkequal([b'a', b'b', b'c', b'd'], b'a|b|c|d', 'rsplit', b'|',
               SYS_MAXSIZE-100)
    checkequal([b'a|b|c|d'], b'a|b|c|d', 'rsplit', b'|', 0)
    checkequal([b'a||b||c', b'', b'd'], b'a||b||c||d', 'rsplit', b'|', 2)
    checkequal([b'abcd'], b'abcd', 'rsplit', b'|')
    checkequal([b''], b'', 'rsplit', b'|')
    checkequal([b'', b' begincase'], b'| begincase', 'rsplit', b'|')
    checkequal([b'endcase ', b''], b'endcase |', 'rsplit', b'|')
    checkequal([b'', b'bothcase', b''], b'|bothcase|', 'rsplit', b'|')

    checkequal([b'a\x00\x00b', b'c', b'd'], b'a\x00\x00b\x00c\x00d', 'rsplit', b'\x00', 2)

    checkequal([b'a']*20, (b'a|'*20)[:-1], 'rsplit', b'|')
    checkequal([b'a|a|a|a|a']+[b'a']*15,
                 (b'a|'*20)[:-1], 'rsplit', b'|', 15)

    # by string
    checkequal([b'a', b'b', b'c', b'd'], b'a//b//c//d', 'rsplit', b'//')
    checkequal([b'a//b//c', b'd'], b'a//b//c//d', 'rsplit', b'//', 1)
    checkequal([b'a//b', b'c', b'd'], b'a//b//c//d', 'rsplit', b'//', 2)
    checkequal([b'a', b'b', b'c', b'd'], b'a//b//c//d', 'rsplit', b'//', 3)
    checkequal([b'a', b'b', b'c', b'd'], b'a//b//c//d', 'rsplit', b'//', 4)
    checkequal([b'a', b'b', b'c', b'd'], b'a//b//c//d', 'rsplit', b'//',
               SYS_MAXSIZE-5)
    checkequal([b'a//b//c//d'], b'a//b//c//d', 'rsplit', b'//', 0)
    checkequal([b'a////b////c', b'', b'd'], b'a////b////c////d', 'rsplit', b'//', 2)
    checkequal([b'', b' begincase'], b'test begincase', 'rsplit', b'test')
    checkequal([b'endcase ', b''], b'endcase test', 'rsplit', b'test')
    checkequal([b'', b' bothcase ', b''], b'test bothcase test',
                 'rsplit', b'test')
    checkequal([b'ab', b'c'], b'abbbc', 'rsplit', b'bb')
    checkequal([b'', b''], b'aaa', 'rsplit', b'aaa')
    checkequal([b'aaa'], b'aaa', 'rsplit', b'aaa', 0)
    checkequal([b'ab', b'ab'], b'abbaab', 'rsplit', b'ba')
    checkequal([b'aaaa'], b'aaaa', 'rsplit', b'aab')
    checkequal([b''], b'', 'rsplit', b'aaa')
    checkequal([b'aa'], b'aa', 'rsplit', b'aaa')
    checkequal([b'bbob', b'A'], b'bbobbbobbA', 'rsplit', b'bbobb')
    checkequal([b'', b'B', b'A'], b'bbobbBbbobbA', 'rsplit', b'bbobb')

    checkequal([b'a']*20, (b'aBLAH'*20)[:-4], 'rsplit', b'BLAH')
    checkequal([b'a']*20, (b'aBLAH'*20)[:-4], 'rsplit', b'BLAH', 19)
    checkequal([b'aBLAHa'] + [b'a']*18, (b'aBLAH'*20)[:-4],
                 'rsplit', b'BLAH', 18)

    # with keyword args
    checkequal([b'a', b'b', b'c', b'd'], b'a|b|c|d', 'rsplit', sep=b'|')
    checkequal([b'a|b|c', b'd'],
                 b'a|b|c|d', 'rsplit', b'|', maxsplit=1)
    checkequal([b'a|b|c', b'd'],
                 b'a|b|c|d', 'rsplit', sep=b'|', maxsplit=1)
    checkequal([b'a|b|c', b'd'],
                 b'a|b|c|d', 'rsplit', maxsplit=1, sep=b'|')
    checkequal([b'a b c', b'd'],
                 b'a b c d', 'rsplit', maxsplit=1)

def test_strip():
    assert_eq(b'abc'.strip(b'ac'), b'b')
    assert_fails(lambda: b'abc'.strip('ac'), "got value of type 'string', want 'bytes or NoneType'")
    assert_fails(lambda: b' abc '.strip(32), "got value of type 'int', want 'bytes or NoneType'")

    # whitespace
    checkequal(b'hello', b'   hello   ', 'strip')
    checkequal(b'hello', b'hello', 'strip')

    b = b' \t\n\r\f\vabc \t\n\r\f\v'
    checkequal(b'abc', b, 'strip')

    # strip with None arg
    checkequal(b'hello', b'   hello   ', 'strip', None)
    checkequal(b'hello', b'hello', 'strip', None)

    # strip with byte string arg
    checkequal(b'hello', b'xyzzyhelloxyzzy', 'strip', b'xyz')
    checkequal(b'hello', b'hello', 'strip', b'xyz')
    checkequal(b'', b'mississippi', 'strip', b'mississippi')

    # only trim the start and end; does not strip internal characters
    checkequal(b'mississipp', b'mississippi', 'strip', b'i')

    assert_fails(lambda: checkequal(b'hello', 'strip', 42, 42), "got value of type 'int', want 'string'")


def test_lstrip():
    assert_eq(b'abc'.lstrip(b'ac'), b'bc')
    assert_fails(lambda: b'abc'.lstrip('ac'), "got value of type 'string', want 'bytes or NoneType'")
    assert_fails(lambda: b' abc '.lstrip(32), "got value of type 'int', want 'bytes or NoneType'")
    checkequal(b'hello   ', b'   hello   ', 'lstrip')
    b = b' \t\n\r\f\vabc \t\n\r\f\v'
    checkequal(b'abc \t\n\r\f\v', b, 'lstrip')
    # lstrip with None arg
    checkequal(b'hello   ', b'   hello   ', 'lstrip', None)
    # lstrip with byte string arg
    checkequal(b'helloxyzzy', b'xyzzyhelloxyzzy', 'lstrip', b'xyz')
    assert_fails(lambda: checkequal(b'hello', 'lstrip', 42, 42), "got value of type 'int', want 'string'")


def test_rstrip():
    assert_eq(b'abc'.rstrip(b'ac'), b'ab')
    assert_fails(lambda: b'abc'.rstrip('ac'), "got value of type 'string', want 'bytes or NoneType'")
    assert_fails(lambda: b' abc '.rstrip(32), "got value of type 'int', want 'bytes or NoneType'")
    checkequal(b'   hello', b'   hello   ', 'rstrip')
    b = b' \t\n\r\f\vabc \t\n\r\f\v'
    checkequal(b' \t\n\r\f\vabc', b, 'rstrip')
    # rstrip with None arg
    checkequal(b'   hello', b'   hello   ', 'rstrip', None)
    # rstrip with byte string arg
    checkequal(b'xyzzyhello', b'xyzzyhelloxyzzy', 'rstrip', b'xyz')
    assert_fails(lambda: checkequal(b'hello', 'rstrip', 42, 42), "got value of type 'int', want 'string'")


def test_rindex():
    b = b'mississippi'
    i = 105
    w = 119

    assert_eq(b.rindex(b'ss'), 5)
    assert_fails(lambda: b.rindex(b'w'), "subsection not found")
    assert_fails(lambda: b.rindex(b'mississippian'), "subsection not found")

    assert_eq(b.rindex(i), 10)
    assert_fails(lambda: b.rindex(w), "subsection not found")

    assert_eq(b.rindex(b'ss', 3), 5)
    assert_eq(b.rindex(b'ss', 0, 6), 2)

    assert_eq(b.rindex(i, 1, 3), 1)
    assert_eq(b.rindex(i, 3, 9), 7)
    assert_fails(lambda: b.rindex(w, 1, 3), "subsection not found")

    checkequal(12, b'abcdefghiabc', 'rindex', b'')
    checkequal(3,  b'abcdefghiabc', 'rindex', b'def')
    checkequal(9,  b'abcdefghiabc', 'rindex', b'abc')
    checkequal(0,  b'abcdefghiabc', 'rindex', b'abc', 0, -1)

    assert_fails(lambda: checkequal(b'abcdefghiabc', 'rindex', b'hib'), "got value of type 'bytes', want 'string'")
    assert_fails(lambda: checkequal(b'defghiabc', 'rindex', b'def', 1), "got value of type 'bytes', want 'string'")
    assert_fails(lambda: checkequal(b'defghiabc', 'rindex', b'abc', 0, -1), "got value of type 'bytes', want 'string'")
    assert_fails(lambda: checkequal(b'abcdefghi', 'rindex', b'ghi', 0, 8), "got value of type 'bytes', want 'string'")
    assert_fails(lambda: checkequal(b'abcdefghi', 'rindex', b'ghi', 0, -1), "got value of type 'bytes', want 'string'")

    # to check the ability to pass None as defaults
    checkequal(12, b'rrarrrrrrrrra', 'rindex', b'a')
    checkequal(12, b'rrarrrrrrrrra', 'rindex', b'a', 4)
    assert_fails(lambda: checkequal(b'rrarrrrrrrrra', 'rindex', b'a', 4, 6), "got value of type 'bytes', want 'string'")
    checkequal(12, b'rrarrrrrrrrra', 'rindex', b'a', 4, None)
    checkequal( 2, b'rrarrrrrrrrra', 'rindex', b'a', None, 6)

    assert_fails(lambda: checkequal("fail?", b'hello', 'rindex'), "missing 1 required positional argument")

    # none tests
    b = b'hello'
    l = b'l'
    h = b'h'
    x = b'x'
    o = b'o'
    assert_eq(3, b.rindex(l, None))
    assert_eq(3, b.rindex(l, -2, None))
    assert_eq(2, b.rindex(l, None, -2))
    assert_eq(0, b.rindex(h, None, None))


def test_rfind():
    b = b'mississippi'
    i = 105
    w = 119

    assert_eq(b.rfind(b'ss'), 5)
    assert_eq(b.rfind(b'w'), -1)
    assert_eq(b.rfind(b'mississippian'), -1)

    assert_eq(b.rfind(i), 10)
    assert_eq(b.rfind(w), -1)

    assert_eq(b.rfind(b'ss', 3), 5)
    assert_eq(b.rfind(b'ss', 0, 6), 2)

    assert_eq(b.rfind(i, 1, 3), 1)
    assert_eq(b.rfind(i, 3, 9), 7)
    assert_eq(b.rfind(w, 1, 3), -1)

    checkequal(9,  b'abcdefghiabc', 'rfind', b'abc')
    checkequal(12, b'abcdefghiabc', 'rfind', b'')
    checkequal(0, b'abcdefghiabc', 'rfind', b'abcd')
    checkequal(-1, b'abcdefghiabc', 'rfind', b'abcz')

    checkequal(3, b'abc', 'rfind', b'', 0)
    checkequal(3, b'abc', 'rfind', b'', 3)
    checkequal(-1, b'abc', 'rfind', b'', 4)

    # to check the ability to pass None as defaults
    checkequal(12, b'rrarrrrrrrrra', 'rfind', b'a')
    checkequal(12, b'rrarrrrrrrrra', 'rfind', b'a', 4)
    checkequal(-1, b'rrarrrrrrrrra', 'rfind', b'a', 4, 6)
    checkequal(12, b'rrarrrrrrrrra', 'rfind', b'a', 4, None)
    checkequal( 2, b'rrarrrrrrrrra', 'rfind', b'a', None, 6)

    assert_fails(lambda: checkequal("fail?", b'hello', 'rfind'), "missing 1 required positional argument")

    # none tests
    b = b'hello'
    l = b'l'
    h = b'h'
    x = b'x'
    o = b'o'
    assert_eq(3, b.rfind(l, None))
    assert_eq(3, b.rfind(l, -2, None))
    assert_eq(2, b.rfind(l, None, -2))
    assert_eq(0, b.rfind(h, None, None))


def test_ljust():
    checkequal(b'abc       ', b'abc', 'ljust', 10)
    checkequal(b'abc   ', b'abc', 'ljust', 6)
    checkequal(b'abc', b'abc', 'ljust', 3)
    checkequal(b'abc', b'abc', 'ljust', 2)
    checkequal(b'abc*******', b'abc', 'ljust', 10, b'*')
    assert_fails(lambda: checkequal("fail?", b'abc', 'ljust'), "missing 1 required positional argument")
    assert_fails(lambda: checkequal("fail?", b'abc', 'ljust', 7, 32), "'fillbyte' got value of type 'int', want 'bytes'")
    b = b'abc'
    for fill_type in (bytes,):
        assert_eq(b.ljust(7, fill_type(b'-')), fill_type(b'abc----'))


def test_rjust():
    checkequal(b'       abc', b'abc', 'rjust', 10)
    checkequal(b'   abc', b'abc', 'rjust', 6)
    checkequal(b'abc', b'abc', 'rjust', 3)
    checkequal(b'abc', b'abc', 'rjust', 2)
    checkequal(b'*******abc', b'abc', 'rjust', 10, b'*')
    assert_fails(lambda: checkequal("fail?", b'abc', 'rjust'), "missing 1 required positional argument")
    assert_fails(lambda: checkequal("fail?", b'abc', 'rjust', 7, 32), "'fillbyte' got value of type 'int', want 'bytes'")
    b = b'abc'
    for fill_type in (bytes,):
        assert_eq(b.rjust(7, fill_type(b'-')), fill_type(b'----abc'))


def test_center():
    checkequal(b'   abc    ', b'abc', 'center', 10)
    checkequal(b' abc  ', b'abc', 'center', 6)
    checkequal(b'abc', b'abc', 'center', 3)
    checkequal(b'abc', b'abc', 'center', 2)
    checkequal(b'***abc****', b'abc', 'center', 10, b'*')
    assert_fails(lambda: checkequal("fail?", b'abc', 'center'), "missing 1 required positional argument")
    assert_fails(lambda: checkequal("fail?", b'abc', 'center', 7, 32), "'fillbyte' got value of type 'int', want 'bytes'")
    # Fill character can be either bytes or bytearray (issue 12380)
    b = b'abc'
    for fill_type in (bytes,):
        assert_eq(b.center(7, fill_type(b'-')), fill_type(b'--abc--'))


def test_swapcase():
    checkequal(b'hEllO CoMPuTErS', b'HeLLo cOmpUteRs', 'swapcase')
    assert_fails(lambda: checkequal("fail?", b'hello', 'swapcase', 42), "got unexpected positional argument")


def test_zfill():
    checkequal(b'123', b'123', 'zfill', 2)
    checkequal(b'123', b'123', 'zfill', 3)
    checkequal(b'0123', b'123', 'zfill', 4)
    checkequal(b'+123', b'+123', 'zfill', 3)
    checkequal(b'+123', b'+123', 'zfill', 4)
    checkequal(b'+0123', b'+123', 'zfill', 5)
    checkequal(b'-123', b'-123', 'zfill', 3)
    checkequal(b'-123', b'-123', 'zfill', 4)
    checkequal(b'-0123', b'-123', 'zfill', 5)
    checkequal(b'000', b'', 'zfill', 3)
    checkequal(b'34', b'34', 'zfill', 1)
    checkequal(b'0034', b'34', 'zfill', 4)

    assert_fails(lambda: checkequal("fail?", b'123', 'zfill'), "missing 1 required positional argument")


def test_islower():
    checkequal(False, b'', 'islower')
    checkequal(True, b'a', 'islower')
    checkequal(False, b'A', 'islower')
    checkequal(False, b'\n', 'islower')
    checkequal(True, b'abc', 'islower')
    checkequal(False, b'aBc', 'islower')
    checkequal(True, b'abc\n', 'islower')
    assert_fails(lambda: checkequal("fail?", b'abc', 'islower', 42), "got unexpected positional argument")


def test_isupper():
    checkequal(False, b'', 'isupper')
    checkequal(False, b'a', 'isupper')
    checkequal(True, b'A', 'isupper')
    checkequal(False, b'\n', 'isupper')
    checkequal(True, b'ABC', 'isupper')
    checkequal(False, b'AbC', 'isupper')
    checkequal(True, b'ABC\n', 'isupper')
    assert_fails(lambda: checkequal("fail?", b'abc', 'isupper', 42), "got unexpected positional argument")


def test_istitle():
    checkequal(False, b'', 'istitle')
    checkequal(False, b'a', 'istitle')
    checkequal(True, b'A', 'istitle')
    checkequal(False, b'\n', 'istitle')
    checkequal(True, b'A Titlecased Line', 'istitle')
    checkequal(True, b'A\nTitlecased Line', 'istitle')
    checkequal(True, b'A Titlecased, Line', 'istitle')
    checkequal(False, b'Not a capitalized String', 'istitle')
    checkequal(False, b'Not\ta Titlecase String', 'istitle')


def test_isspace():
    checkequal(False, b'a', 'isspace')
    checkequal(True, b' ', 'isspace')
    checkequal(True, '\t', 'isspace')
    checkequal(True, b'\r', 'isspace')
    checkequal(True, b'\n', 'isspace')
    checkequal(True, b' \t\r\n', 'isspace')
    checkequal(False, b' \t\r\na', 'isspace')
    assert_fails(lambda: checkequal("fail?", b'abc', 'isspace', 42), "got unexpected positional argument")


def test_isalpha():
    checkequal(False, b'', 'isalpha')
    checkequal(True, b'a', 'isalpha')
    checkequal(True, b'A', 'isalpha')
    checkequal(False, b'\n', 'isalpha')
    checkequal(True, b'abc', 'isalpha')
    checkequal(False, b'aBc123', 'isalpha')
    checkequal(False, b'abc\n', 'isalpha')
    assert_fails(lambda: checkequal("fail?", b'abc', 'isalpha', 42), "got unexpected positional argument")


def test_isalnum():
    checkequal(False, b'', 'isalnum')
    checkequal(True, b'a', 'isalnum')
    checkequal(True, b'A', 'isalnum')
    checkequal(False, b'\n', 'isalnum')
    checkequal(True, b'123abc456', 'isalnum')
    checkequal(True, b'a1b3c', 'isalnum')
    checkequal(False, b'aBc000 ', 'isalnum')
    checkequal(False, b'abc\n', 'isalnum')
    assert_fails(lambda: checkequal("fail?", b'abc', 'isalnum', 42), "got unexpected positional argument")


def test_isascii():
    checkequal(True, b'', 'isascii')
    checkequal(True, b'\x00', 'isascii')
    checkequal(True, b'\x7f', 'isascii')
    checkequal(True, b'\x00\x7f', 'isascii')
    checkequal(False, b'\x80', 'isascii')
    checkequal(False, b'\xe9', 'isascii')

    # bytes.isascii() and bytearray.isascii() has optimization which
    # check 4 or 8 bytes at once.  So check some alignments.
    for p in range(8):
      checkequal(True, b' '*p + b'\x7f', 'isascii')
      checkequal(False, b' '*p + b'\u0080', 'isascii')
      checkequal(True, b' '*p + b'\x7f' + b' '*8, 'isascii')
      checkequal(False, b' '*p + b'\u0080' + b' '*8, 'isascii')


def test_isdigit():
    checkequal(False, b'', 'isdigit')
    checkequal(False, b'a', 'isdigit')
    checkequal(True, b'0', 'isdigit')
    checkequal(True, b'0123456789', 'isdigit')
    checkequal(False, b'0123456789a', 'isdigit')

    assert_fails(lambda: checkequal("fail?", b'abc', 'isdigit', 42), "got unexpected positional argument")


def test_title():
    checkequal(b' Hello ', b' hello ', 'title')
    checkequal(b'Hello ', b'hello ', 'title')
    checkequal(b'Hello ', b'Hello ', 'title')
    checkequal(b'Format This As Title String', b"fOrMaT thIs aS titLe String", 'title')
    checkequal(b'Format,This-As*Title;String', b"fOrMaT,thIs-aS*titLe;String", 'title', )
    checkequal(b'Getint', b"getInt", 'title')
    assert_fails(lambda: checkequal("fail?", b'hello', 'title', 42), "got unexpected positional argument")


def test_splitlines():
    checkequal([b'abc', b'def', b'', b'ghi'], b"abc\ndef\n\rghi", 'splitlines')
    checkequal([b'abc', b'def', b'', b'ghi'], b"abc\ndef\n\r\nghi", 'splitlines')
    checkequal([b'abc', b'def', b'ghi'], b"abc\ndef\r\nghi", 'splitlines')
    checkequal([b'abc', b'def', b'ghi'], b"abc\ndef\r\nghi\n", 'splitlines')
    checkequal([b'abc', b'def', b'ghi', b''], b"abc\ndef\r\nghi\n\r", 'splitlines')
    checkequal([b'', b'abc', b'def', b'ghi', b''], b"\nabc\ndef\r\nghi\n\r", 'splitlines')
    checkequal([b'', b'abc', b'def', b'ghi', b''],
                  b"\nabc\ndef\r\nghi\n\r", 'splitlines', False)
    checkequal([b'\n', b'abc\n', b'def\r\n', b'ghi\n', b'\r'],
                  b"\nabc\ndef\r\nghi\n\r", 'splitlines', True)
    checkequal([b'', b'abc', b'def', b'ghi', b''], b"\nabc\ndef\r\nghi\n\r",
                  'splitlines', keepends=False)
    checkequal([b'\n', b'abc\n', b'def\r\n', b'ghi\n', b'\r'],
                  b"\nabc\ndef\r\nghi\n\r", 'splitlines', keepends=True)

    assert_fails(lambda: checkequal("fail?", b'abc', 'splitlines', 42, 42), "parameter 'keepends' got value of type 'int', want 'bool'")


def test_lower():
    checkequal(b'hello', b'HeLLo', 'lower')
    checkequal(b'hello', b'hello', 'lower')
    assert_fails(lambda: checkequal("fail?", b'hello', 'lower', 42), "got unexpected positional argument")


def test_upper():
    checkequal(b'HELLO', b'HeLLo', 'upper')
    checkequal(b'HELLO', b'HELLO', 'upper')
    assert_fails(lambda: checkequal("fail?", b'hello', 'upper', 42), "got unexpected positional argument")


def test_removeprefix():
    prefix= b'pip-'
    filename = b'pip-20.2.2-py2.py3-none-any.whl'
    # from: https://github.com/python/cpython/blob/521ba8892ef367c45bf1647b04a726d3f553637c/Lib/ensurepip/__init__.py#L54-L55
    # Extract '20.2.2' from 'pip-20.2.2-py2.py3-none-any.whl'
    version = filename.removeprefix(prefix).partition(b'-')[0]
    assert_eq(version, b"20.2.2")

    checkequal(b'am', b'spam', 'removeprefix', b'sp')
    checkequal(b'spamspam', b'spamspamspam', 'removeprefix', b'spam')
    checkequal(b'spam', b'spam', 'removeprefix', b'python')
    checkequal(b'spam', b'spam', 'removeprefix', b'spider')
    checkequal(b'spam', b'spam', 'removeprefix', b'spam and eggs')

    checkequal(b'', b'', 'removeprefix', b'')
    checkequal(b'', b'', 'removeprefix', b'abcde')
    checkequal(b'abcde', b'abcde', 'removeprefix', b'')
    checkequal(b'', b'abcde', 'removeprefix', b'abcde')

    assert_fails(lambda: checkequal("fail?", b'hello', 'removeprefix'), "missing 1 required positional argument: prefix")
    assert_fails(lambda: checkequal("fail?", b'hello', 'removeprefix', 42), "parameter 'prefix' got value of type 'int', want 'bytes'")
    assert_fails(lambda: checkequal("fail?", b'hello', 'removeprefix', 42, b'h'), "parameter 'prefix' got value of type 'int', want 'bytes'")
    assert_fails(lambda: checkequal("fail?", b'hello', 'removeprefix', b'h', 42), "accepts no more than 1 positional argument but got 2")
    assert_fails(lambda: checkequal("fail?", b'hello', 'removeprefix', (b"he", b"l")), "parameter 'prefix' got value of type 'tuple', want 'bytes'")


def test_removesuffix():
    checkequal(b'sp', b'spam', 'removesuffix', b'am')
    checkequal(b'spamspam', b'spamspamspam', 'removesuffix', b'spam')
    checkequal(b'spam', b'spam', 'removesuffix', b'python')
    checkequal(b'spam', b'spam', 'removesuffix', b'blam')
    checkequal(b'spam', b'spam', 'removesuffix', b'eggs and spam')

    checkequal(b'', b'', 'removesuffix', b'')
    checkequal(b'', b'', 'removesuffix', b'abcde')
    checkequal(b'abcde', b'abcde', 'removesuffix', b'')
    checkequal(b'', b'abcde', 'removesuffix', b'abcde')

    assert_fails(lambda: checkequal("fail?", b'hello', 'removesuffix'),  "missing 1 required positional argument: suffix")
    assert_fails(lambda: checkequal("fail?", b'hello', 'removesuffix', 42), "parameter 'suffix' got value of type 'int', want 'bytes'")
    assert_fails(lambda: checkequal("fail?", b'hello', 'removesuffix', 42, b'h'), "parameter 'suffix' got value of type 'int', want 'bytes'")
    assert_fails(lambda: checkequal("fail?", b'hello', 'removesuffix', b'h', 42), "accepts no more than 1 positional argument but got 2")
    assert_fails(lambda: checkequal("fail?", b'hello', 'removesuffix', (b"lo", b"l")),  "parameter 'suffix' got value of type 'tuple', want 'bytes'")


def test_capitalize():
    checkequal(b' hello ', b' hello ', 'capitalize')
    checkequal(b'Hello ', b'Hello ','capitalize')
    checkequal(b'Hello ', b'hello ', 'capitalize')
    checkequal(b'Aaaa', b'aaaa', 'capitalize')
    checkequal(b'Aaaa', b'AaAa', 'capitalize')

    assert_fails(lambda: checkequal("fail?", b'hello', 'capitalize', 42), " got unexpected positional argument")


def test_translate():
    b = b'hello'
    rosetta = list(range(256))
    rosetta[ord('o')] = ord('e')
    rosetta = bytes(rosetta)

    assert_fails(lambda: checkequal("fail?", b, 'translate'), "missing 1 required positional argument: table")
    assert_fails(lambda: checkequal("fail?", b, 'translate', None, None), "parameter 'delete' got value of type 'NoneType', want 'bytes'")
    assert_fails(lambda: checkequal("fail?", b, 'translate', bytes(range(255))), "translation table must be 256 characters long")

    c = b.translate(rosetta)
    d = b.translate(rosetta, b'')
    assert_eq(c, d)
    assert_eq(c, b'helle')

    c = b.translate(rosetta, b'hello')
    assert_eq(b, b'hello')  # does not mutate
    assert_eq(type(c), "bytes")
    assert_eq(c, b'')

    c = b.translate(rosetta, b'l')
    assert_eq(c, b'hee')
    c = b.translate(None, b'e')
    assert_eq(c, b'hllo')

    # test delete as a keyword argument
    c = b.translate(rosetta, delete=b'')
    assert_eq(c, b'helle')
    c = b.translate(rosetta, delete=b'l')
    assert_eq(c, b'hee')
    c = b.translate(None, delete=b'e')
    assert_eq(c, b'hllo')


def test_expandtabs():
    checkequal(b'abc\rab      def\ng       hi', b'abc\rab\tdef\ng\thi',
                    'expandtabs')
    checkequal(b'abc\rab      def\ng       hi', b'abc\rab\tdef\ng\thi',
                    'expandtabs', 8)
    checkequal(b'abc\rab  def\ng   hi', b'abc\rab\tdef\ng\thi',
                    'expandtabs', 4)
    checkequal(b'abc\r\nab      def\ng       hi', b'abc\r\nab\tdef\ng\thi',
                    'expandtabs')
    checkequal(b'abc\r\nab      def\ng       hi', b'abc\r\nab\tdef\ng\thi',
                    'expandtabs', 8)
    checkequal(b'abc\r\nab  def\ng   hi', b'abc\r\nab\tdef\ng\thi',
                    'expandtabs', 4)
    checkequal(b'abc\r\nab\r\ndef\ng\r\nhi', b'abc\r\nab\r\ndef\ng\r\nhi',
                    'expandtabs', 4)
    # check keyword args
    checkequal(b'abc\rab      def\ng       hi', b'abc\rab\tdef\ng\thi',
                    'expandtabs', tabsize=8)
    checkequal(b'abc\rab  def\ng   hi', b'abc\rab\tdef\ng\thi',
                    'expandtabs', tabsize=4)

    checkequal(b'  a\n b', b' \ta\n\tb', 'expandtabs', 1)

    assert_fails(
        lambda: checkequal("fail?", b'hello', 'expandtabs', 42, 42),
        "accepts no more than 1 positional argument but got 2")

# test_bytearray_append()
test_capitalize()
test_center()
# test_bytearray_clear()
# test_bytearray_copy()
test_count()
# unsupported: test_decode()
test_endswith()
test_expandtabs()
# test_bytearray_extend()
test_find()
# TODO: test_fromhex()
test_hex()
test_index()
# test_bytearray_insert()
test_isalnum()
test_isalpha()
test_isascii()
test_isdigit()
test_islower()
test_isspace()
test_istitle()
test_isupper()
test_join()
test_ljust()
test_lower()
test_lstrip()
# TODO: test_maketrans()
test_partition()
# test_bytearray_pop()
# test_bytearray_remove()
test_removeprefix()
test_removesuffix()
test_replace()
# test_bytearray_reverse()
test_rfind()
test_rindex()
test_rjust()
test_rpartition()
test_rsplit()
test_rstrip()
test_split()
test_splitlines()
test_startswith()
test_strip()
test_swapcase()
test_title()
test_translate()
test_upper()
test_zfill()