# Ported from CPython 3.9.7-3.10.1, pinned to 3.10.1 (2cd268a): Lib/urllib/request.py. See larky/src/main/resources/VENDORED.md.
# Direct copy from: https://github.com/python/cpython/blob/3.10/Lib/urllib/request.py

"""An extensible library for opening URLs using a variety of protocols

The simplest way to use this module is to call the urlopen function,
which accepts a string containing a URL or a Request object (described
below).  It opens the URL and returns the results as file-like
object; the returned object has some extra methods described below.

The OpenerDirector manages a collection of Handler objects that do
all the actual work.  Each Handler implements a particular protocol or
option.  The OpenerDirector is a composite object that invokes the
Handlers needed to open the requested URL.  For example, the
HTTPHandler performs HTTP GET and POST requests and deals with
non-error returns.  The HTTPRedirectHandler automatically deals with
HTTP 301, 302, 303 and 307 redirect errors, and the HTTPDigestAuthHandler
deals with digest authentication.

urlopen(url, data=None) -- Basic usage is the same as original
urllib.  pass the url and optionally data to post to an HTTP URL, and
get a file-like object back.  One difference is that you can also pass
a Request instance instead of URL.  Raises a URLError (subclass of
OSError); for HTTP errors, raises an HTTPError, which can also be
treated as a valid response.

build_opener -- Function that creates a new OpenerDirector instance.
Will install the default handlers.  Accepts one or more Handlers as
arguments, either instances or Handler classes that it will
instantiate.  If one of the argument is a subclass of the default
handler, the argument will be installed instead of the default.

install_opener -- Installs a new opener as the default opener.

objects of interest:

OpenerDirector -- Sets up the User Agent as the Python-urllib client and manages
the Handler classes, while dealing with requests and responses.

Request -- An object that encapsulates the state of a request.  The
state can be as simple as the URL.  It can also include extra HTTP
headers, e.g. a User-Agent.

BaseHandler --

internals:
BaseHandler and parent
_call_chain conventions

Example usage:

import urllib.request

# set up authentication info
authinfo = urllib.request.HTTPBasicAuthHandler()
authinfo.add_password(realm='PDQ Application',
                      uri='https://mahler:8092/site-updates.py',
                      user='klem',
                      passwd='geheim$parole')

proxy_support = urllib.request.ProxyHandler({"http" : "http://ahad-haam:3128"})

# build a new opener that adds authentication and caching FTP handlers
opener = urllib.request.build_opener(proxy_support, authinfo,
                                     urllib.request.CacheFTPHandler)

# install it
urllib.request.install_opener(opener)

f = urllib.request.urlopen('https://www.python.org/')
"""

load("@stdlib//builtins", builtins="builtins")
load("@stdlib//larky", larky="larky")
load("@stdlib//operator", operator="operator")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//re", re="re")
load("@stdlib//types", types="types")
load("@stdlib//urllib/parse", parse="parse")
load("@vendor//option/result", Result="Result", Error="Error")
load("@vgs//jhttprequest", _native_request="jhttprequest")


__all__ = [
    # Classes
    "Request",
]


urlparse = parse.urlparse
unwrap = parse.unwrap
_splittag = parse._splittag


# copied from cookielib.py
_cut_port_re = re.compile(r":\d+$")


def request_host(request):
    """Return request-host, as defined by RFC 2965.
    Variation from RFC: returned value is lowercased, for convenient
    comparison.
    """
    url = request.full_url
    host = parse.urlsplit(url).netloc
    if host == "":
        host = request.get_header("Host", "")

    # remove port, if present
    host = _cut_port_re.sub("", host, 1)
    return host.lower()

def parse_keqv_list(l):
    """Parse list of key=value strings where keys are not duplicated."""
    parsed = {}
    for elt in l:
        k, v = elt.split("=", 1)
        if v[0] == '"' and v[-1] == '"':
            v = v[1:-1]
        parsed[k] = v
    return parsed


def parse_http_list(s):
    """Parse lists as described by RFC 2068 Section 2.
    In particular, parse comma-separated lists where the elements of
    the list may include quoted-strings.  A quoted-string could
    contain a comma.  A non-quoted string could have quotes in the
    middle.  Neither commas nor quotes count if they are escaped.
    Only double-quotes count, not single-quotes.
    """
    res = []
    part = ""
    escape = False
    quote = escape
    for cur in iter(s):
        if escape:
            part += cur
            escape = False
            continue
        if quote:
            if cur == "\\":
                escape = True
                continue
            elif cur == '"':
                quote = False
            part += cur
            continue

        if cur == ",":
            res.append(part)
            part = ""
            continue

        if cur == '"':
            quote = True

        part += cur

    # append last part
    if part:
        res.append(part)

    return [part.strip() for part in res]

def _copy_header_items(self, headers):
    for key, value in headers.items():
        self.add_header(key, value)


def Request(url, data=None, headers={}, origin_req_host=None, unverifiable=False, method=None):
    """An HTTP request: CPython's urllib.request.Request, implemented in Java."""
    return _native_request.Request(
        Request, parse, request_host, _copy_header_items,
        url, data, headers, origin_req_host, unverifiable, method,
    )


request = larky.struct(
    __name__='request',
    Request=Request,
    request_host=request_host,
    _copy_header_items=_copy_header_items,
)
