"""VGSHttpRequest behaves as its Starlark implementation did.

_legacy_Request is urllib's Request as Larky 1.3.0 implemented it in Starlark, and
_legacy_VGSHttpRequest is VGSHttpRequest's implementation on top of it. Both are built
from the same inputs, then changed in the same ways through the public API, and every
public attribute is compared.
"""
load("@stdlib//builtins", builtins="builtins")
load("@stdlib//larky", larky="larky")
load("@stdlib//re", re="re")
load("@stdlib//unittest", unittest="unittest")
load("@stdlib//urllib/parse", parse="parse")
load("@vendor//asserts", asserts="asserts")
load("@vendor//option/result", safe="safe")
load("@vgs//http/request", VGSHttpRequest="VGSHttpRequest", VGSCIMultiDict="VGSCIMultiDict")


# Larky 1.3.0 stdlib/urllib/request.star, with unwrap/_splittag qualified.
_cut_port_re = re.compile(r":\d+$")


def _legacy_request_host(request):
    url = request.full_url
    host = parse.urlsplit(url).netloc
    if host == "":
        host = request.get_header("Host", "")

    # remove port, if present
    host = _cut_port_re.sub("", host, 1)
    return host.lower()


def _legacy_Request(
    url,
    data=None,
    headers={},
    origin_req_host=None,
    unverifiable=False,
    method=None
):
    self = larky.mutablestruct(__name__ = "Request", __class__ = _legacy_Request)

    # full_url property
    def _get_full_url():
        if self.fragment:
            return "{}#{}".format(self._full_url, self.fragment)
        return self._full_url
    def _set_full_url(url):
        # unwrap('<URL:type://host/path>') --> 'type://host/path'
        self._full_url = parse.unwrap(url)
        self._full_url, self.fragment = parse._splittag(self._full_url)
        self._parse()
    self.full_url = larky.property(_get_full_url, _set_full_url)

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
        return getattr(self, "method", default_method)
    self.get_method = get_method

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
        # useful for something like authentication
        self.headers[key.capitalize()] = val
    self.add_header = add_header

    def add_unredirected_header(key, val):
        # will not be added to a redirected request
        self.unredirected_hdrs[key.capitalize()] = val
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
        hdrs.update(self.headers)
        return list(hdrs.items())
    self.header_items = header_items


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
        self.headers = {}
        self._data = None
        self.data = data
        self._tunnel_host = None
        for key, value in headers.items():
            self.add_header(key, value)
        if origin_req_host == None:
            origin_req_host = _legacy_request_host(self)
        self.origin_req_host = origin_req_host
        self.unverifiable = unverifiable
        if method:
            self.method = method
        return self
    self.__init__ = __init__

    self.__init__(url, data, headers, origin_req_host, unverifiable, method)

    return self


def _legacy_VGSHttpRequest(
    url,
    data=None,
    headers={},
    method=None
):
    super = _legacy_Request(url)

    self = super
    self.__name__ = "VGSHttpRequest"
    self.__class__ = _legacy_VGSHttpRequest

    # url property
    def _get_url():
        return self.full_url
    def _set_url(url):
        self.full_url = url

        parsed_url = parse.urlsplit(url)
        self.path = parsed_url.path
        self.query_string = parsed_url.query
    self.url = larky.property(_get_url, _set_url)

    # body property
    def _get_body():
        return self._data

    def _set_body(data):
        self.data = data
    self.body = larky.property(_get_body, _set_body)

    # headers property
    def _get_headers():
        return self._headers

    def _set_headers(headers):
        self._headers = VGSCIMultiDict(headers)
    self.headers = larky.property(_get_headers, _set_headers)
    # override super
    def add_header(key, val):
        # original implementation uses key.capitalize(), however, we will not modify the keys.
        self.headers[key] = val
    self.add_header = add_header

    # override super
    def add_unredirected_header(key, val):
        # will not be added to a redirected request
        # original implementation uses key.capitalize(), however, we will not modify the keys.
        self.unredirected_hdrs[key] = val
    self.add_unredirected_header = add_unredirected_header

    def __init__(
        url,
        data=None,
        headers={},
        method=None
    ):
        # We want the "base class" to initialize headers, then after
        # it takes care of all the initialization, we then, overwrite
        # the headers property to make it into a Case Insensitive "MultiDict"
        self.__init__(url, data=data, headers={}, method=method)
        self.headers = VGSCIMultiDict(headers)
        self.url = url
        parsed_url = parse.urlsplit(url)
        self.path = parsed_url.path
        self.query_string = parsed_url.query
        if method:
            self.method = method

        return self

    self = __init__(url, data, headers, method)

    return self



