"""Unit tests for parse.star"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@stdlib//urllib/parse", "parse")
load("@stdlib//builtins", builtins="builtins")

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


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_urlparse))
    _suite.addTest(unittest.FunctionTestCase(_test_urlsplit))
    _suite.addTest(unittest.FunctionTestCase(_test_urlunparse))
    _suite.addTest(unittest.FunctionTestCase(_test_urlunsplit))
    _suite.addTest(unittest.FunctionTestCase(_test_parse_qsl))
    _suite.addTest(unittest.FunctionTestCase(_test_parse_qs))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
