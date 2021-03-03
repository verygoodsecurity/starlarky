def clear_cache():
    """
    Clear the parse cache and the quoters cache.
    """
def _noop(obj):
    """
    ''
    """
def _coerce_args(*args):
    """
     Invokes decode if necessary to create str args
     and returns the coerced inputs along with
     an appropriate result coercion function
       - noop for str inputs
       - encoding function otherwise

    """
def _ResultMixinStr(object):
    """
    Standard approach to encoding parsed results from str to bytes
    """
    def encode(self, encoding='ascii', errors='strict'):
        """
        Standard approach to decoding parsed results from bytes to str
        """
    def decode(self, encoding='ascii', errors='strict'):
        """
        Shared methods for the parsed result objects containing a netloc element
        """
    def username(self):
        """
         Scoped IPv6 address may have zone info, which must not be lowercased
         like http://[fe80::822a:a8ff:fe49:470c%tESt]:1234/keys

        """
    def port(self):
        """
        f'Port could not be cast to integer value as {port!r}'
        """
def _NetlocResultMixinStr(_NetlocResultMixinBase, _ResultMixinStr):
    """
    '@'
    """
    def _hostinfo(self):
        """
        '@'
        """
def _NetlocResultMixinBytes(_NetlocResultMixinBase, _ResultMixinBytes):
    """
    b'@'
    """
    def _hostinfo(self):
        """
        b'@'
        """
def DefragResult(_DefragResultBase, _ResultMixinStr):
    """
    '#'
    """
def SplitResult(_SplitResultBase, _NetlocResultMixinStr):
    """
     Structured result objects for bytes data

    """
def DefragResultBytes(_DefragResultBase, _ResultMixinBytes):
    """
    b'#'
    """
def SplitResultBytes(_SplitResultBase, _NetlocResultMixinBytes):
    """
     Set up the encode/decode result pairs

    """
def _fix_result_transcoding():
    """
    ''
    """
def _splitparams(url):
    """
    '/'
    """
def _splitnetloc(url, start=0):
    """
     position of end of domain part of url, default is end
    """
def _checknetloc(netloc):
    """
     looking for characters like \u2100 that expand to 'a/c'
     IDNA uses NFKC equivalence, so normalize for this check

    """
def urlsplit(url, scheme='', allow_fragments=True):
    """
    Parse a URL into 5 components:
        <scheme>://<netloc>/<path>?<query>#<fragment>
        Return a 5-tuple: (scheme, netloc, path, query, fragment).
        Note that we don't break the components up in smaller bits
        (e.g. netloc is a single string) and we don't expand % escapes.
    """
def urlunparse(components):
    """
    Put a parsed URL back together again.  This may result in a
        slightly different, but equivalent URL, if the URL that was parsed
        originally had redundant delimiters, e.g. a ? with an empty query
        (the draft states that these are equivalent).
    """
def urlunsplit(components):
    """
    Combine the elements of a tuple as returned by urlsplit() into a
        complete URL as a string. The data argument can be any five-item iterable.
        This may result in a slightly different, but equivalent URL, if the URL that
        was parsed originally had unnecessary delimiters (for example, a ? with an
        empty query; the RFC states that these are equivalent).
    """
def urljoin(base, url, allow_fragments=True):
    """
    Join a base URL and a possibly relative URL to form an absolute
        interpretation of the latter.
    """
def urldefrag(url):
    """
    Removes any existing fragment from URL.

        Returns a tuple of the defragmented URL and the fragment.  If
        the URL contained no fragments, the second element is the
        empty string.
    
    """
def unquote_to_bytes(string):
    """
    unquote_to_bytes('abc%20def') -> b'abc def'.
    """
def unquote(string, encoding='utf-8', errors='replace'):
    """
    Replace %xx escapes by their single-character equivalent. The optional
        encoding and errors parameters specify how to decode percent-encoded
        sequences into Unicode characters, as accepted by the bytes.decode()
        method.
        By default, percent-encoded sequences are decoded with UTF-8, and invalid
        sequences are replaced by a placeholder character.

        unquote('abc%20def') -> 'abc def'.
    
    """
