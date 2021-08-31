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
    res_parse_qsl = parse.parse_qsl('key=\\u0141%C3%A9', encoding='utf-8')
    # print("parse_qsl result: ", res_parse_qsl)
    eq([('key','\\xc5\\x81\\xc3\\xa9')], res_parse_qsl)

def _test_parse_qs():
    # python test:
    # >>> print(parse.parse_qs("key=\u0141%C3%A9", encoding="utf-8")['key'][0].encode('utf-8'))
    # b'\xc5\x81\xc3\xa9'
    res_parse_qs = parse.parse_qs("key=\\u0141%C3%A9", encoding="utf-8")['key']
    # print("parse_qs result: ", res_parse_qs)
    eq(["\\xc5\\x81\\xc3\\xa9"], res_parse_qs)

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

    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
