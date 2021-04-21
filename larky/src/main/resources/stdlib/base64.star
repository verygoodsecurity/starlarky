"""RFC 3548: Base16, Base32, Base64 Data Encodings"""

# Modified 04-Oct-1995 by Jack Jansen to use binascii module
# Modified 30-Dec-2003 by Barry Warsaw to add full RFC 3548 support
# Modified 22-May-2007 by Guido van Rossum to use bytes everywhere

load("@stdlib//binascii", binascii="binascii")
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//larky", larky="larky")
load("@stdlib//re", re="re")
load("@stdlib//struct", struct="struct")
load("@stdlib//types", types="types")

_WHILE_LOOP_EMULATION_ITERATION = larky.WHILE_LOOP_EMULATION_ITERATION


NOT_IMPLEMENTED = 'not implemented'


__all__ = [
    # Legacy interface exports traditional RFC 1521 Base64 encodings
    'encode', 'decode', 'encodebytes', 'decodebytes',
    # Generalized interface for other encodings
    'b64encode', 'b64decode', 'b32encode', 'b32decode',
    'b16encode', 'b16decode',
    # Standard Base64 encoding
    'standard_b64encode', 'standard_b64decode',
    # Some common Base64 alternatives.  As referenced by RFC 3458, see thread
    # starting at:
    #
    # http://zgp.org/pipermail/p2p-hackers/2001-September/000316.html
    'urlsafe_b64encode', 'urlsafe_b64decode',
    ]



def _bytes_from_decode_data(s):
    if types.is_bytelike(s):
        return s
    elif types.is_string(s):
        return codecs.encode(s, encoding='ascii')
    else:
        fail('TypeError: argument should be bytes or ASCII string, not %s'
             % type(s))


# Base64 encoding/decoding uses binascii

def b64encode(s, altchars=None):
    """Encode a byte string using Base64.

    s is the byte string to encode.  Optional altchars must be a byte
    string of length 2 which specifies an alternative alphabet for the
    '+' and '/' characters.  This allows an application to
    e.g. generate url or filesystem safe Base64 strings.

    The encoded byte string is returned.
    """
    if not types.is_bytelike(s):
        fail('TypeError: expected bytes, not %s' % type(s))
    # Strip off the trailing newline
    encoded = binascii.b2a_base64(s)[:-1]
    if altchars != None:
        if not types.is_bytelike(altchars):
            fail('TypeError: expected bytes, not %s' % type(altchars))
        if len(altchars) != 2:
            fail("%s should have length 2" % repr(altchars))
        fail("larky does not support altcharts at the moment!")
        #return encoded.translate(bytes.maketrans(bytes([0x2b, 0x2f]), altchars))
    return encoded


def b64decode(s, altchars=None, validate=False):
    """Decode a Base64 encoded byte string.

    s is the byte string to decode.  Optional altchars must be a
    string of length 2 which specifies the alternative alphabet used
    instead of the '+' and '/' characters.

    The decoded string is returned.  A binascii.Error is raised if s is
    incorrectly padded.

    If validate is False (the default), non-base64-alphabet characters are
    discarded prior to the padding check.  If validate is True,
    non-base64-alphabet characters in the input result in a binascii.Error.
    """
    s = _bytes_from_decode_data(s)
    if altchars != None:
        if not types.is_bytelike(altchars):
            fail('TypeError: expected bytes, not %s' % type(altchars))
        if len(altchars) != 2:
            fail("%s should have length 2" % repr(altchars))
        fail("larky does not support altcharts at the moment!")
        # s = s.translate(bytes.maketrans(altchars, bytes([0x2b, 0x2f])))
    if validate and not re.match(r'^[A-Za-z0-9+/]*={0,2}$', s):
        fail(" binascii.Error('Non-base64 digit found')")
    return binascii.a2b_base64(s)


def standard_b64encode(s):
    """Encode a byte string using the standard Base64 alphabet.

    s is the byte string to encode.  The encoded byte string is returned.
    """
    return b64encode(s)


def standard_b64decode(s):
    """Decode a byte string encoded with the standard Base64 alphabet.

    s is the byte string to decode.  The decoded byte string is
    returned.  binascii.Error is raised if the input is incorrectly
    padded or if there are non-alphabet characters present in the
    input.
    """
    return b64decode(s)


#_urlsafe_encode_translation = bytes.maketrans(b'+/', b'-_')
#_urlsafe_decode_translation = bytes.maketrans(b'-_', b'+/')

def urlsafe_b64encode(s):
    """Encode a byte string using a url-safe Base64 alphabet.

    s is the byte string to encode.  The encoded byte string is
    returned.  The alphabet uses '-' instead of '+' and '_' instead of
    '/'.
    """
#    return b64encode(s).translate(_urlsafe_encode_translation)
    fail(" NotImplementedError()")

def urlsafe_b64decode(s):
    """Decode a byte string encoded with the standard Base64 alphabet.

    s is the byte string to decode.  The decoded byte string is
    returned.  binascii.Error is raised if the input is incorrectly
    padded or if there are non-alphabet characters present in the
    input.

    The alphabet uses '-' instead of '+' and '_' instead of '/'.
    """
