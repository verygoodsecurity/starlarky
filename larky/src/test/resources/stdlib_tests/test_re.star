"""Unit tests for re.star"""

load("@stdlib/asserts", "asserts")
load("@stdlib/unittest", "unittest")
load("@stdlib/re", "re")


def _test_escape():
    asserts.assert_that(re.escape(r"1243*&[]_dsfAd")).is_equal_to(r"1243\*\&\[\]_dsfAd")


# search
def _test_search():
    m = re.search(r"a+", "caaab")
    asserts.assert_that(m.group(0)).is_equal_to("aaa")
    asserts.assert_that(m.group()).is_equal_to("aaa")


# match
def _test_match():
    m = re.match(r"(?ms)foo.*", "foo\nbar")
    asserts.assert_that(m.group(0)).is_equal_to("foo\nbar")

    asserts.assert_that(re.match(r"a+", "caaab")).is_none()
    m = re.match(r"a+", "aaaab")
    asserts.assert_that(m.group(0)).is_equal_to("aaaa")


def _test_groups():
    m = re.match(r"(\d+)\.(\d+)", "24.1632")
    asserts.assert_that(m.groups()).is_equal_to(('24', '1632'))
    asserts.assert_that(m.group(2, 1)).is_equal_to(('1632', '24'))

    m = re.match("(b)|(:+)", ":a")
    asserts.assert_that(m.groups()).is_equal_to((None, ":"))


# sub
def _test_sub():
    asserts.assert_that(re.sub("a", "z", "caaab")).is_equal_to("czzzb")
    asserts.assert_that(re.sub("a+", "z", "caaab")).is_equal_to("czb")
    asserts.assert_that(re.sub("a", "z", "caaab", 1)).is_equal_to("czaab")
    asserts.assert_that(re.sub("a", "z", "caaab", 2)).is_equal_to("czzab")
    asserts.assert_that(re.sub("a", "z", "caaab", 10)).is_equal_to("czzzb")
    asserts.assert_that(re.sub(r"[ :/?&]", "_", "http://foo.ua/bar/?a=1&b=baz/")).is_equal_to("http___foo.ua_bar__a=1_b=baz_")
    asserts.assert_that(re.sub("a", lambda m: m.group(0) * 2, "caaab")).is_equal_to("caaaaaab")


# subn
def _test_subn():
    asserts.assert_that(re.subn("b*", "x", "xyz")).is_equal_to(('xxxyxzx', 4))


# zero-length matches
def _test_zero_length_matches():
    # currently not supported!
    # you could try (?:$|[^,]) as an alternative to (?!,).
    asserts.assert_that(re.sub('(?m)^(?:$|[^$])', '--', 'foo')).is_equal_to('--foo')
    asserts.assert_that(re.sub('(?m)^(?:$|[^$])', '--', 'foo\n')).is_equal_to('--foo\n')
    asserts.assert_that(re.sub('(?m)^(?:$|[^$])', '--', 'foo\na')).is_equal_to('--foo\n--a')
    asserts.assert_that(re.sub('(?m)^(?:$|[^$])', '--', 'foo\n\na')).is_equal_to('--foo\n\n--a')
    asserts.assert_that(re.sub('(?m)^(?:$|[^$])', '--', 'foo\n\na', 1)).is_equal_to('--foo\n\na')
    asserts.assert_that(re.sub('(?m)^(?:$|[^$])', '--', 'foo\n  \na', 2)).is_equal_to('--foo\n--  \na')


