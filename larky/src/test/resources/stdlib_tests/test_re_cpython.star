"""Tests that stdlib/re.star behaves like CPython's re module.

Every expected value in the _test_cpython_* functions was computed by running
the same expression in CPython 3.12 (re.error messages as str(error)).
"""

load("@vendor//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@stdlib//re", "re")


def _test_cpython_template():
    asserts.assert_that(re.sub(r"(?P<x>a)", r"\g<x>\g<x>", "ab")).is_equal_to('aab')
    asserts.assert_that(re.sub(r"(a)", r"\g<1>\g<0>", "ab")).is_equal_to('aab')
    asserts.assert_that(re.sub(r"(a)(b)", r"\2\1", "ab")).is_equal_to('ba')
    asserts.assert_that(re.sub(r"a", r"\n", "a")).is_equal_to('\n')
    asserts.assert_that(re.sub(r"a", r"\a\b\f\n\r\t\v\\", "a")).is_equal_to('\x07\x08\x0c\n\r\t\x0b\\')
    asserts.assert_that(re.sub(r"a", r"\0\012\101\1010", "a")).is_equal_to('\x00\nAA0')
    asserts.assert_that(re.sub(r"a", r"\&\-", "a")).is_equal_to('\\&\\-')
    asserts.assert_that(re.sub(r"(a)|b", r"[\1]", "ab")).is_equal_to('[a][]')
    asserts.assert_that(re.sub(r"(a)|b", r"[\g<1>]", "ab")).is_equal_to('[a][]')
    asserts.assert_that(re.subn(r"(a)", r"<\1>", "aaa", 2)).is_equal_to(('<a><a>a', 2))
    asserts.assert_that(re.match(r"(?P<n>a)(b)", "ab").expand(r"\2-\g<n>-\1")).is_equal_to('b-a-a')
    asserts.assert_fails(lambda: re.sub("(a)", r"\10", "a"), '^re\\.error:\\ invalid\\ group\\ reference\\ 10\\ at\\ position\\ 1$')
    asserts.assert_fails(lambda: re.sub("(a)", r"\18", "a"), '^re\\.error:\\ invalid\\ group\\ reference\\ 18\\ at\\ position\\ 1$')
    asserts.assert_fails(lambda: re.sub("a", "\\", "a"), '^re\\.error:\\ bad\\ escape\\ \\(end\\ of\\ pattern\\)\\ at\\ position\\ 0$')
    asserts.assert_fails(lambda: re.sub(r"(a)", r"\2", "a"), '^re\\.error:\\ invalid\\ group\\ reference\\ 2\\ at\\ position\\ 1$')
    asserts.assert_fails(lambda: re.sub("a", r"\q", "a"), '^re\\.error:\\ bad\\ escape\\ \\\\q\\ at\\ position\\ 0$')
    asserts.assert_fails(lambda: re.sub("(a)", r"\g<y>", "a"), "^IndexError:\\ unknown\\ group\\ name\\ 'y'$")
    asserts.assert_fails(lambda: re.sub("(a)", r"\g<1", "a"), '^re\\.error:\\ missing\\ >,\\ unterminated\\ name\\ at\\ position\\ 3$')
    asserts.assert_fails(lambda: re.sub("(a)", r"\g", "a"), '^re\\.error:\\ missing\\ <\\ at\\ position\\ 2$')
    asserts.assert_fails(lambda: re.sub("(a)", r"\g<2>", "a"), '^re\\.error:\\ invalid\\ group\\ reference\\ 2\\ at\\ position\\ 3$')
    asserts.assert_fails(lambda: re.sub("(a)", r"\g<>", "a"), '^re\\.error:\\ missing\\ group\\ name\\ at\\ position\\ 3$')
    asserts.assert_fails(lambda: re.sub("(a)", r"\g<a b>", "a"), "^re\\.error:\\ bad\\ character\\ in\\ group\\ name\\ 'a\\ b'\\ at\\ position\\ 3$")
    asserts.assert_fails(lambda: re.sub("a", r"\400", "a"), '^re\\.error:\\ octal\\ escape\\ value\\ \\\\400\\ outside\\ of\\ range\\ 0\\-0o377\\ at\\ position\\ 0$')
    asserts.assert_fails(lambda: re.sub("x", r"\q", "a"), '^re\\.error:\\ bad\\ escape\\ \\\\q\\ at\\ position\\ 0$')
    asserts.assert_that(re.sub("x*", "-", "abxd")).is_equal_to('-a-b--d-')
    asserts.assert_that(re.sub("b*", "x", "xyz")).is_equal_to('xxxyxzx')
    asserts.assert_that(re.subn("", "-", "abc")).is_equal_to(('-a-b-c-', 4))
    asserts.assert_that(re.sub(r"(a)|b", lambda m: str(m.group(1)), "ab")).is_equal_to('aNone')
    asserts.assert_that(re.sub(r"\d+", lambda m: str(int(m.group()) * 2), "a1b22")).is_equal_to('a2b44')