#    s = _bytes_from_decode_data(s)
#    s = s.translate(_urlsafe_decode_translation)
#    return b64decode(s)
    fail(" NotImplementedError()")



# Base32 encoding/decoding must be done in Python
_b32alphabet = {
    0: bytes([0x41]),  9: bytes([0x4a]), 18: bytes([0x53]), 27: bytes([0x33]),
    1: bytes([0x42]), 10: bytes([0x4b]), 19: bytes([0x54]), 28: bytes([0x34]),
    2: bytes([0x43]), 11: bytes([0x4c]), 20: bytes([0x55]), 29: bytes([0x35]),
    3: bytes([0x44]), 12: bytes([0x4d]), 21: bytes([0x56]), 30: bytes([0x36]),
    4: bytes([0x45]), 13: bytes([0x4e]), 22: bytes([0x57]), 31: bytes([0x37]),
    5: bytes([0x46]), 14: bytes([0x4f]), 23: bytes([0x58]),
    6: bytes([0x47]), 15: bytes([0x50]), 24: bytes([0x59]),
    7: bytes([0x48]), 16: bytes([0x51]), 25: bytes([0x5a]),
    8: bytes([0x49]), 17: bytes([0x52]), 26: bytes([0x32]),
    }

_b32tab = [v[0] for k, v in sorted(_b32alphabet.items())]
_b32rev = dict([(v[0], k) for k, v in _b32alphabet.items()])


def b32encode(s):
    """Encode a byte string using Base32.

    s is the byte string to encode.  The encoded byte string is returned.
    """
    if not types.is_bytelike(s):
        fail('TypeError: expected bytes, not %s' % type(s))
    quanta, leftover = divmod(len(s), 5)
    # Pad the last quantum with zero bits if necessary
    if leftover:
        s = s + bytearray([0x00] * (5 - leftover))  # Don't use += !
        quanta += 1
    encoded = bytearray()
    for i in range(quanta):
        # c1 and c2 are 16 bits wide, c3 is 8 bits wide.  The intent of this
        # code is to process the 40 bits in units of 5 bits.  So we take the 1
        # leftover bit of c1 and tack it onto c2.  Then we take the 2 leftover
        # bits of c2 and tack them onto c3.  The shifts and masks are intended
        # to give us values of exactly 5 bits in width.
        c1, c2, c3 = struct.unpack('!HHB', s[i*5:(i+1)*5])
        c2 += (c1 & 1) << 16 # 17 bits wide
        c3 += (c2 & 3) << 8  # 10 bits wide
        encoded += bytes(
             [_b32tab[c1 >> 11],         # bits 1 - 5
              _b32tab[(c1 >> 6) & 0x1f], # bits 6 - 10
              _b32tab[(c1 >> 1) & 0x1f], # bits 11 - 15
              _b32tab[c2 >> 12],         # bits 16 - 20 (1 - 5)
              _b32tab[(c2 >> 7) & 0x1f], # bits 21 - 25 (6 - 10)
              _b32tab[(c2 >> 2) & 0x1f], # bits 26 - 30 (11 - 15)
              _b32tab[c3 >> 5],          # bits 31 - 35 (1 - 5)
              _b32tab[c3 & 0x1f],        # bits 36 - 40 (1 - 5)
        ])
    # Adjust for any leftover partial quanta
    if leftover == 1:
        encoded = encoded[:-6] + bytes([0x3d, 0x3d, 0x3d, 0x3d, 0x3d, 0x3d])
    elif leftover == 2:
        encoded = encoded[:-4] + bytes([0x3d, 0x3d, 0x3d, 0x3d])
    elif leftover == 3:
        encoded = encoded[:-3] + bytes([0x3d, 0x3d, 0x3d])
    elif leftover == 4:
        encoded = encoded[:-1] + bytes([0x3d])
    return bytes(encoded)