# split
def _test_split():
    asserts.assert_that(re.split('x*', 'foo')).is_equal_to(['', 'f', 'o', 'o', ''])
    asserts.assert_that(re.split("(?m)^$", "foo\n\nbar\n")).is_equal_to(['foo\n', '\nbar\n', ''])
    asserts.assert_that(re.split(r'\W+', 'Words, words, words.')).is_equal_to(['Words', 'words', 'words', ''])
    asserts.assert_that(re.split(r'(\W+)', 'Words, words, words.')).is_equal_to(['Words', ', ', 'words', ', ', 'words', '.', ''])
    asserts.assert_that(re.split(r'\W+', 'Words, words, words.', 1)).is_equal_to(['Words', 'words, words.'])
    asserts.assert_that(re.split('[a-f]+', '0a3B9', flags=re.IGNORECASE)).is_equal_to(['0', '3', '9'])
    asserts.assert_that(re.split(r'(\W+)', '...words, words...')).is_equal_to(['', '...', 'words', ', ', 'words', '...', ''])
    asserts.assert_that(re.split("(b)|(:+)", ":abc")).is_equal_to(['', None, ':', 'a', 'b', None, 'c'])
    # for string in ":a:b::c", S(":a:b::c"):
    #             self.assertTypedEqual(re.split(":", string),
    #                                   ['', 'a', 'b', '', 'c'])
    #             self.assertTypedEqual(re.split(":+", string),
    #                                   ['', 'a', 'b', 'c'])
    #             self.assertTypedEqual(re.split("(:+)", string),
    #                                   ['', ':', 'a', ':', 'b', '::', 'c'])
    #         for string in (b":a:b::c", B(b":a:b::c"), bytearray(b":a:b::c"),
    #                        memoryview(b":a:b::c")):
    #             self.assertTypedEqual(re.split(b":", string),
    #                                   [b'', b'a', b'b', b'', b'c'])
    #             self.assertTypedEqual(re.split(b":+", string),
    #                                   [b'', b'a', b'b', b'c'])
    #             self.assertTypedEqual(re.split(b"(:+)", string),
    #                                   [b'', b':', b'a', b':', b'b', b'::', b'c'])
    #         for a, b, c in ("\xe0\xdf\xe7", "\u0430\u0431\u0432",
    #                         "\U0001d49c\U0001d49e\U0001d4b5"):
    #             string = ":%s:%s::%s" % (a, b, c)
    #             self.assertEqual(re.split(":", string), ['', a, b, '', c])
    #             self.assertEqual(re.split(":+", string), ['', a, b, c])
    #             self.assertEqual(re.split("(:+)", string),
    #                              ['', ':', a, ':', b, '::', c])
    #
    #         self.assertEqual(re.split("(?::+)", ":a:b::c"), ['', 'a', 'b', 'c'])
    #         self.assertEqual(re.split("(:)+", ":a:b::c"),
    #                          ['', ':', 'a', ':', 'b', ':', 'c'])
    #         self.assertEqual(re.split("([b:]+)", ":a:b::c"),
    #                          ['', ':', 'a', ':b::', 'c'])
    #         self.assertEqual(re.split("(b)|(:+)", ":a:b::c"),
    #                          ['', None, ':', 'a', None, ':', '', 'b', None, '',
    #                           None, '::', 'c'])
    #         self.assertEqual(re.split("(?:b)|(?::+)", ":a:b::c"),
    #                          ['', 'a', '', '', 'c'])



# findall
def _test_findall():
    text = "He was carefully disguised but captured quickly by police."
    asserts.assert_that(re.findall(r"\w+ly", text)).is_equal_to(['carefully', 'quickly'])

    text = "He was carefully disguised but captured quickly by police."
    asserts.assert_that(re.findall(r"(\w+)(ly)", text)).is_equal_to([('careful', 'ly'), ('quick', 'ly')])

    text = "He was carefully disguised but captured quickly by police."
    asserts.assert_that(re.findall(r"(\w+)ly", text)).is_equal_to(['careful', 'quick'])

    r = re.compile(r"\w+ly")
    text = "carefully disguised but captured quickly by police."
    asserts.assert_that(r.findall(text, 1)).is_equal_to(['arefully', 'quickly'])

    _leading_whitespace_re = re.compile('(^[ \t]*)(?:[^ \t\n])', re.MULTILINE)
    text = "\tfoo\n\tbar"
    indents = _leading_whitespace_re.findall(text)
    asserts.assert_that(indents).is_equal_to(['\t', '\t'])

    text = "  \thello there\n  \t  how are you?"
    indents = _leading_whitespace_re.findall(text)
    asserts.assert_that(indents).is_equal_to(['  \t', '  \t  '])

    asserts.assert_that(re.findall(r"\b", "a")).is_equal_to(['', ''])

    # handling of empty matches
    indent_re = re.compile(r'^([ ]*)(?=\S)', re.MULTILINE)
    s = "line number one\nline number two"
    asserts.assert_that(indent_re.findall(s)).is_equal_to(['', ''])


# finditer
def _test_finditer():
    # based on CPython's test_re.py
    iter = re.finditer(r":+", "a:b::c:::d")
    asserts.assert_that([item.group(0) for item in iter]).is_equal_to([":", "::", ":::"])

    pat = re.compile(r":+")
    iter = pat.finditer("a:b::c:::d", 3, 8)
    asserts.assert_that([item.group(0) for item in iter]).is_equal_to(["::", "::"])

    s = "line one\nline two\n   3"
    iter = re.finditer(r"^ *", s, re.MULTILINE)
    asserts.assert_that([m.group() for m in iter]).is_equal_to(["", "", "   "])

    asserts.assert_that([m.group() for m in re.finditer(r".*", "asdf")]).is_equal_to(["asdf", ""])


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_escape))
    _suite.addTest(unittest.FunctionTestCase(_test_search))
    _suite.addTest(unittest.FunctionTestCase(_test_match))
    _suite.addTest(unittest.FunctionTestCase(_test_groups))
    _suite.addTest(unittest.FunctionTestCase(_test_sub))
    _suite.addTest(unittest.FunctionTestCase(_test_subn))
    # currently not supported!
    #_suite.addTest(unittest.FunctionTestCase(_test_zero_length_matches))
    _suite.addTest(unittest.FunctionTestCase(_test_split))
    _suite.addTest(unittest.FunctionTestCase(_test_findall))
    _suite.addTest(unittest.FunctionTestCase(_test_finditer))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_suite())