2021-03-02 20:46:58,235 : INFO : tokenize_signature : --> do i ever get here?
def parse_qs(qs, keep_blank_values=False, strict_parsing=False,
             encoding='utf-8', errors='replace', max_num_fields=None):
    """
    Parse a query given as a string argument.

            Arguments:

            qs: percent-encoded query string to be parsed

            keep_blank_values: flag indicating whether blank values in
                percent-encoded queries should be treated as blank strings.
                A true value indicates that blanks should be retained as
                blank strings.  The default false value indicates that
                blank values are to be ignored and treated as if they were
                not included.

            strict_parsing: flag indicating what to do with parsing errors.
                If false (the default), errors are silently ignored.
                If true, errors raise a ValueError exception.

            encoding and errors: specify how to decode percent-encoded sequences
                into Unicode characters, as accepted by the bytes.decode() method.

            max_num_fields: int. If set, then throws a ValueError if there
                are more than n fields read by parse_qsl().

            Returns a dictionary.
    
    """
2021-03-02 20:46:58,236 : INFO : tokenize_signature : --> do i ever get here?
def parse_qsl(qs, keep_blank_values=False, strict_parsing=False,
              encoding='utf-8', errors='replace', max_num_fields=None):
    """
    Parse a query given as a string argument.

            Arguments:

            qs: percent-encoded query string to be parsed

            keep_blank_values: flag indicating whether blank values in
                percent-encoded queries should be treated as blank strings.
                A true value indicates that blanks should be retained as blank
                strings.  The default false value indicates that blank values
                are to be ignored and treated as if they were  not included.

            strict_parsing: flag indicating what to do with parsing errors. If
                false (the default), errors are silently ignored. If true,
                errors raise a ValueError exception.

            encoding and errors: specify how to decode percent-encoded sequences
                into Unicode characters, as accepted by the bytes.decode() method.

            max_num_fields: int. If set, then throws a ValueError
                if there are more than n fields read by parse_qsl().

            Returns a list, as G-d intended.
    
    """
def unquote_plus(string, encoding='utf-8', errors='replace'):
    """
    Like unquote(), but also replace plus signs by spaces, as required for
        unquoting HTML form values.

        unquote_plus('%7e/abc+def') -> '~/abc def'
    
    """
def Quoter(collections.defaultdict):
    """
    A mapping from bytes (in range(0,256)) to strings.

        String values are percent-encoded byte values, unless the key < 128, and
        in the "safe" set (either the specified safe set, or default set).
    
    """
    def __init__(self, safe):
        """
        safe: bytes object.
        """
    def __repr__(self):
        """
         Without this, will just display as a defaultdict

        """
    def __missing__(self, b):
        """
         Handle a cache miss. Store quoted string in cache and return.

        """
def quote(string, safe='/', encoding=None, errors=None):
    """
    quote('abc def') -> 'abc%20def'

        Each part of a URL, e.g. the path info, the query, etc., has a
        different set of reserved characters that must be quoted. The
        quote function offers a cautious (not minimal) way to quote a
        string for most of these parts.

        RFC 3986 Uniform Resource Identifier (URI): Generic Syntax lists
        the following (un)reserved characters.

        unreserved    = ALPHA / DIGIT / "-" / "." / "_" / "~"
        reserved      = gen-delims / sub-delims
        gen-delims    = ":" / "/" / "?" / "#" / "[" / "]" / "@"
        sub-delims    = "!" / "$" / "&" / "'" / "(" / ")"
                      / "*" / "+" / "," / ";" / "="

        Each of the reserved characters is reserved in some component of a URL,
        but not necessarily in all of them.

        The quote function %-escapes all characters that are neither in the
        unreserved chars ("always safe") nor the additional chars set via the
        safe arg.

        The default for the safe arg is '/'. The character is reserved, but in
        typical usage the quote function is being called on a path where the
        existing slash characters are to be preserved.

        Python 3.7 updates from using RFC 2396 to RFC 3986 to quote URL strings.
        Now, "~" is included in the set of unreserved characters.

        string and safe may be either str or bytes objects. encoding and errors
        must not be specified if string is a bytes object.

        The optional encoding and errors parameters specify how to deal with
        non-ASCII characters, as accepted by the str.encode method.
        By default, encoding='utf-8' (characters are encoded with UTF-8), and
        errors='strict' (unsupported characters raise a UnicodeEncodeError).
    
    """
