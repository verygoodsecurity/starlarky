def _has_surrogates(s):
    """
    Return True if s contains surrogate-escaped binary data.
    """
def _sanitize(string):
    """
     Turn any escaped bytes into unicode 'unknown' char.  If the escaped
     bytes happen to be utf-8 they will instead get decoded, even if they
     were invalid in the charset the source was supposed to be in.  This
     seems like it is not a bad thing; a defect was still registered.

    """
def formataddr(pair, charset='utf-8'):
    """
    The inverse of parseaddr(), this takes a 2-tuple of the form
        (realname, email_address) and returns the string value suitable
        for an RFC 2822 From, To or Cc header.

        If the first element of pair is false, then the second element is
        returned unmodified.

        The optional charset is the character set that is used to encode
        realname in case realname is not ASCII safe.  Can be an instance of str or
        a Charset-like object which has a header_encode method.  Default is
        'utf-8'.
    
    """
def getaddresses(fieldvalues):
    """
    Return a list of (REALNAME, EMAIL) for each fieldvalue.
    """
def _format_timetuple_and_zone(timetuple, zone):
    """
    '%s, %02d %s %04d %02d:%02d:%02d %s'
    """
def formatdate(timeval=None, localtime=False, usegmt=False):
    """
    Returns a date string as specified by RFC 2822, e.g.:

        Fri, 09 Nov 2001 01:08:47 -0000

        Optional timeval if given is a floating point time value as accepted by
        gmtime() and localtime(), otherwise the current time is used.

        Optional localtime is a flag that when True, interprets timeval, and
        returns a date relative to the local timezone instead of UTC, properly
        taking daylight savings time into account.

        Optional argument usegmt means that the timezone is written out as
        an ascii string, not numeric one (so "GMT" instead of "+0000"). This
        is needed for HTTP, and is only used when localtime==False.
    
    """
def format_datetime(dt, usegmt=False):
    """
    Turn a datetime into a date string as specified in RFC 2822.

        If usegmt is True, dt must be an aware datetime with an offset of zero.  In
        this case 'GMT' will be rendered instead of the normal +0000 required by
        RFC2822.  This is to support HTTP headers involving date stamps.
    
    """
def make_msgid(idstring=None, domain=None):
    """
    Returns a string suitable for RFC 2822 compliant Message-ID, e.g:

        <142480216486.20800.16526388040877946887@nightshade.la.mastaler.com>

        Optional idstring if given is a string used to strengthen the
        uniqueness of the message id.  Optional domain if given provides the
        portion of the message id after the '@'.  It defaults to the locally
        defined hostname.
    
    """
def parsedate_to_datetime(data):
    """

        Parse addr into its constituent realname and email address parts.

        Return a tuple of realname and email address, unless the parse fails, in
        which case return a 2-tuple of ('', '').
    
    """
def unquote(str):
    """
    Remove quotes from a string.
    """
def decode_rfc2231(s):
    """
    Decode string according to RFC 2231
    """
def encode_rfc2231(s, charset=None, language=None):
    """
    Encode string according to RFC 2231.

        If neither charset nor language is given, then s is returned as-is.  If
        charset is given but not language, the string is encoded using the empty
        string for language.
    
    """
def decode_params(params):
    """
    Decode parameters list according to RFC 2231.

        params is a sequence of 2-tuples containing (param name, string value).
    
    """
2021-03-02 20:54:38,908 : INFO : tokenize_signature : --> do i ever get here?
def collapse_rfc2231_value(value, errors='replace',
                           fallback_charset='us-ascii'):
    """
     While value comes to us as a unicode string, we need it to be a bytes
     object.  We do not want bytes() normal utf-8 decoder, we want a straight
     interpretation of the string as character bytes.

    """
def localtime(dt=None, isdst=-1):
    """
    Return local time as an aware datetime object.

        If called without arguments, return current time.  Otherwise *dt*
        argument should be a datetime instance, and it is converted to the
        local time zone according to the system time zone database.  If *dt* is
        naive (that is, dt.tzinfo is None), it is assumed to be in local time.
        In this case, a positive or zero value for *isdst* causes localtime to
        presume initially that summer time (for example, Daylight Saving Time)
        is or is not (respectively) in effect for the specified time.  A
        negative value for *isdst* causes the localtime() function to attempt
        to divine whether summer time is in effect for the specified time.

    
    """