def _test_cpython_findall():
    asserts.assert_that(re.findall(r"a|(b)", "ab")).is_equal_to(['', 'b'])
    asserts.assert_that(re.findall(r"(a)?b", "bab")).is_equal_to(['', 'a'])
    asserts.assert_that(re.findall(r"(a)(b)?", "a")).is_equal_to([('a', '')])
    asserts.assert_that(re.findall(r"(a)|(b)", "ab")).is_equal_to([('a', ''), ('', 'b')])
    asserts.assert_that(re.findall(r"\b", "a")).is_equal_to(['', ''])
    asserts.assert_that(re.findall(r"x*", "axb")).is_equal_to(['', 'x', '', ''])
    asserts.assert_that(re.findall(r"$", "ab\n")).is_equal_to(['', ''])
    asserts.assert_that(re.compile(r"\w+").findall("ab cd", 1, 4)).is_equal_to(['b', 'c'])
    asserts.assert_that([m.span() for m in re.finditer(r"x*", "axb")]).is_equal_to([(0, 0), (1, 2), (2, 2), (3, 3)])
    asserts.assert_that([m.groups() for m in re.finditer(r"(a)|(b)", "ab")]).is_equal_to([('a', None), (None, 'b')])


def _test_cpython_spans():
    asserts.assert_that(re.match(r"(a)|(b)", "b").span(1)).is_equal_to((-1, -1))
    asserts.assert_that(re.match(r"(a)|(b)", "b").start(1)).is_equal_to(-1)
    asserts.assert_that(re.match(r"(a)|(b)", "b").end(1)).is_equal_to(-1)
    asserts.assert_that(re.match(r"(a)|(b)", "b").span(2)).is_equal_to((0, 1))
    asserts.assert_that(re.match(r"(?P<x>a)|(?P<y>b)", "b").span("y")).is_equal_to((0, 1))
    asserts.assert_that(re.match(r"(?P<x>a)|(?P<y>b)", "b").span("x")).is_equal_to((-1, -1))
    asserts.assert_that(re.match(r"(a)|(b)", "b").groups()).is_equal_to((None, 'b'))
    asserts.assert_that(re.match(r"(a)|(b)", "b").groups("")).is_equal_to(('', 'b'))
    asserts.assert_that(re.match(r"(?P<x>a)|(?P<y>b)", "b").groupdict()).is_equal_to({'x': None, 'y': 'b'})
    asserts.assert_that(re.match(r"(?P<x>a)|(?P<y>b)", "b").groupdict("-")).is_equal_to({'x': '-', 'y': 'b'})
    asserts.assert_that(re.match(r"(a)(b)", "ab").group(2, 1, 0)).is_equal_to(('b', 'a', 'ab'))
    asserts.assert_that(re.match(r"(?P<x>a)", "a").group("x")).is_equal_to('a')
    asserts.assert_fails(lambda: re.match("(a)", "a").group(2), '^IndexError:\\ no\\ such\\ group$')
    asserts.assert_fails(lambda: re.match("(a)", "a").group("z"), '^IndexError:\\ no\\ such\\ group$')
    asserts.assert_fails(lambda: re.match("(a)", "a").span(3), '^IndexError:\\ no\\ such\\ group$')
    asserts.assert_that(re.search("b", "abc", 1).span()).is_equal_to((1, 2))
    asserts.assert_that(re.compile("b").search("abcb", 2).span()).is_equal_to((3, 4))
    asserts.assert_that(re.compile("b").search("abcb", 2, 3)).is_equal_to(None)
    asserts.assert_that(re.compile("^b").match("ab", 1)).is_equal_to(None)
    asserts.assert_that(re.compile(r"\bb").match("ab", 1)).is_equal_to(None)
    asserts.assert_that(re.compile(r"\bb").match("a b", 2).span()).is_equal_to((2, 3))
    asserts.assert_that(re.compile("(?m)^b").match("a\nb", 2).span()).is_equal_to((2, 3))
    asserts.assert_that(re.compile("a|ab").fullmatch("ab").group()).is_equal_to('ab')
    asserts.assert_that(re.compile("b").fullmatch("abc", 1, 2).span()).is_equal_to((1, 2))
    asserts.assert_that(re.fullmatch(r"a$", "a\n")).is_equal_to(None)
    asserts.assert_that(re.compile("o").match("dog", 1).span()).is_equal_to((1, 2))
    asserts.assert_that(re.compile(r"(ab)").match("abracadabra", 7, 10).span()).is_equal_to((7, 9))


