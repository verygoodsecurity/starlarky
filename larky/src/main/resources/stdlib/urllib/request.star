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
# Direct copy from: https://github.com/python/cpython/blob/3.10/Lib/urllib/request.py

load("@stdlib//builtins", builtins="builtins")
load("@stdlib//larky", larky="larky",
     WHILE_LOOP_EMULATION_ITERATION="WHILE_LOOP_EMULATION_ITERATION",
)
load("@stdlib//operator", operator="operator")
load("@stdlib//codecs", codecs="codecs")
load("@stdlib//re", re="re")
load("@stdlib//types", types="types")
load("@stdlib//urllib/parse", parse="parse")
load("@vendor//option/result", Result="Result", Error="Error")


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
    host = urlparse(url)[1]
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

def Request(
    url,
    data=None,
    headers={},
    origin_req_host=None,
    unverifiable=False,
    method=None
):
    self = larky.mutablestruct(__name__="Request", __class__=Request)

    # full_url property
    def _get_full_url():
        if self.fragment:
            return "{}#{}".format(self._full_url, self.fragment)
        return self._full_url
    def _set_full_url(url):
        # unwrap('<URL:type://host/path>') --> 'type://host/path'
        self._full_url = unwrap(url)
        self._full_url, self.fragment = _splittag(self._full_url)
        self._parse()
    self.full_url = larky.property(_get_full_url, _set_full_url)
    self.url = larky.property(_get_full_url, _set_full_url) # non standard extension (for serialization..)

    # data property
    def _get_data():
        return self._data

    def _set_data(data):
        if data != self._data:
            self._data = data
            # issue 16464
            # if we change data we need to remove content-length header
            # (cause it's most probably calculated for previous value)
            if self.has_header("Content-length"):
                self.remove_header("Content-length")
    self.data = larky.property(_get_data, _set_data)

    def _parse():
        self.type, rest = parse._splittype(self._full_url)
        if self.type == None:
            fail("ValueError: " + "unknown url type: %r" % self.full_url)
        self.host, self.selector = parse._splithost(rest)
        if self.host:
            self.host = parse.unquote(self.host)
    self._parse = _parse

    def get_method():
        """Return a string indicating the HTTP request method."""
        default_method = "POST" if self.data != None else "GET"
        return getattr(self, "_method", default_method)
    self.get_method = get_method
    self.method = larky.property(get_method)

    def get_full_url():
        return self.full_url
    self.get_full_url = get_full_url

    def set_proxy(host, type):
        if self.type == "https" and not self._tunnel_host:
            self._tunnel_host = self.host
        else:
            self.type = type
            self.selector = self.full_url
        self.host = host
    self.set_proxy = set_proxy

    def has_proxy():
        return self.selector == self.full_url
    self.has_proxy = has_proxy

    def add_header(key, val):
        # original implementation uses key.capitalize(), however, we will not modify the keys.
        self._headers[key] = val
    self.add_header = add_header

    def add_unredirected_header(key, val):
        # will not be added to a redirected request
        # original implementation uses key.capitalize(), however, we will not modify the keys.
        self.unredirected_hdrs[key] = val
    self.add_unredirected_header = add_unredirected_header

    def has_header(header_name):
        return header_name in self.headers or header_name in self.unredirected_hdrs
    self.has_header = has_header

    def get_header(header_name, default=None):
        return self.headers.get(
            header_name,
            self.unredirected_hdrs.get(header_name, default)
        )
    self.get_header = get_header

    def remove_header(header_name):
        self.headers.pop(header_name, None)
        self.unredirected_hdrs.pop(header_name, None)
    self.remove_header = remove_header

    def header_items():
        hdrs = {}
        hdrs.update(self.unredirected_hdrs)
        hdrs.update(self._headers)
        return list(hdrs.items())
    self.header_items = header_items

    def _get_headers():
        return self._headers

    def _set_headers(headers):
        self._headers = {}
        for key, value in headers.items():
            self.add_header(key, value)
    self.headers = larky.property(_get_headers, _set_headers)


    def __init__(
        url,
        data=None,
        headers={},
        origin_req_host=None,
        unverifiable=False,
        method=None
    ):
        self.full_url = url
        self.unredirected_hdrs = {}
        self._headers = {}
        self._data = None
        self.data = data
        self._tunnel_host = None
        for key, value in headers.items():
            self.add_header(key, value)
        if origin_req_host == None:
            origin_req_host = request_host(self)
        self.origin_req_host = origin_req_host
        self.unverifiable = unverifiable
        if method:
            self._method = method
        return self
    self.__init__ = __init__

    self.__init__(url, data, headers, origin_req_host, unverifiable, method)

    return self


request = larky.struct(
    __name__='request',
    Request=Request,
)
