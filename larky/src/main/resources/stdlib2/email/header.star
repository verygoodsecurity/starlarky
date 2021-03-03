def decode_header(header):
    """
    Decode a message header value without converting charset.

        Returns a list of (string, charset) pairs containing each of the decoded
        parts of the header.  Charset is None for non-encoded parts of the header,
        otherwise a lower-case string containing the name of the character set
        specified in the encoded string.

        header may be a string that may or may not contain RFC2047 encoded words,
        or it may be a Header object.

        An email.errors.HeaderParseError may be raised when certain decoding error
        occurs (e.g. a base64 decoding exception).
    
    """
2021-03-02 20:54:37,158 : INFO : tokenize_signature : --> do i ever get here?
def make_header(decoded_seq, maxlinelen=None, header_name=None,
                continuation_ws=' '):
    """
    Create a Header from a sequence of pairs as returned by decode_header()

        decode_header() takes a header value string and returns a sequence of
        pairs of the format (decoded_string, charset) where charset is the string
        name of the character set.

        This function takes one of those sequence of pairs and returns a Header
        instance.  Optional maxlinelen, header_name, and continuation_ws are as in
        the Header constructor.
    
    """
def Header:
    """
    ' '
    """
    def __str__(self):
        """
        Return the string value of the header.
        """
    def __eq__(self, other):
        """
         other may be a Header or a string.  Both are fine so coerce
         ourselves to a unicode (of the unencoded header value), swap the
         args and do another comparison.

        """
    def append(self, s, charset=None, errors='strict'):
        """
        Append a string to the MIME header.

                Optional charset, if given, should be a Charset instance or the name
                of a character set (which will be converted to a Charset instance).  A
                value of None (the default) means that the charset given in the
                constructor is used.

                s may be a byte string or a Unicode string.  If it is a byte string
                (i.e. isinstance(s, str) is false), then charset is the encoding of
                that byte string, and a UnicodeError will be raised if the string
                cannot be decoded with that charset.  If s is a Unicode string, then
                charset is a hint specifying the character set of the characters in
                the string.  In either case, when producing an RFC 2822 compliant
                header using RFC 2047 rules, the string will be encoded using the
                output codec of the charset.  If the string cannot be encoded to the
                output codec, a UnicodeError will be raised.

                Optional `errors' is passed as the errors argument to the decode
                call if s is a byte string.
        
        """
    def _nonctext(self, s):
        """
        True if string s is not a ctext character of RFC822.
        
        """
    def encode(self, splitchars=';, \t', maxlinelen=None, linesep='\n'):
        """
        r"""Encode a message header into an RFC-compliant format.

                There are many issues involved in converting a given string for use in
                an email header.  Only certain character sets are readable in most
                email clients, and as header strings can only contain a subset of
                7-bit ASCII, care must be taken to properly convert and encode (with
                Base64 or quoted-printable) header strings.  In addition, there is a
                75-character length limit on any given encoded header field, so
                line-wrapping must be performed, even with double-byte character sets.

                Optional maxlinelen specifies the maximum length of each generated
                line, exclusive of the linesep string.  Individual lines may be longer
                than maxlinelen if a folding point cannot be found.  The first line
                will be shorter by the length of the header name plus ": " if a header
                name was specified at Header construction time.  The default value for
                maxlinelen is determined at header construction time.

                Optional splitchars is a string containing characters which should be
                given extra weight by the splitting algorithm during normal header
                wrapping.  This is in very rough support of RFC 2822's `higher level
                syntactic breaks':  split points preceded by a splitchar are preferred
                during line splitting, with the characters preferred in the order in
                which they appear in the string.  Space and tab may be included in the
                string to indicate whether preference should be given to one over the
                other as a split point when other split chars do not appear in the line
                being split.  Splitchars does not affect RFC 2047 encoded lines.

                Optional linesep is a string to be used to separate the lines of
                the value.  The default value is the most useful for typical
                Python applications, but it can be set to \r\n to produce RFC-compliant
                line separators when needed.
        
        """
    def _normalize(self):
        """
         Step 1: Normalize the chunks so that all runs of identical charsets
         get collapsed into a single unicode string.

        """
def _ValueFormatter:
    """
    ' '
    """
    def add_transition(self):
        """
        ' '
        """
    def feed(self, fws, string, charset):
        """
         If the charset has no header encoding (i.e. it is an ASCII encoding)
         then we must split the header at the "highest level syntactic break
         possible. Note that we don't have a lot of smarts about field
         syntax; we just try to break on semi-colons, then commas, then
         whitespace.  Eventually, this should be pluggable.

        """
    def _maxlengths(self):
        """
         The first line's length.

        """
    def _ascii_split(self, fws, string, splitchars):
        """
         The RFC 2822 header folding algorithm is simple in principle but
         complex in practice.  Lines may be folded any place where "folding
         white space" appears by inserting a linesep character in front of the
         FWS.  The complication is that not all spaces or tabs qualify as FWS,
         and we are also supposed to prefer to break at "higher level
         syntactic breaks".  We can't do either of these without intimate
         knowledge of the structure of structured headers, which we don't have
         here.  So the best we can do here is prefer to break at the specified
         splitchars, and hope that we don't choose any spaces or tabs that
         aren't legal FWS.  (This is at least better than the old algorithm,
         where we would sometimes *introduce* FWS after a splitchar, or the
         algorithm before that, where we would turn all white space runs into
         single spaces or tabs.)

        """
    def _append_chunk(self, fws, string):
        """
         Find the best split point, working backward from the end.
         There might be none, on a long first line.

        """
def _Accumulator(list):
    """
    ''
    """
    def __len__(self):