def _test_cpython_dollar():
    asserts.assert_that(re.search(r"$", "ab\n").start()).is_equal_to(2)
    asserts.assert_that(re.search(r"$", "ab").start()).is_equal_to(2)
    asserts.assert_that(re.search(r"a$", "a\nb")).is_equal_to(None)
    asserts.assert_that(re.match(r"^\d+$", "123\n").group()).is_equal_to('123')
    asserts.assert_that(re.search(r"a$|a\n", "a\n").group()).is_equal_to('a')
    asserts.assert_that(re.search(r"\s*$", "ab \n").span()).is_equal_to((2, 4))
    asserts.assert_that(re.findall(r"\w$", "a\nb\nc\n")).is_equal_to(['c'])
    asserts.assert_that(re.findall(r"(?m)\w$", "a\nb\nc\n")).is_equal_to(['a', 'b', 'c'])
    asserts.assert_that(re.search(r"(?m)$", "a\nb\n").start()).is_equal_to(1)
    asserts.assert_that(re.search(r"\Z", "ab").start()).is_equal_to(2)
    asserts.assert_that(re.search(r"\Z", "ab\n").start()).is_equal_to(3)
    asserts.assert_that(re.search(r"b\Z", "ab\n")).is_equal_to(None)
    asserts.assert_that(re.sub(r"$", "!", "a\nb\n")).is_equal_to('a\nb!\n!')
    asserts.assert_that(re.split(r"$", "a\n")).is_equal_to(['a', '\n', ''])
    asserts.assert_that(re.search(r"x$", "x\ny\nx\n").span()).is_equal_to((4, 5))