def b32decode(s, casefold=False, map01=None):
    """Decode a Base32 encoded byte string.

    s is the byte string to decode.  Optional casefold is a flag
    specifying whether a lowercase alphabet is acceptable as input.
    For security purposes, the default is False.

    RFC 3548 allows for optional mapping of the digit 0 (zero) to the
    letter O (oh), and for optional mapping of the digit 1 (one) to
    either the letter I (eye) or letter L (el).  The optional argument
    map01 when not None, specifies which letter the digit 1 should be
    mapped to (when map01 is not None, the digit 0 is always mapped to
    the letter O).  For security purposes the default is None, so that
    0 and 1 are not allowed in the input.

    The decoded byte string is returned.  binascii.Error is raised if
    the input is incorrectly padded or if there are non-alphabet
    characters present in the input.
    """
    s = _bytes_from_decode_data(s)
    quanta, leftover = divmod(len(s), 8)
    if leftover:
        fail(" binascii.Error('Incorrect padding')")
    # Handle section 2.4 zero and one mapping.  The flag map01 will be either
    # False, or the character to map the digit 1 (one) to.  It should be
    # either L (el) or I (eye).
    if map01 != None:
        map01 = _bytes_from_decode_data(map01)
        if len(map01) != 1:
            fail("%s should have length 1" % repr(map01))
        fail("larky does not support map01")
        #s = s.translate(bytes.maketrans(bytes([0x30, 0x31]), bytes([0x4f]) + map01))
    if casefold:
        s = s.upper()
    # Strip off pad characters from the right.  We need to count the pad
    # characters because this will tell us how many null bytes to remove from
    # the end of the decoded string.
    padchars = s.find(bytes([0x3d]))
    if padchars > 0:
        padchars = len(s) - padchars
        s = s[:-padchars]
    else:
        padchars = 0

    # Now decode the full quanta
    parts = []
    acc = 0
    shift = 35
    for c in s:
        val = _b32rev.get(c)
        if val == None:
            fail(" binascii.Error('Non-base32 digit found')")
        acc += _b32rev[c] << shift
        shift -= 5
        if shift < 0:
            parts.append(binascii.unhexlify(bytes('%010x' % acc, "ascii")))
            acc = 0
            shift = 35
    # Process the last, partial quanta
    last = binascii.unhexlify(bytes('%010x' % acc, "ascii"))
    if padchars == 0:
        last = bytes(r'', encoding='utf-8')                      # No characters
    elif padchars == 1:
        last = last[:-1]
    elif padchars == 3:
        last = last[:-2]
    elif padchars == 4:
        last = last[:-3]
    elif padchars == 6:
        last = last[:-4]
    else:
        fail(" binascii.Error('Incorrect padding')")
    parts.append(last)
    return bytes(r'', encoding='utf-8').join(parts)



# RFC 3548, Base 16 Alphabet specifies uppercase, but hexlify() returns
# lowercase.  The RFC also recommends against accepting input case
# insensitively.
def b16encode(s):
    """Encode a byte string using Base16.

    s is the byte string to encode.  The encoded byte string is returned.
    """
    if not types.is_bytelike(s):
        fail('TypeError: expected bytes, not %s' % type(s))
    return binascii.hexlify(s).upper()


def b16decode(s, casefold=False):
    """Decode a Base16 encoded byte string.

    s is the byte string to decode.  Optional casefold is a flag
    specifying whether a lowercase alphabet is acceptable as input.
    For security purposes, the default is False.

    The decoded byte string is returned.  binascii.Error is raised if
    s were incorrectly padded or if there are non-alphabet characters
    present in the string.
    """
    s = _bytes_from_decode_data(s)
    if casefold:
        s = s.upper()
    if re.search(r'[^0-9A-F]', s):
        fail(" binascii.Error('Non-base16 digit found')")
    return binascii.unhexlify(s)



# Legacy interface.  This code could be cleaned up since I don't believe
# binascii has any line length limitations.  It just doesn't seem worth it
# though.  The files should be opened in binary mode.

MAXLINESIZE = 76 # Excluding the CRLF
MAXBINSIZE = (MAXLINESIZE//4)*3

def encode(input, output):
    fail(NOT_IMPLEMENTED)
    """Encode a file; input and output are binary files."""
    # for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
    #     if not True:
    #         break
    #     s = input.read(MAXBINSIZE)
    #     if not s:
    #         break
    #     for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
    #         if len(s) >= MAXBINSIZE:
    #             break
    #         ns = input.read(MAXBINSIZE-len(s))
    #         if not ns:
    #             break
    #         s += ns
    #     line = binascii.b2a_base64(s)
    #     output.write(line)


def decode(input, output):
    fail(NOT_IMPLEMENTED)
    """Decode a file; input and output are binary files."""
    # for _while_ in range(_WHILE_LOOP_EMULATION_ITERATION):
    #     if not True:
    #         break
    #     line = input.readline()
    #     if not line:
    #         break
    #     s = binascii.a2b_base64(line)
    #     output.write(s)


def encodebytes(s):
    """Encode a bytestring into a bytestring containing multiple lines
    of base-64 data."""
    if not types.is_bytelike(s):
        fail('TypeError: expected bytes, not %s' % type(s))
    pieces = []
    for i in range(0, len(s), MAXBINSIZE):
        chunk = s[i : i + MAXBINSIZE]
        pieces.append(binascii.b2a_base64(chunk))
    return bytes(r"", encoding='utf-8').join(pieces)


def encodestring(s):
    """Legacy alias of encodebytes()."""
    return encodebytes(s)


def decodebytes(s):
    """Decode a bytestring of base-64 data into a bytestring."""
    if not types.is_bytelike(s):
        fail('TypeError: expected bytes, not %s' % type(s))
    return binascii.a2b_base64(s)


def decodestring(s):
    """Legacy alias of decodebytes()."""
    return decodebytes(s)


base64 = larky.struct(
    b64encode = b64encode,
    b64decode = b64decode,
)