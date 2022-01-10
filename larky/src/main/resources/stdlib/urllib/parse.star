# This module defines a standard interface to break Uniform Resource Locator (URL)
# strings up in components (addressing scheme, network location, path etc.)

# Direct copy from: https://github.com/python/cpython/blob/3.9/Lib/urllib/parse.py
#

load("@stdlib/larky", "larky")
load("@stdlib//builtins","builtins")
load("@stdlib//types", "types")
load("@stdlib//codecs", "codecs")
load("@stdlib//re", "re")
load("@stdlib//binascii", "unhexlify")
load("@stdlib//sets", "sets")
load("@stdlib//collections", namedtuple="namedtuple")
load("@vendor//option/result", Result="Result", Error="Error", _safe="safe")

# Unsafe bytes to be removed per WHATWG spec
_UNSAFE_URL_BYTES_TO_REMOVE = ['\t', '\r', '\n']

# A classification of schemes.
# The empty string classifies URLs with no scheme specified,
# being the default value returned by “urlsplit” and “urlparse”.

uses_netloc = ['', 'ftp', 'http', 'gopher', 'nntp', 'telnet',
               'imap', 'wais', 'file', 'mms', 'https', 'shttp',
               'snews', 'prospero', 'rtsp', 'rtspu', 'rsync',
               'svn', 'svn+ssh', 'sftp', 'nfs', 'git', 'git+ssh',
               'ws', 'wss']

uses_params = ['', 'ftp', 'hdl', 'prospero', 'http', 'imap',
               'https', 'shttp', 'rtsp', 'rtspu', 'sip', 'sips',
               'mms', 'sftp', 'tel']

_implicit_encoding = 'ascii'
_implicit_errors = 'strict'

# MAX_CACHE_SIZE = 20
# _parse_cache = {}
# _safe_quoters = {}

# Characters valid in scheme names
scheme_chars = ('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+-.')

def _encode_result(obj, encoding=_implicit_encoding,
                        errors=_implicit_errors):
    return codecs.encode(obj, encoding, errors)

def _decode_args(args, encoding=_implicit_encoding,
                       errors=_implicit_errors):
    t = ()
    for x in args:
        if x:
            t = t + (codecs.decode(x, encoding, errors),)
        else:
            t = t + ('',)
    return t

def _noop(obj):
    return obj

# def clear_cache():
#     """Clear the parse cache and the quoters cache."""
#     _parse_cache.clear()
#     _safe_quoters.clear()

def _coerce_args(*args):
    # Invokes decode if necessary to create str args
    # and returns the coerced inputs along with
    # an appropriate result coercion function
    #   - noop for str inputs
    #   - encoding function otherwise
    str_input = types.is_string(args[0])
    for arg in args[1:]:
        # We special-case the empty string to support the
        # "scheme=''" default argument to some functions
        if arg and types.is_string(arg) != str_input:
            fail('TypeError("Cannot mix str and non-str arguments")')
    if str_input:
        return args + (_noop,)
    return _decode_args(args) + (_encode_result,)


_SplitResultBase = namedtuple(
    'SplitResult', 'scheme netloc path query fragment')

_ParseResultBase = namedtuple(
    'ParseResult', 'scheme netloc path params query fragment')


def _ParseResult(**kwargs):
    return _ParseResultBase(**kwargs)

def _SplitResult(**kwargs):
    return _SplitResultBase(**kwargs)

def _urlparse(url, scheme='', allow_fragments=True):
    """Parse a URL into 6 components:
        <scheme>://<netloc>/<path>;<params>?<query>#<fragment>
    """
    url, scheme, _coerce_result = _coerce_args(url, scheme)
    splitresult = _urlsplit(url, scheme, allow_fragments)
    scheme, netloc, url, query, fragment = splitresult.scheme, splitresult.netloc, splitresult.path, splitresult.query, splitresult.fragment
    if scheme in uses_params and ';' in url:
        url, params = _splitparams(url)
    else:
        params = ''
    kwargs = {'scheme': scheme, 'netloc': netloc, 'path': url, 'params': params, 'query': query, 'fragment': fragment}
    result = _ParseResult(**kwargs)
    return _coerce_result(result)

