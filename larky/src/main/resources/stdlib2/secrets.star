def randbelow(exclusive_upper_bound):
    """
    Return a random int in the range [0, n).
    """
def token_bytes(nbytes=None):
    """
    Return a random byte string containing *nbytes* bytes.

        If *nbytes* is ``None`` or not supplied, a reasonable
        default is used.

        >>> token_bytes(16)  #doctest:+SKIP
        b'\\xebr\\x17D*t\\xae\\xd4\\xe3S\\xb6\\xe2\\xebP1\\x8b'

    
    """
def token_hex(nbytes=None):
    """
    Return a random text string, in hexadecimal.

        The string has *nbytes* random bytes, each byte converted to two
        hex digits.  If *nbytes* is ``None`` or not supplied, a reasonable
        default is used.

        >>> token_hex(16)  #doctest:+SKIP
        'f9bf78b9a18ce6d46a0cd2b0b86df9da'

    
    """
def token_urlsafe(nbytes=None):
    """
    Return a random URL-safe text string, in Base64 encoding.

        The string has *nbytes* random bytes.  If *nbytes* is ``None``
        or not supplied, a reasonable default is used.

        >>> token_urlsafe(16)  #doctest:+SKIP
        'Drmhze6EPcv0fN_81Bj-nA'

    
    """
