load("@stdlib/larky", "larky")
load("@stdlib/base64j", "base64j")

NOT_IMPLEMENTED = 'not implemented'


def _bytes_from_decode_data(s):
    """
    'ascii'
    """
    return NOT_IMPLEMENTED

def b64encode(s, altchars=None):
    """
    Encode the bytes-like object s using Base64 and return a bytes object.

        Optional altchars should be a byte string of length 2 which specifies an
        alternative alphabet for the '+' and '/' characters.  This allows an
        application to e.g. generate url or filesystem safe Base64 strings.

    """
    return base64j.b64encode(s, altchars)

def b64decode(s, altchars=None, validate=False):
    """
    Decode the Base64 encoded bytes-like object or ASCII string s.

        Optional altchars must be a bytes-like object or ASCII string of length 2
        which specifies the alternative alphabet used instead of the '+' and '/'
        characters.

        The result is returned as a bytes object.  A binascii.Error is raised if
        s is incorrectly padded.

        If validate is False (the default), characters that are neither in the
        normal base-64 alphabet nor the alternative alphabet are discarded prior
        to the padding check.  If validate is True, these non-alphabet characters
        in the input result in a binascii.Error.

    """
    return base64j.b64decode(s, altchars, validate)

def standard_b64encode(s):
    """
    Encode bytes-like object s using the standard Base64 alphabet.

        The result is returned as a bytes object.

    """
    return NOT_IMPLEMENTED

def standard_b64decode(s):
    """
    Decode bytes encoded with the standard Base64 alphabet.

        Argument s is a bytes-like object or ASCII string to decode.  The result
        is returned as a bytes object.  A binascii.Error is raised if the input
        is incorrectly padded.  Characters that are not in the standard alphabet
        are discarded prior to the padding check.

    """
    return NOT_IMPLEMENTED

def urlsafe_b64encode(s):
    """
    Encode bytes using the URL- and filesystem-safe Base64 alphabet.

        Argument s is a bytes-like object to encode.  The result is returned as a
        bytes object.  The alphabet uses '-' instead of '+' and '_' instead of
        '/'.

    """
    return NOT_IMPLEMENTED

def urlsafe_b64decode(s):
    """
    Decode bytes using the URL- and filesystem-safe Base64 alphabet.

        Argument s is a bytes-like object or ASCII string to decode.  The result
        is returned as a bytes object.  A binascii.Error is raised if the input
        is incorrectly padded.  Characters that are not in the URL-safe base-64
        alphabet, and are not a plus '+' or slash '/', are discarded prior to the
        padding check.

        The alphabet uses '-' instead of '+' and '_' instead of '/'.

    """
    return NOT_IMPLEMENTED

def b32encode(s):
    """
    Encode the bytes-like object s using Base32 and return a bytes object.

    """
    return NOT_IMPLEMENTED

def b32decode(s, casefold=False, map01=None):
    """
    Decode the Base32 encoded bytes-like object or ASCII string s.

        Optional casefold is a flag specifying whether a lowercase alphabet is
        acceptable as input.  For security purposes, the default is False.

        RFC 3548 allows for optional mapping of the digit 0 (zero) to the
        letter O (oh), and for optional mapping of the digit 1 (one) to
        either the letter I (eye) or letter L (el).  The optional argument
        map01 when not None, specifies which letter the digit 1 should be
        mapped to (when map01 is not None, the digit 0 is always mapped to
        the letter O).  For security purposes the default is None, so that
        0 and 1 are not allowed in the input.

        The result is returned as a bytes object.  A binascii.Error is raised if
        the input is incorrectly padded or if there are non-alphabet
        characters present in the input.

    """
    return NOT_IMPLEMENTED

def b16encode(s):
    """
    Encode the bytes-like object s using Base16 and return a bytes object.

    """
    return NOT_IMPLEMENTED

def b16decode(s, casefold=False):
    """
    Decode the Base16 encoded bytes-like object or ASCII string s.

        Optional casefold is a flag specifying whether a lowercase alphabet is
        acceptable as input.  For security purposes, the default is False.

        The result is returned as a bytes object.  A binascii.Error is raised if
        s is incorrectly padded or if there are non-alphabet characters present
        in the input.

    """
    return NOT_IMPLEMENTED

def _85encode(b, chars, chars2, pad=False, foldnuls=False, foldspaces=False):
    """
     Helper function for a85encode and b85encode

    """
    return NOT_IMPLEMENTED

def a85encode(b, *, foldspaces=False, wrapcol=0, pad=False, adobe=False):
    """
    Encode bytes-like object b using Ascii85 and return a bytes object.

        foldspaces is an optional flag that uses the special short sequence 'y'
        instead of 4 consecutive spaces (ASCII 0x20) as supported by 'btoa'. This
        feature is not supported by the "standard" Adobe encoding.

        wrapcol controls whether the output should have newline (b'\\n') characters
        added to it. If this is non-zero, each output line will be at most this
        many characters long.

        pad controls whether the input is padded to a multiple of 4 before
        encoding. Note that the btoa implementation always pads.

        adobe controls whether the encoded byte sequence is framed with <~ and ~>,
        which is used by the Adobe implementation.

    """
    return NOT_IMPLEMENTED

def a85decode(b, *, foldspaces=False, adobe=False, ignorechars=' \t\n\r'):
    """
    Decode the Ascii85 encoded bytes-like object or ASCII string b.

        foldspaces is a flag that specifies whether the 'y' short sequence should be
        accepted as shorthand for 4 consecutive spaces (ASCII 0x20). This feature is
        not supported by the "standard" Adobe encoding.

        adobe controls whether the input sequence is in Adobe Ascii85 format (i.e.
        is framed with <~ and ~>).

        ignorechars should be a byte string containing characters to ignore from the
        input. This should only contain whitespace characters, and by default
        contains all whitespace characters in ASCII.

        The result is returned as a bytes object.

    """
    return NOT_IMPLEMENTED

def b85encode(b, pad=False):
    """
    Encode bytes-like object b in base85 format and return a bytes object.

        If pad is true, the input is padded with b'\\0' so its length is a multiple of
        4 bytes before encoding.

    """
    return NOT_IMPLEMENTED

def b85decode(b):
    """
    Decode the base85-encoded bytes-like object or ASCII string b

        The result is returned as a bytes object.

    """
    return NOT_IMPLEMENTED

def encode(input, output):
    """
    Encode a file; input and output are binary files.
    """
    return NOT_IMPLEMENTED

def decode(input, output):
    """
    Decode a file; input and output are binary files.
    """
    return NOT_IMPLEMENTED

def _input_type_check(s):
    """
    expected bytes-like object, not %s
    """
    return NOT_IMPLEMENTED

def encodebytes(s):
    """
    Encode a bytestring into a bytes object containing multiple lines
        of base-64 data.
    """
    return NOT_IMPLEMENTED

def encodestring(s):
    """
    Legacy alias of encodebytes().
    """
    return NOT_IMPLEMENTED

def decodebytes(s):
    """
    Decode a bytestring of base-64 data into a bytes object.
    """
    return NOT_IMPLEMENTED

def decodestring(s):
    """
    Legacy alias of decodebytes().
    """
    return NOT_IMPLEMENTED

def main():
    """
    Small main program
    """
    return NOT_IMPLEMENTED

def test():
    """
    b"Aladdin:open sesame
    """
    return NOT_IMPLEMENTED

base64 = larky.struct(
    b64encode = b64encode,
    b64decode = b64decode,
)