def _urlunparse(components):
    """Put a parsed URL back together again.  This may result in a
    slightly different, but equivalent URL, if the URL that was parsed
    originally had redundant delimiters, e.g. a ? with an empty query
    (the draft states that these are equivalent)."""
    scheme, netloc, url, params, query, fragment, _coerce_result = (
                                                  _coerce_args(*components))
    if params:
        url = "%s;%s" % (url, params)
    return _coerce_result(_urlunsplit((scheme, netloc, url, query, fragment)))

def _splitparams(url):
    if '/'  in url:
        i = url.find(';', url.rfind('/'))
        if i < 0:
            return url, ''
    else:
        i = url.find(';')
    return url[:i], url[i+1:]

def _splitnetloc(url, start=0):
    delim = len(url.elems())   # position of end of domain part of url, default is end
    for c in '/?#'.elems():    # look for delimiters; the order is NOT important
        wdelim = url.find(c, start)        # find first of this delim
        if wdelim >= 0:                    # if found
            delim = min(delim, wdelim)     # use earliest delim position
    return url[start:delim], url[delim:]   # return (domain, rest)

def _checknetloc(netloc):
    pass
    # if not netloc or netloc.isascii():
    #     return
    # looking for characters like \u2100 that expand to 'a/c'
    # IDNA uses NFKC equivalence, so normalize for this check
    # import unicodedata
    # n = netloc.replace('@', '')   # ignore characters already included
    # n = n.replace(':', '')        # but not the surrounding text
    # n = n.replace('#', '')
    # n = n.replace('?', '')
    # netloc2 = unicodedata.normalize('NFKC', n)
    # if n == netloc2:
    #     return
    # for c in '/?#@:'.elems():
    #     if c in netloc2:
    #         fail('ValueError("netloc contains invalid characters under NFKC normalization")')

def _urlsplit(url, scheme='', allow_fragments=True):
    """Parse a URL into 5 components:
    <scheme>://<netloc>/<path>?<query>#<fragment>
    """
    url, scheme, _coerce_result = _coerce_args(url, scheme)
    for b in _UNSAFE_URL_BYTES_TO_REMOVE:
        url = url.replace(b, "")
        scheme = scheme.replace(b, "")
    allow_fragments = bool(allow_fragments)
    # key = url, scheme, allow_fragments, type(url), type(scheme)
    # cached = _parse_cache.get(key, None)
    # if cached:
    #     return _coerce_result(cached)
    # if len(_parse_cache) >= MAX_CACHE_SIZE:
    #     clear_cache()
    netloc = ''
    query = ''
    fragment = ''
    i = url.find(':')
    if i > 0:
        valid = True
        for c in url[:i].elems():
            if c not in scheme_chars:
                valid = False
                break
        if valid:
            scheme, url = url[:i].lower(), url[i+1:]
    if url[:2] == '//':
        netloc, url = _splitnetloc(url, 2)
        if (('[' in netloc and ']' not in netloc) or (']' in netloc and '[' not in netloc)):
            fail('ValueError("Invalid IPv6 URL")')
    if allow_fragments and '#' in url:
        url, fragment = url.split('#', 1)
    if '?' in url:
        url, query = url.split('?', 1)
    # _checknetloc(netloc)
    kwargs = {'scheme': scheme, 'netloc': netloc, 'path': url, 'query': query, 'fragment': fragment}
    v = _SplitResult(**kwargs)
    # _parse_cache[key] = v
    return _coerce_result(v)

def _urlunsplit(components):
    """Combine the elements of a tuple as returned by urlsplit() into a
    complete URL as a string. The data argument can be any five-item iterable.
    This may result in a slightly different, but equivalent URL, if the URL that
    was parsed originally had unnecessary delimiters (for example, a ? with an
    empty query; the RFC states that these are equivalent)."""
    scheme, netloc, url, query, fragment, _coerce_result = (
                                          _coerce_args(*components))
    if netloc or (scheme and scheme in uses_netloc and url[:2] != '//'):
        if url and url[:1] != '/': url = '/' + url
        url = '//' + (netloc or '') + url
    if scheme:
        url = scheme + ':' + url
    if query:
        url = url + '?' + query
    if fragment:
        url = url + '#' + fragment
    return _coerce_result(url)

def _parse_qs(qs, keep_blank_values=False, strict_parsing=False,
             encoding='utf-8', errors='replace', max_num_fields=None, separator='&'):
    """Parse a query given as a string argument.
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
        separator: str. The symbol to use for separating the query arguments.
            Defaults to &.
        Returns a dictionary.
    """
    parsed_result = {}
    pairs = _parse_qsl(qs, keep_blank_values, strict_parsing,
                      encoding=encoding, errors=errors,
                      max_num_fields=max_num_fields, separator=separator)
    for name, value in pairs:
        if name in parsed_result:
            parsed_result[name].append(value)
        else:
            parsed_result[name] = [value]
    return parsed_result

