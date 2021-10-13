"""A collection of string operations (most are no longer used).

Warning: most of the code you see here isn't normally used nowadays.
Beginning with Python 1.6, many of these functions are implemented as
methods on the standard string object. They used to be implemented by
a built-in module called strop, but strop is now obsolete itself.

Public module variables:

whitespace -- a string containing all characters considered whitespace
lowercase -- a string containing all characters considered lowercase letters
uppercase -- a string containing all characters considered uppercase letters
letters -- a string containing all characters considered letters
digits -- a string containing all characters considered decimal digits
hexdigits -- a string containing all characters considered hexadecimal digits
octdigits -- a string containing all characters considered octal digits
punctuation -- a string containing all characters considered punctuation
printable -- a string containing all characters considered printable

"""
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//larky", larky="larky")
load("@stdlib//types", types="types")
load("@vendor//option/result", Error="Error")

# Some strings for ctype-style character classification
whitespace = ' \t\n\r\v\f'
lowercase = 'abcdefghijklmnopqrstuvwxyz'
uppercase = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
letters = lowercase + uppercase
ascii_lowercase = lowercase
ascii_uppercase = uppercase
ascii_letters = ascii_lowercase + ascii_uppercase
digits = '0123456789'
hexdigits = digits + 'abcdef' + 'ABCDEF'
octdigits = '01234567'
punctuation = r"""!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~"""
printable = digits + letters + punctuation + whitespace


# Case conversion helpers
# Use str to convert Unicode literal in case of -U
_idmap = str('').join(builtins.map(chr, range(256)))


# Construct a translation string
_idmapL = list(_idmap.elems())
# NOTE: this function has been removed from python since 3.2
# see: https://github.com/python/cpython/commit/5a6deb4caecca67a08ee0bf79a2df02f8c26b5f9
# TODO: it should probably be moved to a compatibility layer.
def maketrans(fromstr, tostr):
    """maketrans(frm, to) -> string

    Return a translation table (a string of 256 bytes long)
    suitable for use in string.translate.  The strings frm and to
    must be of the same length.

    """
    if len(fromstr) != len(tostr):
        return Error("maketrans arguments must have same length")
    L = list(_idmapL)
    fromstr = builtins.map(ord, fromstr.elems())
    for i in range(len(fromstr)):
        L[fromstr[i]] = tostr[i]
    return ''.join(L)



# for formatters, we can port over parmatter's parser + formatter
# (https://github.com/Ricyteach/parmatter/blob/master/src/parmatter/parmatter.py)

string = larky.struct(
    maketrans=maketrans,
    whitespace=whitespace,
    lowercase=lowercase,
    uppercase=uppercase,
    letters=letters,
    ascii_lowercase=ascii_lowercase,
    ascii_uppercase=ascii_uppercase,
    ascii_letters=ascii_letters,
    digits=digits,
    hexdigits=hexdigits,
    octdigits=octdigits,
    punctuation=punctuation,
    printable=printable,
)