def _test_cpython_unicode():
    asserts.assert_that(re.sub(r"\s", "", "a b c")).is_equal_to('abc')
    asserts.assert_that(re.sub(r"\s", "", "a b c\u000bd\x1ce")).is_equal_to('abcde')
    asserts.assert_that(re.findall(r"\w+", "café naïve")).is_equal_to(['café', 'naïve'])
    asserts.assert_that(re.findall(r"\d", "1٣x")).is_equal_to(['1', '٣'])
    asserts.assert_that(re.findall(r"\D", "1٣x")).is_equal_to(['x'])
    asserts.assert_that(re.findall(r"\S+", "a bé")).is_equal_to(['a', 'bé'])
    asserts.assert_that(re.findall(r"[\W]", "aé!")).is_equal_to(['!'])
    asserts.assert_that(re.findall(r"[^\s]", "a b")).is_equal_to(['a', 'b'])
    asserts.assert_that(re.sub(r"[\s,]", "", "a, b")).is_equal_to('ab')
    asserts.assert_that(re.sub(r"\s", "", "a b c\vd", flags=re.ASCII)).is_equal_to('a\u00a0bcd')
    asserts.assert_that(re.sub(r"(?a)\w", "", "aé")).is_equal_to('é')
    asserts.assert_that(re.findall(r"\w+", "café", re.A)).is_equal_to(['caf'])
    asserts.assert_that(re.search(r"é", "café").start()).is_equal_to(3)
    asserts.assert_that(re.search(r"\é", "café").start()).is_equal_to(3)
    asserts.assert_that(re.search(r"é", "café").span()).is_equal_to((3, 4))
    asserts.assert_that(re.search(r"\x41", "zA").span()).is_equal_to((1, 2))
    asserts.assert_that(re.search(r"\N{LATIN SMALL LETTER E WITH ACUTE}", "café").start()).is_equal_to(3)
    asserts.assert_that(re.search(r"[à-ÿ]+", "café").group()).is_equal_to('é')
    asserts.assert_that(re.search(r"\101", "A").group()).is_equal_to('A')
    asserts.assert_that(re.findall(r"(?i)É", "éÉ")).is_equal_to(['é', 'É'])
    asserts.assert_that(re.findall(r"(?i)[\sA]", "a A ")).is_equal_to(['a', ' ', 'A', '\u00a0'])
    asserts.assert_that(re.findall(r"(?i)[^\sA]", "a A b")).is_equal_to(['b'])
    asserts.assert_that(re.findall(r"(?i)[^\Wk]", "kKKb_")).is_equal_to(['b', '_'])
    asserts.assert_that(re.match(r"(?i)\w", "ͅ")).is_equal_to(None)
    asserts.assert_that(re.findall(r"(?i)\S+|[a-c]", "Ab C")).is_equal_to(['Ab', 'C'])
    asserts.assert_that(re.findall(r"\w+", "Ab c", re.I)).is_equal_to(['Ab', 'c'])
    asserts.assert_that(re.search(re.escape("é.b"), "xé.b").start()).is_equal_to(1)


def _test_cpython_syntax():
    asserts.assert_that(re.findall(r"a{,2}", "aaa")).is_equal_to(['aa', 'a', ''])
    asserts.assert_that(re.findall(r"a{2,}", "aaaaa")).is_equal_to(['aaaaa'])
    asserts.assert_that(re.findall(r"x{", "x{")).is_equal_to(['x{'])
    asserts.assert_that(re.findall(r"x{a}", "x{a}")).is_equal_to(['x{a}'])
    asserts.assert_that(re.findall(r"[]a]", "]a")).is_equal_to([']', 'a'])
    asserts.assert_that(re.findall(r"[^]a]", "]ab")).is_equal_to(['b'])
    asserts.assert_that(re.findall(r"[a-]", "a-b")).is_equal_to(['a', '-'])
    asserts.assert_that(re.findall(r"[[:alpha:]]", "a[:")).is_equal_to([])
    asserts.assert_that(re.findall(r"a b # c", "ab", re.X)).is_equal_to(['ab'])
    asserts.assert_that(re.findall(r"(?x) a [ ] b", "a b")).is_equal_to(['a b'])
    asserts.assert_that(re.findall(r"a(?#comment)b", "ab")).is_equal_to(['ab'])
    asserts.assert_that(re.findall(r"(?i:a)b", "AbAB")).is_equal_to(['Ab'])
    asserts.assert_that(re.findall(r"(?s:.)", "\n")).is_equal_to(['\n'])
    asserts.assert_that(re.findall(r".", "\n")).is_equal_to([])
    asserts.assert_that(re.match(r"(?P<é>a)", "a").group("é")).is_equal_to('a')
    asserts.assert_that(re.compile(r"(?P<a>x)(?P<b>y)").groupindex).is_equal_to({'a': 1, 'b': 2})
    asserts.assert_that(re.compile(r"(a)(?:b)(c)").groups).is_equal_to(2)
    asserts.assert_that(re.compile(r"(?P<a>x)").pattern).is_equal_to('(?P<a>x)')
    asserts.assert_that(re.split(r"(a)|b", "xaybz")).is_equal_to(['x', 'a', 'y', None, 'z'])
    asserts.assert_that(re.split(r"x*", "foo")).is_equal_to(['', 'f', 'o', 'o', ''])
    asserts.assert_that(re.split(r"\W+", "Words, words, words.", 1)).is_equal_to(['Words', 'words, words.'])
    asserts.assert_that(re.search(r"[\d-]+", "a1-2").group()).is_equal_to('1-2')
    asserts.assert_that(re.search(r"[\b]", "a\bb").start()).is_equal_to(1)


