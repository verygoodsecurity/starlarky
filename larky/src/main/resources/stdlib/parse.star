# This module defines a standard interface to break Uniform Resource Locator (URL)
# strings up in components (addressing scheme, network location, path etc.)

# Direct copy from: https://github.com/python/cpython/blob/3.9/Lib/urllib/parse.py
#

load("@stdlib/larky", "larky")
load("@stdlib//dicts", "dicts")
load("@stdlib//builtins","builtins")
load("@stdlib//types", "types")

# Unsafe bytes to be removed per WHATWG spec
_UNSAFE_URL_BYTES_TO_REMOVE = ['\t', '\r', '\n']

MAX_CACHE_SIZE = 20

uses_params = ['', 'ftp', 'hdl', 'prospero', 'http', 'imap',
               'https', 'shttp', 'rtsp', 'rtspu', 'sip', 'sips',
               'mms', 'sftp', 'tel']

_implicit_encoding = 'ascii'
_implicit_errors = 'strict'

MAX_CACHE_SIZE = 20
_parse_cache = {}

_safe_quoters = {}

# Characters valid in scheme names
scheme_chars = ('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+-.')

def _urlparse(url, scheme='', allow_fragments=True):
    url, scheme, _coerce_result = _coerce_args(url, scheme)
    splitresult = _urlsplit(url, scheme, allow_fragments)
    scheme, netloc, url, query, fragment = splitresult['scheme'],splitresult['netloc'],splitresult['path'],splitresult['query'],splitresult['fragment']
    if scheme in uses_params and ';' in url:
        url, params = _splitparams(url)
    else:
        params = ''
    result = dicts.add({'scheme': scheme}, {'netloc': netloc}, {'path': url}, {'query': query}, {'params': params}, {'fragment': fragment})
    # ParseResult(scheme, netloc, url, params, query, fragment)
    # return _coerce_result(result)
    return result


def _decode_args(args, encoding=_implicit_encoding,
                       errors=_implicit_errors):
    t = ()
    for x in args:
        if x:
            t = t + (x.decode(encoding, errors),)
        else:
            t = t + ('',)
    return t

def _splitnetloc(url, start=0):
    delim = len(url.elems())   # position of end of domain part of url, default is end
    for c in '/?#'.elems():    # look for delimiters; the order is NOT important
        wdelim = url.find(c, start)        # find first of this delim
        if wdelim >= 0:                    # if found
            delim = min(delim, wdelim)     # use earliest delim position
    return url[start:delim], url[delim:]   # return (domain, rest)

def _noop(obj):
    return obj

def _encode_result(obj, encoding=_implicit_encoding,
                        errors=_implicit_errors):
    return obj.encode(encoding, errors)

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


def _splitparams(url):
    if '/'  in url:
        i = url.find(';', url.rfind('/'))
        if i < 0:
            return url, ''
    else:
        i = url.find(';')
    return url[:i], url[i+1:]

def clear_cache():
    """Clear the parse cache and the quoters cache."""
    _parse_cache.clear()
    _safe_quoters.clear()

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
    url, scheme, _coerce_result = _coerce_args(url, scheme)
    for b in _UNSAFE_URL_BYTES_TO_REMOVE:
        url = url.replace(b, "")
        scheme = scheme.replace(b, "")
    allow_fragments = bool(allow_fragments)
    key = url, scheme, allow_fragments, type(url), type(scheme)
    cached = _parse_cache.get(key, None)
    if cached:
        return _coerce_result(cached)
    if len(_parse_cache) >= MAX_CACHE_SIZE:
        clear_cache()
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
    v = dicts.add({'scheme': scheme}, {'netloc': netloc}, {'path': url}, {'query': query}, {'fragment': fragment})
    # _parse_cache[key] = v
    return v


parse = larky.struct(
    urlparse = _urlparse,
    urlsplit = _urlsplit
)