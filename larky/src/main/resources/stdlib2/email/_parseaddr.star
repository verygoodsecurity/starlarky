def parsedate_tz(data):
    """
    Convert a date string to a time tuple.

        Accounts for military timezones.
    
    """
def _parsedate_tz(data):
    """
    Convert date to extended time tuple.

        The last (additional) element is the time zone offset in seconds, except if
        the timezone was specified as -0000.  In that case the last element is
        None.  This indicates a UTC timestamp that explicitly declaims knowledge of
        the source timezone, as opposed to a +0000 timestamp that indicates the
        source timezone really was UTC.

    
    """
def parsedate(data):
    """
    Convert a time string to a time tuple.
    """
def mktime_tz(data):
    """
    Turn a 10-tuple as returned by parsedate_tz() into a POSIX timestamp.
    """
def quote(str):
    """
    Prepare string to be used in a quoted string.

        Turns backslash and double quote characters into quoted pairs.  These
        are the only characters that need to be quoted inside a quoted string.
        Does not add the surrounding double quotes.
    
    """
def AddrlistClass:
    """
    Address parser class by Ben Escoto.

        To understand what this class does, it helps to have a copy of RFC 2822 in
        front of you.

        Note: this class interface is deprecated and may be removed in the future.
        Use email.utils.AddressList instead.
    
    """
    def __init__(self, field):
        """
        Initialize a new instance.

                `field' is an unparsed address header field, containing
                one or more addresses.
        
        """
    def gotonext(self):
        """
        Skip white space and extract comments.
        """
    def getaddrlist(self):
        """
        Parse all addresses.

                Returns a list containing all of the addresses.
        
        """
    def getaddress(self):
        """
        Parse the next address.
        """
    def getrouteaddr(self):
        """
        Parse a route address (Return-path value).

                This method just skips all the route stuff and returns the addrspec.
        
        """
    def getaddrspec(self):
        """
        Parse an RFC 2822 addr-spec.
        """
    def getdomain(self):
        """
        Get the complete domain name from an address.
        """
    def getdelimited(self, beginchar, endchars, allowcomments=True):
        """
        Parse a header fragment delimited by special characters.

                `beginchar' is the start character for the fragment.
                If self is not looking at an instance of `beginchar' then
                getdelimited returns the empty string.

                `endchars' is a sequence of allowable end-delimiting characters.
                Parsing stops when one of these is encountered.

                If `allowcomments' is non-zero, embedded RFC 2822 comments are allowed
                within the parsed fragment.
        
        """
    def getquote(self):
        """
        Get a quote-delimited fragment from self's field.
        """
    def getcomment(self):
        """
        Get a parenthesis-delimited fragment from self's field.
        """
    def getdomainliteral(self):
        """
        Parse an RFC 2822 domain-literal.
        """
    def getatom(self, atomends=None):
        """
        Parse an RFC 2822 atom.

                Optional atomends specifies a different set of end token delimiters
                (the default is to use self.atomends).  This is used e.g. in
                getphraselist() since phrase endings must not include the `.' (which
                is legal in phrases).
        """
    def getphraselist(self):
        """
        Parse a sequence of RFC 2822 phrases.

                A phrase is a sequence of words, which are in turn either RFC 2822
                atoms or quoted-strings.  Phrases are canonicalized by squeezing all
                runs of continuous whitespace into one space.
        
        """
def AddressList(AddrlistClass):
    """
    An AddressList encapsulates a list of parsed RFC 2822 addresses.
    """
    def __init__(self, field):
        """
         Set union

        """
    def __iadd__(self, other):
        """
         Set union, in-place

        """
    def __sub__(self, other):
        """
         Set difference

        """
    def __isub__(self, other):
        """
         Set difference, in-place

        """
    def __getitem__(self, index):
        """
         Make indexing, slices, and 'in' work

        """