def _parse_qsl(qs, keep_blank_values=False, strict_parsing=False,
              encoding='utf-8', errors='replace', max_num_fields=None, separator='&'):
    """Parse a query given as a string argument.
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
        separator: str. The symbol to use for separating the query arguments.
            Defaults to &.
        Returns a list, as G-d intended.
    """
    qs, _coerce_result = _coerce_args(qs)
    separator, _ = _coerce_args(separator)

    if not separator or (not types.is_string(separator) and not types.is_bytes(separator)):
        fail('ValueError("Separator must be of type string or bytes.")')

    # If max_num_fields is defined then check that the number of fields
    # is less than max_num_fields. This prevents a memory exhaustion DOS
    # attack via post bodies with many fields.
    if max_num_fields != None:
        num_fields = 1 + qs.count(separator)
        if max_num_fields < num_fields:
            fail('ValueError("Max number of fields exceeded")')

    pairs = qs.split(separator)
    r = []
    for name_value in pairs:
        if not name_value and not strict_parsing:
            continue
        nv = name_value.split('=', 1)
        if len(nv) != 2:
            if strict_parsing:
                fail('ValueError("bad query field: %s" )' % name_value)
            # Handle case of a control-name with no equal sign
            if keep_blank_values:
                nv.append('')
            else:
                continue
        if len(nv[1]) or keep_blank_values:
            name = nv[0].replace('+', ' ')
            name = _unquote(name, encoding=encoding, errors=errors)
            name = _coerce_result(name)
            value = nv[1].replace('+', ' ')
            value = _unquote(value, encoding=encoding, errors=errors)
            value = _coerce_result(value)
            r.append((name, value))
    return r

_asciire = re.compile('([\\x00-\\x7f]+)')

def _unquote(string, encoding='utf-8', errors='replace'):
    """Replace %xx escapes by their single-character equivalent. The optional
    encoding and errors parameters specify how to decode percent-encoded
    sequences into Unicode characters, as accepted by the bytes.decode()
    method.
    By default, percent-encoded sequences are decoded with UTF-8, and invalid
    sequences are replaced by a placeholder character.
    unquote('abc%20def') -> 'abc def'.
    """
    if types.is_bytes(string):
        return _unquote_to_bytes(string).decode(encoding, errors)
    if '%' not in string:
        string.split
        return string
    if encoding == None:
        encoding = 'utf-8'
    if errors == None:
        errors = 'replace'
    bits = _asciire.split(string)
    res = [bits[0]]
    append = res.append
    for i in range(1, len(bits), 2):
        append(codecs.decode(_unquote_to_bytes(bits[i]), encoding, errors))
        append(bits[i + 1])
    return ''.join(res)

_hexdig = '0123456789ABCDEFabcdef'.elems()

def b(s):
    return builtins.bytes(s, encoding='utf-8')

def _unquote_to_bytes(string):
    """unquote_to_bytes('abc%20def') -> b'abc def'."""
    # Note: strings are encoded as UTF-8. This is only an issue if it contains
    # unescaped non-ASCII characters, which URIs should not.
    if not string:
        return b('')
    if types.is_bytes(string):
        string = codecs.encode(string, encoding='utf-8')
    # bits = string.split(b'%')
    bits_str = str(string).split('%')
    bits = [b(el) for el in bits_str]
    if len(bits) == 1:
        return string
    res = [bits[0]]
    # global _hextobyte
    _hextobyte = {codecs.encode(a + b, encoding='utf-8'): unhexlify(a + b)
                    for a in _hexdig for b in _hexdig}
    for item in bits[1:]:
        append = res.append
        if item[:2] in _hextobyte:
            append(_hextobyte[item[:2]])
            append(item[2:])
        else:
            append(b('%'))
            append(item)
    return b('').join(res)


_ALWAYS_SAFE = sets.Set((
    b'ABCDEFGHIJKLMNOPQRSTUVWXYZ' +
    b'abcdefghijklmnopqrstuvwxyz' +
    b'0123456789' +
    b'_.-~').elems())
_ALWAYS_SAFE_BYTES = bytes(_ALWAYS_SAFE._data)


