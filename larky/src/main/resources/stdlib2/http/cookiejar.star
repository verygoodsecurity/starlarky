def _debug(*args):
    """
    http.cookiejar
    """
def _warn_unhandled_exception():
    """
     There are a few catch-all except: statements in this module, for
     catching input that's bad in unexpected ways.  Warn if any
     exceptions are caught there.

    """
def _timegm(tt):
    """
    Mon
    """
def time2isoz(t=None):
    """
    Return a string representing time in seconds since epoch, t.

        If the function is called without an argument, it will use the current
        time.

        The format of the returned string is like "YYYY-MM-DD hh:mm:ssZ",
        representing Universal Time (UTC, aka GMT).  An example of this format is:

        1994-11-24 08:49:37Z

    
    """
def time2netscape(t=None):
    """
    Return a string representing time in seconds since epoch, t.

        If the function is called without an argument, it will use the current
        time.

        The format of the returned string is like this:

        Wed, DD-Mon-YYYY HH:MM:SS GMT

    
    """
def offset_from_tz_string(tz):
    """
    '-'
    """
def _str2time(day, mon, yr, hr, min, sec, tz):
    """
     translate month name to number
     month numbers start with 1 (January)

    """
def http2time(text):
    """
    Returns time in seconds since epoch of time represented by a string.

        Return value is an integer.

        None is returned if the format of str is unrecognized, the time is outside
        the representable range, or the timezone string is not recognized.  If the
        string contains no timezone, UTC is assumed.

        The timezone in the string may be numerical (like "-0800" or "+0100") or a
        string timezone (like "UTC", "GMT", "BST" or "EST").  Currently, only the
        timezone strings equivalent to UTC (zero offset) are known to the function.

        The function loosely parses the following formats:

        Wed, 09 Feb 1994 22:23:32 GMT       -- HTTP format
        Tuesday, 08-Feb-94 14:15:29 GMT     -- old rfc850 HTTP format
        Tuesday, 08-Feb-1994 14:15:29 GMT   -- broken rfc850 HTTP format
        09 Feb 1994 22:23:32 GMT            -- HTTP format (no weekday)
        08-Feb-94 14:15:29 GMT              -- rfc850 format (no weekday)
        08-Feb-1994 14:15:29 GMT            -- broken rfc850 format (no weekday)

        The parser ignores leading and trailing whitespace.  The time may be
        absent.

        If the year is given with only 2 digits, the function will select the
        century that makes the year closest to the current date.

    
    """
def iso2time(text):
    """

        As for http2time, but parses the ISO 8601 formats:

        1994-02-03 14:15:29 -0100    -- ISO 8601 format
        1994-02-03 14:15:29          -- zone is optional
        1994-02-03                   -- only date
        1994-02-03T14:15:29          -- Use T as separator
        19940203T141529Z             -- ISO 8601 compact format
        19940203                     -- only date

    
    """
def unmatched(match):
    """
    Return unmatched part of re.Match object.
    """