def _test_cpython_errors():
    asserts.assert_fails(lambda: re.match(r"(", "a"), '^re\\.error:\\ missing\\ \\),\\ unterminated\\ subpattern\\ at\\ position\\ 0$')
    asserts.assert_fails(lambda: re.compile(r"\q"), '^re\\.error:\\ bad\\ escape\\ \\\\q\\ at\\ position\\ 0$')
    asserts.assert_fails(lambda: re.compile(r"*a"), '^re\\.error:\\ nothing\\ to\\ repeat\\ at\\ position\\ 0$')
    asserts.assert_fails(lambda: re.compile(r"a**"), '^re\\.error:\\ multiple\\ repeat\\ at\\ position\\ 2$')
    asserts.assert_fails(lambda: re.compile(r"a)"), '^re\\.error:\\ unbalanced\\ parenthesis\\ at\\ position\\ 1$')
    asserts.assert_fails(lambda: re.compile(r"[a"), '^re\\.error:\\ unterminated\\ character\\ set\\ at\\ position\\ 0$')
    asserts.assert_fails(lambda: re.compile(r"[z-a]"), '^re\\.error:\\ bad\\ character\\ range\\ z\\-a\\ at\\ position\\ 1$')
    asserts.assert_fails(lambda: re.compile(r"(?P<a>x)(?P<a>y)"), "^re\\.error:\\ redefinition\\ of\\ group\\ name\\ 'a'\\ as\\ group\\ 2;\\ was\\ group\\ 1\\ at\\ position\\ 12$")
    asserts.assert_fails(lambda: re.compile(r"\1"), '^re\\.error:\\ invalid\\ group\\ reference\\ 1\\ at\\ position\\ 1$')
    asserts.assert_fails(lambda: re.compile(r"(a)\2"), '^re\\.error:\\ invalid\\ group\\ reference\\ 2\\ at\\ position\\ 4$')
    asserts.assert_fails(lambda: re.compile(r"a{2,1}"), '^re\\.error:\\ min\\ repeat\\ greater\\ than\\ max\\ repeat\\ at\\ position\\ 2$')
    asserts.assert_fails(lambda: re.compile(r"\x4"), '^re\\.error:\\ incomplete\\ escape\\ \\\\x4\\ at\\ position\\ 0$')
    asserts.assert_fails(lambda: re.compile(r"(?P<1>a)"), "^re\\.error:\\ bad\\ character\\ in\\ group\\ name\\ '1'\\ at\\ position\\ 4$")
    asserts.assert_fails(lambda: re.compile(r"(?Q)"), '^re\\.error:\\ unknown\\ extension\\ \\?Q\\ at\\ position\\ 1$')
    asserts.assert_fails(lambda: re.compile(r"(?P=a)"), "^re\\.error:\\ unknown\\ group\\ name\\ 'a'\\ at\\ position\\ 4$")
    asserts.assert_fails(lambda: re.compile("\\"), '^re\\.error:\\ bad\\ escape\\ \\(end\\ of\\ pattern\\)\\ at\\ position\\ 0$')
    asserts.assert_fails(lambda: re.compile(r"\N{NO SUCH NAME}"), "^re\\.error:\\ undefined\\ character\\ name\\ 'NO\\ SUCH\\ NAME'\\ at\\ position\\ 0$")
    asserts.assert_fails(lambda: re.compile(r"[\d-z]"), '^re\\.error:\\ bad\\ character\\ range\\ \\\\d\\-z\\ at\\ position\\ 1$')