def _byte_quoter_factory(safe):
    safe = _ALWAYS_SAFE.union(sets.Set(safe))

    def quoter(b):
        return chr(b) if safe.contains(b) else ('%%%X' % b)

    return quoter


def quote_from_bytes(bs, safe='/'):
    """Like quote(), but accepts a bytes object rather than a str, and does
    not perform string-to-bytes encoding.  It always returns an ASCII string.
    quote_from_bytes(b'abc def\x3f') -> 'abc%20def%3f'
    """
    if not types.is_bytelike(bs):
        return Error("TypeError: quote_from_bytes() expected bytes").unwrap()
    if not bs:
        return ''
    if types.is_string(safe):
        # Normalize 'safe' by converting to bytes and removing non-ASCII chars
        safe = codecs.encode(safe, encoding='ascii', errors='ignore')
    else:
        # List comprehensions are faster than generator expressions.
        safe = bytes([c for c in safe.elems() if c < 128])
    if not bs.rstrip(_ALWAYS_SAFE_BYTES + safe):
        return bs.decode()
    quoter = _byte_quoter_factory(safe.elems())
    return ''.join([quoter(char) for char in bs.elems()])



def quote(string, safe='/', encoding=None, errors=None):
    """quote('abc def') -> 'abc%20def'

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
    if types.is_string(string):
        if not string:
            return string
        if encoding == None:
            encoding = 'utf-8'
        if errors == None:
            errors = 'strict'
        string = codecs.encode(string, encoding=encoding, errors=errors)
    else:
        if encoding != None:
            return Error("TypeError: quote() doesn't support 'encoding' for bytes").unwrap()
        if errors != None:
            return Error("TypeError: quote() doesn't support 'errors' for bytes").unwrap()
    return quote_from_bytes(string, safe)


def quote_plus(string, safe='', encoding=None, errors=None):
    """Like quote(), but also replace ' ' with '+', as required for quoting
    HTML form values. Plus signs in the original string are escaped unless
    they are included in safe. It also does not have safe default to '/'.
    """
    # Check if ' ' in string, where string may either be a str or bytes.  If
    # there are no spaces, the regular quote will produce the right answer.
    if ((types.is_string(string) and ' ' not in string) or
        (types.is_bytes(string) and b' ' not in string)):
        return quote(string, safe, encoding, errors)
    if types.is_string(safe):
        space = ' '
    else:
        space = b' '
    string = quote(string, safe + space, encoding, errors)
    return string.replace(' ', '+')


def urlencode(query, doseq=False, safe='', encoding=None, errors=None,
              quote_via=quote_plus):
    """Encode a dict or sequence of two-element tuples into a URL query string.

    If any values in the query arg are sequences and doseq is true, each
    sequence element is converted to a separate parameter.

    If the query arg is a sequence of two-element tuples, the order of the
    parameters in the output will match the order of parameters in the
    input.

    The components of a query arg may each be either a string or a bytes type.
    The safe, encoding, and errors parameters are passed down to the function
    specified by quote_via (encoding and errors only if a component is a str).
    """

    if hasattr(query, "items"):
        query = query.items()
    else:
        # In Larky, strings and string-like objects are not sequences.
        if len(query) and (
                types.is_string(query[0])
                or types.is_bytelike(query[0])
        ):
            return Error("not a valid non-string sequence or mapping object").unwrap()
        # Zero-length sequences of all types will get here and succeed,
        # but that's a minor nit.  Since the original implementation
        # allowed empty dicts that type of behavior probably should be
        # preserved for consistency

    l = []
    if not doseq:
        for k, v in query:
            if types.is_bytelike(k):
                k = quote_via(k, safe)
            else:
                k = quote_via(str(k), safe, encoding, errors)

            if types.is_bytelike(v):
                v = quote_via(v, safe)
            else:
                v = quote_via(str(v), safe, encoding, errors)
            l.append(k + '=' + v)
    else:
        for k, v in query:
            if types.is_bytelike(k):
                k = quote_via(k, safe)
            else:
                k = quote_via(str(k), safe, encoding, errors)

            if types.is_bytelike(v):
                v = quote_via(v, safe)
                l.append(k + '=' + v)
            elif types.is_string(v):
                v = quote_via(v, safe, encoding, errors)
                l.append(k + '=' + v)
            else:
                # Is this a sufficient test for sequence-ness?
                rval = _safe(len)(v)
                if rval.is_err:
                    # not a sequence
                    v = quote_via(str(v), safe, encoding, errors)
                    l.append(k + '=' + v)
                else:
                    # loop over the sequence
                    for elt in v:
                        if types.is_bytelike(elt):
                            elt = quote_via(elt, safe)
                        else:
                            elt = quote_via(str(elt), safe, encoding, errors)
                        l.append(k + '=' + elt)
    return '&'.join(l)


def unwrap(url):
    """Transform a string like '<URL:scheme://host/path>' into 'scheme://host/path'.
    The string is returned unchanged if it's not a wrapped URL.
    """
    # url = bytes(url, encoding='utf-8').strip()
    # if url[:1] == b'<' and url[-1:] == b'>':
    #     url = url[1:-1].strip()
    # if url[:4] == b'URL:':
    #     url = url[4:].strip()
    # return url.decode('utf-8')
    url = str(url).strip()
    if url[:1] == '<' and url[-1:] == '>':
        url = url[1:-1].strip()
    if url[:4] == 'URL:':
        url = url[4:].strip()
    return url


_typeprog = re.compile('([^/:]+):(.*)', re.DOTALL)
def _splittype(url):
    """splittype('type:opaquestring') --> 'type', 'opaquestring'."""
    match = _typeprog.match(url)
    if match:
        scheme, data = match.groups()
        return scheme.lower(), data
    return None, url


_hostprog = re.compile('//([^/#?]*)(.*)', re.DOTALL)
def _splithost(url):
    """splithost('//host[:port]/path') --> 'host[:port]', '/path'."""
    match = _hostprog.match(url)
    if match:
        host_port, path = match.groups()
        if path and path[0] != '/':
            path = '/' + path
        return host_port, path
    return None, url


def _splituser(host):
    """splituser('user[:passwd]@host[:port]') --> 'user[:passwd]', 'host[:port]'."""
    user, delim, host = host.rpartition('@')
    return (user if delim else None), host


def _splitpasswd(user):
    """splitpasswd('user:passwd') -> 'user', 'passwd'."""
    user, delim, passwd = user.partition(':')
    return user, (passwd if delim else None)


# splittag('/path#tag') --> '/path', 'tag'
_portprog = re.compile('(.*):([0-9]*)', re.DOTALL)
def _splitport(host):
    """splitport('host:port') --> 'host', 'port'."""
    match = _portprog.fullmatch(host)
    if match:
        host, port = match.groups()
        if port:
            return host, port
    return host, None


def _splitnport(host, defport=-1):
    """Split host and port, returning numeric port.
    Return given default port if no ':' found; defaults to -1.
    Return numerical port if a valid number are found after ':'.
    Return None if ':' but not a valid number."""
    host, delim, port = host.rpartition(':')
    if not delim:
        host = port
    elif port:
        rval = Result.Ok(port).map(int)
        nport = rval.unwrap_or(None)
        return host, nport
    return host, defport


def _splitquery(url):
    """splitquery('/path?query') --> '/path', 'query'."""
    path, delim, query = url.rpartition('?')
    if delim:
        return path, query
    return url, None


def _splittag(url):
    """splittag('/path#tag') --> '/path', 'tag'."""
    path, delim, tag = url.rpartition('#')
    if delim:
        return path, tag
    return url, None


def _splitattr(url):
    """splitattr('/path;attr1=value1;attr2=value2;...') ->
        '/path', ['attr1=value1', 'attr2=value2', ...]."""
    words = url.split(';')
    return words[0], words[1:]


def _splitvalue(attr):
    """splitvalue('attr=value') --> 'attr', 'value'."""
    attr, delim, value = attr.partition('=')
    return attr, (value if delim else None)


parse = larky.struct(
    urlparse = _urlparse,
    urlsplit = _urlsplit,
    urlunparse = _urlunparse,
    urlunsplit = _urlunsplit,
    parse_qs = _parse_qs,
    parse_qsl = _parse_qsl,
    quote_from_bytes = quote_from_bytes,
    quote = quote,
    unquote = _unquote,
    quote_plus = quote_plus,
    urlencode = urlencode,
    unwrap = unwrap,
    _splittype = _splittype,
    _splithost = _splithost,
    _splituser = _splituser,
    _splitpasswd = _splitpasswd,
    _splitport = _splitport,
    _splitnport = _splitnport,
    _splitquery = _splitquery,
    _splittag = _splittag,
    _splitattr = _splitattr,
    _splitvalue = _splitvalue,
)