def split_header_words(header_values):
    """
    r"""Parse header values into a list of lists containing key,value pairs.

        The function knows how to deal with ",", ";" and "=" as well as quoted
        values after "=".  A list of space separated tokens are parsed as if they
        were separated by ";".

        If the header_values passed as argument contains multiple values, then they
        are treated as if they were a single value separated by comma ",".

        This means that this function is useful for parsing header fields that
        follow this syntax (BNF as from the HTTP/1.1 specification, but we relax
        the requirement for tokens).

          headers           = #header
          header            = (token | parameter) *( [";"] (token | parameter))

          token             = 1*<any CHAR except CTLs or separators>
          separators        = "(" | ")" | "<" | ">" | "@"
                            | "," | ";" | ":" | "\" | <">
                            | "/" | "[" | "]" | "?" | "="
                            | "{" | "}" | SP | HT

          quoted-string     = ( <"> *(qdtext | quoted-pair ) <"> )
          qdtext            = <any TEXT except <">>
          quoted-pair       = "\" CHAR

          parameter         = attribute "=" value
          attribute         = token
          value             = token | quoted-string

        Each header is represented by a list of key/value pairs.  The value for a
        simple token (not part of a parameter) is None.  Syntactically incorrect
        headers will not necessarily be parsed as you would want.

        This is easier to describe with some examples:

        >>> split_header_words(['foo="bar"; port="80,81"; discard, bar=baz'])
        [[('foo', 'bar'), ('port', '80,81'), ('discard', None)], [('bar', 'baz')]]
        >>> split_header_words(['text/html; charset="iso-8859-1"'])
        [[('text/html', None), ('charset', 'iso-8859-1')]]
        >>> split_header_words([r'Basic realm="\"foo\bar\""'])
        [[('Basic', None), ('realm', '"foobar"')]]

    
    """
def join_header_words(lists):
    """
    Do the inverse (almost) of the conversion done by split_header_words.

        Takes a list of lists of (key, value) pairs and produces a single header
        value.  Attribute values are quoted if needed.

        >>> join_header_words([[("text/plain", None), ("charset", "iso-8859-1")]])
        'text/plain; charset="iso-8859-1"'
        >>> join_header_words([[("text/plain", None)], [("charset", "iso-8859-1")]])
        'text/plain, charset="iso-8859-1"'

    
    """
def strip_quotes(text):
    """
    '"'
    """
def parse_ns_headers(ns_headers):
    """
    Ad-hoc parser for Netscape protocol cookie-attributes.

        The old Netscape cookie format for Set-Cookie can for instance contain
        an unquoted "," in the expires field, so we have to use this ad-hoc
        parser instead of split_header_words.

        XXX This may not make the best possible effort to parse all the crap
        that Netscape Cookie headers contain.  Ronald Tschalar's HTTPClient
        parser is probably better, so could do worse than following that if
        this ever gives any trouble.

        Currently, this is also used for parsing RFC 2109 cookies.

    
    """
def is_HDN(text):
    """
    Return True if text is a host domain name.
    """
def domain_match(A, B):
    """
    Return True if domain A domain-matches domain B, according to RFC 2965.

        A and B may be host domain names or IP addresses.

        RFC 2965, section 1:

        Host names can be specified either as an IP address or a HDN string.
        Sometimes we compare one host name with another.  (Such comparisons SHALL
        be case-insensitive.)  Host A's name domain-matches host B's if

             *  their host name strings string-compare equal; or

             * A is a HDN string and has the form NB, where N is a non-empty
                name string, B has the form .B', and B' is a HDN string.  (So,
                x.y.com domain-matches .Y.com but not Y.com.)

        Note that domain-match is not a commutative operation: a.b.c.com
        domain-matches .c.com, but not the reverse.

    
    """
def liberal_is_HDN(text):
    """
    Return True if text is a sort-of-like a host domain name.

        For accepting/blocking domains.

    
    """
def user_domain_match(A, B):
    """
    For blocking/accepting domains.

        A and B may be host domain names or IP addresses.

    
    """
def request_host(request):
    """
    Return request-host, as defined by RFC 2965.

        Variation from RFC: returned value is lowercased, for convenient
        comparison.

    
    """
def eff_request_host(request):
    """
    Return a tuple (request-host, effective request-host name).

        As defined by RFC 2965, except both are lowercased.

    
    """
def request_path(request):
    """
    Path component of request-URI, as defined by RFC 2965.
    """
def request_port(request):
    """
    ':'
    """
def uppercase_escaped_char(match):
    """
    %%%s
    """
def escape_path(path):
    """
    Escape any invalid characters in HTTP URL, and uppercase all escapes.
    """
def reach(h):
    """
    Return reach of host h, as defined by RFC 2965, section 1.

        The reach R of a host name H is defined as follows:

           *  If

              -  H is the host domain name of a host; and,

              -  H has the form A.B; and

              -  A has no embedded (that is, interior) dots; and

              -  B has at least one embedded dot, or B is the string "local".
                 then the reach of H is .B.

           *  Otherwise, the reach of H is H.

        >>> reach("www.acme.com")
        '.acme.com'
        >>> reach("acme.com")
        'acme.com'
        >>> reach("acme.local")
        '.local'

    
    """
def is_third_party(request):
    """


        RFC 2965, section 3.3.6:

            An unverifiable transaction is to a third-party host if its request-
            host U does not domain-match the reach R of the request-host O in the
            origin transaction.

    
    """
def Cookie:
    """
    HTTP Cookie.

        This class represents both Netscape and RFC 2965 cookies.

        This is deliberately a very simple class.  It just holds attributes.  It's
        possible to construct Cookie instances that don't comply with the cookie
        standards.  CookieJar.make_cookies is the factory function for Cookie
        objects -- it deals with cookie parsing, supplying defaults, and
        normalising to the representation used in this class.  CookiePolicy is
        responsible for checking them to see whether they should be accepted from
        and returned to the server.

        Note that the port may be present in the headers, but unspecified ("Port"
        rather than"Port=80", for example); if this is the case, port is None.

    
    """
2021-03-02 20:53:51,595 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,595 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,595 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,596 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,596 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,596 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,596 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,596 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,596 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,596 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,596 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self, version, name, value,
                 port, port_specified,
                 domain, domain_specified, domain_initial_dot,
                 path, path_specified,
                 secure,
                 expires,
                 discard,
                 comment,
                 comment_url,
                 rest,
                 rfc2109=False,
                 ):
        """
        if port is None, port_specified must be false
        """
    def has_nonstandard_attr(self, name):
        """

        """
    def __repr__(self):
        """
        version
        """
def CookiePolicy:
    """
    Defines which cookies get accepted from and returned to server.

        May also modify cookies, though this is probably a bad idea.

        The subclass DefaultCookiePolicy defines the standard rules for Netscape
        and RFC 2965 cookies -- override that if you want a customized policy.

    
    """
    def set_ok(self, cookie, request):
        """
        Return true if (and only if) cookie should be accepted from server.

                Currently, pre-expired cookies never get this far -- the CookieJar
                class deletes such cookies itself.

        
        """
    def return_ok(self, cookie, request):
        """
        Return true if (and only if) cookie should be returned to server.
        """
    def domain_return_ok(self, domain, request):
        """
        Return false if cookies should not be returned, given cookie domain.
        
        """
    def path_return_ok(self, path, request):
        """
        Return false if cookies should not be returned, given cookie path.
        
        """
def DefaultCookiePolicy(CookiePolicy):
    """
    Implements the standard rules for accepting and returning cookies.
    """
2021-03-02 20:53:51,599 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,599 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,599 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,599 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,600 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,600 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,600 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,600 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,600 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,600 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,600 : INFO : tokenize_signature : --> do i ever get here?
2021-03-02 20:53:51,600 : INFO : tokenize_signature : --> do i ever get here?
    def __init__(self,
                 blocked_domains=None, allowed_domains=None,
                 netscape=True, rfc2965=False,
                 rfc2109_as_netscape=None,
                 hide_cookie2=False,
                 strict_domain=False,
                 strict_rfc2965_unverifiable=True,
                 strict_ns_unverifiable=False,
                 strict_ns_domain=DomainLiberal,
                 strict_ns_set_initial_dollar=False,
                 strict_ns_set_path=False,
                 secure_protocols=("https", "wss")
                 ):
        """
        Constructor arguments should be passed as keyword arguments only.
        """
    def blocked_domains(self):
        """
        Return the sequence of blocked domains (as a tuple).
        """
    def set_blocked_domains(self, blocked_domains):
        """
        Set the sequence of blocked domains.
        """
    def is_blocked(self, domain):
        """
        Return None, or the sequence of allowed domains (as a tuple).
        """
    def set_allowed_domains(self, allowed_domains):
        """
        Set the sequence of allowed domains, or None.
        """
    def is_not_allowed(self, domain):
        """

                If you override .set_ok(), be sure to call this method.  If it returns
                false, so should your subclass (assuming your subclass wants to be more
                strict about which cookies to accept).

        
        """
    def set_ok_version(self, cookie, request):
        """
         Version is always set to 0 by parse_ns_headers if it's a Netscape
         cookie, so this must be an invalid RFC 2965 cookie.

        """
    def set_ok_verifiability(self, cookie, request):
        """
           third-party RFC 2965 cookie during 
        unverifiable transaction
        """
    def set_ok_name(self, cookie, request):
        """
         Try and stop servers setting V0 cookies designed to hack other
         servers that know both V0 and V1 protocols.

        """
    def set_ok_path(self, cookie, request):
        """
           path attribute %s is not a prefix of request 
        path %s
        """
    def set_ok_domain(self, cookie, request):
        """
           domain %s is in user block-list
        """
    def set_ok_port(self, cookie, request):
        """
        80
        """
    def return_ok(self, cookie, request):
        """

                If you override .return_ok(), be sure to call this method.  If it
                returns false, so should your subclass (assuming your subclass wants to
                be more strict about which cookies to return).

        
        """
    def return_ok_version(self, cookie, request):
        """
           RFC 2965 cookies are switched off
        """
    def return_ok_verifiability(self, cookie, request):
        """
           third-party RFC 2965 cookie during unverifiable 
        transaction
        """
    def return_ok_secure(self, cookie, request):
        """
           secure cookie with non-secure request
        """
    def return_ok_expires(self, cookie, request):
        """
           cookie expired
        """
    def return_ok_port(self, cookie, request):
        """
        80
        """
    def return_ok_domain(self, cookie, request):
        """
        .
        """
    def domain_return_ok(self, domain, request):
        """
         Liberal check of.  This is here as an optimization to avoid
         having to load lots of MSIE cookie files unless necessary.

        """
    def path_return_ok(self, path, request):
        """
        - checking cookie path=%s
        """
def vals_sorted_by_key(adict):
    """
    Iterates over nested mapping, depth-first, in sorted order by key.
    """
def Absent: pass
    """
    Collection of HTTP cookies.

        You may not need to know about this class: try
        urllib.request.build_opener(HTTPCookieProcessor).open(url).
    
    """
    def __init__(self, policy=None):
        """
        Checking %s for cookies to return
        """
    def _cookies_for_request(self, request):
        """
        Return a list of cookies to be returned to server.
        """
    def _cookie_attrs(self, cookies):
        """
        Return a list of cookie-attributes to be returned to server.

                like ['foo="bar"; $Path="/"', ...]

                The $Version attribute is also added when appropriate (currently only
                once per request).

        
        """
    def add_cookie_header(self, request):
        """
        Add correct Cookie: header to request (urllib.request.Request object).

                The Cookie2 header is also added unless policy.hide_cookie2 is true.

        
        """
    def _normalized_cookie_tuples(self, attrs_set):
        """
        Return list of tuples containing normalised cookie information.

                attrs_set is the list of lists of key,value pairs extracted from
                the Set-Cookie or Set-Cookie2 headers.

                Tuples are name, value, standard, rest, where name and value are the
                cookie name and value, standard is a dictionary containing the standard
                cookie-attributes (discard, secure, version, expires or max-age,
                domain, path and port) and rest is a dictionary containing the rest of
                the cookie-attributes.

        
        """
    def _cookie_from_cookie_tuple(self, tup, request):
        """
         standard is dict of standard cookie-attributes, rest is dict of the
         rest of them

        """
    def _cookies_from_attrs_set(self, attrs_set, request):
        """
        'rfc2109_as_netscape'
        """
    def make_cookies(self, response, request):
        """
        Return sequence of Cookie objects extracted from response object.
        """
                def no_matching_rfc2965(ns_cookie, lookup=lookup):
                    """
                    Set a cookie if policy says it's OK to do so.
                    """
    def set_cookie(self, cookie):
        """
        Set a cookie, without checking whether or not it should be set.
        """
    def extract_cookies(self, response, request):
        """
        Extract cookies from response, where allowable given the request.
        """
    def clear(self, domain=None, path=None, name=None):
        """
        Clear some cookies.

                Invoking this method without arguments will clear all cookies.  If
                given a single argument, only cookies belonging to that domain will be
                removed.  If given two arguments, cookies belonging to the specified
                path within that domain are removed.  If given three arguments, then
                the cookie with the specified name, path and domain is removed.

                Raises KeyError if no matching cookie exists.

        
        """
    def clear_session_cookies(self):
        """
        Discard all session cookies.

                Note that the .save() method won't save session cookies anyway, unless
                you ask otherwise by passing a true ignore_discard argument.

        
        """
    def clear_expired_cookies(self):
        """
        Discard all expired cookies.

                You probably don't need to call this method: expired cookies are never
                sent back to the server (provided you're using DefaultCookiePolicy),
                this method is called by CookieJar itself every so often, and the
                .save() method won't save expired cookies anyway (unless you ask
                otherwise by passing a true ignore_expires argument).

        
        """
    def __iter__(self):
        """
        Return number of contained cookies.
        """
    def __repr__(self):
        """
        <%s[%s]>
        """
    def __str__(self):
        """
        <%s[%s]>
        """
def LoadError(OSError): pass
    """
    CookieJar that can be loaded from and saved to a file.
    """
    def __init__(self, filename=None, delayload=False, policy=None):
        """

                Cookies are NOT loaded from the named file until either the .load() or
                .revert() method is called.

        
        """
    def save(self, filename=None, ignore_discard=False, ignore_expires=False):
        """
        Save cookies to a file.
        """
    def load(self, filename=None, ignore_discard=False, ignore_expires=False):
        """
        Load cookies from a file.
        """
2021-03-02 20:53:51,622 : INFO : tokenize_signature : --> do i ever get here?
    def revert(self, filename=None,
               ignore_discard=False, ignore_expires=False):
        """
        Clear all cookies and reload cookies from a saved file.

                Raises LoadError (or OSError) if reversion is not successful; the
                object's state will not be altered if this happens.

        
        """
def lwp_cookie_str(cookie):
    """
    Return string representation of Cookie in the LWP cookie file format.

        Actually, the format is extended a bit -- see module docstring.

    
    """
def LWPCookieJar(FileCookieJar):
    """

        The LWPCookieJar saves a sequence of "Set-Cookie3" lines.
        "Set-Cookie3" is the format used by the libwww-perl library, not known
        to be compatible with any browser, but which is easy to read and
        doesn't lose information about RFC 2965 cookies.

        Additional methods

        as_lwp_str(ignore_discard=True, ignore_expired=True)

    
    """
    def as_lwp_str(self, ignore_discard=True, ignore_expires=True):
        """
        Return cookies as a string of "\\n"-separated "Set-Cookie3" headers.

                ignore_discard and ignore_expires: see docstring for FileCookieJar.save

        
        """
    def save(self, filename=None, ignore_discard=False, ignore_expires=False):
        """
        w
        """
    def _really_load(self, f, filename, ignore_discard, ignore_expires):
        """
        %r does not look like a Set-Cookie3 (LWP) format 
        file
        """
def MozillaCookieJar(FileCookieJar):
    """


        WARNING: you may want to backup your browser's cookies file if you use
        this class to save cookies.  I *think* it works, but there have been
        bugs in the past!

        This class differs from CookieJar only in the format it uses to save and
        load cookies to and from a file.  This class uses the Mozilla/Netscape
        `cookies.txt' format.  lynx uses this file format, too.

        Don't expect cookies saved while the browser is running to be noticed by
        the browser (in fact, Mozilla on unix will overwrite your saved cookies if
        you change them on disk while it's running; on Windows, you probably can't
        save at all while the browser is running).

        Note that the Mozilla/Netscape format will downgrade RFC2965 cookies to
        Netscape cookies on saving.

        In particular, the cookie version and port number information is lost,
        together with information about whether or not Path, Port and Discard were
        specified by the Set-Cookie2 (or Set-Cookie) header, and whether or not the
        domain as set in the HTTP header started with a dot (yes, I'm aware some
        domains in Netscape files start with a dot and some don't -- trust me, you
        really don't want to know any more about this).

        Note that though Mozilla and Netscape use the same format, they use
        slightly different headers.  The class saves cookies using the Netscape
        header by default (Mozilla can cope with that).

    
    """
    def _really_load(self, f, filename, ignore_discard, ignore_expires):
        """
        %r does not look like a Netscape format cookies file
        """
    def save(self, filename=None, ignore_discard=False, ignore_expires=False):
        """
        w
        """
