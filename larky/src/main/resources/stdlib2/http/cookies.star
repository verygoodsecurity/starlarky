def CookieError(Exception):
    """
     These quoting routines conform to the RFC2109 specification, which in
     turn references the character definitions from RFC2068.  They provide
     a two-way quoting algorithm.  Any non-text character is translated
     into a 4 character sequence: a forward-slash followed by the
     three-digit octal equivalent of the character.  Any '\' or '"' is
     quoted with a preceding '\' slash.
     Because of the way browsers really handle cookies (as opposed to what
     the RFC says) we also encode "," and ";".

     These are taken from RFC2068 and RFC2109.
           _LegalChars       is the list of chars which don't require "'s
           _Translator       hash-table for fast quoting


    """
def _quote(str):
    """
    r"""Quote a string for use in a cookie header.

        If the string does not need to be double-quoted, then just return the
        string.  Otherwise, surround the string in doublequotes and quote
        (with a \) special characters.
    
    """
def _unquote(str):
    """
     If there aren't any doublequotes,
     then there can't be any special characters.  See RFC 2109.

    """
def _getdate(future=0, weekdayname=_weekdayname, monthname=_monthname):
    """
    %s, %02d %3s %4d %02d:%02d:%02d GMT
    """
def Morsel(dict):
    """
    A class to hold ONE (key, value) pair.

        In a cookie, each such pair may have several attributes, so this class is
        used to keep the attributes associated with the appropriate key,value pair.
        This class also includes a coded_value attribute, which is used to hold
        the network representation of the value.
    
    """
    def __init__(self):
        """
         Set defaults

        """
    def key(self):
        """
        Invalid attribute %r
        """
    def setdefault(self, key, val=None):
        """
        Invalid attribute %r
        """
    def __eq__(self, morsel):
        """
        Invalid attribute %r
        """
    def isReservedKey(self, K):
        """
        'Attempt to set a reserved key %r'
        """
    def __getstate__(self):
        """
        'key'
        """
    def __setstate__(self, state):
        """
        'key'
        """
    def output(self, attrs=None, header="Set-Cookie:"):
        """
        %s %s
        """
    def __repr__(self):
        """
        '<%s: %s>'
        """
    def js_output(self, attrs=None):
        """
         Print javascript

        """
    def OutputString(self, attrs=None):
        """
         Build up our result


        """
def BaseCookie(dict):
    """
    A container class for a set of Morsels.
    """
    def value_decode(self, val):
        """
        real_value, coded_value = value_decode(STRING)
                Called prior to setting a cookie's value from the network
                representation.  The VALUE is the value read from HTTP
                header.
                Override this function to modify the behavior of cookies.
        
        """
    def value_encode(self, val):
        """
        real_value, coded_value = value_encode(VALUE)
                Called prior to setting a cookie's value from the dictionary
                representation.  The VALUE is the value being assigned.
                Override this function to modify the behavior of cookies.
        
        """
    def __init__(self, input=None):
        """
        Private method for setting a cookie's value
        """
    def __setitem__(self, key, value):
        """
        Dictionary style assignment.
        """
    def output(self, attrs=None, header="Set-Cookie:", sep="\015\012"):
        """
        Return a string suitable for HTTP.
        """
    def __repr__(self):
        """
        '%s=%s'
        """
    def js_output(self, attrs=None):
        """
        Return a string suitable for JavaScript.
        """
    def load(self, rawdata):
        """
        Load cookies from a string (presumably HTTP_COOKIE) or
                from a dictionary.  Loading cookies from a dictionary 'd'
                is equivalent to calling:
                    map(Cookie.__setitem__, d.keys(), d.values())
        
        """
    def __parse_string(self, str, patt=_CookiePattern):
        """
         Our starting point
        """
def SimpleCookie(BaseCookie):
    """

        SimpleCookie supports strings as cookie values.  When setting
        the value using the dictionary assignment notation, SimpleCookie
        calls the builtin str() to convert the value to a string.  Values
        received from HTTP are kept as strings.
    
    """
    def value_decode(self, val):