URLS = [
    "http://netloc/path;parameters?query=argument&k1=v1&k2=v2#fragment",
    "https://u:p@example.com:8443/a/b?x=1&y=2#frag",
    "https://example.com",
    "https://example.com/",
    "https://example.com/pay?a=1&a=2",
    "http://ex%41mple.com/%7Epath?q=%20#f%20",
    "https://example.com#only-fragment",
    "ftp://files.example.com/pub/file.txt",
    "http://",
]
DATA = [None, b"", b"hello", builtins.bytes("request body")]
HEADERS = [
    {},
    {"Content-Type": "application/json", "X-A": "1"},
    {"content-length": "5", "Content-Length": "6"},
    [("X-Dup", "1"), ("x-dup", "2"), ("Accept", "*/*")],
    VGSCIMultiDict([("A", "1"), ("a", "2")]),
]
METHODS = [None, "", "GET", "POST"]


ATTRIBUTES = [
    "__name__", "full_url", "_full_url", "fragment", "type", "host", "selector", "url", "path",
    "query_string", "data", "_data", "body", "headers", "_headers", "unredirected_hdrs",
    "_tunnel_host", "origin_req_host", "unverifiable", "method",
]


def _snapshot(r):
    """Every public attribute of r: its type and repr, or that it is missing."""
    out = []
    for name in ATTRIBUTES:
        if not hasattr(r, name):
            out.append((name, "missing"))
            continue
        v = getattr(r, name)
        if type(v) == "VGSCIMultiDict":
            out.append((name, type(v), list(v.items()), str(v)))
        else:
            out.append((name, type(v), repr(v)))
    return out


def _attempt(f, *args):
    """f(*args), or the error it raised, as a comparable value."""
    result = safe(f)(*args)
    if result.is_err:
        return ("error", "%s" % result._val)
    return ("ok", result.unwrap())


def _mutate(r):
    """Changes r through its public API and returns what the API reported along the way."""
    seen = [r.get_method(), r.get_full_url(), r.has_proxy(), r.has_header("X-A"),
            r.get_header("x-a", "none"), _attempt(r.header_items), str(r.headers)]
    r.url = "https://other.example.org:444/new/path?z=9#g"
    seen.append((r.full_url, r.host, r.path, r.query_string, r.selector, r.fragment))
    r.body = b"changed"
    seen.append((r.data, r.body))
    r.headers = {"Content-Length": "7", "Y": "2"}
    r.add_header("Content-Length", "8")
    r.add_header("z-header", "3")
    seen.append((r.has_header("content-length"), r.get_header("Content-Length"), _attempt(r.header_items)))
    r.body = b"changed again"
    seen.append((r.has_header("Content-Length"), _attempt(r.header_items)))
    r.remove_header("Y")
    r.add_unredirected_header("U", "1")
    r.set_proxy("proxy.example.com:3128", "http")
    seen.append((r.host, r.type, r.selector, _attempt(r.header_items), r.unredirected_hdrs))
    return seen


def _test_same_object_as_legacy():
    n = 0
    for url in URLS:
        for data in DATA:
            for headers in HEADERS:
                for method in METHODS:
                    new = VGSHttpRequest(url, data=data, headers=headers, method=method)
                    old = _legacy_VGSHttpRequest(url, data=data, headers=headers, method=method)
                    case = (url, data, headers, method)
                    asserts.assert_that((case, _snapshot(new))).is_equal_to((case, _snapshot(old)))
                    asserts.assert_that((case, new.__name__, new.__class__ == VGSHttpRequest)).is_equal_to((case, "VGSHttpRequest", True))
                    asserts.assert_that((case, _mutate(new))).is_equal_to((case, _mutate(old)))
                    asserts.assert_that((case, _snapshot(new))).is_equal_to((case, _snapshot(old)))
                    n += 1
    asserts.assert_that(n).is_equal_to(len(URLS) * len(DATA) * len(HEADERS) * len(METHODS))


def _test_keyword_and_positional_arguments():
    for args in [("https://example.com/a",), ("https://example.com/a", b"d"), ("https://example.com/a", b"d", {"H": "1"}, "PUT")]:
        asserts.assert_that(_snapshot(VGSHttpRequest(*args))).is_equal_to(_snapshot(_legacy_VGSHttpRequest(*args)))


def _suite():
    _suite = unittest.TestSuite()
    _suite.addTest(unittest.FunctionTestCase(_test_same_object_as_legacy))
    _suite.addTest(unittest.FunctionTestCase(_test_keyword_and_positional_arguments))
    return _suite


_runner = unittest.TextTestRunner()
_runner.run(_suite())
