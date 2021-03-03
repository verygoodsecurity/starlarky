def header_length(bytearray):
    """
    Return the length of s when it is encoded with base64.
    """
def header_encode(header_bytes, charset='iso-8859-1'):
    """
    Encode a single header line with Base64 encoding in a given charset.

        charset names the character set to use to encode the header.  It defaults
        to iso-8859-1.  Base64 encoding is defined in RFC 2045.
    
    """
def body_encode(s, maxlinelen=76, eol=NL):
    """
    r"""Encode a string with base64.

        Each line will be wrapped at, at most, maxlinelen characters (defaults to
        76 characters).

        Each line of encoded text will end with eol, which defaults to "\n".  Set
        this to "\r\n" if you will be using the result of this function directly
        in an email.
    
    """
def decode(string):
    """
    Decode a raw base64 string, returning a bytes object.

        This function does not parse a full MIME header value encoded with
        base64 (like =?iso-8859-1?b?bmloISBuaWgh?=) -- please use the high
        level email.header class for that functionality.
    
    """