def quote_plus(string, safe='', encoding=None, errors=None):
    """
    Like quote(), but also replace ' ' with '+', as required for quoting
        HTML form values. Plus signs in the original string are escaped unless
        they are included in safe. It also does not have safe default to '/'.
    
    """
def quote_from_bytes(bs, safe='/'):
    """
    Like quote(), but accepts a bytes object rather than a str, and does
        not perform string-to-bytes encoding.  It always returns an ASCII string.
        quote_from_bytes(b'abc def\x3f') -> 'abc%20def%3f'
    
    """
2021-03-02 20:46:58,240 : INFO : tokenize_signature : --> do i ever get here?
def urlencode(query, doseq=False, safe='', encoding=None, errors=None,
              quote_via=quote_plus):
    """
    Encode a dict or sequence of two-element tuples into a URL query string.

        If any values in the query arg are sequences and doseq is true, each
        sequence element is converted to a separate parameter.

        If the query arg is a sequence of two-element tuples, the order of the
        parameters in the output will match the order of parameters in the
        input.

        The components of a query arg may each be either a string or a bytes type.

        The safe, encoding, and errors parameters are passed down to the function
        specified by quote_via (encoding and errors only if a component is a str).
    
    """
def to_bytes(url):
    """
    urllib.parse.to_bytes() is deprecated as of 3.8
    """
def _to_bytes(url):
    """
    to_bytes(u"URL") --> 'URL'.
    """
def unwrap(url):
    """
    Transform a string like '<URL:scheme://host/path>' into 'scheme://host/path'.

        The string is returned unchanged if it's not a wrapped URL.
    
    """
def splittype(url):
    """
    urllib.parse.splittype() is deprecated as of 3.8, 
    use urllib.parse.urlparse() instead
    """
def _splittype(url):
    """
    splittype('type:opaquestring') --> 'type', 'opaquestring'.
    """
def splithost(url):
    """
    urllib.parse.splithost() is deprecated as of 3.8, 
    use urllib.parse.urlparse() instead
    """
def _splithost(url):
    """
    splithost('//host[:port]/path') --> 'host[:port]', '/path'.
    """
def splituser(host):
    """
    urllib.parse.splituser() is deprecated as of 3.8, 
    use urllib.parse.urlparse() instead
    """
def _splituser(host):
    """
    splituser('user[:passwd]@host[:port]') --> 'user[:passwd]', 'host[:port]'.
    """
def splitpasswd(user):
    """
    urllib.parse.splitpasswd() is deprecated as of 3.8, 
    use urllib.parse.urlparse() instead
    """
def _splitpasswd(user):
    """
    splitpasswd('user:passwd') -> 'user', 'passwd'.
    """
def splitport(host):
    """
    urllib.parse.splitport() is deprecated as of 3.8, 
    use urllib.parse.urlparse() instead
    """
def _splitport(host):
    """
    splitport('host:port') --> 'host', 'port'.
    """
def splitnport(host, defport=-1):
    """
    urllib.parse.splitnport() is deprecated as of 3.8, 
    use urllib.parse.urlparse() instead
    """
def _splitnport(host, defport=-1):
    """
    Split host and port, returning numeric port.
        Return given default port if no ':' found; defaults to -1.
        Return numerical port if a valid number are found after ':'.
        Return None if ':' but not a valid number.
    """
def splitquery(url):
    """
    urllib.parse.splitquery() is deprecated as of 3.8, 
    use urllib.parse.urlparse() instead
    """
def _splitquery(url):
    """
    splitquery('/path?query') --> '/path', 'query'.
    """
def splittag(url):
    """
    urllib.parse.splittag() is deprecated as of 3.8, 
    use urllib.parse.urlparse() instead
    """
def _splittag(url):
    """
    splittag('/path#tag') --> '/path', 'tag'.
    """
def splitattr(url):
    """
    urllib.parse.splitattr() is deprecated as of 3.8, 
    use urllib.parse.urlparse() instead
    """
def _splitattr(url):
    """
    splitattr('/path;attr1=value1;attr2=value2;...') ->
            '/path', ['attr1=value1', 'attr2=value2', ...].
    """
def splitvalue(attr):
    """
    urllib.parse.splitvalue() is deprecated as of 3.8, 
    use urllib.parse.parse_qsl() instead
    """
def _splitvalue(attr):
    """
    splitvalue('attr=value') --> 'attr', 'value'.
    """