def _test_unsupported_constructs_fail_cleanly():
    # RE2 runs in linear time and cannot express these; CPython supports them.
    # They must fail with a re.error, not a Java exception.
    asserts.assert_fails(
        lambda: re.search(r"(?<=a)b", "ab"),
        r"^re\.error: look-behind assertions are not supported .* at position 0$")
    asserts.assert_fails(
        lambda: re.search(r"(?<!a)b", "ab"),
        r"^re\.error: look-behind assertions are not supported .* at position 0$")
    asserts.assert_fails(
        lambda: re.search(r"a(?=b)", "ab"),
        r"^re\.error: look-ahead assertions are not supported .* at position 1$")
    asserts.assert_fails(
        lambda: re.search(r"a(?!b)", "ab"),
        r"^re\.error: look-ahead assertions are not supported .* at position 1$")
    asserts.assert_fails(
        lambda: re.match(r"(?P<a>x)(?P=a)", "xx"),
        r"^re\.error: backreferences are not supported .* at position 8$")
    asserts.assert_fails(
        lambda: re.match(r"(x)\1", "xx"),
        r"^re\.error: backreferences are not supported .* at position 3$")
    asserts.assert_fails(
        lambda: re.match(r"(x)(?(1)a|b)", "xa"),
        r"^re\.error: conditional groups are not supported")
    asserts.assert_fails(
        lambda: re.match(r"a{1001}", "a"),
        r"^re\.error: invalid repeat count")


def _test_bytes_input():
    # Unlike CPython (which needs a bytes pattern and returns bytes), Larky
    # matches str patterns against bytes read as Latin-1 and returns str, as
    # earlier versions did; vendored code depends on it. Classes are ASCII.
    asserts.assert_that(re.search(r"b+", b"abbc").group()).is_equal_to("bb")
    asserts.assert_that(re.findall(r"\w", b"a\xe9b")).is_equal_to(["a", "b"])
    asserts.assert_that(re.sub(r"b", b"\\n", b"abc")).is_equal_to("a\nc")
    asserts.assert_that(re.split(r",", b"a,b")).is_equal_to(["a", "b"])


def _test_match_object():
    m = re.compile(r"(?P<k>\w+)=(?P<v>\w*)").search("  key=val ", 1)
    asserts.assert_that(m.pos).is_equal_to(1)
    asserts.assert_that(m.endpos).is_equal_to(10)
    asserts.assert_that(m.string).is_equal_to("  key=val ")
    asserts.assert_that(m.regs).is_equal_to(((2, 9), (2, 5), (6, 9)))
    asserts.assert_that(m.re.pattern).is_equal_to(r"(?P<k>\w+)=(?P<v>\w*)")
    asserts.assert_that(repr(m)).is_equal_to("<re.Match object; span=(2, 9), match=\"key=val\">")
    asserts.assert_that(re.search(re.compile("b"), "abc").span()).is_equal_to((1, 2))


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_cpython_template))
    _suite.addTest(unittest.FunctionTestCase(_test_cpython_findall))
    _suite.addTest(unittest.FunctionTestCase(_test_cpython_spans))
    _suite.addTest(unittest.FunctionTestCase(_test_cpython_dollar))
    _suite.addTest(unittest.FunctionTestCase(_test_cpython_unicode))
    _suite.addTest(unittest.FunctionTestCase(_test_cpython_syntax))
    _suite.addTest(unittest.FunctionTestCase(_test_cpython_errors))
    _suite.addTest(unittest.FunctionTestCase(_test_unsupported_constructs_fail_cleanly))
    _suite.addTest(unittest.FunctionTestCase(_test_bytes_input))
    _suite.addTest(unittest.FunctionTestCase(_test_match_object))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
