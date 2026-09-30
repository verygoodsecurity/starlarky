"""Unit tests for parse.star"""
load("@stdlib//larky", larky="larky")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//unittest", "unittest")
load("@stdlib//urllib/parse", "parse")
load("@vendor//asserts", "asserts")

def b(s):
    return builtins.bytes(s, encoding="utf-8")

eq = asserts.eq

def _test_urlparse():
    res_parse = parse.urlparse(('http://netloc/path;parameters?query=argument#fragment'))
    eq(b('http'), b(res_parse.scheme))
    eq(b('netloc'), b(res_parse.netloc))
    eq(b('/path'), b(res_parse.path))
    eq(b('parameters'), b(res_parse.params))
    eq(b('query=argument'), b(res_parse.query))
    eq(b('fragment'), b(res_parse.fragment))

def _test_urlsplit():
    res_split = parse.urlsplit('http://www.cwi.nl:80/%7Eguido/Python.html')
    eq(b('http'), b(res_split.scheme))
    eq(b('www.cwi.nl:80'), b(res_split.netloc))
    eq(b('/%7Eguido/Python.html'), b(res_split.path))

def _test_urlunparse():
    tuple_unparse = ('http', 'netloc', '/path', 'parameters', 'query=argument', 'fragment')
    eq(b('http://netloc/path;parameters?query=argument#fragment'), b(parse.urlunparse(tuple_unparse)))

def _test_urlunsplit():
    tuple_unsplit = ('http', 'www.cwi.nl:80', '/%7Eguido/Python.html', '', '')
    eq(b('http://www.cwi.nl:80/%7Eguido/Python.html'), b(parse.urlunsplit(tuple_unsplit)))

def _test_parse_qsl():
    # CPython: parse_qsl('key=\\u0141%C3%A9', encoding='utf-8') == [('key', '\\u0141\xe9')]
    res_parse_qsl = parse.parse_qsl('key=\\u0141%C3%A9', encoding='utf-8')
    eq([('key', '\\u0141\u00e9')], res_parse_qsl)
    eq([('key', '\u0141\u00e9')], parse.parse_qsl('key=\u0141%C3%A9'))

def _test_parse_qs():
    # CPython: parse_qs("key=\\u0141%C3%A9", encoding="utf-8")['key'] == ['\\u0141\xe9']
    res_parse_qs = parse.parse_qs("key=\\u0141%C3%A9", encoding="utf-8")['key']
    eq(["\\u0141\u00e9"], res_parse_qs)

    URL='https://someurl.com/with/query_string?i=main&mode=front&sid=12ab&enc=+Hello'
    parsed_url = parse.urlparse(URL)
    eq({"i": ["main"], "mode": ["front"], "sid": ["12ab"], "enc": [" Hello"]}, parse.parse_qs(parsed_url.query))


def _test_urlencode_sequences():
    # Other tests incidentally urlencode things; test non-covered cases:
    # Sequence and object values.
    result = parse.urlencode({'a': [1, 2], 'b': (3, 4, 5)}, True)
    # we can rely on ordering here because Larky is deterministic.
    asserts.assert_that(
        result.split('&')
    ).is_equal_to(['a=1', 'a=2', 'b=3', 'b=4', 'b=5'])

    Trivial = larky.mutablestruct(
        __name__='Trivial',
        __str__ = lambda: 'trivial')

    result = parse.urlencode({'a': Trivial}, True)
    asserts.assert_that(result).is_equal_to('a=trivial')

def _test_urlencode_quote_via():
    result = parse.urlencode({'a': 'some value'})
    asserts.assert_that(result).is_equal_to("a=some+value")
    result = parse.urlencode({'a': 'some value/another'},
                                quote_via=parse.quote)
    asserts.assert_that(result).is_equal_to("a=some%20value%2Fanother")
    result = parse.urlencode({'a': 'some value/another'},
                                safe='/', quote_via=parse.quote)
    asserts.assert_that(result).is_equal_to("a=some%20value/another")


def _test_quote_from_bytes():
    asserts.assert_fails(lambda: parse.quote_from_bytes('foo'), ".*TypeError")
    result = parse.quote_from_bytes(b'archaeological arcana')
    asserts.assert_that(result).is_equal_to('archaeological%20arcana')
    result = parse.quote_from_bytes(b'')
    asserts.assert_that(result).is_equal_to('')


