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
#
# # sub
#
# assert re.sub("a", "z", "caaab") == "czzzb"
# assert re.sub("a+", "z", "caaab") == "czb"
# assert re.sub("a", "z", "caaab", 1) == "czaab"
# assert re.sub("a", "z", "caaab", 2) == "czzab"
# assert re.sub("a", "z", "caaab", 10) == "czzzb"
# assert re.sub(r"[ :/?&]", "_", "http://foo.ua/bar/?a=1&b=baz/") == "http___foo.ua_bar__a=1_b=baz_"
# assert re.sub("a", lambda m: m.group(0) * 2, "caaab") == "caaaaaab"
#
# # subn
#
# assert re.subn("b*", "x", "xyz") == ('xxxyxzx', 4)
#
# # zero-length matches
# assert re.sub('(?m)^(?!$)', '--', 'foo') == '--foo'
# assert re.sub('(?m)^(?!$)', '--', 'foo\n') == '--foo\n'
# assert re.sub('(?m)^(?!$)', '--', 'foo\na') == '--foo\n--a'
# assert re.sub('(?m)^(?!$)', '--', 'foo\n\na') == '--foo\n\n--a'
# assert re.sub('(?m)^(?!$)', '--', 'foo\n\na', 1) == '--foo\n\na'
# assert re.sub('(?m)^(?!$)', '--', 'foo\n  \na', 2) == '--foo\n--  \na'
#
# # split
#
# assert re.split('x*', 'foo') == ['foo']
# assert re.split("(?m)^$", "foo\n\nbar\n") == ["foo\n\nbar\n"]
# assert re.split('\W+', 'Words, words, words.') == ['Words', 'words', 'words', '']
# assert re.split('(\W+)', 'Words, words, words.') == ['Words', ', ', 'words', ', ', 'words', '.', '']
# assert re.split('\W+', 'Words, words, words.', 1) == ['Words', 'words, words.']
# assert re.split('[a-f]+', '0a3B9', flags=re.IGNORECASE) == ['0', '3', '9']
# assert re.split('(\W+)', '...words, words...') == ['', '...', 'words', ', ', 'words', '...', '']
# assert re.split("(b)|(:+)", ":abc") == ['', None, ':', 'a', 'b', None, 'c']
#
# # findall
#
# text = "He was carefully disguised but captured quickly by police."
# assert re.findall(r"\w+ly", text) == ['carefully', 'quickly']
#
# text = "He was carefully disguised but captured quickly by police."
# assert re.findall(r"(\w+)(ly)", text) == [('careful', 'ly'), ('quick', 'ly')]
#
# text = "He was carefully disguised but captured quickly by police."
# assert re.findall(r"(\w+)ly", text) == ['careful', 'quick']
#
# r = re.compile(r"\w+ly")
# text = "carefully disguised but captured quickly by police."
# assert r.findall(text, 1) == ['arefully', 'quickly']
#
# _leading_whitespace_re = re.compile('(^[ \t]*)(?:[^ \t\n])', re.MULTILINE)
# text = "\tfoo\n\tbar"
# indents = _leading_whitespace_re.findall(text)
# assert indents == ['\t', '\t']
#
# text = "  \thello there\n  \t  how are you?"
# indents = _leading_whitespace_re.findall(text)
# assert indents == ['  \t', '  \t  ']
#
# assert re.findall(r"\b", "a") == ['', '']
#
# # handling of empty matches
# indent_re = re.compile('^([ ]*)(?=\S)', re.MULTILINE)
# s = "line number one\nline number two"
# assert indent_re.findall(s) == ['', '']
#
# # finditer
# # based on CPython's test_re.py
# iter = re.finditer(r":+", "a:b::c:::d")
# assert [item.group(0) for item in iter] == [":", "::", ":::"]
#
# pat = re.compile(r":+")
# iter = pat.finditer("a:b::c:::d", 3, 8)
# assert [item.group(0) for item in iter] == ["::", "::"]
#
# s = "line one\nline two\n   3"
# iter = re.finditer(r"^ *", s, re.MULTILINE)
# assert [m.group() for m in iter] == ["", "", "   "]
#
# assert [m.group() for m in re.finditer(r".*", "asdf")] == ["asdf", ""]

def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_escape))
    _suite.addTest(unittest.FunctionTestCase(_test_search))
    _suite.addTest(unittest.FunctionTestCase(_test_match))
    _suite.addTest(unittest.FunctionTestCase(_test_groups))
    return _suite

_runner = unittest.TextTestRunner()
_runner.run(_suite())