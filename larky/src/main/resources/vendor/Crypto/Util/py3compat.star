# -*- coding: utf-8 -*-
#
#  Util/py3compat.py : Compatibility code for handling Py3k / Python 2.x
#
# Written in 2010 by Thorsten Behrens
#
# ===================================================================
# The contents of this file are dedicated to the public domain.  To
# the extent that dedication to the public domain is not available,
# everyone is granted a worldwide, perpetual, royalty-free,
# non-exclusive license to exercise all rights associated with the
# contents of this file for any purpose whatsoever.
# No rights are reserved.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
# BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
# ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
# ===================================================================

"""Compatibility code for handling string/bytes changes from Python 2.x to Py3k

In Python 2.x, strings (of type ''str'') contain binary data, including encoded
Unicode text (e.g. UTF-8).  The separate type ''unicode'' holds Unicode text.
Unicode literals are specified via the u'...' prefix.  Indexing or slicing
either type always produces a string of the same type as the original.
Data read from a file is always of '''str'' type.

In Python 3.x, strings (type ''str'') may only contain Unicode text. The u'...'
prefix and the ''unicode'' type are now redundant.  A new type (called
''bytes'') has to be used for binary data (including any particular
''encoding'' of a string).  The b'...' prefix allows one to specify a binary
literal.  Indexing or slicing a string produces another string.  Slicing a byte
string produces another byte string, but the indexing operation produces an
integer.  Data read from a file is of '''str'' type if the file was opened in
text mode, or of ''bytes'' type otherwise.

Since PyCrypto aims at supporting both Python 2.x and 3.x, the following helper
functions are used to keep the rest of the library as independent as possible
from the actual Python version.

In general, the code should always deal with binary strings, and use integers
instead of 1-byte character strings.

b(s)
    Take a text string literal (with no prefix or with u'...' prefix) and
    make a byte string.
bchr(c)
    Take an integer and make a 1-character byte string.
bord(c)
    Take the result of indexing on a byte string and make an integer.
tobytes(s)
    Take a text string, a byte string, or a sequence of character taken from
    a byte string and make a byte string.
"""

# load("@stdlib//io", BytesIO="BytesIO")
# load("@stdlib//io", StringIO="StringIO")
# load("@stdlib//sys", maxint="maxint")
# load("@stdlib//abc", ABC="ABC")

load("@stdlib//types", types="types")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//codecs", codecs="codecs")


def b(s):
    # latin-1 is ISO-8859-1
    # utf-8 would cause some side-effects we don't want
    return codecs.encode(s, "ISO-8859-1")


def bchr(s):
    return builtins.bytes([s])


def bstr(s):
    if types.is_instance(s,str):
        return builtins.bytes(s, "ISO-8859-1")
    else:
        return builtins.bytes(s)


def bord(s):
    if types.is_bytelike(s) or types.is_string(s):
        return ord(s)
    return s


def tobytes(s, encoding="ISO-8859-1"):
    if types.is_bytes(s):
        return s
    elif types.is_bytearray(s):
        return s
    elif types.is_string(s):
        return codecs.encode(s, encoding=encoding)
    else:
        return builtins.bytes([s])


def tostr(bs, encoding="ISO-8859-1"):
    if types.is_string(bs):
        return bs
    return codecs.decode(bs, encoding=encoding)


def byte_string(s):
    return types.is_bytes(s)


is_bytes = byte_string


iter_range = range


def is_native_int(x):
    return types.is_int(x)


def is_string(x):
    return types.is_string(x)


def FileNotFoundError(*args, **kwargs):
    fail("FileNotFoundError(%s, %s)" % (args, kwargs))


def copy_bytes(start, end, seq):
    """Return an immutable copy of a sequence (byte string, byte array, memoryview)
    in a certain interval [start:seq]"""

    if types.is_bytearray(seq):
        return builtins.bytearray(seq[start:end])
    else:
        return bytes(seq[start:end])
