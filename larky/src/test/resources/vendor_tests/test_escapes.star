"""Unit tests for test_escapes.star"""
load("@stdlib//larky", "larky")
load("@stdlib//asserts", "asserts")
load("@stdlib//unittest", "unittest")
load("@stdlib//builtins", "builtins")
load("@stdlib//codecs", "codecs")
load("@vendor//escapes", "escapes")


# Tests of 'bytes' (immutable byte strings).
b = builtins.b


def _decode(s):
    # remember: decode bytes into string, encode string into bytes
    return codecs.decode(s, encoding='utf-8', errors='replace')


def debug(s):
    print(s, _decode(b(s)))
    return s


# my hack for string escapes
def string_literal_escape(sequence, escape_char):
    return r"\%s%s" % (escape_char, sequence)


x = larky.partial(string_literal_escape, escape_char="x")
u = larky.partial(string_literal_escape, escape_char="u")
U = larky.partial(string_literal_escape, escape_char="U")


escaped = escapes.CEscape().raw("\012").x("ff").u("0400").U("0001F63F")
#  NOTE: because there are no byte literals
#  (until Starlark merges byte-literals for Java)
#  So, the following assert statement:
#    asserts.assert_that(b("\012\xff\u0400\U0001F63F"))\
#    .is_equal_to(b(r"\n\xffЀ😿")) # see scanner tests for more
#  has been converted to:
asserts.assert_that(b(escaped)).is_equal_to(b(escapes.CEscape().esc("n").x("ff").raw("Ѐ😿")))


def _test_escape_literal_workaround():
    asserts.assert_that(
            b(escapes.CEscape()
              .raw("\012")
              .esc("ff", "x")
              .esc("0400", "u")
              .esc("0001F63F", "U"))
    ).is_equal_to(b(escaped))

    asserts.assert_that(
        b(escapes.CEscape()
          .raw("\012").esc("ff", "x").esc("0400", "u").esc("0001F63F", "U"))
    ).is_equal_to(
           # see scanner tests for more
           b("\n" + string_literal_escape("ff", "x") + "Ѐ😿"))

    asserts.assert_that(b("".join(
        ["\012", x("ff"), u("0400"), U("0001F63F")]))
    ).is_equal_to(b(r"\n\xffЀ😿"))  # see scanner tests for more

    asserts.assert_that(
        b(r"\r\n\t")
    ).is_equal_to(builtins.bytes("\\r\\n\\t"))  # raw


def _testsuite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_escape_literal_workaround))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_testsuite())
