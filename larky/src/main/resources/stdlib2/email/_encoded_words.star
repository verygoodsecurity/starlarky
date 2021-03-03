def decode_q(encoded):
    """
    b'_'
    """
def _QByteMap(dict):
    """
    b'-!*+/'
    """
    def __missing__(self, key):
        """
        ={:02X}
        """
def encode_q(bstring):
    """
    ''
    """
def len_q(bstring):
    """

     Base64



    """
def decode_b(encoded):
    """
     First try encoding with validate=True, fixing the padding if needed.
     This will succeed only if encoded includes no invalid characters.

    """
def encode_b(bstring):
    """
    'ascii'
    """
def len_b(bstring):
    """
     4 bytes out for each 3 bytes (or nonzero fraction thereof) in.

    """
def decode(ew):
    """
    Decode encoded word and return (string, charset, lang, defects) tuple.

        An RFC 2047/2243 encoded word has the form:

            =?charset*lang?cte?encoded_string?=

        where '*lang' may be omitted but the other parts may not be.

        This function expects exactly such a string (that is, it does not check the
        syntax and may raise errors if the string is not well formed), and returns
        the encoded_string decoded first from its Content Transfer Encoding and
        then from the resulting bytes into unicode using the specified charset.  If
        the cte-decoded string does not successfully decode using the specified
        character set, a defect is added to the defects list and the unknown octets
        are replaced by the unicode 'unknown' character \\uFDFF.

        The specified charset and language are returned.  The default for language,
        which is rarely if ever encountered, is the empty string.

    
    """
def encode(string, charset='utf-8', encoding=None, lang=''):
    """
    Encode string using the CTE encoding that produces the shorter result.

        Produces an RFC 2047/2243 encoded word of the form:

            =?charset*lang?cte?encoded_string?=

        where '*lang' is omitted unless the 'lang' parameter is given a value.
        Optional argument charset (defaults to utf-8) specifies the charset to use
        to encode the string to binary before CTE encoding it.  Optional argument
        'encoding' is the cte specifier for the encoding that should be used ('q'
        or 'b'); if it is None (the default) the encoding which produces the
        shortest encoded sequence is used, except that 'q' is preferred if it is up
        to five characters longer.  Optional argument 'lang' (default '') gives the
        RFC 2243 language string to specify in the encoded word.

    
    """
