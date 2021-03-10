load("@stdlib/larky", "larky")
load("@stdlib/codecs", "codecs")
load("@stdlib/builtins", "builtins")


def _escaper__init__():
    """Escape a bytes string for use in an text protocol buffer.

      Args:
        text: A byte string to be escaped.
        as_utf8: Specifies if result may contain non-ASCII characters.
            In Python 3 this allows unescaped non-ASCII Unicode characters.
            In Python 2 the return value will be valid UTF-8 rather than only
            ASCII.

      Returns:

        Escaped string (str).
      """

    # my hack for string escapes
    def _sequence_escaper(sequence, escape_char):
        return r"\%s%s" % (escape_char, sequence)

    def esc(s, quoter=""):
        self.literal.append(_sequence_escaper(s, escape_char=quoter))
        return self

    def x(s):
        self.literal.append(_sequence_escaper(s, escape_char="x"))
        return self

    def u(s):
        self.literal.append(_sequence_escaper(s, escape_char="u"))
        return self

    def U(s):
        self.literal.append(_sequence_escaper(s, escape_char="U"))
        return self

    def o(s):
        self.literal.append(_sequence_escaper(s, escape_char="0"))
        return self

    def raw(s):
        self.literal.append(s)
        return self

    def __str__():
        joined = ''.join(self.literal)
        return joined

    def __bytes__():
        # decode bytes into string, encode string into bytes
        return larky.bytearray(__str__(), 'utf-8')

    self = larky.struct(
        literal=[],
        x=x,
        u=u,
        U=U,
        raw=raw,
        o=o,
        esc=esc,
        __class__='escaper',
        __str__=__str__,
        __bytes__=__bytes__,
    )
    return self


escapes = larky.struct(
    CEscape=_escaper__init__,
)
# https://github.com/tortoise/tortoise-orm/blob/ae5f0b113a6bf778e8975a2d32398e6fd55bb08a/tortoise/converters.py
# language:python str escape x u
# def escape_bytes(value, encoding='utf-8'):
#     return "x'%s'" % binascii.hexlify(value).decode(encoding)