def _test_unquote_decodes_utf8():
    # expected values from CPython 3's urllib.parse
    asserts.assert_that(parse.unquote("caf%C3%A9")).is_equal_to("caf\u00e9")
    asserts.assert_that(parse.unquote(parse.quote("\u00e9"))).is_equal_to("\u00e9")
    asserts.assert_that(parse.unquote("%e2%82%ac%E2%82%AC")).is_equal_to("\u20ac\u20ac")
    asserts.assert_that(parse.unquote("\u00e9%C3%A9")).is_equal_to("\u00e9\u00e9")
    asserts.assert_that(parse.unquote("%41\u00e9 abc")).is_equal_to("A\u00e9 abc")
    asserts.assert_that(parse.unquote("a%2")).is_equal_to("a%2")
    asserts.assert_that(parse.unquote("%41%42")).is_equal_to("AB")
    # invalid UTF-8 is replaced (errors='replace'), one U+FFFD per CPython error
    asserts.assert_that(parse.unquote("%E2%82")).is_equal_to("\ufffd")
    asserts.assert_that(parse.unquote("%ff")).is_equal_to("\ufffd")
    asserts.assert_that(parse.unquote("%E2%82%ACx%ff%fe")).is_equal_to("\u20acx\ufffd\ufffd")
    asserts.assert_that(parse.unquote("%E9", "ascii", "ignore")).is_equal_to("")
    asserts.assert_that(parse.unquote("%E9", "ascii")).is_equal_to("\ufffd")
    asserts.assert_fails(lambda: parse.unquote("%E9", "utf-8", "strict"),
                         "^'utf-8' codec can't decode byte 0xe9 in position 0: unexpected end of data$")
    asserts.assert_that(parse.unquote(b"caf%C3%A9")).is_equal_to("caf\u00e9")


def _test_unquote_latin1():
    asserts.assert_that(parse.unquote("%E9", encoding="latin-1")).is_equal_to("\u00e9")
    asserts.assert_that(parse.quote("\u00e9", encoding="latin-1")).is_equal_to("%E9")
    asserts.assert_that(parse.quote_plus("\u00e9 x", encoding="latin-1")).is_equal_to("%E9+x")
    asserts.assert_that(parse.urlencode({"k": "\u00e9"}, encoding="latin-1")).is_equal_to("k=%E9")
    asserts.assert_fails(lambda: parse.quote("\u20ac", encoding="latin-1"),
                         "^'latin-1' codec can't encode character '\\\\u20ac' in position 0: ordinal not in range\\(256\\)$")


def _test_parse_qs_and_unquote_plus_decode_utf8():
    asserts.assert_that(parse.parse_qs("a=caf%C3%A9&b=%ff")).is_equal_to(
        {"a": ["caf\u00e9"], "b": ["\ufffd"]})
    asserts.assert_that(parse.parse_qsl("x=%E2%82+y")).is_equal_to([("x", "\ufffd y")])
    asserts.assert_that(parse.unquote_plus("caf%C3%A9+%E2%82")).is_equal_to("caf\u00e9 \ufffd")
    asserts.assert_that(parse.unquote_plus("%7e/abc+def")).is_equal_to("~/abc def")


def _test_quote_escapes_backslashes_and_control_bytes():
    # expected values from CPython 3's urllib.parse
    asserts.assert_that(parse.quote("\n\t\x01 ")).is_equal_to("%0A%09%01%20")
    asserts.assert_that(parse.quote("\\n")).is_equal_to("%5Cn")
    asserts.assert_that(parse.quote("a\\x41")).is_equal_to("a%5Cx41")
    asserts.assert_that(parse.quote("\\u0141")).is_equal_to("%5Cu0141")
    asserts.assert_that(parse.quote_plus("a\\nb c")).is_equal_to("a%5Cnb+c")
    asserts.assert_that(parse.urlencode({"k": "\\n\n"})).is_equal_to("k=%5Cn%0A")
    asserts.assert_that(parse.quote_from_bytes(b"\x00\x0f\x10")).is_equal_to("%00%0F%10")


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_urlparse))
    _suite.addTest(unittest.FunctionTestCase(_test_urlsplit))
    _suite.addTest(unittest.FunctionTestCase(_test_urlunparse))
    _suite.addTest(unittest.FunctionTestCase(_test_urlunsplit))
    _suite.addTest(unittest.FunctionTestCase(_test_parse_qsl))
    _suite.addTest(unittest.FunctionTestCase(_test_parse_qs))
    _suite.addTest(unittest.FunctionTestCase(_test_urlencode_sequences))
    _suite.addTest(unittest.FunctionTestCase(_test_urlencode_quote_via))
    _suite.addTest(unittest.FunctionTestCase(_test_quote_from_bytes))
    _suite.addTest(unittest.FunctionTestCase(_test_unquote_decodes_utf8))
    _suite.addTest(unittest.FunctionTestCase(_test_unquote_latin1))
    _suite.addTest(unittest.FunctionTestCase(_test_parse_qs_and_unquote_plus_decode_utf8))
    _suite.addTest(unittest.FunctionTestCase(_test_quote_escapes_backslashes_and_control_bytes))

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
