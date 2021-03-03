def header_check(octet):
    """
    Return True if the octet should be escaped with header quopri.
    """
def body_check(octet):
    """
    Return True if the octet should be escaped with body quopri.
    """
def header_length(bytearray):
    """
    Return a header quoted-printable encoding length.

        Note that this does not include any RFC 2047 chrome added by
        `header_encode()`.

        :param bytearray: An array of bytes (a.k.a. octets).
        :return: The length in bytes of the byte array when it is encoded with
            quoted-printable for headers.
    
    """
def body_length(bytearray):
    """
    Return a body quoted-printable encoding length.

        :param bytearray: An array of bytes (a.k.a. octets).
        :return: The length in bytes of the byte array when it is encoded with
            quoted-printable for bodies.
    
    """
def _max_append(L, s, maxlen, extra=''):
    """
    Turn a string in the form =AB to the ASCII character with value 0xab
    """
def quote(c):
    """
    'iso-8859-1'
    """
def body_encode(body, maxlinelen=76, eol=NL):
    """
    Encode with quoted-printable, wrapping at maxlinelen characters.

        Each line of encoded text will end with eol, which defaults to "\\n".  Set
        this to "\\r\\n" if you will be using the result of this function directly
        in an email.

        Each line will be wrapped at, at most, maxlinelen characters before the
        eol string (maxlinelen defaults to 76 characters, the maximum value
        permitted by RFC 2045).  Long lines will have the 'soft line break'
        quoted-printable character "=" appended to them, so the decoded text will
        be identical to the original text.

        The minimum maxlinelen is 4 to have room for a quoted character ("=XX")
        followed by a soft line break.  Smaller values will generate a
        ValueError.

    
    """
def decode(encoded, eol=NL):
    """
    Decode a quoted-printable string.

        Lines are separated with eol, which defaults to \\n.
    
    """
def _unquote_match(match):
    """
    Turn a match in the form =AB to the ASCII character with value 0xab
    """
def header_decode(s):
    """
    Decode a string encoded with RFC 2045 MIME header `Q' encoding.

        This function does not parse a full MIME header value encoded with
        quoted-printable (like =?iso-8859-1?q?Hello_World?=) -- please use
        the high level email.header class for that functionality.
    
    """
