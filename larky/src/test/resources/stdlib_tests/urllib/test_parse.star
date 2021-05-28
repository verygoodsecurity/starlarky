"""Unit tests for parse.star"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@stdlib//urllib/parse", "parse")
load("@stdlib//base64", "base64")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//codecs", "codecs")

def b(s):
    return builtins.bytes(s, encoding="utf-8")

eq = asserts.eq

def _add_test():
    """Unit tests for """

    # Test .
    res_parse = parse.urlparse('http://www.cwi.nl:80/%7Eguido/Python.html')
    eq(b('http'), b(res_parse.scheme))

    res_split = parse.urlsplit('http://www.cwi.nl:80/%7Eguido/Python.html')
    eq(b('www.cwi.nl:80'), b(res_split.netloc))

    tuple_unparse = ('http', 'www.cwi.nl:80', '/%7Eguido/Python.html', '', '', '')
    eq(b('http://www.cwi.nl:80/%7Eguido/Python.html'), b(parse.urlunparse(tuple_unparse)))

    tuple_unsplit = ('http', 'www.cwi.nl:80', '/%7Eguido/Python.html', '', '')
    eq(b('http://www.cwi.nl:80/%7Eguido/Python.html'), b(parse.urlunsplit(tuple_unsplit)))

    # python test:
    # >>> print(parse.parse_qs("key=\u0141%C3%A9", encoding="utf-8")['key'][0].encode('utf-8'))
    # b'\xc5\x81\xc3\xa9'
    res_parse_qs = parse.parse_qs("key=\\u0141%C3%A9", encoding="utf-8")['key']
    print("parse_qs result: ", res_parse_qs)
    eq(["\\xc5\\x81\\xc3\\xa9"], res_parse_qs)

    res_parse_qsl = parse.parse_qsl('key=\\u0141%C3%A9', encoding='utf-8')
    print("parse_qsl result: ", res_parse_qsl)
    eq([('key','\\xc5\\x81\\xc3\\xa9')], res_parse_qsl)

    URL='https://someurl.com/with/query_string?i=main&mode=front&sid=12ab&enc=+Hello'
    parsed_url = parse.urlparse(URL)
    eq({"i": ["main"], "mode": ["front"], "sid": ["12ab"], "enc": [" Hello"]}, parse.parse_qs(parsed_url.query))

def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_add_test))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
